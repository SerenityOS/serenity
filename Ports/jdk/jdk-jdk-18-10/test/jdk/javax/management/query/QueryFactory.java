/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;

import javax.management.Query;
import javax.management.QueryExp;
import javax.management.ValueExp;

/**
 * Class used for building QueryExp instances of all every possible type
 * in terms of JMX API members; note that several JMX classes are private
 * and appears in the JDK API only by their serial form.
 * Comments in each case of the big switch in method getQuery() details which
 * API member we cover with a given query.
 */
public class QueryFactory extends QueryData {

    private String mbeanClassName = "";
    private String primitiveIntAttName = "IntAtt";
    private String primitiveLongAttName = "LongAtt";
    private String integerAttName = "IntegerAtt";
    private String primitiveBooleanAttName = "BooleanAtt";
    private String primitiveDoubleAttName = "DoubleAtt";
    private String primitiveFloatAttName = "FloatAtt";
    private String stringAttName = "StringAtt";
    private ArrayList<QueryExp> queries = new ArrayList<QueryExp>();

    /**
     * Creates a new instance of QueryFactory.
     * The name is the fully qualified class name of an MBean.
     * There is severe constraints on that MBean that must:
     * <ul>
     * <li>extend QueryData in order to inherit attribute values.
     * <li>define a RW attribute IntAtt of type int
     * initialized to QueryData.longValue
     * <li>define a RW attribute LongAtt of type long
     * initialized to QueryData.intValue
     * <li>define a RW attribute IntegerAtt of type Integer
     * initialized to QueryData.integerValue
     * <li>define a RW attribute BooleanAtt of type boolean
     * initialized to QueryData.booleanValue
     * <li>define a RW attribute DoubleAtt of type double
     * initialized to QueryData.doubleValue
     * <li>define a RW attribute FloatAtt of type float
     * initialized to QueryData.floatValue
     * <li>define a RW attribute StringAtt of type String
     * initialized to QueryData.stringValue
     * </ul>
     */
    public QueryFactory(String name) {
        this.mbeanClassName = name;
    }

    /**
     * Returns the highest index value the method getQuery supports.
     * WARNING : returns 0 if buildQueries haven't been called first !
     */
    public int getSize() {
        return queries.size();
    }

    /**
     * Populates an ArrayList of QueryExp.
     * Lowest index is 1.
     * Highest index is returned by getSize().
     * <br>The queries numbered 1 to 23 allow to cover all the underlying
     * Java classes of the JMX API used to build queries.
     */
    public void buildQueries() {
        if ( queries.size() == 0 ) {
            int smallerIntValue = intValue - 1;
            int biggerIntValue = intValue + 1;

            // case 1:
            // True if the MBean is of class mbeanClassName
            // We cover javax.management.InstanceOfQueryExp
            queries.add(Query.isInstanceOf(Query.value(mbeanClassName)));

            // case 2:
            // True if the MBean is of class mbeanClassName
            // We cover javax.management.MatchQueryExp and
            // javax.management.ClassAttributeValueExp
            queries.add(Query.match(Query.classattr(),
                    Query.value(mbeanClassName)));

            // case 3:
            // True if an attribute named primitiveIntAttName of type int has
            // the value intValue
            // We cover javax.management.BinaryRelQueryExp with
            // a relOp equal to EQ and javax.management.NumericValueExp
            queries.add(Query.eq(Query.attr(primitiveIntAttName),
                    Query.value(intValue)));

            // case 4:
            // True if an attribute named primitiveLongAttName of type long has
            // the value longValue
            // We cover javax.management.BinaryRelQueryExp with
            // a relOp equal to EQ and javax.management.NumericValueExp
            queries.add(Query.eq(Query.attr(primitiveLongAttName),
                    Query.value(longValue)));

            // case 5:
            // True if an attribute named primitiveDoubleAttName of type double
            // has the value doubleValue
            // We cover javax.management.BinaryRelQueryExp with
            // a relOp equal to EQ and javax.management.NumericValueExp
            queries.add(Query.eq(Query.attr(primitiveDoubleAttName),
                    Query.value(doubleValue)));

            // case 6:
            // True if an attribute named primitiveFloatAttName of type float
            // has the value floatValue
            // We cover javax.management.BinaryRelQueryExp with
            // a relOp equal to EQ and javax.management.NumericValueExp
            queries.add(Query.eq(Query.attr(primitiveFloatAttName),
                    Query.value(floatValue)));

            // case 7:
            // True if an attribute named primitiveIntAttName of type int is
            // hold by an MBean of class mbeanClassName and has
            // the value intValue
            // We cover javax.management.QualifiedAttributeValueExp
            queries.add(Query.eq(Query.attr(mbeanClassName, primitiveIntAttName),
                    Query.value(intValue)));

            // case 8:
            // True if an attribute named stringAttName of type String has
            // the value stringValue
            // We cover javax.management.BinaryRelQueryExp with
            // a relOp equal to EQ and javax.management.StringValueExp
            queries.add(Query.eq(Query.attr(stringAttName),
                    Query.value(stringValue)));

            // case 9:
            // True if an attribute named integerAttName of type Integer has
            // the value integerValue
            // We cover javax.management.BinaryRelQueryExp with
            // a relOp equal to EQ and javax.management.NumericValueExp
            queries.add(Query.eq(Query.attr(integerAttName),
                    Query.value(integerValue)));

            // case 10:
            // True if an attribute named primitiveBooleanAttName of type boolean
            // has the value booleanValue
            // We cover javax.management.BinaryRelQueryExp with
            // a relOp equal to EQ and javax.management.BooleanValueExp
            queries.add(Query.eq(Query.attr(primitiveBooleanAttName),
                    Query.value(booleanValue)));

            // case 11:
            // True if an attribute named primitiveIntAttName of type int has
            // not the value smallerIntValue
            // We cover javax.management.NotQueryExp
            queries.add(Query.not(Query.eq(Query.attr(primitiveIntAttName),
                    Query.value(smallerIntValue))));

            // case 12:
            // True if either
            // an attribute named primitiveIntAttName of type int has
            // the value intValue
            // or
            // an attribute named primitiveLongAttName of type long has
            // the value longValue
            // We cover javax.management.OrQueryExp
            queries.add(Query.or(
                    Query.eq(Query.attr(primitiveIntAttName),
                    Query.value(intValue)),
                    Query.eq(Query.attr(primitiveLongAttName),
                    Query.value(longValue))));

            // case 13:
            // True if
            // an attribute named primitiveIntAttName of type int has
            // the value intValue
            // and
            // an attribute named primitiveLongAttName of type long has
            // the value longValue
            // We cover javax.management.AndQueryExp
            queries.add(Query.and(
                    Query.eq(Query.attr(primitiveIntAttName),
                    Query.value(intValue)),
                    Query.eq(Query.attr(primitiveLongAttName),
                    Query.value(longValue))));

            // case 14:
            // True if an attribute named primitiveIntAttName of type int has
            // the value intValue
            // We cover javax.management.InQueryExp
            ValueExp[] inArray = {Query.value(intValue)};
            queries.add(Query.in(Query.attr(primitiveIntAttName), inArray));

            // case 15:
            // True if an attribute named primitiveIntAttName of type int has
            // its value in between smallerIntValue and biggerIntValue
            // We cover javax.management.BetweenRelQueryExp
            queries.add(Query.between(Query.attr(primitiveIntAttName),
                    Query.value(smallerIntValue),
                    Query.value(biggerIntValue)));

            // case 16:
            // True if an attribute named primitiveIntAttName of type int has
            // a value greater than smallerIntValue
            // We cover javax.management.BinaryRelQueryExp with
            // a relOp equal to GT
            queries.add(Query.gt(Query.attr(primitiveIntAttName),
                    Query.value(smallerIntValue)));

            // case 17:
            // True if an attribute named primitiveIntAttName of type int has
            // a value greater or equal to smallerIntValue
            // We cover javax.management.BinaryRelQueryExp with
            // a relOp equal to GE
            queries.add(Query.geq(Query.attr(primitiveIntAttName),
                    Query.value(smallerIntValue)));

            // case 18:
            // True if an attribute named primitiveIntAttName of type int has
            // a value smaller than biggerIntValue
            // We cover javax.management.BinaryRelQueryExp with
            // a relOp equal to LT
            queries.add(Query.lt(Query.attr(primitiveIntAttName),
                    Query.value(biggerIntValue)));

            // case 19:
            // True if an attribute named primitiveIntAttName of type int has
            // a value smaller or equal to biggerIntValue
            // We cover javax.management.BinaryRelQueryExp with
            // a relOp equal to LE
            queries.add(Query.leq(Query.attr(primitiveIntAttName),
                    Query.value(biggerIntValue)));

            // case 20:
            // True if an attribute named primitiveIntAttName of type int has
            // a value equal to intValue minus zero
            // We cover javax.management.BinaryRelQueryExp with
            // a relOp equal to MINUS
            queries.add(Query.eq(Query.attr(primitiveIntAttName),
                    Query.minus(Query.value(intValue), Query.value(0))));

            // case 21:
            // True if an attribute named primitiveIntAttName of type int has
            // a value equal to intValue plus zero
            // We cover javax.management.BinaryRelQueryExp with
            // a relOp equal to PLUS
            queries.add(Query.eq(Query.attr(primitiveIntAttName),
                    Query.plus(Query.value(intValue), Query.value(0))));

            // case 22:
            // True if an attribute named primitiveIntAttName of type int has
            // a value equal to intValue divided by one
            // We cover javax.management.BinaryRelQueryExp with
            // a relOp equal to DIV
            queries.add(Query.eq(Query.attr(primitiveIntAttName),
                    Query.div(Query.value(intValue), Query.value(1))));

            // case 23:
            // True if an attribute named primitiveIntAttName of type int has
            // a value equal to intValue multiplicated by one
            // We cover javax.management.BinaryRelQueryExp with
            // a relOp equal to TIMES
            queries.add(Query.eq(Query.attr(primitiveIntAttName),
                    Query.times(Query.value(intValue), Query.value(1))));

            // case 24:
            // That query is a complex one that combines within a big AND
            // queries with index 2 to 23 inclusive. But because a List is
            // zero based, we must decrement all indexes by 1 when retrieving
            // any previously stored query.
            QueryExp q2_3 = Query.and(queries.get(2-1), queries.get(3-1));
            QueryExp q4_5 = Query.and(queries.get(4-1), queries.get(5-1));
            QueryExp q6_7 = Query.and(queries.get(6-1), queries.get(7-1));
            QueryExp q8_9 = Query.and(queries.get(8-1), queries.get(9-1));
            QueryExp q10_11 = Query.and(queries.get(10-1), queries.get(11-1));
            QueryExp q12_13 = Query.and(queries.get(12-1), queries.get(13-1));
            QueryExp q14_15 = Query.and(queries.get(14-1), queries.get(15-1));
            QueryExp q16_17 = Query.and(queries.get(16-1), queries.get(17-1));
            QueryExp q18_19 = Query.and(queries.get(18-1), queries.get(19-1));
            QueryExp q20_21 = Query.and(queries.get(20-1), queries.get(21-1));
            QueryExp q22_23 = Query.and(queries.get(22-1), queries.get(23-1));
            QueryExp q2_5 = Query.and(q2_3, q4_5);
            QueryExp q6_9 = Query.and(q6_7, q8_9);
            QueryExp q10_13 = Query.and(q10_11, q12_13);
            QueryExp q14_17 = Query.and(q14_15, q16_17);
            QueryExp q18_21 = Query.and(q18_19, q20_21);
            QueryExp q2_9 = Query.and(q2_5, q6_9);
            QueryExp q10_17 = Query.and(q10_13, q14_17);
            QueryExp q18_23 = Query.and(q18_21, q22_23);
            QueryExp q2_17 = Query.and(q2_9, q10_17);
            queries.add(Query.and(q2_17, q18_23));

            // case 25:
            // Complex query mixing AND and OR.
            queries.add(Query.or(q6_9, q18_23));
        }
    }

    /**
     * Returns a QueryExp taken is the ArrayList populated by buildQueries().
     * Lowest index is 1.
     * Highest index is returned by getSize().
     * <br>The queries numbered 1 to 23 allow to cover all the underlying
     * Java classes of the JMX API used to build queries.
     */
    public QueryExp getQuery(int index) {
        return queries.get(index - 1);
    }
}
