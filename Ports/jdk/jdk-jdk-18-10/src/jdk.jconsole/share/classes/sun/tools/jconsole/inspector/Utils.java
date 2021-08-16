/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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

package sun.tools.jconsole.inspector;

import java.awt.event.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ExecutionException;
import javax.management.*;
import javax.management.openmbean.*;
import javax.swing.*;
import javax.swing.text.*;

public class Utils {

    private Utils() {
    }
    private static Set<Integer> tableNavigationKeys =
            new HashSet<Integer>(Arrays.asList(new Integer[]{
        KeyEvent.VK_TAB, KeyEvent.VK_ENTER,
        KeyEvent.VK_HOME, KeyEvent.VK_END,
        KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
        KeyEvent.VK_UP, KeyEvent.VK_DOWN,
        KeyEvent.VK_PAGE_UP, KeyEvent.VK_PAGE_DOWN
    }));
    private static final Set<Class<?>> primitiveWrappers =
            new HashSet<Class<?>>(Arrays.asList(new Class<?>[]{
        Byte.class, Short.class, Integer.class, Long.class,
        Float.class, Double.class, Character.class, Boolean.class
    }));
    private static final Set<Class<?>> primitives = new HashSet<Class<?>>();
    private static final Map<String, Class<?>> primitiveMap =
            new HashMap<String, Class<?>>();
    private static final Map<String, Class<?>> primitiveToWrapper =
            new HashMap<String, Class<?>>();
    private static final Set<String> editableTypes = new HashSet<String>();
    private static final Set<Class<?>> extraEditableClasses =
            new HashSet<Class<?>>(Arrays.asList(new Class<?>[]{
        BigDecimal.class, BigInteger.class, Number.class,
        String.class, ObjectName.class
    }));
    private static final Set<String> numericalTypes = new HashSet<String>();
    private static final Set<String> extraNumericalTypes =
            new HashSet<String>(Arrays.asList(new String[]{
        BigDecimal.class.getName(), BigInteger.class.getName(),
        Number.class.getName()
    }));
    private static final Set<String> booleanTypes =
            new HashSet<String>(Arrays.asList(new String[]{
        Boolean.TYPE.getName(), Boolean.class.getName()
    }));

    static {
        // compute primitives/primitiveMap/primitiveToWrapper
        for (Class<?> c : primitiveWrappers) {
            try {
                Field f = c.getField("TYPE");
                Class<?> p = (Class<?>) f.get(null);
                primitives.add(p);
                primitiveMap.put(p.getName(), p);
                primitiveToWrapper.put(p.getName(), c);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
        // compute editableTypes
        for (Class<?> c : primitives) {
            editableTypes.add(c.getName());
        }
        for (Class<?> c : primitiveWrappers) {
            editableTypes.add(c.getName());
        }
        for (Class<?> c : extraEditableClasses) {
            editableTypes.add(c.getName());
        }
        // compute numericalTypes
        for (Class<?> c : primitives) {
            String name = c.getName();
            if (!name.equals(Boolean.TYPE.getName())) {
                numericalTypes.add(name);
            }
        }
        for (Class<?> c : primitiveWrappers) {
            String name = c.getName();
            if (!name.equals(Boolean.class.getName())) {
                numericalTypes.add(name);
            }
        }
    }

    /**
     * This method returns the class matching the name className.
     * It's used to cater for the primitive types.
     */
    public static Class<?> getClass(String className)
            throws ClassNotFoundException {
        Class<?> c;
        if ((c = primitiveMap.get(className)) != null) {
            return c;
        }
        return Class.forName(className);
    }

    /**
     * Check if the given collection is a uniform collection of the given type.
     */
    public static boolean isUniformCollection(Collection<?> c, Class<?> e) {
        if (e == null) {
            throw new IllegalArgumentException("Null reference type");
        }
        if (c == null) {
            throw new IllegalArgumentException("Null collection");
        }
        if (c.isEmpty()) {
            return false;
        }
        for (Object o : c) {
            if (o == null || !e.isAssignableFrom(o.getClass())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the given element denotes a supported array-friendly data
     * structure, i.e. a data structure jconsole can render as an array.
     */
    public static boolean canBeRenderedAsArray(Object elem) {
        if (isSupportedArray(elem)) {
            return true;
        }
        if (elem instanceof Collection) {
            Collection<?> c = (Collection<?>) elem;
            if (c.isEmpty()) {
                // Empty collections of any Java type are not handled as arrays
                //
                return false;
            } else {
                // - Collections of CompositeData/TabularData are not handled
                //   as arrays
                // - Collections of other Java types are handled as arrays
                //
                return !isUniformCollection(c, CompositeData.class) &&
                        !isUniformCollection(c, TabularData.class);
            }
        }
        if (elem instanceof Map) {
            return !(elem instanceof TabularData);
        }
        return false;
    }

    /**
     * Check if the given element is an array.
     *
     * Multidimensional arrays are not supported.
     *
     * Non-empty 1-dimensional arrays of CompositeData
     * and TabularData are not handled as arrays but as
     * tabular data.
     */
    public static boolean isSupportedArray(Object elem) {
        if (elem == null || !elem.getClass().isArray()) {
            return false;
        }
        Class<?> ct = elem.getClass().getComponentType();
        if (ct.isArray()) {
            return false;
        }
        if (Array.getLength(elem) > 0 &&
                (CompositeData.class.isAssignableFrom(ct) ||
                TabularData.class.isAssignableFrom(ct))) {
            return false;
        }
        return true;
    }

    /**
     * This method provides a readable classname if it's an array,
     * i.e. either the classname of the component type for arrays
     * of java reference types or the name of the primitive type
     * for arrays of java primitive types. Otherwise, it returns null.
     */
    public static String getArrayClassName(String name) {
        String className = null;
        if (name.startsWith("[")) {
            int index = name.lastIndexOf('[');
            className = name.substring(index, name.length());
            if (className.startsWith("[L")) {
                className = className.substring(2, className.length() - 1);
            } else {
                try {
                    Class<?> c = Class.forName(className);
                    className = c.getComponentType().getName();
                } catch (ClassNotFoundException e) {
                    // Should not happen
                    throw new IllegalArgumentException(
                            "Bad class name " + name, e);
                }
            }
        }
        return className;
    }

    /**
     * This methods provides a readable classname. If the supplied name
     * parameter denotes an array this method returns either the classname
     * of the component type for arrays of java reference types or the name
     * of the primitive type for arrays of java primitive types followed by
     * n-times "[]" where 'n' denotes the arity of the array. Otherwise, if
     * the supplied name doesn't denote an array it returns the same classname.
     */
    public static String getReadableClassName(String name) {
        String className = getArrayClassName(name);
        if (className == null) {
            return name;
        }
        int index = name.lastIndexOf('[');
        StringBuilder brackets = new StringBuilder(className);
        for (int i = 0; i <= index; i++) {
            brackets.append("[]");
        }
        return brackets.toString();
    }

    /**
     * This method tells whether the type is editable
     * (means can be created with a String or not)
     */
    public static boolean isEditableType(String type) {
        return editableTypes.contains(type);
    }

    /**
     * This method inserts a default value for the standard java types,
     * else it inserts the text name of the expected class type.
     * It acts to give a clue as to the input type.
     */
    public static String getDefaultValue(String type) {
        if (numericalTypes.contains(type) ||
                extraNumericalTypes.contains(type)) {
            return "0";
        }
        if (booleanTypes.contains(type)) {
            return "true";
        }
        type = getReadableClassName(type);
        int i = type.lastIndexOf('.');
        if (i > 0) {
            return type.substring(i + 1, type.length());
        } else {
            return type;
        }
    }

    /**
     * Try to create a Java object using a one-string-param constructor.
     */
    public static Object newStringConstructor(String type, String param)
            throws Exception {
        Constructor<?> c = Utils.getClass(type).getConstructor(String.class);
        try {
            return c.newInstance(param);
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            if (t instanceof Exception) {
                throw (Exception) t;
            } else {
                throw e;
            }
        }
    }

    /**
     * Try to convert a string value into a numerical value.
     */
    private static Number createNumberFromStringValue(String value)
            throws NumberFormatException {
        final String suffix = value.substring(value.length() - 1);
        if ("L".equalsIgnoreCase(suffix)) {
            return Long.valueOf(value.substring(0, value.length() - 1));
        }
        if ("F".equalsIgnoreCase(suffix)) {
            return Float.valueOf(value.substring(0, value.length() - 1));
        }
        if ("D".equalsIgnoreCase(suffix)) {
            return Double.valueOf(value.substring(0, value.length() - 1));
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
        // OK: Ignore exception...
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e1) {
        // OK: Ignore exception...
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e2) {
        // OK: Ignore exception...
        }
        throw new NumberFormatException("Cannot convert string value '" +
                value + "' into a numerical value");
    }

    /**
     * This method attempts to create an object of the given "type"
     * using the "value" parameter.
     * e.g. calling createObjectFromString("java.lang.Integer", "10")
     * will return an Integer object initialized to 10.
     */
    public static Object createObjectFromString(String type, String value)
            throws Exception {
        Object result;
        if (primitiveToWrapper.containsKey(type)) {
            if (type.equals(Character.TYPE.getName())) {
                result = value.charAt(0);
            } else {
                result = newStringConstructor(
                        ((Class<?>) primitiveToWrapper.get(type)).getName(),
                        value);
            }
        } else if (type.equals(Character.class.getName())) {
            result = value.charAt(0);
        } else if (Number.class.isAssignableFrom(Utils.getClass(type))) {
            result = createNumberFromStringValue(value);
        } else if (value == null || value.equals("null")) {
            // hack for null value
            result = null;
        } else {
            // try to create a Java object using
            // the one-string-param constructor
            result = newStringConstructor(type, value);
        }
        return result;
    }

    /**
     * This method is responsible for converting the inputs given by the user
     * into a useful object array for passing into a parameter array.
     */
    public static Object[] getParameters(XTextField[] inputs, String[] params)
            throws Exception {
        Object result[] = new Object[inputs.length];
        Object userInput;
        for (int i = 0; i < inputs.length; i++) {
            userInput = inputs[i].getValue();
            // if it's already a complex object, use the value
            // else try to instantiate with string constructor
            if (userInput instanceof XObject) {
                result[i] = ((XObject) userInput).getObject();
            } else {
                result[i] = createObjectFromString(params[i].toString(),
                        (String) userInput);
            }
        }
        return result;
    }

    /**
     * If the exception is wrapped, unwrap it.
     */
    public static Throwable getActualException(Throwable e) {
        if (e instanceof ExecutionException) {
            e = e.getCause();
        }
        if (e instanceof MBeanException ||
                e instanceof RuntimeMBeanException ||
                e instanceof RuntimeOperationsException ||
                e instanceof ReflectionException) {
            Throwable t = e.getCause();
            if (t != null) {
                return t;
            }
        }
        return e;
    }

    @SuppressWarnings("serial")
    public static class ReadOnlyTableCellEditor
            extends DefaultCellEditor {

        public ReadOnlyTableCellEditor(JTextField tf) {
            super(tf);
            tf.addFocusListener(new Utils.EditFocusAdapter(this));
            tf.addKeyListener(new Utils.CopyKeyAdapter());
        }
    }

    public static class EditFocusAdapter extends FocusAdapter {

        private CellEditor editor;

        public EditFocusAdapter(CellEditor editor) {
            this.editor = editor;
        }

        @Override
        public void focusLost(FocusEvent e) {
            editor.stopCellEditing();
        }
    }

    public static class CopyKeyAdapter extends KeyAdapter {
        private static final String defaultEditorKitCopyActionName =
                DefaultEditorKit.copyAction;
        private static final String transferHandlerCopyActionName =
                (String) TransferHandler.getCopyAction().getValue(Action.NAME);
        @Override
        public void keyPressed(KeyEvent e) {
            // Accept "copy" key strokes
            KeyStroke ks = KeyStroke.getKeyStroke(
                    e.getKeyCode(), e.getModifiersEx());
            JComponent comp = (JComponent) e.getSource();
            for (int i = 0; i < 3; i++) {
                InputMap im = comp.getInputMap(i);
                Object key = im.get(ks);
                if (defaultEditorKitCopyActionName.equals(key) ||
                        transferHandlerCopyActionName.equals(key)) {
                    return;
                }
            }
            // Accept JTable navigation key strokes
            if (!tableNavigationKeys.contains(e.getKeyCode())) {
                e.consume();
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
            e.consume();
        }
    }
}
