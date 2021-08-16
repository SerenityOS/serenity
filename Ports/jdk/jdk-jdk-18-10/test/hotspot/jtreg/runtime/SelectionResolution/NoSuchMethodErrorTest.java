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
 * @summary Test of method selection and resolution cases that
 * generate NoSuchMethodError
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @library /runtime/SelectionResolution/classes
 * @run main NoSuchMethodErrorTest
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import selectionresolution.ClassData;
import selectionresolution.MethodData;
import selectionresolution.Result;
import selectionresolution.SelectionResolutionTest;
import selectionresolution.SelectionResolutionTestCase;
import selectionresolution.Template;

public class NoSuchMethodErrorTest extends SelectionResolutionTest {

    private static final SelectionResolutionTestCase.Builder initBuilder =
        new SelectionResolutionTestCase.Builder();

    static {
        initBuilder.setResult(Result.NSME);
    }

    private static final MethodData concreteMethod =
        new MethodData(MethodData.Access.PUBLIC, MethodData.Context.INSTANCE);

    private static final MethodData staticMethod =
        new MethodData(MethodData.Access.PUBLIC, MethodData.Context.STATIC);

    private static final MethodData privateMethod =
        new MethodData(MethodData.Access.PRIVATE, MethodData.Context.INSTANCE);

    private static final ClassData withDef =
        new ClassData(ClassData.Package.SAME, concreteMethod);

    private static final ClassData withStaticDef =
        new ClassData(ClassData.Package.SAME, staticMethod);

    private static final ClassData withPrivateDef =
        new ClassData(ClassData.Package.SAME, staticMethod);

    private static final Template NoMethodResolutionTemplateClassBottom =
        new Template("NoMethodResolutionTemplate",
                    /* Empty single class
                     *
                     * C[]() = mref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int C = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        builder.methodref = C;
                    },
                    /* Class bottom, inherit empty class
                     *
                     * C2[]()
                     * C1[C2]() = mref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int C1 = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        final int C2 = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        builder.hier.addInherit(C1, C2);
                        builder.methodref = C1;
                    },
                    /* Class bottom, inherit empty interface
                     *
                     * I[]()
                     * C[I]() = mref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int C = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        final int I = builder.addInterface(Template.emptyClass(ClassData.Package.SAME));
                        builder.hier.addInherit(C, I);
                        builder.methodref = C;
                    },
                    /* Class bottom, inherit empty class and interface
                     *
                     * C2[](), I[]()
                     * C1[C2,I]() = mref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int C1 = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        final int C2 = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        final int I = builder.addInterface(Template.emptyClass(ClassData.Package.SAME));
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I);
                        builder.methodref = C1;
                    },
                    /* Class bottom, unrelated class defines
                     *
                     * C20[](con)
                     * C1[]()
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int C = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        builder.addClass(withDef);
                        builder.methodref = C;
                    },
                    /* Class bottom, interface defines static
                     *
                     * I[](stat)
                     * C[]()
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int C = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        final int I = builder.addInterface(withStaticDef);
                        builder.hier.addInherit(C, I);
                        builder.methodref = C;
                    },
                    /* Class bottom, interface defines private
                     *
                     * I[](priv)
                     * C[]()
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int C = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        final int I = builder.addInterface(withPrivateDef);
                        builder.hier.addInherit(C, I);
                        builder.methodref = C;
                    });

    private static final Template NoMethodResolutionTemplateIfaceBottom =
        new Template("NoMethodResolutionTemplate",
                    /* Empty single interface
                     *
                     * I[]() = mref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int I = builder.addInterface(Template.emptyClass(ClassData.Package.SAME));
                        builder.methodref = I;
                    },
                    /* Interface bottom, inherit empty interface
                     *
                     * I2[]()
                     * I1[I2]() = mref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int I1 = builder.addInterface(Template.emptyClass(ClassData.Package.SAME));
                        final int I2 = builder.addInterface(Template.emptyClass(ClassData.Package.SAME));
                        builder.hier.addInherit(I1, I2);
                        builder.methodref = I1;
                    },
                    /* Interface bottom, unrelated class defines
                     *
                     * C0[](con)
                     * I[]() = mref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int I = builder.addInterface(Template.emptyClass(ClassData.Package.SAME));
                        builder.addClass(withDef);
                        builder.methodref = I;
                    },
                    /* Interface bottom, interface defines static
                     *
                     * I2[](stat)
                     * I1[I2]() = mref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int I1 = builder.addInterface(Template.emptyClass(ClassData.Package.SAME));
                        final int I2 = builder.addInterface(withStaticDef);
                        builder.hier.addInherit(I1, I2);
                        builder.methodref = I1;
                    },
                    /* Interface bottom, interface defines private
                     *
                     * I2[](stat)
                     * I1[I2]() = mref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int I1 = builder.addInterface(Template.emptyClass(ClassData.Package.SAME));
                        final int I2 = builder.addInterface(withPrivateDef);
                        builder.hier.addInherit(I1, I2);
                        builder.methodref = I1;
                    });

    private static final Template NoMethodSelectionTemplateClassMethodref =
        new Template("NoMethodSelectionTemplate",
                    /* objectref = methodref
                     *
                     * C[]() = mref = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        builder.objectref = builder.methodref;
                    },
                    /* Inherit methodref
                     *
                     * C2[]() = mref
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int C1 = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        final int C2 = builder.methodref;
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Inherit methodref and interface
                     *
                     * C2[]() = mref, I[]()
                     * C1[C2,I]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int C2 = builder.methodref;
                        final int C1 = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        final int I = builder.addInterface(Template.emptyClass(ClassData.Package.SAME));
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I);
                        builder.objectref = C1;
                    },
                    /* objectref = methodref, unrelated class defines
                     *
                     * C0[](def)
                     * C[]() = mref = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        builder.addClass(withDef);
                        builder.objectref = builder.methodref;
                    },
                    /* Inherit methodref, unrelated class defines
                     *
                     * C0[](def)
                     * C2[]() = mref
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int C1 = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        final int C2 = builder.methodref;
                        builder.addClass(withDef);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Inherit methodref and interface, unrelated class defines.
                     *
                     * C0[](def)
                     * C2[]() = mref, I[]()
                     * C1[C2,I]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int C2 = builder.methodref;
                        final int C1 = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        final int I = builder.addInterface(Template.emptyClass(ClassData.Package.SAME));
                        builder.addClass(withDef);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I);
                        builder.objectref = C1;
                    },
                    /* objectref = methodref, unrelated interface defines
                     *
                     * I0[](def)
                     * C[]() = mref = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        builder.addInterface(withDef);
                        builder.objectref = builder.methodref;
                    },
                    /* Inherit methodref, interface defines static
                     *
                     * C2[]() = mref, I0[](stat)
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int C1 = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        final int C2 = builder.methodref;
                        final int I0 = builder.addInterface(withStaticDef);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I0);
                        builder.objectref = C1;
                    },
                    /* Inherit methodref, interface defines private
                     *
                     * C2[]() = mref, I0[](stat)
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int C1 = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        final int C2 = builder.methodref;
                        final int I0 = builder.addInterface(withPrivateDef);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I0);
                        builder.objectref = C1;
                    });

    private static final Template NoMethodSelectionTemplateIfaceMethodref =
        new Template("NoMethodSelectionTemplate",
                    /* Inherit methodref
                     *
                     * I[]() = mref
                     * C[I]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int C = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        final int I = builder.methodref;
                        builder.hier.addInherit(C, I);
                        builder.objectref = C;
                    },
                    /* Inherit methodref and interface
                     *
                     * I1[]() = mref, I2[]()
                     * C[T,I]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int I1 = builder.methodref;
                        final int C = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        final int I2 = builder.addInterface(Template.emptyClass(ClassData.Package.SAME));
                        builder.hier.addInherit(C, I1);
                        builder.hier.addInherit(C, I2);
                        builder.objectref = C;
                    },
                    /* Inherit methodref, unrelated class defines
                     *
                     * C0[](def)
                     * I[]() = mref
                     * C[I]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int C = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        final int I = builder.methodref;
                        builder.addClass(withDef);
                        builder.hier.addInherit(C, I);
                        builder.objectref = C;
                    },
                    /* Inherit methodref and interface, unrelated class defines
                     *
                     * C0[](def)
                     * I1[]() = mref, I2[]()
                     * C[I1,I2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int I1 = builder.methodref;
                        final int C = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        final int I2 = builder.addInterface(Template.emptyClass(ClassData.Package.SAME));
                        builder.addClass(withDef);
                        builder.hier.addInherit(C, I1);
                        builder.hier.addInherit(C, I2);
                        builder.objectref = C;
                    },
                    /* Inherit methodref, interface defines static
                     *
                     * I[]() = mref, I0[](stat)
                     * C[I,I0]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int C = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        final int I = builder.methodref;
                        final int I0 = builder.addInterface(withStaticDef);
                        builder.hier.addInherit(C, I);
                        builder.hier.addInherit(C, I0);
                        builder.objectref = C;
                    },
                    /* Inherit methodref, unrelated class defines private
                     *
                     * I[]() = mref, I0[](priv)
                     * C[I,I0]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final int C = builder.addClass(Template.emptyClass(ClassData.Package.SAME));
                        final int I = builder.methodref;
                        final int I0 = builder.addInterface(withPrivateDef);
                        builder.hier.addInherit(C, I);
                        builder.hier.addInherit(C, I0);
                        builder.objectref = C;
                    });

    private static final Collection<TestGroup> testgroups =
        Arrays.asList(
                /* invokestatic tests */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKESTATIC),
                        NoMethodResolutionTemplateClassBottom,
                        Template.AllCallsiteCases,
                        Template.TrivialObjectref),
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKESTATIC),
                        NoMethodResolutionTemplateIfaceBottom,
                        Template.CallsiteNotEqualsMethodref,
                        Template.TrivialObjectref),
                /* invokevirtual tests */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        NoMethodResolutionTemplateClassBottom,
                        Template.AllCallsiteCases,
                        NoMethodSelectionTemplateClassMethodref),
                /* invokeinterface tests */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEINTERFACE),
                        NoMethodResolutionTemplateIfaceBottom,
                        Template.CallsiteNotEqualsMethodref,
                        NoMethodSelectionTemplateIfaceMethodref),

                /* Hiding of private interface methods */
                /* invokevirtual */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PRIVATE),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.AllCallsiteCases,
                        Template.TrivialObjectref),
                /* invokeinterface */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEINTERFACE),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PRIVATE),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.IfaceMethodrefNotEqualsExpected,
                        Template.AllCallsiteCases,
                        Template.TrivialObjectrefNotEqualMethodref)
            );

    private NoSuchMethodErrorTest() {
        super(testgroups);
    }

    public static void main(final String... args) {
        new NoSuchMethodErrorTest().run();
    }
}
