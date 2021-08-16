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
 * generate InvokeVirtualSuccessTest
 * @requires vm.opt.final.ClassUnloading
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @library /runtime/SelectionResolution/classes
 * @run main/othervm/timeout=400 -XX:+IgnoreUnrecognizedVMOptions -XX:-VerifyDependencies InvokeVirtualSuccessTest
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import selectionresolution.ClassData;
import selectionresolution.MethodData;
import selectionresolution.SelectionResolutionTest;
import selectionresolution.SelectionResolutionTestCase;
import selectionresolution.Template;

public class InvokeVirtualSuccessTest extends SelectionResolutionTest {

    private static final SelectionResolutionTestCase.Builder initBuilder =
        new SelectionResolutionTestCase.Builder();

    static {
        initBuilder.invoke = SelectionResolutionTestCase.InvokeInstruction.INVOKEVIRTUAL;
    }

    private static final Collection<TestGroup> testgroups =
        Arrays.asList(
                /* invokevirtual tests */
                /* Group 16: callsite = methodref = expected, no override */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PRIVATE),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteEqualsMethodref,
                        Template.MethodrefSelectionResolvedIsClassNoOverride),
                /* Group 17: callsite = methodref = expected, override allowed */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PROTECTED,
                                                        MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteEqualsMethodref,
                        Template.MethodrefSelectionResolvedIsClass,
                        Template.SelectionOverrideAbstract),
                /* Group 18: callsite = methodref = resolved, possibly
                 * skip different package in selection.
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PACKAGE),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteEqualsMethodref,
                        Template.MethodrefSelectionPackageSkip,
                        Template.SelectionOverrideAbstract),
                /* Group 19: callsite = methodref, methodref != expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.IgnoredAbstract,
                        Template.CallsiteEqualsMethodref,
                        Template.MethodrefSelectionResolvedIsClass,
                        Template.SelectionOverrideAbstract),
                /* Group 20: callsite = methodref, methodref \!=
                 * expected, possibly skip different package in
                 * selection.
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PACKAGE),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.IgnoredAbstract,
                        Template.CallsiteEqualsMethodref,
                        Template.MethodrefSelectionPackageSkip,
                        Template.SelectionOverrideAbstract),
                /* Group 21: callsite = methodref, methodref != expected,
                 * expected is interface, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedIface,
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.IgnoredAbstract,
                        Template.CallsiteEqualsMethodref,
                        Template.MethodrefSelectionResolvedIsIface,
                        Template.SelectionOverrideAbstract),
                /* Group 22: callsite :> methodref, methodref = expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.MethodrefSelectionResolvedIsClass,
                        Template.SelectionOverrideAbstract),
                /* Group 23: callsite :>, methodref = expected,
                 * possibly skip different package in selection
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PACKAGE),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.MethodrefSelectionPackageSkip,
                        Template.SelectionOverrideAbstract),
                /* Group 24: callsite :> methodref, methodref != expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.MethodrefSelectionResolvedIsClass,
                        Template.SelectionOverrideAbstract),
                /* Group 25: callsite :>, methodref = expected,
                 * possibly skip different package in selection
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PACKAGE),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.MethodrefSelectionPackageSkip,
                        Template.SelectionOverrideAbstract),
                /* Group 26: callsite :> methodref, methodref != expected,
                 * expected is interface, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedIface,
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.MethodrefSelectionResolvedIsIface,
                        Template.SelectionOverrideAbstract),
                /* Group 27: callsite unrelated to methodref, methodref = expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.MethodrefSelectionResolvedIsClass,
                        Template.SelectionOverrideAbstract),
                /* Group 28: callsite unrelated to methodref,
                 * methodref = expected, possibly skip different
                 * package in selection
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PACKAGE),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.MethodrefSelectionPackageSkip,
                        Template.SelectionOverrideAbstract),
                /* Group 29: callsite unrelated to methodref, methodref != expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.IgnoredAbstract,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.MethodrefSelectionResolvedIsClass,
                        Template.SelectionOverrideAbstract),
                /* Group 30: callsite unrelated to methodref,
                 * methodref \!= expected, possibly skip different
                 * package in selection
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PACKAGE),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.IgnoredAbstract,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.MethodrefSelectionPackageSkip,
                        Template.SelectionOverrideAbstract),
                /* Group 31: callsite unrelated to methodref, methodref != expected,
                 * expected is interface, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedIface,
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.IgnoredAbstract,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.MethodrefSelectionResolvedIsIface,
                        Template.SelectionOverrideAbstract),
                /* Group 32: callsite = methodref, methodref != expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.IgnoredAbstract,
                        Template.CallsiteEqualsMethodref,
                        Template.MethodrefSelectionResolvedIsClass,
                        Template.SelectionOverrideAbstract),
                /* Group 33: callsite = methodref, methodref != expected,
                 * expected is interface, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedIface,
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.IgnoredAbstract,
                        Template.CallsiteEqualsMethodref,
                        Template.MethodrefSelectionResolvedIsIface,
                        Template.SelectionOverrideAbstract),
                /* Group 34: callsite :> methodref, methodref = expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.MethodrefSelectionResolvedIsClass,
                        Template.SelectionOverrideAbstract),

                /* Group 35: callsite :> methodref, methodref != expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.MethodrefSelectionResolvedIsClass,
                        Template.SelectionOverrideAbstract),
                /* Group 36: callsite :> methodref, methodref != expected,
                 * expected is interface, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedIface,
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.MethodrefSelectionResolvedIsIface,
                        Template.SelectionOverrideAbstract),
                /* Group 37: callsite unrelated to methodref, methodref = expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.MethodrefSelectionResolvedIsClass,
                        Template.SelectionOverrideAbstract),
                /* Group 38: callsite unrelated to methodref, methodref != expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.IgnoredAbstract,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.MethodrefSelectionResolvedIsClass,
                        Template.SelectionOverrideAbstract),
                /* Group 39: callsite unrelated to methodref, methodref != expected,
                 * expected is interface, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedIface,
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.IgnoredAbstract,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.MethodrefSelectionResolvedIsIface,
                        Template.SelectionOverrideAbstract)
            );

    private InvokeVirtualSuccessTest() {
        super(testgroups);
    }

    public static void main(final String... args) {
        new InvokeVirtualSuccessTest().run();
    }
}
