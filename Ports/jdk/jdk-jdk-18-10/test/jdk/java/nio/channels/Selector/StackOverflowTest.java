/*
 * Copyright (c) 2019, Red Hat, Inc. and/or its affiliates.
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

/**
 * @test
 * @bug 8216472
 * @summary native call in WindowsSelectorImpl.SubSelector.poll can use
 *     more stack space than available in a shadow zone, this can cause
 *     a crash if selector is called from a deep recursive java call
 * @requires (os.family == "windows")
 */

import java.nio.channels.Selector;

public class StackOverflowTest {

    public static void main(String[] args) throws Exception {
        try (var sel = Selector.open()) {
            recursiveSelect(sel);
        } catch (StackOverflowError e) {
            // ignore SOE from java calls
        }
    }

    static void recursiveSelect(Selector sel) throws Exception {
        sel.selectNow();
        recursiveSelect(sel);
    }
}
