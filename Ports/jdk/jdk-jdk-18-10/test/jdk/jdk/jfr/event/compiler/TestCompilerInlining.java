/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.compiler;

import jdk.internal.org.objectweb.asm.*;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedObject;
import jdk.test.lib.Asserts;
import jdk.test.lib.Platform;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import sun.hotspot.WhiteBox;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.IntStream;

/**
 * @test CompilerInliningTest
 * @bug 8073607
 * @key jfr
 * @summary Verifies that corresponding JFR events are emitted in case of inlining.
 * @requires vm.hasJFR
 *
 * @requires vm.opt.Inline == true | vm.opt.Inline == null
 * @library /test/lib
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          jdk.jfr
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:.
 *     -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *     -Xbatch jdk.jfr.event.compiler.TestCompilerInlining
 */
public class TestCompilerInlining {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final int LEVEL_SIMPLE = 1;
    private static final int LEVEL_FULL_OPTIMIZATION = 4;
    private static final Executable ENTRY_POINT = getConstructor(TestCase.class);
    private static final String TEST_CASE_CLASS_NAME = TestCase.class.getName().replace('.', '/');

    public static void main(String[] args) throws Exception {
        InlineCalls inlineCalls = new InlineCalls(TestCase.class);
        inlineCalls.disableInline(getConstructor(Object.class));
        inlineCalls.disableInline(getMethod(TestCase.class, "qux", boolean.class));
        inlineCalls.forceInline(getMethod(TestCase.class, "foo"));
        inlineCalls.forceInline(getMethod(TestCase.class, "foo", int.class));
        inlineCalls.forceInline(getMethod(TestCase.class, "bar"));
        inlineCalls.forceInline(getMethod(TestCase.class, "baz"));

        Map<Call, Boolean> result = inlineCalls.getExpected(ENTRY_POINT);
        for (int level : determineAvailableLevels()) {
            testLevel(result, level);
        }
    }

    private static void testLevel(Map<Call, Boolean> expectedResult, int level) throws IOException {
        System.out.println("****** Testing level " + level + " *******");
        Recording r = new Recording();
        r.enable(EventNames.CompilerInlining);
        r.start();
        WHITE_BOX.enqueueMethodForCompilation(ENTRY_POINT, level);
        WHITE_BOX.deoptimizeMethod(ENTRY_POINT);
        r.stop();
        System.out.println("Expected:");

        List<RecordedEvent> events = Events.fromRecording(r);
        Set<Call> foundEvents = new HashSet<>();
        int foundRelevantEvent = 0;
        for (RecordedEvent event : events) {
            RecordedMethod callerObject = event.getValue("caller");
            RecordedObject calleeObject = event.getValue("callee");
            MethodDesc caller = methodToMethodDesc(callerObject);
            MethodDesc callee = ciMethodToMethodDesc(calleeObject);
            // only TestCase.* -> TestCase.* OR TestCase.* -> Object.<init> are tested/filtered
            if (caller.className.equals(TEST_CASE_CLASS_NAME) && (callee.className.equals(TEST_CASE_CLASS_NAME)
                    || (callee.className.equals("java/lang/Object") && callee.methodName.equals("<init>")))) {
                System.out.println(event);
                boolean succeeded = (boolean) event.getValue("succeeded");
                int bci = Events.assertField(event, "bci").atLeast(0).getValue();
                Call call = new Call(caller, callee, bci);
                foundRelevantEvent++;
                Boolean expected = expectedResult.get(call);
                Asserts.assertNotNull(expected, "Unexpected inlined call : " + call);
                Asserts.assertEquals(expected, succeeded, "Incorrect result for " + call);
                Asserts.assertTrue(foundEvents.add(call), "repeated event for " + call);
            }
        }
        Asserts.assertEquals(foundRelevantEvent, expectedResult.size(), String.format("not all events found at lavel %d. " + "found = '%s'. expected = '%s'", level, events, expectedResult.keySet()));
        System.out.println();
        System.out.println();
    }

    private static int[] determineAvailableLevels() {
        if (WHITE_BOX.getBooleanVMFlag("TieredCompilation")) {
            return IntStream.rangeClosed(LEVEL_SIMPLE, WHITE_BOX.getIntxVMFlag("TieredStopAtLevel").intValue()).toArray();
        }
        if (Platform.isServer() && !Platform.isEmulatedClient()) {
            return new int[] { LEVEL_FULL_OPTIMIZATION };
        }
        if (Platform.isClient() || Platform.isEmulatedClient()) {
            return new int[] { LEVEL_SIMPLE };
        }
        throw new Error("TESTBUG: unknown VM");
    }

    private static MethodDesc methodToMethodDesc(RecordedMethod method) {
        String internalClassName = method.getType().getName().replace('.', '/');
        String methodName = method.getValue("name");
        String methodDescriptor = method.getValue("descriptor");
        return new MethodDesc(internalClassName, methodName, methodDescriptor);
    }

    private static MethodDesc ciMethodToMethodDesc(RecordedObject ciMethod) {
        String internalClassName = ciMethod.getValue("type");
        String methodName = ciMethod.getValue("name");
        String methodDescriptor = ciMethod.getValue("descriptor");
        return new MethodDesc(internalClassName, methodName, methodDescriptor);
    }

    private static Method getMethod(Class<?> aClass, String name, Class<?>... params) {
        try {
            return aClass.getDeclaredMethod(name, params);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new Error("TESTBUG : cannot get method " + name + Arrays.toString(params), e);
        }
    }

    private static Constructor<?> getConstructor(Class<?> aClass, Class<?>... params) {
        try {
            return aClass.getDeclaredConstructor(params);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new Error("TESTBUG : cannot get constructor" + Arrays.toString(params), e);
        }
    }
}

class TestCase {
    public TestCase() {
        foo();
    }

    public void foo() {
        qux(true);
        bar();
        foo(2);
    }

    private void foo(int i) {
    }

    private void bar() {
        baz();
        qux(false);
        qux(true);
    }

    protected static double baz() {
        qux(false);
        return .0;
    }

    private static int qux(boolean b) {
        qux(b);
        return 0;
    }
}

/**
 * data structure for method call
 */
class Call {
    public final MethodDesc caller;
    public final MethodDesc callee;
    public final int bci;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof Call))
            return false;

        Call call = (Call) o;

        if (bci != call.bci)
            return false;
        if (!callee.equals(call.callee))
            return false;
        if (!caller.equals(call.caller))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = caller.hashCode();
        result = 31 * result + callee.hashCode();
        result = 47 * result + bci;
        return result;
    }

    public Call(MethodDesc caller, MethodDesc callee, int bci) {
        Objects.requireNonNull(caller);
        Objects.requireNonNull(callee);
        this.caller = caller;
        this.callee = callee;
        this.bci = bci;
    }

    @Override
    public String toString() {
        return String.format("Call{caller='%s', callee='%s', bci=%d}", caller, callee, bci);
    }
}

/**
 * data structure for method description
 */
class MethodDesc {
    public final String className;
    public final String methodName;
    public final String descriptor;

    public MethodDesc(Class<?> aClass, String methodName, String descriptor) {
        this(aClass.getName().replace('.', '/'), methodName, descriptor);
    }

    public MethodDesc(String className, String methodName, String descriptor) {
        Objects.requireNonNull(className);
        Objects.requireNonNull(methodName);
        Objects.requireNonNull(descriptor);
        this.className = className.replace('.', '/');
        this.methodName = methodName;
        this.descriptor = descriptor;
    }

    public MethodDesc(Executable executable) {
        Class<?> aClass = executable.getDeclaringClass();
        className = Type.getInternalName(aClass).replace('.', '/');

        if (executable instanceof Constructor<?>) {
            methodName = "<init>";
            descriptor = Type.getConstructorDescriptor((Constructor<?>) executable);
        } else {
            methodName = executable.getName();
            descriptor = Type.getMethodDescriptor((Method) executable);
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        MethodDesc that = (MethodDesc) o;

        if (!className.equals(that.className))
            return false;
        if (!methodName.equals(that.methodName))
            return false;
        if (!descriptor.equals(that.descriptor))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = className.hashCode();
        result = 31 * result + methodName.hashCode();
        result = 47 * result + descriptor.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("MethodDesc{className='%s', methodName='%s', descriptor='%s'}", className, methodName, descriptor);
    }
}

/**
 * Aux class to get all calls in an arbitrary class.
 */
class InlineCalls {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    private final Collection<Call> calls;
    private final Map<Call, Boolean> inline;

    public InlineCalls(Class<?> aClass) {
        calls = getCalls(aClass);
        inline = new HashMap<>();
    }

    /**
     * @return expected inline events
     */
    public Map<Call, Boolean> getExpected(Executable entry) {
        Map<Call, Boolean> result = new HashMap<>();
        Queue<MethodDesc> methods = new ArrayDeque<>();
        Set<MethodDesc> finished = new HashSet<>();
        methods.add(new MethodDesc(entry));
        while (!methods.isEmpty()) {
            MethodDesc method = methods.poll();
            if (finished.add(method)) {
                inline.entrySet().stream().filter(k -> k.getKey().caller.equals(method)).forEach(k -> {
                    result.put(k.getKey(), k.getValue());
                    if (k.getValue()) {
                        methods.add(k.getKey().callee);
                    }
                });
            }
        }

        return result;
    }

    public void disableInline(Executable executable) {
        WHITE_BOX.testSetDontInlineMethod(executable, true);
        MethodDesc md = new MethodDesc(executable);
        calls.stream().filter(c -> c.callee.equals(md)).forEach(c -> inline.put(c, false));
    }

    public void forceInline(Executable executable) {
        WHITE_BOX.testSetForceInlineMethod(executable, true);
        MethodDesc md = new MethodDesc(executable);
        calls.stream().filter(c -> c.callee.equals(md)).forEach(c -> inline.putIfAbsent(c, true));
    }

    private static Collection<Call> getCalls(Class<?> aClass) {
        List<Call> calls = new ArrayList<>();
        ClassWriter cw;
        ClassReader cr;
        try {
            cr = new ClassReader(aClass.getName());
        } catch (IOException e) {
            throw new Error("TESTBUG : unexpected IOE during class reading", e);
        }
        cw = new ClassWriter(cr, 0);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM7, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String descriptor, String[] exceptions) {
                System.out.println("Method: " +name);
                MethodVisitor mv = super.visitMethod(access, name, desc, descriptor, exceptions);
                return new CallTracer(aClass, name, desc, mv, calls);
            }
        };
        cr.accept(cv, 0);

        return calls;
    }

    private static class CallTracer extends MethodVisitor {
        private final MethodDesc caller;
        private Collection<Call> calls;

        public CallTracer(Class<?> aClass, String name, String desc, MethodVisitor mv, Collection<Call> calls) {
            super(Opcodes.ASM7, mv);
            caller = new MethodDesc(aClass.getName(), name, desc);
            this.calls = calls;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            Label label = new Label();
            visitLabel(label);
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            calls.add(new Call(caller, new MethodDesc(owner, name, desc), label.getOffset()));
        }
    }
}
