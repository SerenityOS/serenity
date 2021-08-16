/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

import javax.management.openmbean.CompositeData;
import sun.management.MonitorInfoCompositeData;

/**
 * Information about an object monitor lock.  An object monitor is locked
 * when entering a synchronization block or method on that object.
 *
 * <h2>MXBean Mapping</h2>
 * {@code MonitorInfo} is mapped to a {@link CompositeData CompositeData}
 * with attributes as specified in
 * the {@link #from from} method.
 *
 * @author  Mandy Chung
 * @since   1.6
 */
public class MonitorInfo extends LockInfo {

    private int    stackDepth;
    private StackTraceElement stackFrame;

    /**
     * Construct a {@code MonitorInfo} object.
     *
     * @param className the fully qualified name of the class of the lock object.
     * @param identityHashCode the {@link System#identityHashCode
     *                         identity hash code} of the lock object.
     * @param stackDepth the depth in the stack trace where the object monitor
     *                   was locked.
     * @param stackFrame the stack frame that locked the object monitor.
     * @throws IllegalArgumentException if
     *    {@code stackDepth} &ge; 0 but {@code stackFrame} is {@code null},
     *    or {@code stackDepth} &lt; 0 but {@code stackFrame} is not
     *       {@code null}.
     */
    public MonitorInfo(String className,
                       int identityHashCode,
                       int stackDepth,
                       StackTraceElement stackFrame) {
        super(className, identityHashCode);
        if (stackDepth >= 0 && stackFrame == null) {
            throw new IllegalArgumentException("Parameter stackDepth is " +
                stackDepth + " but stackFrame is null");
        }
        if (stackDepth < 0 && stackFrame != null) {
            throw new IllegalArgumentException("Parameter stackDepth is " +
                stackDepth + " but stackFrame is not null");
        }
        this.stackDepth = stackDepth;
        this.stackFrame = stackFrame;
    }

    /**
     * Returns the depth in the stack trace where the object monitor
     * was locked.  The depth is the index to the {@code StackTraceElement}
     * array returned in the {@link ThreadInfo#getStackTrace} method.
     *
     * @return the depth in the stack trace where the object monitor
     *         was locked, or a negative number if not available.
     */
    public int getLockedStackDepth() {
        return stackDepth;
    }

    /**
     * Returns the stack frame that locked the object monitor.
     *
     * @return {@code StackTraceElement} that locked the object monitor,
     *         or {@code null} if not available.
     */
    public StackTraceElement getLockedStackFrame() {
        return stackFrame;
    }

    /**
     * Returns a {@code MonitorInfo} object represented by the
     * given {@code CompositeData}.
     * The given {@code CompositeData} must contain the following attributes
     * as well as the attributes specified in the
     * <a href="LockInfo.html#MappedType">
     * mapped type</a> for the {@link LockInfo} class:
     * <table class="striped" style="margin-left:2em">
     * <caption style="display:none">The attributes and their types the given CompositeData contains</caption>
     * <thead>
     * <tr>
     *   <th scope="col">Attribute Name</th>
     *   <th scope="col">Type</th>
     * </tr>
     * </thead>
     * <tbody style="text-align:left">
     * <tr>
     *   <th scope="row">lockedStackFrame</th>
     *   <td><a href="ThreadInfo.html#stackTraceElement">
     *       {@code CompositeData} for {@code StackTraceElement}</a> as specified
     *       in {@link ThreadInfo#from(CompositeData)} method.
     *   </td>
     * </tr>
     * <tr>
     *   <th scope="row">lockedStackDepth</th>
     *   <td>{@code java.lang.Integer}</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @param cd {@code CompositeData} representing a {@code MonitorInfo}
     *
     * @throws IllegalArgumentException if {@code cd} does not
     *   represent a {@code MonitorInfo} with the attributes described
     *   above.
     *
     * @return a {@code MonitorInfo} object represented
     *         by {@code cd} if {@code cd} is not {@code null};
     *         {@code null} otherwise.
     */
    public static MonitorInfo from(CompositeData cd) {
        if (cd == null) {
            return null;
        }

        if (cd instanceof MonitorInfoCompositeData) {
            return ((MonitorInfoCompositeData) cd).getMonitorInfo();
        } else {
            MonitorInfoCompositeData.validateCompositeData(cd);
            String className = MonitorInfoCompositeData.getClassName(cd);
            int identityHashCode = MonitorInfoCompositeData.getIdentityHashCode(cd);
            int stackDepth = MonitorInfoCompositeData.getLockedStackDepth(cd);
            StackTraceElement stackFrame = MonitorInfoCompositeData.getLockedStackFrame(cd);
            return new MonitorInfo(className,
                                   identityHashCode,
                                   stackDepth,
                                   stackFrame);
        }
    }

}
