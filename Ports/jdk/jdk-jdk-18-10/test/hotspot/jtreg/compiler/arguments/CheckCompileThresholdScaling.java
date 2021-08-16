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
 */

/*
 * @test CheckCompileThresholdScaling
 * @bug 8059604
 * @summary Add CompileThresholdScaling flag to control when methods are first compiled (with +/-TieredCompilation)
 * @library /test/lib
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @run driver compiler.arguments.CheckCompileThresholdScaling
 */

package compiler.arguments;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class CheckCompileThresholdScaling {

    // The flag CompileThresholdScaling scales compilation thresholds
    // in the following way:
    //
    // - if CompileThresholdScaling==1.0, the default threshold values
    // are used;
    //
    // - if CompileThresholdScaling>1.0, threshold values are scaled
    // up (e.g., CompileThresholdScalingPercentage=1.2 scales up
    // thresholds by a factor of 1.2X);
    //
    // - if CompileThresholdScaling<1.0, threshold values are scaled
    // down;
    //
    // - if CompileThresholdScaling==0, compilation is disabled
    // (equivalent to using -Xint).
    //
    // With tiered compilation enabled, the values of the following
    // flags are changed:
    //
    // Tier0InvokeNotifyFreqLog, Tier0BackedgeNotifyFreqLog,
    // Tier3InvocationThreshold, Tier3MinInvocationThreshold,
    // Tier3CompileThreshold, Tier3BackEdgeThreshold,
    // Tier2InvokeNotifyFreqLog, Tier2BackedgeNotifyFreqLog,
    // Tier3InvokeNotifyFreqLog, Tier3BackedgeNotifyFreqLog,
    // Tier23InlineeNotifyFreqLog, Tier4InvocationThreshold,
    // Tier4MinInvocationThreshold, Tier4CompileThreshold,
    // Tier4BackEdgeThreshold
    //
    // With tiered compilation disabled the value of CompileThreshold
    // is scaled.
    private static final String[][] NON_TIERED_ARGUMENTS = {
        {
            "-XX:-TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:CompileThreshold=1000",
            "-version"
        },
        {
            "-XX:-TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:CompileThreshold=1000",
            "-XX:CompileThresholdScaling=1.25",
            "-version"
        },
        {
            "-XX:-TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:CompileThreshold=1000",
            "-XX:CompileThresholdScaling=0.75",
            "-version"
        },
        {
            "-XX:-TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:CompileThreshold=1000",
            "-XX:CompileThresholdScaling=0.0",
            "-version"
        },
        {
            "-XX:-TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:CompileThreshold=0",
            "-XX:CompileThresholdScaling=0.75",
            "-version"
        }

    };

    private static final String[][] NON_TIERED_EXPECTED_OUTPUTS = {
        {
            "intx CompileThreshold                         = 1000                                   {pd product} {command line}",
            "double CompileThresholdScaling                  = 1.000000                                  {product} {default}"
        },
        {
            "intx CompileThreshold                         = 1250                                   {pd product} {command line, ergonomic}",
            "double CompileThresholdScaling                  = 1.250000                                  {product} {command line}"
        },
        {
            "intx CompileThreshold                         = 750                                    {pd product} {command line, ergonomic}",
            "double CompileThresholdScaling                  = 0.750000                                  {product} {command line}"
        },
        {
            "intx CompileThreshold                         = 1000                                   {pd product} {command line}",
            "double CompileThresholdScaling                  = 0.000000                                  {product} {command line}",
            "interpreted mode"
        },
        {
            "intx CompileThreshold                         = 0                                      {pd product} {command line}",
            "double CompileThresholdScaling                  = 0.750000                                  {product} {command line}",
            "interpreted mode"
        }
    };

    private static final String[][] TIERED_ARGUMENTS = {
        {
            "-XX:+TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:Tier0InvokeNotifyFreqLog=7",
            "-XX:Tier0BackedgeNotifyFreqLog=10",
            "-XX:Tier3InvocationThreshold=200",
            "-XX:Tier3MinInvocationThreshold=100",
            "-XX:Tier3CompileThreshold=2000",
            "-XX:Tier3BackEdgeThreshold=60000",
            "-XX:Tier2InvokeNotifyFreqLog=11",
            "-XX:Tier2BackedgeNotifyFreqLog=14",
            "-XX:Tier3InvokeNotifyFreqLog=10",
            "-XX:Tier3BackedgeNotifyFreqLog=13",
            "-XX:Tier23InlineeNotifyFreqLog=20",
            "-XX:Tier4InvocationThreshold=5000",
            "-XX:Tier4MinInvocationThreshold=600",
            "-XX:Tier4CompileThreshold=15000",
            "-XX:Tier4BackEdgeThreshold=40000",
            "-version"
        },
        {
            "-XX:+TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:Tier0InvokeNotifyFreqLog=7",
            "-XX:Tier0BackedgeNotifyFreqLog=10",
            "-XX:Tier3InvocationThreshold=200",
            "-XX:Tier3MinInvocationThreshold=100",
            "-XX:Tier3CompileThreshold=2000",
            "-XX:Tier3BackEdgeThreshold=60000",
            "-XX:Tier2InvokeNotifyFreqLog=11",
            "-XX:Tier2BackedgeNotifyFreqLog=14",
            "-XX:Tier3InvokeNotifyFreqLog=10",
            "-XX:Tier3BackedgeNotifyFreqLog=13",
            "-XX:Tier23InlineeNotifyFreqLog=20",
            "-XX:Tier4InvocationThreshold=5000",
            "-XX:Tier4MinInvocationThreshold=600",
            "-XX:Tier4CompileThreshold=15000",
            "-XX:Tier4BackEdgeThreshold=40000",
            "-XX:CompileThresholdScaling=0.75",
            "-version"
        },
        {
            "-XX:+TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:Tier0InvokeNotifyFreqLog=7",
            "-XX:Tier0BackedgeNotifyFreqLog=10",
            "-XX:Tier3InvocationThreshold=200",
            "-XX:Tier3MinInvocationThreshold=100",
            "-XX:Tier3CompileThreshold=2000",
            "-XX:Tier3BackEdgeThreshold=60000",
            "-XX:Tier2InvokeNotifyFreqLog=11",
            "-XX:Tier2BackedgeNotifyFreqLog=14",
            "-XX:Tier3InvokeNotifyFreqLog=10",
            "-XX:Tier3BackedgeNotifyFreqLog=13",
            "-XX:Tier23InlineeNotifyFreqLog=20",
            "-XX:Tier4InvocationThreshold=5000",
            "-XX:Tier4MinInvocationThreshold=600",
            "-XX:Tier4CompileThreshold=15000",
            "-XX:Tier4BackEdgeThreshold=40000",
            "-XX:CompileThresholdScaling=1.25",
            "-version"
        },
        {
            "-XX:+TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:Tier0InvokeNotifyFreqLog=7",
            "-XX:Tier0BackedgeNotifyFreqLog=10",
            "-XX:Tier3InvocationThreshold=200",
            "-XX:Tier3MinInvocationThreshold=100",
            "-XX:Tier3CompileThreshold=2000",
            "-XX:Tier3BackEdgeThreshold=60000",
            "-XX:Tier2InvokeNotifyFreqLog=11",
            "-XX:Tier2BackedgeNotifyFreqLog=14",
            "-XX:Tier3InvokeNotifyFreqLog=10",
            "-XX:Tier3BackedgeNotifyFreqLog=13",
            "-XX:Tier23InlineeNotifyFreqLog=20",
            "-XX:Tier4InvocationThreshold=5000",
            "-XX:Tier4MinInvocationThreshold=600",
            "-XX:Tier4CompileThreshold=15000",
            "-XX:Tier4BackEdgeThreshold=40000",
            "-XX:CompileThresholdScaling=2.0",
            "-version"
        },
        {
            "-XX:+TieredCompilation",
            "-XX:+PrintFlagsFinal",
            "-XX:Tier0InvokeNotifyFreqLog=7",
            "-XX:Tier0BackedgeNotifyFreqLog=10",
            "-XX:Tier3InvocationThreshold=200",
            "-XX:Tier3MinInvocationThreshold=100",
            "-XX:Tier3CompileThreshold=2000",
            "-XX:Tier3BackEdgeThreshold=60000",
            "-XX:Tier2InvokeNotifyFreqLog=11",
            "-XX:Tier2BackedgeNotifyFreqLog=14",
            "-XX:Tier3InvokeNotifyFreqLog=10",
            "-XX:Tier3BackedgeNotifyFreqLog=13",
            "-XX:Tier23InlineeNotifyFreqLog=20",
            "-XX:Tier4InvocationThreshold=5000",
            "-XX:Tier4MinInvocationThreshold=600",
            "-XX:Tier4CompileThreshold=15000",
            "-XX:Tier4BackEdgeThreshold=40000",
            "-XX:CompileThresholdScaling=0.0",
            "-version"
        }
    };

    private static final String[][] TIERED_EXPECTED_OUTPUTS = {
        {
            "intx Tier0BackedgeNotifyFreqLog               = 10                                        {product} {command line}",
            "intx Tier0InvokeNotifyFreqLog                 = 7                                         {product} {command line}",
            "intx Tier23InlineeNotifyFreqLog               = 20                                        {product} {command line}",
            "intx Tier2BackedgeNotifyFreqLog               = 14                                        {product} {command line}",
            "intx Tier2InvokeNotifyFreqLog                 = 11                                        {product} {command line}",
            "intx Tier3BackEdgeThreshold                   = 60000                                     {product} {command line}",
            "intx Tier3BackedgeNotifyFreqLog               = 13                                        {product} {command line}",
            "intx Tier3CompileThreshold                    = 2000                                      {product} {command line}",
            "intx Tier3InvocationThreshold                 = 200                                       {product} {command line}",
            "intx Tier3InvokeNotifyFreqLog                 = 10                                        {product} {command line}",
            "intx Tier3MinInvocationThreshold              = 100                                       {product} {command line}",
            "intx Tier4BackEdgeThreshold                   = 40000                                     {product} {command line}",
            "intx Tier4CompileThreshold                    = 15000                                     {product} {command line}",
            "intx Tier4InvocationThreshold                 = 5000                                      {product} {command line}",
            "intx Tier4MinInvocationThreshold              = 600                                       {product} {command line}",
            "double CompileThresholdScaling                  = 1.000000                                  {product} {default}"
        },
        {
            "intx Tier0BackedgeNotifyFreqLog               = 9                                         {product} {command line, ergonomic}",
            "intx Tier0InvokeNotifyFreqLog                 = 6                                         {product} {command line, ergonomic}",
            "intx Tier23InlineeNotifyFreqLog               = 19                                        {product} {command line, ergonomic}",
            "intx Tier2BackedgeNotifyFreqLog               = 13                                        {product} {command line, ergonomic}",
            "intx Tier2InvokeNotifyFreqLog                 = 10                                        {product} {command line, ergonomic}",
            "intx Tier3BackEdgeThreshold                   = 45000                                     {product} {command line, ergonomic}",
            "intx Tier3BackedgeNotifyFreqLog               = 12                                        {product} {command line, ergonomic}",
            "intx Tier3CompileThreshold                    = 1500                                      {product} {command line, ergonomic}",
            "intx Tier3InvocationThreshold                 = 150                                       {product} {command line, ergonomic}",
            "intx Tier3InvokeNotifyFreqLog                 = 9                                         {product} {command line, ergonomic}",
            "intx Tier3MinInvocationThreshold              = 75                                        {product} {command line, ergonomic}",
            "intx Tier4BackEdgeThreshold                   = 30000                                     {product} {command line, ergonomic}",
            "intx Tier4CompileThreshold                    = 11250                                     {product} {command line, ergonomic}",
            "intx Tier4InvocationThreshold                 = 3750                                      {product} {command line, ergonomic}",
            "intx Tier4MinInvocationThreshold              = 450                                       {product} {command line, ergonomic}",
            "double CompileThresholdScaling                  = 0.750000                                  {product} {command line}"
        },
        {
            "intx Tier0BackedgeNotifyFreqLog               = 10                                        {product} {command line, ergonomic}",
            "intx Tier0InvokeNotifyFreqLog                 = 7                                         {product} {command line, ergonomic}",
            "intx Tier23InlineeNotifyFreqLog               = 20                                        {product} {command line, ergonomic}",
            "intx Tier2BackedgeNotifyFreqLog               = 14                                        {product} {command line, ergonomic}",
            "intx Tier2InvokeNotifyFreqLog                 = 11                                        {product} {command line, ergonomic}",
            "intx Tier3BackEdgeThreshold                   = 75000                                     {product} {command line, ergonomic}",
            "intx Tier3BackedgeNotifyFreqLog               = 13                                        {product} {command line, ergonomic}",
            "intx Tier3CompileThreshold                    = 2500                                      {product} {command line, ergonomic}",
            "intx Tier3InvocationThreshold                 = 250                                       {product} {command line, ergonomic}",
            "intx Tier3InvokeNotifyFreqLog                 = 10                                        {product} {command line, ergonomic}",
            "intx Tier3MinInvocationThreshold              = 125                                       {product} {command line, ergonomic}",
            "intx Tier4BackEdgeThreshold                   = 50000                                     {product} {command line, ergonomic}",
            "intx Tier4CompileThreshold                    = 18750                                     {product} {command line, ergonomic}",
            "intx Tier4InvocationThreshold                 = 6250                                      {product} {command line, ergonomic}",
            "intx Tier4MinInvocationThreshold              = 750                                       {product} {command line, ergonomic}",
            "double CompileThresholdScaling                  = 1.250000                                  {product} {command line}"
        },
        {
            "intx Tier0BackedgeNotifyFreqLog               = 11                                        {product} {command line, ergonomic}",
            "intx Tier0InvokeNotifyFreqLog                 = 8                                         {product} {command line, ergonomic}",
            "intx Tier23InlineeNotifyFreqLog               = 21                                        {product} {command line, ergonomic}",
            "intx Tier2BackedgeNotifyFreqLog               = 15                                        {product} {command line, ergonomic}",
            "intx Tier2InvokeNotifyFreqLog                 = 12                                        {product} {command line, ergonomic}",
            "intx Tier3BackEdgeThreshold                   = 120000                                    {product} {command line, ergonomic}",
            "intx Tier3BackedgeNotifyFreqLog               = 14                                        {product} {command line, ergonomic}",
            "intx Tier3CompileThreshold                    = 4000                                      {product} {command line, ergonomic}",
            "intx Tier3InvocationThreshold                 = 400                                       {product} {command line, ergonomic}",
            "intx Tier3InvokeNotifyFreqLog                 = 11                                        {product} {command line, ergonomic}",
            "intx Tier3MinInvocationThreshold              = 200                                       {product} {command line, ergonomic}",
            "intx Tier4BackEdgeThreshold                   = 80000                                     {product} {command line, ergonomic}",
            "intx Tier4CompileThreshold                    = 30000                                     {product} {command line, ergonomic}",
            "intx Tier4InvocationThreshold                 = 10000                                     {product} {command line, ergonomic}",
            "intx Tier4MinInvocationThreshold              = 1200                                      {product} {command line, ergonomic}",
            "double CompileThresholdScaling                  = 2.000000                                  {product} {command line}"
        },
        {
            "intx Tier0BackedgeNotifyFreqLog               = 10                                        {product} {command line}",
            "intx Tier0InvokeNotifyFreqLog                 = 7                                         {product} {command line}",
            "intx Tier23InlineeNotifyFreqLog               = 20                                        {product} {command line}",
            "intx Tier2BackedgeNotifyFreqLog               = 14                                        {product} {command line}",
            "intx Tier2InvokeNotifyFreqLog                 = 11                                        {product} {command line}",
            "intx Tier3BackEdgeThreshold                   = 60000                                     {product} {command line}",
            "intx Tier3BackedgeNotifyFreqLog               = 13                                        {product} {command line}",
            "intx Tier3CompileThreshold                    = 2000                                      {product} {command line}",
            "intx Tier3InvocationThreshold                 = 200                                       {product} {command line}",
            "intx Tier3InvokeNotifyFreqLog                 = 10                                        {product} {command line}",
            "intx Tier3MinInvocationThreshold              = 100                                       {product} {command line}",
            "intx Tier4BackEdgeThreshold                   = 40000                                     {product} {command line}",
            "intx Tier4CompileThreshold                    = 15000                                     {product} {command line}",
            "intx Tier4InvocationThreshold                 = 5000                                      {product} {command line}",
            "intx Tier4MinInvocationThreshold              = 600                                       {product} {command line}",
            "double CompileThresholdScaling                  = 0.000000                                  {product} {command line}",
            "interpreted mode"
        }
    };

    private static void verifyValidOption(String[] arguments, String[] expected_outputs, boolean tiered) throws Exception {
        ProcessBuilder pb;
        OutputAnalyzer out;

        pb = ProcessTools.createJavaProcessBuilder(arguments);
        out = new OutputAnalyzer(pb.start());

        try {
            for (String expected_output : expected_outputs) {
                out.shouldContain(expected_output);
            }
            out.shouldHaveExitValue(0);
        } catch (RuntimeException e) {
            // Check if tiered compilation is available in this JVM
            // Version. Throw exception only if it is available.
            if (!(tiered && out.getOutput().contains("-XX:+TieredCompilation not supported in this VM"))) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) throws Exception {

        if (NON_TIERED_ARGUMENTS.length != NON_TIERED_EXPECTED_OUTPUTS.length) {
            throw new RuntimeException("Test is set up incorrectly: length of arguments and expected outputs in non-tiered mode of operation does not match.");
        }

        if (TIERED_ARGUMENTS.length != TIERED_EXPECTED_OUTPUTS.length) {
            throw new RuntimeException("Test is set up incorrectly: length of arguments and expected outputs in tiered mode of operation.");
        }

        // Check if thresholds are scaled properly in non-tiered mode of operation
        for (int i = 0; i < NON_TIERED_ARGUMENTS.length; i++) {
            verifyValidOption(NON_TIERED_ARGUMENTS[i], NON_TIERED_EXPECTED_OUTPUTS[i], false);
        }

        // Check if thresholds are scaled properly in tiered mode of operation
        for (int i = 0; i < TIERED_ARGUMENTS.length; i++) {
            verifyValidOption(TIERED_ARGUMENTS[i], TIERED_EXPECTED_OUTPUTS[i], true);
        }
    }
}
