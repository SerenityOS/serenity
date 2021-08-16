/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.util.Arrays;

import toolbox.ToolBox;

public class SjavacBase {

    protected final static ToolBox toolbox = new ToolBox();

    /**
     * Utility method for invoking sjavac. Method accepts Objects as arguments
     * (which are turned into Strings through Object::toString) to allow clients
     * to pass for instance Path objects.
     */
    public static int compile(Object... args) throws ReflectiveOperationException {
        // Use reflection to avoid a compile-time dependency on sjavac Main
        System.out.println("compile: " + Arrays.toString(args));
        Class<?> c = Class.forName("com.sun.tools.sjavac.Main");
        Method m = c.getDeclaredMethod("go", String[].class);
        String[] strArgs = new String[args.length];
        for (int i = 0; i < args.length; i++)
            strArgs[i] = args[i].toString();
        int rc = (Integer) m.invoke(null, (Object) strArgs);
        return rc;
    }
}
