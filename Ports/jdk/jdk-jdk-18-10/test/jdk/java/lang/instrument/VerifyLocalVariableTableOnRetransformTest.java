/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7064927
 * @summary Verify LocalVariableTable (LVT) exists when passed to transform() on a retransform operation.
 * @author Daniel D. Daugherty
 *
 * @library /test/lib
 * @run build VerifyLocalVariableTableOnRetransformTest
 * @run compile -g DummyClassWithLVT.java
 * @run shell MakeJAR.sh retransformAgent
 * @run main/othervm -javaagent:retransformAgent.jar VerifyLocalVariableTableOnRetransformTest VerifyLocalVariableTableOnRetransformTest
 */

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.ClassFileTransformer;
import java.net.*;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.regex.Pattern;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class
VerifyLocalVariableTableOnRetransformTest
    extends ATransformerManagementTestCase
{
    private byte[]  fTargetClassBytes;
    private boolean fTargetClassMatches;
    private String  fTargetClassName = "DummyClassWithLVT";
    private String  classFileName = fTargetClassName + ".class";
    private boolean fTargetClassSeen;

    /**
     * Constructor for VerifyLocalVariableTableOnRetransformTest.
     * @param name
     */
    public VerifyLocalVariableTableOnRetransformTest(String name)
    {
        super(name);

        File f = originalClassFile();
        System.out.println("Reading test class from " + f);
        try
        {
            InputStream redefineStream = new FileInputStream(f);
            fTargetClassBytes = NamedBuffer.loadBufferFromStream(redefineStream);
            System.out.println("Read " + fTargetClassBytes.length + " bytes.");
        }
        catch (IOException e)
        {
            fail("Could not load the class: " + f.getName());
        }
    }

    public static void
    main (String[] args)
        throws Throwable {
        ATestCaseScaffold   test = new VerifyLocalVariableTableOnRetransformTest(args[0]);
        test.runTest();
    }

    protected final void
    doRunTest()
        throws Throwable {
        verifyClassFileBuffer();
    }

    public void
    verifyClassFileBuffer()
        throws  Throwable
    {
        beVerbose();

        // With this call here, we will see the target class twice:
        // first when it gets loaded and second when it gets retransformed.
        addTransformerToManager(fInst, new MyObserver(), true);

        ClassLoader loader = getClass().getClassLoader();

        Class target = loader.loadClass(fTargetClassName);
        assertEquals(fTargetClassName, target.getName());

        // make an instance to prove the class was really loaded
        Object testInstance = target.newInstance();

        // With this call here, we will see the target class once:
        // when it gets retransformed.
        //addTransformerToManager(fInst, new MyObserver(), true);

        assertTrue(fTargetClassName + " was not seen by transform()",
            fTargetClassSeen);

        // The HotSpot VM hands us class file bytes at initial class
        // load time that match the .class file contents. However,
        // according to the following spec that is not required:
        // http://docs.oracle.com/javase/7/docs/api/java/lang/instrument/Instrumentation.html#retransformClasses(java.lang.Class...)
        // This test exists to catch any unintentional change in
        // behavior by the HotSpot VM. If this behavior is intentionally
        // changed in the future, then this test will need to be
        // updated.
        compareClassFileBytes(true);

        fTargetClassSeen = false;
        fTargetClassMatches = false;

        fInst.retransformClasses(target);

        assertTrue(fTargetClassName + " was not seen by transform()",
            fTargetClassSeen);

        // The HotSpot VM doesn't currently preserve the LocalVariable
        // Table (LVT) attribute so the class file bytes seen by the
        // retransformClasses() call will not match the class file bytes
        // seen at initial class load time.
        compareClassFileBytes(false);
    }

    private File originalClassFile() {
        return new File(System.getProperty("test.classes", "."), classFileName);
    }

    private File transformedClassFile() {
        // This file will get created in the test execution
        // directory so there is no conflict with the file
        // in the test classes directory.
        return new File(classFileName);
    }

    private static final String[] expectedDifferentStrings = {
            "^Classfile .+$",
            "^[\\s]+SHA-256 checksum .[^\\s]+$"
    };

    private boolean expectedDifferent(String line) {
        for (String s: expectedDifferentStrings) {
            Pattern p = Pattern.compile(s);
            if (p.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    private void compareClassFileBytes(boolean initialLoad) throws Throwable {
        if (fTargetClassMatches) {
            return;
        }
        File f1 = originalClassFile();
        File f2 = transformedClassFile();
        System.out.println(fTargetClassName + " did not match .class file");
        System.out.println("Disassembly difference (" + f1 + " vs " + f2 +"):");
        // compare 'javap -v' output for the class files
        List<String> out1 = disassembleClassFile(f1);
        List<String> out2 = disassembleClassFile(f2);
        boolean different = false;
        boolean orderChanged = false;
        int lineNum = 0;
        for (String line: out1) {
            if (!expectedDifferent(line)) {
                if (!out2.contains(line)) {
                    different = true;
                    System.out.println("< (" + (lineNum + 1) + ") " + line);
                } else {
                    if (lineNum < out2.size() && out1.get(lineNum) != out2.get(lineNum)) {
                        // out2 contains line, but at different position
                        orderChanged = true;
                    }
                }
            }
            lineNum++;
        }
        lineNum = 0;
        for (String line: out2) {
            if (!expectedDifferent(line)) {
                if (!out1.contains(line)) {
                    different = true;
                    System.out.println("> (" + (lineNum + 1) + ") " + line);
                }
            }
            lineNum++;
        }
        // accordingly the spec orderChanged is fine, but we consider it as error
        // (see comments in verifyClassFileBuffer())
        if (different || orderChanged) {
            fail(fTargetClassName + " (" + (initialLoad ? "load" : "retransform") + ") did not match .class file"
                    + (different ? "" : " (only order changed)"));
        }
    }

    private List<String> disassembleClassFile(File file) throws Throwable {
        JDKToolLauncher javap = JDKToolLauncher.create("javap")
                                               .addToolArg("-v")
                                               .addToolArg(file.toString());
        ProcessBuilder pb = new ProcessBuilder(javap.getCommand());
        OutputAnalyzer out = ProcessTools.executeProcess(pb);
        return out.asLines();
    }

    public class MyObserver implements ClassFileTransformer {
        public MyObserver() {
        }

        public String toString() {
            return MyObserver.this.getClass().getName();
        }

        private void saveMismatchedBytes(byte[] classfileBuffer) {
            try {
                FileOutputStream fos = new FileOutputStream(transformedClassFile());
                fos.write(classfileBuffer);
                fos.close();
            } catch (IOException ex) {
            }
        }

        public byte[]
        transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain    protectionDomain,
            byte[] classfileBuffer) {

            System.out.println(this + ".transform() sees '" + className
                + "' of " + classfileBuffer.length + " bytes.");
            if (className.equals(fTargetClassName)) {
                fTargetClassSeen = true;

                if (classfileBuffer.length != fTargetClassBytes.length) {
                    System.out.println("Warning: " + fTargetClassName
                        + " lengths do not match.");
                    fTargetClassMatches = false;
                    saveMismatchedBytes(classfileBuffer);
                    return null;
                } else {
                    System.out.println("Info: " + fTargetClassName
                        + " lengths match.");
                }

                for (int i = 0; i < classfileBuffer.length; i++) {
                    if (classfileBuffer[i] != fTargetClassBytes[i]) {
                        System.out.println("Warning: " + fTargetClassName
                            + "[" + i + "]: '" + classfileBuffer[i]
                            + "' != '" + fTargetClassBytes[i] + "'");
                        fTargetClassMatches = false;
                        saveMismatchedBytes(classfileBuffer);
                        return null;
                    }
                }

                fTargetClassMatches = true;
                System.out.println("Info: verified '" + fTargetClassName
                    + ".class' matches 'classfileBuffer'.");
            }

            return null;
        }
    }
}
