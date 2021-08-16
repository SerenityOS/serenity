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

package javax.print.attribute.standard;

import java.io.Serial;

import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;

/**
 * Class {@code ReferenceUriSchemesSupported} is a printing attribute class an
 * enumeration, that indicates a "URI scheme," such as "http:" or "ftp:", that a
 * printer can use to retrieve print data stored at a {@code URI} location. If a
 * printer supports doc flavors with a print data representation class of
 * {@code "java.net.URL"}, the printer uses instances of class
 * {@code ReferenceUriSchemesSupported} to advertise the {@code URI} schemes it
 * can accept. The acceptable {@code URI} schemes are included as service
 * attributes in the lookup service; this lets clients search the for printers
 * that can get print data using a certain {@code URI} scheme. The acceptable
 * {@code URI} schemes can also be queried using the capability methods in
 * interface {@code PrintService}. However, {@code ReferenceUriSchemesSupported}
 * attributes are used solely for determining acceptable {@code URI} schemes,
 * they are never included in a doc's, print request's, print job's, or print
 * service's attribute set.
 * <p>
 * The Internet Assigned Numbers Authority maintains the
 * <a href="http://www.iana.org/assignments/uri-schemes.html">official list of
 * URI schemes</a>.
 * <p>
 * Class {@code ReferenceUriSchemesSupported} defines enumeration values for
 * widely used {@code URI} schemes. A printer that supports additional
 * {@code URI} schemes can define them in a subclass of class
 * {@code ReferenceUriSchemesSupported}.
 * <p>
 * <b>IPP Compatibility:</b> The category name returned by {@code getName()} is
 * the IPP attribute name. The enumeration's integer value is the IPP enum
 * value. The {@code toString()} method returns the IPP string representation of
 * the attribute value.
 *
 * @author Alan Kaminsky
 */
public class ReferenceUriSchemesSupported
    extends EnumSyntax implements Attribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -8989076942813442805L;

    /**
     * File Transfer Protocol (FTP).
     */
    public static final ReferenceUriSchemesSupported FTP =
        new ReferenceUriSchemesSupported(0);

    /**
     * HyperText Transfer Protocol (HTTP).
     */
    public static final ReferenceUriSchemesSupported HTTP = new ReferenceUriSchemesSupported(1);

    /**
     * Secure HyperText Transfer Protocol (HTTPS).
     */
    public static final ReferenceUriSchemesSupported HTTPS = new ReferenceUriSchemesSupported(2);

    /**
     * Gopher Protocol.
     */
    public static final ReferenceUriSchemesSupported GOPHER = new ReferenceUriSchemesSupported(3);

    /**
     * USENET news.
     */
    public static final ReferenceUriSchemesSupported NEWS = new ReferenceUriSchemesSupported(4);

    /**
     * USENET news using Network News Transfer Protocol (NNTP).
     */
    public static final ReferenceUriSchemesSupported NNTP = new ReferenceUriSchemesSupported(5);

    /**
     * Wide Area Information Server (WAIS) protocol.
     */
    public static final ReferenceUriSchemesSupported WAIS = new ReferenceUriSchemesSupported(6);

    /**
     * Host-specific file names.
     */
    public static final ReferenceUriSchemesSupported FILE = new ReferenceUriSchemesSupported(7);

    /**
     * Construct a new reference {@code URI} scheme enumeration value with the
     * given integer value.
     *
     * @param  value Integer value
     */
    protected ReferenceUriSchemesSupported(int value) {
        super (value);
    }

    /**
     * The string table for class {@code ReferenceUriSchemesSupported}.
     */
    private static final String[] myStringTable = {
        "ftp",
        "http",
        "https",
        "gopher",
        "news",
        "nntp",
        "wais",
        "file",
    };

    /**
     * The enumeration value table for class
     * {@code ReferenceUriSchemesSupported}.
     */
    private static final ReferenceUriSchemesSupported[] myEnumValueTable = {
        FTP,
        HTTP,
        HTTPS,
        GOPHER,
        NEWS,
        NNTP,
        WAIS,
        FILE,
    };

    /**
     * Returns the string table for class {@code ReferenceUriSchemesSupported}.
     */
    protected String[] getStringTable() {
        return myStringTable.clone();
    }

    /**
     * Returns the enumeration value table for class
     * {@code ReferenceUriSchemesSupported}.
     */
    protected EnumSyntax[] getEnumValueTable() {
        return (EnumSyntax[])myEnumValueTable.clone();
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code ReferenceUriSchemesSupported} and any vendor-defined
     * subclasses, the category is class {@code ReferenceUriSchemesSupported}
     * itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return ReferenceUriSchemesSupported.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code ReferenceUriSchemesSupported} and any vendor-defined
     * subclasses, the category name is
     * {@code "reference-uri-schemes-supported"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "reference-uri-schemes-supported";
    }
}
