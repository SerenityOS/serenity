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

import nsk.share.TestBug;
import nsk.share.gc.gp.GarbageProducer;
import nsk.share.gc.gp.GarbageUtils;
import nsk.share.gc.gp.MemoryStrategy;

/**
 *  This is a description which is parsed from command-line string.
 *  It contains the container type, garbage producer, memory strategy,
 *  speed of objects renovations and count of threads to work with
 * container. The conatainer should be MT safe if threadsCount more then 1.
 */
public class ContainerDescription {

    /*
     * The container description is next:
     * containerName(garbageProucer,numberOfThreads,speed,MemoryStrategy)
     * all parameters except containerName are optional
     * containerName is the Name of class from java.util or java.util.concurrent
     */
    static ContainerDescription parseFromString(String string) {
        int params = string.indexOf('(');
        String gp;
        Speed speed = Speed.MEDIUM;
        MemoryStrategy ms = null;
        int count = 1;

        if (params == -1) {
            ContainerDescription descr = new ContainerDescription(string);
            descr.setSpeed(speed);
            descr.setThreadsCount(count);
            return descr;
        }
        if (!string.endsWith(")")) {
            throw new TestBug("Incorrect syntax of container description.");
        }
        String name = string.substring(0, string.indexOf('('));
        String parameters = string.substring(string.indexOf("(") + 1, string.length() - 1);
        final int closedBracket = parameters.lastIndexOf(')');
        int del = parameters.indexOf(',', closedBracket + 1);
        if (del == -1) {
            gp = parameters;
        } else {
            gp = parameters.substring(0, del).trim();
            String[] other = parameters.substring(del + 1).split(",");
            switch (other.length) {
                case 3:
                    ms = MemoryStrategy.fromString(other[2].trim());
                case 2:
                    speed = Speed.fromString(other[1].trim());
                case 1:
                    count = Integer.parseInt(other[0].trim());
                    break;
                default:
                    throw new TestBug("Unexpected size of parameters: " + other.length + 1);
            }
        }
        ContainerDescription descr = new ContainerDescription(name);
        descr.setGarbageProducer(GarbageUtils.getGarbageProducer(gp));
        descr.setSpeed(speed);
        descr.setThreadsCount(count);
        descr.setMemoryStrategy(ms);
        return descr;
    }

    protected ContainerDescription(String name) {
        this.name = name;
    }
    protected GarbageProducer garbageProducer;

    /**
     * Get the value of garbageProducer
     *
     * @return the value of garbageProducer
     */
    public GarbageProducer getGarbageProducer() {
        return garbageProducer;
    }

    /**
     * Set the value of garbageProducer
     *
     * @param garbageProducer new value of garbageProducer
     */
    public void setGarbageProducer(GarbageProducer garbageProducer) {
        this.garbageProducer = garbageProducer;
    }
    protected MemoryStrategy memoryStrategy;

    /**
     * Get the value of memoryStrategy
     *
     * @return the value of memoryStrategy
     */
    public MemoryStrategy getMemoryStrategy() {
        return memoryStrategy;
    }

    /**
     * Set the value of memoryStrategy
     *
     * @param memoryStrategy new value of memoryStrategy
     */
    public void setMemoryStrategy(MemoryStrategy memoryStrategy) {
        this.memoryStrategy = memoryStrategy;
    }
    protected String name;

    /**
     * Get the value of Type
     *
     * @return the value of Type
     */
    public String getName() {
        return name;
    }
    protected Speed speed = Speed.MEDIUM;

    /**
     * Get the value of speed
     *
     * @return the value of speed
     */
    public Speed getSpeed() {
        return speed;
    }

    /**
     * Set the value of speed
     *
     * @param speed new value of speed
     */
    public void setSpeed(Speed speed) {
        this.speed = speed;
    }
    protected int threadsCount;

    /**
     * Get the value of threadsCount
     *
     * @return the value of threadsCount
     */
    public int getThreadsCount() {
        return threadsCount;
    }

    /**
     * Set the value of threadsCount
     *
     * @param threadsCount new value of threadsCount
     */
    public void setThreadsCount(int threadsCount) {
        this.threadsCount = threadsCount;
    }
}
