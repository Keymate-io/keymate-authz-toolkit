package io.keymate.accessrules.loader;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.keymate.accessrules.model.AccessRuleSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads {@link AccessRuleSet} definitions from YAML files or strings
 * using Jackson with YAMLFactory.
 */
public final class PolicyLoader {

    private static final Logger log = LoggerFactory.getLogger(PolicyLoader.class);

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private PolicyLoader() {
        // utility class
    }

    /**
     * Loads an {@link AccessRuleSet} from a YAML file on disk.
     *
     * @param yamlPath path to the YAML policy file
     * @return the parsed AccessRuleSet
     * @throws PolicyLoadException if the file cannot be read or parsed
     */
    public static AccessRuleSet load(Path yamlPath) throws PolicyLoadException {
        log.debug("Loading policy from {}", yamlPath);
        try {
            String content = Files.readString(yamlPath);
            return loadFromString(content);
        } catch (IOException e) {
            throw new PolicyLoadException(
                    "Failed to read policy file: " + yamlPath, e);
        }
    }

    /**
     * Loads an {@link AccessRuleSet} from an input stream.
     *
     * @param input the input stream containing YAML content
     * @return the parsed AccessRuleSet
     * @throws PolicyLoadException if the stream cannot be read or parsed
     */
    public static AccessRuleSet load(InputStream input) throws PolicyLoadException {
        try {
            return MAPPER.readValue(input, AccessRuleSet.class);
        } catch (IOException e) {
            throw new PolicyLoadException(
                    "Failed to parse policy YAML from stream: " + e.getMessage(), e);
        }
    }

    /**
     * Parses an {@link AccessRuleSet} from a YAML string.
     *
     * @param yaml the YAML content
     * @return the parsed AccessRuleSet
     * @throws PolicyLoadException if the YAML cannot be parsed
     */
    public static AccessRuleSet loadFromString(String yaml) throws PolicyLoadException {
        try {
            return MAPPER.readValue(yaml, AccessRuleSet.class);
        } catch (IOException e) {
            throw new PolicyLoadException(
                    "Failed to parse policy YAML: " + e.getMessage(), e);
        }
    }
}
