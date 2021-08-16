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

package selectionresolution;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Templates are sets of transformations that are applied to a
 * SelectionResolutionTestCase.Builder as part of building up a test
 * case.  Templates should contain a collection of different
 * transforms, all of which represent an "interesting" case in a
 * general category of cases.
 *
 */
public class Template {

    public enum Kind { CLASS, INTERFACE; }

    public final Collection<Consumer<SelectionResolutionTestCase.Builder>> cases;
    public final String name;

    /**
     * Create a template from a collection of lambdas that modify a Builder.
     *
     * @param name The name of the template.
     * @param cases The cases in the template.
     */
    public Template(final String name,
                    final Collection<Consumer<SelectionResolutionTestCase.Builder>> cases) {
        this.cases = cases;
        this.name = name;
    }

    /**
     * Build a template out of a set of lambdas that modify a Builder.
     *
     * @param name The name of the template.
     * @param cases The cases in the template.
     */
    public Template(final String name,
                    final Consumer<SelectionResolutionTestCase.Builder>... cases) {
        this(name, Arrays.asList(cases));
    }

    /**
     * Build a template out of a set of lambdas that modify a Builder.
     * Also include all cases from another template.
     *
     * @param name The name of the template.
     * @param include Include all cases from this template.
     * @param cases The cases in the template.
     */
    public Template(final String name,
                    final Template include,
                    final Consumer<SelectionResolutionTestCase.Builder>... cases) {
        this(name, new LinkedList(include.cases));
        this.cases.addAll(Arrays.asList(cases));
    }

    /**
     * Build a template out of a set of lambdas that modify a Builder.
     * Also include all cases from another template.
     *
     * @param name The name of the template.
     * @param include Include all cases from this template.
     * @param cases The cases in the template.
     */
    public Template(final String name,
                    final Template... others) {
        this(name, new LinkedList());

        for(final Template template : others) {
            cases.addAll(template.cases);
        }
    }

    /**
     * Run all cases in the template.  This will run each action in
     * the template and then call the next action on a separate copy
     * of the builder parameter.
     *
     * @param The next action to perform of the Builder.
     * @param The Builder to modify.
     */
    public void runCases(final Consumer<SelectionResolutionTestCase.Builder> next,
                         final SelectionResolutionTestCase.Builder builder) {
        for(final Consumer<SelectionResolutionTestCase.Builder> thiscase : cases) {
            final SelectionResolutionTestCase.Builder localbuilder = builder.copy();
            thiscase.accept(localbuilder);
            next.accept(localbuilder);
        }
    }

    public void printCases(final SelectionResolutionTestCase.Builder builder) {
        int i = 1;
        System.err.println("Template " + name + ":\n");
        for(final Consumer<SelectionResolutionTestCase.Builder> thiscase : cases) {
            final SelectionResolutionTestCase.Builder localbuilder = builder.copy();
            thiscase.accept(localbuilder);
            System.err.println("Case " + i++);
            System.err.println(localbuilder);
        }
    }

    /* Create an empty class in the given package */
    public static final ClassData emptyClass(final ClassData.Package pck) {
        return new ClassData(pck, null);
    }

    /* These are functions that are used to build callsite templates */
    public static void callsiteIsMethodref(final SelectionResolutionTestCase.Builder builder) {
        builder.callsite = builder.methodref;
    }

    public static void callsiteSubclassMethodref(final SelectionResolutionTestCase.Builder builder) {
        final int callsite =
            builder.addClass(Template.emptyClass(ClassData.Package.SAME));
        builder.hier.addInherit(callsite, builder.methodref);
        builder.callsite = callsite;
    }

    public static void callsiteUnrelatedMethodref(final SelectionResolutionTestCase.Builder builder) {
        final int callsite =
            builder.addClass(Template.emptyClass(ClassData.Package.SAME));
        builder.callsite = callsite;
    }

    public static void methodrefIsExpected(final SelectionResolutionTestCase.Builder builder) {
        builder.methodref = builder.expected;
    }

    public static final Template MethodrefEqualsExpected =
        new Template("MethodrefEqualsExpected",
                     Template::methodrefIsExpected);

    /*****************************
     *    Set Invoke Template    *
     *****************************/

    public static final Template SetInvoke(final SelectionResolutionTestCase.InvokeInstruction invoke) {
        return new Template("SetInvoke(" + invoke + ")",
                            Collections.singleton((builder) -> {
                                    builder.invoke = invoke;
                                }));
    }

    /*****************************
     *   Result Combo Template   *
     *****************************/
    public static Template ResultCombo(final EnumSet<Kind> kinds,
                                       final EnumSet<MethodData.Access> accesses,
                                       final EnumSet<MethodData.Context> contexts,
                                       final EnumSet<ClassData.Package> packages) {
        final LinkedList<Consumer<SelectionResolutionTestCase.Builder>> cases =
            new LinkedList<>();

        for (final Kind kind : kinds) {
            for (final MethodData.Access acc : accesses) {
                for (final MethodData.Context ctx : contexts) {
                    if (!(acc == MethodData.Access.PRIVATE &&
                          ctx == MethodData.Context.ABSTRACT)) {
                        for (final ClassData.Package pck : packages) {
                            cases.add((builder) -> {
                                    final MethodData meth = new MethodData(acc, ctx);
                                    final ClassData cls = new ClassData(pck, meth);
                                    switch(kind) {
                                    case CLASS:
                                        builder.expected = builder.addClass(cls);
                                        break;
                                    case INTERFACE:
                                        builder.expected = builder.addInterface(cls);
                                        break;
                                    }
                                });
                        }
                    }
                }
            }
        }

        return new Template("ResultCombo", cases);
    }

    public static Template ResolutionOverride(final EnumSet<Kind> kinds,
                                              final EnumSet<MethodData.Access> accesses,
                                              final EnumSet<MethodData.Context> contexts,
                                              final EnumSet<ClassData.Package> packages) {
        final LinkedList<Consumer<SelectionResolutionTestCase.Builder>> cases =
            new LinkedList<>();

        for (final Kind kind : kinds) {
            for (final MethodData.Access acc : accesses) {
                for (final MethodData.Context ctx : contexts) {
                    if (!(acc == MethodData.Access.PRIVATE &&
                          ctx == MethodData.Context.ABSTRACT)) {
                        for (final ClassData.Package pck : packages) {
                            cases.add((builder) -> {
                                    final MethodData meth = new MethodData(acc, ctx);
                                    final ClassData cls = new ClassData(pck, meth);
                                    int override = -1;
                                    switch(kind) {
                                    case CLASS:
                                        override = builder.addClass(cls);
                                        break;
                                    case INTERFACE:
                                        override = builder.addInterface(cls);
                                        break;
                                    }
                                    builder.hier.addInherit(override, builder.expected);
                                });
                        }
                    }
                }
            }
        }

        return new Template("ResultCombo", cases);
    }

    /******************************
     *    Resolution Templates    *
     ******************************/

    private static MethodData getMethodData(final MethodData.Access acc,
                                            final MethodData.Context ctx) {
        if (!(acc == MethodData.Access.PUBLIC ||
              acc == MethodData.Access.PLACEHOLDER) &&
            ctx != MethodData.Context.STATIC) {
            return null;
        } else {
            return new MethodData(MethodData.Access.PUBLIC, ctx);
        }
    }

    public static final Template MethodrefNotEqualsExpectedClass =
        new Template("MethodrefNotEqualsExpectedClass",
                     /* Case 1: Inherit from super.
                      *
                      * C2[](res)
                      * C1[C2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final int C2 = builder.expected;
                         final int C1 = builder.addClass(emptyClass(ClassData.Package.SAME));
                         builder.hier.addInherit(C1, C2);
                         builder.methodref = C1;
                     },
                     /* Case 2: Inherit from super.
                      *
                      * C2[](res), I[](def)
                      * C1[C2,I]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata = getMethodData(acc, ctx);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final int C2 = builder.expected;
                         final int C1 = builder.addClass(emptyClass(ClassData.Package.SAME));
                         final int I = builder.addInterface(withDef);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(C1, I);
                         builder.methodref = C1;
                     },
                     /* Case 3: Inherit from super's super.
                      *
                      * C3[](res)
                      * C2[](), I[](def)
                      * C1[C2,I]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata = getMethodData(acc, ctx);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final int C3 = builder.expected;
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(ClassData.Package.SAME));
                         final int I = builder.addInterface(withDef);
                         builder.hier.addInherit(C2, C3);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(C1, I);
                         builder.methodref = C1;
                     });

    public static final Template IfaceMethodrefNotEqualsExpected =
        new Template("IfaceMethodrefNotEqualsExpected",
                     /* Case 1: Inherit from super.
                      *
                      * I2[](res)
                      * I1[I2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I2 = builder.expected;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         builder.hier.addInherit(I1, I2);
                         builder.methodref = I1;
                     },
                     /* Case 2: Inherit from super, skip private.
                      *
                      * I2[](res)
                      * I2[I3](priv)
                      * I1[I2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData meth =
                             new MethodData(MethodData.Access.PRIVATE,
                                            MethodData.Context.INSTANCE);
                         final ClassData withPrivDef = new ClassData(pck, meth);
                         final int I3 = builder.expected;
                         final int I2 = builder.addInterface(withPrivDef);
                         final int I1 = builder.addInterface(emptyClass(pck));
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(I2, I3);
                         builder.methodref = I1;
                     },
                     /* Case 3: Inherit from super, skip static.
                      *
                      * I2[](res)
                      * I2[I3](stat)
                      * I1[I2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef = new ClassData(pck, meth);
                         final int I3 = builder.expected;
                         final int I2 = builder.addInterface(withStatDef);
                         final int I1 = builder.addInterface(emptyClass(pck));
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(I2, I3);
                         builder.methodref = I1;
                     },
                     /* Case 4: Maximally-specific.
                      *
                      * I3[](def)
                      * I2[I3](res)
                      * I1[I2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata = getMethodData(acc, ctx);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final int I3 = builder.addInterface(withDef);
                         final int I2 = builder.expected;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(I2, I3);
                         builder.methodref = I1;
                     },
                     /* Case 5: Diamond, expected at top.
                      *
                      * I4[](res)
                      * I2[I4](), I3[I4]()
                      * I1[I2,I3]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I4 = builder.expected;
                         final int I3 = builder.addInterface(emptyClass(pck));
                         final int I2 = builder.addInterface(emptyClass(pck));
                         final int I1 = builder.addInterface(emptyClass(pck));
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I4);
                         builder.hier.addInherit(I3, I4);
                         builder.methodref = I1;
                     },
                     /* Case 6: Diamond, skip private.
                      *
                      * I4[](res)
                      * I2[I4](priv), I3[I4]()
                      * I1[I2,I3]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata = getMethodData(acc, ctx);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PRIVATE,
                                            MethodData.Context.INSTANCE);
                         final ClassData withPrivDef = new ClassData(pck, meth);
                         final int I4 = builder.expected;
                         final int I3 = builder.addInterface(emptyClass(pck));
                         final int I2 = builder.addInterface(withPrivDef);
                         final int I1 = builder.addInterface(emptyClass(pck));
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I4);
                         builder.hier.addInherit(I3, I4);
                         builder.methodref = I1;
                     },
                     /* Case 7: Diamond, skip static.
                      *
                      * I4[](res)
                      * I2[I4](stat), I3[I4]()
                      * I1[I2,I3]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata = getMethodData(acc, ctx);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef = new ClassData(pck, meth);
                         final int I4 = builder.expected;
                         final int I3 = builder.addInterface(emptyClass(pck));
                         final int I2 = builder.addInterface(withStatDef);
                         final int I1 = builder.addInterface(emptyClass(pck));
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I4);
                         builder.hier.addInherit(I3, I4);
                         builder.methodref = I1;
                     },
                     /* Case 8: Diamond, maximally-specific.
                      *
                      * I4[](def)
                      * I2[I4](res), I3[I4]()
                      * I1[I2,I3]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata = getMethodData(acc, ctx);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final int I4 = builder.addInterface(withDef);
                         final int I3 = builder.addInterface(emptyClass(pck));
                         final int I2 = builder.expected;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I4);
                         builder.hier.addInherit(I3, I4);
                         builder.methodref = I1;
                     },
                     /* Case 9: Diamond, maximally-specific, skipping private.
                      *
                      * I4[](def)
                      * I2[I4](res), I3[I4](priv)
                      * I1[I2,I3]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata = getMethodData(acc, ctx);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PRIVATE,
                                            MethodData.Context.INSTANCE);
                         final ClassData withPrivDef = new ClassData(pck, meth);
                         final int I4 = builder.addInterface(withDef);
                         final int I3 = builder.addInterface(withPrivDef);
                         final int I2 = builder.expected;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I4);
                         builder.hier.addInherit(I3, I4);
                         builder.methodref = I1;
                     },
                     /* Case 10: Diamond, maximally-specific, skipping static.
                      *
                      * I4[](def)
                      * I2[I4](res), I3[I4](stat)
                      * I1[I2,I3]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata = getMethodData(acc, ctx);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef = new ClassData(pck, meth);
                         final int I4 = builder.addInterface(withDef);
                         final int I3 = builder.addInterface(withStatDef);
                         final int I2 = builder.expected;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I4);
                         builder.hier.addInherit(I3, I4);
                         builder.methodref = I1;
                     });

    public static final Template MethodrefNotEqualsExpectedIface =
        new Template("MethodrefNotEqualsExpectedIface",
                    /* Case 1: Inherit from superinterface.
                     *
                     * I[](res)
                     * C[I]() = mref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.expected).packageId;
                        final int I = builder.expected;
                        final int C = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C, I);
                        builder.methodref = C;
                     },
                     /* Case 2: Diamond, expected at top.
                      *
                      * I3[](res)
                      * I1[I3](), I2[I3]()
                      * C[I1,I2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I3 = builder.expected;
                         final int I2 = builder.addInterface(emptyClass(pck));
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C, I1);
                         builder.hier.addInherit(C, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.methodref = C;
                     },
                     /* Case 3: Diamond, skipping private.
                      *
                      * I3[](def)
                      * I1[I3](priv), I2[I3]()
                      * C[I1,I2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData meth =
                             new MethodData(MethodData.Access.PRIVATE,
                                            MethodData.Context.INSTANCE);
                         final ClassData withPrivDef = new ClassData(pck, meth);
                         final int I3 = builder.expected;
                         final int I2 = builder.addInterface(emptyClass(pck));
                         final int I1 = builder.addInterface(withPrivDef);
                         final int C = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C, I1);
                         builder.hier.addInherit(C, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.methodref = C;
                     },
                     /* Case 4: Diamond, skipping static.
                      *
                      * I3[](def)
                      * I1[I3](stat), I2[I3]()
                      * C[I1,I2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef = new ClassData(pck, meth);
                         final int I3 = builder.expected;
                         final int I2 = builder.addInterface(emptyClass(pck));
                         final int I1 = builder.addInterface(withStatDef);
                         final int C = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C, I1);
                         builder.hier.addInherit(C, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.methodref = C;
                     },
                     /* Case 5: Diamond, maximally-specific.
                      *
                      * I3[](def)
                      * I1[I3](res), I2[I3]()
                      * C[I1,I2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata = getMethodData(acc, ctx);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final int I3 = builder.addInterface(withDef);
                         final int I2 = builder.addInterface(emptyClass(pck));
                         final int I1 = builder.expected;
                         final int C = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C, I1);
                         builder.hier.addInherit(C, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.methodref = C;
                     },
                     /* Case 6: Diamond, maximally-specific, skipping private.
                      *
                      * I3[](def)
                      * I1[I3](res), I2[I3](priv)
                      * C[I1,I2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata = getMethodData(acc, ctx);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PRIVATE,
                                            MethodData.Context.INSTANCE);
                         final ClassData withPrivDef = new ClassData(pck, meth);
                         final int I3 = builder.addInterface(withDef);
                         final int I2 = builder.addInterface(withPrivDef);
                         final int I1 = builder.expected;
                         final int C = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C, I1);
                         builder.hier.addInherit(C, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.methodref = C;
                     },
                     /* Case 7: Diamond, maximally-specific, skipping static.
                      *
                      * I3[](def)
                      * I1[I3](res), I2[I3](stat)
                      * C[I1,I2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata = getMethodData(acc, ctx);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef = new ClassData(pck, meth);
                         final int I3 = builder.addInterface(withDef);
                         final int I2 = builder.addInterface(withStatDef);
                         final int I1 = builder.expected;
                         final int C = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C, I1);
                         builder.hier.addInherit(C, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.methodref = C;
                     },
                     /* Case 8: Diamond, with superclass, expected at top.
                      *
                      * I2[](res)
                      * C2[I2](), I1[I2]()
                      * C1[I1,C2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I2 = builder.expected;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C2 = builder.addInterface(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C2, I2);
                         builder.methodref = C1;
                     },
                     /* Case 9: Diamond with superclass, maximally-specific.
                      *
                      * I2[](def)
                      * C2[I2](), I1[I2](res),
                      * C1[I1,C2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata = getMethodData(acc, ctx);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final int I2 = builder.addInterface(withDef);
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int I1 = builder.expected;
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C2, I2);
                         builder.methodref = C1;
                     },
                     /* Case 10: Inherit through superclass.
                      *
                      * I[](res)
                      * C2[I]()
                      * C1[C2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I = builder.expected;
                         final int C2 = builder.addInterface(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(C2, I);
                         builder.methodref = C1;
                     },
                     /* Case 11: Diamond, inherit through superclass,
                      * expected at top.
                      *
                      * I3[](res)
                      * I1[I3](), I2[I3]()
                      * C2[I1,I2]()
                      * C1[C2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I3 = builder.expected;
                         final int I2 = builder.addInterface(emptyClass(pck));
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I1);
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.methodref = C1;
                     },
                     /* Case 12: Diamond through superclass, skip private.
                      *
                      * I3[](res)
                      * I1[I3](priv), I2[I3]()
                      * C2[I1,I2]()
                      * C1[C2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata = getMethodData(acc, ctx);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PRIVATE,
                                            MethodData.Context.INSTANCE);
                         final ClassData withPrivDef = new ClassData(pck, meth);
                         final int I3 = builder.expected;
                         final int I2 = builder.addInterface(emptyClass(pck));
                         final int I1 = builder.addInterface(withPrivDef);
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I1);
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.methodref = C1;
                     },
                     /* Case 13: Diamond through superclass, skip static.
                      *
                      * I3[](def)
                      * I1[I3](stat), I2[I3]()
                      * C2[I1,I2]()
                      * C1[C2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata = getMethodData(acc, ctx);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef = new ClassData(pck, meth);
                         final int I3 = builder.expected;
                         final int I2 = builder.addInterface(emptyClass(pck));
                         final int I1 = builder.addInterface(withStatDef);
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I1);
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.methodref = C1;
                     },
                     /* Case 14: Diamond through superclass, maximally-specific.
                      *
                      * I3[](def)
                      * I1[I3](res), I2[I3]()
                      * C2[I1,I2]()
                      * C1[C2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata = getMethodData(acc, ctx);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final int I3 = builder.addInterface(withDef);
                         final int I2 = builder.addInterface(emptyClass(pck));
                         final int I1 = builder.expected;
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I1);
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.methodref = C1;
                     },
                     /* Case 15: Diamond through superclass,
                      * maximally-specific, skip private.
                      *
                      * I3[](def)
                      * I1[I3](res), I2[I3](priv)
                      * C2[I1,I2]()
                      * C1[C2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata = getMethodData(acc, ctx);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PRIVATE,
                                            MethodData.Context.INSTANCE);
                         final ClassData withPrivDef = new ClassData(pck, meth);
                         final int I3 = builder.addInterface(withDef);
                         final int I2 = builder.addInterface(withPrivDef);
                         final int I1 = builder.expected;
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I1);
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.methodref = C1;
                     },
                     /* Case 16: Diamond through superclass,
                      * maximally-specific, skip static.
                      *
                      * I3[](pub)
                      * I1[I3](res), I2[I3](stat)
                      * C2[I1,I2]()
                      * C1[C2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata = getMethodData(acc, ctx);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef = new ClassData(pck, meth);
                         final int I3 = builder.addInterface(withDef);
                         final int I2 = builder.addInterface(withStatDef);
                         final int I1 = builder.expected;
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I1);
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.methodref = C1;
                     },
                     /* Case 17: Diamond, with superclass, inherit through
                      * superclass, expected at top.
                      *
                      * I2[](res)
                      * C3[I2](), I1[I2]()
                      * C2[I1,C3]()
                      * C1[C2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I2 = builder.expected;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C3 = builder.addInterface(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I1);
                         builder.hier.addInherit(C2, C3);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C3, I2);
                         builder.hier.addInherit(C1, C2);
                         builder.methodref = C1;
                     },
                     /* Case 18: Diamond, with superclass, inherit through
                      * superclass, maximally-specific.
                      *
                      * I2[](def)
                      * C3[I2](), I1[I2](res),
                      * C2[I1,C3]()
                      * C1[I1,C2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Context ctx =
                             builder.classdata.get(builder.expected).methoddata.context;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata = getMethodData(acc, ctx);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final int I2 = builder.addInterface(withDef);
                         final int C3 = builder.addClass(emptyClass(pck));
                         final int I1 = builder.expected;
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I1);
                         builder.hier.addInherit(C2, C3);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C3, I2);
                         builder.hier.addInherit(C1, C2);
                         builder.methodref = C1;
                     });

    public static final Template IfaceMethodrefAmbiguous =
        new Template("IfaceMethodrefAmbiguous",
                     /* Ambiguous.
                      *
                      * I2[](def), I3[](def)
                      * I1[I2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData expected =
                             builder.classdata.get(builder.expected);
                         final int I3 = builder.addInterface(expected);
                         final int I2 = builder.expected;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.methodref = I1;
                     });

    public static final Template MethodrefAmbiguous =
        new Template("MethodrefAmbiguous",
                     /* Ambiguous.
                      *
                      * I1[](def), I2[](def)
                      * C[I2]() = mref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData expected =
                             builder.classdata.get(builder.expected);
                         final int I1 = builder.addInterface(expected);
                         final int I2 = builder.expected;
                         final int C = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C, I2);
                         builder.hier.addInherit(C, I1);
                         builder.methodref = C;
                     });

    /******************************
     *     Callsite Templates     *
     ******************************/

    public static final Template AllCallsiteCases =
        new Template("AllCallsiteCases",
                     Template::callsiteIsMethodref,
                     Template::callsiteSubclassMethodref,
                     Template::callsiteUnrelatedMethodref);

    public static final Template InvokespecialCallsiteCases =
        new Template("InvokespecialCallsiteCases",
                     Template::callsiteIsMethodref,
                     Template::callsiteSubclassMethodref);

    public static final Template CallsiteEqualsMethodref =
        new Template("CallsiteEqualsMethodref",
                     Template::callsiteIsMethodref);

    public static final Template CallsiteSubclassMethodref =
        new Template("CallsiteSubclassMethodref",
                     Template::callsiteSubclassMethodref);

    public static final Template CallsiteUnrelatedToMethodref =
        new Template("CallsiteUnrelatedToMethodref",
                     Template::callsiteUnrelatedMethodref);

    public static final Template CallsiteNotEqualsMethodref =
        new Template("CallsiteNotEqualsMethodref",
                     Template::callsiteSubclassMethodref,
                     Template::callsiteUnrelatedMethodref);

    /*********************************
     * AbstractMethodError Templates *
     *********************************/

    public static final Template ReabstractExpectedIface =
        new Template("ReabstractExpectedIface",
                     (builder) -> {},
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData expected =
                             builder.classdata.get(builder.expected);
                         final ClassData.Package pck = expected.packageId;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata =
                             getMethodData(acc, MethodData.Context.STATIC);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final int C2 = builder.addInterface(withDef);
                         final int C1 = builder.expected;
                         builder.hier.addInherit(C1, C2);
                     },
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData expected =
                             builder.classdata.get(builder.expected);
                         final ClassData.Package pck = expected.packageId;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata =
                             getMethodData(acc, MethodData.Context.INSTANCE);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final int C2 = builder.addInterface(withDef);
                         final int C1 = builder.expected;
                         builder.hier.addInherit(C1, C2);
                     });

    public static final Template ReabstractExpectedClass =
        new Template("ReabstractExpectedClass",
                     ReabstractExpectedIface,
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData expected =
                             builder.classdata.get(builder.expected);
                         final ClassData.Package pck = expected.packageId;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata =
                             getMethodData(acc, MethodData.Context.STATIC);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.expected;
                         builder.hier.addInherit(C1, C2);
                     },
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData expected =
                             builder.classdata.get(builder.expected);
                         final ClassData.Package pck = expected.packageId;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final MethodData mdata =
                             getMethodData(acc, MethodData.Context.INSTANCE);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.expected;
                         builder.hier.addInherit(C1, C2);
                     });

    public static final Template ReabstractMethodrefResolvedClass =
        new Template("ReabstractMethodrefResolvedClass",
                    /* Case 1: Objectref overrides.
                     *
                     * C2[](*) = mref
                     * C1[C2](res) = oref = expected
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final int C2 = builder.methodref;
                        final int C1 = builder.addClass(withDef);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                        builder.expected = C1;
                    },
                    /* Case 2: Objectref's super overrides.
                     *
                     * C3[*](*) = mref
                     * C2[C3](res) = expected
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                        builder.expected = C2;
                    },
                    /* Case 3: Objectref's super overrides, skip private.
                     *
                     * C3[*](*) = mref
                     * C2[C3](res) = expected
                     * C1[C2](priv) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final MethodData meth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPrivDef = new ClassData(pck, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(withPrivDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                        builder.expected = C2;
                    },
                    /* Case 4: Objectref's super overrides, skip static.
                     *
                     * C3[*](*) = mref
                     * C2[C3](res) = expected
                     * C1[C2](stat) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.STATIC);
                        final ClassData withStatDef = new ClassData(pck, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(withStatDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                        builder.expected = C2;
                    });

    public static final Template ReabstractMethodrefResolvedIface =
        new Template("ReabstractMethodrefResolvedIface",
                    /* Case 1: Objectref overrides.
                     *
                     * C2[](*) = mref
                     * C1[C2](res) = oref = expected
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final int C2 = builder.methodref;
                        final int C1 = builder.addClass(withDef);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                        builder.expected = C1;
                    },
                    /* Case 2: Objectref's super overrides.
                     *
                     * C3[](*) = mref
                     * C2[C3](res) = expected
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                        builder.expected = C2;
                    },
                    /* Case 3: Objectref's super overrides, skip private.
                     *
                     * C3[*](*) = mref
                     * C2[C3](res) = expected
                     * C1[C2](priv) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final MethodData meth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPrivDef = new ClassData(pck, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(withPrivDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                        builder.expected = C2;
                    },
                    /* Case 4: Objectref's super overrides, skip static.
                     *
                     * C3[*](*) = mref
                     * C2[C3](res) = expected
                     * C1[C2](stat) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.STATIC);
                        final ClassData withStatDef = new ClassData(pck, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(withStatDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                        builder.expected = C2;
                    },
                    /* Case 5: Overlapping with new interface overriding.
                     *
                     * I2[*](def) = old expected
                     * C2[*](*) = mref, I1[I2](res) = expected
                     * C1[C2,I2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final int C2 = builder.methodref;
                        final int I2 = builder.expected;
                        final int I1 = builder.addInterface(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I1);
                        builder.hier.addInherit(I1, I2);
                        builder.objectref = C1;
                        builder.expected = I1;
                    },
                    /* Case 6: Overlapping with new interface, skip private.
                     *
                     * I2[*](def) = old expected
                     * C2[*](*) = mref, I1[I2](res) = expected
                     * C1[C2,I2](priv) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final MethodData meth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPrivDef = new ClassData(pck, meth);
                        final int C2 = builder.methodref;
                        final int I2 = builder.expected;
                        final int I1 = builder.addInterface(withDef);
                        final int C1 = builder.addClass(withPrivDef);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I1);
                        builder.hier.addInherit(I1, I2);
                        builder.objectref = C1;
                        builder.expected = I1;
                    },
                    /* Case 7: Overlapping with new interface, skip static.
                     *
                     * I2[*](def) = old expected
                     * C2[*](*) = mref, I1[I2](res) = expected
                     * C1[C2,I2](stat) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.STATIC);
                        final ClassData withStatDef = new ClassData(pck, meth);
                        final int C2 = builder.methodref;
                        final int I2 = builder.expected;
                        final int I1 = builder.addInterface(withDef);
                        final int C1 = builder.addClass(withStatDef);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I1);
                        builder.hier.addInherit(I1, I2);
                        builder.objectref = C1;
                        builder.expected = I1;
                    },
                    /* Case 8: Overlap with objectref's super with new
                     * interface overriding, inherit through class.
                     *
                     * I2[*](def) = old expected
                     * C3[](*) = mref, I1[I2](res) = expected
                     * C2[C3,I1]()
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(emptyClass(pck));
                        final int I2 = builder.expected;
                        final int I1 = builder.addInterface(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C2, I1);
                        builder.hier.addInherit(I1, I2);
                        builder.objectref = C1;
                        builder.expected = I1;
                    },
                    /* Case 9: Overlap with objectref's super with new
                     * interface double diamond, overriding.
                     *
                     * I3[*](def) = old expected
                     * C3[](*) = mref, I2[I3](def)
                     * C2[C3,I2](), I1[I2](res) = expected
                     * C1[C2,I1]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(emptyClass(pck));
                        final int I3 = builder.expected;
                        final int I2 = builder.addInterface(withDef);
                        final int I1 = builder.addInterface(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I1);
                        builder.hier.addInherit(C2, I2);
                        builder.hier.addInherit(I1, I2);
                        builder.hier.addInherit(I2, I3);
                        builder.objectref = C1;
                        builder.expected = I1;
                    },
                    /* Case 10: Overlap with objectref's super with new
                     * interface double diamond, skip private.
                     *
                     * I3[*](def) = old expected
                     * C3[](*) = mref, I2[I3](res) = expected
                     * C2[C3,I2](), I1[I2](priv)
                     * C1[C2,I1]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final MethodData meth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPrivDef = new ClassData(pck, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(emptyClass(pck));
                        final int I3 = builder.expected;
                        final int I2 = builder.addInterface(withDef);
                        final int I1 = builder.addInterface(withPrivDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I1);
                        builder.hier.addInherit(C2, I2);
                        builder.hier.addInherit(I1, I2);
                        builder.hier.addInherit(I2, I3);
                        builder.objectref = C1;
                        builder.expected = I2;
                    },
                    /* Case 11: Overlap with objectref's super with new
                     * interface double diamond, skip static.
                     *
                     * I3[*](def) = old expected
                     * C3[](*) = mref, I2[I3](res) = expected
                     * C2[C3,I2](), I1[I2](stat)
                     * C1[C2,I1]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.STATIC);
                        final ClassData withStatDef = new ClassData(pck, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(emptyClass(pck));
                        final int I3 = builder.expected;
                        final int I2 = builder.addInterface(withDef);
                        final int I1 = builder.addInterface(withStatDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I1);
                        builder.hier.addInherit(C2, I2);
                        builder.hier.addInherit(I1, I2);
                        builder.hier.addInherit(I2, I3);
                        builder.objectref = C1;
                        builder.expected = I2;
                    },
                    /* Case 12: Objectref's super overrides, skip interface below.
                     *
                     * C3[](*) = mref
                     * C2[C3](res) = expected, I[](def)
                     * C1[C2,I]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        final int I = builder.addInterface(withDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I);
                        builder.objectref = C1;
                        builder.expected = C2;
                    },
                    /* Case 13: Objectref's super overrides, skip interface above.
                     *
                     * C3[](*) = mref, I[](def)
                     * C2[C3,I](res) = expected
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        final int I = builder.addInterface(withDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C2, I);
                        builder.objectref = C1;
                        builder.expected = C2;
                    });

    public static final Template ReabstractIfaceMethodrefResolved =
        new Template("ReabstractIfaceMethodrefResolved",
                     /* Case 1: Objectref overrides.
                      *
                      * I[](*) = mref
                      * C[I](res) = oref = expected
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                         final int I = builder.methodref;
                         final int C = builder.addClass(withDef);
                         builder.hier.addInherit(C, I);
                         builder.objectref = C;
                         builder.expected = C;
                     },
                     /* Case 2: Diamond, methodref at top, overriding.
                      *
                      * I3[](*) = mref
                      * I1[I3](), I2[I3](res) = expected
                      * C[I1,I2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                         final int I3 = builder.methodref;
                         final int I2 = builder.addInterface(withDef);
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C, I1);
                         builder.hier.addInherit(C, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.objectref = C;
                         builder.expected = I2;
                     },
                     /* Case 3: Diamond, methodref at top, skip static.
                      *
                      * I3[](*) = mref
                      * I1[I3](), I2[I3](res) = expected
                      * C[I1,I2](stat) = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.STATIC);
                        final ClassData withStatDef = new ClassData(pck, meth);
                         final int I3 = builder.methodref;
                         final int I2 = builder.addInterface(withDef);
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C = builder.addClass(withStatDef);
                         builder.hier.addInherit(C, I1);
                         builder.hier.addInherit(C, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.objectref = C;
                         builder.expected = I2;
                     },
                     /* Case 4: Diamond, with superclass, methodref at top,
                      * class overriding.
                      *
                      * I2[](*) = mref
                      * C2[I2](res) = expected, I1[I2]()
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                         builder.expected = C2;
                     },
                     /* Case 5: Diamond, with superclass, methodref at top,
                      * class overriding, skip static.
                      *
                      * I2[](*) = mref
                      * C2[I2](res) = expected, I1[I2]()
                      * C1[I1,C2](stat) = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final ClassData withDef =
                            new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.STATIC);
                        final ClassData withStatDef = new ClassData(pck, meth);
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(withStatDef);
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                         builder.expected = C2;
                     },
                     /* Case 6: Diamond, with superclass, expected at top,
                      * interface overriding
                      *
                      * I2[](*) = mref
                      * C2[I2](), I1[I2](res) = expected
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final ClassData withDef =
                             new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(withDef);
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                         builder.expected = I1;
                     },
                     /* Case 7: Diamond, with superclass, expected at top,
                      * interface skip static
                      *
                      * I2[](*) = mref
                      * C2[I2](), I1[I2](res) = expected
                      * C1[I1,C2](stat) = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final ClassData withDef =
                             new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef = new ClassData(pck, meth);
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(withDef);
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(withStatDef);
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                         builder.expected = I1;
                     },
                     /* Case 8: Y, with superclass, overlaping, expected
                      * at top, class overrides
                      *
                      * C2[I2](res) = expected, I1[](*) = mref
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final ClassData withDef =
                             new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                         final int I1 = builder.methodref;
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.objectref = C1;
                         builder.expected = C2;
                     },
                     /* Case 9: Diamond, with superclass, overlaping, expected
                      * at top, class overrides
                      *
                      * I2[](def) = old expected
                      * C2[I2](res) = expected, I1[](*) = mref
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final ClassData withDef =
                             new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                         final int I2 = builder.expected;
                         final int I1 = builder.methodref;
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                         builder.expected = C2;
                     },
                     /* Case 10: Diamond, with superclass, overlaping, expected
                      * at top, class overrides, skipping static
                      *
                      * I2[](def) = old expected
                      * C2[I2](res) = expected, I1[](*) = mref
                      * C1[I1,C2](stat) = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final ClassData withDef =
                             new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef = new ClassData(pck, meth);
                         final int I2 = builder.expected;
                         final int I1 = builder.methodref;
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(withStatDef);
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                         builder.expected = C2;
                     },
                     /* Case 11: Superclass overrides.
                      *
                      * I[](*) = mref
                      * C2[I](res) = expected
                      * C1[C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final ClassData withDef =
                             new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                         final int I = builder.methodref;
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(C2, I);
                         builder.objectref = C1;
                     },
                     /* Case 12: Superclass overrides, skipping static.
                      *
                      * I[](*) = mref
                      * C2[I](res) = expected
                      * C1[C2](stat) = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final ClassData withDef =
                             new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef = new ClassData(pck, meth);
                         final int I = builder.methodref;
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(withStatDef);
                         builder.hier.addInherit(C1, I);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(C2, I);
                         builder.objectref = C1;
                     },
                     /* Case 13: Double diamond, with superclass, inherit through
                      * superclass, expected at top.
                      *
                      * I3[](def) = old expected
                      * C3[I3](), I2[*](*) = mref
                      * C2[I2,C3](), I1[I2](res) = expected
                      * C1[C2,I1]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final ClassData withDef =
                             new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                         final int I3 = builder.expected;
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(withDef);
                         final int C3 = builder.addClass(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(C2, C3);
                         builder.hier.addInherit(C3, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C1, I1);
                         builder.objectref = C1;
                         builder.expected = I1;
                     },
                     /* Case 14: Double diamond, with superclass, inherit through
                      * superclass, expected at top.
                      *
                      * I3[](def) = mref
                      * C3[I3](), I2[*](*) = expected
                      * C2[I2,C3](), I1[I2](priv)
                      * C1[C2,I1]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final ClassData withDef =
                             new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                         final MethodData meth =
                             new MethodData(MethodData.Access.PRIVATE,
                                            MethodData.Context.INSTANCE);
                         final ClassData withPrivDef = new ClassData(pck, meth);
                         final int I3 = builder.methodref;
                         final int I2 = builder.addInterface(withDef);
                         final int I1 = builder.addInterface(withPrivDef);
                         final int C3 = builder.addClass(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(C2, C3);
                         builder.hier.addInherit(C3, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I2, I3);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C1, I1);
                         builder.objectref = C1;
                         builder.expected = I2;
                     },
                     /* Case 15: Double diamond, with superclass, inherit through
                      * superclass, expected at top.
                      *
                      * I3[](def) = mref
                      * C3[I3](), I2[*](*) = expected
                      * C2[I2,C3](), I1[I2](stat)
                      * C1[C2,I1]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final MethodData.Access acc =
                             builder.classdata.get(builder.expected).methoddata.access;
                         final ClassData withDef =
                             new ClassData(pck, new MethodData(acc, MethodData.Context.ABSTRACT));
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef = new ClassData(pck, meth);
                         final int I3 = builder.methodref;
                         final int I2 = builder.addInterface(withDef);
                         final int I1 = builder.addInterface(withStatDef);
                         final int C3 = builder.addClass(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(C2, C3);
                         builder.hier.addInherit(C3, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I2, I3);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C1, I1);
                         builder.objectref = C1;
                         builder.expected = I2;
                     });

    /******************************
     *    Abstract Overrides      *
     ******************************/

    public static final Template OverrideAbstractExpectedIface =
        Template.ResolutionOverride(EnumSet.of(Template.Kind.INTERFACE),
                                    EnumSet.of(MethodData.Access.PUBLIC),
                                    EnumSet.allOf(MethodData.Context.class),
                                    EnumSet.of(ClassData.Package.SAME));

    public static final Template OverrideAbstractExpectedClass =
        Template.ResolutionOverride(EnumSet.allOf(Template.Kind.class),
                                    EnumSet.of(MethodData.Access.PUBLIC),
                                    EnumSet.allOf(MethodData.Context.class),
                                    EnumSet.of(ClassData.Package.SAME));

    public static final Template SelectionOverrideAbstract =
        new Template("SelectionOverrideAbstract",
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData expected =
                             builder.classdata.get(builder.expected);
                         final MethodData olddef =
                             expected.methoddata;
                         if (MethodData.Context.ABSTRACT == olddef.context) {
                             final ClassData.Package pck = expected.packageId;
                             final MethodData.Access acc = olddef.access;
                             final MethodData mdata =
                                 getMethodData(MethodData.Access.PUBLIC,
                                               MethodData.Context.INSTANCE);
                             final ClassData withDef = new ClassData(pck, mdata);
                             final int C2 = builder.objectref;
                             final int C1 = builder.addClass(withDef);
                             builder.hier.addInherit(C1, C2);
                             builder.objectref = C1;
                             builder.expected = C1;
                         }
                     });


    /******************************
     * Ignored Abstract Templates *
     ******************************/

    public static final Template IgnoredAbstract =
        new Template("IgnoredAbstract",
                     (builder) -> {},
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData methodref =
                             builder.classdata.get(builder.methodref);
                         final ClassData.Package pck = methodref.packageId;
                         final MethodData mdata =
                             getMethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.ABSTRACT);
                         final ClassData withDef = new ClassData(pck, mdata);
                         final int C2 = builder.addInterface(withDef);
                         final int C1 = builder.methodref;
                         builder.hier.addInherit(C1, C2);
                     });

    /******************************
     *     Selection Templates    *
     ******************************/



    public static final Template TrivialObjectref =
        new Template("TrivialObjectref",
                     Collections.singleton((builder) -> {
                             builder.objectref = builder.methodref;
                         }));

    public static final Template TrivialObjectrefNotEqualMethodref =
        new Template("TrivialObjectrefNotEqualMethodref",
                     Collections.singleton(
                         (final SelectionResolutionTestCase.Builder builder) -> {
                             final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                             final ClassData.Package pck = oldexpected.packageId;
                             final int C2 = builder.methodref;
                             final int C1 = builder.addClass(emptyClass(pck));
                             builder.hier.addInherit(C1, C2);
                             builder.objectref = C1;
                         }));

    public static final Template MethodrefSelectionResolvedIsClassNoOverride =
        new Template("MethodrefSelectionResolvedIsClassNoOverride",
                    /* Trivial.
                     *
                     * C[](*) = mref = oref
                     */
                    (builder) -> {
                        builder.objectref = builder.methodref;
                    },
                    /* Case 1: Inherit from super.
                     *
                     * C2[](*) = mref
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final int C2 = builder.methodref;
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 2: Objectref has private def.
                     *
                     * C2[](*) = mref
                     * C1[C2](priv) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withDef = new ClassData(pck, meth);
                        final int C2 = builder.methodref;
                        final int C1 = builder.addClass(withDef);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 3: Objectref has static def.
                     *
                     * C2[](*) = mref
                     * C1[C2](stat) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.STATIC);
                        final ClassData withDef = new ClassData(pck, meth);
                        final int C2 = builder.methodref;
                        final int C1 = builder.addClass(withDef);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 4: Skip inherit from interface.
                     *
                     * C2[](*) = mref, I[](def)
                     * C1[C2,I]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData.Package pck = oldexpected.packageId;
                        final MethodData.Context ctx =
                            builder.classdata.get(builder.expected).methoddata.context;
                        final MethodData.Access acc =
                            builder.classdata.get(builder.expected).methoddata.access;
                        final MethodData mdata = getMethodData(acc, ctx);
                        final ClassData withDef = new ClassData(pck, mdata);
                        final int C2 = builder.methodref;
                        final int C1 = builder.addClass(emptyClass(pck));
                        final int I = builder.addInterface(withDef);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I);
                        builder.objectref = C1;
                    },
                    /* Case 5: Objectref's super has a private def.
                     *
                     * C3[*](*) = mref
                     * C2[C3](res) = expected
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withDef =
                            new ClassData(pck, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 6: Objectref's super has a static def.
                     *
                     * C3[*](*) = mref
                     * C2[C3](res) = expected
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.STATIC);
                        final ClassData withDef =
                            new ClassData(pck, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    });

    public static final Template MethodrefSelectionResolvedIsClassOverride =
        new Template("MethodrefSelectionResolvedIsClassOverride",
                    /* Case 7: Objectref overrides.
                     *
                     * C2[](*) = mref
                     * C1[C2](res) = oref = expected
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                        final int C2 = builder.methodref;
                        final int C1 = builder.addClass(withDef);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                        builder.expected = C1;
                    },
                    /* Case 8: Objectref's super overrides.
                     *
                     * C3[*](*) = mref
                     * C2[C3](res)
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                        builder.expected = C2;
                    },
                    /* Case 9: Objectref's super overrides,
                     *         objectref has a private def.
                     *
                     * C3[*](*) = mref
                     * C2[C3](res)
                     * C1[C2](priv) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPrivDef =
                            new ClassData(pck, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(withPrivDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                        builder.expected = C2;
                    },
                    /* Case 10: Objectref's super overrides,
                     *          objectref has a static def.
                     *
                     * C3[*](*) = mref
                     * C2[C3](res)
                     * C1[C2](stat) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.STATIC);
                        final ClassData withPrivDef =
                            new ClassData(pck, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(withPrivDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                        builder.expected = C2;
                    });

    public static final Template MethodrefSelectionResolvedIsClass =
        new Template("MethodrefSelectionResolvedIsClass",
                     MethodrefSelectionResolvedIsClassNoOverride,
                     MethodrefSelectionResolvedIsClassOverride);

    public static final Template MethodrefSelectionPackageSkipNoOverride =
        new Template("MethodrefSelectionPackageSkipNoOverride",
                     MethodrefSelectionResolvedIsClass,
                    /* Case 11: Objectref has public def in other package.
                     *
                     * C2[](*) = mref
                     * Other.C1[C2](pub) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.INSTANCE);
                        final ClassData withDef =
                            new ClassData(ClassData.Package.OTHER, meth);
                        final int C2 = builder.methodref;
                        final int C1 = builder.addClass(withDef);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 12: Objectref has package private def in other package.
                     *
                     * C2[](*) = mref
                     * Other.C1[C2](pack) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PACKAGE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withDef =
                            new ClassData(ClassData.Package.OTHER, meth);
                        final int C2 = builder.methodref;
                        final int C1 = builder.addClass(withDef);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 13: Objectref has protected def in other package.
                     *
                     * C2[](*) = mref
                     * Other.C1[C2](prot) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PROTECTED,
                                           MethodData.Context.INSTANCE);
                        final ClassData withDef =
                            new ClassData(ClassData.Package.OTHER, meth);
                        final int C2 = builder.methodref;
                        final int C1 = builder.addClass(withDef);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 14: Objectref's super has a public def in other package.
                     *
                     * C3[*](*) = mref
                     * Other.C2[C3](pub) = expected
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.INSTANCE);
                        final ClassData withDef =
                            new ClassData(ClassData.Package.OTHER, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 15: Objectref's super has a package
                     * private def in other package.
                     *
                     * C3[*](*) = mref
                     * Other.C2[C3](pack) = expected
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PACKAGE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withDef =
                            new ClassData(ClassData.Package.OTHER, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 16: Objectref's super has a protected def
                     * in other package.
                     *
                     * C3[*](*) = mref
                     * Other.C2[C3](pack) = expected
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PROTECTED,
                                           MethodData.Context.INSTANCE);
                        final ClassData withDef =
                            new ClassData(ClassData.Package.OTHER, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 18: Objectref's has a public def in other
                     * package, skip private.
                     *
                     * C3[*](*) = mref
                     * Other.C2[C3](priv)
                     * C1[C2](pub) = oref, expected
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.INSTANCE);
                        final ClassData withDef =
                            new ClassData(ClassData.Package.OTHER, meth);
                        final MethodData privmeth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPrivDef =
                            new ClassData(pck, privmeth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withPrivDef);
                        final int C1 = builder.addClass(withDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 19: Objectref's has a package private def in other
                     * package, skip private.
                     *
                     * C3[*](*) = mref
                     * Other.C2[C3](priv)
                     * C1[C2](pack) = oref, expected
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PACKAGE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withDef =
                            new ClassData(ClassData.Package.OTHER, meth);
                        final MethodData privmeth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPrivDef =
                            new ClassData(pck, privmeth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withPrivDef);
                        final int C1 = builder.addClass(withDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 20: Objectref's has a protected def in other
                     * package, skip private.
                     *
                     * C3[*](*) = mref
                     * Other.C2[C3](priv)
                     * C1[C2](pro) = oref, expected
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PROTECTED,
                                           MethodData.Context.INSTANCE);
                        final ClassData withDef =
                            new ClassData(ClassData.Package.OTHER, meth);
                        final MethodData privmeth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPrivDef =
                            new ClassData(pck, privmeth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withPrivDef);
                        final int C1 = builder.addClass(withDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 21: Objectref's super has a public def in other
                     * package, skip private.
                     *
                     * C3[*](*) = mref
                     * Other.C2[C3](pub) = expected
                     * C1[C2](priv) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.INSTANCE);
                        final ClassData withDef =
                            new ClassData(ClassData.Package.OTHER, meth);
                        final MethodData privmeth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPrivDef =
                            new ClassData(pck, privmeth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(withPrivDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 22: Objectref's superhas a package private
                     * def in other package, skip private.
                     *
                     * C3[*](*) = mref
                     * Other.C2[C3](pack) = expected
                     * C1[C2](priv) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PACKAGE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withDef =
                            new ClassData(ClassData.Package.OTHER, meth);
                        final MethodData privmeth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPrivDef =
                            new ClassData(pck, privmeth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(withPrivDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 23: Objectref's super has a protected def
                     * in other package, skip private.
                     *
                     * C3[*](*) = mref
                     * Other.C2[C3](pro) = expected
                     * C1[C2](priv) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PROTECTED,
                                           MethodData.Context.INSTANCE);
                        final ClassData withDef =
                            new ClassData(ClassData.Package.OTHER, meth);
                        final MethodData privmeth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPrivDef =
                            new ClassData(pck, privmeth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withPrivDef);
                        final int C1 = builder.addClass(withDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    });

    public static final Template MethodrefSelectionPackageSkip =
        new Template("MethodrefSelectionPackageSkip",
                     MethodrefSelectionPackageSkipNoOverride,
                    /* Case 17: Transitive override.
                     *
                     * C3[*](*) = mref
                     * C2[C3](pub)
                     * Other.C1[C2](pack) = oref, expected
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.expected).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.INSTANCE);
                        final MethodData packmeth =
                            new MethodData(MethodData.Access.PACKAGE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPubDef = new ClassData(pck, meth);
                        final ClassData withPackDef =
                            new ClassData(ClassData.Package.OTHER, packmeth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withPubDef);
                        final int C1 = builder.addClass(withPackDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                        builder.expected = C1;
                    },
                    /* Case 24: Transitive override, skip private in between.
                     *
                     * C4[*](*) = mref
                     * C3[C4](pub)
                     * C2[C3](priv)
                     * Other.C1[C2](pack) = oref, expected
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.expected).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.INSTANCE);
                        final MethodData packmeth =
                            new MethodData(MethodData.Access.PACKAGE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPubDef = new ClassData(pck, meth);
                        final ClassData withPackDef =
                            new ClassData(ClassData.Package.OTHER, packmeth);
                        final MethodData privmeth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPrivDef =
                            new ClassData(pck, privmeth);
                        final int C4 = builder.methodref;
                        final int C3 = builder.addClass(withPubDef);
                        final int C2 = builder.addClass(withPrivDef);
                        final int C1 = builder.addClass(withPackDef);
                        builder.hier.addInherit(C3, C4);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                        builder.expected = C1;
                    },
                    /* Case 25: Transitive override, skip private in between.
                     *
                     * C4[*](*) = mref
                     * C3[C4](pub)
                     * Other.C2[C3](pack) = expected
                     * C1[C2](pack) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.expected).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.INSTANCE);
                        final MethodData packmeth =
                            new MethodData(MethodData.Access.PACKAGE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPubDef = new ClassData(pck, meth);
                        final ClassData withPackDef =
                            new ClassData(ClassData.Package.OTHER, packmeth);
                        final MethodData privmeth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPrivDef =
                            new ClassData(pck, privmeth);
                        final int C4 = builder.methodref;
                        final int C3 = builder.addClass(withPubDef);
                        final int C2 = builder.addClass(withPackDef);
                        final int C1 = builder.addClass(withPrivDef);
                        builder.hier.addInherit(C3, C4);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C2;
                        builder.expected = C2;
                    });

    public static final Template MethodrefSelectionResolvedIsIfaceNoOverride =
        new Template("MethodrefSelectionResolvedIsIfaceNoOverride",
                    /* Trivial objectref.
                     *
                     * C[](*) = mref = oref
                     */
                    (builder) -> {
                        builder.objectref = builder.methodref;
                    },
                    /* Case 1: Inherit from super.
                     *
                     * C2[](*) = mref
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final int C2 = builder.methodref;
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 2: Objectref has private def.
                     *
                     * C2[](*) = mref
                     * C1[C2](priv) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withDef = new ClassData(pck, meth);
                        final int C2 = builder.methodref;
                        final int C1 = builder.addClass(withDef);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 3: Objectref has static def.
                     *
                     * C2[](*) = mref
                     * C1[C2](stat) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.STATIC);
                        final ClassData withDef = new ClassData(pck, meth);
                        final int C2 = builder.methodref;
                        final int C1 = builder.addClass(withDef);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 4: Overlapping.
                     *
                     * I[*](res) = expected
                     * C2[*](*) = mref
                     * C1[C2,I]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final int C2 = builder.methodref;
                        final int I = builder.expected;
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I);
                        builder.objectref = C1;
                    },
                    /* Case 5: Overlapping with new interface.
                     *
                     * I2[*](res) = expected
                     * C2[*](*) = mref, I1[I2]()
                     * C1[C2,I2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final int C2 = builder.methodref;
                        final int I2 = builder.expected;
                        final int I1 = builder.addInterface(emptyClass(pck));
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I1);
                        builder.hier.addInherit(I1, I2);
                        builder.objectref = C1;
                    },
                    /* Case 6: Overlapping with new interface with private def.
                     *
                     * I2[*](res) = expected
                     * C2[*](*) = mref, I1[I2](priv)
                     * C1[C2,I2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final MethodData meth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPrivDef =
                            new ClassData(pck, meth);
                        final int C2 = builder.methodref;
                        final int I2 = builder.expected;
                        final int I1 = builder.addInterface(withPrivDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I1);
                        builder.hier.addInherit(I1, I2);
                        builder.objectref = C1;
                    },
                    /* Case 7: Overlapping with new interface with static def.
                     *
                     * I2[*](res) = expected
                     * C2[*](*) = mref, I1[I2](stat)
                     * C1[C2,I2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.STATIC);
                        final ClassData withStatDef =
                            new ClassData(pck, meth);
                        final int C2 = builder.methodref;
                        final int I2 = builder.expected;
                        final int I1 = builder.addInterface(withStatDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I1);
                        builder.hier.addInherit(I1, I2);
                        builder.objectref = C1;
                    },
                    /* Case 8: Objectref's super has a private def.
                     *
                     * C3[*](*) = mref
                     * C2[C3](priv)
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withDef =
                            new ClassData(pck, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 9: Objectref's super has a static def.
                     *
                     * C3[*](*) = mref
                     * C2[C3](stat)
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.STATIC);
                        final ClassData withDef =
                            new ClassData(pck, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    },
                    /* Case 10: Overlap with objectref's super.
                     *
                     * I[*](res) = expected
                     * C3[](*) = mref
                     * C2[C3,I]()
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(emptyClass(pck));
                        final int I = builder.expected;
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C2, I);
                        builder.objectref = C1;
                    },
                    /* Case 11: Overlap with objectref's super with new interface.
                     *
                     * I2[*](res) = expected
                     * C3[](*) = mref, I1[I2]()
                     * C2[C3,I1]()
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(emptyClass(pck));
                        final int I2 = builder.expected;
                        final int I1 = builder.addInterface(emptyClass(pck));
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C2, I1);
                        builder.hier.addInherit(I1, I2);
                        builder.objectref = C1;
                    },
                    /* Case 12: Overlap with objectref's super with new
                     * interface with private def.
                     *
                     * I2[*](res) = expected
                     * C3[](*) = mref, I1[I2](priv)
                     * C2[C3,I1]()
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final MethodData meth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPrivDef =
                            new ClassData(pck, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(emptyClass(pck));
                        final int I2 = builder.expected;
                        final int I1 = builder.addInterface(withPrivDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C2, I1);
                        builder.hier.addInherit(I1, I2);
                        builder.objectref = C1;
                    },
                    /* Case 13: Overlap with objectref's super with new
                     * interface with static def.
                     *
                     * I2[*](res) = expected
                     * C3[](*) = mref, I1[I2](stat)
                     * C2[C3,I1]()
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.STATIC);
                        final ClassData withStatDef =
                            new ClassData(pck, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(emptyClass(pck));
                        final int I2 = builder.expected;
                        final int I1 = builder.addInterface(withStatDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C2, I1);
                        builder.hier.addInherit(I1, I2);
                        builder.objectref = C1;
                    },
                    /* Case 14: Overlap with objectref's super with new
                     * interface double diamond.
                     *
                     * I3[*](res) = expected
                     * C3[](*) = mref, I2[I3]()
                     * C2[C3,I2](), I1[I2]()
                     * C1[C2,I1]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(emptyClass(pck));
                        final int I3 = builder.expected;
                        final int I2 = builder.addInterface(emptyClass(pck));
                        final int I1 = builder.addInterface(emptyClass(pck));
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I1);
                        builder.hier.addInherit(C2, I2);
                        builder.hier.addInherit(I1, I2);
                        builder.objectref = C1;
                    },
                    /* Case 15: Overlapping with new interface with private def.
                     *
                     * C2[*](*) = mref, I1[](priv)
                     * C1[C2,I2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final MethodData meth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPrivDef =
                            new ClassData(pck, meth);
                        final int C2 = builder.methodref;
                        final int I1 = builder.addInterface(withPrivDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I1);
                        builder.objectref = C1;
                    },
                    /* Case 16: Overlapping with new interface with static def.
                     *
                     * C2[*](*) = mref, I1[](stat)
                     * C1[C2,I2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.STATIC);
                        final ClassData withStatDef =
                            new ClassData(pck, meth);
                        final int C2 = builder.methodref;
                        final int I1 = builder.addInterface(withStatDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I1);
                        builder.objectref = C1;
                    });

    public static final Template MethodrefSelectionResolvedIsIface =
        new Template("MethodrefSelectionResolvedIsIface",
                     MethodrefSelectionResolvedIsIfaceNoOverride,
                    /* Case 17: Objectref overrides.
                     *
                     * C2[](*) = mref
                     * C1[C2](res) = oref = expected
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                        final int C2 = builder.methodref;
                        final int C1 = builder.addClass(withDef);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                        builder.expected = C1;
                    },
                    /* Case 18: Overlapping with new interface overriding.
                     *
                     * I2[*](def) = old expected
                     * C2[*](*) = mref, I1[I2](res) = expected
                     * C1[C2,I2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                        final int C2 = builder.methodref;
                        final int I2 = builder.expected;
                        final int I1 = builder.addInterface(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I1);
                        builder.hier.addInherit(I1, I2);
                        builder.objectref = C1;
                        builder.expected = I1;
                    },
                    /* Case 19: Objectref's super overrides.
                     *
                     * C3[](*) = mref
                     * C2[C3](res) = expected
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                        builder.expected = C2;
                    },
                    /* Case 20: Overlap with objectref's super with
                     * new interface overriding.
                     *
                     * I2[*](def) = old expected
                     * C3[](*) = mref, I1[I2](res) = expected
                     * C2[C3,I1]()
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(emptyClass(pck));
                        final int I2 = builder.expected;
                        final int I1 = builder.addInterface(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C2, I1);
                        builder.hier.addInherit(I1, I2);
                        builder.objectref = C1;
                        builder.expected = I1;
                    },
                    /* Case 21: Overlap with objectref's super with new
                     * interface double diamond, overriding.
                     *
                     * I3[*](def) = old expected
                     * C3[](*) = mref, I2[I3](def)
                     * C2[C3,I2](), I1[I2](res) = expected
                     * C1[C2,I1]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(emptyClass(pck));
                        final int I3 = builder.expected;
                        final int I2 = builder.addInterface(withDef);
                        final int I1 = builder.addInterface(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I1);
                        builder.hier.addInherit(C2, I2);
                        builder.hier.addInherit(I1, I2);
                        builder.hier.addInherit(I2, I3);
                        builder.objectref = C1;
                        builder.expected = I1;
                    },
                    /* Case 22: Objectref's super overrides, skip private.
                     *
                     * C3[](*) = mref
                     * C2[C3](res) = expected
                     * C1[C2](priv) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPrivDef =
                            new ClassData(pck, meth);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(withPrivDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                        builder.expected = C2;
                    },
                    /* Case 23: Objectref's super overrides, skip static.
                     *
                     * C3[](*) = mref
                     * C2[C3](res) = expected
                     * C1[C2](stat) = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.STATIC);
                        final ClassData withStatDef =
                            new ClassData(pck, meth);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(withStatDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                        builder.expected = C2;
                    },
                    /* Case 24: Overlap with objectref's super with new
                     * interface double diamond, overriding, skip private.
                     *
                     * I3[*](def) = old expected
                     * C3[](*) = mref, I2[I3](res) = expected
                     * C2[C3,I2](), I1[I2](priv)
                     * C1[C2,I1]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PRIVATE,
                                           MethodData.Context.INSTANCE);
                        final ClassData withPrivDef =
                            new ClassData(pck, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(emptyClass(pck));
                        final int I3 = builder.expected;
                        final int I2 = builder.addInterface(withDef);
                        final int I1 = builder.addInterface(withPrivDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I1);
                        builder.hier.addInherit(C2, I2);
                        builder.hier.addInherit(I1, I2);
                        builder.hier.addInherit(I2, I3);
                        builder.objectref = C1;
                        builder.expected = I2;
                    },
                    /* Case 25: Overlap with objectref's super with new
                     * interface double diamond, overriding, skip static.
                     *
                     * I3[*](def) = old expected
                     * C3[](*) = mref, I2[I3](res) = expected
                     * C2[C3,I2](), I1[I2](stat)
                     * C1[C2,I1]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                        final MethodData meth =
                            new MethodData(MethodData.Access.PUBLIC,
                                           MethodData.Context.STATIC);
                        final ClassData withStatDef =
                            new ClassData(pck, meth);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(emptyClass(pck));
                        final int I3 = builder.expected;
                        final int I2 = builder.addInterface(withDef);
                        final int I1 = builder.addInterface(withStatDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I1);
                        builder.hier.addInherit(C2, I2);
                        builder.hier.addInherit(I1, I2);
                        builder.hier.addInherit(I2, I3);
                        builder.objectref = C1;
                        builder.expected = I2;
                    },
                    /* Case 26: Skip interface method after class overrides.
                     *
                     * C3[](*) = mref
                     * C2[C3](res) = expected, I[](def)
                     * C1[C2, I]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        final int I = builder.addInterface(withDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I);
                        builder.objectref = C1;
                        builder.expected = C2;
                    },
                    /* Case 27: Skip interface method after class overrides.
                     *
                     * C3[](*) = mref, I[](def)
                     * C2[C3,I](res) = expected
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        final int I = builder.addInterface(withDef);
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C2, I);
                        builder.objectref = C1;
                        builder.expected = C2;
                    },
                    /* Case 28: Overlap with objectref's super with new
                     * interface double diamond, overriding.
                     *
                     * I3[*](def) = old expected
                     * C3[](*) = mref, I2[I3](def)
                     * C2[C3,I2](res) = expected, I1[I2](def) = expected
                     * C1[C2,I1]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                        final int C3 = builder.methodref;
                        final int C2 = builder.addClass(withDef);
                        final int I3 = builder.expected;
                        final int I2 = builder.addInterface(withDef);
                        final int I1 = builder.addInterface(withDef);
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I1);
                        builder.hier.addInherit(C2, I2);
                        builder.hier.addInherit(I1, I2);
                        builder.hier.addInherit(I2, I3);
                        builder.objectref = C1;
                        builder.expected = C2;
                    });

    public static final Template IfaceMethodrefSelectionNoOverride =
        new Template("IfaceMethodrefSelectionNoOverride",
                     /* Case 1: Inherit from super.
                      *
                      * I[](*) = mref
                      * C[I]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.methodref).packageId;
                         final int I = builder.methodref;
                         final int C = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C, I);
                         builder.objectref = C;
                     },
                     /* Case 2: Objectref has static def
                      *
                      * I[](*) = mref
                      * C[I](stat) = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.methodref).packageId;
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef =
                             new ClassData(pck, meth);
                         final int I = builder.methodref;
                         final int C = builder.addClass(withStatDef);
                         builder.hier.addInherit(C, I);
                         builder.objectref = C;
                     },
                     /* Case 3: Diamond, methodref at top.
                      *
                      * I3[](*) = mref
                      * I1[I3](), I2[I3]()
                      * C[I1,I2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I3 = builder.methodref;
                         final int I2 = builder.addInterface(emptyClass(pck));
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C, I1);
                         builder.hier.addInherit(C, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.objectref = C;
                     },
                     /* Case 4: Diamond, methodref at top, skip private def
                      *
                      * I3[](*) = mref
                      * I1[I3](), I2[I3](priv)
                      * C[I1,I2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData meth =
                             new MethodData(MethodData.Access.PRIVATE,
                                            MethodData.Context.INSTANCE);
                         final ClassData withPrivDef =
                             new ClassData(pck, meth);
                         final int I3 = builder.methodref;
                         final int I2 = builder.addInterface(withPrivDef);
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C, I1);
                         builder.hier.addInherit(C, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.objectref = C;
                     },
                     /* Case 5: Diamond, methodref at top, skip static def
                      *
                      * I3[](*) = mref
                      * I1[I3](), I2[I3](stat)
                      * C[I1,I2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef =
                             new ClassData(pck, meth);
                         final int I3 = builder.methodref;
                         final int I2 = builder.addInterface(withStatDef);
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C, I1);
                         builder.hier.addInherit(C, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.objectref = C;
                     },
                     /* Case 6: Diamond, overlap with resolution.
                      *
                      * I3[](res) = expected
                      * I1[I3](), I2[](*) = mref
                      * C[I1,I2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I3 = builder.expected;
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C, I1);
                         builder.hier.addInherit(C, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.objectref = C;
                     },
                     /* Case 7: Diamond, with superclass, expected at top.
                      *
                      * I2[](*) = mref
                      * C2[I2](), I1[I2]()
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                     },
                     /* Case 8: Diamond, with superclass, expected at top,
                      * class has static def.
                      *
                      * I2[](*) = mref
                      * C2[I2](stat), I1[I2]()
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef =
                             new ClassData(pck, meth);
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C2 = builder.addClass(withStatDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                     },
                     /* Case 9: Diamond, with superclass, expected at top,
                      * interface has private def
                      *
                      * I2[](*) = mref
                      * C2[I2](), I1[I2](priv)
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData meth =
                             new MethodData(MethodData.Access.PRIVATE,
                                            MethodData.Context.INSTANCE);
                         final ClassData withPrivDef =
                             new ClassData(pck, meth);
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(withPrivDef);
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                     },
                     /* Case 10: Diamond, with superclass, expected at top,
                      * interface has static def
                      *
                      * I2[](*) = mref
                      * C2[I2](), I1[I2](stat)
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withPrivDef =
                             new ClassData(pck, meth);
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(withPrivDef);
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                     },
                     /* Case 11: Y, with superclass, expected
                      * at top.
                      *
                      * C2[](), I1[](*) = mref
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I1 = builder.methodref;
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.objectref = C1;
                     },
                     /* Case 12: Y, with superclass, expected
                      * at top, class has static def
                      *
                      * C2[](stat), I1[](*) = mref
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef =
                             new ClassData(pck, meth);
                         final int I1 = builder.methodref;
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.objectref = C1;
                     },
                     /* Case 13: Diamond, with superclass, overlapping, expected
                      * at top.
                      *
                      * I2[](res) = expected
                      * C2[I2](), I1[](*) = mref
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I2 = builder.expected;
                         final int I1 = builder.methodref;
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                     },
                     /* Case 14: Diamond, with superclass, overlapping, expected
                      * at top, class has static def
                      *
                      * I2[](def) = expected
                      * C2[I2](stat), I1[](*) = mref
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef =
                             new ClassData(pck, meth);
                         final int I2 = builder.expected;
                         final int I1 = builder.methodref;
                         final int C2 = builder.addClass(withStatDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                     },
                     /* Case 15: Inherit through superclass.
                      *
                      * I[](*) = mref
                      * C2[I]()
                      * C1[C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I = builder.methodref;
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(C2, I);
                         builder.objectref = C1;
                     },
                     /* Case 16: Superclass has static def.
                      *
                      * I[](*) = mref
                      * C2[I](stat) = expected
                      * C1[C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef =
                             new ClassData(pck, meth);
                         final int I = builder.methodref;
                         final int C2 = builder.addClass(withStatDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(C2, I);
                         builder.objectref = C1;
                     },
                     /* Case 17: Diamond, inherit through superclass,
                      * methodref at top.
                      *
                      * I3[](*) = mref
                      * I1[I3](), I2[I3]()
                      * C2[I1,I2]()
                      * C1[C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I3 = builder.methodref;
                         final int I2 = builder.addInterface(emptyClass(pck));
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I1);
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.objectref = C1;
                     },
                     /* Case 18: Diamond, with superclass, inherit through
                      * superclass, methodref at top.
                      *
                      * I2[](*) = mref
                      * C3[I2](), I1[I2]()
                      * C2[I1,C3]()
                      * C1[C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C3 = builder.addClass(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I1);
                         builder.hier.addInherit(C2, C3);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C3, I2);
                         builder.hier.addInherit(C1, C2);
                         builder.objectref = C1;
                     },
                     /* Case 19: Diamond, inherit through superclass,
                      * expected at top, skip private.
                      *
                      * I3[](*) = mref
                      * I1[I3](), I2[I3](priv)
                      * C2[I1,I2]()
                      * C1[C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData meth =
                             new MethodData(MethodData.Access.PRIVATE,
                                            MethodData.Context.INSTANCE);
                         final ClassData withPrivDef =
                             new ClassData(pck, meth);
                         final int I3 = builder.methodref;
                         final int I2 = builder.addInterface(withPrivDef);
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I1);
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.objectref = C1;
                     },
                     /* Case 20: Diamond, inherit through superclass,
                      * expected at top, skip static.
                      *
                      * I3[](*) = mref
                      * I1[I3](), I2[I3](stat)
                      * C2[I1,I2]()
                      * C1[C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef =
                             new ClassData(pck, meth);
                         final int I3 = builder.methodref;
                         final int I2 = builder.addInterface(withStatDef);
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I1);
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.objectref = C1;
                     },
                     /* Case 21: Diamond, inherit through superclass,
                      * overlapping, expected at top.
                      *
                      * I3[](res) = expected
                      * I1[I3](), I2[*](*) = mref
                      * C2[I1,I2]()
                      * C1[C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I3 = builder.expected;
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I1);
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.objectref = C1;
                     },
                     /* Case 22: Y, with superclass, inherit through
                      * superclass, expected at top.
                      *
                      * C3[](), I1[*](*) = mref
                      * C2[I1,C3]()
                      * C1[C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I1 = builder.methodref;
                         final int C3 = builder.addClass(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I1);
                         builder.hier.addInherit(C2, C3);
                         builder.hier.addInherit(C1, C2);
                         builder.objectref = C1;
                     },
                     /* Case 23: Diamond, with superclass, inherit through
                      * superclass, overlapping, expected at top.
                      *
                      * I2[](res) = expected
                      * C3[I2](), I1[*](*) = mref
                      * C2[I1,C3]()
                      * C1[C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I2 = builder.expected;
                         final int I1 = builder.methodref;
                         final int C3 = builder.addClass(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I1);
                         builder.hier.addInherit(C2, C3);
                         builder.hier.addInherit(C3, I2);
                         builder.hier.addInherit(C1, C2);
                         builder.objectref = C1;
                     },
                     /* Case 24: Double diamond, with superclass, inherit through
                      * superclass, overlapping expected at top.
                      *
                      * I3[](res) = expected
                      * C3[I3](), I2[*](*) = mref
                      * C2[I2,C3](), I1[I2]()
                      * C1[C2,I1]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final int I3 = builder.expected;
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C3 = builder.addClass(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(C2, C3);
                         builder.hier.addInherit(C3, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C1, I1);
                         builder.objectref = C1;
                     },
                     /* Case 25: Double diamond, with superclass, inherit through
                      * superclass, skip private.
                      *
                      * I3[](def) = old expected
                      * C3[I3](), I2[*](*) = mref
                      * C2[I2,C3](), I1[I2](priv)
                      * C1[C2,I1]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData meth =
                             new MethodData(MethodData.Access.PRIVATE,
                                            MethodData.Context.INSTANCE);
                         final ClassData withPrivDef =
                             new ClassData(pck, meth);
                         final int I3 = builder.expected;
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(withPrivDef);
                         final int C3 = builder.addClass(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(C2, C3);
                         builder.hier.addInherit(C3, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C1, I1);
                         builder.objectref = C1;
                     },
                     /* Case 26: Double diamond, with superclass, inherit through
                      * superclass, skip static.
                      *
                      * I3[](def) = old expected
                      * C3[I3](), I2[*](*) = mref
                      * C2[I2,C3](), I1[I2](stat)
                      * C1[C2,I1]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef =
                             new ClassData(pck, meth);
                         final int I3 = builder.expected;
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(withStatDef);
                         final int C3 = builder.addClass(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(C2, C3);
                         builder.hier.addInherit(C3, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C1, I1);
                         builder.objectref = C1;
                     });

    public static final Template IfaceMethodrefSelection =
        new Template("IfaceMethodrefSelection",
                     IfaceMethodrefSelectionNoOverride,
                     /* Case 27: Objectref overrides.
                      *
                      * I[](*) = mref
                      * C[I](res) = oref = expected
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.methodref).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                         final int I = builder.methodref;
                         final int C = builder.addClass(withDef);
                         builder.hier.addInherit(C, I);
                         builder.objectref = C;
                         builder.expected = C;
                     },
                     /* Case 28: Diamond, methodref at top, overriding.
                      *
                      * I3[](*) = mref
                      * I1[I3](), I2[I3](res) = expected
                      * C[I1,I2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                         final int I3 = builder.methodref;
                         final int I2 = builder.addInterface(withDef);
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C, I1);
                         builder.hier.addInherit(C, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.objectref = C;
                         builder.expected = I2;
                     },
                     /* Case 29: Diamond, with superclass, expected at top,
                      * class overriding.
                      *
                      * I2[](*) = mref
                      * C2[I2](res) = expected, I1[I2]()
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                         builder.expected = C2;
                     },
                     /* Case 30: Diamond, with superclass, expected at top,
                      * interface overriding
                      *
                      * I2[](*) = mref
                      * C2[I2](), I1[I2](res) = expected
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(withDef);
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                         builder.expected = I1;
                     },
                     /* Case 31: Y, with superclass, overlaping, expected
                      * at top, class overrides
                      *
                      * C2[](res) = expected, I1[](*) = mref
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                         final int I1 = builder.methodref;
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.objectref = C1;
                         builder.expected = C2;
                     },
                     /* Case 32: Diamond, with superclass, overlaping, expected
                      * at top, class overrides
                      *
                      * I2[](def) = old expected
                      * C2[I2](res) = expected, I1[](*) = mref
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                         final int I2 = builder.expected;
                         final int I1 = builder.methodref;
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                         builder.expected = C2;
                     },
                     /* Case 33: Superclass overrides.
                      *
                      * I[](*) = mref
                      * C2[I](res) = expected
                      * C1[C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData withDef =
                            new ClassData(pck, oldexpected.methoddata);
                         final int I = builder.methodref;
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(C2, I);
                         builder.expected = C2;
                         builder.objectref = C1;
                     },
                     /* Case 34: Diamond, inherit through superclass,
                      * expected at top, override.
                      *
                      * I3[](*) = mref
                      * I1[I3](), I2[I3](res) = expected
                      * C2[I1,I2]()
                      * C1[C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final ClassData withDef =
                             new ClassData(pck, oldexpected.methoddata);
                         final int I3 = builder.methodref;
                         final int I2 = builder.addInterface(withDef);
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I1);
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(I1, I3);
                         builder.hier.addInherit(I2, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.objectref = C1;
                         builder.expected = I2;
                     },
                     /* Case 35: Y, with superclass, inherit through
                      * superclass, overlapping, expected at top.
                      *
                      * C3[](res) = expected, I1[*](*) = mref
                      * C2[I1,C3]()
                      * C1[C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final ClassData withDef =
                             new ClassData(pck, oldexpected.methoddata);
                         final int I1 = builder.methodref;
                         final int C3 = builder.addClass(withDef);
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I1);
                         builder.hier.addInherit(C2, C3);
                         builder.hier.addInherit(C1, C2);
                         builder.objectref = C1;
                         builder.expected = C3;
                     },
                     /* Case 36: Diamond, with superclass, inherit through
                      * superclass, overlapping, expected at top.
                      *
                      * I2[](*) = oldexpected
                      * C3[I2](res) = expected, I1[*](*) = mref
                      * C2[I1,C3]()
                      * C1[C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final ClassData withDef =
                             new ClassData(pck, oldexpected.methoddata);
                         final int I2 = builder.expected;
                         final int I1 = builder.methodref;
                         final int C3 = builder.addClass(withDef);
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I1);
                         builder.hier.addInherit(C2, C3);
                         builder.hier.addInherit(C3, I2);
                         builder.hier.addInherit(C1, C2);
                         builder.objectref = C1;
                         builder.expected = C3;
                     },
                     /* Case 37: Double diamond, with superclass, inherit through
                      * superclass, overriding.
                      *
                      * I3[](def) = old expected
                      * C3[I3](), I2[*](*) = mref
                      * C2[I2,C3](), I1[I2](res) = expected
                      * C1[C2,I1]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final ClassData withDef =
                             new ClassData(pck, oldexpected.methoddata);
                         final int I3 = builder.expected;
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(withDef);
                         final int C3 = builder.addClass(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(C2, C3);
                         builder.hier.addInherit(C3, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C1, I1);
                         builder.objectref = C1;
                         builder.expected = I1;
                     },
                     /* Case 38: Double diamond, with superclass, inherit through
                      * superclass, skip private.
                      *
                      * I3[](def) = old expected
                      * C3[I3](), I2[*](*) = mref
                      * C2[I2,C3](), I1[I2](priv)
                      * C1[C2,I1]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final ClassData withDef =
                             new ClassData(pck, oldexpected.methoddata);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PRIVATE,
                                            MethodData.Context.INSTANCE);
                         final ClassData withPrivDef =
                             new ClassData(pck, meth);
                         final int I3 = builder.expected;
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(withPrivDef);
                         final int C3 = builder.addClass(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(C2, C3);
                         builder.hier.addInherit(C3, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C1, I1);
                         builder.objectref = C1;
                     },
                     /* Case 39: Double diamond, with superclass, inherit through
                      * superclass, skip static.
                      *
                      * I3[](def) = old expected
                      * C3[I3](), I2[*](*) = mref
                      * C2[I2,C3](), I1[I2](stat)
                      * C1[C2,I1]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final ClassData withDef =
                             new ClassData(pck, oldexpected.methoddata);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef =
                             new ClassData(pck, meth);
                         final int I3 = builder.expected;
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(withStatDef);
                         final int C3 = builder.addClass(emptyClass(pck));
                         final int C2 = builder.addClass(emptyClass(pck));
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C2, I2);
                         builder.hier.addInherit(C2, C3);
                         builder.hier.addInherit(C3, I3);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C1, I1);
                         builder.objectref = C1;
                     },
                     /* Case 40: Superclass overrides.
                      *
                      * I[](*) = mref
                      * C3[I](res) = expected
                      * C2[C3](stat) = expected
                      * C1[C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                         final ClassData withDef =
                             new ClassData(pck, oldexpected.methoddata);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PUBLIC,
                                            MethodData.Context.STATIC);
                         final ClassData withStatDef =
                             new ClassData(pck, meth);
                         final int I = builder.methodref;
                         final int C3 = builder.addClass(withDef);
                         final int C2 = builder.addClass(withStatDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(C2, C3);
                         builder.hier.addInherit(C2, I);
                         builder.expected = C3;
                         builder.objectref = C1;
                     });

    // NOTE: The selection changes in JVMS 11 mean that private class methods
    // are never selected to satisfy an interface method invocation, and so no
    // IllegalAccessError is subsequently thrown. Because of this all the
    // "private" cases below are commented out. At some point in the future
    // these should be factored out and moved to a test that expects success
    // but it is not that simple as the commented out cases result in 2600
    // testcases being excluded, but only ~150 failing cases were seen. Though
    // it is not clear from the test if a single failure can result in further
    // testcases being skipped.
    public static final Template IfaceMethodrefSelectionOverrideNonPublic =
        new Template("IfaceMethodrefSelection",
                     /* Case 1: Objectref overrides.
                      *
                      * I[](*) = mref
                      * C[I](priv) = oref = expected
                      */
                     // (final SelectionResolutionTestCase.Builder builder) -> {
                     //     final ClassData.Package pck =
                     //         builder.classdata.get(builder.methodref).packageId;
                     //     final ClassData oldexpected =
                     //         builder.classdata.get(builder.expected);
                     //     final MethodData meth =
                     //         new MethodData(MethodData.Access.PRIVATE,
                     //                        MethodData.Context.INSTANCE);
                     //     final ClassData withDef =
                     //         new ClassData(pck, meth);
                     //     final int I = builder.methodref;
                     //     final int C = builder.addClass(withDef);
                     //     builder.hier.addInherit(C, I);
                     //     builder.objectref = C;
                     //     builder.expected = C;
                     // },
                     /* Case 2: Objectref overrides.
                      *
                      * I[](*) = mref
                      * C[I](prot) = oref = expected
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.methodref).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PROTECTED,
                                            MethodData.Context.INSTANCE);
                         final ClassData withDef =
                             new ClassData(pck, meth);
                         final int I = builder.methodref;
                         final int C = builder.addClass(withDef);
                         builder.hier.addInherit(C, I);
                         builder.objectref = C;
                         builder.expected = C;
                     },
                     /* Case 3: Objectref overrides package private.
                      *
                      * I[](*) = mref
                      * C[I](pack) = oref = expected
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.methodref).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PACKAGE,
                                            MethodData.Context.INSTANCE);
                         final ClassData withDef =
                             new ClassData(pck, meth);
                         final int I = builder.methodref;
                         final int C = builder.addClass(withDef);
                         builder.hier.addInherit(C, I);
                         builder.objectref = C;
                         builder.expected = C;
                     },
                     /* Case 4: Diamond, with superclass, expected at top,
                      * class overriding with private.
                      *
                      * I2[](*) = mref
                      * C2[I2](priv) = expected, I1[I2]()
                      * C1[I1,C2]() = oref
                      */
                     // (final SelectionResolutionTestCase.Builder builder) -> {
                     //     final ClassData.Package pck =
                     //         builder.classdata.get(builder.expected).packageId;
                     //     final ClassData oldexpected =
                     //         builder.classdata.get(builder.expected);
                     //     final MethodData meth =
                     //         new MethodData(MethodData.Access.PRIVATE,
                     //                        MethodData.Context.INSTANCE);
                     //     final ClassData withDef =
                     //         new ClassData(pck, meth);
                     //     final int I2 = builder.methodref;
                     //     final int I1 = builder.addInterface(emptyClass(pck));
                     //     final int C2 = builder.addClass(withDef);
                     //     final int C1 = builder.addClass(emptyClass(pck));
                     //     builder.hier.addInherit(C1, I1);
                     //     builder.hier.addInherit(C1, C2);
                     //     builder.hier.addInherit(I1, I2);
                     //     builder.hier.addInherit(C2, I2);
                     //     builder.objectref = C1;
                     //     builder.expected = C2;
                     // },
                     /* Case 5: Diamond, with superclass, expected at top,
                      * class overriding with package private.
                      *
                      * I2[](*) = mref
                      * C2[I2](pack) = expected, I1[I2]()
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PACKAGE,
                                            MethodData.Context.INSTANCE);
                         final ClassData withDef =
                             new ClassData(pck, meth);
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                         builder.expected = C2;
                     },
                     /* Case 6: Diamond, with superclass, expected at top,
                      * class overriding with protected.
                      *
                      * I2[](*) = mref
                      * C2[I2](prot) = expected, I1[I2]()
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PROTECTED,
                                            MethodData.Context.INSTANCE);
                         final ClassData withDef =
                             new ClassData(pck, meth);
                         final int I2 = builder.methodref;
                         final int I1 = builder.addInterface(emptyClass(pck));
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(I1, I2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                         builder.expected = C2;
                     },
                     /* Case 7: Y, with superclass, overlaping, expected
                      * at top, class overrides
                      *
                      * C2[](priv) = expected, I1[](*) = mref
                      * C1[I1,C2]() = oref
                      */
                     // (final SelectionResolutionTestCase.Builder builder) -> {
                     //     final ClassData.Package pck =
                     //         builder.classdata.get(builder.expected).packageId;
                     //    final ClassData oldexpected =
                     //        builder.classdata.get(builder.expected);
                     //     final MethodData meth =
                     //         new MethodData(MethodData.Access.PRIVATE,
                     //                        MethodData.Context.INSTANCE);
                     //    final ClassData withDef =
                     //        new ClassData(pck, meth);
                     //     final int I1 = builder.methodref;
                     //     final int C2 = builder.addClass(withDef);
                     //     final int C1 = builder.addClass(emptyClass(pck));
                     //     builder.hier.addInherit(C1, I1);
                     //     builder.hier.addInherit(C1, C2);
                     //     builder.objectref = C1;
                     //     builder.expected = C2;
                     // },
                     /* Case 8: Y, with superclass, overlaping, expected
                      * at top, class overrides
                      *
                      * C2[](prot) = expected, I1[](*) = mref
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PROTECTED,
                                            MethodData.Context.INSTANCE);
                        final ClassData withDef =
                            new ClassData(pck, meth);
                         final int I1 = builder.methodref;
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.objectref = C1;
                         builder.expected = C2;
                     },
                     /* Case 9: Y, with superclass, overlaping, expected
                      * at top, class overrides
                      *
                      * C2[](pack) = expected, I1[](*) = mref
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PACKAGE,
                                            MethodData.Context.INSTANCE);
                         final ClassData withDef =
                             new ClassData(pck, meth);
                         final int I1 = builder.methodref;
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.objectref = C1;
                         builder.expected = C2;
                     },
                     /* Case 10: Diamond, with superclass, overlaping, expected
                      * at top, class overrides
                      *
                      * I2[](def) = old expected
                      * C2[I2](priv) = expected, I1[](*) = mref
                      * C1[I1,C2]() = oref
                      */
                     // (final SelectionResolutionTestCase.Builder builder) -> {
                     //     final ClassData.Package pck =
                     //         builder.classdata.get(builder.expected).packageId;
                     //     final ClassData oldexpected =
                     //         builder.classdata.get(builder.expected);
                     //     final MethodData meth =
                     //         new MethodData(MethodData.Access.PRIVATE,
                     //                        MethodData.Context.INSTANCE);
                     //     final ClassData withDef =
                     //         new ClassData(pck, meth);
                     //     final int I2 = builder.expected;
                     //     final int I1 = builder.methodref;
                     //     final int C2 = builder.addClass(withDef);
                     //     final int C1 = builder.addClass(emptyClass(pck));
                     //     builder.hier.addInherit(C1, I1);
                     //     builder.hier.addInherit(C1, C2);
                     //     builder.hier.addInherit(C2, I2);
                     //     builder.objectref = C1;
                     //     builder.expected = C2;
                     // },
                     /* Case 11: Diamond, with superclass, overlaping, expected
                      * at top, class overrides
                      *
                      * I2[](def) = old expected
                      * C2[I2](pack) = expected, I1[](*) = mref
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PACKAGE,
                                            MethodData.Context.INSTANCE);
                         final ClassData withDef =
                             new ClassData(pck, meth);
                         final int I2 = builder.expected;
                         final int I1 = builder.methodref;
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                         builder.expected = C2;
                     },
                     /* Case 12: Diamond, with superclass, overlaping, expected
                      * at top, class overrides
                      *
                      * I2[](def) = old expected
                      * C2[I2](prot) = expected, I1[](*) = mref
                      * C1[I1,C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PROTECTED,
                                            MethodData.Context.INSTANCE);
                         final ClassData withDef =
                             new ClassData(pck, meth);
                         final int I2 = builder.expected;
                         final int I1 = builder.methodref;
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I1);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(C2, I2);
                         builder.objectref = C1;
                         builder.expected = C2;
                     },
                     /* Case 13: Superclass overrides.
                      *
                      * I[](*) = mref
                      * C2[I](priv) = expected
                      * C1[C2]() = oref
                      */
                     // (final SelectionResolutionTestCase.Builder builder) -> {
                     //     final ClassData.Package pck =
                     //         builder.classdata.get(builder.expected).packageId;
                     //     final ClassData oldexpected =
                     //         builder.classdata.get(builder.expected);
                     //     final MethodData meth =
                     //         new MethodData(MethodData.Access.PRIVATE,
                     //                        MethodData.Context.INSTANCE);
                     //     final ClassData withDef =
                     //         new ClassData(pck, meth);
                     //     final int I = builder.methodref;
                     //     final int C2 = builder.addClass(withDef);
                     //     final int C1 = builder.addClass(emptyClass(pck));
                     //     builder.hier.addInherit(C1, I);
                     //     builder.hier.addInherit(C1, C2);
                     //     builder.hier.addInherit(C2, I);
                     //     builder.expected = C2;
                     //     builder.objectref = C1;
                     // },
                     /* Case 14: Superclass overrides.
                      *
                      * I[](*) = mref
                      * C2[I](prot) = expected
                      * C1[C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PROTECTED,
                                            MethodData.Context.INSTANCE);
                         final ClassData withDef =
                             new ClassData(pck, meth);
                         final int I = builder.methodref;
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(C2, I);
                         builder.expected = C2;
                         builder.objectref = C1;
                     },
                     /* Case 15: Superclass overrides.
                      *
                      * I[](*) = mref
                      * C2[I](pack) = expected
                      * C1[C2]() = oref
                      */
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         final ClassData.Package pck =
                             builder.classdata.get(builder.expected).packageId;
                         final ClassData oldexpected =
                             builder.classdata.get(builder.expected);
                         final MethodData meth =
                             new MethodData(MethodData.Access.PACKAGE,
                                            MethodData.Context.INSTANCE);
                         final ClassData withDef =
                             new ClassData(pck, meth);
                         final int I = builder.methodref;
                         final int C2 = builder.addClass(withDef);
                         final int C1 = builder.addClass(emptyClass(pck));
                         builder.hier.addInherit(C1, I);
                         builder.hier.addInherit(C1, C2);
                         builder.hier.addInherit(C2, I);
                         builder.expected = C2;
                         builder.objectref = C1;
                     });

    /***********************
     * Ambiguous selection *
     ***********************/

    public static final Template MethodrefAmbiguousResolvedIsIface =
        new Template("MethodrefAmbiguousResolvedIsIface",
                    /* Inherit from interface.
                     *
                     * C2[](*) = mref, I[](any)
                     * C1[C2,I]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData.Package pck = oldexpected.packageId;
                        final MethodData.Context ctx = oldexpected.methoddata.context;
                        final MethodData mdata =
                            new MethodData(MethodData.Access.PUBLIC, ctx);
                        final ClassData withDef = new ClassData(pck, mdata);
                        final int C2 = builder.methodref;
                        final int C1 = builder.addClass(emptyClass(pck));
                        final int I = builder.addInterface(withDef);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I);
                        builder.objectref = C1;
                    });

    public static final Template IfaceMethodrefAmbiguousResolvedIsIface =
        new Template("IfaceMethodrefAmbiguousResolvedIsIface",
                    /* Inherit from interface.
                     *
                     * I1[](*) = mref, I2[](any)
                     * C1[I1,I2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData.Package pck = oldexpected.packageId;
                        final MethodData.Context ctx = oldexpected.methoddata.context;
                        final MethodData mdata =
                            new MethodData(MethodData.Access.PUBLIC, ctx);
                        final ClassData withDef = new ClassData(pck, mdata);
                        final int I1 = builder.methodref;
                        final int C = builder.addClass(emptyClass(pck));
                        final int I2 = builder.addInterface(withDef);
                        builder.hier.addInherit(C, I1);
                        builder.hier.addInherit(C, I2);
                        builder.objectref = C;
                    });

    public static final Template InvokespecialAmbiguousResolvedIsIface =
        new Template("InvokespecialAmbiguousResolvedIsIface",
                    /* Inherit from interface.
                     *
                     * C2[](*) = csite, I[](any)
                     * C1[C2,I]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData oldexpected =
                            builder.classdata.get(builder.expected);
                        final ClassData.Package pck = oldexpected.packageId;
                        final MethodData.Context ctx = oldexpected.methoddata.context;
                        final MethodData mdata =
                            new MethodData(MethodData.Access.PUBLIC, ctx);
                        final ClassData withDef = new ClassData(pck, mdata);
                        final int C2 = builder.callsite;
                        final int C1 = builder.addClass(emptyClass(pck));
                        final int I = builder.addInterface(withDef);
                        builder.hier.addInherit(C1, C2);
                        builder.hier.addInherit(C1, I);
                        builder.objectref = C1;
                    });

    /******************************
     *   invokespecial Templates  *
     ******************************/

    // Create this by taking MethodrefSelection and replacing
    // methodref with callsite.
    public static final Template ObjectrefAssignableToCallsite =
        new Template("ObjectrefAssignableToCallsite",
                    /* Case 1: Objectref equals callsite
                     *
                     * C[](*) = csite = oref
                     */
                    (builder) -> {
                        builder.objectref = builder.callsite;
                    },
                    /* Case 2: Inherit from super.
                     *
                     * C2[](*) = csite
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.callsite).packageId;
                        final int C2 = builder.callsite;
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    });

    public static final Template ObjectrefExactSubclassOfCallsite =
        new Template("ObjectrefSubclassOfCallsite",
                    /* Inherit from super.
                     *
                     * C2[](*) = csite
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.callsite).packageId;
                        final int C2 = builder.callsite;
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    });

    public static final Template ObjectrefEqualsOrExactSubclassOfCallsite =
        new Template("ObjectrefEqualsOrExactSubclassOfCallsite",
                     (final SelectionResolutionTestCase.Builder builder) -> {
                         builder.objectref = builder.callsite;
                     },
                    /* Inherit from super.
                     *
                     * C2[](*) = csite
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.callsite).packageId;
                        final int C2 = builder.callsite;
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    });

    public static final Template ObjectrefEqualsCallsite =
        new Template("TrivialObjectref",
                     Collections.singleton((builder) -> {
                             builder.objectref = builder.callsite;
                         }));

    public static final Template ObjectrefSubclassOfSubclassOfCallsite =
        new Template("ObjectrefSubclassOfCallsite",
                    /* Inherit from super.
                     *
                     * C3[](*) = csite
                     * C2[C3]()
                     * C1[C2]() = oref
                     */
                    (final SelectionResolutionTestCase.Builder builder) -> {
                        final ClassData.Package pck =
                            builder.classdata.get(builder.callsite).packageId;
                        final int C3 = builder.callsite;
                        final int C2 = builder.addClass(emptyClass(pck));
                        final int C1 = builder.addClass(emptyClass(pck));
                        builder.hier.addInherit(C2, C3);
                        builder.hier.addInherit(C1, C2);
                        builder.objectref = C1;
                    });

    private static class Placeholder extends ClassData {
        private final String placeholder;


        private Placeholder(final String placeholder,
                            final MethodData methoddata) {
            super(ClassData.Package.PLACEHOLDER, methoddata);
            this.placeholder = placeholder;
        }

        private Placeholder(final String placeholder) {
            this(placeholder, null);
        }

        public String toString() {
            return " = <Placeholder for " + placeholder + ">\n\n";
        }

        public static final Placeholder objectref = new Placeholder("objectref");
        public static final Placeholder methodref = new Placeholder("methodref");
        public static final Placeholder callsite = new Placeholder("callsite");
        public static final Placeholder expected =
            new Placeholder("expected",
                            new MethodData(MethodData.Access.PLACEHOLDER,
                                           MethodData.Context.PLACEHOLDER));
    }

    public static void main(String... args) {

        System.err.println("*** Resolution Templates ***\n");
        final SelectionResolutionTestCase.Builder withExpectedIface =
            new SelectionResolutionTestCase.Builder();
        withExpectedIface.expected =
            withExpectedIface.addInterface(Placeholder.expected);
        final SelectionResolutionTestCase.Builder withExpectedClass =
            new SelectionResolutionTestCase.Builder();
        withExpectedClass.expected =
            withExpectedClass.addClass(Placeholder.expected);

        MethodrefNotEqualsExpectedClass.printCases(withExpectedClass);
        MethodrefNotEqualsExpectedIface.printCases(withExpectedIface);
        IfaceMethodrefNotEqualsExpected.printCases(withExpectedIface);
        MethodrefAmbiguous.printCases(withExpectedIface);
        IfaceMethodrefAmbiguous.printCases(withExpectedIface);
        ReabstractExpectedIface.printCases(withExpectedIface);
        ReabstractExpectedClass.printCases(withExpectedClass);

        final SelectionResolutionTestCase.Builder methodrefExpectedIface =
            withExpectedIface.copy();
        methodrefExpectedIface.methodref =
            methodrefExpectedIface.addClass(Placeholder.methodref);
        final SelectionResolutionTestCase.Builder methodrefExpectedClass =
            withExpectedClass.copy();
        methodrefExpectedClass.methodref =
            methodrefExpectedClass.addClass(Placeholder.methodref);
        final SelectionResolutionTestCase.Builder ifaceMethodref =
            withExpectedIface.copy();
        ifaceMethodref.methodref =
            ifaceMethodref.addInterface(Placeholder.methodref);

        IgnoredAbstract.printCases(methodrefExpectedIface);
        MethodrefSelectionResolvedIsClass.printCases(methodrefExpectedClass);
        MethodrefSelectionResolvedIsIface.printCases(methodrefExpectedIface);
        IfaceMethodrefSelection.printCases(ifaceMethodref);
        IfaceMethodrefSelectionOverrideNonPublic.printCases(ifaceMethodref);

    }

}
