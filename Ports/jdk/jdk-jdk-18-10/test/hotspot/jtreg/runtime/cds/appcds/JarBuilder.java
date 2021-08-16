/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @summary Simple jar builder
 *   Input: jarName className1 className2 ...
 *     do not specify extensions, just the names
 *     E.g. prot_domain ProtDomainA ProtDomainB
 *   Output: A jar containing compiled classes, placed in a test classes folder
 * @library /open/test/lib
 */

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.helpers.ClassFileInstaller;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.spi.ToolProvider;

public class JarBuilder {
    // to turn DEBUG on via command line: -DJarBuilder.DEBUG=[true, TRUE]
    private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("JarBuilder.DEBUG", "false"));
    private static final String classDir = System.getProperty("test.classes");
    private static final ToolProvider JAR = ToolProvider.findFirst("jar")
        .orElseThrow(() -> new RuntimeException("ToolProvider for jar not found"));

    public static String getJarFilePath(String jarName) {
        return CDSTestUtils.getOutputDir() +  File.separator + jarName + ".jar";
    }

    // jar all files under dir, with manifest file man, with an optional versionArgs
    // for generating a multi-release jar.
    // The jar command is as follows:
    // jar cmf \
    //  <path to output jar> <path to the manifest file>\
    //   -C <path to the base classes> .\
    //    --release 9 -C <path to the versioned classes> .
    // the last line begins with "--release" corresponds to the optional versionArgs.
    public static String build(String jarName, File dir, String man, String ...versionArgs)
        throws Exception {
        ArrayList<String> args = new ArrayList<String>();
        if (man != null) {
            args.add("cfm");
        } else {
            args.add("cf");
        }
        String jarFile = getJarFilePath(jarName);
        args.add(jarFile);
        if (man != null) {
            args.add(man);
        }
        args.add("-C");
        args.add(dir.getAbsolutePath());
        args.add(".");
        for (String verArg : versionArgs) {
            args.add(verArg);
        }
        createJar(args);
        return jarFile;
    }

    public static String build(String jarName, String ...classNames)
        throws Exception {

        return createSimpleJar(classDir, getJarFilePath(jarName), classNames);
    }

    public static String build(boolean classesInWorkDir, String jarName, String ...classNames)
        throws Exception {
        if (classesInWorkDir) {
            return createSimpleJar(".", getJarFilePath(jarName), classNames);
        } else {
            return build(jarName, classNames);
        }
    }


    public static String buildWithManifest(String jarName, String manifest,
        String jarClassesDir, String ...classNames) throws Exception {
        String jarPath = getJarFilePath(jarName);
        ArrayList<String> args = new ArrayList<String>();
        args.add("cvfm");
        args.add(jarPath);
        args.add(System.getProperty("test.src") + File.separator + "test-classes"
            + File.separator + manifest);
        addClassArgs(args, jarClassesDir, classNames);
        createJar(args);

        return jarPath;
    }


    // Execute: jar uvf $jarFile -C $dir .
    static void update(String jarFile, String dir) throws Exception {
        String jarExe = JDKToolFinder.getJDKTool("jar");

        ArrayList<String> args = new ArrayList<>();
        args.add(jarExe);
        args.add("uvf");
        args.add(jarFile);
        args.add("-C");
        args.add(dir);
        args.add(".");

        executeProcess(args.toArray(new String[1]));
    }

    // Add commonly used inner classes that are often omitted by mistake. Currently
    // we support only jdk/test/whitebox/WhiteBox$WhiteBoxPermission and
    // sun/hotspot/WhiteBox$WhiteBoxPermission. See JDK-8199290
    private static String[] addInnerClasses(String[] classes, int startIdx) {
        boolean seenNewWb = false;
        boolean seenNewWbInner = false;
        boolean seenOldWb = false;
        boolean seenOldWbInner = false;
        // This method is different than ClassFileInstaller.addInnerClasses which
        // uses "." as the package delimiter :-(
        final String newWb = "jdk/test/whitebox/WhiteBox";
        final String newWbInner = newWb + "$WhiteBoxPermission";
        final String oldWb = "sun/hotspot/WhiteBox";
        final String oldWbInner = oldWb + "$WhiteBoxPermission";

        ArrayList<String> list = new ArrayList<>();

        for (int i = startIdx; i < classes.length; i++) {
            String cls = classes[i];
            list.add(cls);
            switch (cls) {
            case newWb:      seenNewWb      = true; break;
            case newWbInner: seenNewWbInner = true; break;
            case oldWb:      seenOldWb      = true; break;
            case oldWbInner: seenOldWbInner = true; break;
            }
        }
        if (seenNewWb && !seenNewWbInner) {
            list.add(newWbInner);
        }
        if (seenOldWb && !seenOldWbInner) {
            list.add(oldWbInner);
        }

        String[] array = new String[list.size()];
        list.toArray(array);
        return array;
    }


    private static String createSimpleJar(String jarclassDir, String jarName,
        String[] classNames) throws Exception {

        ArrayList<String> args = new ArrayList<String>();
        args.add("cf");
        args.add(jarName);
        addClassArgs(args, jarclassDir, classNames);
        createJar(args);

        return jarName;
    }

    private static void addClassArgs(ArrayList<String> args, String jarclassDir,
        String[] classNames) {

        classNames = addInnerClasses(classNames, 0);

        for (String name : classNames) {
            args.add("-C");
            args.add(jarclassDir);
            args.add(name + ".class");
        }
    }

    public static void createModularJar(String jarPath,
                                      String classesDir,
                                      String mainClass) throws Exception {
        ArrayList<String> argList = new ArrayList<String>();
        argList.add("--create");
        argList.add("--file=" + jarPath);
        if (mainClass != null) {
            argList.add("--main-class=" + mainClass);
        }
        argList.add("-C");
        argList.add(classesDir);
        argList.add(".");
        createJar(argList);
    }

    private static void createJar(ArrayList<String> args) {
        if (DEBUG) printIterable("createJar args: ", args);

        if (JAR.run(System.out, System.err, args.toArray(new String[1])) != 0) {
            throw new RuntimeException("jar operation failed");
        }
    }

    // Many AppCDS tests use the same simple "hello.jar" which contains
    // simple Hello.class and does not specify additional attributes.
    // For this common use case, use this method to get the jar path.
    // The method will check if the jar already exists
    // (created by another test or test run), and will create the jar
    // if it does not exist
    public static String getOrCreateHelloJar() throws Exception {
        String jarPath = getJarFilePath("hello");

        File jarFile = new File(jarPath);
        if (jarFile.exists()) {
            return jarPath;
        } else {
            return build("hello", "Hello");
        }
    }

    public static void compile(String dstPath, String source, String... extraArgs) throws Exception {
        ArrayList<String> args = new ArrayList<String>();
        args.add(JDKToolFinder.getCompileJDKTool("javac"));
        args.add("-d");
        args.add(dstPath);
        if (extraArgs != null) {
            for (String s : extraArgs) {
                args.add(s);
            }
        }
        args.add(source);

        if (DEBUG) printIterable("compile args: ", args);

        ProcessBuilder pb = new ProcessBuilder(args);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
    }

    public static void compileModule(Path src,
                                     Path dest,
                                     String modulePathArg // arg to --module-path
                                     ) throws Exception {
        boolean compiled = false;
        if (modulePathArg == null) {
            compiled = CompilerUtils.compile(src, dest);
        } else {
            compiled = CompilerUtils.compile(src, dest,
                                           "--module-path", modulePathArg);
        }
        if (!compiled) {
            throw new RuntimeException("module did not compile");
        }
    }


    public static void signJar() throws Exception {
        String keyTool = JDKToolFinder.getJDKTool("keytool");
        String jarSigner = JDKToolFinder.getJDKTool("jarsigner");

        executeProcess(keyTool,
            "-genkey", "-keystore", "./keystore", "-alias", "mykey",
            "-storepass", "abc123", "-keypass", "abc123", "-keyalg", "dsa",
            "-dname", "CN=jvmtest")
            .shouldHaveExitValue(0);

        executeProcess(jarSigner,
           "-keystore", "./keystore", "-storepass", "abc123", "-keypass",
           "abc123", "-signedjar", getJarFilePath("signed_hello"),
           getJarFilePath("hello"), "mykey")
           .shouldHaveExitValue(0);
    }

    private static OutputAnalyzer executeProcess(String... cmds)
        throws Exception {

        JarBuilder.printArray("executeProcess: ", cmds);
        return ProcessTools.executeProcess(new ProcessBuilder(cmds));
    }

    // diagnostic
    public static void printIterable(String msg, Iterable<String> l) {
        StringBuilder sum = new StringBuilder();
        for (String s : l) {
            sum.append(s).append(' ');
        }
        System.out.println(msg + sum.toString());
    }

    public static void printArray(String msg, String[] l) {
        StringBuilder sum = new StringBuilder();
        for (String s : l) {
            sum.append(s).append(' ');
        }
        System.out.println(msg + sum.toString());
    }
}
