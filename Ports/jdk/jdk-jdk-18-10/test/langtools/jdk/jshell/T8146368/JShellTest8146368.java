/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8146368
 * @summary Test Smashing Error when user language is Japanese
 * @library /tools/lib /jdk/jshell
 * @build KullaTesting
 * @run testng/othervm -Duser.language=ja JShellTest8146368
 */

import static jdk.jshell.Snippet.Status.RECOVERABLE_NOT_DEFINED;
import org.testng.annotations.Test;

@Test
public class JShellTest8146368 extends KullaTesting {
    public void test() {
        assertEval("class A extends B {}", added(RECOVERABLE_NOT_DEFINED));
        assertEval("und m() { return new und(); }", added(RECOVERABLE_NOT_DEFINED));
    }
}
