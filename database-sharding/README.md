# Database Sharding

## Introduction to Distributed Storage

### Storage in Distributed Systems
In a typical system we normally have **users** interacting with our **servers** through a ++well defined external API++ or a ++user interface++

Internally to our system, we have multiple **services** that perform real-time and background work on the users data 

Finally each system has some **storage layer** where we store both the temporary and long-term business data 


### Storage Types

- File System
    - A lower level, general purpose approach to storage of data of any format, structure or size
    - Best for unstructured data, or data with no relationship to other data
- Database
    - An application that provides additional capabilities (query language/engine), caching and performance optimizations
    - Provides restrictions on structure, realtionship and format
    - Guarantees ACID Transactions
        - Atomicity
        - Consistency
        - Isolation
        - Durability
    - Database are easy to build and replace



### Distributed Databases

#### Centralized Database Issues

- Single Point of Failure
    - Losing a database is a lot worse than losing a compute node
    - Temporary failure to operate the business
    - Permanently losing our data
    - Compute nodesn can easily be restored
    - Permanent data loss can be detrimental to the business
- Performance Bottleneck
    - Parallelism is limited to the nubmer of cores in a machine
    - Limited connections to the OS and network card can support
    - Minimum latency depends on the geographical location of the database instance and the user
    - Limited to the memory a single machine can have


### Summary

- We got the motivation for building a distributed storage and specifically a distributed database
- We talked about the different storage types and database types as well as the considerations for picking over the other
- Focused on the importance of having a reliable database


***


## Database Sharding

### Introduction to Data Sharding

- Sharding - Partitioning a large dataset into multiple smaller chunks of data called *shards*
- Using Sharding we can split a large database into smaller pieces living on different machines


Shrading based on Key
- Sharding is done based on the record's key
- The key determines in which shard
    - To find an existing record
    - To add a new record with a new key


### Sharding Strategies

1. Hash Based Sharding

- A hash function takes the record's key and generates a numeric has value out of it
- The hash value is used to determine which shard the record belongs to

- Advantage
    - With monotonically increasing keys and a good hash function
    - We can achieve even data distribution
- Disadvantage
    - Keys with "close" values will likely not fall in the same shard
    - Range based queries will span multiple shard

2. Range Based Sharding
- In range based strategy we divide the keyspace into multiple contiguous ranges
- Records, with nearby keys will more likely end up in the same shard
- Range based queries will be a lot more efficient

- Disadvantage
    - If our keyspace is clustered in certain ranges, we will not get even data distribution
    - We might need to readjust the ranges on a continuous basis to guarantee good data distribution

### Sharding Disadvantages

- Operations that involve records that reside on different shards became much more complex
- Concurrency control becomes much harder and more expensive than in a centralized database
- For ⬆️, Some Relational databases do not even support automated sharding 
- Relational databases are a lot harder to scale 
- One of the reasons for the rise in popularity of NoSQL Databases (DynamoDB, Cassandra, MongoDB, Redis, etc)
- NoSQL Databases do not guarantee the same Atomicity, Consistency, Isolation, Durability (ACID) like a SQL DB would 


#### NoSQL Databases Advantages and Disadvantages

- Some NoSQL Databases guarantee atomic of operations on multiple records only on records that reside on the same physical node
- Some NoSQL Databases do not guarantee strict consistency
- Some NoSQL Databases do not even guarantee atomicity of operations on multiple records at all
- NoSQL Databases are easier to shard and scale
- Designing a system with NoSQL Databases is more challenging


### Summary
- Learned about a techique called **sharding** to scale a large database across multiple nodes
- Learned how to shard a SQL DB table vertically and horizontally
- Learned a few sharding strategies
    - Hash based sharding
    - Range based sharding
- Challenges and disadvantages of sharding a database

***

## Dynamic Sharding with Consistent Hashing

- Some of the hash-based sharding strategy issues 
- How we can solve those issues with a popular and very powerful algorithm called **Consistent Hashing**

### Hash Based Sharding Issues

- if we want to add/remove a node from the cluster, we have to reshuffle a large number of keys not only from the node that is going to be removed but also from other nodes as well

- If not all the nodes in our cluster have the same capacity or CPU capabilities, we would like to allocate more records to the more powerful nodes that maybe have more memory and can handle more concurrent reads while allocating fewer records to maybe older or less powerful nodes to make sure they don't crash. However there is no way for us to achieve this using the standard hashing method.

### Consistent Hashing Algorithm node allocation

Idea: To hash not only the keys but also the nodes

- Hash them all to the same hash space 

- We can use the nodes IP addresses or any other unique identifiers as keys to using the hash function 

- In addition to having both the record keys and notes to the same space, we make the hash space continuous by turning it into a ring 



### Consistent Hashing Virtual Nodes

Assign a wider range of keys to stronger nodes and a smaller range of keys to weaker nodes 

- If some physical nodes are more powerful/have more capacity than other nodes, we can assign the stronger nodes more keys
- Method
    - Map each physical node -> one or multiple of virtual nodes
    - More powerful physical nodes -> many virtual nodes
    - Weaker/smaller physical nodes -> single or fewer virtual nodes
- We can build robust database cluster with existing hardware with no need for additional expenses

### Consitent Hashing with multiple hash functions

- Uneven load distribution: after applying a hash function, a disproportionately large portion of the hash space values up to one of the nodes and a disproportionately small portion is allocated to another node

#### Consistent Hashing - Uneven Load Distribution Solution

- Apply multiple hash function on each node
- Using multiple hash functions, each node will be mapped to multiple locations on the hash space ring
- We create an illusion of having more nodes than we actually have
- Statisically it distribted the load more evenly among the physical nodes


### Summary

- Learned about Consistent Hashing
- Using Consistent Hashing we mapped records and DB shards to the same hashing space
- This allowed us to scale our distributed DB both up and down
- We were able to address the capacity imbalance and design a scalable sharded distributed DB with a mix of servers with different HW capabilities
- We used multiple hash functions to achieve a better load distribution and avoid performance bottlenecks


## Database Replication, Consistency Models & Quorum Consensus

Sharding | Replication
:-:|:-:
Splitting the data and placing each chunk on a different machine | Creating identical copies of all the data, and placeing each copy on a different machine
No redundancy | Full Redundancy

### Database Replication Motivation

- High Availability
- Fault Tolerance
- Scalability/Performance


### Replicated Database Arcitectures

#### Master - Slave Architecture

- All the right operations go to the master and the read operations go to the slave

- Every write operation to the master is propagated to the slave, so that the slave always contains an identical copy of the data on the master

#### Master - Master Architecture

- Each node can take both reads and writes
- Every write is propagated to other nodes for consistency
- We can grow our database cluster to as many nodes as necessary 


### Database Consistency Models

#### Eventual Consistency

- If no further updates are made, eventually all readers will have access to the newest data
- However temporarily some readers may see stale data
- Provides lower latency and higher availability
- Good choice for systems that do not need to have the most up to date data across the board
- Examples:
    - Posts/updats to social media profile
    - Analytics for product ratings and number of reviews

#### Strict Consistency

- In Strict Consistency, the writer will not get an acknowledgement until we can guarantee that all the readers will see the new data
- Slows down operations and limits system's availability (if some replicas are temporarily not accessible)
- Essential for systems that need to be consistent across all the services
- Examples:
    - User's Account
    - Number of items in a store's inventory
    - Available/Booked seats in a theater

- Force through consistency by forcing the writer to wait until the master finishes replicating the new value to the slave before the writer can assume that that right was successful


### Quorum Consensus

- Every updates to a record increments the version number
- R - Minimum number of nodes a reader needs to read from
- W - Minimum number of nodes a writer needs to write to
- N - Number of nodes in the database cluster


### Summary 

- Data Replication in Distributed Databases
- Data Replication Architectures:
    - Master - Slave Architecture
    - Master - Master Architecture
- Consistency Models
    - Strict Consistency
    - Eventual Consistency
- Quorum Consensus
    - Optimizing for reads or writes
    - Choosing R and W for High Availability

**Final Note**
- When building a Distributed Database we don't need to choose between **Replication** and **Sharding**
- Most Distributed Database use both sharding and replication to achieve
    - High Scalability
    - Availability
    - Fault Tolerance
