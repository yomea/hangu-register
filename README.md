# hangu-register

#### 一、介绍
一个peer-peer的去中心化注册中心

##### 1.1 为什么使用peer-peer的架构呢？
- 去中心化：所有的节点都是平等的，都可以提供写与读的能力，任何一个节点挂掉都不会影响其他节点
- 扩展性：扩容很简单，加入新的节点，只需发出新增节点的通知即可，无需复杂的成员变更信息同步，无需考虑脑裂的情况
- 简单性：没有master的概念，没有复杂的选举流程，强领导性的集群在master挂掉之后，通常不能提供服务，因为他们需要保证各个服务的状态一致性
- 一致性：采用的是最终一致性，会存在节点与节点之间短暂的不一致，对于注册中心来说，本人觉得应当更注重可用性和分区容错性，以得到更高的性能和稳定性

##### 1.2 与redis哨兵模式对比
redis哨兵模式采用主从架构，需要选举master，在选举过程中服务不可用，但是在数据复制过程中采用了异步复制的方式，所以严格来说redis
只是在选举的时候使用了raft算法，数据的复制并未使用raft算法，所以在数据一致性上，它依然是最终一致性，属于CAP理论中的AP。

##### 1.3 与zookeeper对比
zookeeper采用了zab协议的主从架构，与redis相比，更加强调领导性，数据的复制必须半数以上接受提议方可提交，保证数据的分布式一致性，属于CAP理论中的CP，
在性能上会比采用AP的差，如果集群规模N比较大，意味者要把数据成功同步到 N/2 + 1 个节点以上才能获取到数据

#### 二、软件架构

![在这里插入图片描述](https://img-blog.csdnimg.cn/direct/8666d5ef51164bac97d1e3d7405c183a.png#pic_center)

从架构图中可以看到，注册中心采用了peer-peer的架构，每个节点都可以读写操作，节点与节点之间通过心跳进行增量续约，如果集群中已有其他节点
在启动中，那么新启动的节点会向其中一个节点全量同步注册信息，当其中一个节点收到注册信息或者注销信息时，会尝试向其他的节点主动推送注册或者注销
信息，如果推送失败，会被跳过，最终一致性可通过心跳增量续约来保证。

提供者向注册中心注册并建立心跳续约，保持注册信息的有效性（过期会定时清理，消费者不能拉去到过期的数据）。

消费者可向注册中心拉取信息，也可订阅信息，订阅的信息发生变化时，注册中心会主动通知消费者，消费者与注册中心通过心跳保持链接的可用性，
如果链接失效，注册中心将从注册列表中剔除该链接

#### 三、快速开始

##### 3.1 项目目录结构
hangu-register
------hangu-center
------hangu-client
------hangu-common
------hangu-register-spring-boot-starter

hangu-register为项目主目录，其下有四个子模块
- hangu-center：注册中心模块，该模块提供注册中心服务能力
- hangu-client：客户端，用于链接注册中心的客户端，用户可依赖该模块连接注册中心
- hangu-common：一些通用工具类
- hangu-register-spring-boot-starter：客户端的spring-boot自动装配模块，使用springBoot的用户可依赖该模块自动启动

##### 3.2 注册中心启动

直接启动 hangu-center 模块里的 org.hangu.center.server.Bootstrap 类，注册中心的端口与其他节点地址的配置可在启动参数中设置：
```yaml
hangu:
  center:
    port: 9991
    #    peer-node-hosts: localhost:9991,localhost:9992
    peer-node-hosts: localhost:9992
    thread:
      core-num: 32
      max-num: 32
```

##### 3.3 接入注册中心

- 普通项目
普通项目直接依赖 hangu-client 模块，pom.xml配置如下：

```xml
 <dependency>
  <groupId>org.hangu.center</groupId>
  <artifactId>hangu-client</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```
启动客户端

```java
ClientProperties  clientProperties = new ClientProperties();
DiscoverClient discoverClient = CenterClientStarter.start(clientProperties);
```
启动客户端使用了 CenterClientStarter 启动器，参数为 ClientProperties
ClientProperties 用于设置注册中心集群地址和一些心跳超时之类的参数，返回值
DiscoverClient是一个实现了LookupService, RegistryService接口，实现了拉取注册订阅功能

- springboot项目

引入 hangu-register-spring-boot-starter 模块

```xml
 <dependency>
  <groupId>org.hangu.center</groupId>
  <artifactId>hangu-register-spring-boot-starter</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>

```
然后在application.yml中配置集群地址即可（参数对应 ClientProperties 类的属性）
```yaml
hangu:
  center:
    peer-node-hosts: localhost:9992
```






