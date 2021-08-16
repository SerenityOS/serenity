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
 * @bug 8132565 8155013
 * @summary Some inheritance check for BeanProperty annotation
 * @author a.stepanov
 * @run main InheritanceBeanPropertyTest
 */


public class InheritanceBeanPropertyTest {

    private final static String  DESCRIPTION = "TEST";
    private final static boolean BOUND       = true;
    private final static boolean EXPERT      = false;
    private final static boolean HIDDEN      = true;
    private final static boolean PREFERRED   = false;
    private final static boolean REQUIRED    = true;
    private final static boolean UPDATE      = false;

    private final static double X = java.lang.Math.PI;

    private final static String
        V_NAME  = "java.lang.Math.PI",
        V_SHORT = "PI",
        V = Double.toString(X);

    private final static String DESCRIPTION_2 = "XYZ";


    // ---------- test cases ----------

    public static class BaseGet {

        private final static String TESTCASE = "base getter";

        public double getX() { return X; }
    }

    public static class OverloadGet extends BaseGet {

        private final static String TESTCASE = "overload getter";

        private final double x[] = {X, X, X};

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public double getX(int i) { return x[i]; } // indexed

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // ----------

    public static class BaseSet {

        private final static String TESTCASE = "base setter";

        double u;
        public void setX(double v) { u = v; }
    }

    public static class OverloadSet extends BaseSet {

        private final static String TESTCASE = "overload setter";

        private final double x[] = {X, X, X};

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public void setX(int i, double v) { x[i] = v; } // indexed

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // ----------

    public static class BaseIGet {

        protected final double x[] = {X, X, X};
        public double[] getX() { return x; }
    }

    public static class OverloadIGet extends BaseIGet {

        private final static String TESTCASE = "overload indexed getter";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public double getX(int i) { return x[i]; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // ----------

    public static class BaseISet {

        protected double x[] = {X, X, X};
        public void setX(double a[]) { x = Arrays.copyOf(a, a.length); }
    }

    public static class OverloadISet extends BaseISet {

        private final static String TESTCASE = "overload indexed setter";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public void setX(int i, double v) { x[i] = v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // ----------

    public static class BoolGet {

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        public boolean getX() { return false; }
    }

    public static class BoolGetIs extends BoolGet {

        private final static String TESTCASE = "base boolean getter + is";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE)
        public boolean isX() { return false; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }
    // ----------

    public static class BoolIs {

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        public boolean isX() { return false; }
    }

    public static class BoolIsGet extends BoolIs {

        private final static String TESTCASE = "base is + boolean getter";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE)
        public boolean getX() { return false; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // ----------

    public static class AnnotatedGet {

        private final static String TESTCASE = "annotated super getter";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public double getX() { return 0.; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class OverrideAnnotatedGet extends AnnotatedGet {

        private final static String TESTCASE = "override annotated getter";

        @Override
        public double getX() { return X; }
    }

    // ----------

    public static class AnnotatedIs {

        private final static String TESTCASE = "annotated super is";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public boolean isX() { return false; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class OverrideAnnotatedIs extends AnnotatedIs {

        private final static String TESTCASE = "override annotated is";

        @Override
        public boolean isX() { return false; }
    }

    // ----------

    public static class AnnotatedSet {

        private final static String TESTCASE = "annotated super set";

        protected double x;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public void setX(double v) { x = -v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class OverrideAnnotatedSet extends AnnotatedSet {

        private final static String TESTCASE = "override annotated setter";

        @Override
        public void setX(double v) { x = v; }
    }

    // ----------

    public static class AnnotatedGet2 {

        protected double x;

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        public double getX() { return 0.; }
    }

    public static class OverrideAnnotatedGet2 extends AnnotatedGet2 {

        private final static String TESTCASE = "override annotated getter - 2";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        @Override
        public double getX() { return X; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class AnnotatedGet2Ext extends AnnotatedGet2 {

        private final static String TESTCASE = "extend annotated getter - 2";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public void setX(double v) { x = v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // ----------

    public static class AnnotatedIs2 {

        protected boolean b = false;

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        public boolean isX() { return false; }
    }

    public static class OverrideAnnotatedIs2 extends AnnotatedIs2 {

        private final static String TESTCASE = "override annotated is - 2";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE)
        @Override
        public boolean isX() { return b; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class AnnotatedIs2Ext extends AnnotatedIs2 {

        private final static String TESTCASE = "extend annotated is - 2";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE)
        public void setX(boolean v) { b = v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // ----------

    public static class AnnotatedSet2 {

        protected double x;

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        public void setX(double v) { x = -v; }
    }

    public static class OverrideAnnotatedSet2 extends AnnotatedSet2 {

        private final static String TESTCASE = "override annotated setter - 2";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        @Override
        public void setX(double v) { x = v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static class AnnotatedSet2Ext extends AnnotatedSet2 {

        private final static String TESTCASE = "extend annotated setter - 2";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public double getX() { return x; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // ----------

    public abstract static class AbstractGet {

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        public abstract double getX();
    }

    public static class OverrideAbstractGet extends AbstractGet {

        private final static String TESTCASE =
            "override abstract annotated getter";

        @Override
        public double getX() { return X; }
    }

    public static class OverrideAbstractGet2 extends AbstractGet {

        private final static String TESTCASE =
            "override abstract annotated getter - 2";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        @Override
        public double getX() { return X; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public abstract static class AbstractGetExt extends AbstractGet {

        private final static String TESTCASE =
            "extend abstract annotated getter";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public abstract void setX(double v);

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // ----------

    public abstract static class AbstractIs {

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        public abstract boolean isX();
    }

    public static class OverrideAbstractIs extends AbstractIs {

        private final static String TESTCASE =
            "override abstract annotated is";

        @Override
        public boolean isX() { return true; }
    }

    public static class OverrideAbstractIs2 extends AbstractIs {

        private final static String TESTCASE =
            "override abstract annotated is - 2";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE)
        @Override
        public boolean isX() { return true; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }


    public abstract static class AbstractIsExt extends AbstractIs {

        private final static String TESTCASE =
            "extend abstract annotated is";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE)
        public abstract boolean getX();

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // ----------

    public abstract static class AbstractSet {

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        public abstract void setX(double v);
    }

    public static class OverrideAbstractSet extends AbstractSet {

        private final static String TESTCASE =
            "override abstract annotated setter";

        private double x;

        @Override
        public void setX(double v) { x = v; }
    }

    public static class OverrideAbstractSet2 extends AbstractSet {

        private final static String TESTCASE =
            "override abstract annotated setter - 2";

        private double x;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        @Override
        public void setX(double v) { x = v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public abstract static class AbstractSetExt extends AbstractSet {

        private final static String TESTCASE =
            "extend abstract annotated setter";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public abstract void setX(double v[]);

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // ----------

    public static abstract class AbstractGet2 {

        private final static String TESTCASE = "abstract super getter";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public abstract double getX();

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public static abstract class AbstractGet2Ext extends AbstractGet2 {

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        public abstract void setX(double a[]);
    }

    // ----------

    public static interface IGet {

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        double getX();
    }

    public static class IGetImpl implements IGet {

        private final static String TESTCASE = "implement getter interface";

        @Override
        public double getX() { return X; }
    }

    public static class IGetImpl2 implements IGet {

        private final static String TESTCASE = "implement getter interface - 2";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        @Override
        public double getX() { return X; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public abstract static class IGetImpl3 implements IGet {

        private final static String TESTCASE = "implement getter interface - 3";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public abstract void setX(double v);

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // ----------

    public static interface IIs {

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        public boolean isX();
    }

    public static class IIsImpl implements IIs {

        private final static String TESTCASE = "implement is interface";

        @Override
        public boolean isX() { return true; }
    }

    public static class IIsImpl2 implements IIs {

        private final static String TESTCASE = "implement is interface - 2";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE)
        @Override
        public boolean isX() { return true; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public abstract static class IIsImpl3 implements IIs {

        private final static String TESTCASE = "implement is interface - 3";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE)
        public abstract void setX(boolean v);

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // ----------

    public static interface ISet {

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        public void setX(double v);
    }

    public static class ISetImpl implements ISet {

        private final static String TESTCASE = "implement getter interface";

        private double x;

        @Override
        public void setX(double v) { x = v; }
    }

    public static class ISetImpl2 implements ISet {

        private final static String TESTCASE = "implement getter interface - 2";

        private double x;

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        @Override
        public void setX(double v) { x = v; }

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    public abstract static class ISetImpl3 implements ISet {

        private final static String TESTCASE = "implement getter interface - 3";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public abstract double getX();

        public void addPropertyChangeListener(PropertyChangeListener l)    {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // ----------

    public static interface ISet2 {

        final static String TESTCASE = "super interface - setter";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public void setX(double v);

        public void addPropertyChangeListener(PropertyChangeListener l);
        public void removePropertyChangeListener(PropertyChangeListener l);
    }

    public static class ISet2Impl implements ISet2 {

        private double x;

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        @Override
        public void setX(double v) { x = v; }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener l) {}
        @Override
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // ----------

    public static interface IGet2 {

        final static String TESTCASE = "super interface - indexed getter";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        public double[] getX();

        public void addPropertyChangeListener(PropertyChangeListener l);
        public void removePropertyChangeListener(PropertyChangeListener l);
    }

    public static class IGet2Impl implements IGet2 {

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        @Override
        public double[] getX() { return new double[]{X, X}; }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener l) {}
        @Override
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // ----------

    public static class ProtectedGet {

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        protected double getX() { return 0.; }
    }

    public static class OverrideProtectedGet extends ProtectedGet {

        final static String TESTCASE = "override protected getter";

        @BeanProperty(
            description  = DESCRIPTION,
            bound        = BOUND,
            expert       = EXPERT,
            hidden       = HIDDEN,
            preferred    = PREFERRED,
            required     = REQUIRED,
            visualUpdate = UPDATE,
            enumerationValues = {V_NAME})
        @Override
        public double getX() { return X; }

        public void addPropertyChangeListener(PropertyChangeListener l) {}
        public void removePropertyChangeListener(PropertyChangeListener l) {}
    }

    // ----------

    // static getter - see also JDK-8154938
    public static class StaticGet {

        @BeanProperty(
            description  = DESCRIPTION_2,
            bound        = !BOUND,
            expert       = !EXPERT,
            hidden       = !HIDDEN,
            preferred    = !PREFERRED,
            required     = !REQUIRED,
            visualUpdate = !UPDATE)
        public static double getProp() { return 0.; }
    }

    public static class HideStaticGet extends StaticGet {

        final static String TESTCASE = "hide static getter";

        public double getX() { return X; } // add to get the "default" info
        public static double getProp() { return X; }
    }

    // TODO: if 8154938 is considered to be a real issue,
    // create one more test case "HideStaticGet2 extends StaticGet" with an
    // annotated getter and check the correctness of the corresponding bean info

    // ---------- checks ----------

    private static boolean check(String what, boolean v, boolean ref) {

        boolean ok = (v == ref);
        if (!ok) { System.out.println(
            "invalid " + what + ": " + v + ", expected: " + ref); }
        return ok;
    }

    private static boolean checkInfo(BeanInfo i, boolean ignoreValsCheck) {

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

        if (ignoreValsCheck) { return ok; }

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

    private static boolean checkDefault(BeanInfo i) {

        System.out.println("checking default info...");

        PropertyDescriptor descriptors[] = i.getPropertyDescriptors();
        int nd = descriptors.length;
        if (nd != 1) {
            System.out.println("invalid number of descriptors: " + nd);
            return false;
        }

        PropertyDescriptor d = descriptors[0];

        String descr = d.getShortDescription();
        boolean ok = descr.equals("x");
        if (!ok) { System.out.println("invalid description: " + descr +
                ", expected: x"); }

        ok &= check("isBound",  d.isBound(),  false);
        ok &= check("isExpert", d.isExpert(), false);
        ok &= check("isHidden", d.isHidden(), false);
        ok &= check("isPreferred", d.isPreferred(), false);
        ok &= check("required", (boolean) d.getValue("required"), false);
        ok &= check("visualUpdate",
            (boolean) d.getValue("visualUpdate"), false);

        Object vals[] = (Object[]) d.getValue("enumerationValues");
        if (vals != null && vals.length > 0) {
            System.out.println("non-empty enumerationValues");
            ok = false;
        }

        return ok;
    }

    // do not check enumerationValues for these classes
    private static boolean ignoreVals(Class<?> c) {
        return (
            c.equals(BoolGetIs.class)            ||
            c.equals(BoolIsGet.class)            ||
            c.equals(AnnotatedIs.class)          ||
            c.equals(OverrideAnnotatedIs2.class) ||
            c.equals(AnnotatedIs2Ext.class)      ||
            c.equals(OverrideAbstractIs.class)   ||
            c.equals(OverrideAbstractIs2.class)  ||
            c.equals(AbstractIsExt.class)        ||
            c.equals(OverrideAbstractIs.class)   ||
            c.equals(IIsImpl.class)              ||
            c.equals(IIsImpl2.class)             ||
            c.equals(IIsImpl3.class)
        );
    }

    // default property descriptor data are expected for these classes
    private static boolean isDefault(Class<?> c) {
        return (
            c.equals(OverrideAnnotatedGet.class) ||
            c.equals(OverrideAnnotatedIs.class ) ||
            c.equals(OverrideAnnotatedSet.class) ||
            c.equals(OverrideAbstractGet.class)  ||
            c.equals(OverrideAbstractIs.class)   ||
            c.equals(OverrideAbstractSet.class)  ||
            c.equals(IGetImpl.class)             ||
            c.equals(IIsImpl.class)              ||
            c.equals(ISetImpl.class)             ||
            c.equals(BaseGet.class)              ||
            c.equals(BaseSet.class)              ||
            c.equals(HideStaticGet.class)
        );
    }


    // ---------- run test ----------

    public static void main(String[] args) throws Exception {

        Class<?>
            ic1 = ISet2Impl.class.getInterfaces()[0],
            ic2 = IGet2Impl.class.getInterfaces()[0];

        Class<?> cases[] = {

            OverloadGet.class,
            OverloadGet.class.getSuperclass(),
            OverloadSet.class,
            OverloadSet.class.getSuperclass(),
            OverloadIGet.class,
            OverloadISet.class,

            // TODO: uncomment/update after 8132565 fix
            //BoolGetIs.class,
            //BoolIsGet.class,
            //OverrideAnnotatedGet.class,
            //OverrideAnnotatedIs.class,
            //OverrideAnnotatedSet.class,
            //OverrideAnnotatedGet2.class,
            //AnnotatedGet2Ext.class,
            //OverrideAnnotatedIs2.class
            //AnnotatedIs2Ext.class,
            //OverrideAnnotatedSet2.class,
            //AnnotatedSet2Ext.class,

            OverrideAnnotatedGet.class.getSuperclass(),
            OverrideAnnotatedIs.class.getSuperclass(),
            OverrideAnnotatedSet.class.getSuperclass(),

            // TODO: uncomment/update after 8132565 fix
            //OverrideAbstractGet.class,
            //OverrideAbstractGet2.class,
            //AbstractGetExt.class,
            //OverrideAbstractIs.class,
            //OverrideAbstractIs2.class,
            //AbstractIsExt.class
            //OverrideAbstractSet.class,
            //OverrideAbstractSet2.class,
            //AbstractSetExt.class,

            AbstractGet2Ext.class.getSuperclass(),
            IGetImpl.class,
            IGetImpl2.class,
            IGetImpl3.class,
            IIsImpl.class,
            IIsImpl2.class,
            IIsImpl3.class,
            ISetImpl.class,
            ISetImpl2.class,
            ISetImpl3.class,
            ic1,
            // ic2,  // TODO: uncomment/update after 8155013 fix
            OverrideProtectedGet.class,
            HideStaticGet.class
        };

        boolean passed = true;

        for (Class<?> c: cases) {

            java.lang.reflect.Field f = c.getDeclaredField("TESTCASE");
            f.setAccessible(true);
            String descr = f.get(c).toString();

            System.out.println("\n" + c.getSimpleName() + " (" + descr + "):");
            BeanInfo i;
            try {
                i = Introspector.getBeanInfo(c,
                    (c.equals(ic1) || c.equals(ic2)) ? null : Object.class);
            }
            catch (IntrospectionException e) { throw new RuntimeException(e); }

            boolean ok;

            if (isDefault(c)) {
                ok = checkDefault(i);
            } else {
                ok = checkInfo(i, ignoreVals(c));
            }
            System.out.println(ok ? "OK" : "NOK");
            passed = passed && ok;
        }

        if (!passed) { throw new RuntimeException("test failed"); }
        System.out.println("\ntest passed");
    }
}
