/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;

/**
 * The extending point of basic JGSS-API.
 * <p>
 * If a module wants to extend basic JGSS-API classes, it should extends this
 * class and register itself as "the extender" using the setExtender method.
 * When various GSSManager.createXXX methods are called, they will call
 * "the extender"'s wrap methods to create objects of extended types
 * instead of basic types.
 * <p>
 * We have only one extension now defined in com.sun.security.jgss, and the
 * registering process is triggered in {@link GSSManagerImpl} by calling
 * Class.forName("com.sun.security.jgss.Extender"). Only GSSContext
 * and GSSCredential are extended now.
 * <p>
 * The setExtender method should be called before any JGSS call.
 */
public class JgssExtender {

    // "The extender"
    private static volatile JgssExtender theOne = new JgssExtender();

    /**
     * Gets "the extender". GSSManager calls this method so that it can
     * wrap basic objects into extended objects.
     * @return the extender
     */
    public static JgssExtender getExtender() {
        return theOne;
    }

    /**
     * Set "the extender" so that GSSManager can create extended objects.
     */
    protected static void setExtender(JgssExtender theOne) {
        JgssExtender.theOne = theOne;
    }

    /**
     * Wraps a plain GSSCredential object into an extended type.
     */
    public GSSCredential wrap(GSSCredential cred) {
        return cred;
    }

    /**
     * Wraps a plain GSSContext object into an extended type.
     */
    public GSSContext wrap(GSSContext ctxt) {
        return ctxt;
    }
}
