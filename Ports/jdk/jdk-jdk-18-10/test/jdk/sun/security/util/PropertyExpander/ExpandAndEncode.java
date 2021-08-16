/*
 * Copyright (c) 2002, 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @author Valerie Peng
 * @bug 4716213
 * @bug 4797850
 * @modules java.base/sun.security.util
 * @summary Verify that expand(String, boolean) does not encode if
 * the value is a valid URI with a scheme (it is already encoded),
 * i.e. avoid double encoding.
 */
import java.io.File;
import sun.security.util.*;

public class ExpandAndEncode {

    private static String tests[] = {
        "${env.test1}/test.jar",
        "file:${env.test2}/test.jar",
        "file:/foo/${env.test3}",
        "file:/foo/${env.test4}"
    };

    private static String answers[] = {
        "http://my%20home/test.jar",
        "file:/my%20home/test.jar",
        "file:/foo/bar%23foobar",
        "file:/foo/goofy:bar%23foobar"
    };

    private static boolean checkAnswer(String result, int i) {
        if (result.equals(answers[i])) {
            return true;
        } else {
            System.out.println("Test#" + i + ": expected " + answers[i]);
            System.out.println("Test#" + i + ": got " + result);
            return false;
        }
    }

    public static void main(String[] args) throws Exception {

        boolean status = true;

        System.setProperty("env.test1", "http://my%20home");
        System.setProperty("env.test2", File.separator + "my home");
        System.setProperty("env.test3", "bar#foobar");
        System.setProperty("env.test4", "goofy:bar#foobar");

        for (int i=0; i < tests.length; i++) {
            String result = PropertyExpander.expand(tests[i], true);
            status &= checkAnswer(result, i);
        }

        if (status) {
            System.out.println("PASSED");
        } else {
            throw new Exception("FAILED");
        }
    }
}
