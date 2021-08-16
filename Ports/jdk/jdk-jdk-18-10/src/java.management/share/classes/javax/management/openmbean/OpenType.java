/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.openmbean;

import com.sun.jmx.mbeanserver.GetPropertyAction;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;

/**
 * The <code>OpenType</code> class is the parent abstract class of all classes which describe the actual <i>open type</i>
 * of open data values.
 * <p>
 * An <i>open type</i> is defined by:
 * <ul>
 *  <li>the fully qualified Java class name of the open data values this type describes;
 *      note that only a limited set of Java classes is allowed for open data values
 *      (see {@link #ALLOWED_CLASSNAMES_LIST ALLOWED_CLASSNAMES_LIST}),</li>
 *  <li>its name,</li>
 *  <li>its description.</li>
 * </ul>
 *
 * @param <T> the Java type that instances described by this type must
 * have.  For example, {@link SimpleType#INTEGER} is a {@code
 * SimpleType<Integer>} which is a subclass of {@code OpenType<Integer>},
 * meaning that an attribute, parameter, or return value that is described
 * as a {@code SimpleType.INTEGER} must have Java type
 * {@link Integer}.
 *
 * @since 1.5
 */
public abstract class OpenType<T> implements Serializable {

    /* Serial version */
    static final long serialVersionUID = -9195195325186646468L;


    /**
     * List of the fully qualified names of the Java classes allowed for open
     * data values. A multidimensional array of any one of these classes or
     * their corresponding primitive types is also an allowed class for open
     * data values.
     *
       <pre>ALLOWED_CLASSNAMES_LIST = {
        "java.lang.Void",
        "java.lang.Boolean",
        "java.lang.Character",
        "java.lang.Byte",
        "java.lang.Short",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Float",
        "java.lang.Double",
        "java.lang.String",
        "java.math.BigDecimal",
        "java.math.BigInteger",
        "java.util.Date",
        "javax.management.ObjectName",
        CompositeData.class.getName(),
        TabularData.class.getName() } ;
       </pre>
     *
     */
    public static final List<String> ALLOWED_CLASSNAMES_LIST =
      Collections.unmodifiableList(
        Arrays.asList(
          "java.lang.Void",
          "java.lang.Boolean",
          "java.lang.Character",
          "java.lang.Byte",
          "java.lang.Short",
          "java.lang.Integer",
          "java.lang.Long",
          "java.lang.Float",
          "java.lang.Double",
          "java.lang.String",
          "java.math.BigDecimal",
          "java.math.BigInteger",
          "java.util.Date",
          "javax.management.ObjectName",
          CompositeData.class.getName(),        // better refer to these two class names like this, rather than hardcoding a string,
          TabularData.class.getName()) );       // in case the package of these classes should change (who knows...)


    /**
     * @deprecated Use {@link #ALLOWED_CLASSNAMES_LIST ALLOWED_CLASSNAMES_LIST} instead.
     */
    @Deprecated
    public static final String[] ALLOWED_CLASSNAMES =
        ALLOWED_CLASSNAMES_LIST.toArray(new String[0]);


    /**
     * @serial The fully qualified Java class name of open data values this
     *         type describes.
     */
    private String className;

    /**
     * @serial The type description (should not be null or empty).
     */
    private String description;

    /**
     * @serial The name given to this type (should not be null or empty).
     */
    private String typeName;

    /**
     * Tells if this type describes an array (checked in constructor).
     */
    private transient boolean isArray = false;

    /**
     * Cached Descriptor for this OpenType, constructed on demand.
     */
    private transient Descriptor descriptor;

    /* *** Constructor *** */

    /**
     * Constructs an <code>OpenType</code> instance (actually a subclass instance as <code>OpenType</code> is abstract),
     * checking for the validity of the given parameters.
     * The validity constraints are described below for each parameter.
     * <br>&nbsp;
     * @param  className  The fully qualified Java class name of the open data values this open type describes.
     *                    The valid Java class names allowed for open data values are listed in
     *                    {@link #ALLOWED_CLASSNAMES_LIST ALLOWED_CLASSNAMES_LIST}.
     *                    A multidimensional array of any one of these classes
     *                    or their corresponding primitive types is also an allowed class,
     *                    in which case the class name follows the rules defined by the method
     *                    {@link Class#getName() getName()} of <code>java.lang.Class</code>.
     *                    For example, a 3-dimensional array of Strings has for class name
     *                    &quot;<code>[[[Ljava.lang.String;</code>&quot; (without the quotes).
     * <br>&nbsp;
     * @param  typeName  The name given to the open type this instance represents; cannot be a null or empty string.
     * <br>&nbsp;
     * @param  description  The human readable description of the open type this instance represents;
     *                      cannot be a null or empty string.
     * <br>&nbsp;
     * @throws IllegalArgumentException  if <var>className</var>, <var>typeName</var> or <var>description</var>
     *                                   is a null or empty string
     * <br>&nbsp;
     * @throws OpenDataException  if <var>className</var> is not one of the allowed Java class names for open data
     */
    protected OpenType(String  className,
                       String  typeName,
                       String  description) throws OpenDataException {
        checkClassNameOverride();
        this.typeName = valid("typeName", typeName);
        this.description = valid("description", description);
        this.className = validClassName(className);
        this.isArray = (this.className != null && this.className.startsWith("["));
    }

    /* Package-private constructor for callers we trust to get it right. */
    OpenType(String className, String typeName, String description,
             boolean isArray) {
        this.className   = valid("className",className);
        this.typeName    = valid("typeName", typeName);
        this.description = valid("description", description);
        this.isArray     = isArray;
    }

    @SuppressWarnings("removal")
    private void checkClassNameOverride() throws SecurityException {
        if (this.getClass().getClassLoader() == null)
            return;  // We trust bootstrap classes.
        if (overridesGetClassName(this.getClass())) {
            final GetPropertyAction getExtendOpenTypes =
                new GetPropertyAction("jmx.extend.open.types");
            if (AccessController.doPrivileged(getExtendOpenTypes) == null) {
                throw new SecurityException("Cannot override getClassName() " +
                        "unless -Djmx.extend.open.types");
            }
        }
    }

    @SuppressWarnings("removal")
    private static boolean overridesGetClassName(final Class<?> c) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                try {
                    return (c.getMethod("getClassName").getDeclaringClass() !=
                            OpenType.class);
                } catch (Exception e) {
                    return true;  // fail safe
                }
            }
        });
    }

    private static String validClassName(String className) throws OpenDataException {
        className   = valid("className", className);

        // Check if className describes an array class, and determines its elements' class name.
        // (eg: a 3-dimensional array of Strings has for class name: "[[[Ljava.lang.String;")
        //
        int n = 0;
        while (className.startsWith("[", n)) {
            n++;
        }
        String eltClassName; // class name of array elements
        boolean isPrimitiveArray = false;
        if (n > 0) {
            if (className.startsWith("L", n) && className.endsWith(";")) {
                // removes the n leading '[' + the 'L' characters
                // and the last ';' character
                eltClassName = className.substring(n+1, className.length()-1);
            } else if (n == className.length() - 1) {
                // removes the n leading '[' characters
                eltClassName = className.substring(n, className.length());
                isPrimitiveArray = true;
            } else {
                throw new OpenDataException("Argument className=\"" + className +
                        "\" is not a valid class name");
            }
        } else {
            // not an array
            eltClassName = className;
        }

        // Check that eltClassName's value is one of the allowed basic data types for open data
        //
        boolean ok = false;
        if (isPrimitiveArray) {
            ok = ArrayType.isPrimitiveContentType(eltClassName);
        } else {
            ok = ALLOWED_CLASSNAMES_LIST.contains(eltClassName);
        }
        if ( ! ok ) {
            throw new OpenDataException("Argument className=\""+ className +
                                        "\" is not one of the allowed Java class names for open data.");
        }

        return className;
    }

    /* Return argValue.trim() provided argValue is neither null nor empty;
       otherwise throw IllegalArgumentException.  */
    private static String valid(String argName, String argValue) {
        if (argValue == null || (argValue = argValue.trim()).isEmpty())
            throw new IllegalArgumentException("Argument " + argName +
                                               " cannot be null or empty");
        return argValue;
    }

    /* Package-private access to a Descriptor containing this OpenType. */
    synchronized Descriptor getDescriptor() {
        if (descriptor == null) {
            descriptor = new ImmutableDescriptor(new String[] {"openType"},
                                                 new Object[] {this});
        }
        return descriptor;
    }

    /* *** Open type information methods *** */

    /**
     * Returns the fully qualified Java class name of the open data values
     * this open type describes.
     * The only possible Java class names for open data values are listed in
     * {@link #ALLOWED_CLASSNAMES_LIST ALLOWED_CLASSNAMES_LIST}.
     * A multidimensional array of any one of these classes or their
     * corresponding primitive types is also an allowed class,
     * in which case the class name follows the rules defined by the method
     * {@link Class#getName() getName()} of <code>java.lang.Class</code>.
     * For example, a 3-dimensional array of Strings has for class name
     * &quot;<code>[[[Ljava.lang.String;</code>&quot; (without the quotes),
     * a 3-dimensional array of Integers has for class name
     * &quot;<code>[[[Ljava.lang.Integer;</code>&quot; (without the quotes),
     * and a 3-dimensional array of int has for class name
     * &quot;<code>[[[I</code>&quot; (without the quotes)
     *
     * @return the class name.
     */
    public String getClassName() {
        return className;
    }

    // A version of getClassName() that can only be called from within this
    // package and that cannot be overridden.
    String safeGetClassName() {
        return className;
    }

    /**
     * Returns the name of this <code>OpenType</code> instance.
     *
     * @return the type name.
     */
    public String getTypeName() {

        return typeName;
    }

    /**
     * Returns the text description of this <code>OpenType</code> instance.
     *
     * @return the description.
     */
    public String getDescription() {

        return description;
    }

    /**
     * Returns <code>true</code> if the open data values this open
     * type describes are arrays, <code>false</code> otherwise.
     *
     * @return true if this is an array type.
     */
    public boolean isArray() {

        return isArray;
    }

    /**
     * Tests whether <var>obj</var> is a value for this open type.
     *
     * @param obj the object to be tested for validity.
     *
     * @return <code>true</code> if <var>obj</var> is a value for this
     * open type, <code>false</code> otherwise.
     */
    public abstract boolean isValue(Object obj) ;

    /**
     * Tests whether values of the given type can be assigned to this open type.
     * The default implementation of this method returns true only if the
     * types are equal.
     *
     * @param ot the type to be tested.
     *
     * @return true if {@code ot} is assignable to this open type.
     */
    boolean isAssignableFrom(OpenType<?> ot) {
        return this.equals(ot);
    }

    /* *** Methods overriden from class Object *** */

    /**
     * Compares the specified <code>obj</code> parameter with this
     * open type instance for equality.
     *
     * @param obj the object to compare to.
     *
     * @return true if this object and <code>obj</code> are equal.
     */
    public abstract boolean equals(Object obj) ;

    public abstract int hashCode() ;

    /**
     * Returns a string representation of this open type instance.
     *
     * @return the string representation.
     */
    public abstract String toString() ;

    /**
     * Deserializes an {@link OpenType} from an {@link java.io.ObjectInputStream}.
     */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        checkClassNameOverride();
        ObjectInputStream.GetField fields = in.readFields();
        final String classNameField;
        final String descriptionField;
        final String typeNameField;
        try {
            classNameField =
                validClassName((String) fields.get("className", null));
            descriptionField =
                valid("description", (String) fields.get("description", null));
            typeNameField =
                valid("typeName", (String) fields.get("typeName", null));
        } catch (Exception e) {
            IOException e2 = new InvalidObjectException(e.getMessage());
            e2.initCause(e);
            throw e2;
        }
        className = classNameField;
        description = descriptionField;
        typeName = typeNameField;
        isArray = (className.startsWith("["));
    }
}
