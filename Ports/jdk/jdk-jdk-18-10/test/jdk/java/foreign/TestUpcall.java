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

/*
 * @test
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @modules jdk.incubator.foreign/jdk.internal.foreign
 * @build NativeTestHelper CallGeneratorHelper TestUpcall
 *
 * @run testng/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:-VerifyDependencies
 *   --enable-native-access=ALL-UNNAMED -Dgenerator.sample.factor=17
 *   TestUpcall
 */

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.SymbolLookup;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;

import jdk.incubator.foreign.ResourceScope;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.invoke.MethodHandles.insertArguments;
import static jdk.incubator.foreign.CLinker.C_POINTER;
import static org.testng.Assert.assertEquals;


public class TestUpcall extends CallGeneratorHelper {

    static {
        System.loadLibrary("TestUpcall");
    }
    static CLinker abi = CLinker.getInstance();

    static final SymbolLookup LOOKUP = SymbolLookup.loaderLookup();

    static MethodHandle DUMMY;
    static MethodHandle PASS_AND_SAVE;

    static {
        try {
            DUMMY = MethodHandles.lookup().findStatic(TestUpcall.class, "dummy", MethodType.methodType(void.class));
            PASS_AND_SAVE = MethodHandles.lookup().findStatic(TestUpcall.class, "passAndSave",
                    MethodType.methodType(Object.class, Object[].class, AtomicReference.class));
        } catch (Throwable ex) {
            throw new IllegalStateException(ex);
        }
    }

    static MemoryAddress dummyStub;

    @BeforeClass
    void setup() {
        dummyStub = abi.upcallStub(DUMMY, FunctionDescriptor.ofVoid(), ResourceScope.newImplicitScope());
    }

    @Test(dataProvider="functions", dataProviderClass=CallGeneratorHelper.class)
    public void testUpcalls(int count, String fName, Ret ret, List<ParamType> paramTypes, List<StructFieldType> fields) throws Throwable {
        List<Consumer<Object>> returnChecks = new ArrayList<>();
        List<Consumer<Object[]>> argChecks = new ArrayList<>();
        MemoryAddress addr = LOOKUP.lookup(fName).get();
        MethodType mtype = methodType(ret, paramTypes, fields);
        try (NativeScope scope = new NativeScope()) {
            MethodHandle mh = abi.downcallHandle(addr, scope, mtype, function(ret, paramTypes, fields));
            Object[] args = makeArgs(scope.scope(), ret, paramTypes, fields, returnChecks, argChecks);
            Object[] callArgs = args;
            Object res = mh.invokeWithArguments(callArgs);
            argChecks.forEach(c -> c.accept(args));
            if (ret == Ret.NON_VOID) {
                returnChecks.forEach(c -> c.accept(res));
            }
        }
    }

    @Test(dataProvider="functions", dataProviderClass=CallGeneratorHelper.class)
    public void testUpcallsNoScope(int count, String fName, Ret ret, List<ParamType> paramTypes, List<StructFieldType> fields) throws Throwable {
        List<Consumer<Object>> returnChecks = new ArrayList<>();
        List<Consumer<Object[]>> argChecks = new ArrayList<>();
        MemoryAddress addr = LOOKUP.lookup(fName).get();
        MethodType mtype = methodType(ret, paramTypes, fields);
        MethodHandle mh = abi.downcallHandle(addr, IMPLICIT_ALLOCATOR, mtype, function(ret, paramTypes, fields));
        Object[] args = makeArgs(ResourceScope.newImplicitScope(), ret, paramTypes, fields, returnChecks, argChecks);
        Object[] callArgs = args;
        Object res = mh.invokeWithArguments(callArgs);
        argChecks.forEach(c -> c.accept(args));
        if (ret == Ret.NON_VOID) {
            returnChecks.forEach(c -> c.accept(res));
        }
    }

    static MethodType methodType(Ret ret, List<ParamType> params, List<StructFieldType> fields) {
        MethodType mt = ret == Ret.VOID ?
                MethodType.methodType(void.class) : MethodType.methodType(paramCarrier(params.get(0).layout(fields)));
        for (ParamType p : params) {
            mt = mt.appendParameterTypes(paramCarrier(p.layout(fields)));
        }
        mt = mt.appendParameterTypes(MemoryAddress.class); //the callback
        return mt;
    }

    static FunctionDescriptor function(Ret ret, List<ParamType> params, List<StructFieldType> fields) {
        List<MemoryLayout> paramLayouts = params.stream().map(p -> p.layout(fields)).collect(Collectors.toList());
        paramLayouts.add(C_POINTER); // the callback
        MemoryLayout[] layouts = paramLayouts.toArray(new MemoryLayout[0]);
        return ret == Ret.VOID ?
                FunctionDescriptor.ofVoid(layouts) :
                FunctionDescriptor.of(layouts[0], layouts);
    }

    static Object[] makeArgs(ResourceScope scope, Ret ret, List<ParamType> params, List<StructFieldType> fields, List<Consumer<Object>> checks, List<Consumer<Object[]>> argChecks) throws ReflectiveOperationException {
        Object[] args = new Object[params.size() + 1];
        for (int i = 0 ; i < params.size() ; i++) {
            args[i] = makeArg(params.get(i).layout(fields), checks, i == 0);
        }
        args[params.size()] = makeCallback(scope, ret, params, fields, checks, argChecks);
        return args;
    }

    @SuppressWarnings("unchecked")
    static MemoryAddress makeCallback(ResourceScope scope, Ret ret, List<ParamType> params, List<StructFieldType> fields, List<Consumer<Object>> checks, List<Consumer<Object[]>> argChecks) {
        if (params.isEmpty()) {
            return dummyStub.address();
        }

        AtomicReference<Object[]> box = new AtomicReference<>();
        MethodHandle mh = insertArguments(PASS_AND_SAVE, 1, box);
        mh = mh.asCollector(Object[].class, params.size());

        for (int i = 0; i < params.size(); i++) {
            ParamType pt = params.get(i);
            MemoryLayout layout = pt.layout(fields);
            Class<?> carrier = paramCarrier(layout);
            mh = mh.asType(mh.type().changeParameterType(i, carrier));

            final int finalI = i;
            if (carrier == MemorySegment.class) {
                argChecks.add(o -> assertStructEquals((MemorySegment) box.get()[finalI], (MemorySegment) o[finalI], layout));
            } else {
                argChecks.add(o -> assertEquals(box.get()[finalI], o[finalI]));
            }
        }

        ParamType firstParam = params.get(0);
        MemoryLayout firstlayout = firstParam.layout(fields);
        Class<?> firstCarrier = paramCarrier(firstlayout);

        if (firstCarrier == MemorySegment.class) {
            checks.add(o -> assertStructEquals((MemorySegment) box.get()[0], (MemorySegment) o, firstlayout));
        } else {
            checks.add(o -> assertEquals(o, box.get()[0]));
        }

        mh = mh.asType(mh.type().changeReturnType(ret == Ret.VOID ? void.class : firstCarrier));

        MemoryLayout[] paramLayouts = params.stream().map(p -> p.layout(fields)).toArray(MemoryLayout[]::new);
        FunctionDescriptor func = ret != Ret.VOID
                ? FunctionDescriptor.of(firstlayout, paramLayouts)
                : FunctionDescriptor.ofVoid(paramLayouts);
        return abi.upcallStub(mh, func, scope);
    }

    static Object passAndSave(Object[] o, AtomicReference<Object[]> ref) {
        for (int i = 0; i < o.length; i++) {
            if (o[i] instanceof MemorySegment) {
                MemorySegment ms = (MemorySegment) o[i];
                MemorySegment copy = MemorySegment.allocateNative(ms.byteSize(), ResourceScope.newImplicitScope());
                copy.copyFrom(ms);
                o[i] = copy;
            }
        }
        ref.set(o);
        return o[0];
    }

    static void dummy() {
        //do nothing
    }
}
