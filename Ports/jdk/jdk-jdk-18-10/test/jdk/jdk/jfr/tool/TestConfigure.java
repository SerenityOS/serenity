/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.tool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jdk.jfr.Configuration;
import jdk.test.lib.process.OutputAnalyzer;
/**
 * @test
 * @summary Test jfr configure
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.tool.TestConfigure
 */
public class TestConfigure {

    private static final String DIR = System.getProperty("test.src", ".");

    public static void main(String... args) throws Throwable {
        testSelection();
        testText();
        testFlag();
        testCondition();
        testAnd();
        testOr();
        testNone();
        testMissingControl();
        testDefault();
        testCopyPredefined();
        testSuperflouos();
        testModify();
        testAdding();
        testUnification();
        testTimespan();
        testVerbose();
    }

    private static void testVerbose() throws Throwable {
        var input = newInputFile("flag.jfc");

        var output = newOutputFile("verbose-1.jfc");
        var result = jfrConfigure("--input", input, "--verbose", "mammal=true", "--output", output);
        result.shouldContain("com.example.Lion#enabled=true");
        result.shouldContain("com.example.Tiger#enabled=true");

        output = newOutputFile("verbose-2.jfc");
        result = jfrConfigure("--input", input, "--verbose", "+com.example.Albatross#enabled=true", "--output",  output);
        result.shouldContain("com.example.Albatross#enabled=true");
    }

    private static void testTimespan() throws Throwable {
        var input = newInputFile("timespan.jfc");

        var output = newOutputFile("quoted-timespan.jfc");
        jfrConfigure("--input", input, "value=20 s","--output", output);
        var outputSetting = readSettings(output);
        var expected = readSettings(input);
        expected.put("com.example.Tiger#threshold", "20 s");
        expected.put("com.example.Lion#period", "20 s");

        aseertEqual(outputSetting, expected);

        output = newOutputFile("compact-timespan.jfc");
        jfrConfigure("--input", input, "value=13s","--output", output);
        outputSetting = readSettings(output);
        expected = readSettings(input);
        expected.put("com.example.Tiger#threshold", "13 s");
        expected.put("com.example.Lion#period", "13 s");

        aseertEqual(outputSetting, expected);

        output = newOutputFile("threshold-period-timespan.jfc");
        jfrConfigure("--input", input,
                "com.example.Tiger#threshold=2s",
                "com.example.Lion#period=3s",
                "--output", output);

        outputSetting = readSettings(output);
        expected = readSettings(input);
        expected.put("com.example.Tiger#threshold", "2 s");
        expected.put("com.example.Lion#period", "3 s");

        aseertEqual(outputSetting, expected);
    }

    private static void testUnification() throws Throwable {
        var input1 = newInputFile("or.jfc");
        var input2 = newInputFile("and.jfc");
        var output = newOutputFile("combined.jfc");

        jfrConfigure("--input", input1 + "," + input2, "--output", output);

        var input1Setting = readSettings(input1);
        var input2Setting = readSettings(input2);
        var outputSetting = readSettings(output);

        Map<String, String> expected = new HashMap<>();
        expected.putAll(input1Setting);
        expected.putAll(input2Setting);

        aseertEqual(outputSetting, expected);
    }

    private static void testAdding() throws Throwable {
        var input = newInputFile("plain.jfc");

        var output = newOutputFile("test-adding-succeed-1.jfc");
        var result = jfrConfigure("--input", input, "+com.example.Tiger#legs=4", "--output", output);
        result.shouldNotContain("Could not find");
        var outputSetting = readSettings(output);
        var expected = readSettings(input);
        expected.put("com.example.Tiger#legs", "4");

        aseertEqual(outputSetting, expected);

        output = newOutputFile("test-adding-succeed-2.jfc");
        result = jfrConfigure("--input", input, "+com.example.Foo#bar=baz", "--output", output);
        result.shouldNotContain("Could not find");

        outputSetting = readSettings(output);
        expected = readSettings(input);
        expected.put("com.example.Foo#bar", "baz");

        aseertEqual(outputSetting, expected);
    }

    private static void testModify() throws Throwable {
        var input = newInputFile("plain.jfc");

        var output = newOutputFile("test-modify-fail-1.jfc");
        var result = jfrConfigure("--input", input, "com.example.Zebra#stackTrace=true", "--output", output);
        result.shouldContain("Could not find event 'com.example.Zebra'");

        output = newOutputFile("test-modify-fail-2.jfc");
        result = jfrConfigure("--input", input, "com.example.Tiger#foo=true", "--output", output);
        result.shouldContain("Could not find setting 'foo' for event 'com.example.Tiger'");

        output = newOutputFile("test-modify-succeed.jfc");
        result = jfrConfigure("--input", input, "com.example.Tiger#enabled=true", "--output", output);
        result.shouldNotContain("Could not find");

        var outputSetting = readSettings(output);
        var expected = readSettings(input);
        expected.put("com.example.Tiger#enabled", "true");

        aseertEqual(outputSetting, expected);
    }

    // JMC may add attributes or elements, make sure some random elements/attributes survive
    private static void testSuperflouos() throws Throwable {
        var output = newOutputFile("test-superfluous.jfc");
        var input = newInputFile("superfluous.jfc");
        jfrConfigure("--input", input, "--output", output);
        String content = Files.readString(Path.of(output));
        for (String t : List.of("legs=\"4\"", "radio", "red", "</radio>", "option")) {
            if (!content.contains(t)) {
                throw new Exception("Expected superfluous element '" + t + "' or attribute to survive");
            }
        }
    }

    private static void testMissingControl() throws Throwable {
        var output = newOutputFile("missed.jfc");
        var input = newInputFile("missing.jfc");
        var result = jfrConfigure("--input", input, "--output", output);
        result.shouldContain("Warning! Setting 'com.example.Tiger#enabled' refers to missing control 'tigre'");
     }

    private static void testDefault() throws Throwable {
        var output = newOutputFile("fresh.jfc");
        var result = jfrConfigure("--output", output);
        result.shouldNotContain("Warning"); // checks dangling control reference in default.jfc
        var outputSetting = readSettings(output);
        aseertEqual(outputSetting, Configuration.getConfiguration("default").getSettings());
    }

    private static void testCopyPredefined() throws Throwable {
        var output = newOutputFile("new.jfc");
        var result = jfrConfigure("--input", "profile", "--output", output);
        result.shouldNotContain("Warning"); // checks missing control reference in profile.jfc

        var outputSetting = readSettings(output);
        aseertEqual(outputSetting, Configuration.getConfiguration("profile").getSettings());
    }

    private static void testNone() throws Throwable {
        var output = newOutputFile("new.jfc");
        jfrConfigure("--input", "none", "--output", output);
        var outputSetting = readSettings(output);
        aseertEqual(outputSetting, Map.of());
    }

    private static void testOr() throws Throwable {
        var output = newOutputFile("test-or-true.jfc");
        var input = newInputFile("or.jfc");
        jfrConfigure("--input", input, "month=May", "--output", output);
        var outputSetting = readSettings(output);
        var expected = readSettings(input);
        expected.put("season.Spring#enabled", "true");

        aseertEqual(outputSetting, expected);

        output = newOutputFile("test-or-false.jfc");
        jfrConfigure("--input", input, "month=September", "--output", output);
        outputSetting = readSettings(output);
        expected = readSettings(input);
        expected.put("season.Spring#enabled", "false");

        aseertEqual(outputSetting, expected);
    }

    private static void testAnd() throws Throwable {
        var output = newOutputFile("test-and-true.jfc");
        var input = newInputFile("and.jfc");
        jfrConfigure("--input", input,
                "closure=true",
                "identity=true",
                "associativity=true",
                "inverse=true",
                "--output", output);
        var outputSetting = readSettings(output);
        var expected = readSettings(input);
        expected.put("algebra.Group#enabled", "true");

        aseertEqual(outputSetting, expected);

        output = newOutputFile("test-and-false.jfc");
        jfrConfigure("--input", input,
                "closure=true",
                "identity=true",
                "associativity=true",
                "inverse=false",
                "--output", output);
        outputSetting = readSettings(output);
        expected = readSettings(input);
        expected.put("algebra.Group#enabled", "false");

        aseertEqual(outputSetting, expected);
    }


    private static void testCondition() throws Throwable {
        var output = newOutputFile("test-condition-1.jfc");
        var input = newInputFile("condition.jfc");
        jfrConfigure("--input", input, "variable=activate", "--output", output);
        var outputSetting = readSettings(output);
        var expected = readSettings(input);
        expected.put("com.example.Tiger#period", "1 s");
        expected.put("com.example.Lion#period", "3 s");

        aseertEqual(outputSetting, expected);

        output = newOutputFile("test-condition-2.jfc");
        jfrConfigure("--input", input, "variable=whatever", "--output", output);
        outputSetting = readSettings(output);
        expected = readSettings(input);
        expected.put("com.example.Lion#period", "5 s");
        expected.put("com.example.Zebra#period", "7 s");

        aseertEqual(outputSetting, expected);
    }

    private static void testFlag() throws Throwable {
        var output = newOutputFile("test-flag.jfc");
        var input = newInputFile("flag.jfc");
        jfrConfigure("--input", input, "mammal=true", "--output", output);
        var outputSetting = readSettings(output);
        var expected = readSettings(input);
        expected.put("com.example.Tiger#enabled", "true");
        expected.put("com.example.Lion#enabled", "true");

        aseertEqual(outputSetting, expected);
    }

    private static void testText() throws Throwable {
        var output = newOutputFile("test-text.jfc");
        var input = newInputFile("text.jfc");
        jfrConfigure("--input", input, "animal-threshold=3s", "--output", output);
        var outputSetting = readSettings(output);
        var expected = readSettings(input);
        expected.put("com.example.Tiger#threshold", "3 s");
        expected.put("com.example.Lion#threshold", "3 s");

        aseertEqual(outputSetting, expected);
    }

    private static void testSelection() throws Throwable {
        var output = newOutputFile("test-selection.jfc");
        var input = newInputFile("selection.jfc");
        jfrConfigure("--input", input, "animal=medium", "--output", output);
        var outputSetting = readSettings(output);
        var expected = readSettings(input);
        expected.put("com.example.Tiger#threshold", "10 s");
        expected.put("com.example.Lion#threshold", "10 s");

        aseertEqual(outputSetting, expected);
    }

    private static String newInputFile(String filename) {
        return Path.of(DIR, "configure", filename).toAbsolutePath().toString();
    }

    private static Map<String, String> readSettings(String text) throws IOException, ParseException {
        return Configuration.create(Path.of(text)).getSettings();
    }

    private static OutputAnalyzer jfrConfigure(String... args) throws Throwable {
        String[] all = new String[args.length + 1];
        all[0] = "configure";
        for (int i = 0; i < args.length; i++) {
            all[i + 1] = args[i];
        }
        OutputAnalyzer o = ExecuteHelper.jfr(all);
        System.out.println(o.getOutput());
        return o;
    }

    private static String newOutputFile(String filename) {
        return Path.of(".", System.currentTimeMillis() + filename).toAbsolutePath().toString();
    }

    private static void aseertEqual(Map<String, String> output, Map<String, String> expected) throws Exception {
        if (!output.equals(expected)) {
            System.out.println("Output:");
            for (var e : output.entrySet()) {
                System.out.println("\"" + e.getKey() + "=" + e.getValue() + "\"");
            }
            System.out.println("Expected:");
            for (var e : expected.entrySet()) {
                System.out.println("\"" + e.getKey() + "=" + e.getValue() + "\"");
            }
            throw new Exception("Mismatch between output and expected");
        }
    }
}
