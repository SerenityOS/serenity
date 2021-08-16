/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package org.netbeans.jemmy;

/**
 * Represents one timeout.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class Timeout extends Object {

    private String name;
    private long value;
    private long startTime;

    /**
     * Constructor.
     *
     * @param name Timeout name.
     * @param value Timeout value in milliseconds.
     */
    public Timeout(String name, long value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Returns timeout name.
     *
     * @return timeout name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns timeout value.
     *
     * @return timeout value.
     */
    public long getValue() {
        return value;
    }

    /**
     * Sleeps for timeout value.
     */
    public void sleep() {
        if (getValue() > 0) {
            try {
                Thread.sleep(getValue());
            } catch (InterruptedException e) {
                throw (new JemmyException("Sleep "
                        + getName()
                        + " was interrupted!",
                        e));
            }
        }
    }

    /**
     * Starts timeout measuring.
     */
    public void start() {
        startTime = System.currentTimeMillis();
    }

    /**
     * Checks if timeout has been expired after start() invocation.
     *
     * @return true if timeout has been expired.
     */
    public boolean expired() {
        return System.currentTimeMillis() - startTime > getValue();
    }

    /**
     * Throws a TimeoutExpiredException exception if timeout has been expired.
     *
     * @throws TimeoutExpiredException if timeout has been expired after start()
     * invocation.
     */
    public void check() {
        if (expired()) {
            throw (new TimeoutExpiredException(getName()
                    + " timeout expired!"));
        }
    }
}
