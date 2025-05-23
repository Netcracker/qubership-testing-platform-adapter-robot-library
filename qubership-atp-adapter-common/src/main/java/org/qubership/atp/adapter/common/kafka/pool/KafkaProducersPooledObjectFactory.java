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

package org.qubership.atp.adapter.common.kafka.pool;

import java.util.Properties;

import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import org.qubership.atp.adapter.common.RamConstants;
import org.qubership.atp.adapter.common.serialization.KafkaJsonSerializer;
import org.qubership.atp.adapter.common.serialization.KafkaProtobufSerializer;
import org.qubership.atp.adapter.common.utils.Config;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KafkaProducersPooledObjectFactory implements KeyedPooledObjectFactory<ProducerType, KafkaProducer> {

    @Override
    public PooledObject<KafkaProducer> makeObject(ProducerType producerType) {
        switch (producerType) {
            case JSON:
                log.trace("Create KafkaProducer pool object using KafkaJsonSerializer");
                return new DefaultPooledObject<>(createNewProducer(new KafkaJsonSerializer()));
            case PROTOBUF:
                log.trace("Create KafkaProducer pool object using KafkaProtobufSerializer");
                return new DefaultPooledObject<>(createNewProducer(new KafkaProtobufSerializer()));
            default:
                throw new IllegalArgumentException("Cannot create KafkaProducer: "
                        + "Incorrect producer type is passed: " + producerType);
        }
    }

    private KafkaProducer createNewProducer(Serializer kafkaSerializer) {
        Properties props = new Properties();
        Config config = Config.getConfig();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                config.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, RamConstants.DEFAULT_BOOTSTRAP_SERVERS));
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG,
                config.getIntProperty(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, RamConstants.DEFAULT_MAX_REQUEST_SIZE));
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,
                config.getProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, RamConstants.COMPRESSION_TYPE));
        return new KafkaProducer<>(props, new StringSerializer(), kafkaSerializer);
    }

    @Override
    public void destroyObject(ProducerType producerType, PooledObject<KafkaProducer> pooledObject) {
        log.trace("Close kafka producer type: {}", producerType);
        pooledObject.getObject().flush();
        pooledObject.getObject().close();
    }

    @Override
    public boolean validateObject(ProducerType producerType, PooledObject<KafkaProducer> pooledObject) {
        return pooledObject != null && pooledObject.getObject() != null;
    }

    @Override
    public void activateObject(ProducerType producerType, PooledObject<KafkaProducer> pooledObject) {
    }

    @Override
    public void passivateObject(ProducerType producerType, PooledObject<KafkaProducer> pooledObject) {
    }
}
