/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8174725
 * @summary Throw NoClassDefFoundError if class access_flags have ACC_MODULE set
 * @compile BadAccModule.jcod BadAccModInrClss.jcod
 * @run main AccModuleTest
 */

// Test that classes with access_flags containing ACC_MODULE cause ClassDefNotFoundErrors.
public class AccModuleTest {
    public static void main(String args[]) throws Throwable {

        System.out.println("Regression test for bug 8174725");
        try {
            Class newClass = Class.forName("BadAccModule");
            throw new RuntimeException("Expected NoClassDefFoundError exception not thrown");
        } catch (java.lang.NoClassDefFoundError e) {
            if (!e.getMessage().contains("BadAccModule is not a class because access_flag ACC_MODULE is set")) {
                throw new RuntimeException("Wrong NoClassDefFoundError exception for AccModuleTest: " + e.getMessage());
            }
        }
        try {
            Class newClass = Class.forName("BadAccModInrClss");
            throw new RuntimeException("Expected NoClassDefFoundError exception not thrown");
        } catch (java.lang.NoClassDefFoundError e) {
            if (!e.getMessage().contains("BadAccModInrClss is not a class because access_flag ACC_MODULE is set")) {
                throw new RuntimeException("Wrong NoClassDefFoundError exception for BadAccModInrClss: " + e.getMessage());
            }
        }
    }
}
