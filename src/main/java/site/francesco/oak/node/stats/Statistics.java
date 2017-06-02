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

import static java.util.Collections.synchronizedMap;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

class Statistics {

    private static final long SCALE = 100;

    private final AtomicLong n = new AtomicLong();

    private final AtomicLong sum = new AtomicLong();

    private final AtomicLong max = new AtomicLong();

    private final Map<Long, AtomicLong> histogram = synchronizedMap(new TreeMap<>());

    void addValue(long v) {
        histogram.computeIfAbsent(v / SCALE, n1 -> new AtomicLong()).incrementAndGet();
        n.incrementAndGet();
        sum.addAndGet(v);
        max.accumulateAndGet(v, Long::max);
    }

    double getMean() {
        return ((double) sum.get()) / n.get();
    }

    long getMax() {
        return max.get();
    }

    long getN() {
        return n.get();
    }

    long getSum() {
        return sum.get();
    }

    interface BucketConsumer {

        void consume(long min, long max, long value);

    }

    void forEachBucket(BucketConsumer consumer) {
        for (Entry<Long, AtomicLong> entry : histogram.entrySet()) {
            consumer.consume(entry.getKey() * SCALE, (entry.getKey() + 1) * SCALE, entry.getValue().get());
        }
    }

}
