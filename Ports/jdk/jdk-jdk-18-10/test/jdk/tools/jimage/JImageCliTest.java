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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static jdk.test.lib.Asserts.assertFalse;
import static jdk.test.lib.Asserts.assertTrue;
import static jdk.test.lib.Asserts.fail;

/**
 * This class is intended to be a base class for classes which are about to test
 * command line interface of jimage.
 */
public class JImageCliTest {

    private String bootImagePath;

    public JImageCliTest() {
        Path imagePath = Paths.get(System.getProperty("java.home"), "lib", "modules");
        if (Files.exists(imagePath)) {
            this.bootImagePath = imagePath.toAbsolutePath().toString();
        }
    }

    public void assertMatches(String regex, String output) {
        Pattern pattern = Pattern.compile(regex);
        if (!pattern.matcher(output).find()) {
            fail(String.format("Expected to find a string match for [%s] in output \n[\n%s\n]\n.",
                    pattern, output));
        }
    }

    /**
     * Returns a path to a tested image to share it across tests. By default it returns a path to the boot image
     * of tested JDK. This behavior can be redefined by descendants.
     */
    public String getImagePath() {
        return bootImagePath;
    }

    /**
     * Runs jimage task with the supplied arguments.
     */
    protected static JImageResult jimage(String... args) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.out.println("jimage " + Arrays.asList(args));
        int exitCode = jdk.tools.jimage.Main.run(args, new PrintWriter(ps));
        return new JImageResult(exitCode, new String(baos.toByteArray(), UTF_8));
    }

    protected static class JImageResult {
        final int exitCode;
        final String output;

        JImageResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        JImageResult assertSuccess() { assertTrue(exitCode == 0, output); return this; }
        JImageResult assertFailure() { assertFalse(exitCode == 0, output); return this; }

        // a helper to ensure the error output doesn't exhibit implementation details
        JImageResult assertShowsError() {
            assertTrue(output.contains("Error"),
                    String.format("Output contains error, output=[%s]\n", output));
            assertFalse(output.contains("Exception"),
                    String.format("Output doesn't contain a stacktrace, output=[%s]\n", output));
            return this;
        }

        JImageResult resultChecker(Consumer<JImageResult> r) { r.accept(this); return this; }
    }

    protected final void runTests() throws Throwable {
        if (getImagePath() != null) {
            for (Method m : getClass().getDeclaredMethods()) {
                if (m.getName().startsWith("test")) {
                    System.out.printf("Invoking %s\n", m.getName());
                    m.invoke(this);
                }
            }
        } else {
            System.out.println("This is not an image build. Skipping.");
        }
    }

}

