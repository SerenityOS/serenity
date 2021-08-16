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

import java.io.File;

/*
 * @test
 * @run main/othervm -Xbootclasspath/a:/usr/lib -showversion -Xbootclasspath/a:/i/dont/exist BootClassPathAppendProp
 * @run main/othervm --patch-module=no_module=/not/here -Xbootclasspath/a:/i/may/exist BootClassPathAppendProp
 * @run main/othervm -Djdk.boot.class.path.append=newdir BootClassPathAppendProp
 * @run main/othervm BootClassPathAppendProp
 */

// Test that property jdk.boot.class.path.append contains only the bootclasspath
// info following the "modules" jimage file.
public class BootClassPathAppendProp {
    public static void main(String[] args) throws Exception {
        // jdk.boot.class.path.append is a non-writeable, internal property.
        // The call to System.getProperty should return null.
        if (System.getProperty("jdk.boot.class.path.append") != null) {
            throw new RuntimeException("Test failed, jdk.boot.class.path.append has value: " +
                System.getProperty("jdk.boot.class.path.append"));
        } else {
            System.out.println("Test BootClassPathAppendProp passed");
        }
    }
}
