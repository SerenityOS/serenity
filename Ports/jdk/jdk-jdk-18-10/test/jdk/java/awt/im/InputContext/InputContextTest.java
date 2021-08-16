/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 4151222
 * @bug 4150206
 * @bug 4243948
 * @summary removeNotify(null) and selectInputMethod(null) have to
 * throw a NullPointerException. selectInputMethod doesn't throw a
 * NullPointerException.
 */

import java.awt.Frame;
import java.awt.im.InputContext;
import java.util.Locale;

public class InputContextTest {

    public static void main(String[] args) throws Exception {

        Frame frame = new Frame();
        InputContext ic = frame.getInputContext();

        // 4151222
        try {
            ic.removeNotify(null);
            throw new Exception("InputContext.removeNotify(null) doesn't throw NullPointerException");
        } catch (Exception e) {
            if (! (e instanceof NullPointerException)) {
                throw new Exception("InputContext.removeNotify(null) throws " + e
                                    + " instead of NullPointerException.");
            }
        }

        // 4150206
        try {
            ic.selectInputMethod(null);
            throw new Exception("InputContext.selectInputMethod(null) doesn't throw NullPointerException");
        } catch (Exception e) {
            if (! (e instanceof NullPointerException)) {
                throw new Exception("InputContext.selectInputMethod(null) throws " + e
                                    + " instead of NullPointerException.");
            }
        }

        // 4243948
        try {
            ic.selectInputMethod(Locale.JAPANESE);
        } catch (Exception e) {
            throw new Exception("InputContext.selectInputMethod(Locale) throws " + e);
        }
    }
}
