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

import compiler.compilercontrol.share.method.MethodDescriptor;
import compiler.compilercontrol.share.scenario.CompileCommand;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PrintDirectivesProcessor
        implements Consumer<List<OutputAnalyzer>> {
    private final List<CompileCommand> commands;
    private static final Pattern MATCH_PATTERN
            = Pattern.compile(" matching: (.*)");

    public PrintDirectivesProcessor(List<CompileCommand> commands) {
        this.commands = commands;
    }

    @Override
    public void accept(List<OutputAnalyzer> outputAnalyzers) {
        List<String> directives = new ArrayList<>();
        outputAnalyzers.forEach(outputAnalyzer ->
                directives.addAll(getDirectives(outputAnalyzer)));
        List<String> expectedDirectives = commands.stream()
                .map(cc -> cc.methodDescriptor)
                .map(MethodDescriptor::getCanonicalString)
                .collect(Collectors.toList());

        if (directives.size() != expectedDirectives.size()) {
            printDirectives(directives, expectedDirectives);
            throw new AssertionError(String.format("Different number of "
                    + "directives. Expected: %d, actual: %d",
                    expectedDirectives.size(), directives.size()));
        }
        for (int i = 0; i < directives.size(); i++) {
            if (!directives.get(i).equals(expectedDirectives.get(i))) {
                printDirectives(directives, expectedDirectives);
                throw new AssertionError(
                        String.format("Directives differ at %d, expected:%s%n",
                                i, expectedDirectives.get(i)));
            }
        }
    }

    private List<String> getDirectives(OutputAnalyzer outputAnalyzer) {
        List<String> directives = new ArrayList<>();
        List<String> inputStrings = outputAnalyzer.asLines();
        Iterator<String> iterator = inputStrings.iterator();
        while (iterator.hasNext()) {
            String input = iterator.next();
            if (input.equals("Directive:")) {
                Asserts.assertTrue(iterator.hasNext(), "inconsistent directive"
                        + "printed into the output");
                String matchString = iterator.next();
                Matcher matcher = MATCH_PATTERN.matcher(matchString);
                Asserts.assertTrue(matcher.matches(), "Incorrect matching "
                        + "string in directive");
                directives.add(matcher.group(1));
            }
        }
        return directives;
    }

    private void printDirectives(List<String> directives,
                                 List<String> expected) {
        System.err.println("Actual directives: " + directives);
        System.err.println("Expected directives: " + expected);
    }
}
