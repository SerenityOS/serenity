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

package selectionresolution;

import java.util.ArrayList;
import java.util.Iterator;

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_STATIC;

/**
 * Constructs classes and interfaces based on the information from a
 * DefaultMethodTestCase
 *
 */
public class ClassBuilder extends Builder {
    private final ArrayList<ClassConstruct> classes;

    // Add a class in every package to be able to instantiate package
    // private classes from outside the package
    private final Clazz[] helpers = new Clazz[4];
    private ClassConstruct callsiteClass;

    public enum ExecutionMode { DIRECT, INDY, MH_INVOKE_EXACT, MH_INVOKE_GENERIC}
    private final ExecutionMode execMode;

    public ClassBuilder(SelectionResolutionTestCase testcase,
                        ExecutionMode execMode) {
        super(testcase);
        this.classes = new ArrayList<>();
        this.execMode = execMode;
    }

    public ClassConstruct[] build() throws Exception {
        buildClassConstructs();
        return classes.toArray(new ClassConstruct[0]);
    }

    public ClassConstruct getCallsiteClass() {
        return callsiteClass;
    }

    private void buildClassConstructs() throws Exception {
        TestBuilder tb = new TestBuilder(testcase.methodref, testcase);

        classes.add(new Clazz("Test", ACC_PUBLIC, -1));

        for (int classId = 0; classId < classdata.size(); classId++) {
            ClassConstruct C;
            String[] interfaces = getInterfaces(classId);
            ClassData data = classdata.get(classId);

            if (isClass(classId)) {
                C = new Clazz(getName(classId),
                              getExtending(classId),
                              getClassModifiers(data),
                              classId,
                              interfaces);

                addHelperMethod(classId);

            } else {
                C = new Interface(getName(classId),
                                  getAccessibility(data.access),
                                  classId, interfaces);
            }

            // Add a method "m()LTestObject;" if applicable
            if (containsMethod(data)) {
                // Method will either be abstract or concrete depending on the
                // abstract modifier
                C.addTestMethod(getMethodModifiers(data));
            }

            if (classId == testcase.callsite) {
                // Add test() method
                tb.addTest(C, execMode);
                callsiteClass = C;
            }

            classes.add(C);
        }
        classes.add(tb.getMainTestClass());

    }

    private void addHelperMethod(int classId) {
        int packageId = classdata.get(classId).packageId.ordinal();
        Clazz C = helpers[packageId];
        if (C == null) {
            C = new Clazz(getPackageName(packageId) + "Helper", -1, ACC_PUBLIC);
            helpers[packageId] = C;
            classes.add(C);
        }

        Method m = C.addMethod("get" + getClassName(classId),
                               "()L" + getName(classId) + ";",
                               ACC_PUBLIC + ACC_STATIC);
        m.makeInstantiateMethod(getName(classId));
    }

    private String[] getInterfaces(int classId) {
        ArrayList<String> interfaces = new ArrayList<>();

        // Figure out if we're extending/implementing an interface
        for (final int intf : hier.interfaces()) {
            if (hier.inherits(classId, intf)) {
                interfaces.add(getName(intf));
            }
        }
        return interfaces.toArray(new String[0]);
    }

    private String getExtending(int classId) {
        int extending = -1;

        // See if we're extending another class
        for (final int extendsClass : hier.classes()) {
            if (hier.inherits(classId, extendsClass)) {
                // Sanity check that we haven't already found an extending class
                if (extending != -1) {
                    throw new RuntimeException("Multiple extending classes");
                }
                extending = extendsClass;
            }
        }

        return extending == -1 ? null : getName(extending);
    }

    /**
     * Returns modifiers for a Class
     * @param cd ClassData for the Class
     * @return ASM modifiers for a Class
     */
    private int getClassModifiers(ClassData cd) {
        // For Classes we only care about accessibility (public, private etc)
        return getAccessibility(cd.access) | getAbstraction(cd.abstraction);
    }

    /**
     * Returns modifiers for Method type
     * @param cd ClassData for the Class or Interface where the Method resides
     * @return ASM modifiers for the Method
     */
    private int getMethodModifiers(ClassData cd) {
        int mod = 0;

        // For methods we want everything
        mod += getAccessibility(cd.methoddata.access);
        mod += getAbstraction(cd.methoddata.context);
        mod += getContext(cd.methoddata.context);
        mod += getExtensibility();
        return mod;
    }


    /**
     * Convert ClassData access type to ASM
     * @param access
     * @return ASM version of accessibility (public / private / protected)
     */
    private int getAccessibility(MethodData.Access access) {
        switch(access) {
        case PACKAGE:
            //TODO: Do I need to set this or will this be the default?
            return 0;
        case PRIVATE:
            return ACC_PRIVATE;
        case PROTECTED:
            return ACC_PROTECTED;
        case PUBLIC:
            return ACC_PUBLIC;
        default:
            throw new RuntimeException("Illegal accessibility modifier: " + access);
        }
    }

    /**
     * Convert ClassData abstraction type to ASM
     * @param abstraction
     * @return ASM version of abstraction (abstract / non-abstract)
     */
    private int getAbstraction(MethodData.Context context) {
        return context == MethodData.Context.ABSTRACT ? ACC_ABSTRACT : 0;
    }

    /**
     * Convert ClassData context type to ASM
     * @param context
     * @return ASM version of context (static / non-static)
     */
    private int getContext(MethodData.Context context) {
        return context == MethodData.Context.STATIC ? ACC_STATIC : 0;
    }

    /**
     * Convert ClassData extensibility type to ASM
     * @param extensibility
     * @return ASM version of extensibility (final / non-final)
     */
    private int getExtensibility() {
        return 0;
    }

    /**
     * Determine if we need a method at all, abstraction is set to null if this
     * Class/Interface should not have a test method
     * @param cd
     * @return
     */
    private boolean containsMethod(ClassData cd) {
        return cd.methoddata != null;
    }

}
