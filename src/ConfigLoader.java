import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static Properties properties = new Properties();

    static {
        try (InputStream input = new FileInputStream("src/main/resources/config.properties")) {
            if (input == null) {
                throw new RuntimeException("config.properties file not found!");
            }
            properties.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("Error loading config.properties", ex);
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }
}
