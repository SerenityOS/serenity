/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package compiler.compilercontrol.share.processors;

import com.sun.management.HotSpotDiagnosticMXBean;
import compiler.compilercontrol.share.method.MethodDescriptor;
import compiler.compilercontrol.share.method.MethodGenerator;
import compiler.compilercontrol.share.pool.PoolHelper;
import compiler.compilercontrol.share.scenario.State;
import jdk.test.lib.process.OutputAnalyzer;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Executable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Process output to find compiled methods assemblies printed by print command
 */
public class PrintProcessor implements Consumer<OutputAnalyzer> {
    /**
     * Compiled method string pattern.
     * Capturing groups are
     * 1. Compiler used to compile this method
     * 2. Time stamp
     * 3. Compile ID
     * 4. Method attributes
     * 5. Compilation level
     * 6. Method name
     */
    private static final Pattern COMPILED_METHOD
            = Pattern.compile("Compiled method (?<compiler>\\(.*\\))[ ]+"
            + "(?<time>[0-9]+)[ ]+(?<id>[0-9]+) (?<attr>[ !%sbn]{6})"
            + "(?<level>[0-9]+)[ ]+(?<name>[^ ]+).*");
    private final List<String> printMethods;
    private final List<String> testMethods;

    public PrintProcessor(Map<Executable, State> states) {
        printMethods = states.keySet().stream()
                .filter(x -> states.get(x).isPrintAssembly())
                .map(MethodGenerator::logDescriptor)
                .map(MethodDescriptor::getString)
                .map(s -> s.replaceFirst("\\(.*", "")) // remove signature
                .collect(Collectors.toList());
        testMethods = new PoolHelper().getAllMethods()
                .stream()
                .map(pair -> pair.first)
                .map(MethodGenerator::logDescriptor)
                .map(MethodDescriptor::getString)
                .map(s -> s.replaceFirst("\\(.*", "")) // remove signature
                .collect(Collectors.toList());
    }

    @Override
    public void accept(OutputAnalyzer outputAnalyzer) {
        boolean wizardMode = false;
        try {
            wizardMode = Boolean.parseBoolean(ManagementFactory
                    .getPlatformMXBean(HotSpotDiagnosticMXBean.class)
                    .getVMOption("WizardMode").getValue());
        } catch (IllegalArgumentException e) {
            // ignore exception because WizardMode exists in debug only builds
        }
        if (wizardMode) {
            System.out.println("SKIP: WizardMode's output are not supported");
            return;
        }
        for (String line : outputAnalyzer.asLines()) {
            Matcher matcher = COMPILED_METHOD.matcher(line);
            if (matcher.matches()) {
                String method = normalize(matcher.group("name"));
                if (!printMethods.contains(normalize(method))
                        && testMethods.contains(method)) {
                    System.out.println(outputAnalyzer.getOutput());
                    throw new AssertionError("FAILED: wrong method "
                            + "was printed: " + method + " LINE: " + line);
                }
            }
        }
    }

    // Normalize given signature to conform regular expression used in tests
    private String normalize(String method) {
        return method.replaceAll("\\.", "/") // replace dots in a class string
                .replaceFirst("::", ".")     // replace :: between class and method
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }
}
