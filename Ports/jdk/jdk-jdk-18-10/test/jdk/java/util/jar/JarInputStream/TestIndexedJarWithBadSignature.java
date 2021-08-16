/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6544278
 * @summary Confirm the JarInputStream throws the SecurityException when
 *          verifying an indexed jar file with corrupted signature
 */

import java.io.IOException;
import java.io.FileInputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class TestIndexedJarWithBadSignature {

    public static void main(String...args) throws Throwable {
        try (JarInputStream jis = new JarInputStream(
                 new FileInputStream(System.getProperty("test.src", ".") +
                                     System.getProperty("file.separator") +
                                     "BadSignedJar.jar")))
        {
            JarEntry je1 = jis.getNextJarEntry();
            while(je1!=null){
                System.out.println("Jar Entry1==>"+je1.getName());
                je1 = jis.getNextJarEntry(); // This should throw Security Exception
            }
            throw new RuntimeException(
                "Test Failed:Security Exception not being thrown");
        } catch (IOException ie){
            ie.printStackTrace();
        } catch (SecurityException e) {
            System.out.println("Test passed: Security Exception thrown as expected");
        }
    }
}
