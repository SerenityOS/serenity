/*
 * Copyright (c) 1996, 1997, Oracle and/or its affiliates. All rights reserved.
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

package java.beans;

/**
 * The ParameterDescriptor class allows bean implementors to provide
 * additional information on each of their parameters, beyond the
 * low level type information provided by the java.lang.reflect.Method
 * class.
 * <p>
 * Currently all our state comes from the FeatureDescriptor base class.
 *
 * @since 1.1
 */

public class ParameterDescriptor extends FeatureDescriptor {

    /**
     * Public default constructor.
     */
    public ParameterDescriptor() {
    }

    /**
     * Package private dup constructor.
     * This must isolate the new object from any changes to the old object.
     */
    ParameterDescriptor(ParameterDescriptor old) {
        super(old);
    }

}
