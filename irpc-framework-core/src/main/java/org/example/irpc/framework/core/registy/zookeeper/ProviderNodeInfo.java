package org.example.irpc.framework.core.registy.zookeeper;

public class ProviderNodeInfo {

    private String applicationName;

    private String serviceName;

    private String address;

    private Integer weight;

    private String registryTime;

    private String group;

    public ProviderNodeInfo() {
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public String getRegistryTime() {
        return registryTime;
    }

    public void setRegistryTime(String registryTime) {
        this.registryTime = registryTime;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return "ProviderNodeInfo{" +
                "applicationName='" + applicationName + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", address='" + address + '\'' +
                ", weight=" + weight +
                ", registryTime='" + registryTime + '\'' +
                ", group='" + group + '\'' +
                '}';
    }
}
