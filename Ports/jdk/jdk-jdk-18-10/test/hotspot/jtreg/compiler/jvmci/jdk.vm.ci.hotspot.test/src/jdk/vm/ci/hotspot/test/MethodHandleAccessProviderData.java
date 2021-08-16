/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.vm.ci.hotspot.test;

import jdk.vm.ci.hotspot.HotSpotConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.MethodHandleAccessProvider;
import jdk.vm.ci.runtime.JVMCI;
import org.testng.annotations.DataProvider;

import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

public class MethodHandleAccessProviderData implements TestInterface {
    private static final MetaAccessProvider META_ACCESS = JVMCI.getRuntime().getHostJVMCIBackend().getMetaAccess();
    private static final HotSpotConstantReflectionProvider CONSTANT_REFLECTION = (HotSpotConstantReflectionProvider) JVMCI.getRuntime().getHostJVMCIBackend().getConstantReflection();
    // see DirectMethodHandle.java to check invoke* method names assignment
    private static final String IVIRTUAL_RESOLVED_NAME = "invokeVirtual";
    private static final String ISTATIC_RESOLVED_NAME = "invokeStatic";
    private static final String ISPECIAL_RESOLVED_NAME = "invokeSpecial";
    private static final String ISTATICINIT_RESOLVED_NAME = "invokeStaticInit";
    private static final String IINTERFACE_RESOLVED_NAME = "invokeInterface";
    private static final String INEWSPECIAL_RESOLVED_NAME = "newInvokeSpecial";

    @DataProvider(name = "intrinsicsPositive")
    public static Object[][] getIntrinsicsDataPositive() {
        Object[][] result;
        try {
            result = new Object[][]{
                            new Object[]{
                                            META_ACCESS.lookupJavaMethod(MethodHandle.class.getDeclaredMethod("invokeBasic", Object[].class)),
                                            MethodHandleAccessProvider.IntrinsicMethod.INVOKE_BASIC},
                            new Object[]{
                                            META_ACCESS.lookupJavaMethod(MethodHandle.class.getDeclaredMethod("linkToInterface", Object[].class)),
                                            MethodHandleAccessProvider.IntrinsicMethod.LINK_TO_INTERFACE},
                            new Object[]{
                                            META_ACCESS.lookupJavaMethod(MethodHandle.class.getDeclaredMethod("linkToStatic", Object[].class)),
                                            MethodHandleAccessProvider.IntrinsicMethod.LINK_TO_STATIC},
                            new Object[]{
                                            META_ACCESS.lookupJavaMethod(MethodHandle.class.getDeclaredMethod("linkToVirtual", Object[].class)),
                                            MethodHandleAccessProvider.IntrinsicMethod.LINK_TO_VIRTUAL},
                            new Object[]{
                                            META_ACCESS.lookupJavaMethod(MethodHandle.class.getDeclaredMethod("linkToSpecial", Object[].class)),
                                            MethodHandleAccessProvider.IntrinsicMethod.LINK_TO_SPECIAL}};
        } catch (NoSuchMethodException e) {
            throw new Error("TESTBUG: can't find method: " + e, e);
        }
        return result;
    }

    @DataProvider(name = "intrinsicsNegative")
    public static Object[][] getIntrinsicsDataNegative() {
        Object[][] result;
        try {
            result = new Object[][]{new Object[]{META_ACCESS.lookupJavaMethod(MethodHandle.class.getDeclaredMethod("invokeWithArguments", Object[].class))}};
        } catch (NoSuchMethodException e) {
            throw new Error("TESTBUG: can't find method: " + e, e);
        }
        return result;
    }

    @DataProvider(name = "invokeBasicNegative1")
    public static Object[][] getInvokeBasicDataNegative1() {
        Object[][] result;
        JavaConstant wrongObject = CONSTANT_REFLECTION.forObject("42");
        // 2nd parameter is force bytecode generation = true/false
        result = new Object[][]{
                        new Object[]{wrongObject, true},
                        new Object[]{wrongObject, false},
                        new Object[]{JavaConstant.NULL_POINTER, true},
                        new Object[]{JavaConstant.NULL_POINTER, false}};
        return result;
    }

    @DataProvider(name = "invokeBasicNegative2")
    public static Object[][] getInvokeBasicDataNegative2() {
        Object[][] result;
        // 2nd parameter is force bytecode generation = true/false
        result = new Object[][]{
                        new Object[]{null, true},
                        new Object[]{null, false}};
        return result;
    }

    @DataProvider(name = "invokeBasicPositive")
    public static Object[][] getInvokeBasicDataPositive() {
        Object[][] result;
        MethodHandle mhIVirtual;
        MethodHandle mhIStatic;
        MethodHandle mhISpecial;
        MethodHandle mhIStaticInit;
        MethodHandle mhINewSpecial;
        MethodHandle mhIInterface;
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType voidRet = MethodType.methodType(void.class);
        Class<?> self = MethodHandleAccessProviderData.class;
        // let's get a separate, thus, not initialized class
        ClassLoader current = self.getClassLoader();
        String[] cpaths = System.getProperty("java.class.path").split(File.pathSeparator);
        URL[] urls = new URL[cpaths.length];
        try {
            for (int i = 0; i < cpaths.length; i++) {
                urls[i] = Paths.get(cpaths[i]).toUri().toURL();
            }
        } catch (MalformedURLException e) {
            throw new Error("Can't parse classpath url: " + e, e);
        }
        Class<?> clone;
        try {
            clone = new ParentLastURLClassLoader(urls, current).loadClass(self.getName());
        } catch (ClassNotFoundException e) {
            throw new Error("TESTBUG: can't find class: " + e, e);
        }
        try {
            mhIVirtual = lookup.findVirtual(self, "publicMethod", voidRet);
            mhIStatic = lookup.findStatic(self, "staticMethod", voidRet);
            mhISpecial = lookup.findSpecial(self, "privateMethod", voidRet, self);
            mhIStaticInit = lookup.findStatic(clone, "staticMethod", voidRet);
            mhINewSpecial = lookup.findConstructor(self, voidRet);
            mhIInterface = lookup.findVirtual(TestInterface.class, "interfaceMethod", voidRet);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new Error("TESTBUG: lookup failed: " + e, e);
        }
        JavaConstant jcIVirtual = CONSTANT_REFLECTION.forObject(mhIVirtual);
        JavaConstant jcIStatic = CONSTANT_REFLECTION.forObject(mhIStatic);
        JavaConstant jcISpecial = CONSTANT_REFLECTION.forObject(mhISpecial);
        JavaConstant jcIStaticInit = CONSTANT_REFLECTION.forObject(mhIStaticInit);
        JavaConstant jcINewSpecial = CONSTANT_REFLECTION.forObject(mhINewSpecial);
        JavaConstant jcIInterface = CONSTANT_REFLECTION.forObject(mhIInterface);
        // 2nd parameter is force bytecode generation = true/false
        result = new Object[][]{
                        new Object[]{jcIVirtual, true, IVIRTUAL_RESOLVED_NAME},
                        new Object[]{jcIVirtual, false, IVIRTUAL_RESOLVED_NAME},
                        new Object[]{jcIStatic, true, ISTATIC_RESOLVED_NAME},
                        new Object[]{jcIStatic, false, ISTATIC_RESOLVED_NAME},
                        new Object[]{jcISpecial, true, ISPECIAL_RESOLVED_NAME},
                        new Object[]{jcISpecial, false, ISPECIAL_RESOLVED_NAME},
                        new Object[]{jcIStaticInit, true, ISTATICINIT_RESOLVED_NAME},
                        new Object[]{jcIStaticInit, false, ISTATICINIT_RESOLVED_NAME},
                        new Object[]{jcINewSpecial, false, INEWSPECIAL_RESOLVED_NAME},
                        new Object[]{jcINewSpecial, true, INEWSPECIAL_RESOLVED_NAME},
                        new Object[]{jcIInterface, false, IINTERFACE_RESOLVED_NAME},
                        new Object[]{jcIInterface, true, IINTERFACE_RESOLVED_NAME}};
        return result;
    }

    // can't use nested classes for storing these test methods. see JDK-8010319
    @SuppressWarnings("unused")
    private void privateMethod() {
        // empty
    }

    public static void staticMethod() {
        // empty
    }

    public void publicMethod() {
        // empty
    }

    @Override
    public void interfaceMethod() {
        // empty
    }
}

interface TestInterface {
    void interfaceMethod();
}

class ParentLastURLClassLoader extends URLClassLoader {

    ParentLastURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            Class<?> c = findClass(name);
            if (c != null) {
                return c;
            }
        } catch (ClassNotFoundException e) {
            // ignore
        }
        return super.loadClass(name);
    }
}
