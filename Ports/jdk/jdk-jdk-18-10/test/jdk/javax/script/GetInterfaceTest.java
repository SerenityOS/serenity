/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6960211
 * @summary JavaScript engine allows creation of interface although methods not available.
 */

import javax.script.*;

public class GetInterfaceTest {
    public static void main(String[] args) throws Exception {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("nashorn");

        if (engine == null) {
            System.out.println("Warning: No js engine engine found; test vacuously passes.");
            return;
        }

        // don't define any function.
        engine.eval("");

        Runnable runnable = ((Invocable)engine).getInterface(Runnable.class);
        if (runnable != null) {
            throw new RuntimeException("runnable is not null!");
        }

        // now define "run"
        engine.eval("function run() { print('this is run function'); }");
        runnable = ((Invocable)engine).getInterface(Runnable.class);
        // should not return null now!
        runnable.run();

        // define only one method of "Foo2"
        engine.eval("function bar() { print('bar function'); }");
        Foo2 foo2 = ((Invocable)engine).getInterface(Foo2.class);
        if (foo2 != null) {
            throw new RuntimeException("foo2 is not null!");
        }

        // now define other method of "Foo2"
        engine.eval("function bar2() { print('bar2 function'); }");
        foo2 = ((Invocable)engine).getInterface(Foo2.class);
        foo2.bar();
        foo2.bar2();
    }

    public interface Foo {
        public void bar();
    }

    public interface Foo2 extends Foo {
        public void bar2();
    }
}
