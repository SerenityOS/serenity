/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8140450
 * @summary Basic test for the StackWalker::getByteCodeIndex method
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @run main TestBCI
 */

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Descriptor;
import com.sun.tools.classfile.LineNumberTable_attribute;
import com.sun.tools.classfile.Method;

import java.lang.StackWalker.StackFrame;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;

public class TestBCI {
    public static void main(String... args) throws Exception {
        TestBCI test = new TestBCI(Walker.class);
        System.out.println("Line number table:");
        test.methods.values().stream()
            .sorted(Comparator.comparing(MethodInfo::name).reversed())
            .forEach(System.out::println);

        // walk the stack
        test.walk();
    }

    private final Map<String, MethodInfo> methods;
    private final Class<?> clazz;
    TestBCI(Class<?> c) throws ConstantPoolException, IOException {
        Map<String, MethodInfo> methods;
        String filename = c.getName().replace('.', '/') + ".class";
        try (InputStream in = c.getResourceAsStream(filename)) {
            ClassFile cf = ClassFile.read(in);
            methods = Arrays.stream(cf.methods)
                .map(m -> new MethodInfo(cf, m))
                .collect(Collectors.toMap(MethodInfo::name, Function.identity()));
        }
        this.clazz = c;
        this.methods = methods;
    }

    void walk() {
        Walker walker = new Walker();
        walker.m1();
    }

    void verify(StackFrame frame) {
        if (frame.getDeclaringClass() != clazz)
            return;

        int bci = frame.getByteCodeIndex();
        int lineNumber = frame.getLineNumber();
        System.out.format("%s.%s bci %d (%s:%d)%n",
                          frame.getClassName(), frame.getMethodName(), bci,
                          frame.getFileName(), lineNumber);

        MethodInfo method = methods.get(frame.getMethodName());
        SortedSet<Integer> values = method.findLineNumbers(bci).get();
        if (!values.contains(lineNumber)) {
            throw new RuntimeException("line number for bci: " + bci + " "
                + lineNumber + " not matched line number table: " + values);
        }
    }

    /*
     * BCIs in the execution stack when StackWalker::forEach is invoked
     * will cover BCI range in the line number table.
     */
    class Walker {
        final StackWalker walker = StackWalker.getInstance(RETAIN_CLASS_REFERENCE);
        void m1() {
            int i = (int)Math.random()+2;
            m2(i*2);
        }

        void m2(int i) {
            i++;
            m3(i);
        }

        void m3(int i) {
            i++; m4(i++);
        }

        int m4(int i) {
            walker.forEach(TestBCI.this::verify);
            return i;
        }
    }

    static class MethodInfo {
        final Method method;
        final String name;
        final String paramTypes;
        final String returnType;
        final Map<Integer, SortedSet<Integer>> bciToLineNumbers = new HashMap<>();
        MethodInfo(ClassFile cf, Method m) {
            this.method = m;

            String name;
            String paramTypes;
            String returnType;
            LineNumberTable_attribute.Entry[] lineNumberTable;
            try {
                // method name
                name = m.getName(cf.constant_pool);
                // signature
                paramTypes = m.descriptor.getParameterTypes(cf.constant_pool);
                returnType = m.descriptor.getReturnType(cf.constant_pool);
                Code_attribute codeAttr = (Code_attribute)
                    m.attributes.get(Attribute.Code);
                lineNumberTable = ((LineNumberTable_attribute)
                    codeAttr.attributes.get(Attribute.LineNumberTable)).line_number_table;
            } catch (ConstantPoolException|Descriptor.InvalidDescriptor e) {
                throw new RuntimeException(e);
            }
            this.name = name;
            this.paramTypes = paramTypes;
            this.returnType = returnType;
            Arrays.stream(lineNumberTable).forEach(entry ->
                bciToLineNumbers.computeIfAbsent(entry.start_pc, _n -> new TreeSet<>())
                    .add(entry.line_number));
        }

        String name() {
            return name;
        }

        Optional<SortedSet<Integer>> findLineNumbers(int value) {
            return bciToLineNumbers.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                    .filter(e -> e.getKey().intValue() <= value)
                    .map(Map.Entry::getValue)
                    .findFirst();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(name);
            sb.append(paramTypes).append(returnType).append(" ");
            bciToLineNumbers.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sb.append("bci:").append(entry.getKey()).append(" ")
                                    .append(entry.getValue()).append(" "));
            return sb.toString();
        }
    }

}
