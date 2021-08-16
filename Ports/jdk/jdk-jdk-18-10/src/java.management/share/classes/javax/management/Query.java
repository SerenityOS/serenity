/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javax.management;


/**
 * <p>Constructs query object constraints.</p>
 *
 * <p>The MBean Server can be queried for MBeans that meet a particular
 * condition, using its {@link MBeanServer#queryNames queryNames} or
 * {@link MBeanServer#queryMBeans queryMBeans} method.  The {@link QueryExp}
 * parameter to the method can be any implementation of the interface
 * {@code QueryExp}, but it is usually best to obtain the {@code QueryExp}
 * value by calling the static methods in this class.  This is particularly
 * true when querying a remote MBean Server: a custom implementation of the
 * {@code QueryExp} interface might not be present in the remote MBean Server,
 * but the methods in this class return only standard classes that are
 * part of the JMX implementation.</p>
 *
 * <p>As an example, suppose you wanted to find all MBeans where the {@code
 * Enabled} attribute is {@code true} and the {@code Owner} attribute is {@code
 * "Duke"}. Here is how you could construct the appropriate {@code QueryExp} by
 * chaining together method calls:</p>
 *
 * <pre>
 * QueryExp query =
 *     Query.and(Query.eq(Query.attr("Enabled"), Query.value(true)),
 *               Query.eq(Query.attr("Owner"), Query.value("Duke")));
 * </pre>
 *
 * @since 1.5
 */
 public class Query extends Object   {


     /**
      * A code representing the {@link Query#gt} query.  This is chiefly
      * of interest for the serialized form of queries.
      */
     public static final int GT  = 0;

     /**
      * A code representing the {@link Query#lt} query.  This is chiefly
      * of interest for the serialized form of queries.
      */
     public static final int LT  = 1;

     /**
      * A code representing the {@link Query#geq} query.  This is chiefly
      * of interest for the serialized form of queries.
      */
     public static final int GE  = 2;

     /**
      * A code representing the {@link Query#leq} query.  This is chiefly
      * of interest for the serialized form of queries.
      */
     public static final int LE  = 3;

     /**
      * A code representing the {@link Query#eq} query.  This is chiefly
      * of interest for the serialized form of queries.
      */
     public static final int EQ  = 4;


     /**
      * A code representing the {@link Query#plus} expression.  This
      * is chiefly of interest for the serialized form of queries.
      */
     public static final int PLUS  = 0;

     /**
      * A code representing the {@link Query#minus} expression.  This
      * is chiefly of interest for the serialized form of queries.
      */
     public static final int MINUS = 1;

     /**
      * A code representing the {@link Query#times} expression.  This
      * is chiefly of interest for the serialized form of queries.
      */
     public static final int TIMES = 2;

     /**
      * A code representing the {@link Query#div} expression.  This is
      * chiefly of interest for the serialized form of queries.
      */
     public static final int DIV   = 3;


     /**
      * Basic constructor.
      */
     public Query() {
     }


     /**
      * Returns a query expression that is the conjunction of two other query
      * expressions.
      *
      * @param q1 A query expression.
      * @param q2 Another query expression.
      *
      * @return  The conjunction of the two arguments.  The returned object
      * will be serialized as an instance of the non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.AndQueryExp">
      * javax.management.AndQueryExp</a>.
      */
     public static QueryExp and(QueryExp q1, QueryExp q2)  {
         return new AndQueryExp(q1, q2);
     }

     /**
      * Returns a query expression that is the disjunction of two other query
      * expressions.
      *
      * @param q1 A query expression.
      * @param q2 Another query expression.
      *
      * @return  The disjunction of the two arguments.  The returned object
      * will be serialized as an instance of the non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.OrQueryExp">
      * javax.management.OrQueryExp</a>.
      */
     public static QueryExp or(QueryExp q1, QueryExp q2)  {
         return new OrQueryExp(q1, q2);
     }

     /**
      * Returns a query expression that represents a "greater than" constraint on
      * two values.
      *
      * @param v1 A value expression.
      * @param v2 Another value expression.
      *
      * @return A "greater than" constraint on the arguments.  The
      * returned object will be serialized as an instance of the
      * non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.BinaryRelQueryExp">
      * javax.management.BinaryRelQueryExp</a> with a {@code relOp} equal
      * to {@link #GT}.
      */
     public static QueryExp gt(ValueExp v1, ValueExp v2)  {
         return new BinaryRelQueryExp(GT, v1, v2);
     }

     /**
      * Returns a query expression that represents a "greater than or equal
      * to" constraint on two values.
      *
      * @param v1 A value expression.
      * @param v2 Another value expression.
      *
      * @return A "greater than or equal to" constraint on the
      * arguments.  The returned object will be serialized as an
      * instance of the non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.BinaryRelQueryExp">
      * javax.management.BinaryRelQueryExp</a> with a {@code relOp} equal
      * to {@link #GE}.
      */
     public static QueryExp geq(ValueExp v1, ValueExp v2)  {
         return new BinaryRelQueryExp(GE, v1, v2);
     }

     /**
      * Returns a query expression that represents a "less than or equal to"
      * constraint on two values.
      *
      * @param v1 A value expression.
      * @param v2 Another value expression.
      *
      * @return A "less than or equal to" constraint on the arguments.
      * The returned object will be serialized as an instance of the
      * non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.BinaryRelQueryExp">
      * javax.management.BinaryRelQueryExp</a> with a {@code relOp} equal
      * to {@link #LE}.
      */
     public static QueryExp leq(ValueExp v1, ValueExp v2)  {
         return new BinaryRelQueryExp(LE, v1, v2);
     }

     /**
      * Returns a query expression that represents a "less than" constraint on
      * two values.
      *
      * @param v1 A value expression.
      * @param v2 Another value expression.
      *
      * @return A "less than" constraint on the arguments.  The
      * returned object will be serialized as an instance of the
      * non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.BinaryRelQueryExp">
      * javax.management.BinaryRelQueryExp</a> with a {@code relOp} equal
      * to {@link #LT}.
      */
     public static QueryExp lt(ValueExp v1, ValueExp v2)  {
         return new BinaryRelQueryExp(LT, v1, v2);
     }

     /**
      * Returns a query expression that represents an equality constraint on
      * two values.
      *
      * @param v1 A value expression.
      * @param v2 Another value expression.
      *
      * @return A "equal to" constraint on the arguments.  The
      * returned object will be serialized as an instance of the
      * non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.BinaryRelQueryExp">
      * javax.management.BinaryRelQueryExp</a> with a {@code relOp} equal
      * to {@link #EQ}.
      */
     public static QueryExp eq(ValueExp v1, ValueExp v2)  {
         return new BinaryRelQueryExp(EQ, v1, v2);
     }

     /**
      * Returns a query expression that represents the constraint that one
      * value is between two other values.
      *
      * @param v1 A value expression that is "between" v2 and v3.
      * @param v2 Value expression that represents a boundary of the constraint.
      * @param v3 Value expression that represents a boundary of the constraint.
      *
      * @return The constraint that v1 lies between v2 and v3.  The
      * returned object will be serialized as an instance of the
      * non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.BetweenQueryExp">
      * javax.management.BetweenQueryExp</a>.
      */
     public static QueryExp between(ValueExp v1, ValueExp v2, ValueExp v3) {
         return new BetweenQueryExp(v1, v2, v3);
     }

     /**
      * Returns a query expression that represents a matching constraint on
      * a string argument. The matching syntax is consistent with file globbing:
      * supports "<code>?</code>", "<code>*</code>", "<code>[</code>",
      * each of which may be escaped with "<code>\</code>";
      * character classes may use "<code>!</code>" for negation and
      * "<code>-</code>" for range.
      * (<code>*</code> for any character sequence,
      * <code>?</code> for a single arbitrary character,
      * <code>[...]</code> for a character sequence).
      * For example: <code>a*b?c</code> would match a string starting
      * with the character <code>a</code>, followed
      * by any number of characters, followed by a <code>b</code>,
      * any single character, and a <code>c</code>.
      *
      * @param a An attribute expression
      * @param s A string value expression representing a matching constraint
      *
      * @return A query expression that represents the matching
      * constraint on the string argument.  The returned object will
      * be serialized as an instance of the non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.MatchQueryExp">
      * javax.management.MatchQueryExp</a>.
      */
     public static QueryExp match(AttributeValueExp a, StringValueExp s)  {
         return new MatchQueryExp(a, s);
     }

     /**
      * <p>Returns a new attribute expression.  See {@link AttributeValueExp}
      * for a detailed description of the semantics of the expression.</p>
      *
      * <p>Evaluating this expression for a given
      * <code>objectName</code> includes performing {@link
      * MBeanServer#getAttribute MBeanServer.getAttribute(objectName,
      * name)}.</p>
      *
      * @param name The name of the attribute.
      *
      * @return An attribute expression for the attribute named {@code name}.
      */
     public static AttributeValueExp attr(String name)  {
         return new AttributeValueExp(name);
     }

     /**
      * <p>Returns a new qualified attribute expression.</p>
      *
      * <p>Evaluating this expression for a given
      * <code>objectName</code> includes performing {@link
      * MBeanServer#getObjectInstance
      * MBeanServer.getObjectInstance(objectName)} and {@link
      * MBeanServer#getAttribute MBeanServer.getAttribute(objectName,
      * name)}.</p>
      *
      * @param className The name of the class possessing the attribute.
      * @param name The name of the attribute.
      *
      * @return An attribute expression for the attribute named name.
      * The returned object will be serialized as an instance of the
      * non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.QualifiedAttributeValueExp">
      * javax.management.QualifiedAttributeValueExp</a>.
      */
     public static AttributeValueExp attr(String className, String name)  {
         return new QualifiedAttributeValueExp(className, name);
     }

     /**
      * <p>Returns a new class attribute expression which can be used in any
      * Query call that expects a ValueExp.</p>
      *
      * <p>Evaluating this expression for a given
      * <code>objectName</code> includes performing {@link
      * MBeanServer#getObjectInstance
      * MBeanServer.getObjectInstance(objectName)}.</p>
      *
      * @return A class attribute expression.  The returned object
      * will be serialized as an instance of the non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.ClassAttributeValueExp">
      * javax.management.ClassAttributeValueExp</a>.
      */
     public static AttributeValueExp classattr()  {
         return new ClassAttributeValueExp();
     }

     /**
      * Returns a constraint that is the negation of its argument.
      *
      * @param queryExp The constraint to negate.
      *
      * @return A negated constraint.  The returned object will be
      * serialized as an instance of the non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.NotQueryExp">
      * javax.management.NotQueryExp</a>.
      */
     public static QueryExp not(QueryExp queryExp)  {
         return new NotQueryExp(queryExp);
     }

     /**
      * Returns an expression constraining a value to be one of an explicit list.
      *
      * @param val A value to be constrained.
      * @param valueList An array of ValueExps.
      *
      * @return A QueryExp that represents the constraint.  The
      * returned object will be serialized as an instance of the
      * non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.InQueryExp">
      * javax.management.InQueryExp</a>.
      */
     public static QueryExp in(ValueExp val, ValueExp valueList[])  {
         return new InQueryExp(val, valueList);
     }

     /**
      * Returns a new string expression.
      *
      * @param val The string value.
      *
      * @return  A ValueExp object containing the string argument.
      */
     public static StringValueExp value(String val)  {
         return new StringValueExp(val);
     }

     /**
      * Returns a numeric value expression that can be used in any Query call
      * that expects a ValueExp.
      *
      * @param val An instance of Number.
      *
      * @return A ValueExp object containing the argument.  The
      * returned object will be serialized as an instance of the
      * non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.NumericValueExp">
      * javax.management.NumericValueExp</a>.
      */
     public static ValueExp value(Number val)  {
         return new NumericValueExp(val);
     }

     /**
      * Returns a numeric value expression that can be used in any Query call
      * that expects a ValueExp.
      *
      * @param val An int value.
      *
      * @return A ValueExp object containing the argument.  The
      * returned object will be serialized as an instance of the
      * non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.NumericValueExp">
      * javax.management.NumericValueExp</a>.
      */
     public static ValueExp value(int val)  {
         return new NumericValueExp((long) val);
     }

     /**
      * Returns a numeric value expression that can be used in any Query call
      * that expects a ValueExp.
      *
      * @param val A long value.
      *
      * @return A ValueExp object containing the argument.  The
      * returned object will be serialized as an instance of the
      * non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.NumericValueExp">
      * javax.management.NumericValueExp</a>.
      */
     public static ValueExp value(long val)  {
         return new NumericValueExp(val);
     }

     /**
      * Returns a numeric value expression that can be used in any Query call
      * that expects a ValueExp.
      *
      * @param val A float value.
      *
      * @return A ValueExp object containing the argument.  The
      * returned object will be serialized as an instance of the
      * non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.NumericValueExp">
      * javax.management.NumericValueExp</a>.
      */
     public static ValueExp value(float val)  {
         return new NumericValueExp((double) val);
     }

     /**
      * Returns a numeric value expression that can be used in any Query call
      * that expects a ValueExp.
      *
      * @param val A double value.
      *
      * @return  A ValueExp object containing the argument.  The
      * returned object will be serialized as an instance of the
      * non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.NumericValueExp">
      * javax.management.NumericValueExp</a>.
      */
     public static ValueExp value(double val)  {
         return new NumericValueExp(val);
     }

     /**
      * Returns a boolean value expression that can be used in any Query call
      * that expects a ValueExp.
      *
      * @param val A boolean value.
      *
      * @return A ValueExp object containing the argument.  The
      * returned object will be serialized as an instance of the
      * non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.BooleanValueExp">
      * javax.management.BooleanValueExp</a>.
      */
     public static ValueExp value(boolean val)  {
         return new BooleanValueExp(val);
     }

     /**
      * Returns a binary expression representing the sum of two numeric values,
      * or the concatenation of two string values.
      *
      * @param value1 The first '+' operand.
      * @param value2 The second '+' operand.
      *
      * @return A ValueExp representing the sum or concatenation of
      * the two arguments.  The returned object will be serialized as
      * an instance of the non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.BinaryOpValueExp">
      * javax.management.BinaryOpValueExp</a> with an {@code op} equal to
      * {@link #PLUS}.
      */
     public static ValueExp plus(ValueExp value1, ValueExp value2) {
         return new BinaryOpValueExp(PLUS, value1, value2);
     }

     /**
      * Returns a binary expression representing the product of two numeric values.
      *
      *
      * @param value1 The first '*' operand.
      * @param value2 The second '*' operand.
      *
      * @return A ValueExp representing the product.  The returned
      * object will be serialized as an instance of the non-public
      * class
      * <a href="{@docRoot}/serialized-form.html#javax.management.BinaryOpValueExp">
      * javax.management.BinaryOpValueExp</a> with an {@code op} equal to
      * {@link #TIMES}.
      */
     public static ValueExp times(ValueExp value1,ValueExp value2) {
         return new BinaryOpValueExp(TIMES, value1, value2);
     }

     /**
      * Returns a binary expression representing the difference between two numeric
      * values.
      *
      * @param value1 The first '-' operand.
      * @param value2 The second '-' operand.
      *
      * @return A ValueExp representing the difference between two
      * arguments.  The returned object will be serialized as an
      * instance of the non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.BinaryOpValueExp">
      * javax.management.BinaryOpValueExp</a> with an {@code op} equal to
      * {@link #MINUS}.
      */
     public static ValueExp minus(ValueExp value1, ValueExp value2) {
         return new BinaryOpValueExp(MINUS, value1, value2);
     }

     /**
      * Returns a binary expression representing the quotient of two numeric
      * values.
      *
      * @param value1 The first '/' operand.
      * @param value2 The second '/' operand.
      *
      * @return A ValueExp representing the quotient of two arguments.
      * The returned object will be serialized as an instance of the
      * non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.BinaryOpValueExp">
      * javax.management.BinaryOpValueExp</a> with an {@code op} equal to
      * {@link #DIV}.
      */
     public static ValueExp div(ValueExp value1, ValueExp value2) {
         return new BinaryOpValueExp(DIV, value1, value2);
     }

     /**
      * Returns a query expression that represents a matching constraint on
      * a string argument. The value must start with the given literal string
      * value.
      *
      * @param a An attribute expression.
      * @param s A string value expression representing the beginning of the
      * string value.
      *
      * @return The constraint that a matches s.  The returned object
      * will be serialized as an instance of the non-public class
      *
      * <a href="{@docRoot}/serialized-form.html#javax.management.MatchQueryExp">
      * javax.management.MatchQueryExp</a>.
      */
     public static QueryExp initialSubString(AttributeValueExp a, StringValueExp s)  {
         return new MatchQueryExp(a,
             new StringValueExp(escapeString(s.getValue()) + "*"));
     }

     /**
      * Returns a query expression that represents a matching constraint on
      * a string argument. The value must contain the given literal string
      * value.
      *
      * @param a An attribute expression.
      * @param s A string value expression representing the substring.
      *
      * @return The constraint that a matches s.  The returned object
      * will be serialized as an instance of the non-public class
      *
      * <a href="{@docRoot}/serialized-form.html#javax.management.MatchQueryExp">
      * javax.management.MatchQueryExp</a>.
      */
     public static QueryExp anySubString(AttributeValueExp a, StringValueExp s) {
         return new MatchQueryExp(a,
             new StringValueExp("*" + escapeString(s.getValue()) + "*"));
     }

     /**
      * Returns a query expression that represents a matching constraint on
      * a string argument. The value must end with the given literal string
      * value.
      *
      * @param a An attribute expression.
      * @param s A string value expression representing the end of the string
      * value.
      *
      * @return The constraint that a matches s.  The returned object
      * will be serialized as an instance of the non-public class
      *
      * <a href="{@docRoot}/serialized-form.html#javax.management.MatchQueryExp">
      * javax.management.MatchQueryExp</a>.
      */
     public static QueryExp finalSubString(AttributeValueExp a, StringValueExp s) {
         return new MatchQueryExp(a,
             new StringValueExp("*" + escapeString(s.getValue())));
     }

     /**
      * Returns a query expression that represents an inheritance constraint
      * on an MBean class.
      * <p>Example: to find MBeans that are instances of
      * {@link NotificationBroadcaster}, use
      * {@code Query.isInstanceOf(Query.value(NotificationBroadcaster.class.getName()))}.
      * </p>
      * <p>Evaluating this expression for a given
      * <code>objectName</code> includes performing {@link
      * MBeanServer#isInstanceOf MBeanServer.isInstanceOf(objectName,
      * ((StringValueExp)classNameValue.apply(objectName)).getValue()}.</p>
      *
      * @param classNameValue The {@link StringValueExp} returning the name
      *        of the class of which selected MBeans should be instances.
      * @return a query expression that represents an inheritance
      * constraint on an MBean class.  The returned object will be
      * serialized as an instance of the non-public class
      * <a href="{@docRoot}/serialized-form.html#javax.management.InstanceOfQueryExp">
      * javax.management.InstanceOfQueryExp</a>.
      * @since 1.6
      */
     public static QueryExp isInstanceOf(StringValueExp classNameValue) {
        return new InstanceOfQueryExp(classNameValue);
     }

     /**
      * Utility method to escape strings used with
      * Query.{initial|any|final}SubString() methods.
      */
     private static String escapeString(String s) {
         if (s == null)
             return null;
         s = s.replace("\\", "\\\\");
         s = s.replace("*", "\\*");
         s = s.replace("?", "\\?");
         s = s.replace("[", "\\[");
         return s;
     }
 }
