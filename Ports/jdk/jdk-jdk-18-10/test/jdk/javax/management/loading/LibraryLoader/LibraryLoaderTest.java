/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4969756
 * @summary Test that the same native library coming from the same jar file can
 * be loaded twice by two different MLets on the same JVM without conflict.
 * @author Luis-Miguel Alventosa
 *
 * @run clean LibraryLoaderTest
 * @run build LibraryLoaderTest
 * @run main/othervm LibraryLoaderTest
 */

import java.io.File;
import java.util.Set;
import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class LibraryLoaderTest {

    private static final String mletInfo[][] = {
        {"testDomain:type=MLet,index=0", "UseNativeLib0.html"},
        {"testDomain:type=MLet,index=1", "UseNativeLib1.html"}
    };

    public static void main(String args[]) throws Exception {

        String osName = System.getProperty("os.name");
        System.out.println("os.name=" + osName);
        String osArch = System.getProperty("os.arch");
        System.out.println("os.name=" + osArch);

        // Check for supported platforms:
        //
        // Windows/x86
        //
        if ((!(osName.startsWith("Windows") && osArch.equals("x86")))) {
            System.out.println(
              "This test runs only on Windows/x86 platforms");
            System.out.println("Bye! Bye!");
            return;
        }

        String libPath = System.getProperty("java.library.path");
        System.out.println("java.library.path=" + libPath);
        String testSrc = System.getProperty("test.src");
        System.out.println("test.src=" + testSrc);
        String workingDir = System.getProperty("user.dir");
        System.out.println("user.dir=" + workingDir);

        String urlCodebase;
        if (testSrc.startsWith("/")) {
            urlCodebase =
                "file:" + testSrc.replace(File.separatorChar, '/') + "/";
        } else {
            urlCodebase =
                "file:/" + testSrc.replace(File.separatorChar, '/') + "/";
        }

        // Create MBeanServer
        //
        MBeanServer server = MBeanServerFactory.newMBeanServer();

        // Create MLet instances and call getRandom on the loaded MBeans
        //
        for (int i = 0; i < mletInfo.length; i++) {
            // Create ObjectName for MLet
            //
            ObjectName mlet = new ObjectName(mletInfo[i][0]);
            server.createMBean("javax.management.loading.MLet", mlet);
            System.out.println("MLet = " + mlet);

            // Display old library directory and set it to test.classes
            //
            String libraryDirectory =
                (String) server.getAttribute(mlet, "LibraryDirectory");
            System.out.println("Old Library Directory = " +
                               libraryDirectory);
            Attribute attribute =
                new Attribute("LibraryDirectory", workingDir);
            server.setAttribute(mlet, attribute);
            libraryDirectory =
                (String) server.getAttribute(mlet, "LibraryDirectory");
            System.out.println("New Library Directory = " +
                               libraryDirectory);

            // Get MBeans from URL
            //
            String mletURL = urlCodebase + mletInfo[i][1];
            System.out.println("MLet URL = " + mletURL);
            Object[] params = new Object[] { mletURL };
            String[] signature = new String[] {"java.lang.String"};
            Object res[] = ((Set<?>) server.invoke(mlet,
                                                   "getMBeansFromURL",
                                                   params,
                                                   signature)).toArray();

            // Iterate through all the loaded MBeans
            //
            for (int j = 0; j < res.length; j++) {
                // Now ensure none of the returned objects is a Throwable
                //
                if (res[j] instanceof Throwable) {
                    ((Throwable) res[j]).printStackTrace(System.out);
                    throw new Exception("Failed to load the MBean #" + j
                        ,(Throwable)res[j]);
                }

                // On each of the loaded MBeans, try to invoke their
                // native operation
                //
                Object result = null;
                try {
                    ObjectName mbean =
                        ((ObjectInstance) res[j]).getObjectName();
                    result = server.getAttribute(mbean, "Random");
                    System.out.println("MBean #" + j + " = " + mbean);
                    System.out.println("Random number = " + result);
                } catch (ReflectionException e) {
                    e.getTargetException().printStackTrace(System.out);
                    throw new Exception ("A ReflectionException "
                            + "occured when attempting to invoke "
                            + "a native library based operation.",
                            e.getTargetException());
                }
            }
        }
    }
}
