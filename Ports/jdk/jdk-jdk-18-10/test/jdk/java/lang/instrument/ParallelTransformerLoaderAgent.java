/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.instrument.*;
import java.net.*;
import java.io.*;
import java.security.*;

/**
 *      Test Java Agent
 *
 *      @author Daryl Puryear
 *      @copyright 1999-2004 Wily Technology, Inc.  All rights reserved.
 */
public class ParallelTransformerLoaderAgent
{
        private static URL sURL;
        private static ClassLoader sClassLoader;

        public static synchronized ClassLoader
        getClassLoader()
        {
                return sClassLoader;
        }

        public static synchronized void
        generateNewClassLoader()
        {
                sClassLoader = new URLClassLoader(new URL[] {sURL});
        }

        public static void
        premain(        String agentArgs,
                        Instrumentation instrumentation)
                throws Exception
        {
                if (agentArgs == null || agentArgs == "")
                {
                        System.err.println("Error: No jar file name provided, test will not run.");
                        return;
                }

                sURL = (new File(agentArgs)).toURL();
                System.out.println("Using jar file: " + sURL);
                generateNewClassLoader();

                instrumentation.addTransformer(new TestTransformer());
        }

        private static class TestTransformer
                implements ClassFileTransformer
        {
                public byte[]
                transform(      ClassLoader loader,
                                String className,
                                Class classBeingRedefined,
                                ProtectionDomain protectionDomain,
                                byte[] classfileBuffer)
                        throws IllegalClassFormatException
                {
                        String tName = Thread.currentThread().getName();

                        // Load additional classes when called from thread 'TestThread'
                        // When a class is loaded during a callback handling the boot loader, we can
                        // run into ClassCircularityError if the ClassFileLoadHook is set early enough
                        // to catch classes needed during class loading, e.g.
                        //          sun.misc.URLClassPath$JarLoader$2.
                        // The goal of the test is to stress class loading on the test class loaders.

                        if (tName.equals("TestThread") && loader != null)
                        {
                                loadClasses(3);
                        }
                        return null;
                }

                public static void
                loadClasses( int index)
                {
                        ClassLoader loader = ParallelTransformerLoaderAgent.getClassLoader();
                        try
                        {
                                Class.forName("TestClass" + index, true, loader);
                        }
                        catch (Exception e)
                        {
                                e.printStackTrace();
                        }
                }
        }
}
