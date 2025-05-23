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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.admin.TopicDescription;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KafkaConfigurator {

    private final KafkaAdminClientService clientService;
    private final short replicationFactor;
    private static final short DEFAULT_REPLICATION_FACTOR = 3;

    public KafkaConfigurator(Properties properties) {
        this.clientService = new KafkaAdminClientService(propsToMap(properties));
        replicationFactor = DEFAULT_REPLICATION_FACTOR;
    }

    public KafkaConfigurator(Properties properties, short replicationFactor) {
        this.clientService = new KafkaAdminClientService(propsToMap(properties));
        this.replicationFactor = replicationFactor;
    }

    /**
     * Creates topic with specified name and number of partitions if not exists and increases partitions in existing
     * topic if necessary. Kafk admin client is closed after this operations.
     * @param topicName topic name
     * @param partitions number of partitions
     * */
    public void createOrUpdate(String topicName, int partitions) {
        try {
            log.debug("Create or update topic {} with {} partitions", topicName, partitions);
            if (clientService.isTopicExists(topicName)) {
                TopicDescription topicDescription = clientService.describeTopic(topicName);
                if (topicDescription.partitions().size() < partitions) {
                    log.info("Creating partitions for topic {}. Current number of partitions {}, requested number {}",
                            topicName, topicDescription.partitions().size(), partitions);
                    clientService.increasePartitions(topicName, partitions);
                }
            } else {
                log.info("Topic {} does not exist, creating it", topicName);
                clientService.createTopic(topicName, partitions, replicationFactor);
            }
        } catch (Exception exception) {
            log.error("Unrecoverable error: failed to configure topic {}", topicName, exception);
            throw new RuntimeException(exception);
        } finally {
            clientService.teardown();
        }
    }

    private static Map<String, Object> propsToMap(Properties properties) {
        Map<String, Object> propertiesMap = new HashMap<>();
        for (final String name : properties.stringPropertyNames()) {
            propertiesMap.put(name, properties.getProperty(name));
        }
        return propertiesMap;
    }
}
