/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.io.File;
import java.io.FileOutputStream;
import jdk.test.lib.process.OutputAnalyzer;
import java.nio.file.Files;

import java.util.*;
import jdk.internal.org.objectweb.asm.*;

/**
 * The testsets contained in this class are executed by ./VerifierTest_*.java, so that
 * individual testsets can be executed in parallel to shorten the total time required.
 */
public class VerifierTest implements Opcodes {
    // Test verification settings for dumping & runtime
    static final String VFY_ALL = "-Xverify:all";
    static final String VFY_REMOTE = "-Xverify:remote"; // default
    static final String VFY_NONE = "-XX:+UnlockDiagnosticVMOptions, -XX:-BytecodeVerificationRemote, -XX:-BytecodeVerificationLocal";

    static final String ERR =
        "ERROR: class VerifierTestC was loaded unexpectedly";
    static final String MAP_FAIL_VFY_LOCAL =
        "shared archive file's BytecodeVerificationLocal setting";
    static final String VFY_ERR = "java.lang.VerifyError";
    static final String PASS_RESULT = "Hi, how are you?";
    static final String VFY_INFO_MESSAGE =
        "All non-system classes will be verified (-Xverify:remote) during CDS dump time.";
    static final String CDS_LOGGING = "-Xlog:cds,cds+hashtables";

    enum Testset1Part {
        A, B
    }

    public static void main(String[] args) throws Exception {
        String subCaseId = args[0];
        String jarName_verifier_test_tmp = "verifier_test_tmp" + "_" + subCaseId;
        String jarName_verifier_test = "verifier_test" + "_" + subCaseId;
        String jarName_greet = "greet" + "_" + subCaseId;
        String jarName_hi = "hi" + "_" + subCaseId;


        File jarSrcFile = new File(JarBuilder.build(jarName_verifier_test_tmp, "VerifierTest0", "VerifierTestA",
                         "VerifierTestB", "VerifierTestC", "VerifierTestD", "VerifierTestE",
                         "UnverifiableBase", "UnverifiableIntf", "UnverifiableIntfSub"));
        JarBuilder.build(jarName_greet, "Greet");
        JarBuilder.build(jarName_hi, "Hi", "Hi$MyClass");

        File jarFile = new File(JarBuilder.getJarFilePath(jarName_verifier_test));
        String jar = jarFile.getPath();

        if (!jarFile.exists() || jarFile.lastModified() < jarSrcFile.lastModified()) {
            createTestJarFile(jarSrcFile, jarFile);
        } else {
            System.out.println("Already up-to-date: " + jarFile);
        }

        String noAppClasses[] = TestCommon.list("");
        String appClasses[] = TestCommon.list("UnverifiableBase",
                                              "UnverifiableIntf",
                                              "UnverifiableIntfSub",
                                              "VerifierTestA",
                                              "VerifierTestB",
                                              "VerifierTestC",
                                              "VerifierTestD",
                                              "VerifierTestE",
                                              "VerifierTest0");


        switch (subCaseId) {
        case "0":         testset_0(jar, noAppClasses, appClasses);                 return;
        case "1A":        testset_1(jar, noAppClasses, appClasses, Testset1Part.A); return;
        case "1B":        testset_1(jar, noAppClasses, appClasses, Testset1Part.B); return;
        case "2":         testset_2(jarName_greet, jarName_hi);                   return;
        default:
            throw new RuntimeException("Unknown option: " + subCaseId);
        }
    }

    static void testset_0(String jar, String[] noAppClasses, String[] appClasses) throws Exception {
        // Unverifiable classes won't be included in the CDS archive.
        // Dumping should not fail.
        OutputAnalyzer output = TestCommon.dump(jar, appClasses);
        output.shouldHaveExitValue(0);
        if (output.getStdout().contains("Loading clases to share")) {
            // last entry in appClasses[] is a verifiable class
            for (int i = 0; i < (appClasses.length - 1); i++) {
                output.shouldContain("Verification failed for " + appClasses[i]);
                output.shouldContain("Removed error class: " + appClasses[i]);
            }
        }
    }

    static void checkRuntimeOutput(OutputAnalyzer output, String expected) throws Exception {
        output.shouldContain(expected);
        if (expected.equals(PASS_RESULT) ||
            expected.equals(VFY_ERR)) {
            output.shouldHaveExitValue(0);
        } else {
            output.shouldNotHaveExitValue(0);
        }
    }

    static void testset_1(String jar, String[] noAppClasses, String[] appClasses, Testset1Part part)
        throws Exception
    {
        String config[][] = {
            // {dump_list, dumptime_verification_setting,
            //  runtime_verification_setting, expected_output_str},

            // Dump app/ext with -Xverify:remote
            {"app",   VFY_REMOTE, VFY_REMOTE, VFY_ERR},
            {"app",   VFY_REMOTE, VFY_ALL,    MAP_FAIL_VFY_LOCAL},
            {"app",   VFY_REMOTE, VFY_NONE,   ERR },
            // Dump app/ext with -Xverify:all
            {"app",   VFY_ALL,    VFY_REMOTE, VFY_ERR },
            {"app",   VFY_ALL,    VFY_ALL,    VFY_ERR },
            {"app",   VFY_ALL,    VFY_NONE,   ERR },
            // Dump app/ext with verifier turned off
            {"app",   VFY_NONE,   VFY_REMOTE, VFY_ERR},
            {"app",   VFY_NONE,   VFY_ALL,    MAP_FAIL_VFY_LOCAL},
            {"app",   VFY_NONE,   VFY_NONE,   ERR },
            // Dump sys only with -Xverify:remote
            {"noApp", VFY_REMOTE, VFY_REMOTE, VFY_ERR},
            {"noApp", VFY_REMOTE, VFY_ALL,    MAP_FAIL_VFY_LOCAL},
            {"noApp", VFY_REMOTE, VFY_NONE,   ERR},
            // Dump sys only with -Xverify:all
            {"noApp", VFY_ALL, VFY_REMOTE,    VFY_ERR},
            {"noApp", VFY_ALL, VFY_ALL,       VFY_ERR},
            {"noApp", VFY_ALL, VFY_NONE,      ERR},
            // Dump sys only with verifier turned off
            {"noApp", VFY_NONE, VFY_REMOTE,   VFY_ERR},
            {"noApp", VFY_NONE, VFY_ALL,      MAP_FAIL_VFY_LOCAL},
            {"noApp", VFY_NONE, VFY_NONE,     ERR},
        };

        int loop_start, loop_stop;

        // Further break down testset_1 into two parts (to be invoked from VerifierTest_1A.java
        // and VerifierTest_1B.java) to improve parallel test execution time.
        switch (part) {
        case A:
            loop_start = 0;
            loop_stop  = 9;
            break;
        case B:
        default:
            assert part == Testset1Part.B;
            loop_start = 9;
            loop_stop  = config.length;
            break;
        }

        String prev_dump_setting = "";
        for (int i = loop_start; i < loop_stop; i ++) {
            String dump_list[] = config[i][0].equals("app") ? appClasses :
                noAppClasses;
            String dump_setting = config[i][1];
            String runtime_setting = config[i][2];
            String expected_output_str = config[i][3];
            System.out.println("Test case [" + i + "]: dumping " + config[i][0] +
                               " with " + dump_setting +
                               ", run with " + runtime_setting);
            if (!dump_setting.equals(prev_dump_setting)) {
                String dump_arg1;
                String dump_arg2;
                String dump_arg3;
                // Need to break this into two separate arguments.
                if (dump_setting.equals(VFY_NONE)) {
                    dump_arg1 = "-XX:+UnlockDiagnosticVMOptions";
                    dump_arg2 = "-XX:-BytecodeVerificationRemote";
                    dump_arg3 = "-XX:-BytecodeVerificationLocal";
                } else {
                    // Redundant args should be harmless.
                    dump_arg1 = dump_arg2 = dump_arg3 = dump_setting;
                }

                OutputAnalyzer dumpOutput = TestCommon.dump(
                                                            jar, dump_list, dump_arg1, dump_arg2,
                                                            dump_arg3, CDS_LOGGING,
                                                            // FIXME: the following options are for working around a GC
                                                            // issue - assert failure when dumping archive with the -Xverify:all
                                                            "-Xms256m",
                                                            "-Xmx256m");
                if (dump_setting.equals(VFY_NONE) &&
                    runtime_setting.equals(VFY_REMOTE)) {
                    dumpOutput.shouldContain(VFY_INFO_MESSAGE);
                }
            }
            String runtime_arg1;
            String runtime_arg2;
            String runtime_arg3;
            if (runtime_setting.equals(VFY_NONE)) {
                runtime_arg1 = "-XX:+UnlockDiagnosticVMOptions";
                runtime_arg2 = "-XX:-BytecodeVerificationRemote";
                runtime_arg3 = "-XX:-BytecodeVerificationLocal";
            } else {
                // Redundant args should be harmless.
                runtime_arg1 = runtime_arg2 = runtime_arg3 = runtime_setting;
            }
            TestCommon.run("-cp", jar,
                           runtime_arg1, runtime_arg2, runtime_arg3,
                           "VerifierTest0")
                .ifNoMappingFailure(output -> checkRuntimeOutput(output, expected_output_str));
            prev_dump_setting = dump_setting;
        }
    }

    static void testset_2(String jarName_greet, String jarName_hi) throws Exception {
        String appClasses[];
        String jar;

        // The following section is for testing the scenarios where
        // the classes are verifiable during dump time.
        appClasses = TestCommon.list("Hi",
                                     "Greet",
                                     "Hi$MyClass");
        jar = TestCommon.getTestJar(jarName_hi + ".jar") + File.pathSeparator +
            TestCommon.getTestJar(jarName_greet + ".jar");
        String config2[][] = {
            // {dump_list, dumptime_verification_setting,
            //  runtime_verification_setting, expected_output_str},

            // Dump app/ext with -Xverify:remote
            {"app",   VFY_REMOTE, VFY_REMOTE, PASS_RESULT},
            {"app",   VFY_REMOTE, VFY_ALL,    MAP_FAIL_VFY_LOCAL},
            {"app",   VFY_REMOTE, VFY_NONE,   PASS_RESULT },
            // Dump app/ext with -Xverify:all
            {"app",   VFY_ALL,    VFY_REMOTE, PASS_RESULT },
            {"app",   VFY_ALL,    VFY_ALL,    PASS_RESULT },
            {"app",   VFY_ALL,    VFY_NONE,   PASS_RESULT },
            // Dump app/ext with verifier turned off
            {"app",   VFY_NONE,   VFY_REMOTE, PASS_RESULT},
            {"app",   VFY_NONE,   VFY_ALL,    MAP_FAIL_VFY_LOCAL},
            {"app",   VFY_NONE,   VFY_NONE,   PASS_RESULT },
        };
        String prev_dump_setting = "";
        for (int i = 0; i < config2.length; i ++) {
            // config2[i][0] is always set to "app" in this test
            String dump_setting = config2[i][1];
            String runtime_setting = config2[i][2];
            String expected_output_str = config2[i][3];
            System.out.println("Test case [" + i + "]: dumping " + config2[i][0] +
                               " with " + dump_setting +
                               ", run with " + runtime_setting);
            if (!dump_setting.equals(prev_dump_setting)) {
                String dump_arg1;
                String dump_arg2;
                String dump_arg3;
                if (dump_setting.equals(VFY_NONE)) {
                    dump_arg1 = "-XX:+UnlockDiagnosticVMOptions";
                    dump_arg2 = "-XX:-BytecodeVerificationRemote";
                    dump_arg3 = "-XX:-BytecodeVerificationLocal";
                } else {
                    // Redundant args should be harmless.
                    dump_arg1 = dump_arg2 = dump_arg3 = dump_setting;
                }
                OutputAnalyzer dumpOutput = TestCommon.dump(
                                                            jar, appClasses, dump_arg1, dump_arg2,
                                                            dump_arg3, CDS_LOGGING,
                                                            // FIXME: the following options are for working around a GC
                                                            // issue - assert failure when dumping archive with the -Xverify:all
                                                            "-Xms256m",
                                                            "-Xmx256m");
                if (dump_setting.equals(VFY_NONE) &&
                    runtime_setting.equals(VFY_REMOTE)) {
                    dumpOutput.shouldContain(VFY_INFO_MESSAGE);
                }
            }
            String runtime_arg1;
            String runtime_arg2;
            String runtime_arg3;
            if (runtime_setting.equals(VFY_NONE)) {
                runtime_arg1 = "-XX:+UnlockDiagnosticVMOptions";
                runtime_arg2 = "-XX:-BytecodeVerificationRemote";
                runtime_arg3 = "-XX:-BytecodeVerificationLocal";
            } else {
                // Redundant args should be harmless.
                runtime_arg1 = runtime_arg2 = runtime_arg3 = runtime_setting;
            }
            TestCommon.run("-cp", jar,
                           runtime_arg1, runtime_arg2, runtime_arg3,
                           "Hi")
                .ifNoMappingFailure(output -> checkRuntimeOutput(output, expected_output_str));
           prev_dump_setting = dump_setting;
        }
    }

    static void createTestJarFile(File jarSrcFile, File jarFile) throws Exception {
        jarFile.delete();
        Files.copy(jarSrcFile.toPath(), jarFile.toPath());

        File dir = new File(System.getProperty("test.classes", "."));
        File outdir = new File(dir, "verifier_test_classes");
        outdir.mkdir();

        writeClassFile(new File(outdir, "UnverifiableBase.class"), makeUnverifiableBase());
        writeClassFile(new File(outdir, "UnverifiableIntf.class"), makeUnverifiableIntf());

        JarBuilder.update(jarFile.getPath(), outdir.getPath());
    }

    static void writeClassFile(File file, byte bytecodes[]) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytecodes);
        }
    }

    // This was obtained using JDK8: java jdk.internal.org.objectweb.asm.util.ASMifier tmpclasses/UnverifiableBase.class
    static byte[] makeUnverifiableBase() throws Exception {
        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        cw.visit(V1_8, ACC_SUPER, "UnverifiableBase", null, "java/lang/Object", null);
        {
            fv = cw.visitField(ACC_FINAL + ACC_STATIC, "x", "LVerifierTest;", null, null);
            fv.visitEnd();
        }
        {
            mv = cw.visitMethod(0, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            mv.visitTypeInsn(NEW, "VerifierTest0");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "VerifierTest0", "<init>", "()V", false);
            mv.visitFieldInsn(PUTSTATIC, "UnverifiableBase", "x", "LVerifierTest;");
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 0);
            mv.visitEnd();
        }
        addBadMethod(cw);
        cw.visitEnd();

        return cw.toByteArray();
    }

    // This was obtained using JDK8: java jdk.internal.org.objectweb.asm.util.ASMifier tmpclasses/UnverifiableIntf.class
    static byte[] makeUnverifiableIntf() throws Exception {
        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        cw.visit(V1_8, ACC_ABSTRACT + ACC_INTERFACE, "UnverifiableIntf", null, "java/lang/Object", null);

        {
            fv = cw.visitField(ACC_PUBLIC + ACC_FINAL + ACC_STATIC, "x", "LVerifierTest0;", null, null);
            fv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            mv.visitTypeInsn(NEW, "VerifierTest0");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "VerifierTest0", "<init>", "()V", false);
            mv.visitFieldInsn(PUTSTATIC, "UnverifiableIntf", "x", "LVerifierTest0;");
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 0);
            mv.visitEnd();
        }
        addBadMethod(cw);
        cw.visitEnd();

        return cw.toByteArray();
    }

    // Add a bad method to make the class fail verification.
    static void addBadMethod(ClassWriter cw) throws Exception {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "bad", "()V", null, null);
        mv.visitCode();
        mv.visitInsn(ARETURN); //  java.lang.VerifyError: Operand stack underflow
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }
}
