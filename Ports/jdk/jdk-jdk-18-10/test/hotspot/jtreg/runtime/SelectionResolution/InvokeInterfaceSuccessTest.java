/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * generate InvokeInterfaceSuccessTest
 * @requires vm.opt.final.ClassUnloading
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @library /runtime/SelectionResolution/classes
 * @run main/othervm/timeout=300 -XX:+IgnoreUnrecognizedVMOptions -XX:-VerifyDependencies InvokeInterfaceSuccessTest
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import selectionresolution.ClassData;
import selectionresolution.MethodData;
import selectionresolution.SelectionResolutionTest;
import selectionresolution.SelectionResolutionTestCase;
import selectionresolution.Template;

public class InvokeInterfaceSuccessTest extends SelectionResolutionTest {

    private static final SelectionResolutionTestCase.Builder initBuilder =
        new SelectionResolutionTestCase.Builder();

    static {
        initBuilder.invoke = SelectionResolutionTestCase.InvokeInstruction.INVOKEINTERFACE;
    }

    private static final Collection<TestGroup> testgroups =
        Arrays.asList(
                /* invokeinterface tests */

                /* Group 40: callsite = methodref = expected */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedIface,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteEqualsMethodref,
                        Template.IfaceMethodrefSelection,
                        Template.SelectionOverrideAbstract),
                /* Group 41: callsite = methodref, methodref != expected */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedIface,
                        Template.IfaceMethodrefNotEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteEqualsMethodref,
                        Template.IfaceMethodrefSelection,
                        Template.SelectionOverrideAbstract),
                /* Group 42: callsite :> methodref, methodref = expected */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedIface,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.IfaceMethodrefSelection,
                        Template.SelectionOverrideAbstract),
                /* Group 43: callsite :> methodref, methodref != expected */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedIface,
                        Template.IfaceMethodrefNotEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.IfaceMethodrefSelection,
                        Template.SelectionOverrideAbstract),
                /* Group 44: callsite unrelated to methodref, methodref = expected */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedIface,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.IfaceMethodrefSelection,
                        Template.SelectionOverrideAbstract),
                /* Group 45: callsite unrelated to methodref, methodref != expected */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedIface,
                        Template.IfaceMethodrefNotEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.IfaceMethodrefSelection,
                        Template.SelectionOverrideAbstract)
                /*                ,
                // Group 175: private method in interface [was ICCE case up to JDK 11]
                // Can't get this to work for some reason.
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PRIVATE),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedIface,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteEqualsMethodref,
                        Template.IfaceMethodrefSelection)
                */
            );

    private InvokeInterfaceSuccessTest() {
        super(testgroups);
    }

    public static void main(final String... args) {
        new InvokeInterfaceSuccessTest().run();
    }
}
