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
 * @build Checker CheckIsAccessible
 * @run main CheckIsAccessible
 */

import java.util.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;

/*
 * Check the accessibility of items of a scope against the contents of string literals.
 */
public class CheckIsAccessible extends Checker {
    public static void main(String... args) throws Exception {
        Checker chk = new CheckIsAccessible();
        chk.check("TestIsAccessible.java", "A.java");
    }

    @Override
    protected boolean check(Scope s, String ref) {
        System.err.println("checkIsAccessible: " + s + " " + s.getEnclosingClass() + " " + ref);
        if (ref.length() == 0)
            return true;

        Trees trees = getTrees();
        String[] args = ref.split(" +", 3);
        boolean expect = args[args.length - 1].equals("yes");
        boolean actual;
        switch (args.length) {
        case 2:
            TypeElement te = getTypeElement(args[0]);
            actual = trees.isAccessible(s, te);
            if (actual != expect)
                error(s, ref, "accessible issue found: " + te + " " + actual);
            break;

        case 3:
            DeclaredType site = getType(args[0]);
            Element member = getMember(args[1]);
            actual = trees.isAccessible(s, member, site);
            if (actual != expect)
                error(s, ref, "accessible issue found: " + member + "@" + site + " " + actual);
            break;

        default:
            throw new IllegalArgumentException(ref);
        }

        return (actual == expect);
    }

    private TypeElement getTypeElement(String name) {
        TypeElement te = getElements().getTypeElement(name);
        if (te == null)
            throw new IllegalArgumentException("can't find element " + name);
        return te;
    }

    private DeclaredType getType(String name) {
        return (DeclaredType)(getTypeElement(name).asType());
    }

    private Element getMember(String name) {
        int sep = name.indexOf("#");
        String tname = name.substring(0, sep);
        String mname = name.substring(sep+1);
        TypeElement te = getTypeElement(tname);
        for (Element e: te.getEnclosedElements()) {
            if (mname.contentEquals(e.getSimpleName()))
                return e;
        }
        throw new IllegalArgumentException("can't find member " + mname + " in " + tname);
    }

}
