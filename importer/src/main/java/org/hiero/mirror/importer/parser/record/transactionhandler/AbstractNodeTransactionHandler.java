// SPDX-License-Identifier: Apache-2.0

package org.hiero.mirror.importer.parser.record.transactionhandler;

import com.hedera.mirror.common.domain.entity.Node;
import com.hedera.mirror.common.domain.transaction.RecordItem;
import com.hedera.mirror.common.domain.transaction.Transaction;
import lombok.RequiredArgsConstructor;
import org.hiero.mirror.importer.parser.record.entity.EntityListener;

@RequiredArgsConstructor
public abstract class AbstractNodeTransactionHandler extends AbstractTransactionHandler {

    private final EntityListener entityListener;

    public abstract Node parseNode(RecordItem recordItem);

    @Override
    protected void doUpdateTransaction(Transaction transaction, RecordItem recordItem) {
        transaction.setTransactionBytes(recordItem.getTransaction().toByteArray());
        transaction.setTransactionRecordBytes(recordItem.getTransactionRecord().toByteArray());

        var node = parseNode(recordItem);
        if (node != null) {
            entityListener.onNode(node);
        }
    }
}
