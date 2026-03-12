package uk.gov.dstl.sapient.generator.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratorConfig {
    private String nodeId;
    private String destinationId;
    private String outputDirectory;
    private String utmZone;
    private List<ObjectConfig> objects;

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getDestinationId() { return destinationId; }
    public void setDestinationId(String destinationId) { this.destinationId = destinationId; }

    public String getOutputDirectory() { return outputDirectory; }
    public void setOutputDirectory(String outputDirectory) { this.outputDirectory = outputDirectory; }

    public String getUtmZone() { return utmZone; }
    public void setUtmZone(String utmZone) { this.utmZone = utmZone; }

    public List<ObjectConfig> getObjects() { return objects; }
    public void setObjects(List<ObjectConfig> objects) { this.objects = objects; }
}
