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

import java.beans.BeanInfo;
import java.beans.BeanProperty;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;

import java.util.Arrays;


/**
 * @test
 * @bug 8132703 8132163 8132732 8132973 8154756 8132888 8155103
 * @summary Some check for BeanProperty annotation
 * @author a.stepanov
 * @run main BeanPropertyTest
 */
public class BeanPropertyTest {

    private final static String  DESCRIPTION = "TEST";
    private final static boolean BOUND       = true;
    private final static boolean EXPERT      = false;
    private final static boolean HIDDEN      = true;
    private final static boolean PREFERRED   = false;
    private final static boolean REQUIRED    = true;
    private final static boolean UPDATE      = false;
    private final static String
        V_NAME  = "javax.swing.SwingConstants.TOP",
        V_SHORT = "TOP",
        V = Integer.toString(javax.swing.SwingConstants.TOP);
    private final static int X = javax.swing.SwingConstants.TOP;

    private final static String DESCRIPTION_2 = "XYZ";


    // ---------- test cases ----------

    public static class G01 {

        private final static String TESTCASE = "arbitrary getter name";

        private final int x = X;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public int get1() { return x; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class S01 {

        private final static String TESTCASE = "arbitrary setter name";

        private int x;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public void setXXXXX(int v) { x = v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // JDK-8132703
    public static class G02 {

        private final static String TESTCASE = "arbitrary getter name";

        private final int x = X;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public int get() { return x; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // JDK-8132703
    public static class S02 {

        private final static String TESTCASE = "arbitrary setter name";

        private int x;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public void set(int v) { x = v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // JDK-8132703
    public static class G03 {

        private final static String TESTCASE = "arbitrary getter name";

        private final int x = X;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public int GetX() { return x; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // JDK-8132703
    public static class S03 {

        private final static String TESTCASE = "arbitrary setter name";

        private int x;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public void SetX(int v) { x = v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // JDK-8132163
    public static class G04 {

        private final static String TESTCASE = "arbitrary getter return type";

        private final int x = X;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public Object getX() { return x; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // JDK-8132163
    public static class S04 {

        private final static String TESTCASE = "arbitrary setter argument type";

        private int x;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public void setX(short v) { x = v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class G05 {

        private final static String TESTCASE =
            "annotated getter + arbitrary setter argument type";

        private int x;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public int getX() { return x; }
        public void setX(short v) { x = v; }


        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // JDK-8132163
    public static class S05 {

        private final static String TESTCASE =
            "annotated setter + arbitrary getter return type";

        private int x;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public void setX(int v) { x = v; }
        public Object getX() { return x; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class G06 {

        private final static String TESTCASE = "indexed getter";

        private final int x[] = {X, X, X};

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public int getX(int i) { return x[i]; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class S06 {

        private final static String TESTCASE = "indexed setter";

        private final int x[] = {X, X, X};

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public void setX(int i, int v) { x[i] = v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class G07 {

        private final static String TESTCASE =
            "indexed (annotated) + non-indexed getters";

        private final int x[] = {X, X, X};

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public int getX(int i) { return x[i]; }

        public int[] getX() { return x; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class S07 {

        private final static String TESTCASE =
            "indexed (annotated) + non-indexed setters";

        private int x[] = new int[3];

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public void setX(int i, int v) { x[i] = v; }

        public void setX(int a[]) { x = Arrays.copyOf(a, a.length); }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // JDK-8132732
    public static class G08 {

        private final static String TESTCASE =
            "non-indexed (annotated) + indexed getters";

        private final int x[] = {X, X, X};

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public int[] getX() { return x; }

        public int getX(int i) { return x[i]; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // JDK-8132732
    public static class S08 {

        private final static String TESTCASE =
            "non-indexed (annotated) + indexed setters";

        private int x[] = new int[3];

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public void setX(int a[]) { x = Arrays.copyOf(a, a.length); }

        public void setX(int i, int v) { x[i] = v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // JDK-8132732
    public static class G09 {

        private final static String TESTCASE = "two annotated getters";

        private final int x[] = {X, X, X};

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public int[] getX() { return x; }

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        public int getX(int i) { return x[i]; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // JDK-8132732
    public static class S09 {

        private final static String TESTCASE = "two annotated setters";

        private int x[] = new int[3];

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public void setX(int a[]) { x = Arrays.copyOf(a, a.length); }

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        public void setX(int i, int v) { x[i] = v; }


        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class G10 {

        private final static String TESTCASE =
            "getter + similarly named field";

        public int prop, Prop, setProp, getProp;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public int getProp() { return X; }
        public void setProp(int v) { prop = Prop = setProp = getProp = v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class S10 {

        private final static String TESTCASE =
            "setter + similarly named field";

        public int prop, Prop, setProp, getProp;

        private int x;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public int getProp() { return x; }
        public void setProp(int v) { x = v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class G11 {

        private final static String TESTCASE =
            "getter + similarly named field of other type";

        public Object prop, Prop, setProp, getProp;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public int getProp() { return X; }
        public void setProp(int v) { prop = Prop = setProp = getProp = v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class S11 {

        private final static String TESTCASE =
            "setter + similarly named field of other type";

        public String prop, Prop, setProp, getProp;

        private int x;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public int getProp() { return x; }
        public void setProp(int v) { x = v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // JDK-8132163
    public static class G12 {

        private final static String TESTCASE =
            "getter having wrapper class return type";

        private final int x = X;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public Integer getProp() { return x; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // JDK-8132163
    public static class S12 {

        private final static String TESTCASE =
            "setter with wrapper class argument type";

        private int x;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public void setX(Integer v) { x = v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class G13 {

        private final static String TESTCASE =
            "getter + overloading methods";

        private final int x = X;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public int getX() { return x; }
        public int getX(boolean arg) { return (arg ? x : 0); }
        public int getX(int ... dummy) { return 0; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // JDK-8154756
    public static class S13 {

        private final static String TESTCASE =
            "setter + overloading methods";

        private int x;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public void setX(int v) { x = v; }
        public int  setX() { return (x = X); }
        public void setX(int ... dummy) {}
        private void setX(Object ... dummy) {}


        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }


    // JDK-8132888
    public static class G14 {

        private final static String TESTCASE = "non-public getter";

        private final int x = X;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        int getX() { return x; } // getter is not public

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // JDK-8132888
    public static class S14 {

        private final static String TESTCASE = "non-public setter";

        private int x;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        void setX(int v) { x = v; } // setter is not public

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class getX {

        private final static String TESTCASE =
            "class name coincides with getter name";

        private int x;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public int  getX() { return x; }
        public void setX(int v) { x = v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class setX {

        private final static String TESTCASE =
            "class name coincides with setter name";

        private int x;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public void setX(int v) { x = v; }
        public int  getX() { return x; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // JDK-8132973
    public static class GS {

        private final static String TESTCASE =
            "both getter and setter are annotated";

        private int x;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public int getX() { return x; }

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        public void setX(int v) { x = v; }


        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class Self {

        private final static String TESTCASE = "trivial singleton";

        private static Self instance = null;
        private Self() {}

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE)
        public Self getSelf() {
            if (instance == null) { instance = new Self(); }
            return instance;
        }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class SelfArr {

        private final static String TESTCASE = "trivial singleton + array";

        private static SelfArr arr[] = null;
        private SelfArr() {}

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE)
        public SelfArr[] getSelfArr() {
            if (arr == null) { arr = new SelfArr[]{new SelfArr(), new SelfArr()}; }
            return arr;
        }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public enum E {

        ONE,
        TWO {
            @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
            public void setX(int v) {}

            public void addPropertyChangeListener(PropertyChangeListener l)    {}
            public void removePropertyChangeListener(PropertyChangeListener l) {}
        };

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public void setX(int v) {}

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}

    }

    private enum EB {

        TRUE(true), FALSE(false);

        private boolean b;
        private EB(boolean v) { b = v; }

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE)
        public boolean isTrue() { return true; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}

    }


    // ---------- checks ----------

    private static boolean check(String what, boolean v, boolean ref) {

        boolean ok = (v == ref);
        if (!ok) { System.out.println(
            "invalid " + what + ": " + v + ", expected: " + ref); }
        return ok;
    }

    private static boolean checkInfo(BeanInfo i, boolean checkVals) {

        System.out.println("checking info...");

        PropertyDescriptor descriptors[] = i.getPropertyDescriptors();
        int nd = descriptors.length;
        if (nd != 1) {
            System.out.println("invalid number of descriptors: " + nd);
            return false;
        }

        PropertyDescriptor d = descriptors[0];

        String descr = d.getShortDescription();
        boolean ok = descr.equals(DESCRIPTION);
        if (!ok) { System.out.println("invalid description: " + descr +
                ", expected: " + DESCRIPTION); }

        ok &= check("isBound",  d.isBound(),  BOUND);
        ok &= check("isExpert", d.isExpert(), EXPERT);
        ok &= check("isHidden", d.isHidden(), HIDDEN);
        ok &= check("isPreferred", d.isPreferred(), PREFERRED);
        ok &= check("required", (boolean) d.getValue("required"), REQUIRED);
        ok &= check("visualUpdate",
            (boolean) d.getValue("visualUpdate"), UPDATE);

        if (!checkVals) { return ok; }

        Object vals[] = (Object[]) d.getValue("enumerationValues");
        if (vals == null) {
            System.out.println("null enumerationValues");
            return false;
        }

        boolean okVals = (
            (vals.length == 3) &&
             vals[0].toString().equals(V_SHORT) &&
             vals[1].toString().equals(V)       &&
             vals[2].toString().equals(V_NAME));

        if (!okVals) { System.out.println("invalid enumerationValues"); }

        return (ok && okVals);
    }

    private static boolean checkAlternativeInfo(BeanInfo i) {

        System.out.println("checking alternative info...");

        PropertyDescriptor descriptors[] = i.getPropertyDescriptors();
        int nd = descriptors.length;
        if (nd != 1) {
            System.out.println("invalid number of descriptors: " + nd);
            return false;
        }

        PropertyDescriptor d = descriptors[0];

        String descr = d.getShortDescription();
        boolean ok = descr.equals(DESCRIPTION_2);
        if (!ok) { System.out.println("invalid alternative description: " +
            descr + ", expected: " + DESCRIPTION_2); }

        ok &= check("isBound",  d.isBound(),  !BOUND);
        ok &= check("isExpert", d.isExpert(), !EXPERT);
        ok &= check("isHidden", d.isHidden(), !HIDDEN);
        ok &= check("isPreferred", d.isPreferred(), !PREFERRED);
        ok &= check("required", (boolean) d.getValue("required"), !REQUIRED);
        ok &= check("visualUpdate",
            (boolean) d.getValue("visualUpdate"), !UPDATE);

        Object vals[] = (Object[]) d.getValue("enumerationValues");
        if (vals != null || vals.length > 0) {
            System.out.println("non-null enumerationValues");
            return false;
        }

        return ok;
    }


    private static boolean checkAlternative(Class<?> c) {
        return (
            c.equals(G09.class) ||
            c.equals(S09.class) ||
            c.equals(GS.class));
    }

    private static boolean ignoreVals(Class<?> c) {
        return (c.equals(Self.class) || c.equals(SelfArr.class)) || c.equals(EB.class);
    }


    // ---------- run test ----------

    public static void main(String[] args) throws Exception {

        Class<?> cases[] = {

            G01.class, S01.class,
            // G02.class, S02.class, // TODO: please update after 8132703 fix
            // G03.class, S03.class, // TODO: please update after 8132703 fix
            // G04.class, S04.class, // TODO: please update after 8132163 fix
            G05.class, // S05.class, // TODO: please update after 8132163 fix
            G06.class, S06.class,
            G07.class, S07.class,
            // G08.class, S08.class, // TODO: please update after 8132732 fix
            // G09.class, S09.class, // TODO: please update after 8132732 fix
            G10.class, S10.class,
            G11.class, S11.class,
            // G12.class, S12.class, // TODO: please update after 8132163 fix
            G13.class, // S13.class, // TODO: please update after 8154756 fix
            // G14.class, S14.class, // TODO: please update after 8132888 fix or
                                     // remove these cases if it is not an issue
            GS.class,
            getX.class, setX.class,
            Self.class, SelfArr.class
        };

        boolean passed = true;

        for (Class<?> c: cases) {

            java.lang.reflect.Field f = c.getDeclaredField("TESTCASE");
            f.setAccessible(true);
            String descr = f.get(c).toString();

            System.out.println("\n" + c.getSimpleName() + " (" + descr + "):");
            BeanInfo i;
            try { i = Introspector.getBeanInfo(c, Object.class); }
            catch (IntrospectionException e) { throw new RuntimeException(e); }
            boolean ok = checkInfo(i, !ignoreVals(c));
            if (checkAlternative(c)) {
                ok |= checkAlternativeInfo(i);
            }
            System.out.println(ok ? "OK" : "NOK");
            passed = passed && ok;
        }

        // enums

        Class<?> enumCases[] = {
            E.class, E.TWO.getClass(), EB.class
        };

        int ne = 1;
        for (Class<?> c: enumCases) {

            System.out.println("\nEnum-" + ne + ":");
            ne++;

            BeanInfo i;
            try { i = Introspector.getBeanInfo(c, Enum.class); }
            catch (IntrospectionException e) { throw new RuntimeException(e); }
            boolean ok = checkInfo(i, !ignoreVals(c));
            System.out.println(ok ? "OK" : "NOK");
            passed = passed && ok;
        }


        if (!passed) { throw new RuntimeException("test failed"); }
        System.out.println("\ntest passed");
    }
}
