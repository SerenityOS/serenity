/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * Represents values that can be passed as arguments to
 * relational expressions. Strings, numbers, attributes are valid values
 * and should be represented by implementations of <CODE>ValueExp</CODE>.
 *
 * @since 1.5
 */
/*
  We considered generifying this interface as ValueExp<T>, where T is
  the Java type that this expression generates.  This allows some additional
  checking in the various methods of the Query class, but in practice
  not much.  Typically you have something like
  Query.lt(Query.attr("A"), Query.value(5)).  We can arrange for Query.value
  to have type ValueExp<Integer> (or maybe ValueExp<Long> or ValueExp<Number>)
  but for Query.attr we can't do better than ValueExp<?> or plain ValueExp.
  So even though we could define Query.lt as:
  QueryExp <T> lt(ValueExp<T> v1, ValueExp<T> v2)
  and thus prevent comparing a
  number against a string, in practice the first ValueExp will almost always
  be a Query.attr so this check serves no purpose.  You would have to
  write Query.<Number>attr("A"), for example, which would be awful.  And,
  if you wrote Query.<Integer>attr("A") you would then discover that you
  couldn't compare it against Query.value(5) if the latter is defined as
  ValueExp<Number>, or against Query.value(5L) if it is defined as
  ValueExp<Integer>.

  Worse, for Query.in we would like to define:
  QueryExp <T> in(ValueExp<T> val, ValueExp<T>[] valueList)
  but this is unusable because you cannot write
  "new ValueExp<Integer>[] {...}" (the compiler forbids it).

  The few mistakes you might catch with this generification certainly
  wouldn't justify the hassle of modifying user code to get the checks
  to be made and the "unchecked" warnings that would arise if it
  wasn't so modified.

  We could reconsider this if the Query methods were augmented, for example
  with:
  AttributeValueExp<Number> numberAttr(String name);
  AttributeValueExp<String> stringAttr(String name);
  AttributeValueExp<Boolean> booleanAttr(String name);
  QueryExp <T> in(ValueExp<T> val, Set<ValueExp<T>> valueSet).
  But it's not really clear what numberAttr should do if it finds that the
  attribute is not in fact a Number.
 */
public interface ValueExp extends java.io.Serializable {

    /**
     * Applies the ValueExp on a MBean.
     *
     * @param name The name of the MBean on which the ValueExp will be applied.
     *
     * @return  The <CODE>ValueExp</CODE>.
     *
     * @throws BadStringOperationException when an invalid string
     * operation is passed to a method for constructing a query
     * @throws BadBinaryOpValueExpException when an invalid expression
     * is passed to a method for constructing a query
     * @throws BadAttributeValueExpException when an invalid MBean
     * attribute is passed to a query constructing method
     * @throws InvalidApplicationException when an invalid apply is attempted
     */
    public ValueExp apply(ObjectName name)
            throws BadStringOperationException, BadBinaryOpValueExpException,
                   BadAttributeValueExpException, InvalidApplicationException;

    /**
     * Sets the MBean server on which the query is to be performed.
     *
     * @param s The MBean server on which the query is to be performed.
     *
     * @deprecated This method is not needed because a
     * <code>ValueExp</code> can access the MBean server in which it
     * is being evaluated by using {@link QueryEval#getMBeanServer()}.
     */
    @Deprecated
    public  void setMBeanServer(MBeanServer s) ;
}
