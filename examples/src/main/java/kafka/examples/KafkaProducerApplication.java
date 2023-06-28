package kafka.examples;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

public class KafkaProducerApplication {

    private final Producer<String, String> producer;
    final String outTopic;

    public KafkaProducerApplication(final Producer<String, String> producer,
                                    final String topic) {
        this.producer = producer;
        outTopic = topic;
    }

    public void produce(final String message) {
        final String[] parts = message.split("-");
        final String key, value;
        if (parts.length > 1) {
            key = parts[0];
            value = parts[1];
        } else {
            key = null;
            value = parts[0];
        }
        final ProducerRecord<String, String> producerRecord = new ProducerRecord<>(outTopic, key, value);
        producer.send(producerRecord,
                (recordMetadata, e) -> {
                    if(e != null) {
                        e.printStackTrace();
                    } else {
                        System.out.println("key/value " + key + "/" + value + "\twritten to topic[partition] " + recordMetadata.topic() + "[" + recordMetadata.partition() + "] at offset " + recordMetadata.offset());
                    }
                }
        );
    }

    public void shutdown() {
        producer.close();
    }

    public static void main(String[] args) {

        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaProperties.KAFKA_SERVER_URL
                + ":" + KafkaProperties.KAFKA_SERVER_PORT);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        props.put(ProducerConfig.CLIENT_ID_CONFIG, "myApp");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put("max.in.flight.requests.per.connection", 5);

        final String topic = "myTopic";
        final Producer<String, String> producer = new KafkaProducer<>(props);
        final KafkaProducerApplication producerApp = new KafkaProducerApplication(producer, topic);

        String filePath = "/home/donald/Documents/Code/Source/kafka-2.7.1-src/examples/src/main/java/kafka/examples/input.txt";
        try {
            // 1. 读取文件 input.txt
            List<String> linesToProduce = Files.readAllLines(Paths.get(filePath));

            // 2. 发送消息
            linesToProduce.stream().filter(l -> !l.trim().isEmpty())
                    .forEach(producerApp::produce);
            System.out.println("Offsets and timestamps committed in batch from " + filePath);
        } catch (IOException e) {
            System.err.printf("Error reading file %s due to %s %n", filePath, e);
        } finally {
            producerApp.shutdown();
        }
    }
}
