/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4358979
 * @summary Tests BasicStroke encoding
 * @run main/othervm -Djava.security.manager=allow java_awt_BasicStroke
 * @author Sergey Malenkov
 */

import java.awt.BasicStroke;

public final class java_awt_BasicStroke extends AbstractTest<BasicStroke> {
    public static void main(String[] args) {
        new java_awt_BasicStroke().test(true);
    }

    protected BasicStroke getObject() {
        return new BasicStroke();
    }

    protected BasicStroke getAnotherObject() {
        float[] f = {1.0f, 2.0f, 3.0f, 4.0f};
        return new BasicStroke(f[1], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, f[2], f, f[3]);
    }
}
