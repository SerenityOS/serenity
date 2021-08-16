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
 * @summary Test of invokestatic method selection and resolution cases that
 * generate IncompatibleClassChangeError
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @library /runtime/SelectionResolution/classes
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:-VerifyDependencies InvokeStaticICCE
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

public class InvokeStaticICCE extends SelectionResolutionTest {

    private static final SelectionResolutionTestCase.Builder initBuilder =
        new SelectionResolutionTestCase.Builder();

    static {
        initBuilder.setResult(Result.ICCE);
    }

    private static final Collection<TestGroup> testgroups =
        Arrays.asList(
                /* invokestatic tests */
                /* Group 157: methodref = expected */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKESTATIC),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED,
                                                        MethodData.Access.PRIVATE),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefEqualsExpected,
                        Template.AllCallsiteCases,
                        Template.TrivialObjectref),
                /* Group 158: methodref = expected, expected is interface */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKESTATIC),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PRIVATE),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedIface,
                        Template.MethodrefEqualsExpected,
                        Template.AllCallsiteCases,
                        Template.TrivialObjectref),
                /* Group 159: methodref != expected, expected is class
                 */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKESTATIC),
                        Template.ResultCombo(EnumSet.of(Template.Kind.CLASS),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PACKAGE,
                                                        MethodData.Access.PROTECTED,
                                                        MethodData.Access.PRIVATE),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedClass,
                        Template.MethodrefNotEqualsExpectedClass,
                        Template.AllCallsiteCases,
                        Template.TrivialObjectref),
                /* Group 160: methodref = expected, expected is interface */
                new TestGroup.Simple(initBuilder,
                        Template.SetInvoke(SelectionResolutionTestCase.InvokeInstruction.INVOKESTATIC),
                        Template.ResultCombo(EnumSet.of(Template.Kind.INTERFACE),
                                             EnumSet.of(MethodData.Access.PUBLIC,
                                                        MethodData.Access.PRIVATE),
                                             EnumSet.of(MethodData.Context.INSTANCE,
                                                        MethodData.Context.ABSTRACT),
                                             EnumSet.of(ClassData.Package.SAME,
                                                        ClassData.Package.DIFFERENT)),
                        Template.OverrideAbstractExpectedIface,
                        Template.MethodrefNotEqualsExpectedIface,
                        Template.AllCallsiteCases,
                        Template.TrivialObjectref)
            );

    private InvokeStaticICCE() {
        super(testgroups);
    }

    public static void main(final String... args) {
        new InvokeStaticICCE().run();
    }
}
