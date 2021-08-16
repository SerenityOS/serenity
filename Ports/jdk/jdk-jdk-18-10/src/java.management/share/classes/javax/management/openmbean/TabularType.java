/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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


// java import
//
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

// jmx import
//


/**
 * The <code>TabularType</code> class is the <i> open type</i> class
 * whose instances describe the types of {@link TabularData TabularData} values.
 *
 * @since 1.5
 */
public class TabularType extends OpenType<TabularData> {

    /* Serial version */
    static final long serialVersionUID = 6554071860220659261L;


    /**
     * @serial The composite type of rows
     */
    private CompositeType  rowType;

    /**
     * @serial The items used to index each row element, kept in the order the user gave
     *         This is an unmodifiable {@link ArrayList}
     */
    @SuppressWarnings("serial") // Conditionally serializable
    private List<String> indexNames;


    private transient Integer myHashCode = null; // As this instance is immutable, these two values
    private transient String  myToString = null; // need only be calculated once.


    /* *** Constructor *** */

    /**
     * Constructs a <code>TabularType</code> instance, checking for the validity of the given parameters.
     * The validity constraints are described below for each parameter.
     * <p>
     * The Java class name of tabular data values this tabular type represents
     * (ie the class name returned by the {@link OpenType#getClassName() getClassName} method)
     * is set to the string value returned by <code>TabularData.class.getName()</code>.
     *
     * @param  typeName  The name given to the tabular type this instance represents; cannot be a null or empty string.
     * <br>&nbsp;
     * @param  description  The human readable description of the tabular type this instance represents;
     *                      cannot be a null or empty string.
     * <br>&nbsp;
     * @param  rowType  The type of the row elements of tabular data values described by this tabular type instance;
     *                  cannot be null.
     * <br>&nbsp;
     * @param  indexNames  The names of the items the values of which are used to uniquely index each row element in the
     *                     tabular data values described by this tabular type instance;
     *                     cannot be null or empty. Each element should be an item name defined in <var>rowType</var>
     *                     (no null or empty string allowed).
     *                     It is important to note that the <b>order</b> of the item names in <var>indexNames</var>
     *                     is used by the methods {@link TabularData#get(java.lang.Object[]) get} and
     *                     {@link TabularData#remove(java.lang.Object[]) remove} of class
     *                     <code>TabularData</code> to match their array of values parameter to items.
     * <br>&nbsp;
     * @throws IllegalArgumentException  if <var>rowType</var> is null,
     *                                   or <var>indexNames</var> is a null or empty array,
     *                                   or an element in <var>indexNames</var> is a null or empty string,
     *                                   or <var>typeName</var> or <var>description</var> is a null or empty string.
     * <br>&nbsp;
     * @throws OpenDataException  if an element's value of <var>indexNames</var>
     *                            is not an item name defined in <var>rowType</var>.
     */
    public TabularType(String         typeName,
                       String         description,
                       CompositeType  rowType,
                       String[]       indexNames) throws OpenDataException {

        // Check and initialize state defined by parent.
        //
        super(TabularData.class.getName(), typeName, description, false);

        // Check rowType is not null
        //
        if (rowType == null) {
            throw new IllegalArgumentException("Argument rowType cannot be null.");
        }

        // Check indexNames is neither null nor empty and does not contain any null element or empty string
        //
        checkForNullElement(indexNames, "indexNames");
        checkForEmptyString(indexNames, "indexNames");

        // Check all indexNames values are valid item names for rowType
        //
        for (int i=0; i<indexNames.length; i++) {
            if ( ! rowType.containsKey(indexNames[i]) ) {
                throw new OpenDataException("Argument's element value indexNames["+ i +"]=\""+ indexNames[i] +
                                            "\" is not a valid item name for rowType.");
            }
        }

        // initialize rowType
        //
        this.rowType    = rowType;

        // initialize indexNames (copy content so that subsequent
        // modifs to the array referenced by the indexNames parameter
        // have no impact)
        //
        List<String> tmpList = new ArrayList<String>(indexNames.length + 1);
        for (int i=0; i<indexNames.length; i++) {
            tmpList.add(indexNames[i]);
        }
        this.indexNames = Collections.unmodifiableList(tmpList);
    }

    /**
     * Checks that Object[] arg is neither null nor empty (ie length==0)
     * and that it does not contain any null element.
     */
    private static void checkForNullElement(Object[] arg, String argName) {
        if ( (arg == null) || (arg.length == 0) ) {
            throw new IllegalArgumentException("Argument "+ argName +"[] cannot be null or empty.");
        }
        for (int i=0; i<arg.length; i++) {
            if (arg[i] == null) {
                throw new IllegalArgumentException("Argument's element "+ argName +"["+ i +"] cannot be null.");
            }
        }
    }

    /**
     * Checks that String[] does not contain any empty (or blank characters only) string.
     */
    private static void checkForEmptyString(String[] arg, String argName) {
        for (int i=0; i<arg.length; i++) {
            if (arg[i].trim().isEmpty()) {
                throw new IllegalArgumentException("Argument's element "+ argName +"["+ i +"] cannot be an empty string.");
            }
        }
    }


    /* *** Tabular type specific information methods *** */

    /**
     * Returns the type of the row elements of tabular data values
     * described by this <code>TabularType</code> instance.
     *
     * @return the type of each row.
     */
    public CompositeType getRowType() {

        return rowType;
    }

    /**
     * <p>Returns, in the same order as was given to this instance's
     * constructor, an unmodifiable List of the names of the items the
     * values of which are used to uniquely index each row element of
     * tabular data values described by this <code>TabularType</code>
     * instance.</p>
     *
     * @return a List of String representing the names of the index
     * items.
     *
     */
    public List<String> getIndexNames() {

        return indexNames;
    }

    /**
     * Tests whether <var>obj</var> is a value which could be
     * described by this <code>TabularType</code> instance.
     *
     * <p>If <var>obj</var> is null or is not an instance of
     * <code>javax.management.openmbean.TabularData</code>,
     * <code>isValue</code> returns <code>false</code>.</p>
     *
     * <p>If <var>obj</var> is an instance of
     * <code>javax.management.openmbean.TabularData</code>, say {@code
     * td}, the result is true if this {@code TabularType} is
     * <em>assignable from</em> {@link TabularData#getTabularType()
     * td.getTabularType()}, as defined in {@link
     * CompositeType#isValue CompositeType.isValue}.</p>
     *
     * @param obj the value whose open type is to be tested for
     * compatibility with this <code>TabularType</code> instance.
     *
     * @return <code>true</code> if <var>obj</var> is a value for this
     * tabular type, <code>false</code> otherwise.
     */
    public boolean isValue(Object obj) {

        // if obj is null or not a TabularData, return false
        //
        if (!(obj instanceof TabularData))
            return false;

        // if obj is not a TabularData, return false
        //
        TabularData value = (TabularData) obj;
        TabularType valueType = value.getTabularType();
        return isAssignableFrom(valueType);
    }

    @Override
    boolean isAssignableFrom(OpenType<?> ot) {
        if (!(ot instanceof TabularType))
            return false;
        TabularType tt = (TabularType) ot;
        if (!getTypeName().equals(tt.getTypeName()) ||
                !getIndexNames().equals(tt.getIndexNames()))
            return false;
        return getRowType().isAssignableFrom(tt.getRowType());
    }


    /* *** Methods overriden from class Object *** */

    /**
     * Compares the specified <code>obj</code> parameter with this <code>TabularType</code> instance for equality.
     * <p>
     * Two <code>TabularType</code> instances are equal if and only if all of the following statements are true:
     * <ul>
     * <li>their type names are equal</li>
     * <li>their row types are equal</li>
     * <li>they use the same index names, in the same order</li>
     * </ul>
     * <br>&nbsp;
     * @param  obj  the object to be compared for equality with this <code>TabularType</code> instance;
     *              if <var>obj</var> is <code>null</code>, <code>equals</code> returns <code>false</code>.
     *
     * @return  <code>true</code> if the specified object is equal to this <code>TabularType</code> instance.
     */
    public boolean equals(Object obj) {

        // if obj is null, return false
        //
        if (obj == null) {
            return false;
        }

        // if obj is not a TabularType, return false
        //
        TabularType other;
        try {
            other = (TabularType) obj;
        } catch (ClassCastException e) {
            return false;
        }

        // Now, really test for equality between this TabularType instance and the other:
        //

        // their names should be equal
        if ( ! this.getTypeName().equals(other.getTypeName()) ) {
            return false;
        }

        // their row types should be equal
        if ( ! this.rowType.equals(other.rowType) ) {
            return false;
        }

        // their index names should be equal and in the same order (ensured by List.equals())
        if ( ! this.indexNames.equals(other.indexNames) ) {
            return false;
        }

        // All tests for equality were successfull
        //
        return true;
    }

    /**
     * Returns the hash code value for this <code>TabularType</code> instance.
     * <p>
     * The hash code of a <code>TabularType</code> instance is the sum of the hash codes
     * of all elements of information used in <code>equals</code> comparisons
     * (ie: name, row type, index names).
     * This ensures that <code> t1.equals(t2) </code> implies that <code> t1.hashCode()==t2.hashCode() </code>
     * for any two <code>TabularType</code> instances <code>t1</code> and <code>t2</code>,
     * as required by the general contract of the method
     * {@link Object#hashCode() Object.hashCode()}.
     * <p>
     * As <code>TabularType</code> instances are immutable, the hash code for this instance is calculated once,
     * on the first call to <code>hashCode</code>, and then the same value is returned for subsequent calls.
     *
     * @return  the hash code value for this <code>TabularType</code> instance
     */
    public int hashCode() {

        // Calculate the hash code value if it has not yet been done (ie 1st call to hashCode())
        //
        if (myHashCode == null) {
            int value = 0;
            value += this.getTypeName().hashCode();
            value += this.rowType.hashCode();
            for (String index : indexNames)
                value += index.hashCode();
            myHashCode = Integer.valueOf(value);
        }

        // return always the same hash code for this instance (immutable)
        //
        return myHashCode.intValue();
    }

    /**
     * Returns a string representation of this <code>TabularType</code> instance.
     * <p>
     * The string representation consists of the name of this class (ie <code>javax.management.openmbean.TabularType</code>),
     * the type name for this instance, the row type string representation of this instance,
     * and the index names of this instance.
     * <p>
     * As <code>TabularType</code> instances are immutable, the string representation for this instance is calculated once,
     * on the first call to <code>toString</code>, and then the same value is returned for subsequent calls.
     *
     * @return  a string representation of this <code>TabularType</code> instance
     */
    public String toString() {

        // Calculate the string representation if it has not yet been done (ie 1st call to toString())
        //
        if (myToString == null) {
            final StringBuilder result = new StringBuilder()
                .append(this.getClass().getName())
                .append("(name=")
                .append(getTypeName())
                .append(",rowType=")
                .append(rowType.toString())
                .append(",indexNames=(");
            String sep = "";
            for (String index : indexNames) {
                result.append(sep).append(index);
                sep = ",";
            }
            result.append("))");
            myToString = result.toString();
        }

        // return always the same string representation for this instance (immutable)
        //
        return myToString;
    }

}
