/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.compiler.coverage.parentheses;

import vm.compiler.coverage.parentheses.share.HotspotInstructionsExecutor;
import vm.share.options.Option;
import vm.share.options.Options;
import vm.share.options.OptionSupport;

import nsk.share.Failure;
import nsk.share.Log;
import nsk.share.test.StressOptions;

import vm.compiler.coverage.parentheses.share.InstructionSequence;
import vm.compiler.coverage.parentheses.share.TinyInstructionsExecutor;
import vm.compiler.coverage.parentheses.share.generation.RandomInstructionsGenerator;

import java.io.IOException;

public class Parentheses {

    private Log log;

    @Option(name = "iterations", default_value = "100", description = "number of iterations")
    int iterations = 2000;

    @Option(name = "maxStackDepth", default_value = "100",
        description = "maximal stack depth that can be required by generated instruction sequence")
    int maxStackDepth = 100;

    @Options
    StressOptions stressOptions = new StressOptions();

    @Option(name = "verbose", default_value = "false", description = "verbose mode")
    boolean verbose;

    @Option(name = "loadFrom", default_value = "", description = "path to file that contains instruction sequence")
    String loadFrom = "";

    @Option(name = "saveTo", default_value = "parentheses",
        description = "path to file in which will be stored instruction sequence if errors will be occur")
    String saveTo = "saveTo";

    public static void main(String[] args) throws Exception {
        Parentheses test = new Parentheses();
        OptionSupport.setup(test, args);
        test.run();
    }

    public void run() throws IOException, ReflectiveOperationException {

        log = new Log(System.out, verbose);

        InstructionSequence instructionSequence = null;
        for (int i = 0; i < iterations * stressOptions.getIterationsFactor(); i++) {
            log.display("Iteration " + i);
            if (loadFrom.isEmpty()) {
                log.display("generating instructions list");
                instructionSequence = new RandomInstructionsGenerator(maxStackDepth).generate();
            } else {
                if (instructionSequence == null) {
                    log.display("loading instructions list from file: " + loadFrom);
                    instructionSequence = InstructionSequence.fromFile(loadFrom);
                }
            }

            log.display("executing instructions");

            TinyInstructionsExecutor tinyVM = new TinyInstructionsExecutor(instructionSequence.getMaxStackDepth());
            int tinyRes = tinyVM.execute(instructionSequence.getInstructions());

            HotspotInstructionsExecutor hotspot = new HotspotInstructionsExecutor(instructionSequence.getMaxStackDepth());
            int hotspotRes = hotspot.execute(instructionSequence.getInstructions());

            if (tinyRes != hotspotRes) {
                log.complain("Incorrect results of InstructionsExecutor instructions computations");
                log.complain("instructions:");
                log.complain(instructionSequence.toString());
                log.complain("TinyInstructionsExecutor result: " + tinyRes);
                log.complain("HotspotInstructionsExecutor result: " + hotspotRes);
                log.complain("Instruction sequence was written to file: " + saveTo);
                instructionSequence.saveToFile(saveTo);
                throw new Failure("Incorrect results of InstructionsExecutor instructions computations");
            } else {
                log.display("TinyInstructionsExecutor result: " + tinyRes);
                log.display("HotspotInstructionsExecutor result: " + hotspotRes);
                log.display("");
            }
        }

    }
}
