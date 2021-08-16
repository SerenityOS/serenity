/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.classfile.*;

import java.io.IOException;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.*;

/**
 * Base class for LocalVariableTable and LocalVariableTypeTable attributes tests.
 * To add tests cases you should extend this class.
 * Then implement {@link #getVariableTables} to get LocalVariableTable or LocalVariableTypeTable attribute.
 * Then add method with local variables.
 * Finally, annotate method with information about expected variables and their types
 * by several {@link LocalVariableTestBase.ExpectedLocals} annotations.
 * To run test invoke {@link #test()} method.
 * If there are variables with the same name, set different scopes for them.
 *
 * @see #test()
 */
public abstract class LocalVariableTestBase extends TestBase {
    public static final int DEFAULT_SCOPE = 0;
    private final ClassFile classFile;
    private final Class<?> clazz;

    /**
     * @param clazz class to test. Must contains annotated methods with expected results.
     */
    public LocalVariableTestBase(Class<?> clazz) {
        this.clazz = clazz;
        try {
            this.classFile = ClassFile.read(getClassFile(clazz));
        } catch (IOException | ConstantPoolException e) {
            throw new IllegalArgumentException("Can't read classfile for specified class", e);
        }
    }

    protected abstract List<VariableTable> getVariableTables(Code_attribute codeAttribute);

    /**
     * Finds expected variables with their type in VariableTable.
     * Also does consistency checks, like variables from the same scope must point to different indexes.
     */
    public void test() throws IOException {
        List<java.lang.reflect.Method> testMethods = Stream.of(clazz.getDeclaredMethods())
                .filter(m -> m.getAnnotationsByType(ExpectedLocals.class).length > 0)
                .collect(toList());
        int failed = 0;
        for (java.lang.reflect.Method method : testMethods) {
            try {
                Map<String, String> expectedLocals2Types = new HashMap<>();
                Map<String, Integer> sig2scope = new HashMap<>();
                for (ExpectedLocals anno : method.getDeclaredAnnotationsByType(ExpectedLocals.class)) {
                    expectedLocals2Types.put(anno.name(), anno.type());
                    sig2scope.put(anno.name() + "&" + anno.type(), anno.scope());
                }

                test(method.getName(), expectedLocals2Types, sig2scope);
            } catch (AssertionFailedException ex) {
                System.err.printf("Test %s failed.%n", method.getName());
                ex.printStackTrace();
                failed++;
            }
        }
        if (failed > 0)
            throw new RuntimeException(format("Failed %d out of %d. See logs.", failed, testMethods.size()));
    }

    public void test(String methodName, Map<String, String> expectedLocals2Types, Map<String, Integer> sig2scope)
            throws IOException {

        for (Method m : classFile.methods) {
            String mName = getString(m.name_index);
            if (methodName.equals(mName)) {
                System.out.println("Testing local variable table in method " + mName);
                Code_attribute code_attribute = (Code_attribute) m.attributes.get(Attribute.Code);

                List<? extends VariableTable> variableTables = getVariableTables(code_attribute);
                generalLocalVariableTableCheck(variableTables);

                List<VariableTable.Entry> entries = variableTables.stream()
                        .flatMap(table -> table.entries().stream())
                        .collect(toList());

                generalEntriesCheck(entries, code_attribute);
                assertIndexesAreUnique(entries, sig2scope);
                checkNamesAndTypes(entries, expectedLocals2Types);
                checkDoubleAndLongIndexes(entries, sig2scope, code_attribute.max_locals);
            }
        }
    }

    private void generalLocalVariableTableCheck(List<? extends VariableTable> variableTables) {
        for (VariableTable localTable : variableTables) {
            //only one per variable.
            assertEquals(localTable.localVariableTableLength(),
                    localTable.entries().size(), "Incorrect local variable table length");
            //attribute length is offset(line_number_table_length) + element_size*element_count
            assertEquals(localTable.attributeLength(),
                    2 + (5 * 2) * localTable.localVariableTableLength(), "Incorrect attribute length");
        }
    }

    private void generalEntriesCheck(List<VariableTable.Entry> entries, Code_attribute code_attribute) {
        for (VariableTable.Entry e : entries) {
            assertTrue(e.index() >= 0 && e.index() < code_attribute.max_locals,
                    "Index " + e.index() + " out of variable array. Size of array is " + code_attribute.max_locals);
            assertTrue(e.startPC() >= 0, "StartPC is less then 0. StartPC = " + e.startPC());
            assertTrue(e.length() >= 0, "Length is less then 0. Length = " + e.length());
            assertTrue(e.startPC() + e.length() <= code_attribute.code_length,
                    format("StartPC+Length > code length.%n" +
                            "%s%n" +
                            "code_length = %s"
                            , e, code_attribute.code_length));
        }
    }

    private void checkNamesAndTypes(List<VariableTable.Entry> entries,
                                    Map<String, String> expectedLocals2Types) {
        Map<String, List<String>> actualNames2Types = entries.stream()
                .collect(
                        groupingBy(VariableTable.Entry::name,
                                mapping(VariableTable.Entry::type, toList())));
        for (Map.Entry<String, String> name2type : expectedLocals2Types.entrySet()) {
            String name = name2type.getKey();
            String type = name2type.getValue();

            assertTrue(actualNames2Types.containsKey(name),
                    format("There is no record for local variable %s%nEntries: %s", name, entries));

            assertTrue(actualNames2Types.get(name).contains(type),
                    format("Types are different for local variable %s%nExpected type: %s%nActual type: %s",
                            name, type, actualNames2Types.get(name)));
        }
    }


    private void assertIndexesAreUnique(Collection<VariableTable.Entry> entries, Map<String, Integer> scopes) {
        //check every scope separately
        Map<Object, List<VariableTable.Entry>> entriesByScope = groupByScope(entries, scopes);
        for (Map.Entry<Object, List<VariableTable.Entry>> mapEntry : entriesByScope.entrySet()) {
            mapEntry.getValue().stream()
                    .collect(groupingBy(VariableTable.Entry::index))
                    .entrySet()
                    .forEach(e ->
                            assertTrue(e.getValue().size() == 1,
                                    "Multiple variables point to the same index in common scope. " + e.getValue()));
        }

    }

    private void checkDoubleAndLongIndexes(Collection<VariableTable.Entry> entries,
                                           Map<String, Integer> scopes, int maxLocals) {
        //check every scope separately
        Map<Object, List<VariableTable.Entry>> entriesByScope = groupByScope(entries, scopes);
        for (List<VariableTable.Entry> entryList : entriesByScope.values()) {
            Map<Integer, VariableTable.Entry> index2Entry = entryList.stream()
                    .collect(toMap(VariableTable.Entry::index, e -> e));

            entryList.stream()
                    .filter(e -> "J".equals(e.type()) || "D".equals(e.type()))
                    .forEach(e -> {
                        assertTrue(e.index() + 1 < maxLocals,
                                format("Index %s is out of variable array. Long and double occupy 2 cells." +
                                        " Size of array is %d", e.index() + 1, maxLocals));
                        assertTrue(!index2Entry.containsKey(e.index() + 1),
                                format("An entry points to the second cell of long/double entry.%n%s%n%s", e,
                                        index2Entry.get(e.index() + 1)));
                    });
        }
    }

    private Map<Object, List<VariableTable.Entry>> groupByScope(
            Collection<VariableTable.Entry> entries, Map<String, Integer> scopes) {
        return entries.stream().collect(groupingBy(e -> scopes.getOrDefault(e.name() + "&" + e.type(), DEFAULT_SCOPE)));
    }

    protected String getString(int i) {
        try {
            return classFile.constant_pool.getUTF8Info(i).value;
        } catch (ConstantPool.InvalidIndex | ConstantPool.UnexpectedEntry ex) {
            ex.printStackTrace();
            throw new AssertionFailedException("Issue while reading constant pool");
        }
    }

    /**
     * LocalVariableTable and LocalVariableTypeTable are similar.
     * VariableTable interface is introduced to test this attributes in the same way without code duplication.
     */
    interface VariableTable {

        int localVariableTableLength();

        List<VariableTable.Entry> entries();

        int attributeLength();

        interface Entry {

            int index();

            int startPC();

            int length();

            String name();

            String type();

            default String dump() {
                return format("Entry{" +
                        "%n    name    = %s" +
                        "%n    type    = %s" +
                        "%n    index   = %d" +
                        "%n    startPC = %d" +
                        "%n    length  = %d" +
                        "%n}", name(), type(), index(), startPC(), length());
            }
        }
    }

    /**
     * Used to store expected results in sources
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Container.class)
    @interface ExpectedLocals {
        /**
         * @return name of a local variable
         */
        String name();

        /**
         * @return type of local variable in the internal format.
         */
        String type();

        //variables from different scopes can share the local variable table index and/or name.
        int scope() default DEFAULT_SCOPE;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Container {
        ExpectedLocals[] value();
    }
}
