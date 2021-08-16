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

import java.io.IOException;
import java.lang.management.PlatformManagedObject;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import jdk.jfr.Configuration;
import jdk.jfr.EventType;
import jdk.jfr.Recording;

/**
 * Management interface for controlling Flight Recorder.
 * <p>
 * The object name for identifying the MXBean in the platform MBean
 * server is: <blockquote> {@code jdk.management.jfr:type=FlightRecorder} </blockquote>
 * <p>
 * Flight Recorder can be configured in the following ways:
 * <ul>
 * <li><b>Recording options</b><br>
 * Specify how long a recording should last, and where and when data
 * should be dumped.</li>
 * <li><b>Settings</b><br>
 * Specify which events should be enabled and what kind information each
 * event should capture.</li>
 * <li><b>Configurations</b><br>
 * Predefined sets of settings, typically derived from a settings file,
 * that specify the configuration of multiple events simultaneously.</li>
 * </ul>
 * <p>
 * See the package {@code jdk.jfr} documentation for descriptions of the settings
 * syntax and the {@link ConfigurationInfo} class documentation for configuration information.
 *
 * <h2>Recording options</h2>
 * <p>
 * The following table shows the options names to use with {@link #setRecordingOptions(long, Map)}
 * and {@link #getRecordingOptions(long)}.
 *
 * <table class="striped">
 * <caption>Recording options</caption>
 * <thead>
 * <tr>
 * <th scope="col">Name</th>
 * <th scope="col">Descripion</th>
 * <th scope="col">Default value</th>
 * <th scope="col">Format</th>
 * <th scope="col">Example values</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <th scope="row">{@code name}</th>
 * <td>Sets a human-readable recording name</td>
 * <td>String representation of the recording id</td>
 * <td>{@code String}</td>
 * <td>{@code "My Recording"}, <br>
 * {@code "profiling"}</td>
 * </tr>
 * <tr>
 * <th scope="row">{@code maxAge}</th>
 * <td>Specify the length of time that the data is kept in the disk repository until the
 * oldest data may be deleted. Only works if {@code disk=true}, otherwise
 * this parameter is ignored.</td>
 * <td>{@code "0"} (no limit)</td>
 * <td>{@code "0"} if no limit is imposed, otherwise a string
 * representation of a positive {@code Long} value followed by an empty space
 * and one of the following units,<br>
 * <br>
 * {@code "ns"} (nanoseconds)<br>
 * {@code "us"} (microseconds)<br>
 * {@code "ms"} (milliseconds)<br>
 * {@code "s"} (seconds)<br>
 * {@code "m"} (minutes)<br>
 * {@code "h"} (hours)<br>
 * {@code "d"} (days)<br>
 * </td>
 * <td>{@code "2 h"},<br>
 * {@code "24 h"},<br>
 * {@code "2 d"},<br>
 * {@code "0"}</td>
 * </tr>
 * <tr>
 * <th scope="row">{@code maxSize}</th>
 * <td>Specifies the size, measured in bytes, at which data is kept in disk
 * repository. Only works if
 * {@code disk=true}, otherwise this parameter is ignored.</td>
 * <td>{@code "0"} (no limit)</td>
 * <td>String representation of a {@code Long} value, must be positive</td>
 * <td>{@code "0"}, <br>
 * {@code "1000000000"}</td>
 * </tr>
 * <tr>
 * <th scope="row">{@code dumpOnExit}</th>
 * <td>Dumps recording data to disk on Java Virtual Machine (JVM) exit</td>
 * <td>{@code "false"}</td>
 * <td>String representation of a {@code Boolean} value, {@code "true"} or
 * {@code "false"}</td>
 * <td>{@code "true"},<br>
 * {@code "false"}</td>
 * </tr>
 * <tr>
 * <th scope="row">{@code destination}</th>
 * <td>Specifies the path where recording data is written when the recording stops.</td>
 * <td>{@code "false"}</td>
 * <td>See {@code Paths#getPath} for format. <br>
 * If this method is invoked from another process, the data is written on the
 * machine where the target JVM is running. If destination is a relative path, it
 * is relative to the working directory where the target JVM was started.}</td>
 * <td>{@code "c:\recording\recotding.jfr"},<br>
 * {@code "/recordings/recording.jfr"}, {@code "recording.jfr"}</td>
 * </tr>
 * <tr>
 * <th scope="row">{@code disk}</th>
 * <td>Stores recorded data as it is recorded</td>
 * <td><code>"false"</code></td>
 * <td>String representation of a {@code Boolean} value, {@code "true"} or
 * {@code "false"}</td>
 * <td>{@code "true"},<br>
 * {@code "false"}</td>
 * <tr>
 * <th scope="row">{@code duration}</th>
 * <td>Sets how long the recording should be running</td>
 * <td>{@code "0"} (no limit, continuous)</td>
 * <td>{@code "0"} if no limit should be imposed, otherwise a string
 * representation of a positive {@code Long} followed by an empty space and one
 * of the following units:<br>
 * <br>
 * {@code "ns"} (nanoseconds)<br>
 * {@code "us"} (microseconds)<br>
 * {@code "ms"} (milliseconds)<br>
 * {@code "s"} (seconds)<br>
 * {@code "m"} (minutes)<br>
 * {@code "h"} (hours)<br>
 * {@code "d"} (days)<br>
 * </td>
 * <td>{@code "60 s"},<br>
 * {@code "10 m"},<br>
 * {@code "4 h"},<br>
 * {@code "0"}</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @since 9
 */
public interface FlightRecorderMXBean extends PlatformManagedObject {
    /**
     * String representation of the {@code ObjectName} for the
     * {@code FlightRecorderMXBean}.
     */
    public static final String MXBEAN_NAME = "jdk.management.jfr:type=FlightRecorder";

    /**
     * Creates a recording, but doesn't start it.
     *
     * @return a unique ID that can be used to start, stop, close and
     *         configure the recording
     *
     * @throws IllegalStateException if Flight Recorder can't be created (for
     *         example, if the Java Virtual Machine (JVM) lacks Flight Recorder
     *         support, or if the file repository can't be created or accessed)
     *
     * @throws java.lang.SecurityException if a security manager exists and the
     *         caller does not have {@code ManagementPermission("control")}
     *
     * @see Recording
     */
    long newRecording() throws IllegalStateException, SecurityException;

    /**
     * Creates a snapshot recording of all available recorded data.
     * <p>
     * A snapshot is a synthesized recording in a stopped state. If no data is
     * available, a recording with size {@code 0} is returned.
     * <p>
     * A snapshot provides stable access to data for later operations (for example,
     * operations to change the time interval or to reduce the data size).
     * <p>
     * The caller must close the recording when access to the data is no longer
     * needed.
     *
     * @return a unique ID that can be used for reading recording data
     *
     * @throws java.lang.SecurityException if a security manager exists and the
     *         caller does not have {@code ManagementPermission("control")}
     *
     * @see Recording
     */
    public long takeSnapshot();

    /**
     * Creates a copy of an existing recording, useful for extracting parts of a
     * recording.
     * <p>
     * The cloned recording contains the same recording data as the
     * original, but it has a new ID and a name prefixed with
     * {@code "Clone of recording"}. If the original recording is running, then
     * the clone is also running.
     *
     * @param recordingId the recording ID of the recording to create a clone
     *        from
     *
     * @param stop if the newly created clone is stopped before
     *        returning.
     *
     * @return a unique ID that can be used to start, stop,
     *         close and configure the recording
     *
     * @throws IllegalArgumentException if a recording with the specified ID
     *         doesn't exist
     *
     * @throws java.lang.SecurityException if a security manager exists and the
     *         caller does not have {@code ManagementPermission("control")}
     *
     * @see Recording
     */
    long cloneRecording(long recordingId, boolean stop) throws IllegalArgumentException, SecurityException;

    /**
     * Starts the recording with the specified ID.
     * <p>
     * A recording that is stopped can't be restarted.
     *
     * @param recordingId ID of the recording to start
     *
     * @throws IllegalArgumentException if a recording with the specified ID
     *         doesn't exist
     *
     * @throws java.lang.SecurityException if a security manager exists and the
     *         caller does not have {@code ManagementPermission("control")}
     *
     * @see Recording
     */
    void startRecording(long recordingId) throws IllegalStateException, SecurityException;

    /**
     * Stops the running recording with the specified ID.
     *
     * @param recordingId the ID of the recording to stop
     *
     * @return {@code true} if the recording is stopped, {@code false}
     *         otherwise
     *
     * @throws IllegalArgumentException if a recording with the specified ID
     *         doesn't exist
     * @throws IllegalStateException if the recording is not running
     * @throws java.lang.SecurityException if a security manager exists and the
     *         caller does not have {@code ManagementPermission("control")}
     *
     * @see #newRecording()
     */
    boolean stopRecording(long recordingId) throws IllegalArgumentException, IllegalStateException, SecurityException;

    /**
     * Closes the recording with the specified ID and releases any system
     * resources that are associated with the recording.
     * <p>
     * If the recording is already closed, invoking this method has no effect.
     *
     * @param recordingId the ID of the recording to close
     *
     * @throws IllegalArgumentException if a recording with the specified ID
     *         doesn't exist
     * @throws IOException if an I/O error occurs
     * @throws java.lang.SecurityException if a security manager exists and the
     *         caller does not have {@code ManagementPermission("control")}
     *
     * @see #newRecording()
     */
    void closeRecording(long recordingId) throws IOException;

    /**
     * Opens a data stream for the recording with the specified ID, or {@code 0}
     * to get data irrespective of recording.
     * <table class="striped">
     * <caption>Recording stream options</caption>
     * <thead>
     * <tr>
     * <th scope="col">Name</th>
     * <th scope="col">Descripion</th>
     * <th scope="col">Default value</th>
     * <th scope="col">Format</th>
     * <th scope="col">Example values</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     * <th scope="row">{@code startTime}</th>
     * <td>Specifies the point in time to start a recording stream. Due to
     * how data is stored, some events that start or end prior to the
     * start time may be included.</td>
     * <td>{@code Instant.MIN_VALUE.toString()}</td>
     * <td>ISO-8601. See {@link Instant#toString}<br>
     * or milliseconds since epoch</td>
     * <td>{@code "2015-11-03T00:00"},<br>
     * {@code "1446508800000"}</td>
     * </tr>
     * <tr>
     * <th scope="row">{@code endTime}</th>
     * <td>Specifies the point in time to end a recording stream. Due to how
     * data is stored, some events that start or end after the end time may
     * be included.</td>
     * <td>{@code Instant.MAX_VALUE.toString()}</td>
     * <td>ISO-8601. See {@link Instant#toString} <br>
     * or milliseconds since epoch</td>
     * <td>{@code "2015-11-03T01:00"}, <br>
     * {@code "1446512400000"}</td>
     * </tr>
     *
     * <tr>
     * <th scope="row">{@code blockSize}</th>
     * <td>Specifies the maximum number of bytes to read with a call to {@code readStream}
     * </td>
     * <td>{@code "50000"}</td>
     * <td>A positive {@code long} value. <br>
     * <br>
     * Setting {@code blockSize} to a very high value may result in
     * {@link OutOfMemoryError} or an {@link IllegalArgumentException}, if the
     * Java Virtual Machine (JVM) deems the value too large to handle.</td>
     * <td>{@code "50000"},<br>
     * {@code "1000000"},<br>
     * </tr>
     * <tr>
     * <th scope="row">{@code streamVersion}</th>
     * <td>Specifies format to use when reading data from a running recording
     * </td>
     * <td>{@code "1.0"}</td>
     * <td>A version number with a major and minor.<br>
     * <br>
     * To be able to read from a running recording the value must be set</td>
     * <td>{@code "1.0"}
     * </tr>
     * </tbody>
     * </table>
     * If an option is omitted from the map the default value is used.
     * <p>
     * The recording with the specified ID must be stopped before a stream can
     * be opened, unless the option {@code "streamVersion"} is specified.
     *
     * @param recordingId ID of the recording to open the stream for
     *
     * @param streamOptions a map that contains the options that controls the amount of data
     *        and how it is read, or {@code null} to get all data for the
     *        recording with the default block size
     *
     * @return a unique ID for the stream.
     *
     * @throws IllegalArgumentException if a recording with the iD doesn't
     *         exist, or if {@code options} contains invalid values
     *
     * @throws IOException if the recording is closed, an I/O error occurs, or
     *         no data is available for the specified recording or
     *         interval
     *
     * @throws java.lang.SecurityException if a security manager exists and the
     *         caller does not have {@code ManagementPermission("control")}
     */
    long openStream(long recordingId, Map<String, String> streamOptions) throws IOException;

    /**
     * Closes the recording stream with the specified ID and releases any system
     * resources that are associated with the stream.
     * <p>
     * If the stream is already closed, invoking this method has no effect.
     *
     * @param streamId the ID of the stream
     *
     * @throws IllegalArgumentException if a stream with the specified ID doesn't
     *         exist
     * @throws IOException if an I/O error occurs while trying to close the stream
     * @throws java.lang.SecurityException if a security manager exists and the
     *         caller does not have {@code ManagementPermission("control")}
     *
     * @see #openStream(long, Map)
     */
    void closeStream(long streamId) throws IOException;

    /**
     * Reads a portion of data from the stream with the specified ID, or returns
     * {@code null} if no more data is available.
     * <p>
     * To read all data for a recording, invoke this method repeatedly until
     * {@code null} is returned.
     *
     * @param streamId the ID of the stream
     *
     * @return byte array that contains recording data, or {@code null} when no more
     *         data is available
     * @throws IOException if the stream is closed, or an I/O error occurred while
     *         trying to read the stream
     * @throws IllegalArgumentException if no recording with the stream ID exists
     * @throws java.lang.SecurityException if a security manager exists and the
     *         caller does not have {@code ManagementPermission("monitor")}
     */
    byte[] readStream(long streamId) throws IOException;

    /**
     * Returns a map that contains the options for the recording with the
     * specified ID (for example, the destination file or time span to keep
     * recorded data).
     * <p>
     * See {@link FlightRecorderMXBean} for available option names.
     *
     * @param recordingId the ID of the recording to get options for
     *
     * @return a map describing the recording options, not {@code null}
     *
     * @throws IllegalArgumentException if no recording with the
     *         specified ID exists
     * @throws java.lang.SecurityException if a security manager exists and the
     *         caller does not have {@code ManagementPermission("monitor")}
     *
     */
    Map<String, String> getRecordingOptions(long recordingId) throws IllegalArgumentException;

    /**
     * Returns a {@code Map} that contains the settings for the recording with the specified ID,
     * (for example, the event thresholds)
     * <p>
     * If multiple recordings are running at the same time, more data could be
     * recorded than what is specified in the {@code Map} object.
     * <p>
     * The name in the {@code Map} is the event name and the setting name separated by
     * {@code "#"} (for example, {@code "jdk.VMInfo#period"}). The value
     * is a textual representation of the settings value (for example,
     * {@code "60 s"}).
     *
     * @param recordingId the ID of the recordings to get settings for
     *
     * @return a map that describes the recording settings, not {@code null}
     *
     * @throws IllegalArgumentException if no recording with the specified ID exists
     * @throws java.lang.SecurityException if a security manager exists and the
     *         caller does not have {@code ManagementPermission("monitor")}
     */
    Map<String, String> getRecordingSettings(long recordingId) throws IllegalArgumentException;

    /**
     * Sets a configuration as a string representation for the recording with the
     * specified ID.
     *
     * @param recordingId ID of the recording
     * @param contents a string representation of the configuration file to use,
     *        not {@code null}
     * @throws IllegalArgumentException if no recording with the
     *         specified ID exists or if the configuration could not be parsed.
     * @throws java.lang.SecurityException if a security manager exists and the
     *         caller does not have {@code ManagementPermission("control")}
     *
     * @see Configuration#getContents()
     */
    void setConfiguration(long recordingId, String contents) throws IllegalArgumentException;

    /**
     * Sets a predefined configuration for the recording with the specified ID.
     *
     * @param recordingId ID of the recording to set the configuration for
     * @param configurationName the name of the configuration (for example,
     *        {@code "profile"} or {@code "default"}), not {@code null}
     * @throws IllegalArgumentException if no recording with the
     *         specified ID exists
     * @throws java.lang.SecurityException if a security manager exists and the
     *         caller does not have {@code ManagementPermission("control")}
     *
     * @see #getConfigurations()
     */
    void setPredefinedConfiguration(long recordingId, String configurationName) throws IllegalArgumentException;

    /**
     * Sets and replaces all previous settings for the specified recording.
     * <p>
     * A setting consists of a name/value pair, where <em>name</em> specifies the
     * event and setting to configure, and the <em>value</em> specifies what to set
     * it to.
     * <p>
     * The name can be formed in the following ways:
     * <p>
     * {@code
     *   <event-name> + "#" + <setting-name>
     * }
     * <p>
     * or
     * <p>
     * {@code
     *   <event-id> + "#" + <setting-name>
     * }
     * <p>
     * For example, to set the sample interval of the CPU Load event to once every
     * second, use the name {@code "jdk.CPULoad#period"} and the value
     * {@code "1 s"}. If multiple events use the same name, for example if an event
     * class is loaded in multiple class loaders, and differentiation is needed
     * between them, then the name is {@code "56#period"}. The ID for an event is
     * obtained by invoking {@link jdk.jfr.EventType#getId()} method and is valid
     * for the Java Virtual Machine (JVM) instance that the event is registered in.
     * <p>
     * A list of available event names is retrieved by invoking
     * {@link jdk.jfr.FlightRecorder#getEventTypes()} and
     * {@link jdk.jfr.EventType#getName()}. A list of available settings for an
     * event type is obtained by invoking
     * {@link jdk.jfr.EventType#getSettingDescriptors()} and
     * {@link jdk.jfr.ValueDescriptor#getName()}.
     *
     * @param recordingId ID of the recording
     *
     * @param settings name value map of the settings to set, not {@code null}
     *
     * @throws IllegalArgumentException if no recording with the specified ID exists
     * @throws java.lang.SecurityException if a security manager exists and the
     *         caller does not have {@code ManagementPermission("control")}
     *
     * @see Recording#getId()
     */
    void setRecordingSettings(long recordingId, Map<String, String> settings) throws IllegalArgumentException;

    /**
     * Configures the recording options (for example, destination file and time span
     * to keep data).
     * <p>
     * See {@link FlightRecorderMXBean} for a description of the options and values
     * that can be used. Setting a value to {@code null} restores the value to the
     * default value.
     *
     * @param recordingId the ID of the recording to set options for
     *
     * @param options name/value map of the settings to set, not {@code null}
     *
     * @throws IllegalArgumentException if no recording with the specified ID exists
     * @throws java.lang.SecurityException if a security manager exists, and the
     *         caller does not have {@code ManagementPermission("control")} or an
     *         option contains a file that the caller does not have permission to
     *         operate on.
     * @see Recording#getId()
     */
    void setRecordingOptions(long recordingId, Map<String, String> options) throws IllegalArgumentException;

    /**
     * Returns the list of the available recordings, not necessarily running.
     * <p>
     * <b>MBeanServer access</b>:<br>
     * The mapped type of {@code RecordingInfo} is {@code CompositeData} with
     * attributes as specified in the {@link RecordingInfo#from
     * RecordingInfo.from} method.
     *
     * @return list of recordings, not {@code null}
     *
     * @throws java.lang.SecurityException if a security manager exists and the
     *         caller does not have {@code  ManagementPermission("monitor")}
     *
     * @see RecordingInfo
     * @see Recording
     */
    List<RecordingInfo> getRecordings();

    /**
     * Returns the list of predefined configurations for this Java Virtual Machine (JVM).
     * <p>
     * <b>MBeanServer access</b>:<br>
     * The mapped type of {@code ConfigurationInfo} is {@code CompositeData}
     * with attributes as specified in the {@link ConfigurationInfo#from
     * ConfigurationInfo.from} method.
     *
     * @return the list of predefined configurations, not {@code null}
     *
     * @throws java.lang.SecurityException if a security manager exists and the
     *         caller does not have {@code ManagementPermission("monitor")}
     *
     * @see ConfigurationInfo
     * @see Configuration
     */
    List<ConfigurationInfo> getConfigurations();

    /**
     * Returns the list of currently registered event types.
     * <p>
     * <b>MBeanServer access</b>:<br>
     * The mapped type of {@code EventTypeInfo} is {@code CompositeData} with
     * attributes as specified in the {@link EventTypeInfo#from
     * EventTypeInfo.from} method.
     *
     * @return the list of registered event types, not {@code null}
     *
     * @throws java.lang.SecurityException if a security manager exists and the
     *         caller does not have {@code ManagementPermission("monitor")}
     *
     * @see EventTypeInfo
     * @see EventType
     */
    List<EventTypeInfo> getEventTypes();

    /**
     * Writes recording data to the specified file.
     * <p>
     * If this method is invoked remotely from another process, the data is written
     * to a file named {@code outputFile} on the machine where the target Java
     * Virtual Machine (JVM) is running. If the file location is a relative path, it
     * is relative to the working directory where the target JVM was started.
     *
     * @param recordingId the ID of the recording to dump data for
     *
     * @param outputFile the system-dependent file name where data is written, not
     *        {@code null}
     *
     * @throws IOException if the recording can't be dumped due to an I/O error (for
     *         example, an invalid path)
     *
     * @throws IllegalArgumentException if a recording with the specified ID doesn't
     *         exist
     *
     * @throws IllegalStateException if the recording is not yet started or if it is
     *         already closed
     *
     * @throws SecurityException if a security manager exists and its
     *         {@code SecurityManager.checkWrite(java.lang.String)} method denies
     *         write access to the named file or the caller does not have
     *         {@code ManagmentPermission("control")}
     *
     * @see java.nio.file.Path#toString()
     * @see Recording#dump(java.nio.file.Path)
     */
    void copyTo(long recordingId, String outputFile) throws IOException, SecurityException;
}
