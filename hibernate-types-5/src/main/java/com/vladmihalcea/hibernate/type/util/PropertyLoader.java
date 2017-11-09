package com.vladmihalcea.hibernate.type.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * <code>PropertyLoader</code> - The Property Loader allows declarative configuration through the <code>hibernate-types.properties</code> file.
 * It loads the {@link Properties} configuration file and it's then used to create all required properties.
 *
 * @author Vlad Mihalcea
 * @since 1.1.2
 */
public class PropertyLoader {

    public static final PropertyLoader INSTANCE = new PropertyLoader();

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyLoader.class);

    public static final String PROPERTIES_FILE_PATH = "hibernate-types.properties.path";
    public static final String PROPERTIES_FILE_NAME = "hibernate-types.properties";

    /**
     * Each Property has a well-defined key.
     */
    public enum PropertyKey {
        JACKSON_OBJECT_MAPPER("hibernate.types.jackson.object.mapper");

        private final String key;

        PropertyKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    private final Properties properties = new Properties();

    public PropertyLoader() {
        load();
    }

    public PropertyLoader(Properties overridingProperties) {
        this();
        properties.putAll( overridingProperties );
    }

    /**
     * Load {@link Properties} from the resolved {@link InputStream}
     */
    private void load() {
        InputStream propertiesInputStream = null;
        try {
            propertiesInputStream = propertiesInputStream();
            if (propertiesInputStream != null) {
                properties.load(propertiesInputStream);
            }
        } catch (IOException e) {
            LOGGER.error("Can't load properties", e);
        } finally {
            try {
                if (propertiesInputStream != null) {
                    propertiesInputStream.close();
                }
            } catch (IOException e) {
                LOGGER.error("Can't close the properties InputStream", e);
            }
        }
    }

    /**
     * Get {@link Properties} file {@link InputStream}
     *
     * @return {@link Properties} file {@link InputStream}
     * @throws IOException the file couldn't be loaded properly
     */
    private InputStream propertiesInputStream() throws IOException {
        String propertiesFilePath = System.getProperty(PROPERTIES_FILE_PATH);
        URL propertiesFileUrl = null;
        if (propertiesFilePath != null) {
            try {
                propertiesFileUrl = new URL(propertiesFilePath);
            } catch (MalformedURLException ignore) {
                propertiesFileUrl = ClassLoaderUtils.getResource(propertiesFilePath);
                if (propertiesFileUrl == null) {
                    File f = new File(propertiesFilePath);
                    if (f.exists() && f.isFile()) {
                        try {
                            propertiesFileUrl = f.toURI().toURL();
                        } catch (MalformedURLException e) {
                            LOGGER.error("The property " + propertiesFilePath + " can't be resolved to either a URL/a Classpath resource or a File");
                        }
                    }
                }
            }
            if (propertiesFileUrl != null) {
                return propertiesFileUrl.openStream();
            }
        }
        return ClassLoaderUtils.getResourceAsStream(PROPERTIES_FILE_NAME);
    }

    /**
     * Get {@link ObjectMapperWrapper} reference
     *
     * @return {@link ObjectMapperWrapper} reference
     */
    public ObjectMapperWrapper getObjectMapperWrapper() {
        Object objectMapperPropertyInstance = instantiateClass(PropertyKey.JACKSON_OBJECT_MAPPER);

        if (objectMapperPropertyInstance != null) {

            if(objectMapperPropertyInstance instanceof ObjectMapperSupplier) {
                ObjectMapper objectMapper = ((ObjectMapperSupplier) objectMapperPropertyInstance).get();
                if(objectMapper != null) {
                    return new ObjectMapperWrapper(objectMapper);
                }
            }
        }
        return ObjectMapperWrapper.INSTANCE;
    }

    /**
     * Instantiate class associated to the given property key
     *
     * @param propertyKey property key
     * @param <T>         class parameter type
     * @return class instance
     */
    private <T> T instantiateClass(PropertyKey propertyKey) {
        T object = null;
        String property = properties.getProperty(propertyKey.getKey());
        if (property != null) {
            try {
                Class<T> clazz = ClassLoaderUtils.loadClass(property);
                LOGGER.debug("Instantiate {}", clazz);
                object = clazz.newInstance();
            } catch (ClassNotFoundException e) {
                LOGGER.error("Couldn't load the " + property + " class given by the " + propertyKey + " property", e);
            } catch (InstantiationException e) {
                LOGGER.error("Couldn't instantiate the " + property + " class given by the " + propertyKey + " property", e);
            } catch (IllegalAccessException e) {
                LOGGER.error("Couldn't access the " + property + " class given by the " + propertyKey + " property", e);
            }
        }
        return object;
    }

    /**
     * Get Integer property value
     *
     * @param propertyKey property key
     * @return Integer property value
     */
    private Integer integerProperty(PropertyKey propertyKey) {
        Integer value = null;
        String property = properties.getProperty(propertyKey.getKey());
        if (property != null) {
            value = Integer.valueOf(property);
        }
        return value;
    }

    /**
     * Get Long property value
     *
     * @param propertyKey property key
     * @return Long property value
     */
    private Long longProperty(PropertyKey propertyKey) {
        Long value = null;
        String property = properties.getProperty(propertyKey.getKey());
        if (property != null) {
            value = Long.valueOf(property);
        }
        return value;
    }

    /**
     * Get Boolean property value
     *
     * @param propertyKey property key
     * @return Boolean property value
     */
    private Boolean booleanProperty(PropertyKey propertyKey) {
        Boolean value = null;
        String property = properties.getProperty(propertyKey.getKey());
        if (property != null) {
            value = Boolean.valueOf(property);
        }
        return value;
    }
}
