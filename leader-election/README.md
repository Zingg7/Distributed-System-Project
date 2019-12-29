
## Configuration (pom.xml)
```
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.8.0</version>
            <configuration>
                <source>1.8</source>
                <target>1.8</target>
            </configuration>
        </plugin>
    </plugins>
</build>


<dependencies>
    <dependency>
        <groupId>org.apache.zookeeper</groupId>
        <artifactId>zookeeper</artifactId>
        <version>3.4.12</version>
    </dependency>
</dependencies>
```


## Dubug using log levels and the IDE
create log4j.properties
```
# Root logger option
log4j.rootLogger = WARN, zookeeper

# Direct log messages to stdout
log4j.appender.zookeeper=org.apache.log4j.ConsoleAppender
log4j.appender.zookeeper.Target=System.out
log4j.appender.zookeeper.layout=org.apache.log4j.PatternLayout
log4j.appender.zookeeper.layout.ConversionPattern=%d{HH:mm:ss} %-5p %c{1}:%L - %m%n
```

## How connect and handle
Connection (KeeperState.SyncConnected)

Disconnection (KeeperState.Disconnected)
```
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.WatchedEvent;

import java.io.IOException;

public class LeaderElection implements Watcher{


    private  static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private ZooKeeper zooKeeper;
    public static void main(String[] args) throws IOException, InterruptedException {
        LeaderElection leaderElection = new LeaderElection();
        leaderElection.connectToZookeeper();
        leaderElection.run();
        leaderElection.close();
        System.out.println("Disconnected from Zookeeper, exiting application");
    }

    public void connectToZookeeper() throws IOException {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT,  this);
    }

    public void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    public void close() throws InterruptedException {
        zooKeeper.close();
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            case None:
                if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Successfully connected to Zookeeper");
                } else {
                    synchronized (zooKeeper) {
                        System.out.println("Disconnected from Zookeeper event");
                        zooKeeper.notifyAll();
                    }
                }
        }
    }
}

```

## Leader Election

```
public void volunteerForLeadership() throws KeeperException, InterruptedException {
    // C means candidates
    String znodePrefix = ELECTION_NAMESPACE + "/C_";
    String znodeFullPath = zooKeeper.create(znodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    System.out.println("znode name " + znodeFullPath);
    this.currentZnodeName = znodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
}

public void electLeader() throws KeeperException, InterruptedException {
    List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);

    Collections.sort(children);
    String smallestChild = children.get(0);

    if (smallestChild.equals(currentZnodeName)) {
        System.out.println("I am the leader.");
        return;
    }
    System.out.println("I am not the leader, " + smallestChild + "is the leader.");
}

private static final String ELECTION_NAMESPACE = "/election";
private String currentZnodeName;

public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
    LeaderElection leaderElection = new LeaderElection();
    leaderElection.connectToZookeeper();
    leaderElection.volunteerForLeadership();
    leaderElection.electLeader();
    leaderElection.run();
    leaderElection.close();
    System.out.println("Disconnected from Zookeeper, exiting application");
}
```


## Package and test our first distributed application (pom.xml)
tell maven to package our code

Remember this program is meant to deploy on the cluster and run as a distributed system

so we need to package it as a standalone jar file which includes all its dependencies, so we can easily deploy and run it

```
<plugin>
    <artifactId>maven-assembly-plugin</artifactId>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>single</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <archive>
            <manifest>
                <!-- the class that our main thread located -->
                <mainClass>LeaderElection</mainClass>
            </manifest>
        </archive>
        <descriptorRefs>
            <descriptorRef>jar-with-dependencies</descriptorRef>
        </descriptorRefs>
    </configuration>
</plugin>
```



open the terminal, run ` mvn clean package` which will build and package all our code together with its dependencies in a single excutable jar file


After the build has completed, we can find leader election jar with dependency file inside target directory.

```
> ls target
archive-tmp                                             leader.election-1.0-SNAPSHOT-jar-with-dependencies.jar  maven-status
classes                                                 leader.election-1.0-SNAPSHOT.jar
generated-sources                                       maven-archiver
```

## Watcher and Triggers

We can register a watcher when we call the methods
- getChildren(..)
- getData(..)
- exists(..)

The watcher allows us to get a notification when a change happens


If we passed a watcher to methods:
- getChildren(.., **watcher**) - Get notified when the list of a znode's children changes
- exists(znodePath, **watcher**) - Get notified if a znode gets deleted or created
- getData(znodePath, **watcher**) - Get notified if a znode's data gets modified

we can also takes a watcher to create a Zookeeper object
- public Zookeeper(String connectString, int sessionTimeout, Watcher watcher)

Watcher registered with getChildren(), exists(), and getData() are **one-time triggers**

If we want to get future notifications, we need to register the watcher again


```
/**
 * Watchers, Triggers and Introduction to Failure Detection
 */
public void watchTargetZnode() throws KeeperException, InterruptedException {
    Stat stat = zooKeeper.exists(TARGET_ZNODE, this);
    if (stat == null) {
        return;
    }

    byte[] data = zooKeeper.getData(TARGET_ZNODE, this, stat);
    List<String> children = zooKeeper.getChildren(TARGET_ZNODE, this);

    System.out.println("Data : " + new String(data) + ", children : " + children);
}

@Override
public void process(WatchedEvent event) {
    switch (event.getType()) {
        case None:
            if (event.getState() == Event.KeeperState.SyncConnected) {
                System.out.println("Successfully connected to Zookeeper");
            } else {
                synchronized (zooKeeper) {
                    System.out.println("Disconnected from Zookeeper event");
                    zooKeeper.notifyAll();
                }
            }
            break;
        case NodeDeleted:
            System.out.println(TARGET_ZNODE + " was deleted");
            break;
        case NodeCreated:
            System.out.println(TARGET_ZNODE + " was created");
            break;
        case NodeDataChanged:
            System.out.println(TARGET_ZNODE + " data changed");
            break;
        case NodeChildrenChanged:
            System.out.println(TARGET_ZNODE + " children changed");
            break;
    }

    try {
        watchTargetZnode();
    } catch (KeeperException e) {
    } catch (InterruptedException e) {
    }
}
```

run above, get to terminal

``` 
> ./zkCli.sh

> ls /
[election, zookeeper]

> create /target_znode "some test data"
Created /target_znode

# in IDEA, we can see: 
# /target_znode was created
# Data : some test data, children : []

> set /target_znode " some new data"
# /target_znode data changed
# Data :  some new data, children : []

> create /target_znode/child_znode " "
# /target_znode children changed
# Data :  some new data, children : [child_znode]

> rmr /target_znode
# /target_znode children changed
# /target_znode was deleted

```


## Watcher for Failure Detection

### The Herd Effect

- A larget number of nodes waiting for an event
- When the event happens all nodes get notified and they all wake up
- only one node can "succeed"
- Indicates bad design, can negatively impact the performance and can completely freeze the cluster
 

### Leader Re-election Algorithm

** Fault Tolerance **

- To be able to recover from failures
- Re-elect a new leader automatically
 
By using watchers, each node will watch the previous node ephmeral znode, and gets notified when that znode gets deleted. 

- If the deleted znode belongs to the leader, the notified node becomes the leader itself.
- If the notified znode do not belongs to the leader, the notified node simplely close the connection. 


```
public void reelectLeader() throws KeeperException, InterruptedException {
    Stat predecessorStat = null;
    String predecessorZnodeName = "";
    while (predecessorStat == null) {
        List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);

        Collections.sort(children);
        String smallestChild = children.get(0);

        if (smallestChild.equals(currentZnodeName)) {
            System.out.println("I am the leader");
            return;
        } else {
            System.out.println("I am not the leader");
            int predecessorIndex = Collections.binarySearch(children, currentZnodeName) - 1;
            predecessorZnodeName = children.get(predecessorIndex);
            predecessorStat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorZnodeName, this);
        }
    }

    System.out.println("Watching znode " + predecessorZnodeName);
    System.out.println();
}
    
```


```
@Override
public void process(WatchedEvent event) {
    switch (event.getType()) {
        case None:
            if (event.getState() == Event.KeeperState.SyncConnected) {
                System.out.println("Successfully connected to Zookeeper");
            } else {
                synchronized (zooKeeper) {
                    System.out.println("Disconnected from Zookeeper event");
                    zooKeeper.notifyAll();
                }
            }
        case NodeDeleted:
            try {
                reelectLeader();
            } catch (InterruptedException e) {
            } catch (KeeperException e) {
            }
    }
}
```
