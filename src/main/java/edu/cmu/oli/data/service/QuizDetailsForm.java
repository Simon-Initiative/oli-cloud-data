package edu.cmu.oli.data.service;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.time.LocalDate;

public class QuizDetailsForm {

    @NotBlank(message = "admit_code may not be blank")
    public String admitCode;

    @NotBlank(message = "quiz_id may not be blank")
    public String quizId;

    @NotBlank(message = "semester may not be blank")
    public String semester;

    @NotBlank(message = "s3Folder may not be blank")
    public String s3Folder;

    @Positive(message = "quiz_number may not be blank")
    public Integer quizNumber;

    @NotBlank(message = "start_date may not be blank")
    public String startDate;

    @NotBlank(message = "end_date may not be blank")
    public String endDate;

    public QuizDetailsForm() {
    }

    public QuizDetailsForm(String admitCode, String quizId, String semester,
                           Integer quizNumber, String startDate, String endDate) {
        this.admitCode = admitCode;
        this.quizId = quizId;
        this.semester = semester;
        this.quizNumber = quizNumber;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void validate() {
        LocalDate.parse(startDate);
        LocalDate.parse(endDate);
    }
}
