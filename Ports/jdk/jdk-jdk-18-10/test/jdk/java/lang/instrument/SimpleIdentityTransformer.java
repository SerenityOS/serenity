/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.lang.instrument.Instrumentation;
import java.lang.instrument.ClassFileTransformer;

import java.security.*;

/*
 * Copyright 2003 Wily Technology, Inc.
 */

/**
 * ClassFileTransformer implementation that returns a copy of its input
 */
public class
SimpleIdentityTransformer implements ClassFileTransformer {

    /**
     * Constructor for SimpleIdentityTransform.
     */
    public SimpleIdentityTransformer() {
        super();
    }

    /**
     *
     */
    public byte[]
    transform(
        ClassLoader loader,
        String className,
        Class<?> classBeingRedefined,
        ProtectionDomain    protectionDomain,
        byte[] classfileBuffer) {
        byte[] newBuffer = new byte[classfileBuffer.length];
        System.arraycopy(classfileBuffer, 0, newBuffer, 0, classfileBuffer.length);

        return newBuffer;
    }

    public byte[]
    transform(
        Module module,
        ClassLoader loader,
        String className,
        Class<?> classBeingRedefined,
        ProtectionDomain    protectionDomain,
        byte[] classfileBuffer) {
        byte[] newBuffer = new byte[classfileBuffer.length];
        System.arraycopy(classfileBuffer, 0, newBuffer, 0, classfileBuffer.length);

        return newBuffer;
    }
}
