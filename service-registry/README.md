# Service Discovery

When a group of computers set up, the only device they are aware of is themselves, even if they are connected to the same network. 

To form a logical cluster, the different nodes need to find out each other somehow, need to learn who else is in the same cluster and most importantly, how to communicate with those other nodes.


#### 1) Static Configuration

The obvious simple solution can be a **Static Configuration**. To simply find out all the nodes' addresses ahead of time, and we put them all in a single configuration file, and distributed the file among all the nodes before we launch the application. This way, all the nodes can communicate with each other using those addresses. 

The problem is if one of the nodes becomes unavailable, the other nodes will still try to use the old addresses and will never to be able to discover the new address of the node. 

Also if we want to expand our cluster, we will have to regenerate this file and distribute the new file to all the nodes. 

#### 2) Dynamic Configuration

- Some companies still manage their clusters in a similar way, with some degree of automation
- Evertime a new ndoe is added - one central configuration is updated
- An automated configuration management tool like *Chef* or *Pupper*, can pick up the configuration and distribute it among the nodes in the cluster
- More dynamic but still involves a human to update the configuration
 

## Service Registry with Zookeeper

The idea is as follows:

- We are going to start with a permanent znode called `/service_registry`
- every node that join the cluster will add a ephmeral sequential znode under the registry node in a simple fashion as in Leader Election
- Unlike in the Leader Election, this case these znode are not going to be empty. Instead, each node would put its own address inside its znode. 


## Service Discovery

- Each node that wants to communicate or just be aware of any other node in the cluster, needs to simply register a watcher on the `/service_registry` znode using the `getChildren()` methods. 
- Then when it wants to read or use a particular node's address, the node will simply need to call the `getData()` method, to read the address data store inside the znode. 
- If there is any change in the cluster at any point, the node is going to get notified immediately, with the `NodeChildrenChanged` event. 


## Leader/Worker Architecture

- Workers will register themselves with the cluster
- Only the leader will register for notificiations
- Leader will know about the state of the cluster at all times and distribute the work accordingly
- If a leader dies, the new leader will remove itself from the service registery and continue distributing the work



## Implementation

Storing Configuration / Address

- We can store any configuration in the znode
- We need to store the minimum data to allow communication within the cluster
- The address we will store will be in the form of: 
      Host Name: Port example: http://127.0.0.1:8080

```
Implementation: 
See service-registry
```

Terminal: 
```
evelynn@Evelynns-MacBook-Pro ~ % java -jar /Users/evelynn/Documents/zD/Project/Distributed\ System/service-registry/target/service.registry-1.0-SNAPSHOT-jar-with-dependencies.jar 8083
log4j:WARN No appenders could be found for logger (org.apache.zookeeper.ZooKeeper).
log4j:WARN Please initialize the log4j system properly.
log4j:WARN See http://logging.apache.org/log4j/1.2/faq.html#noconfig for more info.
Successfully connected to Zookeeper
znode name /election/c_0000000004
I am the leader
The cluster addresses are: []
The cluster addresses are: [http://localhost:8082]
The cluster addresses are: [http://localhost:8082, http://localhost:8081]
The cluster addresses are: [http://localhost:8082, http://localhost:8081, http://localhost:8080]
The cluster addresses are: [http://localhost:8082, http://localhost:8081]
```



## Summary

- Implemented a fully automated service registry and discovery using Zookeeper

- Using this Service Registry nodes can:
    - Register to the cluster by publishing their address
    - Register for updates to get any node's address

- Integreated with the Leader Election algorithm using callbacks

- Tested our implementation and proved it to be fully function and fault tolerant

- Using the Service Registry out cluster can scale without any modifications

- Using the address published in the service registry we can establish the communication within the cluster
