/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.management;

import java.lang.management.PlatformManagedObject;
import javax.management.DynamicMBean;

/**
 * Management interface for the diagnostic commands for the HotSpot Virtual Machine.
 *
 * <p>The {@code DiagnosticCommandMBean} is registered to the
 * {@linkplain java.lang.management.ManagementFactory#getPlatformMBeanServer
 * platform MBeanServer} as are other platform MBeans.
 *
 * <p>The {@link javax.management.ObjectName ObjectName} for uniquely identifying
 * the diagnostic MBean within an MBeanServer is:
 * <blockquote>
 *    {@code com.sun.management:type=DiagnosticCommand}
 * </blockquote>
 *
 * <p>This MBean is a {@link javax.management.DynamicMBean DynamicMBean}
 * and also a {@link javax.management.NotificationEmitter}.
 * The {@code DiagnosticCommandMBean} is generated at runtime and is subject to
 * modifications during the lifetime of the Java virtual machine.
 *
 * A <em>diagnostic command</em> is represented as an operation of
 * the {@code DiagnosticCommandMBean} interface. Each diagnostic command has:
 * <ul>
 * <li>the diagnostic command name which is the name being referenced in
 *     the HotSpot Virtual Machine</li>
 * <li>the MBean operation name which is the
 *     {@linkplain javax.management.MBeanOperationInfo#getName() name}
 *     generated for the diagnostic command operation invocation.
 *     The MBean operation name is implementation dependent</li>
 * </ul>
 *
 * The recommended way to transform a diagnostic command name into a MBean
 * operation name is as follows:
 * <ul>
 * <li>All characters from the first one to the first dot are set to be
 *      lower-case characters</li>
 * <li>Every dot or underline character is removed and the following
 *   character is set to be an upper-case character</li>
 * <li>All other characters are copied without modification</li>
 * </ul>
 *
 * <p>The diagnostic command name is always provided with the meta-data on the
 * operation in a field named {@code dcmd.name} (see below).
 *
 * <p>A diagnostic command may or may not support options or arguments.
 * All the operations return {@code String} and either take
 * no parameter for operations that do not support any option or argument,
 * or take a {@code String[]} parameter for operations that support at least
 * one option or argument.
 * Each option or argument must be stored in a single String.
 * Options or arguments split across several String instances are not supported.
 *
 * <p>The distinction between options and arguments: options are identified by
 * the option name while arguments are identified by their position in the
 * command line. Options and arguments are processed in the order of the array
 * passed to the invocation method.
 *
 * <p>Like any operation of a dynamic MBean, each of these operations is
 * described by {@link javax.management.MBeanOperationInfo MBeanOperationInfo}
 * instance. Here's the values returned by this object:
 * <ul>
 *  <li>{@link javax.management.MBeanOperationInfo#getName() getName()}
 *      returns the operation name generated from the diagnostic command name</li>
 *  <li>{@link javax.management.MBeanOperationInfo#getDescription() getDescription()}
 *      returns the diagnostic command description
 *      (the same as the one return in the 'help' command)</li>
 *  <li>{@link javax.management.MBeanOperationInfo#getImpact() getImpact()}
 *      returns {@code ACTION_INFO}</li>
 *  <li>{@link javax.management.MBeanOperationInfo#getReturnType() getReturnType()}
 *      returns {@code java.lang.String}</li>
 *  <li>{@link javax.management.MBeanOperationInfo#getDescriptor() getDescriptor()}
 *      returns a Descriptor instance (see below)</li>
 * </ul>
 *
 * <p>The {@link javax.management.Descriptor Descriptor}
 * is a collection of fields containing additional
 * meta-data for a JMX element. A field is a name and an associated value.
 * The additional meta-data provided for an operation associated with a
 * diagnostic command are described in the table below:
 *
 * <table class="striped"><caption style="display:none">description</caption>
 *   <thead>
 *   <tr>
 *     <th scope="col">Name</th><th scope="col">Type</th><th scope="col">Description</th>
 *   </tr>
 *   </thead>
 *   <tbody>
 *   <tr>
 *     <th scope="row">dcmd.name</th><td>String</td>
 *     <td>The original diagnostic command name (not the operation name)</td>
 *   </tr>
 *   <tr>
 *     <th scope="row">dcmd.description</th><td>String</td>
 *     <td>The diagnostic command description</td>
 *   </tr>
 *   <tr>
 *     <th scope="row">dcmd.help</th><td>String</td>
 *     <td>The full help message for this diagnostic command (same output as
 *          the one produced by the 'help' command)</td>
 *   </tr>
 *   <tr>
 *     <th scope="row">dcmd.vmImpact</th><td>String</td>
 *     <td>The impact of the diagnostic command,
 *      this value is the same as the one printed in the 'impact'
 *      section of the help message of the diagnostic command, and it
 *      is different from the getImpact() of the MBeanOperationInfo</td>
 *   </tr>
 *   <tr>
 *     <th scope="row">dcmd.enabled</th><td>boolean</td>
 *     <td>True if the diagnostic command is enabled, false otherwise</td>
 *   </tr>
 *   <tr>
 *     <th scope="row">dcmd.permissionClass</th><td>String</td>
 *     <td>Some diagnostic command might require a specific permission to be
 *          executed, in addition to the MBeanPermission to invoke their
 *          associated MBean operation. This field returns the fully qualified
 *          name of the permission class or null if no permission is required
 *   </td>
 *   </tr>
 *   <tr>
 *     <th scope="row">dcmd.permissionName</th><td>String</td>
 *     <td>The fist argument of the permission required to execute this
 *          diagnostic command or null if no permission is required</td>
 *   </tr>
 *   <tr>
 *     <th scope="row">dcmd.permissionAction</th><td>String</td>
 *     <td>The second argument of the permission required to execute this
 *          diagnostic command or null if the permission constructor has only
 *          one argument (like the ManagementPermission) or if no permission
 *          is required</td>
 *   </tr>
 *   <tr>
 *     <th scope="row">dcmd.arguments</th><td>Descriptor</td>
 *     <td>A Descriptor instance containing the descriptions of options and
 *          arguments supported by the diagnostic command (see below)</td>
 *   </tr>
 *   </tbody>
 * </table>
 *
 * <p>The description of parameters (options or arguments) of a diagnostic
 * command is provided within a Descriptor instance. In this Descriptor,
 * each field name is a parameter name, and each field value is itself
 * a Descriptor instance. The fields provided in this second Descriptor
 * instance are described in the table below:
 *
 * <table class="striped"><caption style="display:none">description</caption>
 *   <thead>
 *   <tr>
 *     <th scope="col">Name</th><th scope="col">Type</th><th scope="col">Description</th>
 *   </tr>
 *   </thead>
 *   <tbody>
 *   <tr>
 *     <th scope="row">dcmd.arg.name</th><td>String</td>
 *     <td>The name of the parameter</td>
 *   </tr>
 *   <tr>
 *     <th scope="row">dcmd.arg.type</th><td>String</td>
 *     <td>The type of the parameter. The returned String is the name of a type
 *          recognized by the diagnostic command parser. These types are not
 *          Java types and are implementation dependent.
 *          </td>
 *   </tr>
 *   <tr>
 *     <th scope="row">dcmd.arg.description</th><td>String</td>
 *     <td>The parameter description</td>
 *   </tr>
 *   <tr>
 *     <th scope="row">dcmd.arg.isMandatory</th><td>boolean</td>
 *     <td>True if the parameter is mandatory, false otherwise</td>
 *   </tr>
 *   <tr>
 *     <th scope="row">dcmd.arg.isOption</th><td>boolean</td>
 *     <td>True if the parameter is an option, false if it is an argument</td>
 *   </tr>
 *   <tr>
 *     <th scope="row">dcmd.arg.isMultiple</th><td>boolean</td>
 *     <td>True if the parameter can be specified several times, false
 *          otherwise</td>
 *   </tr>
 *   </tbody>
 * </table>
 *
 * <p>When the set of diagnostic commands currently supported by the Java
 * Virtual Machine is modified, the {@code DiagnosticCommandMBean} emits
 * a {@link javax.management.Notification} with a
 * {@linkplain javax.management.Notification#getType() type} of
 * <a href="{@docRoot}/java.management/javax/management/MBeanInfo.html#info-changed">
 * {@code "jmx.mbean.info.changed"}</a> and a
 * {@linkplain javax.management.Notification#getUserData() userData} that
 * is the new {@code MBeanInfo}.
 *
 * @since 1.8
 */
public interface DiagnosticCommandMBean extends DynamicMBean
{

}
