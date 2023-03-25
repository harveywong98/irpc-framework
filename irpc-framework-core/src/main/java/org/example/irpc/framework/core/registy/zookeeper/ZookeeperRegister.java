package org.example.irpc.framework.core.registy.zookeeper;

import org.example.irpc.framework.core.common.event.IRpcEvent;
import org.example.irpc.framework.core.common.event.IRpcListenerLoader;
import org.example.irpc.framework.core.common.event.IRpcUpdateEvent;
import org.example.irpc.framework.core.common.event.data.URLChangeWrapper;
import org.example.irpc.framework.core.registy.RegistryService;
import org.example.irpc.framework.core.registy.URL;
import org.example.irpc.framework.interfaces.DataService;

import java.util.List;

/**
 * TODO 这样设计的原理是什么？什么设计模式？
 */
public class ZookeeperRegister extends AbstractRegister implements RegistryService {
    private AbstractZookeeperClient zkClient;

    private String ROOT = "irpc";

    private String getProviderPath(URL url) {
        return ROOT + "/" + url.getServiceName() + "/provider/" + url.getParameters().get("host") + ":" + url.getParameters().get("port");
    }

    private String getConsumerPath(URL url) {
        return ROOT + "/" + url.getServiceName() + "/consumer/" + url.getApplicationName() + ":" + url.getParameters().get("host")+":";
    }

    public ZookeeperRegister(String address) {
        // todo 注意这里
        this.zkClient = new CuratorZookeeperClient(address);
    }

    @Override
    public void register(URL url) {
        if (!this.zkClient.existNode(ROOT)) {
            zkClient.createPersistentData(ROOT, "");
        }
        String urlStr = URL.buildProviderUrlStr(url);
        if (!zkClient.existNode(getProviderPath(url))) {
            zkClient.createTemporaryData(getProviderPath(url), urlStr);
        } else {
            zkClient.deleteNode(getProviderPath(url));
            zkClient.createTemporaryData(getProviderPath(url), urlStr);
        }
        super.register(url);
    }

    @Override
    public void unRegister(URL url) {
        zkClient.deleteNode(getProviderPath(url));
        super.unRegister(url);
    }

    @Override
    public void subscribe(URL url) {
        if (!this.zkClient.existNode(ROOT)) {
            zkClient.createPersistentData(ROOT, "");
        }
        String urlStr = URL.buildConsumerUrlStr(url);
        if (!zkClient.existNode(getConsumerPath(url))) {
            zkClient.createTemporarySeqData(getConsumerPath(url), urlStr);
        } else {
            zkClient.deleteNode(getConsumerPath(url));
            zkClient.createTemporarySeqData(getConsumerPath(url), urlStr);
        }
        super.subscribe(url);
    }

    @Override
    public void doUnsubscribe(URL url) {
        this.zkClient.deleteNode(getConsumerPath(url));
        super.doUnsubscribe(url);
    }

    @Override
    public void doAfterSubscribe(URL url) {
        //监听是否有新的服务注册
        String newServerNodePath = ROOT + "/" + url.getServiceName() + "/provider";
        watchChildNodeData(newServerNodePath);
    }

    @Override
    public void doBeforeSubscribe(URL url) {

    }

    @Override
    public List<String> getProviderIps(String serviceName) {
        List<String> nodeDataList = this.zkClient.getChildrenData(ROOT + "/" + serviceName + "/provider");
        return nodeDataList;
    }

    public void watchChildNodeData(String newServerNodePath){
        zkClient.watchChildNodeData(newServerNodePath, watchedEvent -> {
            System.out.println(watchedEvent);
            String path = watchedEvent.getPath();
            List<String> childrenDataList = zkClient.getChildrenData(path);
            // TODO 这些是在做什么？
            URLChangeWrapper urlChangeWrapper = new URLChangeWrapper();
            urlChangeWrapper.setProviderUrl(childrenDataList);
            urlChangeWrapper.setServiceName(path.split("/")[2]);
            //自定义的一套事件监听组件
            IRpcEvent iRpcEvent = new IRpcUpdateEvent(urlChangeWrapper);
            IRpcListenerLoader.sendEvent(iRpcEvent);
            //收到回调之后在注册一次监听，这样能保证一直都收到消息
            // TODO 为什么是 watch childNodeData 而不是 watch 节点本身？
            watchChildNodeData(path);
        });
    }

    public static void main(String[] args) throws InterruptedException {
        ZookeeperRegister zookeeperRegister = new ZookeeperRegister("localhost:2181");
        List<String> urls = zookeeperRegister.getProviderIps(DataService.class.getName());
        System.out.println(urls);
        Thread.sleep(2000000);
    }

}
