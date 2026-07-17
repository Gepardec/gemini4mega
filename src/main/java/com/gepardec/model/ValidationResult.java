package com.gepardec.model;

import java.util.List;

/**
 * Represents the validation result from the LLM time entry checker.
 */
public class ValidationResult {

    private Boolean valid;
    private List<ValidationError> errors;

    public ValidationResult() {
    }

    public ValidationResult(Boolean valid, List<ValidationError> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
    }

    public static class ValidationError {
        private Integer entryIndex;
        private String entryDate;
        private String entryProject;
        private String entryDescription;
        private String ruleId;
        private String category;
        private String severity;
        private String message;

        public ValidationError() {
        }

        public ValidationError(Integer entryIndex, String ruleId, String category, String severity, String message) {
            this.entryIndex = entryIndex;
            this.ruleId = ruleId;
            this.category = category;
            this.severity = severity;
            this.message = message;
        }

        public ValidationError(Integer entryIndex, String entryDate, String entryProject, String entryDescription,
                String ruleId, String category, String severity, String message) {
            this.entryIndex = entryIndex;
            this.entryDate = entryDate;
            this.entryProject = entryProject;
            this.entryDescription = entryDescription;
            this.ruleId = ruleId;
            this.category = category;
            this.severity = severity;
            this.message = message;
        }

        public Integer getEntryIndex() {
            return entryIndex;
        }

        public void setEntryIndex(Integer entryIndex) {
            this.entryIndex = entryIndex;
        }

        public String getEntryDate() {
            return entryDate;
        }

        public void setEntryDate(String entryDate) {
            this.entryDate = entryDate;
        }

        public String getEntryProject() {
            return entryProject;
        }

        public void setEntryProject(String entryProject) {
            this.entryProject = entryProject;
        }

        public String getEntryDescription() {
            return entryDescription;
        }

        public void setEntryDescription(String entryDescription) {
            this.entryDescription = entryDescription;
        }

        public String getRuleId() {
            return ruleId;
        }

        public void setRuleId(String ruleId) {
            this.ruleId = ruleId;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("ValidationError{");
            sb.append("entryIndex=").append(entryIndex);

            if (entryDate != null) {
                sb.append(", entryDate='").append(entryDate).append('\'');
            }
            if (entryProject != null) {
                sb.append(", entryProject='").append(entryProject).append('\'');
            }
            if (entryDescription != null) {
                sb.append(", entryDescription='").append(entryDescription).append('\'');
            }

            sb.append(", ruleId='").append(ruleId).append('\'');
            sb.append(", category='").append(category).append('\'');
            sb.append(", severity='").append(severity).append('\'');
            sb.append(", message='").append(message).append('\'');
            sb.append('}');

            return sb.toString();
        }
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "valid=" + valid +
                ", errors=" + errors +
                '}';
    }
}
