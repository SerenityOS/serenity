/*
 * Copyright (c) 2001, 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4030374
 * @summary Initialization of up-level links, immediately after super(), occurs too late.
 * @author gafter
 *
 * @compile Closure3.java
 * @run main Closure3
 */

// Make sure the closure is present when the superclass is constructed.
// Specifically, Closure3.$1 must have its $parameter and $local_var copies initialized when BaseClass calls x().

abstract class BaseClass
{
    protected BaseClass()
    {
        x();
    }
    protected abstract void x();
}

public class Closure3
{
    public static void main(String[] args)
    {
        callingMethod("12345678");
    }

    protected static void callingMethod(final String parameter)
    {
        String t = "12345";
        final String local_var = t;
        BaseClass enum_ = new BaseClass() {
            public void x()
                {
                    int i = parameter.length() + local_var.length();
                    if (i != 13) throw new Error();
                }
        };
    }
}
