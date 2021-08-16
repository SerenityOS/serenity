/*
 * Copyright (c) 2004, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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

package sun.jvmstat.perfdata.monitor;

import sun.jvmstat.monitor.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.ByteBuffer;

/**
 * Abstraction for the HotSpot PerfData instrumentation buffer. This class
 * is responsible for acquiring access to the instrumentation buffer for
 * a target HotSpot Java Virtual Machine and providing method level access
 * to its contents.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public abstract class AbstractPerfDataBuffer {

    /**
     * Reference to the concrete instance created by the
     * {@link #createPerfDataBuffer} method.
     */
    protected PerfDataBufferImpl impl;

    /**
     * Get the Local Java Virtual Machine Identifier, or <em>lvmid</em>
     * for the target JVM associated with this instrumentation buffer.
     *
     * @return int - the lvmid
     */
    public int getLocalVmId() {
        return impl.getLocalVmId();
    }

    /**
     * Get a copy of the raw instrumentation data.
     * This method is used to get a copy of the current bytes in the
     * instrumentation buffer. It is generally used for transporting
     * those bytes over the network.
     *
     * @return byte[] - a copy of the bytes in the instrumentation buffer.
     */
    public byte[] getBytes() {
        return impl.getBytes();
    }

    /**
     * Get the capacity of the instrumentation buffer.
     *
     * @return int - the capacity, or size, of the instrumentation buffer.
     */
    public int getCapacity() {
        return impl.getCapacity();
    }

    /**
     * Find a named Instrumentation object.
     *
     * This method will look for the named instrumentation object in the
     * instrumentation exported by this Java Virtual Machine. If an
     * instrumentation object with the given name exists, a Monitor interface
     * to that object will be return. Otherwise, the method returns
     * {@code null}.
     *
     * @param name the name of the Instrumentation object to find.
     * @return Monitor - the {@link Monitor} object that can be used to
     *                   monitor the named instrumentation object, or
     *                   {@code null} if the named object doesn't exist.
     * @throws MonitorException Thrown if an error occurs while communicating
     *                          with the target Java Virtual Machine.
     */
    public Monitor findByName(String name) throws MonitorException {
        return impl.findByName(name);
    }

    /**
     * Find all Instrumentation objects with names matching the given pattern.
     *
     * This method returns a {@link List} of Monitor objects such that
     * the name of each object matches the given pattern.
     *
     * @param patternString  a string containing a pattern as described in
     *                       {@link java.util.regex.Pattern}.
     * @return {@code List<Monitor>} - a List of {@link Monitor}
     *                objects that can be used to
     *                monitor the instrumentation objects whose names match
     *                the given pattern. If no instrumentation objects have`
     *                names matching the given pattern, then an empty List
     *                is returned.
     * @throws MonitorException Thrown if an error occurs while communicating
     *                          with the target Java Virtual Machine.
     * @see java.util.regex.Pattern
     */
    public List<Monitor> findByPattern(String patternString) throws MonitorException {
        return impl.findByPattern(patternString);
    }

    /**
     * Get a list of the inserted and removed monitors since last called.
     *
     * @return MonitorStatus - the status of available Monitors for the
     *                         target Java Virtual Machine.
     * @throws MonitorException Thrown if communications errors occur
     *                          while communicating with the target.
     */
    public MonitorStatus getMonitorStatus() throws MonitorException {
        return impl.getMonitorStatus();
    }

    /**
     * Get the ByteBuffer containing the instrumentation data.
     *
     * @return ByteBuffer - a ByteBuffer object that refers to the
     *                      instrumentation data.
     */
    public ByteBuffer getByteBuffer() {
        return impl.getByteBuffer();
    }

    /**
     * Create the perfdata instrumentation buffer for the given lvmid
     * using the given ByteBuffer object as the source of the instrumentation
     * data. This method parses the instrumentation buffer header to determine
     * key characteristics of the instrumentation buffer and then dynamically
     * loads the appropriate class to handle the particular instrumentation
     * version.
     *
     * @param bb the ByteBuffer that references the instrumentation data.
     * @param lvmid the Local Java Virtual Machine identifier for this
     *              instrumentation buffer.
     *
     * @throws MonitorException
     */
    protected void createPerfDataBuffer(ByteBuffer bb, int lvmid)
                   throws MonitorException {
        int majorVersion = AbstractPerfDataBufferPrologue.getMajorVersion(bb);
        int minorVersion = AbstractPerfDataBufferPrologue.getMinorVersion(bb);

        // instantiate the version specific class
        String classname = "sun.jvmstat.perfdata.monitor.v"
                           + majorVersion + "_" + minorVersion
                           + ".PerfDataBuffer";

        try {
            Class<?> implClass = Class.forName(classname);
            Constructor<?> cons = implClass.getConstructor(new Class<?>[] {
                    Class.forName("java.nio.ByteBuffer"),
                    Integer.TYPE
            });

            impl = (PerfDataBufferImpl)cons.newInstance(new Object[] {
                     bb, lvmid
            });

        } catch (ClassNotFoundException e) {
            // from Class.forName();
            throw new IllegalArgumentException(
                    "Could not find " + classname + ": " + e.getMessage(), e);

        } catch (NoSuchMethodException e) {
            // from Class.getConstructor();
            throw new IllegalArgumentException(
                    "Expected constructor missing in " + classname + ": "
                    + e.getMessage(), e);

        } catch (IllegalAccessException e) {
            // from Constructor.newInstance()
            throw new IllegalArgumentException(
                   "Unexpected constructor access in " + classname + ": "
                   + e.getMessage(), e);

        } catch (InstantiationException e) {
            throw new IllegalArgumentException(
                    classname + "is abstract: " + e.getMessage(), e);

        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof MonitorException) {
                throw (MonitorException)cause;
            }
            throw new RuntimeException("Unexpected exception: "
                                       + e.getMessage() , e);
        }
    }
}
