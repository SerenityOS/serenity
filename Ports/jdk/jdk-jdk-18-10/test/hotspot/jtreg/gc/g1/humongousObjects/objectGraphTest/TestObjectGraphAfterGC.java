/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1.humongousObjects.objectGraphTest;

import jdk.test.lib.process.OutputAnalyzer;
import sun.hotspot.WhiteBox;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * @test TestObjectGraphAfterGC
 * @summary Checks that objects' graph behave as expected after gc
 * @requires vm.gc.G1
 * @requires vm.opt.ExplicitGCInvokesConcurrent != true
 * @library /test/lib /
 * @modules java.management java.base/jdk.internal.misc
 * @build sun.hotspot.WhiteBox
 *
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xms200M -Xmx200M -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 * -XX:+UnlockExperimentalVMOptions -XX:MaxGCPauseMillis=30000 -XX:G1MixedGCLiveThresholdPercent=100 -XX:G1HeapWastePercent=0
 * -XX:G1HeapRegionSize=1M -Xlog:gc=info:file=TestObjectGraphAfterGC_MIXED_GC.gc.log -XX:MaxTenuringThreshold=1
 * -XX:G1MixedGCCountTarget=1  -XX:G1OldCSetRegionThresholdPercent=100 -XX:SurvivorRatio=1 -XX:InitiatingHeapOccupancyPercent=0
 * gc.g1.humongousObjects.objectGraphTest.TestObjectGraphAfterGC MIXED_GC
 *
 * @run main/othervm -Xms200M -Xmx200M -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 * -XX:G1HeapRegionSize=1M -Xlog:gc*=debug:file=TestObjectGraphAfterGC_YOUNG_GC.gc.log
 * gc.g1.humongousObjects.objectGraphTest.TestObjectGraphAfterGC YOUNG_GC
 *
 * @run main/othervm -Xms200M -Xmx200M -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 * -XX:G1HeapRegionSize=1M -Xlog:gc=info:file=TestObjectGraphAfterGC_FULL_GC.gc.log
 * gc.g1.humongousObjects.objectGraphTest.TestObjectGraphAfterGC FULL_GC
 *
 * @run main/othervm -Xms200M -Xmx200M -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 * -XX:G1HeapRegionSize=1M -Xlog:gc=info:file=TestObjectGraphAfterGC_FULL_GC_MEMORY_PRESSURE.gc.log
 * gc.g1.humongousObjects.objectGraphTest.TestObjectGraphAfterGC FULL_GC_MEMORY_PRESSURE
 *
 * @run main/othervm -Xms200M -Xmx200M -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 * -XX:G1HeapRegionSize=1M -Xlog:gc=info:file=TestObjectGraphAfterGC_CMC.gc.log -XX:MaxTenuringThreshold=16
 * gc.g1.humongousObjects.objectGraphTest.TestObjectGraphAfterGC CMC
 *
 * @run main/othervm -Xms200M -Xmx200M -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 * -XX:G1HeapRegionSize=1M -Xlog:gc=info:file=TestObjectGraphAfterGC_CMC_NO_SURV_ROOTS.gc.log -XX:MaxTenuringThreshold=1
 * gc.g1.humongousObjects.objectGraphTest.TestObjectGraphAfterGC CMC_NO_SURV_ROOTS
 *
 */

/**
 * Checks that objects' graph behave as expected after gc
 * See README file for detailed info on test's logic
 */
public class TestObjectGraphAfterGC {

    private static final int simpleAllocationSize = 1024;

    /**
     * Entry point
     *
     * @param args - first argument - gc name
     */
    public static void main(String[] args) {

        if (args.length < 1) {
            throw new Error("Expected gc name wasn't provided as command line argument");
        }

        GC gcType = GC.valueOf(args[0].toUpperCase());

        System.out.println("Testing " + gcType.name());

        TestcaseData.getPregeneratedTestcases().stream().forEach(testcase -> {
            System.out.println("Testcase: " + testcase);

            try {
                TestObjectGraphAfterGC.doTesting(testcase, gcType.get(), gcType.getChecker(),
                        gcType.getGcLogName(TestObjectGraphAfterGC.class.getSimpleName()), gcType.shouldContain(),
                        gcType.shouldNotContain());
            } catch (IOException e) {
                throw new Error("Problems trying to find or open " + TestObjectGraphAfterGC.class.getSimpleName()
                        + ".gc.log", e);
            }
            System.out.println(" Passed");
        });
    }

    /**
     * Implements testing with 3 methods - allocateObjectGraph, checkResults and checkGCLog
     *
     * @param testcaseData     testcase in the following notation:
     *                         H - humongous node
     *                         S - non-humongous node
     *                         s - external soft reference
     *                         w - external weak reference
     *                         Hs->Sw - 1st node is humongous, externally soft referenced and strong references to
     *                         non-humongous node 2 which is externally weak referenced
     *                         H->1 - humongous node connects to the first node of chain
     * @param doGC             method that initiates gc
     * @param checker          consumer that checks node's state after gc and throws Error if it's wrong
     * @param gcLogName        name of gc log
     * @param shouldContain    list of tokens that should be contained in gc log
     * @param shouldNotContain list of tokens that should not be contained in gc log
     * @throws IOException if there are some issues with gc log
     */
    private static void doTesting(String testcaseData, Runnable doGC, Consumer<ReferenceInfo<Object[]>> checker,
                                  String gcLogName, List<String> shouldContain, List<String> shouldNotContain)
            throws IOException {
        Set<ReferenceInfo<Object[]>> nodeData = allocateObjectGraph(testcaseData);
        doGC.run();
        checkResults(nodeData, checker);
        checkGCLog(gcLogName, shouldContain, shouldNotContain);
    }

    /**
     * Allocates a number of objects of humongous and regular size and links then with strong references.
     * How many objects to create, their size and links between them is encoded in the given parameters.
     * As the result an object graph will be created.
     * For the testing purpose for each created object (a graph node) an extra ReferenceInfo object will be created.
     * The ReferenceInfo instances will contain either weak or soft reference to the graph node.
     *
     * @param testcaseData testcase in the
     *                     <p>
     *                     H - humongous node
     *                     S - non-humongous node
     *                     s - external soft reference
     *                     w - external weak reference
     *                     Hs->Sw - 1st node is humongous, externally soft referenced and strong references to
     *                     non-humongous node 2 which is externally weak referenced
     *                     H->1 - humongous node connects to the first node of chain
     * @return set of ReferenceInfo objects containing weak/soft reference to the graph node and other data on how
     * objects should behave after gc
     */
    private static Set<ReferenceInfo<Object[]>> allocateObjectGraph(String testcaseData) {
        Map<Object[], String> nodeIds = new HashMap<>();
        Set<Object[]> humongousNodes = new HashSet<>();
        Set<Object[]> externalSoftReferenced = new HashSet<>();
        Set<Object[]> externalWeakReferenced = new HashSet<>();

        Map<Predicate<TestcaseData.FinalParsedNode>, BiConsumer<TestcaseData.FinalParsedNode, Object[][]>> visitors
                = new HashMap<>();

        visitors.put((parsedNode -> true),
                (parsedNode, objects) -> nodeIds.put(objects[Integer.valueOf(parsedNode.id)], parsedNode.id)
        );

        visitors.put((parsedNode -> parsedNode.isHumongous),
                (parsedNode, objects) -> humongousNodes.add(objects[Integer.valueOf(parsedNode.id)])
        );

        visitors.put(parsedNode -> parsedNode.getReferencesTypes().stream().
                        anyMatch(referenceType -> referenceType == ObjectGraph.ReferenceType.SOFT),
                (parsedNode, objects) -> externalSoftReferenced.add(objects[Integer.valueOf(parsedNode.id)])
        );

        visitors.put(parsedNode -> parsedNode.getReferencesTypes().stream().
                        anyMatch(referenceType -> referenceType == ObjectGraph.ReferenceType.WEAK),
                (parsedNode, objects) -> externalWeakReferenced.add(objects[Integer.valueOf(parsedNode.id)])
        );

        List<TestcaseData.FinalParsedNode> internalParsedNodes = TestcaseData.parse(testcaseData);

        Object[] root = ObjectGraph.generateObjectNodes(internalParsedNodes, visitors,
                WhiteBox.getWhiteBox().g1RegionSize(), simpleAllocationSize);

        ObjectGraph.propagateTransitiveProperty(humongousNodes, humongousNodes::add);
        Set<Object[]> effectiveSoftReferenced = new HashSet<>();
        ObjectGraph.propagateTransitiveProperty(externalSoftReferenced, effectiveSoftReferenced::add);

        // Create external references
        ReferenceQueue<Object[]> referenceQueue = new ReferenceQueue<>();
        Set<Reference<Object[]>> externalRefs = new HashSet<>();

        externalWeakReferenced.stream()
                .forEach(objects -> externalRefs.add(new WeakReference<>(objects, referenceQueue)));
        externalSoftReferenced.stream()
                .forEach(objects -> externalRefs.add(new SoftReference<>(objects, referenceQueue)));

        return externalRefs.stream()
                .map(ref -> new ReferenceInfo<>(ref, testcaseData, nodeIds.get(ref.get()),
                        effectiveSoftReferenced.contains(ref.get()), humongousNodes.contains(ref.get())))
                .collect(Collectors.toSet());

    }

    /**
     * Checks that object' state after gc is as expected
     *
     * @param nodeData array with information about nodes
     * @param checker  consumer that checks node's state after gc and throws Error if it's wrong
     */
    private static void checkResults(Set<ReferenceInfo<Object[]>> nodeData, Consumer<ReferenceInfo<Object[]>> checker) {
        nodeData.stream().forEach(checker::accept);
    }

    /**
     * Checks that gc log contains what we expected and does not contain what we didn't expect
     *
     * @param gcLogName        gc log name
     * @param shouldContain    list of tokens that should be contained in gc log
     * @param shouldNotContain list of tokens that should not be contained in gc log
     * @throws IOException if there are some issues with gc log
     */
    private static void checkGCLog(String gcLogName, List<String> shouldContain, List<String> shouldNotContain)
            throws IOException {

        if (gcLogName == null) {
            return;
        }
        String gcLog = new String(Files.readAllBytes(new File(gcLogName).toPath()));

        OutputAnalyzer outputAnalyzer = new OutputAnalyzer(gcLog, "");

        shouldContain.stream().forEach(outputAnalyzer::shouldContain);
        shouldNotContain.stream().forEach(outputAnalyzer::shouldNotContain);
    }

}
