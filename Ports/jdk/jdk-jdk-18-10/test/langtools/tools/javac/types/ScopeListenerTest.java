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
 * @bug 8039262
 * @summary Ensure that using Types.membersClosure does not increase the number of listeners on the
 *          class's members Scope.
 * @modules jdk.compiler/com.sun.tools.javac.code:+open
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 */

import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Scope.ScopeListenerList;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;
import java.lang.reflect.Field;
import java.util.Collection;

public class ScopeListenerTest {

    public static void main(String[] args) throws Exception {
        new ScopeListenerTest().run();
    }

    void run() throws Exception {
        Context context = new Context();
        JavacFileManager.preRegister(context);
        Types types = Types.instance(context);
        Symtab syms = Symtab.instance(context);
        Names names = Names.instance(context);
        types.membersClosure(syms.stringType, true);
        types.membersClosure(syms.stringType, false);

        int listenerCount = listenerCount(syms.stringType.tsym.members());

        for (int i = 0; i < 100; i++) {
            types.membersClosure(syms.stringType, true);
            types.membersClosure(syms.stringType, false);
        }

        int newListenerCount = listenerCount(syms.stringType.tsym.members());

        if (listenerCount != newListenerCount) {
            throw new AssertionError("Orig listener count: " + listenerCount +
                                     "; new listener count: " + newListenerCount);
        }

        for (Symbol s : types.membersClosure(syms.stringType, true).getSymbols())
            ;
        for (Symbol s : types.membersClosure(syms.stringType, false).getSymbolsByName(names.fromString("substring")))
            ;
    }

    int listenerCount(Scope s) throws ReflectiveOperationException {
        Field listenersListField = Scope.class.getDeclaredField("listeners");
        listenersListField.setAccessible(true);
        Field listenersField = ScopeListenerList.class.getDeclaredField("listeners");
        listenersField.setAccessible(true);
        return ((Collection<?>)listenersField.get(listenersListField.get(s))).size();
    }

}
