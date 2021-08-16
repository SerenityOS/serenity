/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package gc.g1.unloading.bytecode;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import vm.share.InMemoryJavaCompiler;

/**
 * BytecodeFactory that employs in memory compilation.
 */
public class BytecodeGeneratorFactory implements BytecodeFactory {

    private Random random;

    private SourceGenerator sourceGenerator;

    public BytecodeGeneratorFactory(long seed) {
        random = new Random(seed);
        sourceGenerator = new SourceGenerator(random.nextLong());
    }

    @Override
    public Bytecode createBytecode(String className) {
        Map<String, CharSequence> sources = new HashMap<String, CharSequence>();
        sources.put(className, sourceGenerator.generateSource(className,
                "public static void main() { System.out.println(\"From main method in in-mem-compiled code " + random.nextGaussian() +
                        " + str_bytesToReplace0 str_bytesToReplace1\"); }\n " +
                        "public static int methodForCompilation(Object object) { int i = object.hashCode(); i = i * 2000 / 1994 + 153; return i; }\n"));
        byte[] bytecode = InMemoryJavaCompiler.compile(sources).values().iterator().next();
        return new Bytecode(className, bytecode);
    }

}
