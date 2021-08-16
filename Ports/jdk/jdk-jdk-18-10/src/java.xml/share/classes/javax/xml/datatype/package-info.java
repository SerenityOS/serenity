/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 *
 * <p>
 * Defines XML/Java Type Mappings.
 *
 * <p>
 * This API provides XML/Java type mappings.
 *
 * <p>
 * The following XML standards apply:
 *
 * <ul>
 *     <li><a href="http://www.w3.org/TR/xmlschema-2/#dateTime">
 *             W3C XML Schema 1.0 Part 2, Section 3.2.7-14</a>
 *     </li>
 *     <li><a href="http://www.w3.org/TR/xpath-datamodel#dt-dayTimeDuration">
 *             XQuery 1.0 and XPath 2.0 Data Model, xdt:dayTimeDuration</a>
 *     </li>
 *     <li><a href="http://www.w3.org/TR/xpath-datamodel#dt-yearMonthDuration">
 *             XQuery 1.0 and XPath 2.0 Data Model, xdt:yearMonthDuration</a>
 *     </li>
 * </ul>
 *
 * <hr>
 *
 * <table class="striped">
 *     <caption> W3C XML Schema/Java Type Mappings</caption>
 *     <thead>
 *         <tr>
 *             <th scope="col">W3C XML Schema Data Type</th>
 *             <th scope="col">Java Data Type</th>
 *         </tr>
 *     </thead>
 *
 *     <tbody>
 *         <tr>
 *             <th scope="row">xs:date</th>
 *             <td>{@link javax.xml.datatype.XMLGregorianCalendar}</td>
 *         </tr>
 *         <tr>
 *             <th scope="row">xs:dateTime</th>
 *             <td>{@link javax.xml.datatype.XMLGregorianCalendar}</td>
 *         </tr>
 *         <tr>
 *             <th scope="row">xs:duration</th>
 *             <td>{@link javax.xml.datatype.Duration}</td>
 *         </tr>
 *         <tr>
 *             <th scope="row">xs:gDay</th>
 *             <td>{@link javax.xml.datatype.XMLGregorianCalendar}</td>
 *         </tr>
 *         <tr>
 *             <th scope="row">xs:gMonth </th>
 *             <td>{@link javax.xml.datatype.XMLGregorianCalendar}</td>
 *         </tr>
 *         <tr>
 *             <th scope="row">xs:gMonthDay</th>
 *             <td>{@link javax.xml.datatype.XMLGregorianCalendar}</td>
 *         </tr>
 *         <tr>
 *             <th scope="row">xs:gYear</th>
 *             <td>{@link javax.xml.datatype.XMLGregorianCalendar}</td>
 *         </tr>
 *         <tr>
 *             <th scope="row">xs:gYearMonth</th>
 *             <td>{@link javax.xml.datatype.XMLGregorianCalendar}</td>
 *         </tr>
 *         <tr>
 *             <th scope="row">xs:time</th>
 *             <td>{@link javax.xml.datatype.XMLGregorianCalendar}</td>
 *         </tr>
 *
 *     </tbody>
 * </table>
 *
 * <hr>
 *
 *
 * <table class="striped">
 *     <caption>XQuery and XPath/Java Type Mappings</caption>
 *     <thead>
 *         <tr>
 *             <th scope="col">XQuery 1.0 and XPath 2.0 Data Model</th>
 *             <th scope="col">Java Data Type</th>
 *         </tr>
 *     </thead>
 *
 *     <tbody>
 *         <tr>
 *             <th scope="row">xdt:dayTimeDuration</th>
 *             <td>{@link javax.xml.datatype.Duration}</td>
 *         </tr>
 *         <tr>
 *             <th scope="row">xdt:yearMonthDuration</th>
 *             <td>{@link javax.xml.datatype.Duration}</td>
 *         </tr>
 *     </tbody>
 * </table>
 *
 * <hr>
 *
 * <p>
 * W3C XML Schema data types that have a "<em>natural</em>" mapping to Java types are defined by
 * JSR 31: Java Architecture for XML Binding (JAXB) Specification, Binding XML Schema to Java Representations.
 * JAXB defined mappings for XML Schema built-in data types include:
 *
 * <ul>
 *     <li>xs:anySimpleType</li>
 *     <li>xs:base64Binary</li>
 *     <li>xs:boolean</li>
 *     <li>xs:byte</li>
 *     <li>xs:decimal</li>
 *     <li>xs:double</li>
 *     <li>xs:float</li>
 *     <li>xs:hexBinary</li>
 *     <li>xs:int</li>
 *     <li>xs:integer</li>
 *     <li>xs:long</li>
 *     <li>xs:QName</li>
 *     <li>xs:short</li>
 *     <li>xs:string</li>
 *     <li>xs:unsignedByte</li>
 *     <li>xs:unsignedInt</li>
 *     <li>xs:unsignedShort</li>
 * </ul>
 *
 * @author Jeff Suttor
 * @see <a href="http://www.w3.org/TR/xmlschema-2/#dateTime">
 *             W3C XML Schema 1.0 Part 2, Section 3.2.7-14</a>
 * @see <a href="http://www.w3.org/TR/xpath-datamodel#dt-dayTimeDuration">
 *             XQuery 1.0 and XPath 2.0 Data Model, xdt:dayTimeDuration</a>
 * @see <a href="http://www.w3.org/TR/xpath-datamodel#dt-yearMonthDuration">
 *             XQuery 1.0 and XPath 2.0 Data Model, xdt:yearMonthDuration</a>
 * @since 1.5
 */

package javax.xml.datatype;
