package uk.gov.dstl.sapient.generator.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassificationConfig {
    private String type;
    private double confidence;
    private List<SubClassConfig> subClasses;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public List<SubClassConfig> getSubClasses() { return subClasses; }
    public void setSubClasses(List<SubClassConfig> subClasses) { this.subClasses = subClasses; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubClassConfig {
        private String type;
        private double confidence;
        private int level;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }

        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
    }
}
