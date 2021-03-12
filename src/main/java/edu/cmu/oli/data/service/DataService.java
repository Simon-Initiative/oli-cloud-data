package edu.cmu.oli.data.service;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class DataService {

    private static final String quizTimeDiffQuery;
    private static final String quizDetailsQuery;
    private static final String registeredStudentQuery;
    private static final String loggingQuery;
    static Logger log = Logger.getLogger(DataService.class);

    private static final Set<QuizDetailsForm> duplicateRequests = Collections.synchronizedSet(new HashSet<>());

    static {
        quizTimeDiffQuery = readScript("quiz-time-diff.sql");
        quizDetailsQuery = readScript("quiz-details.sql");
        registeredStudentQuery = readScript("registered-students.sql");
        loggingQuery = readScript("logging.sql");
    }

    @Inject
    MySQLPool mainClient;

    @Inject
    @ReactiveDataSource("logging")
    MySQLPool loggingClient;

    @Inject
    S3AsyncClient s3;

    @ConfigProperty(name = "bucket.name")
    String bucketName;

    public Uni<JsonObject> quizData(QuizDetailsForm formData) {
        if(duplicateRequests.contains(formData)){
            return Uni.createFrom().item(new JsonObject(Map.of("message", "S3 already in progress")));
        }
        duplicateRequests.add(formData);
        Map<String, String> variableReplacements =
                Map.of("quiz_id", "'" + formData.quizId + "'",
                        "admit_code", "'" + formData.admitCode + "'",
                        "starting", "'" + formData.startDate + "'",
                        "ending", "'" + formData.endDate + "'");

        Uni<List<List<String>>> csvOutput = toCsvOutput(mainClient, parseQuery(quizTimeDiffQuery, variableReplacements));
        csvOutput.subscribe().with(data -> {
            String csvData = getCSVOutput(data);
            uploadFile(formData.s3Folder+"/"+formData.semester + "-Quiz-" + formData.quizNumber + "-" + formData.quizId + ".csv", csvData);
        });

        csvOutput = toCsvOutput(mainClient, parseQuery(quizDetailsQuery, variableReplacements));
        csvOutput.subscribe().with(data -> {
            String csvData = getCSVOutput(data);
            uploadFile(formData.s3Folder+"/"+formData.semester + "-Quiz-" + formData.quizNumber + "-" + formData.quizId + "_details.csv", csvData);
        });

        Uni<List<String>> studentList = studentList(parseQuery(registeredStudentQuery, variableReplacements));

        studentList.subscribe().with(it -> {
            String joinedStudentList = "'" + String.join("','", it) + "'";
            HashMap<String, String> stringStringHashMap = new HashMap<>(variableReplacements);
            stringStringHashMap.put("students", joinedStudentList);
            uploadLoggingData(formData, stringStringHashMap);
        });

        return Uni.createFrom().item(new JsonObject(Map.of("message", "S3 bucket data upload initiated")));
    }

    private static String readScript(final String scriptLocation) {
        try {
            InputStream stream = DataService.class.getClassLoader().getResourceAsStream(scriptLocation);
            return readFile(stream);
        } catch (Exception ex) {
            final String message = "An unexpected error has occurred while reading data creation query from file";
            // log.error(message, ex);
            throw new RuntimeException(message, ex);
        }
    }

    private static String readFile(InputStream inputStream) throws IOException {
        byte[] buffer;
        int length;
        try (inputStream) {
            buffer = new byte[inputStream.available()];
            length = inputStream.read(buffer);
        }
        return new String(buffer, 0, length, StandardCharsets.UTF_8);
    }

    private String getCSVOutput(List<List<String>> data) {
        StringWriter stringWriter = new StringWriter();
        String output = "";
        List<String> s = data.get(0);
        String[] headers = s.toArray(new String[0]);
        data.remove(0);
        CSVPrinter printer = null;
        try {
            printer = new CSVPrinter(stringWriter, CSVFormat.DEFAULT.withHeader(headers));
            printer.printRecords(data);
            output = stringWriter.toString();
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        } finally {
            try {
                if (printer != null) {
                    printer.close();
                }
                stringWriter.close();
            } catch (IOException e) {
            }
        }
        return output;
    }

    private void uploadFile(String fileName, String fileData) {

        Uni<String> csv = Uni.createFrom()
                .completionStage(() -> {
                    return s3.putObject(buildPutRequest(fileName),
                            AsyncRequestBody.fromFile(uploadToTemp(new ByteArrayInputStream(fileData.getBytes()))));
                })
                .onItem().ignore().andSwitchTo(Uni.createFrom().item(fileName + " uploaded"))
                .onFailure().recoverWithItem(th -> {
                    log.error(th.getMessage(), th);
                    return "Failed: " + th.getLocalizedMessage();
                });
        csv.subscribe().with(uploads -> log.info(uploads));
    }

    private void misc() {
        Uni.createFrom()
                .completionStage(() -> s3.listObjects(buildListObjectsRequest())).onItem().transform(ListObjectsResponse::contents)
                .subscribe().with(buckets -> log.info(buckets));
    }

    private void uploadLoggingData(QuizDetailsForm formData, Map<String, String> variableReplacements) {
        try {
            Uni<List<List<String>>> csvOutput = toCsvOutput(loggingClient, parseQuery(loggingQuery, variableReplacements));
            csvOutput.subscribe().with(data -> {
                String csvData = getCSVOutput(data);
                uploadFile(formData.s3Folder+"/"+formData.semester + "-log-data-" + formData.startDate + "-" + formData.endDate + ".csv", csvData);
            });
        }finally {
            duplicateRequests.remove(formData);
        }
    }

    private Uni<List<List<String>>> toCsvOutput(MySQLPool pool, String query) {
        AtomicInteger atomicInteger = new AtomicInteger();
        return pool.query(query).execute().onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform((Row row) -> fromRow(row, atomicInteger)).collect().asList();
    }

    private Uni<List<String>> studentList(String query) {
        return mainClient.query(query).execute().onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
                .onItem().transform(this::toStudent).collect().asList();
    }

    private String toStudent(Row row) {
        return row.getString("user_guid");
    }

    private List<String> fromRow(Row row, AtomicInteger atomicInteger) {
        List<String> sb = new ArrayList<>();
        if (atomicInteger.getAndIncrement() == 0) {
            // Add header
            for (int x = 0; x < row.size(); x++) {
                sb.add(row.getColumnName(x));
            }
        }
        for (int x = 0; x < row.size(); x++) {
            Object value = row.getValue(x);
            sb.add(value == null ? "null" : value.toString());
        }
        return sb;
    }

    /**
     * Replaces all variable values in a SQL file with a stringified version so that
     * it can be executed.
     */
    private String parseQuery(final String query, final Map<String, String> variableReplacements) {
        StringBuilder queryBuilder = new StringBuilder();
        Scanner scanner = new Scanner(query);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();

            // Ignore comments
            if (line.startsWith("#")) {
                continue;
            }

            // Find all ':' instances to replace in the sql query
            Pattern pattern = Pattern.compile(":(\\w+)");
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                line = matcher.replaceAll(variableReplacements.get(matcher.group(1)));
            }
            queryBuilder.append(line).append("\n");
        }
        scanner.close();

        return queryBuilder.toString();
    }

    protected PutObjectRequest buildPutRequest(String fileName) {
        return PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType("csv")
                .build();
    }

    protected ListObjectsRequest buildListObjectsRequest() {
        return ListObjectsRequest.builder()
                .bucket(bucketName)
                .build();
    }

    protected File uploadToTemp(InputStream data) {
        File tempPath;
        try {
            tempPath = File.createTempFile("uploadS3Tmp", ".tmp");
            Files.copy(data, tempPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return tempPath;
    }
}
