# Distributed MongoDB

## Introduction to MongoDB

### Introduction to MongoDB

- MongoDB is a scalable, NoSQL database
- There are many databases, optimized for different
    - Data models
    - Workloads
    - Operations
- No single distributed database to rule them all
- MongoDB has a very simple data model
- We can focus on its distributed features, and practice what we learned right away
- MongoDB is very powerful, scalable and popular database

### MongoDB CRUD operations

- CRUD Operations:
    - Create
        - db.collections.insertOne(object)
        - db.collections.insertMany([object1, object2,...])
    - Read
        - db.collections.find(filter) 
    - Update
        - db.collections.updateOne(filter, update action)
        - db.collections.updateMany(filter, update action)
        - db.collections.replaceOne(filter, replacement)
    - Delete
        - db.collections.deleteOne(filter)
        - db.collections.deleteMany(filter)
- The document's "_id" is immutalbe and cannot be changed after the document's creation

### MongoDB Demo

1. Download MongoDB

2. Launch a single instance of MongoDB 

typing Mongo D and pointing into the config files location 

```shell
mongod --config /usr/local/etc/mongod.conf -v
```

3. Connect

using comman line `mongo`

4. Create a database

```
use [database-name]
```

5. add, read, etc

### Summary
- Learned about MongoDB, which is document-oriented, NoSQL dsitributed database
- MongoDB's terminology and data model
- Learned about basic CRUD (Create, Read, Update, Delete) operations
- Launched a *mongod* instance locally
- Connected to our *mongod* instance using the *mongo* command line client
- Practice creating a new database, a new collection, and insert, read, update and delete documents

***

## Scaling MongoDB using Data Replication

### MongoDB Replicaiton Sets

In a replication set, there is one node that is the primary. the rest of the nodes are considered secondaries， which is a variation of the master/slave.

Writes from the application client go to the primary node, the secondary is constantly sync with the primary to stay up-to-date.



### Write Semantics

**Data loss**: The primary goes down before the data is a synchronously replicated to the secondaries

Solution: We can specify right concern majority to force the right to be replicated to the majority of the nodes regardless of the cluster size

### Read Preferences

As we mentioned before, all the read operations are also directed to the primary to guarantee straight consistency. However we can change that behavior using the **read preference**.

When we issue the read request, we can set the read preference to primary preferred, to still read from the primary when it's available but if the primary has failed and a new primary has not been elected yet, we can still read from the secondary nodes that are not too much out of sync.


### Summary

- Learned how to create replication sets, and provide high availability through redundancy
- MongoDB's Master-Slave architecture, where a primary node takes all the writes and reads by default for consistency
- Data is replicated asynchronously to the secondary nodes
- Learned how to trade off write operation latency for higher reliability suing ***Write Concern*** (2+nodes, "majority")
- Using ***Read Preference***, we can trade-off strict consistency for eventual consistency, but get higher read throughput or lower latency

***

## Launching a Replication Set in Distributed MongoDB

### Replication Set configuration

1. launch each of those instances in a separate machine

```
mkdir -p /usr/local/var/mongodb/rs0-0
mkdir -p /usr/local/var/mongodb/rs0-1
mkdir -p /usr/local/var/mongodb/rs0-2
```

2. Launch the MongoDB instance 
- Pass it their application set name 
- Tell it to listen on port 27017 
- Bind it to localhost 
- Using the DB path parameter we pass it the directory where it will store all its data
- Finally it will limit its operations log size to 128

```
mongod --repSet re0 --port 27017 --bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/rs0-0 --oplogSize 128
```
```
mongod --repSet re0 --port 27018 --bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/rs0-0 --oplogSize 128
```
```
mongod --repSet re0 --port 27019 --bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/rs0-0 --oplogSize 128
```
could put all those parameters into three separate fake files and then point each node to itw own configuration


3. Initialize the replication set

- connect to any of the mangu the instances which is going to be part of their replication set 
```
mongo --port 27017
```
- call the `rs.initialize` method and pass it a JSON object which sets the ID of our application set as well as all the members which will be part of their replication set

- once we run that command our three node's joined together and run an election which elects one of the notes to be a primary and the rest of the notes become the secondaries 


### Building a Java client

- Online School Enrollment
    - Application to enroll new students into out online school
    - Our online-school database will have a separate collection for each course
    - Before we allowing a new student to join a course we validate:
        - Student is not already enrolled
        - Student has sufficiently highly GPA
    - The application can easily be extended to accept network calls and have more complex business logic

1. Add the MongoDB driver dependency so we connection to MongoDB using the Java API

```
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver</artifactId>
    <version>3.4.3</version>
</dependency>
```

2. Implement the `connectToMongoDB` method (in application class)
```java
private static final String MONGO_DB_URL = "mongodb://127.0.0.1:27017, 127.0.0.1:27018, 127.0.0.1:12719/?replicaSet = rs0";
private static final String DB_NAME = "online-school";
public static MongoDatabase connectToMongoDB(String url, String dbName) {
    MongoClient mongoClient = new MongoClient(new MongoClientURI(url));
    return mongoClient.getDatabase(dbName);
}
```

3. Create the main method and parse the input from our appliction arguments
```java
public static void main(String[] args) {
    String courseName = args[0];
    String studentName = args[1];
    int age = Integer.parseInt(args[2]);
    double gpa = Double.parseDouble(args[3]);
    
    MongoDatabase onlineSchoolDb = connectToMongoDB(MONGO_DB_URL, DB_NAME);
    
    enroll(onlineSchoolDb, courseName, studentName, age, gpa);
}
```

4. Implement the `enroll` method

```java
private static final double MIN_GPA = 90.0;

private static void enroll(MongoDatabase database, String course, String studentName, int age, double gpa) {
    if (!isValidCourse(database, course)) {
        System.out.println("Invalid course " + course);
        return;
    }

    MongoCollection<Document> courseCollection = database.getCollection(course)
            .withWriteConcern(WriteConcern.MAJORITY)
            .withReadPreference(ReadPreference.primaryPreferred());
    
    if (courseCollection.find(eq("name", studentName)).first() != null) {
        System.out.println("Student " + studentName + " already enrolled");
        return;
    }
    
    if (gpa < MIN_GPA) {
        System.out.println("Improve your grades");
        return;
    }
    
    courseCollection.insertOne(new Document("name", studentName).append("age", age).append("gpa", gpa));

    System.out.println("Student " + studentName + " was successfully enrolled in " + course);

    for (Document document : courseCollection.find()) {
        System.out.println(document);
    }
```

5. Implement the `isValidCourse` method
```java
private static boolean isValidCourse(MongoDatabase database, String course) {
    for (String collectionName : database.listCollectionNames()) {
        if (collectionName.equals(course)) {
            return true;
        }
    }
    return false;
}
```
6. Run `mvn clean package` to build our application

7. Try to enroll a student to physics course

```
java -jar mongodb.reader-1.0-SNAPSHOT-jar-with-dependencies.jar physics Michael 25 90.5
> Invalid course physics
```

8. Connect the course

```
mongo --port 27018

show dbs

use online-school

db.createCollection("physics")
```



### Failure Injection


### Summary

- Built a fully replicatd MongoDB dluster with 3 nodes using a Replication Set
- Build a client application that can read and write to out cluster through the MongoDB Java Drive library
- Learned to control the Write Concern and Read Preference per database collection
- Put our replication set to the test by injecting failure
- Observed that our data stayed intact and our cluster remained available for read and write operations

***

## Scaling MongoDB using Data Sharding 

### Sharding Strategies in MongoDB

- Hash based sharding strategy
- Range based sharding strategy


#### Sharding Strategy Decision

- The key and the sharding strategy must be chosen togetehr to guarantee
    - Scalability as the collection grows
    - Efficient queries as the number of operations per second increases

### MongoDB Router and Config servers



### Summary

- Learned how to sclae a distributed MongoDB using data sharding
- Learned about the two strategies (Hashing and Range) MongoDB supports and how they are implemented in MongoDB
- Documents are assigned to chunks that span the entire key space
- Chunks are load balanced across the shards based on size and number of documents
- mongos router that routs queries to the right shard
- Config Server replication set that stores, maintains and balances the chunks among the shard nodes
- Ended with a fully replicated and distributed MongoDB architecture that can scale horizontally and recover from failures, transparently to the user

***

## Launching a Sharded Distributed MongoDB

### Launch a sharded cluster of MongoDB

Minimum components we need to run a sharded cluster:
- Shard nodes thenselves
- Mongos router, whose job is to route queries from the client application to the correct shard
- Config server cluster, whose job is to maintain all the shards related data as well as to perform the balancing of chunks among the different shards 


#### Launch a sharded cluster of MongoDB

1. Launch our config server cluster 

we will need three directories one for each config server replication set member 

```
mkdir -p /usr/local/var/mongodb/config-srv-0
mkdir -p /usr/local/var/mongodb/config-srv-1
mkdir -p /usr/local/var/mongodb/config-srv-2
```

2. Launch the first config server instance by running the Mongo d instance with the config server parameter

```
mongod --configsvr --replSet config-rs --dbpath /usr/local/var/mongodb/config-srv-0 --bind_ip 127.0.0.1 --port 27020
```
Similarly to other two

3. Group those config servers into a single replicated cluster

```
# connect
mongo --port 27020
rs.initiate({...})
```

4. Lauch actual MongoDB shards

- Create the directory for shard
```
mkdir /usr/local/var/mongodb/shard-0
mkdir /usr/local/var/mongodb/shard-1
```
- Run those shards
```
mongod --shardsvr --port 27017 --bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/shrad-0 --oplogSize 128
```

```
mongod --shardsvr --port 27018 --bind_ip 127.0.0.1 --dbpath /usr/local/var/mongodb/shrad-0 --oplogSize 128
```

5. Launch our Mongos point its config database location to the config server replication set which consists of our three config server nodes and also tell mongers to bind to localhost and listen on 27023
```
mongos --configdb config-rs/127.0.0.1:27020, 127.0.0.1:27021, 127.0.0.1:27022 --bind_ip 127.0.0.1 --port 27023
```

6. Tell Mongos about our two shards
```
mongo --port 27023
```
- Add the first shard by running `sh.addShard()`
```
sh.addShard("127.0.0.1:27017") {}
sh.addShard("127.0.0.1:27018") {}
```

### Video on Demand Use Case




### Sharding a collection using range strategy





### Sharding a collection using hashed strategy





### Summary

- Brought all our theoretical and practical knowledge together
- Launched a distributed mongodb cluster with
    - Multiple shards
    - Mongos router
    - Replication Set of Config servers
- Went through the entire process of choosing a sharding strategy and sharding key based on the use case
- Implemented our sharding design decisions in practice
- Put or cluster to the test by inserting a large number of documents in each collections
- Laid out a plan for the evolution of our service and distributed system
