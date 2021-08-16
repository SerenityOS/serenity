/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6599601
 * @summary tests that a simple GUI application runs without any
 *          exceptions thrown
 * @author Artem.Ananiev area=awt.Toolkit
 * @run main/othervm -Djava.security.manager=allow SecurityTest2
 */

import java.awt.*;

public class SecurityTest2
{
    public static void main(String[] args)
    {
        System.setSecurityManager(new SecurityManager());

        try
        {
            Frame f = new Frame();
            f.setVisible(true);
            f.dispose();
        }
        catch (Exception z)
        {
            throw new RuntimeException("Test FAILED because of some Exception thrown", z);
        }
    }
}
