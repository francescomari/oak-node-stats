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

package site.francesco.oak.node.stats;

import static java.lang.Runtime.getRuntime;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.commons.PathUtils;
import org.apache.jackrabbit.oak.segment.SegmentNodeState;
import org.apache.jackrabbit.oak.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.segment.SegmentNodeStoreBuilders;
import org.apache.jackrabbit.oak.segment.file.FileStoreBuilder;
import org.apache.jackrabbit.oak.segment.file.ReadOnlyFileStore;
import org.apache.jackrabbit.oak.spi.state.ChildNodeEntry;
import org.apache.jackrabbit.oak.spi.state.NodeState;

public class Main {

    private static final CountDownLatch done = new CountDownLatch(1);

    private static final AtomicInteger pending = new AtomicInteger();

    private static final BlockingQueue<SegmentNodeState> nodes = new ArrayBlockingQueue<>(1024 * 1024);

    private static final int processors = getRuntime().availableProcessors() - 1;

    private static final ExecutorService executor = Executors.newFixedThreadPool(processors);

    private static final Statistics propertiesStats = new Statistics();

    private static final Statistics propertyNamesLengthStats = new Statistics();

    private static final Statistics propertyValuesSizeStats = new Statistics();

    private static final Statistics childrenStats = new Statistics();

    private static final Statistics childNamesLengthStats = new Statistics();

    private static final Statistics singlePropertyNameStats = new Statistics();

    private static final Statistics singlePropertyValueStats = new Statistics();

    private static final Statistics singleChildNameStats = new Statistics();

    public static void main(String... args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: oak-node-stats directory [path]");
            System.exit(1);
        }
        String path = "/";
        if (args.length <= 2) {
            path = args[1];
        }
        for (int i = 0; i < processors; i++) {
            executor.submit(() -> {
                while (true) {
                    try {
                        process(nodes.take());
                    } finally {
                        if (pending.decrementAndGet() == 0) {
                            done.countDown();
                        }
                    }
                }
            });
        }
        ReadOnlyFileStore segmentStore = newFileStore(args[0]);
        SegmentNodeStore segmentNodeStore = newSegmentStore(segmentStore);
        NodeState root = segmentNodeStore.getRoot();
        for (String name : PathUtils.elements(path)) {
            if (root.hasChildNode(name)) {
                root = root.getChildNode(name);
            } else {
                System.err.println("Node at path " + path + " does not exist");
                System.exit(1);
            }
        }
        traverse((SegmentNodeState) root);
        done.await();
        printSummary("properties", propertiesStats);
        printSummary("property.names.length", propertyNamesLengthStats);
        printSummary("property.values.size", propertyValuesSizeStats);
        printSummary("children", childrenStats);
        printSummary("child.names.length", childNamesLengthStats);
        printSummary("single.property.names.length", singlePropertyNameStats);
        printSummary("single.property.values.size", singlePropertyValueStats);
        printSummary("single.child.names.length", singleChildNameStats);
        System.exit(0);
    }

    private static ReadOnlyFileStore newFileStore(String path) throws Exception {
        return FileStoreBuilder.fileStoreBuilder(new File(path)).buildReadOnly();
    }

    private static SegmentNodeStore newSegmentStore(ReadOnlyFileStore store) {
        return SegmentNodeStoreBuilders.builder(store).build();
    }

    private static void traverse(SegmentNodeState state) throws InterruptedException {
        pending.incrementAndGet();
        for (ChildNodeEntry entry : state.getChildNodeEntries()) {
            traverse((SegmentNodeState) entry.getNodeState());
        }
        nodes.put(state);
    }

    private static void process(SegmentNodeState state) throws Exception {
        long properties = 0;
        long propertyNamesLength = 0;
        long propertyValuesSize = 0;
        for (PropertyState property : state.getProperties()) {
            properties++;

            int pnl = property.getName().length();
            singlePropertyNameStats.addValue(propertyNamesLength);
            propertyNamesLength += pnl;

            if (property.getType() == Type.BINARY) {
                continue;
            }
            if (property.getType() == Type.BINARIES) {
                continue;
            }

            for (int i = 0; i < property.count(); i++) {
                long pvs = property.size(i);
                singlePropertyValueStats.addValue(pvs);
                propertyValuesSize += pvs;
            }
        }

        int children = 0;
        int childNamesLength = 0;
        for (ChildNodeEntry entry : state.getChildNodeEntries()) {
            children++;

            int cnl = entry.getName().length();
            singleChildNameStats.addValue(cnl);
            childNamesLength += cnl;
        }

        propertiesStats.addValue(properties);
        propertyNamesLengthStats.addValue(propertyNamesLength);
        propertyValuesSizeStats.addValue(propertyValuesSize);
        childrenStats.addValue(children);
        childNamesLengthStats.addValue(childNamesLength);
    }

    private static void printSummary(String name, Statistics stats) {
        System.out.printf("%s.n %s\n", name, stats.getN());
        System.out.printf("%s.max %s\n", name, stats.getMax());
        System.out.printf("%s.sum %s\n", name, stats.getSum());
        System.out.printf("%s.mean %s\n", name, stats.getMean());
        stats.forEachBucket((from, to, value) -> {
            if (value > 0) {
                System.out.printf("%s.histogram %d %d %s\n", name, from, to, value);
            }
        });
    }

}
