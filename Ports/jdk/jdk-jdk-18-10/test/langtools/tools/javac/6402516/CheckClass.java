/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6402516
 * @summary need Trees.getScope(TreePath)
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build Checker CheckClass
 * @run main CheckClass
 */

import java.util.*;
import com.sun.source.tree.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;

/*
 * Check the enclosing class of a scope against the contents of string literals.
 */
public class CheckClass extends Checker {
    public static void main(String... args) throws Exception {
        Checker chk = new CheckClass();
        chk.check("TestClass.java");
    }

    @Override
    protected boolean checkLocal(Scope s, String ref) {
        //System.err.println("checkLocal: " + s + " " + ref + " " + s.getEnclosingClass());
        TypeElement te = s.getEnclosingClass();
        boolean ok;
        if (te == null)
            ok = ref.equals("0");
        else {
            CharSequence name = te.getQualifiedName();
            ok = ref.equals(name == null || name.length() == 0 ? "-" : name.toString());
        }
        if (!ok)
            error(s, ref, "bad enclosing class found: " + te);
        return ok;
    }
}
