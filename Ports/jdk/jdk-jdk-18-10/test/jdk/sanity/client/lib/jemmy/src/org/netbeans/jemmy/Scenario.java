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
 *
 * A test scenario. This interface provides a mechanism for putting something
 * into execution. The execution is conditioned in a very general way by passing
 * a {@code java.lang.Object} to it's {@code runIt} method.
 *
 * @see Test
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public interface Scenario {

    /**
     * Defines a way to execute this test scenario.
     *
     * @param param An object passed to configure the test scenario execution.
     * For example, this parameter might be a      <code>java.lang.String[]<code> object that lists the
     * command line arguments to the Java application corresponding
     * to a test.
     * @return an int that tells something about the execution. For, example, a
     * status code.
     */
    public int runIt(Object param);
}
