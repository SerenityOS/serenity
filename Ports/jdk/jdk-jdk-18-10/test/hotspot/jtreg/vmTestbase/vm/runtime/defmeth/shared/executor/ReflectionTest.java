/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package vm.runtime.defmeth.shared.executor;

import nsk.share.TestFailure;
import nsk.share.test.TestUtils;
import vm.runtime.defmeth.shared.builder.TestBuilder;
import vm.runtime.defmeth.shared.data.AbstractVisitor;
import vm.runtime.defmeth.shared.DefMethTest;
import vm.runtime.defmeth.shared.MemoryClassLoader;
import vm.runtime.defmeth.shared.data.Clazz;
import vm.runtime.defmeth.shared.data.ConcreteClass;
import vm.runtime.defmeth.shared.data.Interface;
import vm.runtime.defmeth.shared.data.Visitor;
import vm.runtime.defmeth.shared.data.Tester;
import vm.runtime.defmeth.shared.data.method.AbstractMethod;
import vm.runtime.defmeth.shared.data.method.ConcreteMethod;
import vm.runtime.defmeth.shared.data.method.DefaultMethod;
import vm.runtime.defmeth.shared.data.method.body.CallMethod;
import vm.runtime.defmeth.shared.data.method.body.EmptyBody;
import vm.runtime.defmeth.shared.data.method.body.ReturnIntBody;
import vm.runtime.defmeth.shared.data.method.body.ThrowExBody;
import vm.runtime.defmeth.shared.data.method.result.IntResult;
import vm.runtime.defmeth.shared.data.method.result.ThrowExResult;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.lang.String.format;

/**
 * Test runner for invocation mode through Reflection API (Method.invoke).
 */
public class ReflectionTest extends AbstractReflectionTest {
    private TestBuilder builder;

    public ReflectionTest(MemoryClassLoader cl, TestBuilder builder, DefMethTest testInstance,
                          Collection<? extends Tester> tests) {
        super(testInstance, cl, tests);
        this.builder = builder;
    }

    public ReflectionTest(MemoryClassLoader cl, TestBuilder builder, DefMethTest testInstance,
                          Tester... tests) {
        this(cl, builder, testInstance, Arrays.asList(tests));
    }

    private class ReflectionTesterVisitor extends AbstractVisitor implements Visitor {
        private CallMethod call;
        private Object receiver;

        private Executable targetMethod;
        private Object[] values;

        private Tester tester;

        @Override
        public void visitTester(Tester t) {
            tester = t;
            try {
                // Resolve targetMethod & prepare invocation parameters
                t.getCall().visit(this);

                // Invoke resolved targetMethod and check returned value
                t.getResult().visit(this);
            } finally {
                tester = null;
            }
        }

        @Override
        public void visitCallMethod(CallMethod call) {
            // Cache call site info for future uses
            this.call = call;

            // NB! don't call prepareForInvocation yet - it can throw exceptions expected by a test
        }

        private void prepareForInvocation()
                throws IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException {
            Class<?> staticClass = resolve(call.staticClass());

            if (call.isConstructorCall()) {
                targetMethod = staticClass.getDeclaredConstructor();
            } else {
                String methodName = call.methodName();
                Class[] paramTypes = paramType(call.methodDesc());

                if (tester.getTestPrivateMethod() != true) {
                    targetMethod = staticClass.getMethod(methodName, paramTypes);
                } else {
                    try {
                        targetMethod = staticClass.getDeclaredMethod(methodName, paramTypes);
                    } catch (NoSuchMethodException nsme) {}

                    Class clazz = staticClass.getSuperclass();
                    while ((targetMethod == null) && (clazz != null)) {
                        try {
                            targetMethod = clazz.getDeclaredMethod(methodName, paramTypes);
                        } catch (NoSuchMethodException nsme) {}
                        clazz = clazz.getSuperclass();
                    }
                }

                // Check reflection info for Class.getMethod(...)
                checkReflectionInfo((Method)targetMethod);

                // Prepare receiver after resolving target method, because it can throw instantiation exception
                if (call.invokeInsn() != CallMethod.Invoke.STATIC) {
                    Class<?> receiverClass = resolve(call.receiverClass());
                    receiver = receiverClass.newInstance();
                } else {
                    // receiver == null; Method.invoke ignores first argument when static method is called
                }

                // Check reflection info for Class.getDeclaredMethod(...)
                try {
                    Method m = staticClass.getDeclaredMethod(methodName, paramTypes);
                    checkReflectionInfo(m);
                } catch (NoSuchMethodException e) {
                }
            }

            values = values(call.params());
        }

        /** Calculate Method.isDefault() property for a Method */
        private abstract class AbstractResultExtractor extends AbstractVisitor {
            public boolean isDefault;
            public void visitConcreteMethod(ConcreteMethod m) {
                isDefault = false;
                m.body().visit(this); // extract return value
            }

            public void visitAbstractMethod(AbstractMethod m) {
                isDefault = false;
            }

            public void visitDefaultMethod(DefaultMethod m) {
                isDefault = true;
                m.body().visit(this); // extract return value
            }

            public void visitEmptyBody(EmptyBody body) {
                // ignore body
            }

            public void visitThrowExBody(ThrowExBody body) {
                // ignore body
            }

            public void visitReturnIntBody(ReturnIntBody body) {
                // ignore body
            }

            public void visitResultInt(IntResult res) {
                // ignore body
            }

            public void visitCallMethod(CallMethod call) {
                // ignore body
            }
        }

        private vm.runtime.defmeth.shared.data.method.Method findMethod(Clazz clz, String name, String desc) {
            if (clz == null)  return null;

            // Look for the method declaration in current class only
            for (vm.runtime.defmeth.shared.data.method.Method m : clz.methods()) {
                if (name.equals(m.name()) && desc.equals(m.desc())) {
                    return m;
                }
            }

            return null;
        }

        /** Verify reflection info for a statically known method */
        private void checkReflectionInfo(Method method) {
            vm.runtime.defmeth.shared.data.method.Method m =
                    findMethod(call.staticClass(), call.methodName(), call.methodDesc());

            if (m != null) {
                int flags = m.acc();
                boolean shouldBeDefault = (m instanceof DefaultMethod) &&
                                          ((flags & ACC_PUBLIC) == ACC_PUBLIC) &&
                                          ((flags & ACC_STATIC) != ACC_STATIC) &&
                                          ((flags & ACC_ABSTRACT) != ACC_ABSTRACT);
                boolean shouldBeAbstract = Modifier.isAbstract(flags);

                // check isDefault property
                boolean isDefault = method.isDefault();
                if (shouldBeDefault != isDefault) {
                    throw new TestFailure(format("Reflection info for Method.isDefault() is invalid:" +
                            " expected: %b; actual: %b. Method info: %s vs %s",
                            shouldBeDefault, isDefault, method.toString(), m.toString()));
                }

                boolean isAbstract = Modifier.isAbstract(method.getModifiers());
                // check that the method w/ a body shouldn't be abstract
                if (shouldBeAbstract != isAbstract) {
                    throw new TestFailure(format("Method shouldn't be abstract: %s vs %s", method.toString(), m.toString()));
                }
            }
        }

        private Object invokeInTestContext(Method m, Object obj, Object... args)
                throws InvocationTargetException {
            Class<?> context = cl.getTestContext();
            try {
                // Invoke target method from TestContext using TestContext.invoke(Method, Object, Object...)
                Method invoker = context.getDeclaredMethod("invoke", Method.class, Object.class, Object[].class);
                return invoker.invoke(null, m, obj, args);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new TestFailure("Exception during reflection invocation", e.getCause());
            }
        }

        private Object invokeInTestContext(Constructor m, Object... args)
                throws InvocationTargetException {
            Class<?> context = cl.getTestContext();
            try {
                // Invoke target method from TestContext using TestContext.invoke(Constructor, Object...)
                Method invoker = context.getDeclaredMethod("invoke", Constructor.class, Object[].class);
                return invoker.invoke(null, m, args);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new TestFailure("Exception during reflection invocation", e.getCause());
            }
        }

        @Override
        public void visitResultInt(IntResult res) {
            try {
                prepareForInvocation();
                int result = (int) invokeInTestContext((Method)targetMethod, receiver, values);
                TestUtils.assertEquals(res.getExpected(), result);
            } catch (TestFailure e) {
                throw e; // no need to wrap test exception
            } catch (Exception e) {
                throw new TestFailure("Unexpected exception", e);
            }
        }

        private void checkExpectedException(ThrowExResult exceptionInfo, Throwable actualExc, boolean unwrap) {
            String expectedExcName = exceptionInfo.getExc().name();
            String initialExpectedExcName = expectedExcName;

            // *Error <==> *Exception correspondence for reflection invocation
            switch (expectedExcName) {
                case "java.lang.NoSuchMethodError":
                case "java.lang.InstantiationError":
                case "java.lang.IllegalAccessError":
                    expectedExcName = expectedExcName.replace("Error", "Exception");
                    break;
            }

            // Need to dig 2 levels down since there are 2 levels of indirection during invocation:
            // invokeInTestContext(...) => TestContext.invoke(...) => Method.invoke(...)
            // For some exceptions, it's not the case (like NMSE)
            Throwable target = actualExc;
            if (unwrap) {
                if (target.getCause() != null) target = target.getCause();
                if (target.getCause() != null) target = target.getCause();
            }

            Class<?> expectedExc;
            try {
                expectedExc = cl.getSystemClassLoader().loadClass(expectedExcName);
            } catch (ClassNotFoundException e) {
                throw new Error(e);
            }

            String excName = target.getClass().getName();
            if (!expectedExc.isAssignableFrom(target.getClass()) &&
                !initialExpectedExcName.equals(excName)) {
                throw new TestFailure(
                        format("Caught exception as expected, but it's type is wrong: expected: %s; actual: %s.",
                                expectedExcName, excName), target);
            }
        }

        @Override
        public void visitResultThrowExc(ThrowExResult res) {
            String expectedExcName = res.getExc().name();
            try {
                prepareForInvocation(); // can throw an exception expected by a test
                if (targetMethod instanceof Method) {
                    invokeInTestContext((Method)targetMethod, receiver, values);
                } else if (targetMethod instanceof Constructor) {
                    invokeInTestContext((Constructor)targetMethod, receiver, values);
                } else {
                    throw new InternalError("Unknown target: " + targetMethod);
                }
                throw new TestFailure("No exception was thrown: " + expectedExcName);
            } catch (IllegalAccessException | IllegalArgumentException | ClassNotFoundException e) {
                throw new TestFailure("Exception during reflection invocation", e.getCause());
            } catch (InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                checkExpectedException(res, e, true);
            } catch (Throwable e) {
                checkExpectedException(res, e, false);
            }
        }

        @Override
        public void visitResultIgnore() {
            try {
                prepareForInvocation();
                if (targetMethod instanceof Method) {
                    invokeInTestContext((Method)targetMethod, receiver, values);
                } else if (targetMethod instanceof Constructor) {
                    invokeInTestContext((Constructor)targetMethod, receiver, values);
                } else {
                    throw new InternalError("Unknown target: " + targetMethod);
                }
            } catch (Exception e) {
                throw new TestFailure("Unexpected exception", e);
            }
        }
    }

    /**
     * Run individual assertion for the test by it's name.
     *
     * @param test
     * @throws Throwable
     */
    public void run(Tester test) throws Throwable {
        test.visit(new ReflectionTesterVisitor());
    }
}
