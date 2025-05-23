/*
 *  Copyright 2024-2025 NetCracker Technology Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.qubership.atp.adapter.common.kafka.client;

import static org.qubership.atp.adapter.common.kafka.error.AtpKafkaAdminClientException.KAFKA_TOPIC_CREATION_ERROR_MESSAGE_TEMPLATE;
import static org.qubership.atp.adapter.common.kafka.error.AtpKafkaAdminClientException.KAFKA_TOPIC_DELETE_ERROR_MESSAGE_TEMPLATE;
import static org.qubership.atp.adapter.common.kafka.error.AtpKafkaAdminClientException.KAFKA_TOPIC_DESCRIPTION_ERROR_MESSAGE_TEMPLATE;
import static org.qubership.atp.adapter.common.kafka.error.AtpKafkaAdminClientException.KAFKA_TOPIC_EXISTENCE_CHECK_ERROR_MESSAGE_TEMPLATE;
import static org.qubership.atp.adapter.common.kafka.error.AtpKafkaAdminClientException.KAFKA_TOPIC_INCREASE_PARTITIONS_ERROR_MESSAGE_TEMPLATE;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;

import org.qubership.atp.adapter.common.kafka.error.AtpKafkaAdminClientException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KafkaAdminClientService {

    private final AdminClient client;

    public KafkaAdminClientService(Map<String, Object> config) {
        client = AdminClient.create(config);
        log.debug("Kafka client {} created", client);
    }

    public void teardown() {
        client.close();
        log.debug("Kafka client {} closed.", client);
    }

    public KafkaFuture<Set<String>> getTopicNames() {
        ListTopicsResult ltr = client.listTopics();
        return ltr.names();
    }

    /**
     * Check if the specified topic exists.
     * */
    public boolean isTopicExists(String topicName) throws AtpKafkaAdminClientException {
        try {
            Set<String> existingTopics = getTopicNames().get();
            return existingTopics.contains(topicName);
        } catch (InterruptedException | ExecutionException exc) {
            String message = String.format(KAFKA_TOPIC_EXISTENCE_CHECK_ERROR_MESSAGE_TEMPLATE, topicName);
            log.error(message, exc);
            throw new AtpKafkaAdminClientException(exc, message);
        }
    }

    /**
     * Create topic with specified name, number of partitions and replication factor.
     * */
    public void createTopic(String topicName, int partitions, short replicationFactor) throws AtpKafkaAdminClientException {
        try {
            log.info("Creating topic {} with {} partitions and replication factor {}", topicName, partitions,
                    replicationFactor);
            KafkaFuture<Void> future = client
                    .createTopics(Collections.singleton(new NewTopic(topicName, partitions, replicationFactor)),
                            new CreateTopicsOptions().timeoutMs(10000))
                    .all();
            future.get();
        } catch (InterruptedException | ExecutionException exc) {
            String message = String.format(KAFKA_TOPIC_CREATION_ERROR_MESSAGE_TEMPLATE, topicName);
            log.error(message, exc);
            throw new AtpKafkaAdminClientException(exc, message);
        }
    }

    /**
     * Get {@link TopicDescription} for topic with specified name.
     * */
    public TopicDescription describeTopic(String topicName) throws AtpKafkaAdminClientException {
        try {
            log.info("Request for topic {} description", topicName);
            KafkaFuture<Map<String, TopicDescription>> topicDescriptionFuture =
                    client.describeTopics(Collections.singleton(topicName)).all();
            Map<String, TopicDescription> topicDescription = topicDescriptionFuture.get();
            return topicDescription.get(topicName);
        } catch (InterruptedException | ExecutionException exc) {
            String message = String.format(KAFKA_TOPIC_DESCRIPTION_ERROR_MESSAGE_TEMPLATE, topicName);
            log.error(message, exc);
            throw new AtpKafkaAdminClientException(exc, message);
        }
    }

    /**
     * Delete specified topic.
     * */
    public void deleteTopic(String topicName) throws AtpKafkaAdminClientException {
        log.info("Deleting topic {}", topicName);
        KafkaFuture<Void> future = client.deleteTopics(Collections.singleton(topicName)).all();
        try {
            future.get();
        } catch (InterruptedException | ExecutionException exc) {
            String message = String.format(KAFKA_TOPIC_DELETE_ERROR_MESSAGE_TEMPLATE, topicName);
            log.error(message, exc);
            throw new AtpKafkaAdminClientException(exc, message);
        }
    }

    /**
     * Increase number of partitions in topic up to specified number.
     * */
    public void increasePartitions(String topicName, int numPartitions) throws AtpKafkaAdminClientException {
        try {
            log.info("Increasing number of partitions in topic {} up to {}", topicName, numPartitions);
            Map<String, NewPartitions> newPartitionSet = new HashMap<>();
            newPartitionSet.put(topicName, NewPartitions.increaseTo(numPartitions));
            KafkaFuture<Void> future = client.createPartitions(newPartitionSet).all();
            future.get();
        } catch (InterruptedException | ExecutionException exc) {
            String message = String.format(KAFKA_TOPIC_INCREASE_PARTITIONS_ERROR_MESSAGE_TEMPLATE, topicName);
            log.error(message, exc);
            throw new AtpKafkaAdminClientException(exc, message);
        }
    }
}
