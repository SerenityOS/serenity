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
 * generate AbstractMethodErrorTest
 * @requires vm.opt.final.ClassUnloading
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @library /runtime/SelectionResolution/classes
 * @run main/othervm/timeout=300 -XX:+IgnoreUnrecognizedVMOptions -XX:-VerifyDependencies AbstractMethodErrorTest
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

public class AbstractMethodErrorTest extends SelectionResolutionTest {

    private static final SelectionResolutionTestCase.Builder initBuilder =
        new SelectionResolutionTestCase.Builder();

    static {
        initBuilder.setResult(Result.AME);
    }

    private static final Collection<TestGroup> testgroups =
        Arrays.asList(
                /* invokevirtual tests */
                /* Group 63: callsite = methodref = expected */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED,
                                                        MethodData.Access.PRIVATE),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefEqualsExpected,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteEqualsMethodref,
                        Template.MethodrefSelectionResolvedIsClass),
                /* Group 64: callsite = methodref, methodref != expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteEqualsMethodref,
                        Template.MethodrefSelectionResolvedIsClass),
                /* Group 65: callsite = methodref = resolved, possibly
                 * skip different package in selection.
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PACKAGE),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteEqualsMethodref,
                        Template.MethodrefSelectionPackageSkipNoOverride),
                /* Group 66: callsite = methodref, methodref != expected,
                 * expected is interface, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.ReabstractExpectedIface,
                        Template.CallsiteEqualsMethodref,
                        Template.MethodrefSelectionResolvedIsIface),
                /* Group 67: callsite :> methodref, methodref = expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefEqualsExpected,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteSubclassMethodref,
                        Template.MethodrefSelectionResolvedIsClass),
                /* Group 68: callsite :> methodref, methodref = expected,
                 * possibly skip different package in selection.
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PACKAGE),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefEqualsExpected,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteSubclassMethodref,
                        Template.MethodrefSelectionPackageSkipNoOverride),
                /* Group 69: callsite :> methodref, methodref != expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteSubclassMethodref,
                        Template.MethodrefSelectionResolvedIsClass),
                /* Group 70: callsite :> methodref, methodref != expected,
                 * possibly skip different package in selection
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PACKAGE),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteSubclassMethodref,
                        Template.MethodrefSelectionPackageSkipNoOverride),
                /* Group 71: callsite :> methodref, methodref != expected,
                 * expected is interface, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.ReabstractExpectedIface,
                        Template.CallsiteSubclassMethodref,
                        Template.MethodrefSelectionResolvedIsIface),
                /* Group 72: callsite unrelated to methodref, methodref = expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefEqualsExpected,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.MethodrefSelectionResolvedIsClass),
                /* Group 73: callsite unrelated to methodref, methodref = expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PACKAGE),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefEqualsExpected,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.MethodrefSelectionPackageSkipNoOverride),
                /* Group 74: callsite unrelated to methodref, methodref != expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.MethodrefSelectionResolvedIsClass),
                /* Group 75: callsite unrelated to methodref, methodref != expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PACKAGE),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.MethodrefSelectionPackageSkipNoOverride),
                /* Group 76: callsite unrelated to methodref, methodref != expected,
                 * expected is interface, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.ReabstractExpectedIface,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.MethodrefSelectionResolvedIsIface),
                /* Group 77: callsite = methodref, methodref != expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteEqualsMethodref,
                        Template.MethodrefSelectionResolvedIsClass),
                /* Group 78: callsite = methodref, methodref != expected,
                 * expected is interface, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.ReabstractExpectedIface,
                        Template.CallsiteEqualsMethodref,
                        Template.MethodrefSelectionResolvedIsIface),
                /* Group 79: callsite :> methodref, methodref = expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefEqualsExpected,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteSubclassMethodref,
                        Template.MethodrefSelectionResolvedIsClass),

                /* Group 80: callsite :> methodref, methodref != expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteSubclassMethodref,
                        Template.MethodrefSelectionResolvedIsClass),
                /* Group 81: callsite :> methodref, methodref != expected,
                 * expected is interface, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.ReabstractExpectedIface,
                        Template.CallsiteSubclassMethodref,
                        Template.MethodrefSelectionResolvedIsIface),
                /* Group 82: callsite unrelated to methodref, methodref = expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefEqualsExpected,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.MethodrefSelectionResolvedIsClass),
                /* Group 83: callsite unrelated to methodref, methodref != expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.MethodrefSelectionResolvedIsClass),
                /* Group 84: callsite unrelated to methodref, methodref != expected,
                 * expected is interface, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.ReabstractExpectedIface,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.MethodrefSelectionResolvedIsIface),

                /* Reabstraction during selection */
                /* Group 85: callsite = methodref = expected */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PROTECTED,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefEqualsExpected,
                        Template.CallsiteEqualsMethodref,
                        Template.ReabstractMethodrefResolvedClass),
                /* Group 86: callsite = methodref, methodref != expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.CallsiteEqualsMethodref,
                        Template.ReabstractMethodrefResolvedClass),
                /* Group 87: callsite = methodref, methodref != expected,
                 * expected is interface, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.CallsiteEqualsMethodref,
                        Template.ReabstractMethodrefResolvedIface),
                /* Group 88: callsite :> methodref, methodref = expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefEqualsExpected,
                        Template.CallsiteSubclassMethodref,
                        Template.ReabstractMethodrefResolvedClass),
                /* Group 89: callsite :> methodref, methodref != expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.CallsiteSubclassMethodref,
                        Template.ReabstractMethodrefResolvedClass),
                /* Group 90: callsite :> methodref, methodref != expected,
                 * expected is interface, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.CallsiteSubclassMethodref,
                        Template.ReabstractMethodrefResolvedIface),
                /* Group 91: callsite unrelated to methodref, methodref = expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefEqualsExpected,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.ReabstractMethodrefResolvedClass),
                /* Group 92: callsite unrelated to methodref, methodref != expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.ReabstractMethodrefResolvedClass),
                /* Group 93: callsite unrelated to methodref, methodref != expected,
                 * expected is interface, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.ReabstractMethodrefResolvedIface),
                /* Group 94: callsite = methodref, methodref != expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.CallsiteEqualsMethodref,
                        Template.ReabstractMethodrefResolvedClass),
                /* Group 95: callsite = methodref, methodref != expected,
                 * expected is interface, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.CallsiteEqualsMethodref,
                        Template.ReabstractMethodrefResolvedIface),
                /* Group 96: callsite :> methodref, methodref = expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefEqualsExpected,
                        Template.CallsiteSubclassMethodref,
                        Template.ReabstractMethodrefResolvedClass),

                /* Group 97: callsite :> methodref, methodref != expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.CallsiteSubclassMethodref,
                        Template.ReabstractMethodrefResolvedClass),
                /* Group 98: callsite :> methodref, methodref != expected,
                 * expected is interface, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.CallsiteSubclassMethodref,
                        Template.ReabstractMethodrefResolvedIface),
                /* Group 99: callsite unrelated to methodref, methodref = expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefEqualsExpected,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.ReabstractMethodrefResolvedClass),
                /* Group 100: callsite unrelated to methodref, methodref != expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.ReabstractMethodrefResolvedClass),
                /* Group 101: callsite unrelated to methodref, methodref != expected,
                 * expected is interface, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.ReabstractMethodrefResolvedIface),

                /* invokeinterface */
                /* Group 102: callsite = methodref = expected */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEINTERFACE),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefEqualsExpected,
                        Template.ReabstractExpectedIface,
                        Template.CallsiteEqualsMethodref,
                        Template.IfaceMethodrefSelection),
                /* Group 103: callsite = methodref, methodref != expected,
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEINTERFACE),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.IfaceMethodrefNotEqualsExpected,
                        Template.ReabstractExpectedIface,
                        Template.CallsiteEqualsMethodref,
                        Template.IfaceMethodrefSelection),
                /* Group 104: callsite :> methodref, methodref = expected,
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEINTERFACE),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.MethodrefEqualsExpected,
                        Template.ReabstractExpectedIface,
                        Template.CallsiteSubclassMethodref,
                        Template.IfaceMethodrefSelection),
                /* Group 105: callsite :> methodref, methodref != expected,
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEINTERFACE),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.IfaceMethodrefNotEqualsExpected,
                        Template.ReabstractExpectedIface,
                        Template.CallsiteSubclassMethodref,
                        Template.IfaceMethodrefSelection),
                /* Group 106: callsite unrelated to methodref, methodref = expected,
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEINTERFACE),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.MethodrefEqualsExpected,
                        Template.ReabstractExpectedIface,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.IfaceMethodrefSelection),
                /* Group 107: callsite unrelated to methodref, methodref != expected,
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEINTERFACE),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.IfaceMethodrefNotEqualsExpected,
                        Template.ReabstractExpectedIface,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.IfaceMethodrefSelection),

                /* Reabstraction during selection */
                /* Group 108: callsite = methodref = expected */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEINTERFACE),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefEqualsExpected,
                        Template.CallsiteEqualsMethodref,
                        Template.ReabstractIfaceMethodrefResolved),
                /* Group 109: callsite = methodref, methodref != expected,
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEINTERFACE),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.IfaceMethodrefNotEqualsExpected,
                        Template.CallsiteEqualsMethodref,
                        Template.ReabstractIfaceMethodrefResolved),
                /* Group 110: callsite :> methodref, methodref = expected,
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEINTERFACE),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.MethodrefEqualsExpected,
                        Template.CallsiteSubclassMethodref,
                        Template.ReabstractIfaceMethodrefResolved),
                /* Group 111: callsite :> methodref, methodref != expected,
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEINTERFACE),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.IfaceMethodrefNotEqualsExpected,
                        Template.CallsiteSubclassMethodref,
                        Template.ReabstractIfaceMethodrefResolved),
                /* Group 112: callsite unrelated to methodref, methodref = expected,
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEINTERFACE),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.MethodrefEqualsExpected,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.ReabstractIfaceMethodrefResolved),
                /* Group 113: callsite unrelated to methodref, methodref != expected,
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKEINTERFACE),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.IfaceMethodrefNotEqualsExpected,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.ReabstractIfaceMethodrefResolved),

                /* invokespecial tests */
                /* Group 114: callsite = methodref = expected */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKESPECIAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED,
                                                        MethodData.Access.PRIVATE),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefEqualsExpected,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteEqualsMethodref,
                        Template.ObjectrefExactSubclassOfCallsite),
                /* Group 115: callsite = methodref, methodref != expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKESPECIAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteEqualsMethodref,
                        Template.ObjectrefExactSubclassOfCallsite),
                /* Group 116: callsite = methodref, methodref != expected,
                 * expected is interface, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKESPECIAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.ReabstractExpectedIface,
                        Template.CallsiteEqualsMethodref,
                        Template.ObjectrefExactSubclassOfCallsite),
                /* Group 117: callsite :> methodref, methodref = expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKESPECIAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefEqualsExpected,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteSubclassMethodref,
                        Template.ObjectrefExactSubclassOfCallsite),
                /* Group 118: callsite :> methodref, methodref != expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKESPECIAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteSubclassMethodref,
                        Template.ObjectrefExactSubclassOfCallsite),
                /* Group 119: callsite :> methodref, methodref != expected,
                 * expected is interface, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKESPECIAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.ReabstractExpectedIface,
                        Template.CallsiteSubclassMethodref,
                        Template.ObjectrefExactSubclassOfCallsite),
                /* Group 120: callsite = methodref, methodref != expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKESPECIAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteEqualsMethodref,
                        Template.ObjectrefExactSubclassOfCallsite),
                /* Group 121: callsite = methodref, methodref != expected,
                 * expected is interface, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKESPECIAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.ReabstractExpectedIface,
                        Template.CallsiteEqualsMethodref,
                        Template.ObjectrefExactSubclassOfCallsite),
                /* Group 122: callsite :> methodref, methodref = expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKESPECIAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefEqualsExpected,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteSubclassMethodref,
                        Template.ObjectrefExactSubclassOfCallsite),

                /* Group 123: callsite :> methodref, methodref != expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKESPECIAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.ReabstractExpectedClass,
                        Template.CallsiteSubclassMethodref,
                        Template.ObjectrefExactSubclassOfCallsite),
                /* Group 124: callsite :> methodref, methodref != expected,
                 * expected is interface, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKESPECIAL),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.ReabstractExpectedIface,
                        Template.CallsiteSubclassMethodref,
                        Template.ObjectrefExactSubclassOfCallsite)
            );

    private AbstractMethodErrorTest() {
        super(testgroups);
    }

    public static void main(final String... args) {
        new AbstractMethodErrorTest().run();
    }
}
