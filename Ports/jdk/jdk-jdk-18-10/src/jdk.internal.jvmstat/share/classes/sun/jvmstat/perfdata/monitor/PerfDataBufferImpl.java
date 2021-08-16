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
import java.nio.*;
import java.io.*;
import java.net.*;
import java.util.regex.*;

/**
 * The base classes for the concrete implementations of the HotSpot
 * PerfData instrumentation buffer.
 *
 * @author Brian Doherty
 * @since 1.5
 * @see AbstractPerfDataBuffer
 */
public abstract class PerfDataBufferImpl {

    /**
     * The buffer containing the instrumentation data.
     */
    protected ByteBuffer buffer;

    /**
     * A Map of monitor objects found in the instrumentation buffer.
     */
    protected Map<String, Monitor> monitors;

    /**
     * The Local Java Virtual Machine Identifier for this buffer.
     */
    protected int lvmid;

    /**
     * A Map of monitor object names to aliases as read in from the alias map
     * file.
     */
    protected Map<String, ArrayList<String>> aliasMap;

    /**
     * A cache of resolved monitor aliases.
     */
    protected Map<String, Monitor> aliasCache;


    /**
     * Constructor.
     *
     * @param buffer the ByteBuffer containing the instrumentation data.
     * @param lvmid the Local Java Virtual Machine Identifier for this
     *              instrumentation buffer.
     */
    protected PerfDataBufferImpl(ByteBuffer buffer, int lvmid) {
        this.buffer = buffer;
        this.lvmid = lvmid;
        this.monitors = new TreeMap<>();
        this.aliasMap = new HashMap<>();
        this.aliasCache = new HashMap<>();
    }

    /**
     * Get the Local Java Virtual Machine Identifier, or <em>lvmid</em>
     * for the target JVM associated with this instrumentation buffer.
     *
     * @return int - the lvmid
     */
    public int getLocalVmId() {
        return lvmid;
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
        ByteBuffer bb = null;
        synchronized (this) {
            /*
             * this operation is potentially time consuming, and the result
             * is unused when the getBytes() interface is used. However, the
             * call is necessary in order to synchronize this monitoring
             * client with the target jvm, which assures that the receiver
             * of the byte[] gets an image that is initialized to a usable
             * state. Otherwise, they might only  get a snapshot of an
             * empty instrumentation buffer immediately after it was created.
             */
            try {
                if (monitors.isEmpty()) {
                    buildMonitorMap(monitors);
                }
            } catch (MonitorException e) {
                /*
                 * just ignore this here and let the receiver of the
                 * byte[] detect and handle the problem.
                 */
            }
            bb = buffer.duplicate();
        }
        bb.rewind();
        byte[] bytes = new byte[bb.limit()];
        bb.get(bytes);
        return bytes;
    }

    /**
     * Get the capacity of the instrumentation buffer.
     *
     * @return int - the capacity, or size, of the instrumentation buffer.
     */
    public int getCapacity() {
        return buffer.capacity();
    }

    /**
     * Get the ByteBuffer containing the instrumentation data.
     *
     * @return ByteBuffer - a ByteBuffer object that refers to the
     *                      instrumentation data.
     */
    ByteBuffer getByteBuffer() {
        // receiver is responsible for assuring that the buffer's state
        // is that of an initialized target.
        return buffer;
    }

    /**
     * Build the alias mapping. Uses the default alias map file unless
     * the sun.jvmstat.perfdata.aliasmap file indicates some other
     * file as the source.
     */
    @SuppressWarnings("deprecation")
    private void buildAliasMap() {
        assert Thread.holdsLock(this);

        URL aliasURL = null;
        String filename = System.getProperty("sun.jvmstat.perfdata.aliasmap");

        if (filename != null) {
            File f = new File(filename);
            try {
                aliasURL = f.toURL();

            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            aliasURL = getClass().getResource(
                "/sun/jvmstat/perfdata/resources/aliasmap");
        }

        assert aliasURL != null;

        AliasFileParser aliasParser = new AliasFileParser(aliasURL);

        try {
            aliasParser.parse(aliasMap);

        } catch (IOException e) {
            System.err.println("Error processing " + filename + ": "
                               + e.getMessage());
        } catch (SyntaxException e) {
            System.err.println("Syntax error parsing " + filename + ": "
                               + e.getMessage());
        }
    }

    /**
     * Find the Monitor object for the named counter by using one of its
     * aliases.
     */
    protected Monitor findByAlias(String name) {
        assert Thread.holdsLock(this);

        Monitor  m = aliasCache.get(name);
        if (m == null) {
            ArrayList<String> al = aliasMap.get(name);
            if (al != null) {
                for (Iterator<String> i = al.iterator(); i.hasNext() && m == null; ) {
                    String alias = i.next();
                    m = monitors.get(alias);
                }
            }
        }
        return m;
    }


    /**
     * Find a named Instrumentation object.
     *
     * This method will look for the named instrumentation object in the
     * instrumentation exported by this Java Virtual Machine. If an
     * instrumentation object with the given name exists, a Monitor interface
     * to that object will be return. Otherwise, the method returns
     * {@code null}. The method will map requests for instrumention objects
     * using old names to their current names, if applicable.
     *
     *
     *
     * @param name the name of the Instrumentation object to find.
     * @return Monitor - the {@link Monitor} object that can be used to
     *                   monitor the named instrumentation object, or
     *                   {@code null} if the named object doesn't exist.
     * @throws MonitorException Thrown if an error occurs while communicating
     *                          with the target Java Virtual Machine.
     */
    public Monitor findByName(String name) throws MonitorException {
        Monitor m = null;

        synchronized (this) {
            if (monitors.isEmpty()) {
                buildMonitorMap(monitors);
                buildAliasMap();
            }

            // look for the requested monitor
            m = monitors.get(name);
            if (m == null) {
                // not found - load any new monitors, and try again.
                getNewMonitors(monitors);
                m = monitors.get(name);
            }
            if (m == null) {
                // still not found, look for aliases
                m = findByAlias(name);
            }
        }
        return m;
    }

    /**
     * Find all Instrumentation objects with names matching the given pattern.
     *
     * This method returns a {@link List} of Monitor objects such that
     * the name of each object matches the given pattern.
     *
     * @param patternString a string containing a pattern as described in
     *                      {@link java.util.regex.Pattern}.
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
    public List<Monitor> findByPattern(String patternString)
                throws MonitorException, PatternSyntaxException {

        synchronized(this) {
            if (monitors.isEmpty()) {
                buildMonitorMap(monitors);
            } else {
                getNewMonitors(monitors);
            }
        }

        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher("");
        List<Monitor> matches = new ArrayList<>();

        Set<Map.Entry<String,Monitor>> monitorSet = monitors.entrySet();

        for (Iterator<Map.Entry<String, Monitor>> i = monitorSet.iterator(); i.hasNext(); /* empty */) {
            Map.Entry<String, Monitor> me = i.next();
            String name = me.getKey();
            Monitor m = me.getValue();

            // apply pattern to monitor item name
            matcher.reset(name);

            // if the pattern matches, then add monitor to list
            if (matcher.lookingAt()) {
                 matches.add(me.getValue());
            }
        }
        return matches;
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
        synchronized(this) {
            if (monitors.isEmpty()) {
                buildMonitorMap(monitors);
            }
            return getMonitorStatus(monitors);
        }
    }

    // PerfDataBuffer implementation specific classes

    /**
     * get the list of inserted and removed monitors since last called.
     *
     * @param m the map of Monitors.
     * @throws MonitorException Thrown if communications errors occur
     *                          while communicating with the target.
     */
    protected abstract MonitorStatus getMonitorStatus(Map<String, Monitor> m)
                                     throws MonitorException;

    /**
     * build the map of Monitor objects.
     *
     * @param m the map of Monitors.
     * @throws MonitorException Thrown if communications errors occur
     *                          while communicating with the target.
     */
    protected abstract void buildMonitorMap(Map<String, Monitor> m) throws MonitorException;

    /**
     * get the new Monitor objects from the Map of Monitor objects.
     *
     * @param m the map of Monitors.
     * @throws MonitorException Thrown if communications errors occur
     *                          while communicating with the target.
     */
    protected abstract void getNewMonitors(Map<String, Monitor> m) throws MonitorException;
}
