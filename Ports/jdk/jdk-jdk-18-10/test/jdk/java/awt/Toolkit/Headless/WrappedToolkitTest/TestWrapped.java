/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * test
 * @bug 6282388
 * @summary Tests that AWT use correct toolkit to be wrapped into HeadlessToolkit
 * @author artem.ananiev@sun.com: area=awt.headless
 * @run shell WrappedToolkitTest.sh
 */

import java.awt.*;

import java.lang.reflect.*;

import sun.awt.*;

public class TestWrapped
{
    public static void main(String[] args)
    {
        try
        {
        if (args.length != 1) {
            System.err.println("No correct toolkit class name is specified, test is not run");
            System.exit(0);
        }

        String correctToolkitClassName = args[0];
        Toolkit tk = Toolkit.getDefaultToolkit();
        Class tkClass = tk.getClass();
        if (!tkClass.getName().equals("sun.awt.HeadlessToolkit"))
        {
            System.err.println(tkClass.getName());
            System.err.println("Error: default toolkit is not an instance of HeadlessToolkit");
            System.exit(-1);
        }

        Field f = tkClass.getDeclaredField("tk");
        f.setAccessible(true);
        Class wrappedClass = f.get(tk).getClass();
        if (!wrappedClass.getName().equals(correctToolkitClassName)) {
            System.err.println(wrappedClass.getName());
            System.err.println("Error: wrapped toolkit is not an instance of " + correctToolkitClassName);
            System.exit(-1);
        }
        }
        catch (Exception z)
        {
            z.printStackTrace(System.err);
            System.exit(-1);
        }

        System.exit(0);
    }
}
