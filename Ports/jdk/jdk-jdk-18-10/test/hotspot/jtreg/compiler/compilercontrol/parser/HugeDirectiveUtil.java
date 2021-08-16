/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package compiler.compilercontrol.parser;

import compiler.compilercontrol.share.JSONFile;
import compiler.compilercontrol.share.method.MethodDescriptor;
import compiler.compilercontrol.share.scenario.DirectiveWriter;
import compiler.compilercontrol.share.scenario.Scenario;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static compiler.compilercontrol.share.IntrinsicCommand.VALID_INTRINSIC_SAMPLES;

/**
 * Creates a huge directive file
 */
public final class HugeDirectiveUtil {
    protected static final String EXPECTED_ERROR_STRING = "Parsing of compiler "
            + "directives failed";

    private HugeDirectiveUtil() { }

    /**
     * Creates huge file with specified amount of directives
     *
     * @param descriptors a list of descriptors to be randomly used
     *                    in match and inline blocks
     * @param fileName    a directives file name to be created
     * @param amount      an amount of match objects
     */
    public static void createHugeFile(List<MethodDescriptor> descriptors,
            String fileName, int amount) {
        try (DirectiveWriter file = new DirectiveWriter(fileName)) {
            file.write(JSONFile.Element.ARRAY);
            for (int i = 0; i < amount; i++) {
                createMatchObject(descriptors, file, 1);
            }
            file.end();
        }
    }

    /**
     * Creates match object in the given file with specified size
     *
     * @param descriptors a list of method descriptors to be used
     * @param file        a directive file to write at
     * @param objectSize  a size of the match object
     */
    public static void createMatchObject(List<MethodDescriptor> descriptors,
            DirectiveWriter file, int objectSize) {
        // get random amount of methods for the match
        List<String> methods = getRandomDescriptors(descriptors);
        file.match(methods.toArray(new String[methods.size()]));
        Random random = Utils.getRandomInstance();
        for (int i = 0; i < objectSize; i++) {
            // emit compiler block
            file.emitCompiler(Utils.getRandomElement(
                    Scenario.Compiler.values()));
            // add option inside the compiler block
            DirectiveWriter.Option option = Utils.getRandomElement(DirectiveWriter.Option.values());
            file.option(option,
                    option != DirectiveWriter.Option.INTRINSIC
                    ? random.nextBoolean()
                    : "\"" + Utils.getRandomElement(VALID_INTRINSIC_SAMPLES) + "\"");
            file.end(); // ends compiler block

            // add standalone option, enable can't be used standalone
            EnumSet<DirectiveWriter.Option> options = EnumSet.complementOf(
                    EnumSet.of(DirectiveWriter.Option.ENABLE));
            file.option(Utils.getRandomElement(options), random.nextBoolean());
        }
        // add inline block with random inlinees
        methods = getRandomDescriptors(descriptors).stream()
                .map(s -> (random.nextBoolean() ? "+" : "-") + s)
                .collect(Collectors.toList());
        file.inline(methods);

        // end match block
        file.end();
    }

    private static List<String> getRandomDescriptors(
            List<MethodDescriptor> descriptors) {
        Random random = Utils.getRandomInstance();
        int amount = 1 + random.nextInt(descriptors.size() - 1);
        int skipAmount = random.nextInt(descriptors.size() - amount);
        return descriptors.stream()
                .skip(skipAmount)
                .limit(amount)
                .map(MethodDescriptor::getString)
                .collect(Collectors.toList());
    }

    protected static OutputAnalyzer execute(String fileName) {
        OutputAnalyzer output;
        try {
            output = ProcessTools.executeTestJvm(
                    "-XX:+UnlockDiagnosticVMOptions",
                    "-XX:CompilerDirectivesLimit=1000",
                    "-XX:CompilerDirectivesFile=" + fileName,
                    "-version");
        } catch (Throwable thr) {
            throw new Error("Execution failed with: " + thr, thr);
        }
        return output;
    }
}
