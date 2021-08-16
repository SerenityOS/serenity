/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.instrument;

/*
 * Copyright 2003 Wily Technology, Inc.
 */

/**
 * This class serves as a parameter block to the <code>Instrumentation.redefineClasses</code> method.
 * Serves to bind the <code>Class</code> that needs redefining together with the new class file bytes.
 *
 * @see     java.lang.instrument.Instrumentation#redefineClasses
 * @since   1.5
 */
public final class ClassDefinition {
    /**
     *  The class to redefine
     */
    private final Class<?> mClass;

    /**
     *  The replacement class file bytes
     */
    private final byte[]   mClassFile;

    /**
     *  Creates a new <code>ClassDefinition</code> binding using the supplied
     *  class and class file bytes. Does not copy the supplied buffer, just captures a reference to it.
     *
     * @param theClass the <code>Class</code> that needs redefining
     * @param theClassFile the new class file bytes
     *
     * @throws java.lang.NullPointerException if the supplied class or array is <code>null</code>.
     */
    public
    ClassDefinition(    Class<?> theClass,
                        byte[]  theClassFile) {
        if (theClass == null || theClassFile == null) {
            throw new NullPointerException();
        }
        mClass      = theClass;
        mClassFile  = theClassFile;
    }

    /**
     * Returns the class.
     *
     * @return    the <code>Class</code> object referred to.
     */
    public Class<?>
    getDefinitionClass() {
        return mClass;
    }

    /**
     * Returns the array of bytes that contains the new class file.
     *
     * @return    the class file bytes.
     */
    public byte[]
    getDefinitionClassFile() {
        return mClassFile;
    }
}
