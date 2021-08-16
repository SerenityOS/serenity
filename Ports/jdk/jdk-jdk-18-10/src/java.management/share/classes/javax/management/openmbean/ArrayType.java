/*
 * Copyright (c) 2000, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ObjectStreamException;
import java.lang.reflect.Array;

/**
 * The {@code ArrayType} class is the <i>open type</i> class whose instances describe
 * all <i>open data</i> values which are n-dimensional arrays of <i>open data</i> values.
 * <p>
 * Examples of valid {@code ArrayType} instances are:
 * <pre>{@code
 * // 2-dimension array of java.lang.String
 * ArrayType<String[][]> a1 = new ArrayType<String[][]>(2, SimpleType.STRING);
 *
 * // 1-dimension array of int
 * ArrayType<int[]> a2 = new ArrayType<int[]>(SimpleType.INTEGER, true);
 *
 * // 1-dimension array of java.lang.Integer
 * ArrayType<Integer[]> a3 = new ArrayType<Integer[]>(SimpleType.INTEGER, false);
 *
 * // 4-dimension array of int
 * ArrayType<int[][][][]> a4 = new ArrayType<int[][][][]>(3, a2);
 *
 * // 4-dimension array of java.lang.Integer
 * ArrayType<Integer[][][][]> a5 = new ArrayType<Integer[][][][]>(3, a3);
 *
 * // 1-dimension array of java.lang.String
 * ArrayType<String[]> a6 = new ArrayType<String[]>(SimpleType.STRING, false);
 *
 * // 1-dimension array of long
 * ArrayType<long[]> a7 = new ArrayType<long[]>(SimpleType.LONG, true);
 *
 * // 1-dimension array of java.lang.Integer
 * ArrayType<Integer[]> a8 = ArrayType.getArrayType(SimpleType.INTEGER);
 *
 * // 2-dimension array of java.lang.Integer
 * ArrayType<Integer[][]> a9 = ArrayType.getArrayType(a8);
 *
 * // 2-dimension array of int
 * ArrayType<int[][]> a10 = ArrayType.getPrimitiveArrayType(int[][].class);
 *
 * // 3-dimension array of int
 * ArrayType<int[][][]> a11 = ArrayType.getArrayType(a10);
 *
 * // 1-dimension array of float
 * ArrayType<float[]> a12 = ArrayType.getPrimitiveArrayType(float[].class);
 *
 * // 2-dimension array of float
 * ArrayType<float[][]> a13 = ArrayType.getArrayType(a12);
 *
 * // 1-dimension array of javax.management.ObjectName
 * ArrayType<ObjectName[]> a14 = ArrayType.getArrayType(SimpleType.OBJECTNAME);
 *
 * // 2-dimension array of javax.management.ObjectName
 * ArrayType<ObjectName[][]> a15 = ArrayType.getArrayType(a14);
 *
 * // 3-dimension array of java.lang.String
 * ArrayType<String[][][]> a16 = new ArrayType<String[][][]>(3, SimpleType.STRING);
 *
 * // 1-dimension array of java.lang.String
 * ArrayType<String[]> a17 = new ArrayType<String[]>(1, SimpleType.STRING);
 *
 * // 2-dimension array of java.lang.String
 * ArrayType<String[][]> a18 = new ArrayType<String[][]>(1, a17);
 *
 * // 3-dimension array of java.lang.String
 * ArrayType<String[][][]> a19 = new ArrayType<String[][][]>(1, a18);
 * }</pre>
 *
 *
 * @since 1.5
 */
/*
  Generification note: we could have defined a type parameter that is the
  element type, with class ArrayType<E> extends OpenType<E[]>.  However,
  that doesn't buy us all that much.  We can't say
    public OpenType<E> getElementOpenType()
  because this ArrayType could be a multi-dimensional array.
  For example, if we had
    ArrayType(2, SimpleType.INTEGER)
  then E would have to be Integer[], while getElementOpenType() would
  return SimpleType.INTEGER, which is an OpenType<Integer>.

  Furthermore, we would like to support int[] (as well as Integer[]) as
  an Open Type (RFE 5045358).  We would want this to be an OpenType<int[]>
  which can't be expressed as <E[]> because E can't be a primitive type
  like int.
 */
public class ArrayType<T> extends OpenType<T> {

    /* Serial version */
    static final long serialVersionUID = 720504429830309770L;

    /**
     * @serial The dimension of arrays described by this {@link ArrayType}
     *         instance.
     */
    private int dimension;

    /**
     * @serial The <i>open type</i> of element values contained in the arrays
     *         described by this {@link ArrayType} instance.
     */
    private OpenType<?> elementType;

    /**
     * @serial This flag indicates whether this {@link ArrayType}
     *         describes a primitive array.
     *
     * @since 1.6
     */
    private boolean primitiveArray;

    private transient Integer  myHashCode = null;       // As this instance is immutable, these two values
    private transient String   myToString = null;       // need only be calculated once.

    // indexes refering to columns in the PRIMITIVE_ARRAY_TYPES table.
    private static final int PRIMITIVE_WRAPPER_NAME_INDEX = 0;
    private static final int PRIMITIVE_TYPE_NAME_INDEX = 1;
    private static final int PRIMITIVE_TYPE_KEY_INDEX  = 2;
    private static final int PRIMITIVE_OPEN_TYPE_INDEX  = 3;

    private static final Object[][] PRIMITIVE_ARRAY_TYPES = {
        { Boolean.class.getName(),   boolean.class.getName(), "Z", SimpleType.BOOLEAN },
        { Character.class.getName(), char.class.getName(),    "C", SimpleType.CHARACTER },
        { Byte.class.getName(),      byte.class.getName(),    "B", SimpleType.BYTE },
        { Short.class.getName(),     short.class.getName(),   "S", SimpleType.SHORT },
        { Integer.class.getName(),   int.class.getName(),     "I", SimpleType.INTEGER },
        { Long.class.getName(),      long.class.getName(),    "J", SimpleType.LONG },
        { Float.class.getName(),     float.class.getName(),   "F", SimpleType.FLOAT },
        { Double.class.getName(),    double.class.getName(),  "D", SimpleType.DOUBLE }
    };

    static boolean isPrimitiveContentType(final String primitiveKey) {
        for (Object[] typeDescr : PRIMITIVE_ARRAY_TYPES) {
            if (typeDescr[PRIMITIVE_TYPE_KEY_INDEX].equals(primitiveKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the key used to identify the element type in
     * arrays - e.g. "Z" for boolean, "C" for char etc...
     * @param elementClassName the wrapper class name of the array
     *        element ("Boolean",  "Character", etc...)
     * @return the key corresponding to the given type ("Z", "C", etc...)
     *         return null if the given elementClassName is not a primitive
     *         wrapper class name.
     **/
    static String getPrimitiveTypeKey(String elementClassName) {
        for (Object[] typeDescr : PRIMITIVE_ARRAY_TYPES) {
            if (elementClassName.equals(typeDescr[PRIMITIVE_WRAPPER_NAME_INDEX]))
                return (String)typeDescr[PRIMITIVE_TYPE_KEY_INDEX];
        }
        return null;
    }

    /**
     * Return the primitive type name corresponding to the given wrapper class.
     * e.g. "boolean" for "Boolean", "char" for "Character" etc...
     * @param elementClassName the type of the array element ("Boolean",
     *        "Character", etc...)
     * @return the primitive type name corresponding to the given wrapper class
     *         ("boolean", "char", etc...)
     *         return null if the given elementClassName is not a primitive
     *         wrapper type name.
     **/
    static String getPrimitiveTypeName(String elementClassName) {
        for (Object[] typeDescr : PRIMITIVE_ARRAY_TYPES) {
            if (elementClassName.equals(typeDescr[PRIMITIVE_WRAPPER_NAME_INDEX]))
                return (String)typeDescr[PRIMITIVE_TYPE_NAME_INDEX];
        }
        return null;
    }

    /**
     * Return the primitive open type corresponding to the given primitive type.
     * e.g. SimpleType.BOOLEAN for "boolean", SimpleType.CHARACTER for
     * "char", etc...
     * @param primitiveTypeName the primitive type of the array element ("boolean",
     *        "char", etc...)
     * @return the OpenType corresponding to the given primitive type name
     *         (SimpleType.BOOLEAN, SimpleType.CHARACTER, etc...)
     *         return null if the given elementClassName is not a primitive
     *         type name.
     **/
    static SimpleType<?> getPrimitiveOpenType(String primitiveTypeName) {
        for (Object[] typeDescr : PRIMITIVE_ARRAY_TYPES) {
            if (primitiveTypeName.equals(typeDescr[PRIMITIVE_TYPE_NAME_INDEX]))
                return (SimpleType<?>)typeDescr[PRIMITIVE_OPEN_TYPE_INDEX];
        }
        return null;
    }

    /* *** Constructor *** */

    /**
     * Constructs an {@code ArrayType} instance describing <i>open data</i> values which are
     * arrays with dimension <var>dimension</var> of elements
     * whose <i>open type</i> is <var>elementType</var>.
     * <p>
     * When invoked on an {@code ArrayType} instance,
     * the {@link OpenType#getClassName() getClassName} method
     * returns the class name of the array instances it describes
     * (following the rules defined by the
     * {@link Class#getName() getName} method of {@code java.lang.Class}),
     * not the class name of the array elements
     * (which is returned by a call to {@code getElementOpenType().getClassName()}).
     * <p>
     * The internal field corresponding to the type name of this
     * {@code ArrayType} instance is also set to
     * the class name of the array instances it describes.
     * In other words, the methods {@code getClassName} and
     * {@code getTypeName} return the same string value.
     * The internal field corresponding to the description of this
     * {@code ArrayType} instance is set to a string value
     * which follows the following template:
     * <ul>
     * <li>if non-primitive array: <code><i>&lt;dimension&gt;</i>-dimension array
     *     of <i>&lt;element_class_name&gt;</i></code></li>
     * <li>if primitive array: <code><i>&lt;dimension&gt;</i>-dimension array
     *     of <i>&lt;primitive_type_of_the_element_class_name&gt;</i></code></li>
     * </ul>
     * <p>
     * As an example, the following piece of code:
     * <pre>{@code
     * ArrayType<String[][][]> t = new ArrayType<String[][][]>(3, SimpleType.STRING);
     * System.out.println("array class name       = " + t.getClassName());
     * System.out.println("element class name     = " + t.getElementOpenType().getClassName());
     * System.out.println("array type name        = " + t.getTypeName());
     * System.out.println("array type description = " + t.getDescription());
     * }</pre>
     * would produce the following output:
     * <pre>{@code
     * array class name       = [[[Ljava.lang.String;
     * element class name     = java.lang.String
     * array type name        = [[[Ljava.lang.String;
     * array type description = 3-dimension array of java.lang.String
     * }</pre>
     * And the following piece of code which is equivalent to the one listed
     * above would also produce the same output:
     * <pre>{@code
     * ArrayType<String[]> t1 = new ArrayType<String[]>(1, SimpleType.STRING);
     * ArrayType<String[][]> t2 = new ArrayType<String[][]>(1, t1);
     * ArrayType<String[][][]> t3 = new ArrayType<String[][][]>(1, t2);
     * System.out.println("array class name       = " + t3.getClassName());
     * System.out.println("element class name     = " + t3.getElementOpenType().getClassName());
     * System.out.println("array type name        = " + t3.getTypeName());
     * System.out.println("array type description = " + t3.getDescription());
     * }</pre>
     *
     * @param  dimension  the dimension of arrays described by this {@code ArrayType} instance;
     *                    must be greater than or equal to 1.
     *
     * @param  elementType  the <i>open type</i> of element values contained
     *                      in the arrays described by this {@code ArrayType}
     *                      instance; must be an instance of either
     *                      {@code SimpleType}, {@code CompositeType},
     *                      {@code TabularType} or another {@code ArrayType}
     *                      with a {@code SimpleType}, {@code CompositeType}
     *                      or {@code TabularType} as its {@code elementType}.
     *
     * @throws IllegalArgumentException if {@code dimension} is not a positive
     *                                  integer.
     * @throws OpenDataException  if <var>elementType's className</var> is not
     *                            one of the allowed Java class names for open
     *                            data.
     */
    public ArrayType(int dimension,
                     OpenType<?> elementType) throws OpenDataException {
        // Check and construct state defined by parent.
        // We can't use the package-private OpenType constructor because
        // we don't know if the elementType parameter is sane.
        super(buildArrayClassName(dimension, elementType),
              buildArrayClassName(dimension, elementType),
              buildArrayDescription(dimension, elementType));

        // Check and construct state specific to ArrayType
        //
        if (elementType.isArray()) {
            ArrayType<?> at = (ArrayType<?>) elementType;
            this.dimension = at.getDimension() + dimension;
            this.elementType = at.getElementOpenType();
            this.primitiveArray = at.isPrimitiveArray();
        } else {
            this.dimension = dimension;
            this.elementType = elementType;
            this.primitiveArray = false;
        }
    }

    /**
     * Constructs a unidimensional {@code ArrayType} instance for the
     * supplied {@code SimpleType}.
     * <p>
     * This constructor supports the creation of arrays of primitive
     * types when {@code primitiveArray} is {@code true}.
     * <p>
     * For primitive arrays the {@link #getElementOpenType()} method
     * returns the {@link SimpleType} corresponding to the wrapper
     * type of the primitive type of the array.
     * <p>
     * When invoked on an {@code ArrayType} instance,
     * the {@link OpenType#getClassName() getClassName} method
     * returns the class name of the array instances it describes
     * (following the rules defined by the
     * {@link Class#getName() getName} method of {@code java.lang.Class}),
     * not the class name of the array elements
     * (which is returned by a call to {@code getElementOpenType().getClassName()}).
     * <p>
     * The internal field corresponding to the type name of this
     * {@code ArrayType} instance is also set to
     * the class name of the array instances it describes.
     * In other words, the methods {@code getClassName} and
     * {@code getTypeName} return the same string value.
     * The internal field corresponding to the description
     * of this {@code ArrayType} instance is set to a string value
     * which follows the following template:
     * <ul>
     * <li>if non-primitive array: <code>1-dimension array
     *     of <i>&lt;element_class_name&gt;</i></code></li>
     * <li>if primitive array: <code>1-dimension array
     *     of <i>&lt;primitive_type_of_the_element_class_name&gt;</i></code></li>
     * </ul>
     * <p>
     * As an example, the following piece of code:
     * <pre>{@code
     * ArrayType<int[]> t = new ArrayType<int[]>(SimpleType.INTEGER, true);
     * System.out.println("array class name       = " + t.getClassName());
     * System.out.println("element class name     = " + t.getElementOpenType().getClassName());
     * System.out.println("array type name        = " + t.getTypeName());
     * System.out.println("array type description = " + t.getDescription());
     * }</pre>
     * would produce the following output:
     * <pre>{@code
     * array class name       = [I
     * element class name     = java.lang.Integer
     * array type name        = [I
     * array type description = 1-dimension array of int
     * }</pre>
     *
     * @param elementType the {@code SimpleType} of the element values
     *                    contained in the arrays described by this
     *                    {@code ArrayType} instance.
     *
     * @param primitiveArray {@code true} when this array describes
     *                       primitive arrays.
     *
     * @throws IllegalArgumentException if {@code dimension} is not a positive
     * integer.
     * @throws OpenDataException if {@code primitiveArray} is {@code true} and
     * {@code elementType} is not a valid {@code SimpleType} for a primitive
     * type.
     *
     * @since 1.6
     */
    public ArrayType(SimpleType<?> elementType,
                     boolean primitiveArray) throws OpenDataException {

        // Check and construct state defined by parent.
        // We can call the package-private OpenType constructor because the
        // set of SimpleTypes is fixed and SimpleType can't be subclassed.
        super(buildArrayClassName(1, elementType, primitiveArray),
              buildArrayClassName(1, elementType, primitiveArray),
              buildArrayDescription(1, elementType, primitiveArray),
              true);

        // Check and construct state specific to ArrayType
        //
        this.dimension = 1;
        this.elementType = elementType;
        this.primitiveArray = primitiveArray;
    }

    /* Package-private constructor for callers we trust to get it right. */
    ArrayType(String className, String typeName, String description,
              int dimension, OpenType<?> elementType,
              boolean primitiveArray) {
        super(className, typeName, description, true);
        this.dimension = dimension;
        this.elementType = elementType;
        this.primitiveArray = primitiveArray;
    }

    private static String buildArrayClassName(int dimension,
                                              OpenType<?> elementType)
        throws OpenDataException {
        boolean isPrimitiveArray = false;
        if (elementType.isArray()) {
            isPrimitiveArray = ((ArrayType<?>) elementType).isPrimitiveArray();
        }
        return buildArrayClassName(dimension, elementType, isPrimitiveArray);
    }

    private static String buildArrayClassName(int dimension,
                                              OpenType<?> elementType,
                                              boolean isPrimitiveArray)
        throws OpenDataException {
        if (dimension < 1) {
            throw new IllegalArgumentException(
                "Value of argument dimension must be greater than 0");
        }
        StringBuilder result = new StringBuilder();
        String elementClassName = elementType.getClassName();
        // Add N (= dimension) additional '[' characters to the existing array
        for (int i = 1; i <= dimension; i++) {
            result.append('[');
        }
        if (elementType.isArray()) {
            result.append(elementClassName);
        } else {
            if (isPrimitiveArray) {
                final String key = getPrimitiveTypeKey(elementClassName);
                // Ideally we should throw an IllegalArgumentException here,
                // but for compatibility reasons we throw an OpenDataException.
                // (used to be thrown by OpenType() constructor).
                //
                if (key == null)
                    throw new OpenDataException("Element type is not primitive: "
                            + elementClassName);
                result.append(key);
            } else {
                result.append("L");
                result.append(elementClassName);
                result.append(';');
            }
        }
        return result.toString();
    }

    private static String buildArrayDescription(int dimension,
                                                OpenType<?> elementType)
        throws OpenDataException {
        boolean isPrimitiveArray = false;
        if (elementType.isArray()) {
            isPrimitiveArray = ((ArrayType<?>) elementType).isPrimitiveArray();
        }
        return buildArrayDescription(dimension, elementType, isPrimitiveArray);
    }

    private static String buildArrayDescription(int dimension,
                                                OpenType<?> elementType,
                                                boolean isPrimitiveArray)
        throws OpenDataException {
        if (elementType.isArray()) {
            ArrayType<?> at = (ArrayType<?>) elementType;
            dimension += at.getDimension();
            elementType = at.getElementOpenType();
            isPrimitiveArray = at.isPrimitiveArray();
        }
        StringBuilder result = new StringBuilder();
        result.append(dimension).append("-dimension array of ");
        final String elementClassName = elementType.getClassName();
        if (isPrimitiveArray) {
            // Convert from wrapper type to primitive type
            final String primitiveType =
                    getPrimitiveTypeName(elementClassName);

            // Ideally we should throw an IllegalArgumentException here,
            // but for compatibility reasons we throw an OpenDataException.
            // (used to be thrown by OpenType() constructor).
            //
            if (primitiveType == null)
                throw new OpenDataException("Element is not a primitive type: "+
                        elementClassName);
            result.append(primitiveType);
        } else {
            result.append(elementClassName);
        }
        return result.toString();
    }

    /* *** ArrayType specific information methods *** */

    /**
     * Returns the dimension of arrays described by this {@code ArrayType} instance.
     *
     * @return the dimension.
     */
    public int getDimension() {

        return dimension;
    }

    /**
     * Returns the <i>open type</i> of element values contained
     * in the arrays described by this {@code ArrayType} instance.
     *
     * @return the element type.
     */
    public OpenType<?> getElementOpenType() {

        return elementType;
    }

    /**
     * Returns {@code true} if the open data values this open
     * type describes are primitive arrays, {@code false} otherwise.
     *
     * @return true if this is a primitive array type.
     *
     * @since 1.6
     */
    public boolean isPrimitiveArray() {

        return primitiveArray;
    }

    /**
     * Tests whether <var>obj</var> is a value for this {@code ArrayType}
     * instance.
     * <p>
     * This method returns {@code true} if and only if <var>obj</var>
     * is not null, <var>obj</var> is an array and any one of the following
     * is {@code true}:
     *
     * <ul>
     * <li>if this {@code ArrayType} instance describes an array of
     * {@code SimpleType} elements or their corresponding primitive types,
     * <var>obj</var>'s class name is the same as the className field defined
     * for this {@code ArrayType} instance (i.e. the class name returned
     * by the {@link OpenType#getClassName() getClassName} method, which
     * includes the dimension information),<br>&nbsp;</li>
     * <li>if this {@code ArrayType} instance describes an array of
     * classes implementing the {@code TabularData} interface or the
     * {@code CompositeData} interface, <var>obj</var> is assignable to
     * such a declared array, and each element contained in {<var>obj</var>
     * is either null or a valid value for the element's open type specified
     * by this {@code ArrayType} instance.</li>
     * </ul>
     *
     * @param obj the object to be tested.
     *
     * @return {@code true} if <var>obj</var> is a value for this
     * {@code ArrayType} instance.
     */
    public boolean isValue(Object obj) {

        // if obj is null, return false
        //
        if (obj == null) {
            return false;
        }

        Class<?> objClass = obj.getClass();
        String objClassName = objClass.getName();

        // if obj is not an array, return false
        //
        if ( ! objClass.isArray() ) {
            return false;
        }

        // Test if obj's class name is the same as for the array values that this instance describes
        // (this is fine if elements are of simple types, which are final classes)
        //
        if ( this.getClassName().equals(objClassName) ) {
            return true;
        }

        // In case this ArrayType instance describes an array of classes implementing the TabularData or CompositeData interface,
        // we first check for the assignability of obj to such an array of TabularData or CompositeData,
        // which ensures that:
        //  . obj is of the same dimension as this ArrayType instance,
        //  . it is declared as an array of elements which are either all TabularData or all CompositeData.
        //
        // If the assignment check is positive,
        // then we have to check that each element in obj is of the same TabularType or CompositeType
        // as the one described by this ArrayType instance.
        //
        // [About assignment check, note that the call below returns true: ]
        // [Class.forName("[Lpackage.CompositeData;").isAssignableFrom(Class.forName("[Lpackage.CompositeDataImpl;)")); ]
        //
        if ( (this.elementType.getClassName().equals(TabularData.class.getName()))  ||
             (this.elementType.getClassName().equals(CompositeData.class.getName()))   ) {

            boolean isTabular =
                (elementType.getClassName().equals(TabularData.class.getName()));
            int[] dims = new int[getDimension()];
            Class<?> elementClass = isTabular ? TabularData.class : CompositeData.class;
            Class<?> targetClass = Array.newInstance(elementClass, dims).getClass();

            // assignment check: return false if negative
            if  ( ! targetClass.isAssignableFrom(objClass) ) {
                return false;
            }

            // check that all elements in obj are valid values for this ArrayType
            if ( ! checkElementsType( (Object[]) obj, this.dimension) ) { // we know obj's dimension is this.dimension
                return false;
            }

            return true;
        }

        // if previous tests did not return, then obj is not a value for this ArrayType instance
        return false;
    }

    /**
     * Returns true if and only if all elements contained in the array argument x_dim_Array of dimension dim
     * are valid values (ie either null or of the right openType)
     * for the element open type specified by this ArrayType instance.
     *
     * This method's implementation uses recursion to go down the dimensions of the array argument.
     */
    private boolean checkElementsType(Object[] x_dim_Array, int dim) {

        // if the elements of x_dim_Array are themselves array: go down recursively....
        if ( dim > 1 ) {
            for (int i=0; i<x_dim_Array.length; i++) {
                if ( ! checkElementsType((Object[])x_dim_Array[i], dim-1) ) {
                    return false;
                }
            }
            return true;
        }
        // ...else, for a non-empty array, each element must be a valid value: either null or of the right openType
        else {
            for (int i=0; i<x_dim_Array.length; i++) {
                if ( (x_dim_Array[i] != null) && (! this.getElementOpenType().isValue(x_dim_Array[i])) ) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    boolean isAssignableFrom(OpenType<?> ot) {
        if (!(ot instanceof ArrayType<?>))
            return false;
        ArrayType<?> at = (ArrayType<?>) ot;
        return (at.getDimension() == getDimension() &&
                at.isPrimitiveArray() == isPrimitiveArray() &&
                at.getElementOpenType().isAssignableFrom(getElementOpenType()));
    }


    /* *** Methods overriden from class Object *** */

    /**
     * Compares the specified {@code obj} parameter with this
     * {@code ArrayType} instance for equality.
     * <p>
     * Two {@code ArrayType} instances are equal if and only if they
     * describe array instances which have the same dimension, elements'
     * open type and primitive array flag.
     *
     * @param obj the object to be compared for equality with this
     *            {@code ArrayType} instance; if <var>obj</var>
     *            is {@code null} or is not an instance of the
     *            class {@code ArrayType} this method returns
     *            {@code false}.
     *
     * @return {@code true} if the specified object is equal to
     *         this {@code ArrayType} instance.
     */
    public boolean equals(Object obj) {

        // if obj is null, return false
        //
        if (obj == null) {
            return false;
        }

        // if obj is not an ArrayType, return false
        //
        if (!(obj instanceof ArrayType<?>))
            return false;
        ArrayType<?> other = (ArrayType<?>) obj;

        // if other's dimension is different than this instance's, return false
        //
        if (this.dimension != other.dimension) {
            return false;
        }

        // Test if other's elementType field is the same as for this instance
        //
        if (!this.elementType.equals(other.elementType)) {
            return false;
        }

        // Test if other's primitiveArray flag is the same as for this instance
        //
        return this.primitiveArray == other.primitiveArray;
    }

    /**
     * Returns the hash code value for this {@code ArrayType} instance.
     * <p>
     * The hash code of an {@code ArrayType} instance is the sum of the
     * hash codes of all the elements of information used in {@code equals}
     * comparisons (i.e. dimension, elements' open type and primitive array flag).
     * The hashcode for a primitive value is the hashcode of the corresponding boxed
     * object (e.g. the hashcode for {@code true} is {@code Boolean.TRUE.hashCode()}).
     * This ensures that {@code t1.equals(t2)} implies that
     * {@code t1.hashCode()==t2.hashCode()} for any two
     * {@code ArrayType} instances {@code t1} and {@code t2},
     * as required by the general contract of the method
     * {@link Object#hashCode() Object.hashCode()}.
     * <p>
     * As {@code ArrayType} instances are immutable, the hash
     * code for this instance is calculated once, on the first call
     * to {@code hashCode}, and then the same value is returned
     * for subsequent calls.
     *
     * @return  the hash code value for this {@code ArrayType} instance
     */
    public int hashCode() {

        // Calculate the hash code value if it has not yet been done (ie 1st call to hashCode())
        //
        if (myHashCode == null) {
            int value = 0;
            value += dimension;
            value += elementType.hashCode();
            value += Boolean.valueOf(primitiveArray).hashCode();
            myHashCode = Integer.valueOf(value);
        }

        // return always the same hash code for this instance (immutable)
        //
        return myHashCode.intValue();
    }

    /**
     * Returns a string representation of this {@code ArrayType} instance.
     * <p>
     * The string representation consists of the name of this class (i.e.
     * {@code javax.management.openmbean.ArrayType}), the type name,
     * the dimension, the elements' open type and the primitive array flag
     * defined for this instance.
     * <p>
     * As {@code ArrayType} instances are immutable, the
     * string representation for this instance is calculated
     * once, on the first call to {@code toString}, and
     * then the same value is returned for subsequent calls.
     *
     * @return a string representation of this {@code ArrayType} instance
     */
    public String toString() {

        // Calculate the string representation if it has not yet been done (ie 1st call to toString())
        //
        if (myToString == null) {
            myToString = getClass().getName() +
                         "(name=" + getTypeName() +
                         ",dimension=" + dimension +
                         ",elementType=" + elementType +
                         ",primitiveArray=" + primitiveArray + ")";
        }

        // return always the same string representation for this instance (immutable)
        //
        return myToString;
    }

    /**
     * Create an {@code ArrayType} instance in a type-safe manner.
     * <p>
     * Multidimensional arrays can be built up by calling this method as many
     * times as necessary.
     * <p>
     * Calling this method twice with the same parameters may return the same
     * object or two equal but not identical objects.
     * <p>
     * As an example, the following piece of code:
     * <pre>{@code
     * ArrayType<String[]> t1 = ArrayType.getArrayType(SimpleType.STRING);
     * ArrayType<String[][]> t2 = ArrayType.getArrayType(t1);
     * ArrayType<String[][][]> t3 = ArrayType.getArrayType(t2);
     * System.out.println("array class name       = " + t3.getClassName());
     * System.out.println("element class name     = " + t3.getElementOpenType().getClassName());
     * System.out.println("array type name        = " + t3.getTypeName());
     * System.out.println("array type description = " + t3.getDescription());
     * }</pre>
     * would produce the following output:
     * <pre>{@code
     * array class name       = [[[Ljava.lang.String;
     * element class name     = java.lang.String
     * array type name        = [[[Ljava.lang.String;
     * array type description = 3-dimension array of java.lang.String
     * }</pre>
     *
     * @param <E> the Java type that described instances must have
     * @param  elementType  the <i>open type</i> of element values contained
     *                      in the arrays described by this {@code ArrayType}
     *                      instance; must be an instance of either
     *                      {@code SimpleType}, {@code CompositeType},
     *                      {@code TabularType} or another {@code ArrayType}
     *                      with a {@code SimpleType}, {@code CompositeType}
     *                      or {@code TabularType} as its {@code elementType}.
     * @return an {@code ArrayType} instance
     * @throws OpenDataException if <var>elementType's className</var> is not
     *                           one of the allowed Java class names for open
     *                           data.
     *
     * @since 1.6
     */
    public static <E> ArrayType<E[]> getArrayType(OpenType<E> elementType)
        throws OpenDataException {
        return new ArrayType<E[]>(1, elementType);
    }

    /**
     * Create an {@code ArrayType} instance in a type-safe manner.
     * <p>
     * Calling this method twice with the same parameters may return the
     * same object or two equal but not identical objects.
     * <p>
     * As an example, the following piece of code:
     * <pre>{@code
     * ArrayType<int[][][]> t = ArrayType.getPrimitiveArrayType(int[][][].class);
     * System.out.println("array class name       = " + t.getClassName());
     * System.out.println("element class name     = " + t.getElementOpenType().getClassName());
     * System.out.println("array type name        = " + t.getTypeName());
     * System.out.println("array type description = " + t.getDescription());
     * }</pre>
     * would produce the following output:
     * <pre>{@code
     * array class name       = [[[I
     * element class name     = java.lang.Integer
     * array type name        = [[[I
     * array type description = 3-dimension array of int
     * }</pre>
     *
     * @param <T> the Java type that described instances must have
     * @param arrayClass a primitive array class such as {@code int[].class},
     *                   {@code boolean[][].class}, etc. The {@link
     *                   #getElementOpenType()} method of the returned
     *                   {@code ArrayType} returns the {@link SimpleType}
     *                   corresponding to the wrapper type of the primitive
     *                   type of the array.
     * @return an {@code ArrayType} instance
     *
     * @throws IllegalArgumentException if <var>arrayClass</var> is not
     *                                  a primitive array.
     *
     * @since 1.6
     */
    @SuppressWarnings("unchecked")  // can't get appropriate T for primitive array
    public static <T> ArrayType<T> getPrimitiveArrayType(Class<T> arrayClass) {
        // Check if the supplied parameter is an array
        //
        if (!arrayClass.isArray()) {
            throw new IllegalArgumentException("arrayClass must be an array");
        }

        // Calculate array dimension and component type name
        //
        int n = 1;
        Class<?> componentType = arrayClass.getComponentType();
        while (componentType.isArray()) {
            n++;
            componentType = componentType.getComponentType();
        }
        String componentTypeName = componentType.getName();

        // Check if the array's component type is a primitive type
        //
        if (!componentType.isPrimitive()) {
            throw new IllegalArgumentException(
                "component type of the array must be a primitive type");
        }

        // Map component type name to corresponding SimpleType
        //
        final SimpleType<?> simpleType =
                getPrimitiveOpenType(componentTypeName);

        // Build primitive array
        //
        try {
            @SuppressWarnings("rawtypes")
            ArrayType at = new ArrayType(simpleType, true);
            if (n > 1)
                at = new ArrayType<T>(n - 1, at);
            return at;
        } catch (OpenDataException e) {
            throw new IllegalArgumentException(e); // should not happen
        }
    }

    /**
     * Replace/resolve the object read from the stream before it is returned
     * to the caller.
     *
     * @serialData The new serial form of this class defines a new serializable
     * {@code boolean} field {@code primitiveArray}. In order to guarantee the
     * interoperability with previous versions of this class the new serial
     * form must continue to refer to primitive wrapper types even when the
     * {@code ArrayType} instance describes a primitive type array. So when
     * {@code primitiveArray} is {@code true} the {@code className},
     * {@code typeName} and {@code description} serializable fields
     * are converted into primitive types before the deserialized
     * {@code ArrayType} instance is return to the caller. The
     * {@code elementType} field always returns the {@code SimpleType}
     * corresponding to the primitive wrapper type of the array's
     * primitive type.
     * <p>
     * Therefore the following serializable fields are deserialized as follows:
     * <ul>
     *   <li>if {@code primitiveArray} is {@code true} the {@code className}
     *       field is deserialized by replacing the array's component primitive
     *       wrapper type by the corresponding array's component primitive type,
     *       e.g. {@code "[[Ljava.lang.Integer;"} will be deserialized as
     *       {@code "[[I"}.</li>
     *   <li>if {@code primitiveArray} is {@code true} the {@code typeName}
     *       field is deserialized by replacing the array's component primitive
     *       wrapper type by the corresponding array's component primitive type,
     *       e.g. {@code "[[Ljava.lang.Integer;"} will be deserialized as
     *       {@code "[[I"}.</li>
     *   <li>if {@code primitiveArray} is {@code true} the {@code description}
     *       field is deserialized by replacing the array's component primitive
     *       wrapper type by the corresponding array's component primitive type,
     *       e.g. {@code "2-dimension array of java.lang.Integer"} will be
     *       deserialized as {@code "2-dimension array of int"}.</li>
     * </ul>
     *
     * @since 1.6
     */
    private Object readResolve() throws ObjectStreamException {
        if (primitiveArray) {
            return convertFromWrapperToPrimitiveTypes();
        } else {
            return this;
        }
    }

    private <T> ArrayType<T> convertFromWrapperToPrimitiveTypes() {
        String cn = getClassName();
        String tn = getTypeName();
        String d = getDescription();
        for (Object[] typeDescr : PRIMITIVE_ARRAY_TYPES) {
            if (cn.indexOf((String)typeDescr[PRIMITIVE_WRAPPER_NAME_INDEX]) != -1) {
                cn = cn.replaceFirst(
                    "L" + typeDescr[PRIMITIVE_WRAPPER_NAME_INDEX] + ";",
                    (String) typeDescr[PRIMITIVE_TYPE_KEY_INDEX]);
                tn = tn.replaceFirst(
                    "L" + typeDescr[PRIMITIVE_WRAPPER_NAME_INDEX] + ";",
                    (String) typeDescr[PRIMITIVE_TYPE_KEY_INDEX]);
                d = d.replaceFirst(
                    (String) typeDescr[PRIMITIVE_WRAPPER_NAME_INDEX],
                    (String) typeDescr[PRIMITIVE_TYPE_NAME_INDEX]);
                break;
            }
        }
        return new ArrayType<T>(cn, tn, d,
                                dimension, elementType, primitiveArray);
    }

    /**
     * Nominate a replacement for this object in the stream before the object
     * is written.
     *
     * @serialData The new serial form of this class defines a new serializable
     * {@code boolean} field {@code primitiveArray}. In order to guarantee the
     * interoperability with previous versions of this class the new serial
     * form must continue to refer to primitive wrapper types even when the
     * {@code ArrayType} instance describes a primitive type array. So when
     * {@code primitiveArray} is {@code true} the {@code className},
     * {@code typeName} and {@code description} serializable fields
     * are converted into wrapper types before the serialized
     * {@code ArrayType} instance is written to the stream. The
     * {@code elementType} field always returns the {@code SimpleType}
     * corresponding to the primitive wrapper type of the array's
     * primitive type.
     * <p>
     * Therefore the following serializable fields are serialized as follows:
     * <ul>
     *   <li>if {@code primitiveArray} is {@code true} the {@code className}
     *       field is serialized by replacing the array's component primitive
     *       type by the corresponding array's component primitive wrapper type,
     *       e.g. {@code "[[I"} will be serialized as
     *       {@code "[[Ljava.lang.Integer;"}.</li>
     *   <li>if {@code primitiveArray} is {@code true} the {@code typeName}
     *       field is serialized by replacing the array's component primitive
     *       type by the corresponding array's component primitive wrapper type,
     *       e.g. {@code "[[I"} will be serialized as
     *       {@code "[[Ljava.lang.Integer;"}.</li>
     *   <li>if {@code primitiveArray} is {@code true} the {@code description}
     *       field is serialized by replacing the array's component primitive
     *       type by the corresponding array's component primitive wrapper type,
     *       e.g. {@code "2-dimension array of int"} will be serialized as
     *       {@code "2-dimension array of java.lang.Integer"}.</li>
     * </ul>
     *
     * @since 1.6
     */
    private Object writeReplace() throws ObjectStreamException {
        if (primitiveArray) {
            return convertFromPrimitiveToWrapperTypes();
        } else {
            return this;
        }
    }

    private <T> ArrayType<T> convertFromPrimitiveToWrapperTypes() {
        String cn = getClassName();
        String tn = getTypeName();
        String d = getDescription();
        for (Object[] typeDescr : PRIMITIVE_ARRAY_TYPES) {
            if (cn.indexOf((String) typeDescr[PRIMITIVE_TYPE_KEY_INDEX]) != -1) {
                cn = cn.replaceFirst(
                    (String) typeDescr[PRIMITIVE_TYPE_KEY_INDEX],
                    "L" + typeDescr[PRIMITIVE_WRAPPER_NAME_INDEX] + ";");
                tn = tn.replaceFirst(
                    (String) typeDescr[PRIMITIVE_TYPE_KEY_INDEX],
                    "L" + typeDescr[PRIMITIVE_WRAPPER_NAME_INDEX] + ";");
                d = d.replaceFirst(
                    (String) typeDescr[PRIMITIVE_TYPE_NAME_INDEX],
                    (String) typeDescr[PRIMITIVE_WRAPPER_NAME_INDEX]);
                break;
            }
        }
        return new ArrayType<T>(cn, tn, d,
                                dimension, elementType, primitiveArray);
    }
}
