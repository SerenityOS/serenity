/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.management.jfr;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.jfr.internal.management.ManagementSupport;

/**
 * Management representation of a {@code Recording}.
 *
 * @see Recording
 *
 * @since 9
 */
public final class RecordingInfo {
    private final long id;
    private final String name;
    private final String state;
    private final boolean dumpOnExit;
    private final long size;
    private final boolean toDisk;
    private final long maxAge;
    private final long maxSize;
    private final long startTime;
    private final long stopTime;
    private final String destination;
    private final long durationInSeconds;
    private final Map<String, String> settings;

    // package private
    RecordingInfo(Recording recording) {
        id = recording.getId();
        name = recording.getName();
        state = recording.getState().toString();
        dumpOnExit = recording.getDumpOnExit();
        size = recording.getSize();
        toDisk = recording.isToDisk();

        Duration d = recording.getMaxAge();
        if (d == null) {
            maxAge = 0;
        } else {
            maxAge = d.getSeconds();
        }
        maxSize = recording.getMaxSize();
        Instant s = recording.getStartTime();
        startTime = s == null ? 0L : s.toEpochMilli();
        Instant st = recording.getStopTime();
        stopTime = st == null ? 0L : st.toEpochMilli();
        destination = ManagementSupport.getDestinationOriginalText(recording);
        Duration duration = recording.getDuration();
        durationInSeconds = duration == null ? 0 : duration.getSeconds();
        settings = recording.getSettings();
    }

    private RecordingInfo(CompositeData cd) {
        id = (long) cd.get("id");
        name = (String) cd.get("name");
        state = (String) cd.get("state");
        dumpOnExit = (boolean) cd.get("dumpOnExit");
        size = (long) cd.get("size");
        if(cd.containsKey("toDisk")){
            toDisk = (boolean) cd.get("toDisk");
        } else {
            // Before JDK-8219904 was fixed, the element name was disk, so for compatibility
            toDisk = (boolean) cd.get("disk");
        }
        maxAge = (Long) cd.get("maxAge");
        maxSize = (Long) cd.get("maxSize");
        startTime = (Long) cd.get("startTime");
        stopTime = (Long) cd.get("stopTime");
        destination = (String) cd.get("destination");
        durationInSeconds = (long) cd.get("duration");
        settings = new LinkedHashMap<>();
        Object map = cd.get("settings");
        if (map instanceof TabularData) {
            TabularData td = (TabularData) map;
            List<String> keyNames = td.getTabularType().getIndexNames();
            int size = keyNames.size();
            for (Object keys : td.keySet()) {
                Object[] keyValues = ((List<?>) keys).toArray();
                for (int i = 0; i < size; i++) {
                    String key = keyNames.get(i);
                    Object value = keyValues[i];
                    if (value instanceof String) {
                        settings.put(key, (String) value);
                    }
                }
            }
        }
    }

    /**
     * Returns the name of the recording associated with this
     * {@code RecordingInfo}.
     *
     * @return the recording name, not {@code null}
     *
     * @see Recording#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the unique ID for the recording associated with this
     * {@code RecordingInfo}.
     *
     * @return the recording ID
     *
     * @see Recording#getId()
     */
    public long getId() {
        return id;
    }

    /**
     * Returns if the recording associated with this {@code RecordingInfo}
     * should be dumped to file when the JVM exits.
     *
     * @return {@code true} if recording should be dumped on exit, {@code false}
     *         otherwise
     *
     * @see Recording#getDumpOnExit()
     */
    public boolean getDumpOnExit() {
        return dumpOnExit;
    }

    /**
     * Returns how many seconds data should be kept on disk, or {@code 0} if
     * data is to be kept forever.
     * <p>
     * In-memory recordings are not affected by maximum age.
     *
     * @see Recording#getMaxAge()
     * @see Recording#setToDisk(boolean)
     * @return how long data should be kept on disk, measured in seconds
     *
     */
    public long getMaxAge() {
        return maxAge;
    }

    /**
     * Returns the amount of data, measured in bytes, the recording associated
     * with this {@code RecordingInfo}, should be kept on disk, before it's
     * rotated away, or {@code 0} if data is to be kept indefinitely.
     * <p>
     * In-memory recordings are not affected by maximum size.
     *
     * @return the amount of data should be kept on disk, in bytes
     *
     * @see Recording#setToDisk(boolean)
     * @see Recording#getMaxSize()
     */
    public long getMaxSize() {
        return maxSize;
    }

    /**
     * Returns a {@code String} representation of state of the recording
     * associated with this {@code RecordingInfo}.
     * <p>
     * Valid return values are {@code "NEW"}, {@code "DELAYED"}, {@code "STARTING"},
     * {@code "RUNNING"}, {@code "STOPPING"}, {@code "STOPPED"} and {@code "CLOSED"}.
     *
     * @return the recording state, not {@code null}
     *
     * @see RecordingState#toString()
     * @see Recording#getState()
     */
    public String getState() {
        return state;
    }

    /**
     * Returns start time of the recording associated with this
     * {@code RecordingInfo}, measured as ms since epoch, or {@code null} if the
     * recording hasn't started.
     *
     * @return the start time of the recording, or {@code null} if the recording
     *         hasn't started
     *
     * @see Recording#getStartTime()
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Returns the actual or expected stop time of the recording associated with
     * this {@code RecordingInfo}, measured as ms since epoch, or {@code null}
     * if the expected or actual stop time is not known, which can only happen
     * if the recording has not yet been stopped.
     *
     * @return the stop time of recording, or {@code null} if recording hasn't
     *         been stopped.
     *
     * @see Recording#getStopTime()
     */
    public long getStopTime() {
        return stopTime;
    }

    /**
     * Returns the settings for the recording associated with this
     * {@code RecordingInfo}.
     *
     * @return the recording settings, not {@code null}
     *
     * @see Recording#getSettings()
     */
    public Map<String, String> getSettings() {
        return settings;
    }

    /**
     * Returns destination path where data, for the recording associated with
     * this {@link RecordingInfo}, should be written when the recording stops,
     * or {@code null} if the recording should not be written.
     *
     * @return the destination, or {@code null} if not set
     *
     * @see Recording#getDestination()
     */
    public String getDestination() {
        return destination;
    }

    /**
     * Returns a string description of the recording associated with this
     * {@code RecordingInfo}
     *
     * @return description, not {@code null}
     */
    @Override
    public String toString() {
        Stringifier s = new Stringifier();
        s.add("name", name);
        s.add("id", id);
        s.add("maxAge", maxAge);
        s.add("maxSize", maxSize);
        return s.toString();
    }

    /**
     * Returns the amount data recorded by recording. associated with this
     * {@link RecordingInfo}.
     *
     * @return the amount of recorded data, measured in bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * Returns {@code true} if the recording associated with this
     * {@code RecordingInfo} should be flushed to disk, when memory buffers are
     * full, {@code false} otherwise.
     *
     * @return {@code true} if recording is to disk, {@code false} otherwise
     */
    public boolean isToDisk() {
        return toDisk;
    }

    /**
     * Returns the desired duration, measured in seconds, of the recording
     * associated with this {@link RecordingInfo}, or {code 0} if no duration
     * has been set.
     *
     * @return the desired duration, or {code 0} if no duration has been set
     *
     * @see Recording#getDuration()
     */
    public long getDuration() {
        return durationInSeconds;
    }

    /**
     * Returns a {@code RecordingInfo} represented by the specified
     * {@code CompositeData} object.
     * <p>
     * The specified {@code CompositeData} must have the following item names and
     * item types to be valid. <blockquote>
     * <table class="striped">
     * <caption>Supported names and types in a specified {@code CompositeData} object</caption>
     * <thead>
     * <tr>
     * <th scope="col" style="text-align:left">Name</th>
     * <th scope="col" style="text-align:left">Type</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <th scope="row">id</th>
     * <td>{@code Long}</td>
     * </tr>
     * <tr>
     * <th scope="row">name</th>
     * <td>{@code String}</td>
     * </tr>
     * <tr>
     * <th scope="row">state</th>
     * <td>{@code String}</td>
     * </tr>
     * <tr>
     * <th scope="row">dumpOnExit</th>
     * <td>{@code Boolean}</td>
     * </tr>
     * <tr>
     * <th scope="row">size</th>
     * <td>{@code Long}</td>
     * </tr>
     * <tr>
     * <th scope="row">toDisk</th>
     * <td>{@code Boolean}</td>
     * </tr>
     * <tr>
     * <th scope="row">maxAge</th>
     * <td>{@code Long}</td>
     * </tr>
     * <tr>
     * <th scope="row">maxSize</th>
     * <td>{@code Long}</td>
     * </tr>
     * <tr>
     * <th scope="row">startTime</th>
     * <td>{@code Long}</td>
     * </tr>
     * <tr>
     * <th scope="row">stopTime</th>
     * <td>{@code Long}</td>
     * </tr>
     * <tr>
     * <th scope="row">destination</th>
     * <td>{@code String}</td>
     * </tr>
     * <tr>
     * <th scope="row">duration</th>
     * <td>{@code Long}</td>
     * </tr>
     * <tr>
     * <th scope="row">settings</th>
     * <td>{@code javax.management.openmbean.CompositeData[]} whose element type
     * is the mapped type for {@link SettingDescriptorInfo} as specified in the
     * {@link SettingDescriptorInfo#from} method.</td>
     * </tr>
     * </tbody>
     * </table>
     * </blockquote>
     *
     * @param cd {@code CompositeData} representing the {@code RecordingInfo} to
     *        return
     *
     * @throws IllegalArgumentException if {@code cd} does not represent a valid
     *         {@code RecordingInfo}
     *
     * @return the {@code RecordingInfo} represented by {@code cd}, or
     *         {@code null} if {@code cd} is {@code null}
     */
    public static RecordingInfo from(CompositeData cd) {
        if (cd == null) {
            return null;
        }
        return new RecordingInfo(cd);
    }
}
