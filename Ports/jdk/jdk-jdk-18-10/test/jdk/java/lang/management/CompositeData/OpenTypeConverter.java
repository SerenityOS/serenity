/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     5024531
 * @summary Utility class to convert a struct-like class to a CompositeData.
 * @author Mandy Chung
 */

import java.lang.reflect.*;
import java.util.*;
import javax.management.*;
import javax.management.openmbean.*;
import static javax.management.openmbean.SimpleType.*;

/**
 * A converter utiltiy class to automatically convert a given
 * class to a CompositeType.
 */
public class OpenTypeConverter {
    private static final WeakHashMap<Class,OpenType> convertedTypes =
        new WeakHashMap<Class,OpenType>();
    private static final OpenType[] simpleTypes = {
        BIGDECIMAL, BIGINTEGER, BOOLEAN, BYTE, CHARACTER, DATE,
        DOUBLE, FLOAT, INTEGER, LONG, OBJECTNAME, SHORT, STRING,
        VOID,
    };

    static {
        for (int i = 0; i < simpleTypes.length; i++) {
            final OpenType t = simpleTypes[i];
            Class c;
            try {
                c = Class.forName(t.getClassName(), false,
                                  String.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                // the classes that these predefined types declare must exist!
                assert(false);
                c = null; // not reached
            }
            convertedTypes.put(c, t);

            if (c.getName().startsWith("java.lang.")) {
                try {
                    final Field typeField = c.getField("TYPE");
                    final Class primitiveType = (Class) typeField.get(null);
                    convertedTypes.put(primitiveType, t);
                } catch (NoSuchFieldException e) {
                    // OK: must not be a primitive wrapper
                } catch (IllegalAccessException e) {
                    // Should not reach here
                    throw new AssertionError(e);
                }
            }
        }
    }

    private static class InProgress extends OpenType {
        private static final String description =
                  "Marker to detect recursive type use -- internal use only!";

        InProgress() throws OpenDataException {
            super("java.lang.String", "java.lang.String", description);
        }

        public String toString() {
            return description;
        }

        public int hashCode() {
            return 0;
        }

        public boolean equals(Object o) {
            return false;
        }

        public boolean isValue(Object o) {
            return false;
        }
    }
    private static final OpenType inProgress;
    static {
        OpenType t;
        try {
            t = new InProgress();
        } catch (OpenDataException e) {
            // Should not reach here
            throw new AssertionError(e);
        }
        inProgress = t;
    }

    // Convert a class to an OpenType
    public static synchronized OpenType toOpenType(Class c)
            throws OpenDataException {

        OpenType t;

        t = convertedTypes.get(c);
        if (t != null) {
            if (t instanceof InProgress)
                throw new OpenDataException("Recursive data structure");
            return t;
        }

        convertedTypes.put(c, inProgress);

        if (Enum.class.isAssignableFrom(c))
            t = STRING;
        else if (c.isArray())
            t = makeArrayType(c);
        else
            t = makeCompositeType(c);

        convertedTypes.put(c, t);

        return t;
    }

    private static OpenType makeArrayType(Class c) throws OpenDataException {
        int dim;
        for (dim = 0; c.isArray(); dim++)
            c = c.getComponentType();
        return new ArrayType(dim, toOpenType(c));
    }

    private static OpenType makeCompositeType(Class c)
            throws OpenDataException {
        // Make a CompositeData containing all the getters
        final Method[] methods = c.getMethods();
        final List<String> names = new ArrayList<String>();
        final List<OpenType> types = new ArrayList<OpenType>();

        /* Select public methods that look like "T getX()" or "boolean
           isX() or hasX()", where T is not void and X is not the empty
           string.  Exclude "Class getClass()" inherited from Object.  */
        for (int i = 0; i < methods.length; i++) {
            final Method method = methods[i];
            final String name = method.getName();
            final Class type = method.getReturnType();
            final String rest;
            if (name.startsWith("get"))
                rest = name.substring(3);
            else if (name.startsWith("is") && type == boolean.class)
                rest = name.substring(2);
            else if (name.startsWith("has") && type == boolean.class)
                rest = name.substring(3);
            else
                continue;

            if (rest.equals("") || method.getParameterTypes().length > 0
                || type == void.class || rest.equals("Class"))
                continue;

            names.add(decapitalize(rest));
            types.add(toOpenType(type));
        }

        final String[] nameArray = names.toArray(new String[0]);
        return new CompositeType(c.getName(),
                                 c.getName(),
                                 nameArray, // field names
                                 nameArray, // field descriptions
                                 types.toArray(new OpenType[0]));
    }

    /**
     * Utility method to take a string and convert it to normal Java variable
     * name capitalization.  This normally means converting the first
     * character from upper case to lower case, but in the (unusual) special
     * case when there is more than one character and both the first and
     * second characters are upper case, we leave it alone.
     * <p>
     * Thus "FooBah" becomes "fooBah" and "X" becomes "x", but "URL" stays
     * as "URL".
     *
     * @param  name The string to be decapitalized.
     * @return  The decapitalized version of the string.
     */
    private static String decapitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                        Character.isUpperCase(name.charAt(0))){
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

}
