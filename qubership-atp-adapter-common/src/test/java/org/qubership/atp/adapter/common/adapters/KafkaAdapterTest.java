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

package org.qubership.atp.adapter.common.adapters;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.qubership.atp.adapter.common.RamConstants;
import org.qubership.atp.adapter.common.kafka.pool.KafkaPoolManagementService;
import org.qubership.atp.adapter.common.kafka.pool.KafkaProducersPooledObjectFactory;
import org.qubership.atp.adapter.common.kafka.pool.ProducerType;
import org.qubership.atp.adapter.common.utils.Config;

public class KafkaAdapterTest {

    int poolSize = 3;

    KafkaPoolManagementService kafkaPoolManagementService;

    KafkaProducersPooledObjectFactory kafkaProducersPooledObjectFactory = new KafkaProducersPooledObjectFactory();

    @Before
    public void init() {
        Config cfg = Config.getConfig();
        cfg.setProperty(RamConstants.KAFKA_PRODUCERS_POOL_MAX_TOTAL_PER_KEY, String.valueOf(poolSize));
        cfg.setProperty(RamConstants.KAFKA_PRODUCERS_POOL_MAX_WAIT_MILLIS, String.valueOf(30000));
        cfg.setProperty("bootstrap.servers", "http://localhost:9092");
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                Config.getConfig().getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        RamConstants.DEFAULT_BOOTSTRAP_SERVERS));
        kafkaProducersPooledObjectFactory = new KafkaProducersPooledObjectFactory();
        kafkaPoolManagementService = Mockito.spy(new KafkaPoolManagementService(kafkaProducersPooledObjectFactory));
    }

    @Test
    public void sendProducerRecord_MustBorrowKafkaProducerAndSendRecord_oneCall() {
        Mockito.doNothing().when(kafkaPoolManagementService).sendRecord(Mockito.any(), Mockito.any());
        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord("", ""), ProducerType.PROTOBUF);
        Mockito.verify(kafkaPoolManagementService, Mockito.times(1)).sendRecord(Mockito.any(), Mockito.any());
        Assert.assertEquals(1, kafkaPoolManagementService.getKafkaProducersKeyedPool().getCreatedCount());
    }

    @Test
    public void sendProducerRecord_MustBorrowKafkaProducerAndSendRecord_callsCountMoreThanPoolSize() {
        Mockito.doNothing().when(kafkaPoolManagementService).sendRecord(Mockito.any(), Mockito.any());
        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord("", ""), ProducerType.PROTOBUF);
        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord("", ""), ProducerType.PROTOBUF);
        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord("", ""), ProducerType.PROTOBUF);
        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord("", ""), ProducerType.PROTOBUF);
        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord("", ""), ProducerType.PROTOBUF);
        kafkaPoolManagementService.sendProducerRecord(new ProducerRecord("", ""), ProducerType.PROTOBUF);
        Mockito.verify(kafkaPoolManagementService, Mockito.times(6)).sendRecord(Mockito.any(), Mockito.any());
        Assert.assertEquals(1, kafkaPoolManagementService.getKafkaProducersKeyedPool().getCreatedCount());
    }

    @Test
    public void sendProducerRecord_MustBorrowKafkaProducerAndSendRecord_concurrentCallsCountMoreThanIdle() throws InterruptedException {
        Mockito.doNothing().when(kafkaPoolManagementService).sendRecord(Mockito.any(), Mockito.any());
        ExecutorService executorService = Executors.newFixedThreadPool(6);
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        CountDownLatch latch3 = new CountDownLatch(1);
        CountDownLatch latch4 = new CountDownLatch(1);
        CountDownLatch latch5 = new CountDownLatch(1);
        CountDownLatch latch6 = new CountDownLatch(1);
        executorService.execute(new SendThread(latch1));
        executorService.execute(new SendThread(latch2));
        executorService.execute(new SendThread(latch3));
        executorService.execute(new SendThread(latch4));
        executorService.execute(new SendThread(latch5));
        executorService.execute(new SendThread(latch6));

        latch1.await();
        latch2.await();
        latch3.await();
        latch4.await();
        latch5.await();
        latch6.await();

        Mockito.verify(kafkaPoolManagementService, Mockito.times(6)).sendRecord(Mockito.any(), Mockito.any());
        Assert.assertTrue(poolSize >= kafkaPoolManagementService.getKafkaProducersKeyedPool().getCreatedCount());
    }

    class SendThread implements Runnable {
        CountDownLatch latch;

        SendThread(CountDownLatch latch) {
            this.latch = latch;
        }

        public void run() {
            kafkaPoolManagementService.sendProducerRecord(new ProducerRecord("", ""), ProducerType.PROTOBUF);
            latch.countDown();
        }
    }

}
