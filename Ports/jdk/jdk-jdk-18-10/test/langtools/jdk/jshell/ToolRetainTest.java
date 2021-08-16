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

/*
 * @test
 * @bug 8157200 8163840 8154513
 * @summary Tests of what information is retained across jshell tool runs
 * @modules jdk.jshell/jdk.internal.jshell.tool
 * @build ToolRetainTest ReplToolTesting
 * @run testng ToolRetainTest
 */

import java.util.Locale;
import org.testng.annotations.Test;

@Test
public class ToolRetainTest extends ReplToolTesting {

    public void testRetainMode() {
        test(
                (a) -> assertCommand(a, "/set mode trm -quiet", "|  Created new feedback mode: trm"),
                (a) -> assertCommand(a, "/set feedback trm", ""),
                (a) -> assertCommand(a, "/set format trm display '{name}:{value}'", ""),
                (a) -> assertCommand(a, "int x = 45", "x:45"),
                (a) -> assertCommand(a, "/set mode -retain trm", ""),
                (a) -> assertCommand(a, "/exit", "")
        );
        test(
                (a) -> assertCommandOutputContains(a, "/set mode trm",
                        "/set format trm display \"{name}:{value}\""),
                (a) -> assertCommand(a, "/set feedback trm", ""),
                (a) -> assertCommand(a, "int x = 45", "x:45")
        );
    }

    public void testRetain2Mode() {
        test(
                (a) -> assertCommand(a, "/set mode trm1 -quiet", "|  Created new feedback mode: trm1"),
                (a) -> assertCommand(a, "/set mode -retain trm1", ""),
                (a) -> assertCommand(a, "/set feedback -retain trm1", ""),
                (a) -> assertCommand(a, "/set format trm1 display '{name}:{value}'", ""),
                (a) -> assertCommand(a, "int x = 66", "x:66"),
                (a) -> assertCommand(a, "/set mode -retain trm1", ""),
                (a) -> assertCommand(a, "/exit", "")
        );
        test(Locale.ROOT, true, new String[0], "",
                (a) -> assertCommand(a, "/set mode trm2 -quiet", ""),
                (a) -> assertCommand(a, "/set format trm2 display '{name}={value}'", ""),
                (a) -> assertCommand(a, "int x = 45", "x:45"),
                (a) -> assertCommand(a, "/set mode -retain trm2", ""),
                (a) -> assertCommand(a, "/exit", "")
        );
        test(Locale.ROOT, true, new String[0], "",
                (a) -> assertCommandOutputContains(a, "/set mode trm1",
                        "/set format trm1 display \"{name}:{value}\""),
                (a) -> assertCommand(a, "/set format trm2 display",
                        "|  /set format trm2 display \"{name}={value}\""),
                (a) -> assertCommand(a, "int x = 99", "x:99"),
                (a) -> assertCommand(a, "/set feedback trm2", ""),
                (a) -> assertCommand(a, "int z = 77", "z=77")
        );
    }

    public void testRetainFeedback() {
        test(
                (a) -> assertCommand(a, "/set feedback -retain verbose", "|  Feedback mode: verbose"),
                (a) -> assertCommand(a, "/exit", "")
        );
        test(
                (a) -> assertCommandOutputStartsWith(a, "/set feedback",
                        "|  /set feedback -retain verbose\n" +
                        "|  \n" +
                        "|  "),
                (a) -> assertCommandOutputContains(a, "int h =8", "|  created variable h : int")
        );
    }

    public void testRetainFeedbackBlank() {
        String feedbackOut =
                        "|  /set feedback -retain verbose\n" +
                        "|  \n" +
                        "|  Available feedback modes:\n" +
                        "|     concise\n" +
                        "|     normal\n" +
                        "|     silent\n" +
                        "|     verbose";
        test(
                (a) -> assertCommand(a, "/set feedback verbose", "|  Feedback mode: verbose"),
                (a) -> assertCommand(a, "/set feedback -retain", ""),
                (a) -> assertCommand(a, "/set feedback", feedbackOut),
                (a) -> assertCommand(a, "/exit", "")
        );
        test(
                (a) -> assertCommand(a, "/set feedback", feedbackOut),
                (a) -> assertCommandOutputContains(a, "int qw = 5", "|  created variable qw : int")
        );
    }

    public void testRetainEditor() {
        test(
                (a) -> assertCommand(a, "/set editor -retain nonexistent",
                        "|  Editor set to: nonexistent\n" +
                        "|  Editor setting retained: nonexistent"),
                (a) -> assertCommand(a, "/exit", "")
        );
        test(
                (a) -> assertCommand(a, "/set editor", "|  /set editor -retain nonexistent"),
                (a) -> assertCommandOutputContains(a, "int h =8", ""),
                (a) -> assertCommandOutputContains(a, "/edit h", "Edit Error:")
        );
    }

    public void testRetainEditorBlank() {
        test(
                (a) -> assertCommand(a, "/set editor nonexistent", "|  Editor set to: nonexistent"),
                (a) -> assertCommand(a, "/set editor -retain", "|  Editor setting retained: nonexistent"),
                (a) -> assertCommand(a, "/exit", "")
        );
        test(
                (a) -> assertCommandOutputContains(a, "int h =8", ""),
                (a) -> assertCommandOutputContains(a, "/edit h", "Edit Error:")
        );
    }

    public void testRetainModeNeg() {
        test(
                (a) -> assertCommandOutputStartsWith(a, "/set mode -retain verbose",
                        "|  Not valid with a predefined mode"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode -retain ????",
                        "|  Expected a feedback mode name: ????")
        );
    }

    public void testRetainFeedbackNeg() {
        test(
                (a) -> assertCommandOutputStartsWith(a, "/set feedback -retain babble1",
                        "|  Does not match any current feedback mode"),
                (a) -> assertCommandOutputStartsWith(a, "/set mode trfn",
                        "|  To create a new mode either the -command or the -quiet option must be used -- \n" +
                        "|  Does not match any current feedback mode: trfn -- /set mode trfn"),
                (a) -> assertCommand(a, "/set mode trfn -command",
                        "|  Created new feedback mode: trfn"),
                (a) -> assertCommandOutputContains(a, "/set feedback -retain trfn",
                        "is predefined or has been retained"),
                (a) -> assertCommandOutputStartsWith(a, "/set feedback -retain !!!!",
                        "|  Expected a feedback mode name: !!!!")
        );
    }

    public void testNoRetainMode() {
        test(
                (a) -> assertCommand(a, "/set mode trm -quiet", "|  Created new feedback mode: trm"),
                (a) -> assertCommand(a, "/set feedback trm", ""),
                (a) -> assertCommand(a, "/set format trm display '{name}:{value}'", ""),
                (a) -> assertCommand(a, "int x = 45", "x:45"),
                (a) -> assertCommand(a, "/exit", "")
        );
        test(
                (a) -> assertCommandOutputStartsWith(a, "/set feedback trm",
                        "|  Does not match any current feedback mode"),
                (a) -> assertCommandOutputContains(a, "int x = 45", "==> 45")
        );
    }

    public void testNoRetainFeedback() {
        test(
                (a) -> assertCommand(a, "/set feedback verbose", "|  Feedback mode: verbose"),
                (a) -> assertCommand(a, "/exit", "")
        );
        test(
                (a) -> assertCommand(a, "int h =8", "h ==> 8")
        );
    }

}
