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

import java.util.StringTokenizer;

import org.netbeans.jemmy.operators.Operator.DefaultStringComparator;
import org.netbeans.jemmy.operators.Operator.StringComparator;

/**
 *
 * Implementation of org.netbeans.jemmy.ComponentChooser interface. Class can be
 * used to find component by its field/methods values converted to String.<br>
 *
 * Example:
 * <pre>
 *            JLabel label = JLabelOperator.findJLabel(frm0, new StringPropChooser("getText=JLabel",
 *                                                                                 false, true));
 * </pre>
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class StringPropChooser extends PropChooser {

    private StringComparator comparator;

    /**
     * Constructs a StringPropChooser object.
     *
     * @param propNames Names of methods/fields
     * @param params Parameters values for methods. <BR>
     * @param classes Parameters classes.
     * @param results Objects to compare converted to String method/field values
     * to.
     * @param comparator Defines string comparision criteria.
     */
    public StringPropChooser(String[] propNames,
            Object[][] params,
            Class<?>[][] classes,
            String[] results,
            StringComparator comparator) {
        super(propNames, params, classes, results);
        this.comparator = comparator;
    }

    /**
     * Constructs a StringPropChooser object.
     *
     * @param propNames Names of methods/fields
     * @param params Parameters values for methods. <BR>
     * @param classes Parameters classes.
     * @param results Objects to compare converted to String method/field values
     * to.
     * @param ce Compare exactly.<BR>
     * If true, compare exactly (<value>.toString().equals(<result>)) <BR>
     * If false, compare as substring (<value>.toString().indexOf(<result>) !=
     * -1)
     * @param ccs Compare case sensitive. <BR>
     * if false convert both <value>.toString() and <result> to uppercase before
     * comparison.
     */
    public StringPropChooser(String[] propNames,
            Object[][] params,
            Class<?>[][] classes,
            String[] results,
            boolean ce,
            boolean ccs) {
        this(propNames, params, classes, results, new DefaultStringComparator(ce, ccs));
    }

    /**
     * Constructs a StringPropChooser object.
     *
     * @param propNames Names of methods/fields
     * @param results Objects to compare converted to String method/field values
     * to.
     * @param comparator Defines string comparision criteria.
     */
    public StringPropChooser(String[] propNames,
            String[] results,
            StringComparator comparator) {
        this(propNames, (Object[][]) null, (Class<?>[][]) null, results, comparator);
    }

    /**
     * Constructs a StringPropChooser object.
     *
     * @param propNames Names of methods/fields
     * @param results Objects to compare converted to String method/field values
     * to.
     * @param ce Compare exactly.
     * @param ccs Compare case sensitive.
     * @deprecated Use constructors with {@code StringComparator}
     * parameters.
     */
    @Deprecated
    public StringPropChooser(String[] propNames,
            String[] results,
            boolean ce,
            boolean ccs) {
        this(propNames, (Object[][]) null, (Class<?>[][]) null, results, ce, ccs);
    }

    /**
     * Constructs a StringPropChooser object.
     *
     * @param props Method/field names && values <BR>
     * Like "getText=button;isVisible=true"
     * @param semicolonChar Method(field) names separator.
     * @param equalChar Method(field) name - expected value separator.
     * @param params Parameters values for methods.
     * @param classes Parameters classes.
     * @param comparator Defines string comparision criteria.
     */
    public StringPropChooser(String props,
            String semicolonChar,
            String equalChar,
            Object[][] params,
            Class<?>[][] classes,
            StringComparator comparator) {
        this(cutToArray(props, semicolonChar, equalChar, true), params, classes,
                cutToArray(props, semicolonChar, equalChar, false), comparator);
    }

    /**
     * Constructs a StringPropChooser object.
     *
     * @param props Method/field names && values <BR>
     * Like "getText=button;isVisible=true"
     * @param semicolonChar Method(field) names separator.
     * @param equalChar Method(field) name - expected value separator.
     * @param params Parameters values for methods.
     * @param classes Parameters classes.
     * @param ce Compare exactly.
     * @param ccs Compare case sensitive.
     * @deprecated Use constructors with {@code StringComparator}
     * parameters.
     */
    @Deprecated
    public StringPropChooser(String props,
            String semicolonChar,
            String equalChar,
            Object[][] params,
            Class<?>[][] classes,
            boolean ce,
            boolean ccs) {
        this(cutToArray(props, semicolonChar, equalChar, true), params, classes,
                cutToArray(props, semicolonChar, equalChar, false), ce, ccs);
    }

    /**
     * Constructs a StringPropChooser object.
     *
     * @param props Method/field names && values
     * @param semicolonChar Method(field) names separator.
     * @param equalChar Method(field) name - expected value separator.
     * @param comparator Defines string comparision criteria.
     */
    public StringPropChooser(String props,
            String semicolonChar,
            String equalChar,
            StringComparator comparator) {
        this(props, semicolonChar, equalChar, (Object[][]) null, (Class<?>[][]) null, comparator);
    }

    /**
     * Constructs a StringPropChooser object.
     *
     * @param props Method/field names && values
     * @param semicolonChar Method(field) names separator.
     * @param equalChar Method(field) name - expected value separator.
     * @param ce Compare exactly.
     * @param ccs Compare case sensitive.
     * @deprecated Use constructors with {@code StringComparator}
     * parameters.
     */
    @Deprecated
    public StringPropChooser(String props,
            String semicolonChar,
            String equalChar,
            boolean ce,
            boolean ccs) {
        this(props, semicolonChar, equalChar, (Object[][]) null, (Class<?>[][]) null, ce, ccs);
    }

    /**
     * Constructs a StringPropChooser object.
     *
     * @param props Method/field names && values <BR>
     * ";" is used as a method(field) names separator. <BR>
     * "=" is used as a method(field) name - expected value separator.
     * @param params Parameters values for methods.
     * @param classes Parameters classes.
     * @param comparator Defines string comparision criteria.
     */
    public StringPropChooser(String props,
            Object[][] params,
            Class<?>[][] classes,
            StringComparator comparator) {
        this(props, ";", "=", params, classes, comparator);
    }

    /**
     * Constructs a StringPropChooser object.
     *
     * @param props Method/field names && values <BR>
     * ";" is used as a method(field) names separator. <BR>
     * "=" is used as a method(field) name - expected value separator.
     * @param params Parameters values for methods.
     * @param classes Parameters classes.
     * @param ce Compare exactly.
     * @param ccs Compare case sensitive.
     * @deprecated Use constructors with {@code StringComparator}
     * parameters.
     */
    @Deprecated
    public StringPropChooser(String props,
            Object[][] params,
            Class<?>[][] classes,
            boolean ce,
            boolean ccs) {
        this(props, ";", "=", params, classes, ce, ccs);
    }

    /**
     * Constructs a StringPropChooser object.
     *
     * @param props Method/field names && values ";" is used as a method(field)
     * names separator. <BR>
     * "=" is used as a method(field) name - expected value separator.
     * @param comparator Defines string comparision criteria.
     */
    public StringPropChooser(String props,
            StringComparator comparator) {
        this(props, (Object[][]) null, (Class<?>[][]) null, comparator);
    }

    /**
     * Constructs a StringPropChooser object.
     *
     * @param props Method/field names && values ";" is used as a method(field)
     * names separator. <BR>
     * "=" is used as a method(field) name - expected value separator.
     * @param ce Compare exactly.
     * @param ccs Compare case sensitive.
     * @deprecated Use constructors with {@code StringComparator}
     * parameters.
     */
    @Deprecated
    public StringPropChooser(String props,
            boolean ce,
            boolean ccs) {
        this(props, (Object[][]) null, (Class<?>[][]) null, ce, ccs);
    }

    /**
     * @see org.netbeans.jemmy.ComponentChooser
     */
    @Override
    public String getDescription() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < propNames.length; i++) {
            if (result.length() > 0) {
                result.append(';');
            }
            result.append(propNames[i]).append('=').append((String) results[i]);
        }
        return "Component by properties array\n    : " + result;
    }

    @Override
    public String toString() {
        return "StringPropChooser{description = " + getDescription() + ", comparator=" + comparator + '}';
    }

    /**
     * Method to check property. Compares "value".toString() to (String)etalon
     * according ce and ccs constructor parameters.
     *
     * @param value Method/field value
     * @param etalon Object to compare to.
     * @return true if the value matches the etalon.
     */
    @Override
    protected boolean checkProperty(Object value, Object etalon) {
        return comparator.equals(value.toString(), (String) etalon);
    }

    /*split string to array*/
    private static String[] cutToArray(String resources, String semicolon, String equal, boolean names) {
        StringTokenizer token = new StringTokenizer(resources, semicolon);
        String[] props = new String[token.countTokens()];
        String nextProp;
        int ind = 0;
        while (token.hasMoreTokens()) {
            nextProp = token.nextToken();
            StringTokenizer subtoken = new StringTokenizer(nextProp, equal);
            if (subtoken.countTokens() == 2) {
                props[ind] = subtoken.nextToken();
                if (!names) {
                    props[ind] = subtoken.nextToken();
                }
            } else {
                props[ind] = null;
            }
            ind++;
        }
        return props;
    }

}
