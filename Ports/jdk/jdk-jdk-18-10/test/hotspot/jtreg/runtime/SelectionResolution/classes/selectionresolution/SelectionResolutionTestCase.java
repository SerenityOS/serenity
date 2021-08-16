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

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;

/**
 * One individual test case.  This class also defines a builder, which
 * can be used to build up cases.
 */
public class SelectionResolutionTestCase {

    public enum InvokeInstruction {
        INVOKESTATIC,
        INVOKESPECIAL,
        INVOKEINTERFACE,
        INVOKEVIRTUAL;
    }

    /**
     * The class data (includes interface data).
     */
    public final HashMap<Integer, ClassData> classdata;
    /**
     * The hierarchy shape.
     */
    public final HierarchyShape hier;
    /**
     * The invoke instruction to use.
     */
    public final InvokeInstruction invoke;
    /**
     * Which class is the methodref (or interface methodref).
     */
    public final int methodref;
    /**
     * Which class is the objectref.
     */
    public final int objectref;
    /**
     * Which class is the callsite (this must be a class, not an interface.
     */
    public final int callsite;
    /**
     * The expected result.
     */
    public final Result result;

    private SelectionResolutionTestCase(final HashMap<Integer, ClassData> classdata,
                                        final HierarchyShape hier,
                                        final InvokeInstruction invoke,
                                        final int methodref,
                                        final int objectref,
                                        final int callsite,
                                        final int expected) {
        this.classdata = classdata;
        this.hier = hier;
        this.invoke = invoke;
        this.methodref = methodref;
        this.objectref = objectref;
        this.callsite = callsite;
        this.result = Result.is(expected);
    }

    private SelectionResolutionTestCase(final HashMap<Integer, ClassData> classdata,
                                        final HierarchyShape hier,
                                        final InvokeInstruction invoke,
                                        final int methodref,
                                        final int objectref,
                                        final int callsite,
                                        final Result result) {
        this.classdata = classdata;
        this.hier = hier;
        this.invoke = invoke;
        this.methodref = methodref;
        this.objectref = objectref;
        this.callsite = callsite;
        this.result = result;
    }

    private static int currError = 0;

    private String dumpClasses(final ClassConstruct[] classes)
        throws Exception {
        final String errorDirName = "error_" + currError++;
        final File errorDir = new File(errorDirName);
        errorDir.mkdirs();
        for (int i = 0; i < classes.length; i++) {
            classes[i].writeClass(errorDir);
        }
        try (final FileWriter fos =
             new FileWriter(new File(errorDir, "description.txt"))) {
            fos.write(this.toString());
        }
        return errorDirName;
    }

    /**
     * Run this case, return an error message, or null.
     *
     * @return An error message, or null if the case succeeded.
     */
    public String run() {
        /* Uncomment this line to print EVERY case */
        //System.err.println("Running\n" + this);
        final ClassBuilder builder =
            new ClassBuilder(this, ClassBuilder.ExecutionMode.DIRECT);
        try {
            final ByteCodeClassLoader bcl = new ByteCodeClassLoader();
            final ClassConstruct[] classes = builder.build();

            try {
                bcl.addClasses(classes);
                bcl.loadAll();

                // Grab the callsite class.
                final Class testclass =
                    bcl.findClass(builder.getCallsiteClass().getDottedName());

                // Get the 'test' method out of it and call it.  The
                // return value tess which class that got selected.
                final java.lang.reflect.Method method =
                    testclass.getDeclaredMethod("test");
                final int actual = (Integer) method.invoke(null);
                // Check the result.
                if (!result.complyWith(actual)) {
                    final String dump = dumpClasses(classes);
                    return "Failed:\n" + this + "\nExpected " + result + " got " + actual + "\nClasses written to " + dump;
                }
            } catch (Throwable t) {
                // This catch block is handling exceptions that we
                // might expect to see.
                final Throwable actual = t.getCause();
                if (actual == null) {
                    final String dump = dumpClasses(classes);
                    System.err.println("Unexpected exception in test\n" + this + "\nClasses written to " + dump);
                    throw t;
                } else if (result == null) {
                    final String dump = dumpClasses(classes);
                    return "Failed:\n" + this + "\nUnexpected exception " + actual + "\nClasses written to " + dump;
                } else if (!result.complyWith(actual)) {
                    final String dump = dumpClasses(classes);
                    return "Failed:\n" + this + "\nExpected " + this.result + " got " + actual + "\nClasses written to " + dump;
                }
            }
        } catch(Throwable e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private static void addPackage(final StringBuilder sb,
                                  final ClassData cd) {
        switch (cd.packageId) {
        case SAME: sb.append("Same."); break;
        case DIFFERENT: sb.append("Different."); break;
        case OTHER: sb.append("Other."); break;
        case PLACEHOLDER: sb.append("_."); break;
        default: throw new RuntimeException("Impossible case");
        }
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        //sb.append("hierarchy:\n" + hier + "\n");
        sb.append("invoke:    " + invoke + "\n");
        if (methodref != -1) {
            if (hier.isClass(methodref)) {
                sb.append("methodref: C" + methodref + "\n");
            } else {
                sb.append("methodref: I" + methodref + "\n");
            }
        }
        if (objectref != -1) {
            if (hier.isClass(objectref)) {
                sb.append("objectref: C" + objectref + "\n");
            } else {
                sb.append("objectref: I" + objectref + "\n");
            }
        }
        if (callsite != -1) {
            if (hier.isClass(callsite)) {
                sb.append("callsite: C" + callsite + "\n");
            } else {
                sb.append("callsite: I" + callsite + "\n");
            }
        }
        sb.append("result: " + result + "\n");
        sb.append("classes:\n\n");

        for(int i = 0; classdata.containsKey(i); i++) {
            final ClassData cd = classdata.get(i);

            if (hier.isClass(i)) {
                sb.append("class ");
                addPackage(sb, cd);
                sb.append("C" + i);
            } else {
                sb.append("interface ");
                addPackage(sb, cd);
                sb.append("I" + i);
            }

            boolean first = true;
            for(final int j : hier.classes()) {
                if (hier.inherits(i, j)) {
                    if (first) {
                        sb.append(" extends C" + j);
                    } else {
                        sb.append(", C" + j);
                    }
                }
            }

            first = true;
            for(final int j : hier.interfaces()) {
                if (hier.inherits(i, j)) {
                    if (first) {
                        sb.append(" implements I" + j);
                    } else {
                        sb.append(", I" + j);
                    }
                }
            }

            sb.append(cd);
        }

        return sb.toString();
    }

    /**
     * A builder, facilitating building up test cases.
     */
    public static class Builder {
        /**
         * A map from class (or interface) id's to ClassDatas
         */
        public final HashMap<Integer, ClassData> classdata;
        /**
         * The hierarchy shape.
         */
        public final HierarchyShape hier;
        /**
         * Which invoke instruction to use.
         */
        public InvokeInstruction invoke;
        /**
         * The id of the methodref (or interface methodref).
         */
        public int methodref = -1;
        /**
         * The id of the object ref.  Note that for the generator
         * framework to work, this must be set to something.  If an
         * objectref isn't used, just set it to the methodref.
         */
        public int objectref = -1;
        /**
         * The id of the callsite.
         */
        public int callsite = -1;
        /**
         * The id of the expected result.  This is used to store the
         * expected resolution result.
         */
        public int expected;
        /**
         * The expected result.  This needs to be set before the final
         * test case is built.
         */
        public Result result;

        /**
         * Create an empty Builder object.
         */
        public Builder() {
            classdata = new HashMap<>();
            hier = new HierarchyShape();
        }

        private Builder(final HashMap<Integer, ClassData> classdata,
                        final HierarchyShape hier,
                        final InvokeInstruction invoke,
                        final int methodref,
                        final int objectref,
                        final int callsite,
                        final int expected,
                        final Result result) {
            this.classdata = classdata;
            this.hier = hier;
            this.invoke = invoke;
            this.methodref = methodref;
            this.objectref = objectref;
            this.callsite = callsite;
            this.expected = expected;
            this.result = result;
        }

        private Builder(final Builder other) {
            this((HashMap<Integer, ClassData>) other.classdata.clone(),
                 other.hier.copy(), other.invoke, other.methodref, other.objectref,
                 other.callsite, other.expected, other.result);
        }

        public SelectionResolutionTestCase build() {
            if (result != null) {
                return new SelectionResolutionTestCase(classdata, hier, invoke,
                                                       methodref, objectref,
                                                       callsite, result);
            } else {
                return new SelectionResolutionTestCase(classdata, hier, invoke,
                                                       methodref, objectref,
                                                       callsite, expected);
            }
        }

        /**
         * Set the expected result.
         */
        public void setResult(final Result result) {
            this.result = result;
        }

        /**
         * Add a class, and return its id.
         *
         * @return The new class' id.
         */
        public int addClass(final ClassData data) {
            final int id = hier.addClass();
            classdata.put(id, data);
            return id;
        }

        /**
         * Add an interface, and return its id.
         *
         * @return The new class' id.
         */
        public int addInterface(final ClassData data) {
            final int id = hier.addInterface();
            classdata.put(id, data);
            return id;
        }

        /**
         * Make a copy of this builder.
         */
        public Builder copy() {
            return new Builder(this);
        }

        public String toString() {
            final StringBuilder sb = new StringBuilder();
            //sb.append("hierarchy:\n" + hier + "\n");
            sb.append("invoke:    " + invoke + "\n");
            if (methodref != -1) {
                if (hier.isClass(methodref)) {
                    sb.append("methodref: C" + methodref + "\n");
                } else {
                    sb.append("methodref: I" + methodref + "\n");
                }
            }
            if (objectref != -1) {
                if (hier.isClass(objectref)) {
                    sb.append("objectref: C" + objectref + "\n");
                } else {
                    sb.append("objectref: I" + objectref + "\n");
                }
            }
            if (callsite != -1) {
                if (hier.isClass(callsite)) {
                    sb.append("callsite: C" + callsite + "\n");
                } else {
                    sb.append("callsite: I" + callsite + "\n");
                }
            }
            if (expected != -1) {
                if (hier.isClass(expected)) {
                    sb.append("expected: C" + expected + "\n");
                } else {
                    sb.append("expected: I" + expected + "\n");
                }
            }
            sb.append("result: " + result + "\n");
            sb.append("classes:\n\n");

            for(int i = 0; classdata.containsKey(i); i++) {
                final ClassData cd = classdata.get(i);

                if (hier.isClass(i)) {
                    sb.append("class ");
                    addPackage(sb, cd);
                    sb.append("C" + i);
                } else {
                    sb.append("interface ");
                    addPackage(sb, cd);
                    sb.append("I" + i);
                }

                boolean first = true;
                for(final int j : hier.classes()) {
                    if (hier.inherits(i, j)) {
                        if (first) {
                            sb.append(" extends C" + j);
                        } else {
                            sb.append(", C" + j);
                        }
                    }
                }

                first = true;
                for(final int j : hier.interfaces()) {
                    if (hier.inherits(i, j)) {
                        if (first) {
                            sb.append(" implements I" + j);
                        } else {
                            sb.append(", I" + j);
                        }
                    }
                }

                sb.append(cd);
            }

            return sb.toString();
        }
    }
}
