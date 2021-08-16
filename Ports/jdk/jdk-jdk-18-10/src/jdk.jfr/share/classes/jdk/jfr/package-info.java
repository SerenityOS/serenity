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

/**
 * This package provides classes to create events and control Flight Recorder.
 * <p>
 * <b>Defining events</b>
 * <p>
 * Flight Recorder collects data as events. An event has a time stamp, duration
 * and usually an application-specific payload, useful for diagnosing the
 * running application up to the failure or crash.
 * <p>
 * To define a Flight Recorder event, extend {@link jdk.jfr.Event} and add
 * fields that matches the data types of the payload. Metadata about fields,
 * such as labels, descriptions and units, can be added by using the annotations
 * available in the {@code jdk.jfr} package, or by using a user-defined
 * annotation that has the {@link jdk.jfr.MetadataDefinition} annotation.
 * <p>
 * After an event class is defined, instances can be created (event objects).
 * Data is stored in the event by assigning data to fields. Event timing can be
 * explicitly controlled by using the {@code begin} and {@code end} methods
 * available in the {@code Event} class.
 * <p>
 * Gathering data to store in an event can be expensive. The
 * {@link jdk.jfr.Event#shouldCommit()} method can be used to verify whether
 * an event instance would actually be written to the system when
 * the {@link jdk.jfr.Event#commit()} method is invoked.
 * If {@link jdk.jfr.Event#shouldCommit()} returns {@code false},
 * then those operations can be avoided.
 * <p>
 * Sometimes the field layout of an event is not known at compile time. In that
 * case, an event can be dynamically defined. However, dynamic events might not
 * have the same level of performance as statically defined ones and tools might
 * not be able to identify and visualize the data without knowing the layout.
 * <p>
 * To dynamically define an event, use the {@link jdk.jfr.EventFactory} class
 * and define fields by using the {@link jdk.jfr.ValueDescriptor} class, and
 * define annotations by using the {@link jdk.jfr.AnnotationElement} class. Use
 * the factory to allocate an event and the
 * {@link jdk.jfr.Event#set(int, Object)} method to populate it.
 * <p>
 * <b>Controlling Flight Recorder</b>
 * <p>
 * Flight Recorder can be controlled locally by using the {@code jcmd}
 * command line tool or remotely by using the {@code FlightRecorderMXBean}
 * interface, registered in the platform MBeanServer. When direct programmatic
 * access is needed, a Flight Recorder instance can be obtained by invoking
 * {@link jdk.jfr.FlightRecorder#getFlightRecorder()} and a recording created by
 * using {@link jdk.jfr.Recording} class, from which the amount of data to
 * record is configured.
 * <p>
 * <b>Settings and configuration</b>
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
 * for the Java Virtual Machine instance that the event is registered in.
 * <p>
 * A list of available event names is retrieved by invoking
 * {@link jdk.jfr.FlightRecorder#getEventTypes()} and
 * {@link jdk.jfr.EventType#getName()}. A list of available settings for an
 * event type is obtained by invoking
 * {@link jdk.jfr.EventType#getSettingDescriptors()} and
 * {@link jdk.jfr.ValueDescriptor#getName()}.
 * <p>
 * <b>Predefined settings</b>
 * <table class="striped">
 * <caption>Event setting names and their purpose.</caption> <thead>
 * <tr>
 * <th scope="col">Name</th>
 * <th scope="col">Description</th>
 * <th scope="col">Default value</th>
 * <th scope="col">Format</th>
 * <th scope="col">Example values</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <th scope="row">{@code enabled}</th>
 * <td>Specifies whether the event is recorded</td>
 * <td>{@code "true"}</td>
 * <td>String representation of a {@code Boolean} ({@code "true"} or
 * {@code "false"})</td>
 * <td>{@code "true"}<br>
 * {@code "false"}</td>
 * </tr>
 * <tr>
 * <th scope="row">{@code threshold}</th>
 * <td>Specifies the duration below which an event is not recorded</td>
 * <td>{@code "0"} (no limit)</td>
 * <td>{@code "0"} if no threshold is used, otherwise a string representation of
 * a positive {@code Long} followed by a space and one of the following units:
 * <ul style="list-style-type:none">
 * <li>{@code "ns"} (nanoseconds)
 * <li>{@code "us"} (microseconds)
 * <li>{@code "ms"} (milliseconds)
 * <li>{@code "s"} (seconds)
 * <li>{@code "m"} (minutes)
 * <li>{@code "h"} (hours)
 * <li>{@code "d"} (days)
 * </ul>
 * <td>{@code "0"}<br>
 * {@code "10 ms"}<br>
 * "1 s"</td>
 * </tr>
 * <tr>
 * <th scope="row">{@code period}</th>
 * <td>Specifies the interval at which the event is emitted, if it is
 * periodic</td>
 * <td>{@code "everyChunk"}</td>
 * <td>{@code "everyChunk"}, if a periodic event should be emitted with every
 * file rotation, otherwise a string representation of a positive {@code Long}
 * value followed by an empty space and one of the following units:
 * <ul style="list-style-type:none">
 * <li>{@code "ns"} (nanoseconds)
 * <li>{@code "us"} (microseconds)
 * <li>{@code "ms"} (milliseconds)
 * <li>{@code "s"} (seconds)
 * <li>{@code "m"} (minutes)
 * <li>{@code "h"} (hours)
 * <li>{@code "d"} (days)
 * </ul>
 * </td>
 * <td>{@code "20 ms"}<br>
 * {@code "1 s"}<br>
 * {@code "everyChunk"}</td>
 *
 * </tr>
 * <tr>
 * <th scope="row">{@code stackTrace}</th>
 * <td>Specifies whether the stack trace from the {@link Event#commit()} method
 * is recorded</td>
 * <td>{@code "true"}</td>
 * <td>String representation of a {@code Boolean} ({@code "true"} or
 * {@code "false"})</td>
 * <td>{@code "true"},<br>
 * {@code "false"}</td>
 * </tr>
 * </tbody>
 * </table>
 * <p>
 * <b>Null-handling</b>
 * <p>
 * All methods define whether they accept or return {@code null} in the Javadoc.
 * Typically this is expressed as {@code "not null"}. If a {@code null}
 * parameter is used where it is not allowed, a
 * {@code java.lang.NullPointerException} is thrown. If a {@code null}
 * parameters is passed to a method that throws other exceptions, such as
 * {@code java.io.IOException}, the {@code java.lang.NullPointerException} takes
 * precedence, unless the Javadoc for the method explicitly states how
 * {@code null} is handled, i.e. by throwing
 * {@code java.lang.IllegalArgumentException}.
 *
 * @since 9
 */
package jdk.jfr;
