# Distributed Message Brokers

## Introduction

### Motivation

One of the properties of direct network communication is that it inherently **synchronous**. In this case we're not referring to the internal threading model or the network api used by the servers, but to the fact that through the entire data transaction both the server and the client have to be present and maintain a connection with each other. 

And even if the servers are part of different logical clusters, or if they communicate with each other through a load balancer, both the servers have to maintain the TCP connection to each other, or to the load balancer all the way from the first buyer of the HTTP request to the last byte of the HTTP response.

- Synchrnonous Communication
- Broadcasting Event to Many Services
- Traffic Peaks and Valleys


### Building scalable, event driven distributed systems with Message Brokers

#### Message Broker Definition

- Integermediary software (middleware) that passes messages between sender(s) and receiver(s)
- May provide additional capabilities like:
    - Data transformation
    - Validation
    - Queuing
    - Routing
- Full decoupling between sender(s) and receiver(s)


#### Message Brokers Scalability

- To avoid the ***Message Broker*** from being a single point of failure or a bottleneck
- ***Message Brokers*** need to be scalable and fault tolerant
- ***Message Brokers*** are distributed systems by themselves which makes them harder to design and configure
- The latency, when using a msessage broker, in most cases, is higher than when using direct communication

#### Most Popular Use Cases

- ***Distributed Queue*** - Message delivery from a single producer to a single consumer
- ***Publish/Subscribe*** - Publishing of a message from a single publisher to a group of subscribed consumers
 

***

## Apache Kafka

- Distributed Streaming Platform, for exchanging messages between different servers
- Can be described as a Message Broker on a high level
- Internally Kafka is a distributed system that may use multiple message brokers to handle the messages
 
**Why Apache Kafka**

- There are many great message brokers
    - Open Source: **RabbitMQ, ActiveMQ** etc..
    - Proprietary messaging systems offered by the cloud vendors
- **Apache Kafka** is open source and provides:
    - Distributed Queuing
    - Publish/Subscribe
- Beautifully designed Distributed System for high scalability aand fault tolerance


### Apache Kafka Architecture, Topic and Partitions

#### Producer Record
Message we want to publish to Kafka comes inside a record is consists of a **key**, a **value** which is our message and the **timestamp**.

#### Kafka Topic - Category of event/messages, that consists of *Partitions*
The center of the obstruction Kafka provides us is the **topic**.

- Each partition is an ordered queue
- Each record in a partition has an offset number

Apache Kafka Partitioning allows us to scale a Topic horizontally

#### Consumer Groups, Distributed Queue and Pub/Sub
- Single Consumer Group
    - Message from a topic are load balanced among consumers within a single consumer group
    - Like in a distributed queue
- Consumers in different Consumer Groups
    - Messages from a topic are broadcasted to all consumer groups
    - Like in a Publish/Subscribe system

***

## Apache Kafka as a Distributed System

### Kafka Performance and Scalability

#### Kafka Brokers & Topic Partitions

- Number of partitions in a topic ≈ maximum unit of parallelism for a Kafka Topic
- We can estimate the right number of partitions for a topic given expected our peak message rate/volme
- By adding more message brokers we can increase the Kafka topics capacity, transparently to the publishers

#### Topic Partitions and Parallelism

- Thanks to the partitioning of a Topic
    - We can have many broker instances working in parallel to handle incoming messages
    - And having many consumers, consuming in parallel from the same topic


### Kafka Fault Tolerance

#### Replication - Leader & Followers

- Each Kafka Topic is configured with a *replication factor*
- A *replication factor* is replicated by N Kafka brokers
- For each partition, only one broker acts as a partition leader, other brokers are partition followers
- The leader takes all the reads and writes
- The followers replicate the partition data to stay in sync with the leader

**⬆️ Pros & Cons ⬆️

- The higher replication factor the more failures our system can tolerate
- But more replication means more space is taken away fromt he system just for redundancy

#### Falut Tolerance Implementation

- The replication is configured on per topic basis
- Kafka tries its best to spread the partitions and leadership fairly among the brokers
- Kafka is using Apache Zookeeper for all its coordination logic
- Kafka is using Zookeeper as a registry for brokers as well as for monitoring and failure detection (ephemeral znodes, watchers, etc)
 

#### Data Persistence in Kafka

- Kafka persists all its message on disk
- Even after messages are consumed by the consumers, the records still stay within Kafka for a configurable period of time
- Persistence allows new consumers to join and consumer older messages
- Consumers that failed in the process of reading/processing a message to retry
- Failed Brokers can recover very fast

### Summary

- The implementation of Kafka as a distributed and learned lessons we can apply in our own distributed systems
- Topic partitioning allows us:
    - Scale a topic horizontally across multiple brokers and multple machines
    - Redundancy and replication allow us to achieve fault tolerance
- Persisitence to disk enables log replay, as well as fast Kafka brokers' recovery
- Use of Zookeeper for coordination, and fault detection

***

## Apache Kafka - Building a Kafka Cluster in Practice

### Running, Configuring and Testing a Kafka cluster

In the terminal
1. Running the zookeeper server start pointing to the config file
```
bin/zookeeper-server-start.sh config/zookeeper.properties
```

2. Launch a single Kafka broker using the Kafka server start script pointing to the server.properties config file
```
bin/kafka-server-start.sh config/server.properties
```
**⬆️ Now we have a fully functional single broker Kafka cluster ⬆️**

3. Before we can publish any messages to Kafka, we need to create a topic:

- (in another terminal) Use the Kafka topic script with the create argument 

- Point the script to our cluster by setting the bootstrap server to the address of our single Kafka server 

- Send a replication factor (our topic to one, meaning it will not be replicated to any other server since we don't have any other server yet) 

- Set the number of partitions to 1 (which means all the messages to the topic will be in one order at queue)

- Use the topic parameter to name the topic "chat"

```linux
bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factore 1 --partitions 1 --topic chat
```

4. Observe that the topic we just created now exists within Kafka 

use the same script with the list argument 

```linux
bin/kafka-topics.sh --list --bootstrap-server localhost:9092
```

5. Publish some messages to our chat

using the Kafka console producer script which is a very basic and feature limited script we can use to test our cluster


```
bin/kafka-console-producer.sh --broker-list localhost:9092 --topic chat
```

we will point it to a subset of our Kafka servers which is our only server at this point and tell it to publish messages to our chat topic 

6. Publish two messages while we still don't have any consumers now 

```
first message
second message
```

7. Start a consumer
- using the Kafka console consumer script pointing it to our cluster, telling you to consume messages from the chat topic 
- passing the from beginning argument to tell it to read all the messages that were ever published to the topic
```linux
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic chat --from-beginning
> first message
> second message
```

### Run a multi broker Kafka cluster

1. Create more `server.properties` file
- chage the port, id, log.dirs

2. Launch the server by pointing to new properties

3. Create a new topic with replication factor of 3

4. Describe the topic just created

Use the same script with the describe parameter 
```linux
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic purchases
> Topic:purchases PartitionCount:3  ReplicationFactor:3 Config:segment.bytes=1073741824
>   Topic: purchase     Partition:0     Leader: 0       Replicas: 0,1,2 Isr: 0,1,2
>   Topic: purchase     Partition:1     Leader: 2       Replicas: 2,0,1 Isr: 2,0,1
>   Topic: purchase     Partition:2     Leader: 1       Replicas: 1,2,0 Isr: 1,2,0
```

5. Publish a few purchase message to test
```
bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic purchases
> purchase1
> purchase2
> purchase3
> purchase4
```

6. read
```
> purchase3
> purchase2
> purchase1
> purchase4
```


### Test Kafka fault tolerance


### Summary

- Run a single multi-broker Kafka cluster
- Successfully created a signle partition topic, where the order of the messages is equivalent to the ordered they were published
- Successfully created a multi partition topic, where messages are not globally ordered within a topic
- Tested Kafka's failover and replication
- A failure of a single broker is transparent to the producers and the consumers
- No data has been lost thanks to partition replication


***

## Apache Kafka - Kafka Producer with Java

⬇️ terminal ⬇️ 
1. Setup of a single zookeeper instance
```
bin/zookeeper-server-start.sh config/zookeeper.properties
```
2. Launched three Kafka brokers
```
bin/kafka-server-start.sh config/server-1.properties
...
```
3. Create a new topic with a replication factor of two, three partitions and name the topic
```
bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 2 --partition 3 --topic events
```
⬇️ Maven ⬇️
4. Adding Kafka clients and login libraries into dependency section 
```
<dependencies>
    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>kafka-clients</artifactId>
        <version>2.3.0</version>
    </dependency>

    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-log4j12</artifactId>
        <version>1.7.6</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>log4j</groupId>
        <artifactId>log4j</artifactId>
        <version>1.2.17</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```
5. Method `createKafkaProducer`
take the bootstrap servers as an argument and create a fully configured Kafka producer for us

```java
public static Producer<Long, String> createKafkaProducer(String bootstrapServers) {
    Properties properties = new Properties();

    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.put(ProducerConfig.CLIENT_ID_CONFIG, "events-producer");
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

    return new KafkaProducer<>(properties);
}
```

6. implement `produceMessages` 
take the number of messages we want to produce in the Kafka producer as arguments 

```java
private static final String TOPIC = "events";

public static void produceMessages(int numberOfMessages, Producer<Long, String> kafkaProducer) throws ExecutionException, InterruptedException {
    int partition = 1;

    for (int i = 0; i < numberOfMessages; i++) {
        long key = i;
        String value = String.format("event %d", i);

        long timeStamp = System.currentTimeMillis();

        ProducerRecord<Long, String> record = new ProducerRecord<>(TOPIC, partition, timeStamp, key, value);

        RecordMetadata recordMetadata = kafkaProducer.send(record).get();

        System.out.println(String.format("Record with (key: %s, value: %s), was sent to (partition: %d, offset: %d",
                record.key(), record.value(), recordMetadata.partition(), recordMetadata.offset()));
    }
}
```

### Produce messages to a distributed Kafka Topic


```java
private static final String BOOTSTRAP_SERVERS = "localhost:9092,localhost:9093,localhost:9094";

public static void main(String[] args) {
    Producer<Long, String> kafkaProducer = createKafkaProducer(BOOTSTRAP_SERVERS);

    try {
        produceMessages(10, kafkaProducer);
    } catch (ExecutionException | InterruptedException e) {
        e.printStackTrace();
    } finally {
        kafkaProducer.flush();
        kafkaProducer.close();
    }
}
```

### Summary

- Learned how to build a Kafka producer, using Kafka's Java API
- Learned how to configure the producer programmatically
- Learned a few ways to send messages to a distributed Kafka Topic:
    - Sent messages directly to a particular partition
    - Used the key to defer the partition choice to Kafka's client library
    - Omiited the key to send messages in a round robin strategy


***

## Apache Kafka - Building Kafka Consumers, Scalability and Pub/Sub


### Kafka Consumer Java API

1. Add the same dependencies 

2. Implementing `createKafkaConsumer` method
this method is going to create a Kafka consumer for records that have keys of type long and values of type string to match the producer

```java
public static Consumer<Long, String> createKafkaConsumer(String bootstrapServers, String consumerGroup) {
    Properties properties = new Properties();

    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
    properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

    return new KafkaConsumer<>(properties);
}
```
3. Implement `consumeMessages` method

```java
public static void consumeMessages(String topic, Consumer<Long, String> kafkaConsumer) {
    kafkaConsumer.subscribe(Collections.singletonList(topic));

    while (true) {
        ConsumerRecords<Long, String> consumerRecords = kafkaConsumer.poll(Duration.ofSeconds(1));

        if (consumerRecords.isEmpty()) {
            // do something else
        }

        for (ConsumerRecord<Long, String> record : consumerRecords) {
            System.out.println(String.format("Received record (key: %d, value: %s, partition: %d, offset: %d",
                    record.key(), record.value(), record.partition(), record.offset()));
        }

        // do something with the records

        kafkaConsumer.commitAsync();
    }
}
```

4. Define the topic we're going to use to test our consumer and the list of bootstrap servers were going to use to connect our Kafka cluster

```java
private static final String TOPIC = "events";
private static final String BOOTSTRAP_SERVERS = "localhost:9092,localhost:9093,localhost:9094";

public static void main(String[] args) {
    String consumerGroup = "defaultConsumerGroup";
    if (args.length == 1) {
        consumerGroup = args[0];
    }

    System.out.println("Consumer is part of consumer group " + consumerGroup);

    Consumer<Long, String> kafkaConsumer = createKafkaConsumer(BOOTSTRAP_SERVERS, consumerGroup);

    consumeMessages(TOPIC, kafkaConsumer);
}
```

5. Build and package our application


### Partition Load Balancing within a Consumer Group

1. Test our consumer with a Kafka console producer
```
bin/kafka-console-producer.sh --broker-list localhost:9092 --topic events
```

2. Lauch a single instance of our Java consumer
```
java -jar target/kafka.consumer-1.0-SNAPSHOT-jar-with-dependencies.jar
> Consumer is part of consumer group defaultConsumerGroup
```

**Result: message would be dynamic load-balancing**

### Publish/Subscribe with Kafka

1. Run each instance within a separate consumer group
```
java -jar target/kafka.consumer-1.0-SNAPSHOT-jar-with-dependencies.jar group1/2/3...
```
**Result: message published to the topic is broadcasted to all the subscribers**


### Consumer commit failure


### Summary

- Learned the Kafka Consumer API
- We can now connect our distributed system components through Kafka
- Learned how Kafka automatically and dynamically balances the load on Kafka consumers within the same consumer group
- Experimented with the Publish/Subscribe pattern, where each consumer belonged to a different consumer group
- Learned how to manually commit to Kafka, which safeguards our consumer applications from losing messages
