package common;

import java.nio.file.Path;
import java.util.Objects;


public class Config {

    private String serviceName;
    private String host;
    private int port;


    private String metadataAddress;
    private Path dataDirectory;

    public String getServiceName() {
        return serviceName;
    }

    public Config setServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public String getHost() {
        return host;
    }

    public Config setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public Config setPort(int port) {
        this.port = port;
        return this;
    }

    public String getMetadataAddress() {
        return metadataAddress;
    }

    public Config setMetadataAddress(String metadataAddress) {
        this.metadataAddress = metadataAddress;
        return this;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public Config setDataDirectory(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        return this;
    }

    @Override
    public String toString() {
        return "Config{" +
                "serviceName='" + serviceName + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", metadataAddress='" + metadataAddress + '\'' +
                ", dataDirectory=" + dataDirectory +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Config)) return false;
        Config config = (Config) o;
        return port == config.port &&
                Objects.equals(serviceName, config.serviceName) &&
                Objects.equals(host, config.host) &&
                Objects.equals(metadataAddress, config.metadataAddress) &&
                Objects.equals(dataDirectory, config.dataDirectory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, host, port, metadataAddress, dataDirectory);
    }
}
