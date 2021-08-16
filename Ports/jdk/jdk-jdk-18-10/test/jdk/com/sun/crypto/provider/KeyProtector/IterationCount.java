/*
 * Copyright (c) 2019, Red Hat, Inc.
 *
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
 * @bug 8233404
 * @library /test/lib
 * @run main/othervm/timeout=30 IterationCount HOST 200000
 * @run main/othervm/timeout=30 IterationCount HOST 200000 1
 * @run main/othervm/timeout=30 IterationCount HOST 200000 6000000
 * @run main/othervm/timeout=30 IterationCount HOST 200000 invalid
 * @run main/othervm/timeout=30 IterationCount HOST 30000 30000
 * @run main/othervm/timeout=30 IterationCount OVERRIDE
 * @author Martin Balao (mbalao@redhat.com)
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class IterationCount {
    private static final String clientStr = "CLIENT";
    private static final String javaBinPath =
            System.getProperty("java.home", ".") + File.separator + "bin" +
                    File.separator + "java";

    public static void main(String[] args) throws Throwable {
        if (args[0].equals("HOST")) {
            String setValue = null;
            if (args.length > 2) {
                setValue = args[2];
            }
            testSystem(args[1], setValue);
            testSecurity(args[1], setValue);
        } else if (args[0].equals(clientStr)) {
            int expectedIterationCount = Integer.parseInt(args[1]);
            int currentIterationCount = getCurrentIterationCountValue();
            System.out.println("Expected value: " + expectedIterationCount);
            System.out.println("Current value: " + currentIterationCount);
            if (currentIterationCount != expectedIterationCount) {
                throw new Exception("Expected value different than current");
            }
        } else if (args[0].equals("OVERRIDE")) {
            testSystemOverridesSecurity();
        }
        System.out.println("TEST PASS - OK");
    }

    private static List<String> getBasicCommand() {
        List<String> cmd = new ArrayList<>();
        cmd.add(javaBinPath);
        cmd.add("-cp");
        cmd.add(System.getProperty("test.classes", "."));
        return cmd;
    }

    private static void executeCommand(List<String> cmd, String expectedCount)
            throws Throwable {
        cmd.add("--add-opens=java.base/com.sun.crypto.provider=ALL-UNNAMED");
        cmd.add(IterationCount.class.getName());
        cmd.add(clientStr);
        cmd.add(expectedCount);
        OutputAnalyzer out = ProcessTools.executeCommand(
                cmd.toArray(new String[cmd.size()]));
        out.shouldHaveExitValue(0);
    }

    private static void testSystem(String expectedCount, String setValue)
            throws Throwable {
        System.out.println("Test setting " +
                (setValue != null ? setValue : "nothing") +
                " as a System property");
        List<String> cmd = getBasicCommand();
        if (setValue != null) {
            cmd.add("-Djdk.jceks.iterationCount=" + setValue);
        }
        executeCommand(cmd, expectedCount);
        System.out.println(".............................");
    }

    private static void testSecurity(String expectedCount, String setValue)
            throws Throwable {
        testSecurity(expectedCount, setValue, getBasicCommand());
    }

    private static void testSecurity(String expectedCount, String setValue,
            List<String> cmd) throws Throwable {
        System.out.println("Test setting " +
            (setValue != null ? setValue : "nothing") +
            " as a Security property");
        Path tmpDirPath = Files.createTempDirectory("tmpdir");
        try {
            if (setValue != null) {
                String javaSecurityPath = tmpDirPath +
                        File.separator + "java.security";
                writeJavaSecurityProp(javaSecurityPath, setValue);
                cmd.add("-Djava.security.properties=" + javaSecurityPath);
            }
            executeCommand(cmd, expectedCount);
            System.out.println(".............................");
        } finally {
            deleteDir(tmpDirPath);
        }
    }

    private static void testSystemOverridesSecurity() throws Throwable {
        System.out.println("Test that setting a System property overrides" +
                " the Security one");
        String systemValue = Integer.toString(30000);
        System.out.println("System value: " + systemValue);
        List<String> cmd = getBasicCommand();
        cmd.add("-Djdk.jceks.iterationCount=" + systemValue);
        testSecurity(systemValue, Integer.toString(40000), cmd);
    }

    private static void writeJavaSecurityProp(String javaSecurityPath,
            String setValue) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(
                new File(javaSecurityPath))) {
            fos.write(("jdk.jceks.iterationCount=" + setValue).getBytes());
        }
    }

    private static int getCurrentIterationCountValue() throws Exception {
        Class<?> KeyProtectorClass =
                Class.forName("com.sun.crypto.provider.KeyProtector");
        Field iterationCountField =
                KeyProtectorClass.getDeclaredField("ITERATION_COUNT");
        iterationCountField.setAccessible(true);
        return iterationCountField.getInt(KeyProtectorClass);
    }

    private static void deleteDir(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
