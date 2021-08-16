/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6249843 6705893
 * @summary Test basic script compilation. Value eval'ed from
 * compiled and interpreted scripts should be same.
 */

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class Test6 {
        public static void main(String[] args) throws Exception {
            System.out.println("\nTest6\n");
            ScriptEngineManager m = new ScriptEngineManager();
            ScriptEngine engine = Helper.getJsEngine(m);
            if (engine == null) {
                System.out.println("Warning: No js engine found; test vacuously passes.");
                return;
            }

            try (Reader reader = new FileReader(
                new File(System.getProperty("test.src", "."), "Test6.js"))) {
                engine.eval(reader);
            }
            Object res = engine.get("res");

            CompiledScript scr = null;
            try (Reader reader = new FileReader(
                new File(System.getProperty("test.src", "."), "Test6.js"))) {
                scr = ((Compilable)engine).compile(reader);
            }

            if (scr == null) {
                throw new RuntimeException("compilation failed!");
            }

            scr.eval();
            Object res1 = engine.get("res");
            if (! res.equals(res1)) {
                throw new RuntimeException("values not equal");
            }
        }
}
