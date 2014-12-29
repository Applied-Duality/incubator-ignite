/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache;

import org.apache.ignite.*;
import org.apache.ignite.configuration.*;
import org.gridgain.grid.cache.*;
import org.gridgain.testframework.junits.common.*;

import javax.cache.processor.*;
import java.io.*;
import java.util.*;

import static org.gridgain.grid.cache.GridCacheAtomicWriteOrderMode.*;
import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheFlag.*;
import static org.gridgain.grid.cache.GridCacheMode.*;

/**
 * Tests transform for extra traffic.
 */
public class GridCacheReturnValueTransferSelfTest extends GridCommonAbstractTest {
    /** Distribution mode. */
    private GridCacheDistributionMode distroMode;

    /** Atomicity mode. */
    private GridCacheAtomicityMode atomicityMode;

    /** Atomic write order mode. */
    private GridCacheAtomicWriteOrderMode writeOrderMode;

    /** Number of backups. */
    private int backups;

    /** Fail deserialization flag. */
    private static volatile boolean failDeserialization;

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        GridCacheConfiguration ccfg = new GridCacheConfiguration();

        ccfg.setBackups(backups);
        ccfg.setCacheMode(PARTITIONED);
        ccfg.setAtomicityMode(atomicityMode);
        ccfg.setAtomicWriteOrderMode(writeOrderMode);

        ccfg.setDistributionMode(distroMode);

        cfg.setCacheConfiguration(ccfg);

        return cfg;
    }

    /**
     * @throws Exception If failed.
     */
    public void testTransformAtomicPrimaryNoBackups() throws Exception {
        checkTransform(ATOMIC, PRIMARY, 0);
    }

    /**
     * @throws Exception If failed.
     */
    public void testTransformAtomicClockNoBackups() throws Exception {
        checkTransform(ATOMIC, CLOCK, 0);
    }

    /**
     * @throws Exception If failed.
     */
    public void testTransformAtomicPrimaryOneBackup() throws Exception {
        checkTransform(ATOMIC, PRIMARY, 1);
    }

    /**
     * @throws Exception If failed.
     */
    public void testTransformAtomicClockOneBackup() throws Exception {
        checkTransform(ATOMIC, CLOCK, 1);
    }

    /**
     * @throws Exception If failed.
     */
    public void testTransformTransactionalNoBackups() throws Exception {
        checkTransform(TRANSACTIONAL, PRIMARY, 0);
    }

    /**
     * @throws Exception If failed.
     */
    public void testTransformTransactionalOneBackup() throws Exception {
        checkTransform(TRANSACTIONAL, PRIMARY, 1);
    }

    /**
     * @param mode Atomicity mode.
     * @param order Atomic cache write order mode.
     * @param b Number of backups.
     * @throws Exception If failed.
     */
    private void checkTransform(GridCacheAtomicityMode mode, GridCacheAtomicWriteOrderMode order, int b)
        throws Exception {
        try {
            atomicityMode = mode;

            backups = b;

            writeOrderMode = order;

            distroMode = GridCacheDistributionMode.PARTITIONED_ONLY;

            startGrids(2);

            distroMode = GridCacheDistributionMode.CLIENT_ONLY;

            startGrid(2);

            failDeserialization = false;

            // Get client grid.
            IgniteCache<Integer, TestObject> cache = grid(2).jcache(null);

            if (backups > 0 && atomicityMode == ATOMIC)
                cache = cache.flagsOn(FORCE_TRANSFORM_BACKUP);

            for (int i = 0; i < 100; i++)
                cache.put(i, new TestObject());

            failDeserialization = true;

            info(">>>>>> Transforming");

            // Transform (check non-existent keys also).
            for (int i = 0; i < 200; i++)
                cache.invoke(i, new Transform());

            Set<Integer> keys = new HashSet<>();

            // Check transformAll.
            for (int i = 0; i < 300; i++)
                keys.add(i);

            cache.invokeAll(keys, new Transform());

            // Avoid errors during stop.
            failDeserialization = false;
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     *
     */
    private static class Transform implements EntryProcessor<Integer, TestObject, Void>, Serializable {
        /** {@inheritDoc} */
        @Override public Void process(MutableEntry<Integer, TestObject> entry, Object... args) {
            entry.setValue(new TestObject());

            return null;
        }
    }

    /**
     *
     */
    private static class TestObject implements Externalizable {
        /**
         *
         */
        public TestObject() {
            // No-op.
        }

        /** {@inheritDoc} */
        @Override public void writeExternal(ObjectOutput out) throws IOException {
            assert !failDeserialization;
        }

        /** {@inheritDoc} */
        @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            assert !failDeserialization;
        }
    }
}
