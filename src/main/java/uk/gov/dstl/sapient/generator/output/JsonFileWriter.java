package uk.gov.dstl.sapient.generator.output;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonFileWriter {

    private static final JsonFormat.Printer PRINTER = JsonFormat.printer();
    private final Path outputDirectory;

    public JsonFileWriter(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void ensureDirectoryExists() throws IOException {
        Files.createDirectories(outputDirectory);
    }

    /**
     * Serialize a protobuf message to JSON and write it to a file.
     * File name format: [Timestamp]_[ObjectName]_[ReportID].json
     */
    public void write(MessageOrBuilder message, String timestamp, String objectName, String reportId)
            throws InvalidProtocolBufferException, IOException {
        String json = PRINTER.print(message);
        String sanitizedTimestamp = timestamp.replace(":", "-").replace(".", "-");
        String fileName = sanitizedTimestamp + "_" + objectName + "_" + reportId + ".json";
        Path filePath = outputDirectory.resolve(fileName);
        Files.writeString(filePath, json);
    }
}
