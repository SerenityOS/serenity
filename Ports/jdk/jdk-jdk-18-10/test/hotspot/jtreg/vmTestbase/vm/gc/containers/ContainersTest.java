/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package vm.gc.containers;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.*;
import nsk.share.TestBug;
import nsk.share.gc.*;
import nsk.share.gc.gp.GarbageProducer;
import nsk.share.gc.gp.GarbageProducerAware;
import nsk.share.gc.gp.MemoryStrategy;
import nsk.share.gc.gp.MemoryStrategyAware;
import nsk.share.test.ExecutionController;

/*
 * The main class and launcher for all containers tests.
 * Allow to set containers via set -ct parameters. See parseFromString decription.
 */
public class ContainersTest extends ThreadedGCTest implements MemoryStrategyAware, GarbageProducerAware {

    private GarbageProducer garbageProducer;
    private MemoryStrategy memoryStrategy;
    MemoryUsage mbean = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

    public ContainersTest(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-ct")) {
                descrs.add(ContainerDescription.parseFromString(args[i + 1]));
            }
        }
        if (descrs.size() == 0) {
            throw new TestBug("No containers were given");
        }
    }

    @Override
    public void setMemoryStrategy(MemoryStrategy memoryStrategy) {
        this.memoryStrategy = memoryStrategy;
    }

    @Override
    public void setGarbageProducer(GarbageProducer gp) {
        this.garbageProducer = gp;
    }

    private class Mutator implements Runnable {

        Container container;

        public Mutator(Container container) {
            this.container = container;
        }

        @Override
        public void run() {
            ExecutionController stresser = getExecutionController();
            container.setExecutionController(stresser);
            try {
                container.initialize();
                while (stresser.continueExecution()) {
                    container.update();
                }
            } catch (OutOfMemoryError oome) {
                // The OOME is teoretically possible in the case
                // if data structure has a large overhead
            }
        }
    }
    List<ContainerDescription> descrs = new ArrayList<ContainerDescription>();
    List<Container> containers = new ArrayList<Container>();

    @Override
    protected Runnable createRunnable(int i) {
        return new Mutator(containers.get(i));
    }

    public Container createContainter(long maxMemory, ContainerDescription descr) {
        String[] prefixes = {"java.util", "java.util.concurrent", "vm.gc.containers"};
        String name = descr.getName();
        Class clz = null;
        for (String prefix : prefixes) {
            try {
                clz = Class.forName(prefix + "." + name);
            } catch (ClassNotFoundException ex) {
                // no such class, contiue
            }
        }
        Object container;
        try {
            container = clz.newInstance();
        } catch (Exception ex) {
            throw new TestBug("Not able to create a container", ex);
        }
        // Deques are also collections...
        if (container instanceof Map) {
            return new MapContainer((Map) container, maxMemory,
                    descr.getGarbageProducer() != null ? descr.getGarbageProducer() : garbageProducer,
                    descr.getMemoryStrategy() != null ? descr.getMemoryStrategy() : memoryStrategy,
                    descr.getSpeed());
        } else if (container instanceof Deque) {
            return new DequeueContainer((Deque) container, maxMemory,
                    descr.getGarbageProducer() != null ? descr.getGarbageProducer() : garbageProducer,
                    descr.getMemoryStrategy() != null ? descr.getMemoryStrategy() : memoryStrategy,
                    descr.getSpeed(), runParams.getNumberOfThreads());
        } else if (container instanceof Collection) {
            return new CollectionContainer((Collection) container, maxMemory,
                    descr.getGarbageProducer() != null ? descr.getGarbageProducer() : garbageProducer,
                    descr.getMemoryStrategy() != null ? descr.getMemoryStrategy() : memoryStrategy,
                    descr.getSpeed());
        }
        throw new TestBug("Not able to create a container");
    }

    void createEmptyContainers(long maxMemory) {
        containers = new ArrayList<Container>();
        for (int i = 0; containers.size() < runParams.getNumberOfThreads(); i++) {
            Container container = createContainter(maxMemory / runParams.getNumberOfThreads(),
                    descrs.get(i % descrs.size()));
            // we share container between workers if threadsCount > 1
            for (int cnt = descrs.get(i % descrs.size()).getThreadsCount();
                    cnt != 0 && containers.size() < runParams.getNumberOfThreads();
                    cnt--) {
                containers.add(container);
            }
        }
    }

    @Override
    public void run() {
        long maxMemory = runParams.getTestMemory();
        createEmptyContainers(maxMemory);
        super.run();
    }

    public static void main(String args[]) {
        GC.runTest(new ContainersTest(args), args);
    }
}
