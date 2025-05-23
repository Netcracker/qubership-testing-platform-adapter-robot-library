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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import org.qubership.atp.adapter.common.RamConstants;
import org.qubership.atp.adapter.common.utils.Config;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class KafkaPoolManagementService {

    public static class KafkaPoolManagementServiceHolder {
        public static final KafkaPoolManagementService HOLDER_INSTANCE = new KafkaPoolManagementService();
    }

    private final GenericKeyedObjectPool<ProducerType, KafkaProducer> kafkaProducersKeyedPool;

    private int maxTotalPerKey;
    private long maxWaitMillis;

    public KafkaPoolManagementService() {
        this(new KafkaProducersPooledObjectFactory());
    }

    public KafkaPoolManagementService(KafkaProducersPooledObjectFactory kafkaProducersKeyedPool) {
        this.kafkaProducersKeyedPool = new GenericKeyedObjectPool<>(kafkaProducersKeyedPool, getPoolConfig());
    }

    protected GenericKeyedObjectPoolConfig getPoolConfig() {
        Config cfg = getConfig();
        String maxTotalPerKeyValue = cfg.getProperty(RamConstants.KAFKA_PRODUCERS_POOL_MAX_TOTAL_PER_KEY,
                StringUtils.EMPTY);
        String maxWaitMillisValue = cfg.getProperty(RamConstants.KAFKA_PRODUCERS_POOL_MAX_WAIT_MILLIS,
                StringUtils.EMPTY);
        GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
        if (StringUtils.isNotEmpty(maxTotalPerKeyValue) && StringUtils.isNotEmpty(maxWaitMillisValue)) {
            maxTotalPerKey = Integer.parseInt(maxTotalPerKeyValue);
            maxWaitMillis = Long.parseLong(maxWaitMillisValue);
            config.setMaxTotalPerKey(maxTotalPerKey);
            config.setMaxWaitMillis(maxWaitMillis);
        }
        log.debug("GenericKeyedObjectPoolConfig has been created: maxTotalPerKey : {} maxWaitMillis: {}",
                config.getMaxTotalPerKey(), config.getMaxWaitMillis());
        return config;
    }

    protected Config getConfig() {
        return Config.getConfig();
    }

    public static KafkaPoolManagementService getInstance() {
        return KafkaPoolManagementServiceHolder.HOLDER_INSTANCE;
    }

    /**
     * Send ProducerRecord via ProducerType.
     *
     * @param record ProducerRecord.
     */
    public void sendProducerRecord(ProducerRecord record, ProducerType producerType) {
        try {
            KafkaProducer kafkaProducer = kafkaProducersKeyedPool.borrowObject(producerType);
            sendRecord(kafkaProducer, record);
            kafkaProducersKeyedPool.returnObject(producerType, kafkaProducer);
            log.trace("Send ProducerRecord by KafkaProducer type {} and return to pool", producerType);
        } catch (Exception e) {
            log.error("Cannot borrow KafkaProducer type {}", producerType, e);
            throw new RuntimeException("Unable to borrow kafka producer from pool" + e);
        }
    }

    /**
     * Send record.
     *
     * @param kafkaProducer borrowed producer.
     * @param record        ProducerRecord.
     */
    public void sendRecord(KafkaProducer kafkaProducer, ProducerRecord record) {
        kafkaProducer.send(record);
    }
}
