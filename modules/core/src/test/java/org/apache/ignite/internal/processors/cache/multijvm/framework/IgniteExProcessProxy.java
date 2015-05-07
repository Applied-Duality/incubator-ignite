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

package org.apache.ignite.internal.processors.cache.multijvm.framework;

import org.apache.ignite.*;
import org.apache.ignite.cache.affinity.*;
import org.apache.ignite.cluster.*;
import org.apache.ignite.configuration.*;
import org.apache.ignite.internal.*;
import org.apache.ignite.internal.cluster.*;
import org.apache.ignite.internal.processors.cache.*;
import org.apache.ignite.internal.processors.hadoop.*;
import org.apache.ignite.internal.util.*;
import org.apache.ignite.internal.util.lang.*;
import org.apache.ignite.internal.util.typedef.*;
import org.apache.ignite.internal.util.typedef.internal.*;
import org.apache.ignite.lang.*;
import org.apache.ignite.plugin.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Ignite proxy for ignite instance at another JVM.
 */
public class IgniteExProcessProxy implements IgniteEx {
    /** Grid proxies. */
    private transient static final Map<String, IgniteExProcessProxy> gridProxies = new HashMap<>();

    /** Jvm process with ignite instance. */
    private transient final GridJavaProcess proc;

    /** Configuration. */
    private transient final IgniteConfiguration cfg;

    /** Local jvm grid. */
    private transient final Ignite locJvmGrid;

    /** Logger. */
    private transient final IgniteLogger log;

    /** Grid id. */
    private final UUID id = UUID.randomUUID();

    /** Compute. */
    private transient final IgniteCompute compute;

    /**
     * @param cfg Configuration.
     * @param log Logger.
     * @param locJvmGrid Local jvm grid.
     */
    public IgniteExProcessProxy(final IgniteConfiguration cfg, final IgniteLogger log, final Ignite locJvmGrid) throws Exception {
        this.cfg = cfg;
        this.locJvmGrid = locJvmGrid;
        this.log = log.getLogger("jvm-" + id);

        IgniteNodeRunner.storeToFile(IgniteNodeRunner.IGNITE_CONFIGURATION_TMP_FILE, cfg);

        List<String> jvmArgs = U.jvmArgs();

        List<String> filteredJvmArgs = new ArrayList<>();

        for (String arg : jvmArgs) {
            if(!arg.toLowerCase().startsWith("-agentlib"))
                filteredJvmArgs.add(arg);
        }

        proc = GridJavaProcess.exec(
            IgniteNodeRunner.class,
            IgniteNodeRunner.asParams(id, cfg), // Params.
            this.log,
            // Optional closure to be called each time wrapped process prints line to system.out or system.err.
            new IgniteInClosure<String>() {
                @Override public void apply(String s) {
                    IgniteExProcessProxy.this.log.info(s);
                }
            },
            null,
            filteredJvmArgs, // JVM Args.
            System.getProperty("surefire.test.class.path")
        );

        // TODO: delete sleep.
        U.sleep(3_000);

        gridProxies.put(cfg.getGridName(), this);

        compute = locJvmGrid.compute(locJvmGrid.cluster().forNodeId(id));
    }

    /**
     * @param gridName Grid name.
     * @return Instance by name or <code>null</code>.
     */
    public static IgniteExProcessProxy get(String gridName) {
        return gridProxies.get(gridName);
    }

    /**
     * Kill all running processes.
     *
     * @throws Exception if failed.
     */
    public static void killAll() throws Exception {
        for (IgniteExProcessProxy ignite : gridProxies.values())
            try {
                ignite.getProcess().kill();
            }
            catch (Exception e) {
                U.error(ignite.log, "Killing failed.", e);
            }

        gridProxies.clear();
    }

    /**
     * @return Local JVM grid instance.
     */
    public Ignite localJvmGrid() {
        return locJvmGrid;
    }

    /**
     * @return Grid id.
     */
    public UUID getId() {
        return id;
    }

    /** {@inheritDoc} */
    @Override public String name() {
        return cfg.getGridName();
    }

    /** {@inheritDoc} */
    @Override public IgniteLogger log() {
        return log;
    }

    /** {@inheritDoc} */
    @Override public IgniteConfiguration configuration() {
        return cfg;
    }

    /** {@inheritDoc} */
    @Override public <K extends GridCacheUtilityKey, V> IgniteInternalCache<K, V> utilityCache() {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Nullable @Override public <K, V> IgniteInternalCache<K, V> cachex(@Nullable String name) {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Nullable @Override public <K, V> IgniteInternalCache<K, V> cachex() {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public Collection<IgniteInternalCache<?, ?>> cachesx(
        @Nullable IgnitePredicate<? super IgniteInternalCache<?, ?>>... p) {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public boolean eventUserRecordable(int type) {
        return false; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public boolean allEventsUserRecordable(int[] types) {
        return false; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public boolean isJmxRemoteEnabled() {
        return false; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public boolean isRestartEnabled() {
        return false; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Nullable @Override public IgniteFileSystem igfsx(@Nullable String name) {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public Hadoop hadoop() {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public IgniteClusterEx cluster() {
        return (IgniteClusterEx)locJvmGrid.cluster();
    }

    /** {@inheritDoc} */
    @Nullable @Override public String latestVersion() {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public ClusterNode localNode() {
        return F.first(compute.broadcast(new IgniteClosureX<Object, ClusterNode>() {
            @Override public ClusterNode applyx(Object o) {
                return ((IgniteEx)Ignition.ignite(id)).localNode();
            }
        }, null));
    }

    /** {@inheritDoc} */
    @Override public GridKernalContext context() {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public IgniteCompute compute() {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public IgniteCompute compute(ClusterGroup grp) {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public IgniteMessaging message() {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public IgniteMessaging message(ClusterGroup grp) {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public IgniteEvents events() {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public IgniteEvents events(ClusterGroup grp) {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public IgniteServices services() {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public IgniteServices services(ClusterGroup grp) {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public ExecutorService executorService() {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public ExecutorService executorService(ClusterGroup grp) {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public IgniteProductVersion version() {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public IgniteScheduler scheduler() {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public <K, V> IgniteCache<K, V> createCache(CacheConfiguration<K, V> cacheCfg) {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public <K, V> IgniteCache<K, V> createCache(String cacheName) {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public <K, V> IgniteCache<K, V> getOrCreateCache(CacheConfiguration<K, V> cacheCfg) {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public <K, V> IgniteCache<K, V> getOrCreateCache(String cacheName) {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public <K, V> void addCacheConfiguration(CacheConfiguration<K, V> cacheCfg) {
        // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public <K, V> IgniteCache<K, V> createCache(CacheConfiguration<K, V> cacheCfg,
        NearCacheConfiguration<K, V> nearCfg) {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public <K, V> IgniteCache<K, V> getOrCreateCache(CacheConfiguration<K, V> cacheCfg,
        NearCacheConfiguration<K, V> nearCfg) {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override  public <K, V> IgniteCache<K, V> createNearCache(@Nullable String cacheName, NearCacheConfiguration<K, V> nearCfg) {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public <K, V> IgniteCache<K, V> getOrCreateNearCache(@Nullable String cacheName,
        NearCacheConfiguration<K, V> nearCfg) {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public void destroyCache(String cacheName) {
        // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public <K, V> IgniteCache<K, V> cache(@Nullable final String name) {
        return new IgniteCacheProcessProxy(name, this);
    }

    /** {@inheritDoc} */
    @Override public IgniteTransactions transactions() {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public <K, V> IgniteDataStreamer<K, V> dataStreamer(@Nullable String cacheName) {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public IgniteFileSystem fileSystem(String name) {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public Collection<IgniteFileSystem> fileSystems() {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override  public IgniteAtomicSequence atomicSequence(String name, long initVal, boolean create) throws IgniteException {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public IgniteAtomicLong atomicLong(String name, long initVal, boolean create) throws IgniteException {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public <T> IgniteAtomicReference<T> atomicReference(String name, @Nullable T initVal,
        boolean create) throws IgniteException {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override  public <T, S> IgniteAtomicStamped<T, S> atomicStamped(String name, @Nullable T initVal, @Nullable S initStamp,
        boolean create) throws IgniteException {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public IgniteCountDownLatch countDownLatch(String name, int cnt, boolean autoDel,
        boolean create) throws IgniteException {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public <T> IgniteQueue<T> queue(String name, int cap,
        @Nullable CollectionConfiguration cfg) throws IgniteException {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public <T> IgniteSet<T> set(String name, @Nullable CollectionConfiguration cfg) throws IgniteException {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public <T extends IgnitePlugin> T plugin(String name) throws PluginNotFoundException {
        return null; // TODO: CODE: implement.
    }

    /** {@inheritDoc} */
    @Override public void close() throws IgniteException {
        try {
            getProcess().kill();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** {@inheritDoc} */
    @Override public <K> Affinity<K> affinity(String cacheName) {
        return null; // TODO: CODE: implement.
    }

    public GridJavaProcess getProcess() {
        return proc;
    }

    public static Ignite grid(@Nullable String name) {
//        IgniteNamedInstance grid = name != null ? grids.get(name) : dfltGrid;

        Ignite res = gridProxies.get(name);

        if (res == null)
            throw new IgniteIllegalStateException("Grid instance was not properly started " +
                "or was already stopped: " + name);

        return res;
    }
}
