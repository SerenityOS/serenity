/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.reflect;

import java.lang.reflect.InvocationTargetException;

/** <P> Package-private implementation of the MethodAccessor interface
    which has access to all classes and all fields, regardless of
    language restrictions. See MagicAccessor. </P>

    <P> This class is known to the VM; do not change its name without
    also changing the VM's code. </P>

    <P> NOTE: ALL methods of subclasses are skipped during security
    walks up the stack. The assumption is that the only such methods
    that will persistently show up on the stack are the implementing
    methods for java.lang.reflect.Method.invoke(). </P>
*/

abstract class MethodAccessorImpl extends MagicAccessorImpl
    implements MethodAccessor {
    /** Matches specification in {@link java.lang.reflect.Method} */
    public abstract Object invoke(Object obj, Object[] args)
        throws IllegalArgumentException, InvocationTargetException;
}
