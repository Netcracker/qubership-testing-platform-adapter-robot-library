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

package org.qubership.atp.adapter.common.mocks;

import java.util.UUID;

import org.qubership.atp.adapter.common.context.AtpCompaund;
import org.qubership.atp.adapter.common.entities.Message;
import org.qubership.atp.ram.enums.TestingStatuses;
import org.qubership.atp.ram.enums.TypeAction;
import org.qubership.atp.ram.models.MetaInfo;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ModelMocks {
    private static final String compoundId = UUID.randomUUID().toString();
    private static final String compoundName = "Step";
    private static final String parentCompoundId = UUID.randomUUID().toString();
    private static final String parentCompoundName = "Compound";

    public static final String ENCRYPTED_MESSAGE = "Login as \"sysadm1\" with password " +
            "\"{ENC}{Rsm7948qIW/Gc+jiWMr+6w==}{mPqHrB1aTpkAjd1Ce+813Q==}\"";
    public Message generateMessage(String typeAction) {
        Message message = new Message();
        message.setMessage("some");
        message.setName("Test");
        message.setType(typeAction);
        MetaInfo metaInfo = new MetaInfo();
        metaInfo.setHidden(false);
        message.setMetaInfo(metaInfo);
        return message;
    }

    public Message generateEncryptedMessage(String typeAction) {
        Message message = new Message();
        message.setMessage(ENCRYPTED_MESSAGE);
        message.setName(ENCRYPTED_MESSAGE);
        message.setType(typeAction);
        MetaInfo metaInfo = new MetaInfo();
        metaInfo.setHidden(false);
        message.setMetaInfo(metaInfo);
        return message;
    }

    public AtpCompaund generateAtpCompounds(TestingStatuses status) {
        AtpCompaund parent = new AtpCompaund(parentCompoundId, parentCompoundName, null, TypeAction.COMPOUND,
                status, false);
        AtpCompaund compound = new AtpCompaund(compoundId, compoundName, parent, TypeAction.UI, status, false);
        return compound;
    }
}
