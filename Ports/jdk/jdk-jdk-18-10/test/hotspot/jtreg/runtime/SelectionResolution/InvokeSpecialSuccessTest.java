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
 * generate InvokeSpecialSuccessTest
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @library /runtime/SelectionResolution/classes
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:-VerifyDependencies InvokeSpecialSuccessTest
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import selectionresolution.ClassData;
import selectionresolution.MethodData;
import selectionresolution.SelectionResolutionTest;
import selectionresolution.SelectionResolutionTestCase;
import selectionresolution.Template;

public class InvokeSpecialSuccessTest extends SelectionResolutionTest {

    private static final SelectionResolutionTestCase.Builder initBuilder =
        new SelectionResolutionTestCase.Builder();

    static {
        initBuilder.invoke = SelectionResolutionTestCase.InvokeInstruction.INVOKESPECIAL;
    }

    private static final Collection<TestGroup> testgroups =
        Arrays.asList(
                /* Group 46: callsite = methodref = expected */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED,
                                                        MethodData.Access.PRIVATE),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteEqualsMethodref,
                        Template.ObjectrefAssignableToCallsite),
                /* Group 47: callsite = methodref, methodref != expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.IgnoredAbstract,
                        Template.CallsiteEqualsMethodref,
                        Template.ObjectrefAssignableToCallsite),
                /* Group 48: callsite = methodref, methodref != expected,
                 * expected is interface, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedIface,
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.IgnoredAbstract,
                        Template.CallsiteEqualsMethodref,
                        Template.ObjectrefAssignableToCallsite),
                /* Group 49: callsite :> methodref, methodref = expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.ObjectrefAssignableToCallsite),
                /* Group 50: callsite :> methodref, methodref != expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.ObjectrefAssignableToCallsite),
                /* Group 51: callsite :> methodref, methodref != expected,
                 * expected is interface, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.OverrideAbstractExpectedIface,
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.ObjectrefAssignableToCallsite),
                /* Group 52: callsite = methodref, methodref != expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.IgnoredAbstract,
                        Template.CallsiteEqualsMethodref,
                        Template.ObjectrefAssignableToCallsite),
                /* Group 53: callsite = methodref, methodref != expected,
                 * expected is interface, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedIface,
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.IgnoredAbstract,
                        Template.CallsiteEqualsMethodref,
                        Template.ObjectrefAssignableToCallsite),
                /* Group 54: callsite :> methodref, methodref = expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.ObjectrefAssignableToCallsite),

                /* Group 55: callsite :> methodref, methodref != expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.ObjectrefAssignableToCallsite),
                /* Group 56: callsite :> methodref, methodref != expected,
                 * expected is interface, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedIface,
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.ObjectrefAssignableToCallsite),

                /* Funny cases */
                /* Group 57: callsite = methodref, methodref =
                 * expected expected is interface, expected and
                 * callsite in a different package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteEqualsMethodref,
                        Template.ObjectrefEqualsOrExactSubclassOfCallsite),
                /* Group 58: callsite = methodref, methodref \!=
                 * expected expected is interface, expected and
                 * callsite in a different package */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.IgnoredAbstract,
                        Template.CallsiteEqualsMethodref,
                        Template.ObjectrefEqualsOrExactSubclassOfCallsite),
                /* Group 59: callsite subclass methodref, methodref =
                 * expected expected is interface, expected and
                 * callsite in a different package */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.ObjectrefEqualsOrExactSubclassOfCallsite),
                /* Group 60: callsite subclass methodref, methodref
                 * \!= expected expected is interface, expected and
                 * callsite in a different package */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.ObjectrefEqualsOrExactSubclassOfCallsite),

                /* Methodref is an interface */
                /* Group 61: callsite :> methodref, methodref = expected,
                 * expected is interface, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedIface,
                        Template.MethodrefEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.ObjectrefAssignableToCallsite),
                /* Group 62: callsite :> methodref, methodref != expected,
                 * expected is interface, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.INSTANCE),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedIface,
                        Template.IfaceMethodrefNotEqualsExpected,
                        Template.IgnoredAbstract,
                        Template.CallsiteSubclassMethodref,
                        Template.ObjectrefAssignableToCallsite)
            );

    private InvokeSpecialSuccessTest() {
        super(testgroups);
    }

    public static void main(final String... args) {
        new InvokeSpecialSuccessTest().run();
    }
}
