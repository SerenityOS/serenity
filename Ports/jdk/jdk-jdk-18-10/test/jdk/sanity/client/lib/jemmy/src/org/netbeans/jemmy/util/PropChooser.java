/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package org.netbeans.jemmy.util;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;

import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;

/**
 *
 * Implementation of org.netbeans.jemmy.ComponentChooser interface. Class can be
 * used to find component by its field/methods values. <br>
 * Example:
 * <pre>
 *            String[] methods = {"getClientProperty"};
 *            Object[][] params = {{"classname"}};
 *            Class<?>[][] classes = {{Object.class}};
 *            Object[] results = {"javax.swing.JCheckBox"};
 *
 *            JCheckBox box = JCheckBoxOperator.findJCheckBox(frm0, new PropChooser(methods, params, classes, results));
 * </pre> Or:
 * <pre>
 *            String[] methods = {"getText"};
 *            Object[] results = {"Open"};
 *
 *            JButtonOperator box = new JButtonOperator(containerOperator, new PropChooser(fields, results));
 * </pre>
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class PropChooser implements ComponentChooser, Outputable {

    /**
     * Names of methods to check.
     */
    protected String[] propNames;

    /**
     * Methods parameters.
     */
    protected Object[][] params;

    /**
     * Classes of parameters.
     */
    protected Class<?>[][] classes;

    /**
     * Expected results of methods.
     */
    protected Object[] results;

    private TestOut output;

    /**
     * Constructs a PropChooser object.
     *
     * @param propNames Names of methods/fields
     * @param params Parameters values for methods. <BR>
     * params[0] is an array of parameters for propNames[0] methods. <BR>
     * If propNames[0] is a field, params[0] is ignored.
     * @param classes Parameters classes.
     * @param results Objects to compare method/field values to. <BR>
     * A value of propNames[0] method/field should be equal to results[0]
     * object.
     */
    public PropChooser(String[] propNames,
            Object[][] params,
            Class<?>[][] classes,
            Object[] results) {
        this.propNames = propNames;
        this.results = results;
        if (params != null) {
            this.params = params;
        } else {
            this.params = new Object[propNames.length][0];
        }
        if (classes != null) {
            this.classes = classes;
        } else {
            this.classes = new Class<?>[this.params.length][0];
            for (int i = 0; i < this.params.length; i++) {
                Class<?>[] clsss = new Class<?>[this.params[i].length];
                for (int j = 0; j < this.params[i].length; j++) {
                    clsss[j] = this.params[i][j].getClass();
                }
                this.classes[i] = clsss;
            }
        }
        setOutput(JemmyProperties.getCurrentOutput());
    }

    /**
     * Constructs a PropChooser object for checking of methods with no
     * parameters.
     *
     * @param propNames Names of methods/fields
     * @param results Objects to compare method/field values to.
     */
    public PropChooser(String[] propNames,
            Object[] results) {
        this(propNames, null, null, results);
    }

    @Override
    public void setOutput(TestOut output) {
        this.output = output;
    }

    @Override
    public TestOut getOutput() {
        return output;
    }

    @Override
    public boolean checkComponent(Component comp) {
        try {
            String propName = null;
            Object value;
            ClassReference disp = new ClassReference(comp);
            for (int i = 0; i < propNames.length; i++) {
                propName = propNames[i];
                if (propName != null) {
                    if (isField(comp, propName, classes[i])) {
                        try {
                            value = disp.getField(propName);
                        } catch (IllegalStateException e) {
                            output.printStackTrace(e);
                            return false;
                        } catch (NoSuchFieldException e) {
                            output.printStackTrace(e);
                            return false;
                        } catch (IllegalAccessException e) {
                            output.printStackTrace(e);
                            return false;
                        }
                    } else {
                        try {
                            value = disp.invokeMethod(propName, params[i], classes[i]);
                        } catch (InvocationTargetException e) {
                            output.printStackTrace(e);
                            return false;
                        } catch (IllegalStateException e) {
                            output.printStackTrace(e);
                            return false;
                        } catch (NoSuchMethodException e) {
                            output.printStackTrace(e);
                            return false;
                        } catch (IllegalAccessException e) {
                            output.printStackTrace(e);
                            return false;
                        }
                    }
                    if (!checkProperty(value, results[i])) {
                        return false;
                    }
                }
            }
            return true;
        } catch (SecurityException e) {
            output.printStackTrace(e);
            return false;
        }
    }

    @Override
    public String getDescription() {
        StringBuilder result = new StringBuilder();
        for (String propName : propNames) {
            result.append(' ').append(propName);
        }
        return "Component by properties array\n    :" + result.toString();
    }

    @Override
    public String toString() {
        return "PropChooser{" + "description=" + getDescription() + '}';
    }

    /**
     * Method to check one method result with an etalon. Can be overrided by a
     * subclass.
     *
     * @param value Method/field value
     * @param etalon Object to compare to.
     * @return true if the value matches the etalon.
     */
    protected boolean checkProperty(Object value, Object etalon) {
        return value.equals(etalon);
    }

    /* try to define if propName is a field or method*/
    private boolean isField(Component comp, String propName, Class<?>[] params)
            throws SecurityException {
        try {
            comp.getClass().getField(propName);
            comp.getClass().getMethod(propName, params);
        } catch (NoSuchMethodException e) {
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
        return true;
    }
}
