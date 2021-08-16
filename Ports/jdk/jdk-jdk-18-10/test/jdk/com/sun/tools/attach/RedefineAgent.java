/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * @bug 6439234 6446941
 *
 * Test attach used in concert with transformers and redefineClasses.
 *
 * 6439234 java.lang.instrument: 8 JCK JPLIS tests fail when running in Live phase (no transform events)
 * 6446941 java.lang.instrument: multiple agent attach fails (first agent chooses capabilities)
 */
import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.util.Arrays;
import java.security.ProtectionDomain;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.ClassDefinition;

public class RedefineAgent implements ClassFileTransformer {

    static byte[] classfilebytes;
    static final String targetName = "RedefineDummy";
    static final String targetNameSlashes = "RedefineDummy";
    static boolean gotRedefineTransform = false;

    // test transform and capture class bytes for redefine
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain  protectionDomain,
                            byte[] classfileBuffer) {
        if (className.equals(targetNameSlashes)) {
            if (classBeingRedefined == null) {
                System.out.println("Got bytes for: " + className);
                classfilebytes = Arrays.copyOf(classfileBuffer, classfileBuffer.length);
            } else {
                gotRedefineTransform = true;
            }
        }
        return null;
    }

    // test transform and redefine for an attached agent
    public static void testRedefine(Instrumentation inst) throws Exception {
        Class[] classes = inst.getAllLoadedClasses();
        for (Class k : classes) {
            if (k.getName().equals(targetName)) {
                throw new Exception("RedefineAgent Test error: class " + targetName + " has already been loaded.");
            }
        }
        inst.addTransformer(new RedefineAgent());
        ClassLoader.getSystemClassLoader().loadClass(targetName);
        classes = inst.getAllLoadedClasses();
        Class targetClass = null;
        for (Class k : classes) {
            if (k.getName().equals(targetName)) {
                targetClass = k;
                break;
            }
        }
        if (targetClass == null) {
            throw new Exception("RedefineAgent Test error: class " + targetName + " not loaded.");
        }
        if (classfilebytes == null) {
            throw new Exception("RedefineAgent Error(6439234): no transform call for class " + targetName);
        }
        ClassDefinition cd = new ClassDefinition(targetClass, classfilebytes);
        inst.redefineClasses(cd);
        System.out.println("RedefineAgent did redefine.");
        if (gotRedefineTransform) {
            System.out.println("RedefineAgent got redefine transform.");
        } else {
            throw new Exception("RedefineAgent Error(6439234): no transform call for redefine " + targetName);
        }
    }

    public static void agentmain(String args, Instrumentation inst) throws Exception {
        System.out.println("RedefineAgent running...");
        System.out.println("RedefineAgent redefine supported: " + inst.isRedefineClassesSupported());
        int port = Integer.parseInt(args);
        System.out.println("RedefineAgent connecting back to Tool....");
        Socket s = new Socket();
        s.connect( new InetSocketAddress(port) );
        System.out.println("RedefineAgent connected to Tool.");

        testRedefine(inst);

        s.close();
    }

}
