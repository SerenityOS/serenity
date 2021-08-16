/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

// This class contains methods common to all transformation test cases
public class TransformTestCommon {

    // get parameters to an agent depending on the test case
    // these parameters will instruct the agent which classes should be
    // transformed
    public static String getAgentParams(TestEntry entry,
                                        String parent, String child) {

        if (entry.transformParent && entry.transformChild)
            return parent + "," + child;
        if (entry.transformParent)
            return parent;
        if (entry.transformChild)
            return child;

        return "";
    }


    private static void checkTransformationResults(TestEntry entry,
                                                   OutputAnalyzer out)
        throws Exception {

        if (entry.transformParent)
            out.shouldContain(TransformUtil.ParentCheckPattern +
                              TransformUtil.AfterPattern);

        if (entry.transformChild)
            out.shouldContain(TransformUtil.ChildCheckPattern +
                              TransformUtil.AfterPattern);
    }


    private static void checkSharingByClass(TestEntry entry, OutputAnalyzer out,
                                            String parent, String child)
        throws Exception {

        String parentSharedMatch = " " + parent + " source: shared objects file";
        String childSharedMatch =  " " + child +  " source: shared objects file";

        if (entry.isParentExpectedShared)
            out.shouldContain(parentSharedMatch);
        else
            out.shouldNotContain(parentSharedMatch);

        if (entry.isChildExpectedShared)
            out.shouldContain(childSharedMatch);
        else
            out.shouldNotContain(childSharedMatch);
    }


    // Both parent and child classes should be passed to ClassFileTransformer.transform()
    // exactly once.
    private static void checkTransformationCounts(TestEntry entry, OutputAnalyzer out,
                                                  String parent, String child)
        throws Exception {

        String patternBase = "TransformerAgent: SimpleTransformer called for: ";

        out.shouldContain(patternBase + child + "@1");
        out.shouldContain(patternBase + parent + "@1");

        out.shouldNotContain(patternBase + child + "@2");
        out.shouldNotContain(patternBase + parent + "@2");
    }


    public static void checkResults(TestEntry entry, OutputAnalyzer out,
                                    String parent, String child)
        throws Exception {

        // If we were not able to map an archive,
        // then do not perform other checks, since
        // there was no sharing at all
        CDSTestUtils.checkMappingFailure(out);

        String childVmName = child.replace('.', '/');
        String parentVmName = parent.replace('.', '/');

        CDSTestUtils.checkExec(out);
        checkTransformationCounts(entry, out, parentVmName, childVmName);
        checkTransformationResults(entry, out);
        checkSharingByClass(entry, out, parent, child);
    }
}
