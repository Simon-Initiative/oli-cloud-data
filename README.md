# oli-cloud-data project

This project exports data from OLI main and logging databases, formats that data into csv files, then uploads those files into Amazon S3 buckets

## Running the application in dev mode

Copy application.properties.example to application.properties
``` 
cd src/main/resources
cp application.properties.example application.properties
```
You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```
## Local S3 functionality 

In order to test out S3 functionality locally, you will need to provision and run a local copy using the following command
```
docker run -it --publish 8008:4566 -e SERVICES=s3 -e START_WEB=0 localstack/localstack:0.11.5
```
Create an AWS profile for your local instance using AWS CLI:
```
aws configure --profile localstack
AWS Access Key ID [None]: test-key
AWS Secret Access Key [None]: test-secret
Default region name [None]: us-east-1
Default output format [None]:
```
Create a S3 bucket using AWS CLI
```
aws s3 mb s3://quarkus.s3.quickstart --profile localstack --endpoint-url=http://localhost:8008
```
For more details, visit https://quarkus.io/guides/amazon-s3

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

## Running in Docker container

- First build and package the project using the commands above
- Derive a copy of the configuration file service.env from service.env.example 
- ```cp service.env.example service.env```
- Configure service.env by replacing the placeholder values within
- Build the docker container
- ```docker-compose buid```
- Run the container  
- ```docker-compose up -d```

## Calling the data endpoint
Example:
```
curl --location --request POST 'localhost:8080/cloud/quiz/data' \
--header 'api_token: j8682yesb7tJdc9xRWMC#t!&%aD28xfNvb*gJWv!SNQKzYnbgw' \
--header 'Content-Type: application/json' \
--data-raw '{
    "admitCode": "CC-S21",
    "quizId": "u02_quiz_01",
    "semester": "S21",
    "quizNumber": 2,
    "startDate": "2021-02-14",
    "endDate": "2021-02-21"
}'
```

## Creating an _über-jar_

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/oli-cloud-data-1.0.0-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.html.

## Related guides

- Amazon S3 ([guide](https://quarkus.io/guides/amazon-s3)): Connect to Amazon S3 cloud storage
- If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

