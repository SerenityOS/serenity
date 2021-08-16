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

/*
 * @test
 * @key randomness
 * @modules java.base/jdk.internal.misc:+open
 *
 * @summary converted from VM Testbase metaspace/staticReferences.
 * VM Testbase keywords: [nonconcurrent, javac, no_cds]
 *
 * @requires vm.opt.final.ClassUnloading
 * @library /vmTestbase /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm
 *      -Xmx800m
 *      -Xbootclasspath/a:.
 *      -XX:+UnlockDiagnosticVMOptions
 *      -XX:+WhiteBoxAPI
 *      StaticReferences
 */

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.ref.WeakReference;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import vm.share.InMemoryJavaCompiler;
import nsk.share.gc.GCTestBase;
import nsk.share.test.ExecutionController;
import nsk.share.test.Stresser;
import nsk.share.test.TestBase;
import nsk.share.test.Tests;
import vm.share.gc.TriggerUnloadingHelper;
import vm.share.gc.TriggerUnloadingWithWhiteBox;

/**
 * Test checks that static fields will be initialized in new loaded class. Test performs in loop the following routine:
 * 1.) Load class either by regular classloader or by defineHiddenClass.
 * 2.) Trigger unloading. Class must be alive. Next step will check that static fields were not lost.
 * 3.) Change static fields.
 * 4.) Unload class.
 * 5.) Load class again as in step 1.
 * 6.) Check that static fields were initialized.
 */
@SuppressWarnings("rawtypes")
public class StaticReferences extends GCTestBase {

    private static final int UNLOADING_ATTEMPTS_LIMIT = 50;

    private static final Object[] NO_CP_PATCHES = new Object[0];

    private static String[] args;

    private static final int LIMIT = 20;

    private List<Object> keepAlive = new LinkedList<Object>();

    private Random random;

    private TriggerUnloadingHelper triggerUnloadingHelper = new TriggerUnloadingWithWhiteBox();

    private String[] typesArray = new String[] {"Object object", "boolean boolean", "byte byte", "char char", "double double", "float float", "int int", "long long", "short short"};

    public static void main(String[] args) {
        StaticReferences.args = args;
        Tests.runTest(new StaticReferences(), args);
    }

        @Override
    public void run() {
        random = new Random(runParams.getSeed());
        ExecutionController stresser = new Stresser(args);
        stresser.start(1);

        // Generate and compile classes
        List<byte[]> bytecodeList = new LinkedList<byte[]>();
        int[] fieldQuantities = new int[9];
        long startTimeStamp = System.currentTimeMillis();
        for (int i = 0; i < LIMIT; i++) {
            if (!stresser.continueExecution()) {
                        return;
                }
            for (int j = 0; j < fieldQuantities.length; j++) {
                fieldQuantities[j] = 1 + random.nextInt(20);
            }
            bytecodeList.add(generateAndCompile(fieldQuantities));
        }
        log.info("Compilation finished in " + ((System.currentTimeMillis() - startTimeStamp)/1000/60.0) + " minutes ");

        // Core of test
        for (byte[] classBytecode : bytecodeList) {
            boolean hidden = random.nextBoolean();

            log.info("Load class first time");
            Class clazz = loadClass(classBytecode, hidden);

            log.info("Trigger unloading");
            triggerUnloadingHelper.triggerUnloading(stresser);
            if (!stresser.continueExecution()) {
                        return;
                }

            log.info("Set up static fields. This will check that static fields are reachable.");
            setupFields(clazz);

            log.info("Cleanup references");
            Reference<Class> weakReference = new WeakReference<Class>(clazz);
            clazz = null;

            log.info("Trigger unloading again");
            int numberOfAttemps = 0;
            while (weakReference.get() != null && numberOfAttemps < UNLOADING_ATTEMPTS_LIMIT) {
                if (!stresser.continueExecution()) {
                        return;
                }
                triggerUnloadingHelper.triggerUnloading(stresser);
            }
            if (numberOfAttemps >= UNLOADING_ATTEMPTS_LIMIT) {
                setFailed(true);
                throw new RuntimeException("Test failed: was unable to unload class with " + UNLOADING_ATTEMPTS_LIMIT + " attempts.");
            }

            log.info("Load class second time");
            clazz = loadClass(classBytecode, hidden);

            log.info("check fields reinitialized");
            checkStaticFields(clazz);

            keepAlive.add(clazz);
        }
    }

        private Class loadClass(byte[] classBytecode, boolean hidden) {
                Class clazz;
                if (hidden) {
                    Lookup lookup = MethodHandles.lookup();
                    try {
                        clazz = lookup.defineHiddenClass(classBytecode, false).lookupClass();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        throw new RuntimeException(
                            "Lookup.defineHiddenClass failed: " + e.getMessage());
                    }
                } else {
                        OneUsageClassloader classloader = new OneUsageClassloader();
                        clazz = classloader.define(classBytecode);
                }
                return clazz;
        }

    private void checkStaticFields(Class clazz) {
        for (Field field : clazz.getFields()) {
            try {
                if (Modifier.isStatic(field.getModifiers())) {
                    Class fieldType = field.getType();
                    if ((fieldType.equals(Object.class) && field.get(null) != null )
                            || (fieldType.equals(int.class) && field.getInt(null) != 0)
                            || (fieldType.equals(boolean.class) && field.getBoolean(null) != false)
                            || (fieldType.equals(char.class) && field.getChar(null) != 0)
                            || (fieldType.equals(long.class) && field.getLong(null) != 0)
                            || (fieldType.equals(short.class) && field.getShort(null) != 0)
                            || (fieldType.equals(float.class) && field.getFloat(null) != 0.0f)
                            || (fieldType.equals(double.class) && field.getDouble(null) != 0.0)
                            || (fieldType.equals(byte.class) && field.getByte(null) != 0)) {
                        setFailed(true);
                        throw new RuntimeException("Failing test: field "
                                + field.getName() + " of type "
                                + field.getType() + " in class "
                                + field.getDeclaringClass().getName()
                                + " was not cleared");
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
                throw new RuntimeException("Was unable to set static field "
                        + field.getName() + " of type "
                        + field.getType().getName() + " in class "
                        + field.getDeclaringClass().getName(), e);
            }
        }
    }

    private byte[] generateAndCompile(int[] fieldQuantities) {
        Map<String, CharSequence> sources = new HashMap<String, CharSequence>();
        sources.put("A", generateSource(fieldQuantities));
        return InMemoryJavaCompiler.compile(sources).values().iterator().next();
    }

    private StringBuffer generateSource(int[] fieldQuantities) {
        StringBuffer result = new StringBuffer("public class A { \n");
        int fieldsCounter = 0;
        for (int i = 0; i < typesArray.length; i++) {
            for (int j = 0; j < fieldQuantities[i]; j++) {
                result.append(" public static " + typesArray[i] + fieldsCounter++ + ";\n");
            }
        }
        result.append(" } ");
        return result;
    }

    private void setupFields(Class clazz) {
        for (Field field : clazz.getFields()) {
            try {
                if (Modifier.isStatic(field.getModifiers())) {
                    Class fieldType = field.getType();
                    if (fieldType.equals(Object.class)) {
                        field.set(null, this);
                    } else if (fieldType.equals(int.class)) {
                        field.setInt(null, 42);
                    } else if (fieldType.equals(boolean.class)) {
                        field.setBoolean(null, true);
                    } else if (fieldType.equals(char.class)) {
                        field.setChar(null, 'c');
                    } else if (fieldType.equals(long.class)) {
                        field.setLong(null, (long) 42);
                    } else if (fieldType.equals(short.class)) {
                        field.setShort(null, (short) 42);
                    } else if (fieldType.equals(float.class)) {
                        field.setFloat(null, 42.42f);
                    } else if (fieldType.equals(double.class)) {
                        field.setDouble(null, 42.42);
                    } else if (fieldType.equals(byte.class)) {
                        field.setByte(null, (byte) 42);
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
                throw new RuntimeException(
                        "Was unable to set static field " + field.getName()
                                + " of type " + field.getType().getName()
                                + " in class "
                                + field.getDeclaringClass().getName(), e);
            }
        }
    }

}
