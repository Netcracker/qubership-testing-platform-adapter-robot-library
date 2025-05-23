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

package org.qubership.atp.adapter.common.adapters.providers;

import static org.qubership.atp.adapter.common.RamConstants.ADAPTER_TYPE_KEY;
import static org.qubership.atp.adapter.common.RamConstants.DEFAULT_ADAPTER_TYPE;
import static org.qubership.atp.adapter.common.RamConstants.IMPORTER_ADAPTER_TYPE;
import static org.qubership.atp.adapter.common.RamConstants.KAFKA_ADAPTER_TYPE;
import static org.qubership.atp.adapter.common.RamConstants.RECEIVER_ADAPTER_TYPE;
import static org.qubership.atp.adapter.common.RamConstants.STANDALONE_ADAPTER_TYPE;

import java.security.InvalidParameterException;

import org.qubership.atp.adapter.common.AtpRamAdapter;
import org.qubership.atp.adapter.common.adapters.AtpImporterRamAdapter;
import org.qubership.atp.adapter.common.adapters.AtpKafkaRamAdapter;
import org.qubership.atp.adapter.common.adapters.AtpReceiverRamAdapter;
import org.qubership.atp.adapter.common.adapters.AtpStandaloneRamAdapter;
import org.qubership.atp.adapter.common.context.TestRunContext;
import org.qubership.atp.adapter.common.utils.Config;

public class RamAdapterProvider {

    private static final String adapterType = Config.getConfig().getProperty(ADAPTER_TYPE_KEY, DEFAULT_ADAPTER_TYPE);

    public static AtpRamAdapter getNewAdapter(TestRunContext context) {
        switch (adapterType) {
            case RECEIVER_ADAPTER_TYPE:
                return new AtpReceiverRamAdapter(context);
            case KAFKA_ADAPTER_TYPE:
                return new AtpKafkaRamAdapter(context);
            case STANDALONE_ADAPTER_TYPE:
                return new AtpStandaloneRamAdapter(context);
            case IMPORTER_ADAPTER_TYPE:
                return new AtpImporterRamAdapter(context);
            default:
                throw new InvalidParameterException("Invalid adapter type");
        }
    }

    public static AtpRamAdapter getNewAdapter(String testRunName) {
        switch (adapterType) {
            case RECEIVER_ADAPTER_TYPE:
                return new AtpReceiverRamAdapter(testRunName);
            case KAFKA_ADAPTER_TYPE:
                return new AtpKafkaRamAdapter(testRunName);
            case STANDALONE_ADAPTER_TYPE:
                return new AtpStandaloneRamAdapter(testRunName);
            case IMPORTER_ADAPTER_TYPE:
                return new AtpImporterRamAdapter(testRunName);
            default:
                throw new InvalidParameterException("Invalid adapter type");
        }
    }
}
