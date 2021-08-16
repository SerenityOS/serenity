/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file and, per its terms, should not be removed:
 *
 * Copyright (c) 2004 World Wide Web Consortium,
 *
 * (Massachusetts Institute of Technology, European Research Consortium for
 * Informatics and Mathematics, Keio University). All Rights Reserved. This
 * work is distributed under the W3C(r) Software License [1] in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * [1] http://www.w3.org/Consortium/Legal/2002/copyright-software-20021231
 */

package org.w3c.dom;

/**
 *  The <code>TypeInfo</code> interface represents a type referenced from
 * <code>Element</code> or <code>Attr</code> nodes, specified in the schemas
 * associated with the document. The type is a pair of a namespace URI and
 * name properties, and depends on the document's schema.
 * <p> If the document's schema is an XML DTD [<a href='http://www.w3.org/TR/2004/REC-xml-20040204'>XML 1.0</a>], the values
 * are computed as follows:
 * <ul>
 * <li> If this type is referenced from an
 * <code>Attr</code> node, <code>typeNamespace</code> is
 * <code>"http://www.w3.org/TR/REC-xml"</code> and <code>typeName</code>
 * represents the <b>[attribute type]</b> property in the [<a href='http://www.w3.org/TR/2004/REC-xml-infoset-20040204/'>XML Information Set</a>]
 * . If there is no declaration for the attribute, <code>typeNamespace</code>
 *  and <code>typeName</code> are <code>null</code>.
 * </li>
 * <li> If this type is
 * referenced from an <code>Element</code> node, <code>typeNamespace</code>
 * and <code>typeName</code> are <code>null</code>.
 * </li>
 * </ul>
 * <p> If the document's schema is an XML Schema [<a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/'>XML Schema Part 1</a>]
 * , the values are computed as follows using the post-schema-validation
 * infoset contributions (also called PSVI contributions):
 * <ul>
 * <li> If the <b>[validity]</b> property exists AND is <em>"invalid"</em> or <em>"notKnown"</em>: the {target namespace} and {name} properties of the declared type if
 * available, otherwise <code>null</code>.
 * <p ><b>Note:</b>  At the time of writing, the XML Schema specification does
 * not require exposing the declared type. Thus, DOM implementations might
 * choose not to provide type information if validity is not valid.
 * </li>
 * <li> If the <b>[validity]</b> property exists and is <em>"valid"</em>:
 * <ol>
 * <li> If <b>[member type definition]</b> exists:
 * <ol>
 * <li>If {name} is not absent, then expose {name} and {target
 * namespace} properties of the <b>[member type definition]</b> property;
 * </li>
 * <li>Otherwise, expose the namespace and local name of the
 * corresponding anonymous type name.
 * </li>
 * </ol>
 * </li>
 * <li> If the <b>[type definition]</b> property exists:
 * <ol>
 * <li>If {name} is not absent, then expose {name} and {target
 * namespace} properties of the <b>[type definition]</b> property;
 * </li>
 * <li>Otherwise, expose the namespace and local name of the
 * corresponding anonymous type name.
 * </li>
 * </ol>
 * </li>
 * <li> If the <b>[member type definition anonymous]</b> exists:
 * <ol>
 * <li>If it is false, then expose <b>[member type definition name]</b> and <b>[member type definition namespace]</b> properties;
 * </li>
 * <li>Otherwise, expose the namespace and local name of the
 * corresponding anonymous type name.
 * </li>
 * </ol>
 * </li>
 * <li> If the <b>[type definition anonymous]</b> exists:
 * <ol>
 * <li>If it is false, then expose <b>[type definition name]</b> and <b>[type definition namespace]</b> properties;
 * </li>
 * <li>Otherwise, expose the namespace and local name of the
 * corresponding anonymous type name.
 * </li>
 * </ol>
 * </li>
 * </ol>
 * </li>
 * </ul>
 * <p ><b>Note:</b>  Other schema languages are outside the scope of the W3C
 * and therefore should define how to represent their type systems using
 * <code>TypeInfo</code>.
 * <p>See also the <a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407'>Document Object Model (DOM) Level 3 Core Specification</a>.
 * @since 1.5, DOM Level 3
 */
public interface TypeInfo {
    /**
     *  The name of a type declared for the associated element or attribute,
     * or <code>null</code> if unknown.
     */
    public String getTypeName();

    /**
     *  The namespace of the type declared for the associated element or
     * attribute or <code>null</code> if the element does not have
     * declaration or if no namespace information is available.
     */
    public String getTypeNamespace();

    // DerivationMethods
    /**
     *  If the document's schema is an XML Schema [<a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/'>XML Schema Part 1</a>]
     * , this constant represents the derivation by <a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/#key-typeRestriction'>
     * restriction</a> if complex types are involved, or a <a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/#element-restriction'>
     * restriction</a> if simple types are involved.
     * <br>  The reference type definition is derived by restriction from the
     * other type definition if the other type definition is the same as the
     * reference type definition, or if the other type definition can be
     * reached recursively following the {base type definition} property
     * from the reference type definition, and all the <em>derivation methods</em> involved are restriction.
     */
    public static final int DERIVATION_RESTRICTION    = 0x00000001;
    /**
     *  If the document's schema is an XML Schema [<a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/'>XML Schema Part 1</a>]
     * , this constant represents the derivation by <a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/#key-typeExtension'>
     * extension</a>.
     * <br>  The reference type definition is derived by extension from the
     * other type definition if the other type definition can be reached
     * recursively following the {base type definition} property from the
     * reference type definition, and at least one of the <em>derivation methods</em> involved is an extension.
     */
    public static final int DERIVATION_EXTENSION      = 0x00000002;
    /**
     *  If the document's schema is an XML Schema [<a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/'>XML Schema Part 1</a>]
     * , this constant represents the <a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/#element-union'>
     * union</a> if simple types are involved.
     * <br> The reference type definition is derived by union from the other
     * type definition if there exists two type definitions T1 and T2 such
     * as the reference type definition is derived from T1 by
     * <code>DERIVATION_RESTRICTION</code> or
     * <code>DERIVATION_EXTENSION</code>, T2 is derived from the other type
     * definition by <code>DERIVATION_RESTRICTION</code>, T1 has {variety} <em>union</em>, and one of the {member type definitions} is T2. Note that T1 could be
     * the same as the reference type definition, and T2 could be the same
     * as the other type definition.
     */
    public static final int DERIVATION_UNION          = 0x00000004;
    /**
     *  If the document's schema is an XML Schema [<a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/'>XML Schema Part 1</a>]
     * , this constant represents the <a href='http://www.w3.org/TR/2001/REC-xmlschema-1-20010502/#element-list'>list</a>.
     * <br> The reference type definition is derived by list from the other
     * type definition if there exists two type definitions T1 and T2 such
     * as the reference type definition is derived from T1 by
     * <code>DERIVATION_RESTRICTION</code> or
     * <code>DERIVATION_EXTENSION</code>, T2 is derived from the other type
     * definition by <code>DERIVATION_RESTRICTION</code>, T1 has {variety} <em>list</em>, and T2 is the {item type definition}. Note that T1 could be the same as
     * the reference type definition, and T2 could be the same as the other
     * type definition.
     */
    public static final int DERIVATION_LIST           = 0x00000008;

    /**
     *  This method returns if there is a derivation between the reference
     * type definition, i.e. the <code>TypeInfo</code> on which the method
     * is being called, and the other type definition, i.e. the one passed
     * as parameters.
     * @param typeNamespaceArg  the namespace of the other type definition.
     * @param typeNameArg  the name of the other type definition.
     * @param derivationMethod  the type of derivation and conditions applied
     *   between two types, as described in the list of constants provided
     *   in this interface.
     * @return  If the document's schema is a DTD or no schema is associated
     *   with the document, this method will always return <code>false</code>
     *   .  If the document's schema is an XML Schema, the method will return
     *   <code>true</code> if the reference type definition is derived from
     *   the other type definition according to the derivation parameter. If
     *   the value of the parameter is <code>0</code> (no bit is set to
     *   <code>1</code> for the <code>derivationMethod</code> parameter),
     *   the method will return <code>true</code> if the other type
     *   definition can be reached by recursing any combination of {base
     *   type definition}, {item type definition}, or {member type
     *   definitions} from the reference type definition.
     */
    public boolean isDerivedFrom(String typeNamespaceArg,
                                 String typeNameArg,
                                 int derivationMethod);

}
