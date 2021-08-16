/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

import jdk.incubator.foreign.GroupLayout;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import jdk.incubator.foreign.ValueLayout;

import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.testng.annotations.*;

import static jdk.incubator.foreign.CLinker.*;
import static org.testng.Assert.*;

public class CallGeneratorHelper extends NativeTestHelper {

    static SegmentAllocator IMPLICIT_ALLOCATOR = (size, align) -> MemorySegment.allocateNative(size, align, ResourceScope.newImplicitScope());

    static final int SAMPLE_FACTOR = Integer.parseInt((String)System.getProperties().getOrDefault("generator.sample.factor", "-1"));

    static final int MAX_FIELDS = 3;
    static final int MAX_PARAMS = 3;
    static final int CHUNK_SIZE = 600;

    public static void assertStructEquals(MemorySegment actual, MemorySegment expected, MemoryLayout layout) {
        assertEquals(actual.byteSize(), expected.byteSize());
        GroupLayout g = (GroupLayout) layout;
        for (MemoryLayout field : g.memberLayouts()) {
            if (field instanceof ValueLayout) {
                VarHandle vh = g.varHandle(vhCarrier(field), MemoryLayout.PathElement.groupElement(field.name().orElseThrow()));
                assertEquals(vh.get(actual), vh.get(expected));
            }
        }
    }

    private static Class<?> vhCarrier(MemoryLayout layout) {
        if (layout instanceof ValueLayout) {
            if (isIntegral(layout)) {
                if (layout.bitSize() == 64) {
                    return long.class;
                }
                return int.class;
            } else if (layout.bitSize() == 32) {
                return float.class;
            }
            return double.class;
        } else {
            throw new IllegalStateException("Unexpected layout: " + layout);
        }
    }

    enum Ret {
        VOID,
        NON_VOID
    }

    enum StructFieldType {
        INT("int", C_INT),
        FLOAT("float", C_FLOAT),
        DOUBLE("double", C_DOUBLE),
        POINTER("void*", C_POINTER);

        final String typeStr;
        final MemoryLayout layout;

        StructFieldType(String typeStr, MemoryLayout layout) {
            this.typeStr = typeStr;
            this.layout = layout;
        }

        MemoryLayout layout() {
            return layout;
        }

        @SuppressWarnings("unchecked")
        static List<List<StructFieldType>>[] perms = new List[10];

        static List<List<StructFieldType>> perms(int i) {
            if (perms[i] == null) {
                perms[i] = generateTest(i, values());
            }
            return perms[i];
        }
    }

    enum ParamType {
        INT("int", C_INT),
        FLOAT("float", C_FLOAT),
        DOUBLE("double", C_DOUBLE),
        POINTER("void*", C_POINTER),
        STRUCT("struct S", null);

        private final String typeStr;
        private final MemoryLayout layout;

        ParamType(String typeStr, MemoryLayout layout) {
            this.typeStr = typeStr;
            this.layout = layout;
        }

        String type(List<StructFieldType> fields) {
            return this == STRUCT ?
                    typeStr + "_" + sigCode(fields) :
                    typeStr;
        }

        MemoryLayout layout(List<StructFieldType> fields) {
            if (this == STRUCT) {
                long offset = 0L;
                List<MemoryLayout> layouts = new ArrayList<>();
                for (StructFieldType field : fields) {
                    MemoryLayout l = field.layout();
                    long padding = offset % l.bitSize();
                    if (padding != 0) {
                        layouts.add(MemoryLayout.paddingLayout(padding));
                        offset += padding;
                    }
                    layouts.add(l.withName("field" + offset));
                    offset += l.bitSize();
                }
                return MemoryLayout.structLayout(layouts.toArray(new MemoryLayout[0]));
            } else {
                return layout;
            }
        }

        @SuppressWarnings("unchecked")
        static List<List<ParamType>>[] perms = new List[10];

        static List<List<ParamType>> perms(int i) {
            if (perms[i] == null) {
                perms[i] = generateTest(i, values());
            }
            return perms[i];
        }
    }

    static <Z> List<List<Z>> generateTest(int i, Z[] elems) {
        List<List<Z>> res = new ArrayList<>();
        generateTest(i, new Stack<>(), elems, res);
        return res;
    }

    static <Z> void generateTest(int i, Stack<Z> combo, Z[] elems, List<List<Z>> results) {
        if (i == 0) {
            results.add(new ArrayList<>(combo));
        } else {
            for (Z z : elems) {
                combo.push(z);
                generateTest(i - 1, combo, elems, results);
                combo.pop();
            }
        }
    }

    @DataProvider(name = "functions")
    public static Object[][] functions() {
        int functions = 0;
        List<Object[]> downcalls = new ArrayList<>();
        for (Ret r : Ret.values()) {
            for (int i = 0; i <= MAX_PARAMS; i++) {
                if (r != Ret.VOID && i == 0) continue;
                for (List<ParamType> ptypes : ParamType.perms(i)) {
                    String retCode = r == Ret.VOID ? "V" : ptypes.get(0).name().charAt(0) + "";
                    String sigCode = sigCode(ptypes);
                    if (ptypes.contains(ParamType.STRUCT)) {
                        for (int j = 1; j <= MAX_FIELDS; j++) {
                            for (List<StructFieldType> fields : StructFieldType.perms(j)) {
                                String structCode = sigCode(fields);
                                int count = functions;
                                int fCode = functions++ / CHUNK_SIZE;
                                String fName = String.format("f%d_%s_%s_%s", fCode, retCode, sigCode, structCode);
                                if (SAMPLE_FACTOR == -1 || (count % SAMPLE_FACTOR) == 0) {
                                    downcalls.add(new Object[]{count, fName, r, ptypes, fields});
                                }
                            }
                        }
                    } else {
                        String structCode = sigCode(List.<StructFieldType>of());
                        int count = functions;
                        int fCode = functions++ / CHUNK_SIZE;
                        String fName = String.format("f%d_%s_%s_%s", fCode, retCode, sigCode, structCode);
                        if (SAMPLE_FACTOR == -1 || (count % SAMPLE_FACTOR) == 0) {
                            downcalls.add(new Object[]{count, fName, r, ptypes, List.of()});
                        }
                    }
                }
            }
        }
        return downcalls.toArray(new Object[0][]);
    }

    static <Z extends Enum<Z>> String sigCode(List<Z> elems) {
        return elems.stream().map(p -> p.name().charAt(0) + "").collect(Collectors.joining());
    }

    static void generateStructDecl(List<StructFieldType> fields) {
        String structCode = sigCode(fields);
        List<String> fieldDecls = new ArrayList<>();
        for (int i = 0 ; i < fields.size() ; i++) {
            fieldDecls.add(String.format("%s p%d;", fields.get(i).typeStr, i));
        }
        String res = String.format("struct S_%s { %s };", structCode,
                fieldDecls.stream().collect(Collectors.joining(" ")));
        System.out.println(res);
    }

    /* this can be used to generate the test header/implementation */
    public static void main(String[] args) {
        boolean header = args.length > 0 && args[0].equals("header");
        boolean upcall = args.length > 1 && args[1].equals("upcall");
        if (upcall) {
            generateUpcalls(header);
        } else {
            generateDowncalls(header);
        }
    }

    static void generateDowncalls(boolean header) {
        if (header) {
            System.out.println(
                "#ifdef _WIN64\n" +
                "#define EXPORT __declspec(dllexport)\n" +
                "#else\n" +
                "#define EXPORT\n" +
                "#endif\n"
            );

            for (int j = 1; j <= MAX_FIELDS; j++) {
                for (List<StructFieldType> fields : StructFieldType.perms(j)) {
                    generateStructDecl(fields);
                }
            }
        } else {
            System.out.println(
                "#include \"libh\"\n" +
                "#ifdef __clang__\n" +
                "#pragma clang optimize off\n" +
                "#elif defined __GNUC__\n" +
                "#pragma GCC optimize (\"O0\")\n" +
                "#elif defined _MSC_BUILD\n" +
                "#pragma optimize( \"\", off )\n" +
                "#endif\n"
            );
        }

        for (Object[] downcall : functions()) {
            String fName = (String)downcall[0];
            Ret r = (Ret)downcall[1];
            @SuppressWarnings("unchecked")
            List<ParamType> ptypes = (List<ParamType>)downcall[2];
            @SuppressWarnings("unchecked")
            List<StructFieldType> fields = (List<StructFieldType>)downcall[3];
            generateDowncallFunction(fName, r, ptypes, fields, header);
        }
    }

    static void generateDowncallFunction(String fName, Ret ret, List<ParamType> params, List<StructFieldType> fields, boolean declOnly) {
        String retType = ret == Ret.VOID ? "void" : params.get(0).type(fields);
        List<String> paramDecls = new ArrayList<>();
        for (int i = 0 ; i < params.size() ; i++) {
            paramDecls.add(String.format("%s p%d", params.get(i).type(fields), i));
        }
        String sig = paramDecls.isEmpty() ?
                "void" :
                paramDecls.stream().collect(Collectors.joining(", "));
        String body = ret == Ret.VOID ? "{ }" : "{ return p0; }";
        String res = String.format("EXPORT %s f%s(%s) %s", retType, fName,
                sig, declOnly ? ";" : body);
        System.out.println(res);
    }

    static void generateUpcalls(boolean header) {
        if (header) {
            System.out.println(
                "#ifdef _WIN64\n" +
                "#define EXPORT __declspec(dllexport)\n" +
                "#else\n" +
                "#define EXPORT\n" +
                "#endif\n"
            );

            for (int j = 1; j <= MAX_FIELDS; j++) {
                for (List<StructFieldType> fields : StructFieldType.perms(j)) {
                    generateStructDecl(fields);
                }
            }
        } else {
            System.out.println(
                "#include \"libh\"\n" +
                "#ifdef __clang__\n" +
                "#pragma clang optimize off\n" +
                "#elif defined __GNUC__\n" +
                "#pragma GCC optimize (\"O0\")\n" +
                "#elif defined _MSC_BUILD\n" +
                "#pragma optimize( \"\", off )\n" +
                "#endif\n"
            );
        }

        for (Object[] downcall : functions()) {
            String fName = (String)downcall[0];
            Ret r = (Ret)downcall[1];
            @SuppressWarnings("unchecked")
            List<ParamType> ptypes = (List<ParamType>)downcall[2];
            @SuppressWarnings("unchecked")
            List<StructFieldType> fields = (List<StructFieldType>)downcall[3];
            generateUpcallFunction(fName, r, ptypes, fields, header);
        }
    }

    static void generateUpcallFunction(String fName, Ret ret, List<ParamType> params, List<StructFieldType> fields, boolean declOnly) {
        String retType = ret == Ret.VOID ? "void" : params.get(0).type(fields);
        List<String> paramDecls = new ArrayList<>();
        for (int i = 0 ; i < params.size() ; i++) {
            paramDecls.add(String.format("%s p%d", params.get(i).type(fields), i));
        }
        String paramNames = IntStream.range(0, params.size())
                .mapToObj(i -> "p" + i)
                .collect(Collectors.joining(","));
        String sig = paramDecls.isEmpty() ?
                "" :
                paramDecls.stream().collect(Collectors.joining(", ")) + ", ";
        String body = String.format(ret == Ret.VOID ? "{ cb(%s); }" : "{ return cb(%s); }", paramNames);
        List<String> paramTypes = params.stream().map(p -> p.type(fields)).collect(Collectors.toList());
        String cbSig = paramTypes.isEmpty() ?
                "void" :
                paramTypes.stream().collect(Collectors.joining(", "));
        String cbParam = String.format("%s (*cb)(%s)",
                retType, cbSig);

        String res = String.format("EXPORT %s %s(%s %s) %s", retType, fName,
                sig, cbParam, declOnly ? ";" : body);
        System.out.println(res);
    }

    //helper methods

    @SuppressWarnings("unchecked")
    static Object makeArg(MemoryLayout layout, List<Consumer<Object>> checks, boolean check) throws ReflectiveOperationException {
        if (layout instanceof GroupLayout) {
            MemorySegment segment = MemorySegment.allocateNative(layout, ResourceScope.newImplicitScope());
            initStruct(segment, (GroupLayout)layout, checks, check);
            return segment;
        } else if (isPointer(layout)) {
            MemorySegment segment = MemorySegment.allocateNative(1, ResourceScope.newImplicitScope());
            if (check) {
                checks.add(o -> {
                    try {
                        assertEquals(o, segment.address());
                    } catch (Throwable ex) {
                        throw new IllegalStateException(ex);
                    }
                });
            }
            return segment.address();
        } else if (layout instanceof ValueLayout) {
            if (isIntegral(layout)) {
                if (check) {
                    checks.add(o -> assertEquals(o, 42));
                }
                return 42;
            } else if (layout.bitSize() == 32) {
                if (check) {
                    checks.add(o -> assertEquals(o, 12f));
                }
                return 12f;
            } else {
                if (check) {
                    checks.add(o -> assertEquals(o, 24d));
                }
                return 24d;
            }
        } else {
            throw new IllegalStateException("Unexpected layout: " + layout);
        }
    }

    static void initStruct(MemorySegment str, GroupLayout g, List<Consumer<Object>> checks, boolean check) throws ReflectiveOperationException {
        for (MemoryLayout l : g.memberLayouts()) {
            if (l.isPadding()) continue;
            VarHandle accessor = g.varHandle(structFieldCarrier(l), MemoryLayout.PathElement.groupElement(l.name().get()));
            List<Consumer<Object>> fieldsCheck = new ArrayList<>();
            Object value = makeArg(l, fieldsCheck, check);
            if (isPointer(l)) {
                value = ((MemoryAddress)value).toRawLongValue();
            }
            //set value
            accessor.set(str, value);
            //add check
            if (check) {
                assertTrue(fieldsCheck.size() == 1);
                checks.add(o -> {
                    MemorySegment actual = (MemorySegment)o;
                    try {
                        if (isPointer(l)) {
                            fieldsCheck.get(0).accept(MemoryAddress.ofLong((long)accessor.get(actual)));
                        } else {
                            fieldsCheck.get(0).accept(accessor.get(actual));
                        }
                    } catch (Throwable ex) {
                        throw new IllegalStateException(ex);
                    }
                });
            }
        }
    }

    static Class<?> structFieldCarrier(MemoryLayout layout) {
        if (isPointer(layout)) {
            return long.class;
        } else if (layout instanceof ValueLayout) {
            if (isIntegral(layout)) {
                return int.class;
            } else if (layout.bitSize() == 32) {
                return float.class;
            } else {
                return double.class;
            }
        } else {
            throw new IllegalStateException("Unexpected layout: " + layout);
        }
    }

    static Class<?> paramCarrier(MemoryLayout layout) {
        if (layout instanceof GroupLayout) {
            return MemorySegment.class;
        } if (isPointer(layout)) {
            return MemoryAddress.class;
        } else if (layout instanceof ValueLayout) {
            if (isIntegral(layout)) {
                return int.class;
            } else if (layout.bitSize() == 32) {
                return float.class;
            } else {
                return double.class;
            }
        } else {
            throw new IllegalStateException("Unexpected layout: " + layout);
        }
    }
}
