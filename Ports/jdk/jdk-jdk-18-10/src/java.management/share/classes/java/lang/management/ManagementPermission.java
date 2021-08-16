/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.management;

/**
 * The permission which the SecurityManager will check when code
 * that is running with a SecurityManager calls methods defined
 * in the management interface for the Java platform.
 * <P>
 * The following table
 * provides a summary description of what the permission allows,
 * and discusses the risks of granting code the permission.
 *
 * <table class="striped">
 * <caption style="display:none">Table shows permission target name, what the permission allows, and associated risks</caption>
 * <thead>
 * <tr>
 * <th scope="col">Permission Target Name</th>
 * <th scope="col">What the Permission Allows</th>
 * <th scope="col">Risks of Allowing this Permission</th>
 * </tr>
 * </thead>
 * <tbody style="text=align:left">
 *
 * <tr>
 *   <th scope="row">control</th>
 *   <td>Ability to control the runtime characteristics of the Java virtual
 *       machine, for example, enabling and disabling the verbose output for
 *       the class loading or memory system, setting the threshold of a memory
 *       pool, and enabling and disabling the thread contention monitoring
 *       support. Some actions controlled by this permission can disclose
 *       information about the running application, like the -verbose:class
 *       flag.
 *   </td>
 *   <td>This allows an attacker to control the runtime characteristics
 *       of the Java virtual machine and cause the system to misbehave. An
 *       attacker can also access some information related to the running
 *       application.
 *   </td>
 * </tr>
 * <tr>
 *   <th scope="row">monitor</th>
 *   <td>Ability to retrieve runtime information about
 *       the Java virtual machine such as thread
 *       stack trace, a list of all loaded class names, and input arguments
 *       to the Java virtual machine.</td>
 *   <td>This allows malicious code to monitor runtime information and
 *       uncover vulnerabilities.</td>
 * </tr>
 *
 * </tbody>
 * </table>
 *
 * <p>
 * Programmers do not normally create ManagementPermission objects directly.
 * Instead they are created by the security policy code based on reading
 * the security policy file.
 *
 * @author  Mandy Chung
 * @since   1.5
 *
 * @see java.security.BasicPermission
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 * @see java.lang.SecurityManager
 *
 */

public final class ManagementPermission extends java.security.BasicPermission {
    private static final long serialVersionUID = 1897496590799378737L;

    /**
     * Constructs a ManagementPermission with the specified name.
     *
     * @param name Permission name. Must be either "monitor" or "control".
     *
     * @throws NullPointerException if <code>name</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>name</code> is empty or invalid.
     */
    public ManagementPermission(String name) {
        super(name);
        if (!name.equals("control") && !name.equals("monitor")) {
            throw new IllegalArgumentException("name: " + name);
        }
    }

    /**
     * Constructs a new ManagementPermission object.
     *
     * @param name Permission name. Must be either "monitor" or "control".
     * @param actions Must be either null or the empty string.
     *
     * @throws NullPointerException if <code>name</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>name</code> is empty or
     * if arguments are invalid.
     */
    public ManagementPermission(String name, String actions)
        throws IllegalArgumentException {
        super(name);
        if (!name.equals("control") && !name.equals("monitor")) {
            throw new IllegalArgumentException("name: " + name);
        }
        if (actions != null && actions.length() > 0) {
            throw new IllegalArgumentException("actions: " + actions);
        }
    }
}
