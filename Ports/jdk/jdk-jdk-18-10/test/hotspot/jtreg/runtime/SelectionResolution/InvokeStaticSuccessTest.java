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
 * generate InvokeStaticSuccessTest
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @library /runtime/SelectionResolution/classes
 * @run main InvokeStaticSuccessTest
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import selectionresolution.ClassData;
import selectionresolution.MethodData;
import selectionresolution.SelectionResolutionTest;
import selectionresolution.SelectionResolutionTestCase;
import selectionresolution.Template;

public class InvokeStaticSuccessTest extends SelectionResolutionTest {

    private static final SelectionResolutionTestCase.Builder initBuilder =
        new SelectionResolutionTestCase.Builder();

    static {
        initBuilder.invoke = SelectionResolutionTestCase.InvokeInstruction.INVOKESTATIC;
    }

    private static final Collection<TestGroup> testgroups =
        Arrays.asList(
                /* invokestatic tests */
                /* Group 1: callsite = methodref = expected */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED,
                                                        MethodData.Access.PRIVATE),
                                             EnumSet.of(MethodData.Context.STATIC),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefEqualsExpected,
                        Template.CallsiteEqualsMethodref,
                        Template.TrivialObjectref),
                /* Group 2: callsite = methodref, methodref != expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.STATIC),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.CallsiteEqualsMethodref,
                        Template.TrivialObjectref),
                /* Group 3: callsite :> methodref, methodref = expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.STATIC),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefEqualsExpected,
                        Template.CallsiteSubclassMethodref,
                        Template.TrivialObjectref),
                /* Group 4: callsite :> methodref, methodref = expected,
                 * expected is interface, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.STATIC),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefEqualsExpected,
                        Template.CallsiteSubclassMethodref,
                        Template.TrivialObjectref),
                /* Group 5: callsite :> methodref, methodref != expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.STATIC),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.CallsiteSubclassMethodref,
                        Template.TrivialObjectref),
                /* Group 6: callsite unrelated to methodref, methodref = expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.STATIC),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefEqualsExpected,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.TrivialObjectref),
                /* Group 7: callsite unrelated to methodref, methodref = expected,
                 * expected is interface, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.STATIC),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefEqualsExpected,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.TrivialObjectref),
                /* Group 8: callsite unrelated to methodref, methodref != expected,
                 * expected is class, expected and callsite in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.STATIC),
                                             EnumSet.of(ClassData.Package.SAME)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.TrivialObjectref),
                /* Group 9: callsite = methodref, methodref != expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.STATIC),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.CallsiteEqualsMethodref,
                        Template.TrivialObjectref),
                /* Group 10: callsite :> methodref, methodref = expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.STATIC),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefEqualsExpected,
                        Template.CallsiteSubclassMethodref,
                        Template.TrivialObjectref),
                /* Group 11: callsite :> methodref, methodref = expected,
                 * expected is interface, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.STATIC),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefEqualsExpected,
                        Template.CallsiteSubclassMethodref,
                        Template.TrivialObjectref),
                /* Group 12: callsite :> methodref, methodref != expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PROTECTED),
                                             EnumSet.of(MethodData.Context.STATIC),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.CallsiteSubclassMethodref,
                        Template.TrivialObjectref),
                /* Group 13: callsite unrelated to methodref, methodref = expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.STATIC),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefEqualsExpected,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.TrivialObjectref),
                /* Group 14: callsite unrelated to methodref, methodref = expected,
                 * expected is interface, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.STATIC),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefEqualsExpected,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.TrivialObjectref),
                /* Group 15: callsite unrelated to methodref, methodref != expected,
                 * expected is class, expected and callsite not in the same package
                 */
                new TestGroup.Simple(initBuilder,
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC),
                                             EnumSet.of(MethodData.Context.STATIC),
                                             EnumSet.of(ClassData.Package.DIFFERENT)),
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.CallsiteUnrelatedToMethodref,
                        Template.TrivialObjectref)
            );

    private InvokeStaticSuccessTest() {
        super(testgroups);
    }

    public static void main(final String... args) {
        new InvokeStaticSuccessTest().run();
    }
}
