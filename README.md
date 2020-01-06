# Distributed-System-Project

## Introduction and Motivation
Distributed System: A system of several processes, running on different computers, communicating with each other through the network, and are sharing a state or are working together to achieve a common goal.

process: After we compile our applications into an excutable class or jar file, it is stored on the file system, just like any other file. When we launch that application, the operating system creates an instance of that application in the memory, that is the process. The process is entirely isolated from any other process running on the same computer.
Inter-process Communication: Processes running on the same machine could communicate with each other through network, the file system and memory through some advanced techinque the operating system provides. 


## Introduction to Cluster Coordination & Theory of Leader Election
#### 1. Terminology 
Node: A process running on a dedicated machine
Cluster: Collection of computers/nodes connected to each other
The nodes in a cluster are working on the same task, and typically are running the same code

#### 2. How to break the work among nodes?
- Attempt 1: Manual Distribution
- Attempt 2: Manually Elect a Leader -> Leader Failure
- Attempt 3: Automatic Leader Election

**Challenges of Master - Workers Architecture**
- Automatic and System Leader election is not a trivial task to solve, even among people
- Arriving to an agreement on a leader in a large cluster of nodes is even harder
- By default each node knows only about itself - Service registry and discovery is required
- Failure Detection mechanism is necessary to trigger automatic leader reelection in a cluster

**Master - Workers Coordination Solution**
- Implement distributed algorithms for consensus and failover from scratch
- Apache Zookeeper

#### 3. Apache Zookeeper
- High Performance coordination service designed specifically for distributed systems
- Provides an abstraction layer for higher level distributed algorithms

**What makes Zookeeper a good Solution?**
- Is a distributed system itself that provides us high availability and reliability
- Typically runs in a cluster of an odd number of nodes, higher than 3
- Uses redundancy to allow failures and stay functional

**Distributed Systems with Zookeeper**

Instead of having our nodes communicating directly with each other to coordinate their work, they are going to communicate with Zookeeper service directly instead.

Zookeeper provides us with a very familiar and easy to use software abstraction and data model that looks like a tree and is very familiar to a file system. Each element in that tree or virtual file system is call a Znode.
 
#### 4. Znodes
- Hybrid between a file and a directory
    Znodes can store any data inside (like a file)
    Znodes can have childre znodes (like a directory)

- Two types of Znodes
    - Persistent - persists between sessions, stays within Zookeeper until it is explicitly deleted. 
    Using a persistent znode we can store data in between sessions
    - Ephemeral - is deleted when the session ends (its creator precess disconnects from Zookeeper) 
    Using an ephemeral znode we can detect that a process died or disconnected from the Zookeeper service

#### 5. Design the Leader Election Algorithm
Step 1: Every node that connects to Zookeeper volunteers to be a leader. Each node submits their candidacy by adding a znode represents itself under the election parent. Since Zookeeper maintains a global order, it can name each znode according to the order of their addition.  
Step 2: After each node finishs creating a znode, it will __ the current children of the election parent. Because of the order that Zookeeper provides us, each node when __ the children of their election parent, it guaranteed to see all the znodes created prior to its own znode creation. 
Step 3: If the zndoe that the current node created is the smallest number, it knows that it is now the leader. Other nodes are waiting for instructions from the elected leader. 

#### 6. What is a Coordination Service such as Apache Zookeeper and why do we need it?
A Coordination Service in distributed systems is a centralized service that allows us to corrdinate between a nodes and help with maintaining configuration information, distributed synchronization and many other group service.
Using a coordination service like Apache Zookeeper, individual nodes can exchange information, and run higher level algorithms to work together as a logical cluster.


## Zookeeper Client Threading Model & Zookeeper Java API
#### Zookeeper Threading Model
- Application's start code int the main method is executed on the main thread
- When Zoopkeeper object is created, two additional threads are created: Event Thread, IO Thread

#### IO Thread and Event Thread
- IO Thread
  - Handles all the network communication with Zookeeper servers
  - Handles Zookeeper requests and responses
  - Responds to pings
  - Session Managenment
  - Session Timeouts
  - etc...

- Event Thread
  - Manages Zookeeper events
      Connection (KeeperState.SyncConnected)
      Disconnection (KeeperState.Disconnected)
  - Custom znode Watchers and Triggers we subscirbe to
  - Events are executed on Event Thread in orde

### Zookeeper Java API
Maven
A build automation tool that will help us get all our dependencies and build unpacked application very easily.


## Watchers, Triggers and Introduction to Failure Dection
### Watchers And Triggers
We can register a watcher when we call the methods
- getChildren(..)
- getData(..)
- exists(..)

The watcher allows us to get a notification when a change happens

If we passed a watcher to methods:
- getChildren(.., watcher) - Get notified when the list of a znode's children changes
- exists(znodePath, watcher) - Get notified if a znode gets deleted or created
- getData(znodePath, watcher) - Get notified if a znode's data gets modified
we can also takes a watcher to create a Zookeeper object
- public Zookeeper(String connectString, int sessionTimeout, Watcher watcher)
Watcher registered with getChildren(), exists(), and getData() are one-time triggers


### Watcher for Failure Detection
#### The Herd Effect
- A larget number of nodes waiting for an event
- When the event happens all nodes get notified and they all wake up
- Only one node can "succeed"
- Indicates bad design, can negatively impact the performance and can completely freeze the cluster

#### Leader Re-election Algorithm
Fault Tolerance
- To be able to recover from failures
- Re-elect a new leader automatically

By using watchers, each node will watch the previous node ephmeral znode, and gets notified when that znode gets deleted.
- If the deleted znode belongs to the leader, the notified node becomes the leader itself.
- If the notified znode do not belongs to the leader, the notified node simplely close the connection.









