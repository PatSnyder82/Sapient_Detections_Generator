package uk.gov.dstl.sapient.generator.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectConfig {
    private String name;
    private double updateFrequencyHz;
    private ClassificationConfig classification;
    private MovementConfig movement;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getUpdateFrequencyHz() { return updateFrequencyHz; }
    public void setUpdateFrequencyHz(double updateFrequencyHz) { this.updateFrequencyHz = updateFrequencyHz; }

    public ClassificationConfig getClassification() { return classification; }
    public void setClassification(ClassificationConfig classification) { this.classification = classification; }

    public MovementConfig getMovement() { return movement; }
    public void setMovement(MovementConfig movement) { this.movement = movement; }
}
