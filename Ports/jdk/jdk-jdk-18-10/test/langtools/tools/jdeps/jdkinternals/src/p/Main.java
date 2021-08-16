/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package p;

import com.sun.image.codec.jpeg.JPEGCodec;
import sun.misc.Service;
import sun.misc.SoftCache;
import sun.reflect.Reflection;
import sun.reflect.ReflectionFactory;

public class Main {
    public static void main() {
        // in jdk.unsupported
        ReflectionFactory factory = ReflectionFactory.getReflectionFactory();

        // removed from jdk.unsupported in JDK 11
        Class<?> caller = Reflection.getCallerClass(2);

        // removed
        JPEGCodec r = new JPEGCodec();

        // removed
        SoftCache s = new SoftCache();

        // removed
        Service.providers(S.class);
    }
}
