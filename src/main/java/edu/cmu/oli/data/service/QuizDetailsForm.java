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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QuizDetailsForm that = (QuizDetailsForm) o;

        if (!admitCode.equals(that.admitCode)) return false;
        if (!quizId.equals(that.quizId)) return false;
        if (!semester.equals(that.semester)) return false;
        if (!s3Folder.equals(that.s3Folder)) return false;
        if (!quizNumber.equals(that.quizNumber)) return false;
        if (!startDate.equals(that.startDate)) return false;
        return endDate.equals(that.endDate);
    }

    @Override
    public int hashCode() {
        int result = admitCode.hashCode();
        result = 31 * result + quizId.hashCode();
        result = 31 * result + semester.hashCode();
        result = 31 * result + s3Folder.hashCode();
        result = 31 * result + quizNumber.hashCode();
        result = 31 * result + startDate.hashCode();
        result = 31 * result + endDate.hashCode();
        return result;
    }
}
