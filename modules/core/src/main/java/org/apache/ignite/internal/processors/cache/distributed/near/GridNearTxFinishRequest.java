/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.cache.distributed.near;

import org.apache.ignite.internal.processors.cache.distributed.*;
import org.apache.ignite.internal.processors.cache.version.*;
import org.apache.ignite.internal.util.tostring.*;
import org.apache.ignite.lang.*;
import org.apache.ignite.plugin.extensions.communication.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.*;
import java.util.*;

/**
 * Near transaction finish request.
 */
public class GridNearTxFinishRequest<K, V> extends GridDistributedTxFinishRequest<K, V> {
    /** */
    private static final long serialVersionUID = 0L;

    /** Mini future ID. */
    private IgniteUuid miniId;

    /** Explicit lock flag. */
    private boolean explicitLock;

    /** Store enabled flag. */
    private boolean storeEnabled;

    /** Topology version. */
    private long topVer;

    /** Subject ID. */
    private UUID subjId;

    /** Task name hash. */
    private int taskNameHash;

    /**
     * Empty constructor required for {@link Externalizable}.
     */
    public GridNearTxFinishRequest() {
        // No-op.
    }

    /**
     * @param futId Future ID.
     * @param xidVer Transaction ID.
     * @param threadId Thread ID.
     * @param commit Commit flag.
     * @param invalidate Invalidate flag.
     * @param sys System flag.
     * @param explicitLock Explicit lock flag.
     * @param storeEnabled Store enabled flag.
     * @param topVer Topology version.
     * @param txSize Expected transaction size.
     */
    public GridNearTxFinishRequest(
        IgniteUuid futId,
        GridCacheVersion xidVer,
        long threadId,
        boolean commit,
        boolean invalidate,
        boolean sys,
        boolean syncCommit,
        boolean syncRollback,
        boolean explicitLock,
        boolean storeEnabled,
        long topVer,
        int txSize,
        @Nullable UUID subjId,
        int taskNameHash) {
        super(xidVer, futId, null, threadId, commit, invalidate, sys, syncCommit, syncRollback, txSize);

        this.explicitLock = explicitLock;
        this.storeEnabled = storeEnabled;
        this.topVer = topVer;
        this.subjId = subjId;
        this.taskNameHash = taskNameHash;
    }

    /**
     * @return Explicit lock flag.
     */
    public boolean explicitLock() {
        return explicitLock;
    }

    /**
     * @return Store enabled flag.
     */
    public boolean storeEnabled() {
        return storeEnabled;
    }

    /**
     * @return Mini future ID.
     */
    public IgniteUuid miniId() {
        return miniId;
    }

    /**
     * @param miniId Mini future ID.
     */
    public void miniId(IgniteUuid miniId) {
        this.miniId = miniId;
    }

    /**
     * @return Subject ID.
     */
    @Nullable public UUID subjectId() {
        return subjId;
    }

    /**
     * @return Task name hash.
     */
    public int taskNameHash() {
        return taskNameHash;
    }

    /**
     * @return Topology version.
     */
    @Override public long topologyVersion() {
        return topVer;
    }

    /** {@inheritDoc} */
    @Override public boolean writeTo(ByteBuffer buf, MessageWriter writer) {
        writer.setBuffer(buf);

        if (!super.writeTo(buf, writer))
            return false;

        if (!writer.isTypeWritten()) {
            if (!writer.writeByte(null, directType()))
                return false;

            writer.onTypeWritten();
        }

        switch (writer.state()) {
            case 15:
                if (!writer.writeBoolean("explicitLock", explicitLock))
                    return false;

                writer.incrementState();

            case 16:
                if (!writer.writeIgniteUuid("miniId", miniId))
                    return false;

                writer.incrementState();

            case 17:
                if (!writer.writeBoolean("storeEnabled", storeEnabled))
                    return false;

                writer.incrementState();

            case 18:
                if (!writer.writeUuid("subjId", subjId))
                    return false;

                writer.incrementState();

            case 19:
                if (!writer.writeInt("taskNameHash", taskNameHash))
                    return false;

                writer.incrementState();

            case 20:
                if (!writer.writeLong("topVer", topVer))
                    return false;

                writer.incrementState();

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public boolean readFrom(ByteBuffer buf) {
        reader.setBuffer(buf);

        if (!super.readFrom(buf))
            return false;

        switch (readState) {
            case 15:
                explicitLock = reader.readBoolean("explicitLock");

                if (!reader.isLastRead())
                    return false;

                readState++;

            case 16:
                miniId = reader.readIgniteUuid("miniId");

                if (!reader.isLastRead())
                    return false;

                readState++;

            case 17:
                storeEnabled = reader.readBoolean("storeEnabled");

                if (!reader.isLastRead())
                    return false;

                readState++;

            case 18:
                subjId = reader.readUuid("subjId");

                if (!reader.isLastRead())
                    return false;

                readState++;

            case 19:
                taskNameHash = reader.readInt("taskNameHash");

                if (!reader.isLastRead())
                    return false;

                readState++;

            case 20:
                topVer = reader.readLong("topVer");

                if (!reader.isLastRead())
                    return false;

                readState++;

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public byte directType() {
        return 53;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return GridToStringBuilder.toString(GridNearTxFinishRequest.class, this, "super", super.toString());
    }
}
