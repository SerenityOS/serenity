/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
package javax.xml.xpath;

import java.util.Objects;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;
/**
 * The {@code XPathEvaluationResult} interface represents the result of the
 * evaluation of an XPath expression within the context of a particular node.
 * The evaluation of an XPath expression can result in various result types as
 * defined in XML Path Language (XPath) Version 1.0.
 *
 * @param <T> the object type returned by the XPath evaluation.
 * @see <a href="http://www.w3.org/TR/xpath">XML Path Language (XPath) Version
 * 1.0</a>
 *
 * @since 9
 */
public interface XPathEvaluationResult<T> {

    /**
     * XPathResultType represents possible return types of an XPath evaluation.
     * Provided as an enum type, it allows the use of switch statement. At the
     * same time, a mapping is provided between the original QName types in
     * {@link XPathConstants} and class types used in the generic methods.
     */
    public static enum XPathResultType {
        /**
         * Any type that represents any of the 5 other types listed below.
         * Maps to {@link XPathEvaluationResult}.
         */
        ANY(new QName("http://www.w3.org/1999/XSL/Transform", "any"), XPathEvaluationResult.class),
        /**
         * The XPath 1.0 boolean data type. Maps to Java {@link Boolean}.
         */
        BOOLEAN(XPathConstants.BOOLEAN, Boolean.class),
        /**
         * The XPath 1.0 Number data type. Maps to Java {@link Number}. Of the
         * subtypes of Number, only Double, Integer and Long are required.
         */
        NUMBER(XPathConstants.NUMBER, Number.class),
        /**
         * The XPath 1.0 String data type. Maps to Java {@link String}.
         */
        STRING(XPathConstants.STRING, String.class),
        /**
         * The XPath 1.0 NodeSet data type. Maps to {@link XPathNodes}.
         */
        NODESET(XPathConstants.NODESET, XPathNodes.class),
        /**
         * The XPath 1.0 Node data type. Maps to {@link org.w3c.dom.Node}.
         */
        NODE(XPathConstants.NODE, Node.class);

        final QName qnameType;
        final Class<?> clsType;
        XPathResultType(QName qnameType, Class<?> clsType) {
            this.qnameType = qnameType;
            this.clsType = clsType;
        }

        /**
         * Compares this type to the specified class type.
         * @param clsType class type
         * @return true if the argument is not null and is a class type or accepted subtype that
         * matches that this type represents, false otherwise.
         */
        private boolean equalsClassType(Class<?> clsType) {
            if (Objects.nonNull(clsType) && this.clsType.isAssignableFrom(clsType)) {
                if (this.clsType == Number.class) {
                    return isAcceptedNumberSubType(clsType);
                }
                return true;
            }
            return false;
        }

        /**
         * Compares the specified class type to accepted subtypes of number.
         * @param clsType class type
         * @return true if class type is an accepted subtype of Number, false otherwise
         */
        private boolean isAcceptedNumberSubType(Class<?> clsType) {
            return clsType.isAssignableFrom(Double.class) ||
                    clsType.isAssignableFrom(Integer.class) ||
                    clsType.isAssignableFrom(Long.class);
        }

        /**
         * Returns the QName type as specified in {@link XPathConstants} that
         * corresponds to the specified class type.
         * @param clsType a class type that the enum type supports
         * @return the QName type that matches with the specified class type,
         * null if there is no match
         */
            static public QName getQNameType(Class<?> clsType) {
            for (XPathResultType type : XPathResultType.values()) {
                if (type.equalsClassType(clsType)) {
                    return type.qnameType;
                }
            }
            return null;
        }
    }

    /**
     * Return the result type as an enum specified by {@code XPathResultType}
     * @return the result type
     */
    public XPathResultType type();

    /**
     * Returns the value of the result as the type {@code <T>} specified for the class.
     *
     * @return The value of the result.
     */
    public T value();

}
