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

/*
 * @test
 * @bug 8146368
 * @summary Test Smashing Error when user language is Japanese
 * @modules jdk.jshell/jdk.internal.jshell.tool
 * @library /tools/lib /jdk/jshell
 * @build ReplToolTesting
 * @run testng/othervm -Duser.language=ja JShellToolTest8146368
 */

import org.testng.annotations.Test;

@Test
public class JShellToolTest8146368 extends ReplToolTesting {
    public void test() {
        test(
                a -> assertCommand(a, "class A extends B {}", "|  created class A, however, it cannot be referenced until class B is declared\n"),
                a -> assertCommand(a, "und m() { return new und(); }", "|  created method m(), however, it cannot be referenced until class und is declared\n")
        );
    }
}
