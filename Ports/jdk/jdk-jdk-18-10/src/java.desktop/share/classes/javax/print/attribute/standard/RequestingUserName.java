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
import java.util.Locale;

import javax.print.attribute.Attribute;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.TextSyntax;

/**
 * Class {@code RequestingUserName} is a printing attribute class, a text
 * attribute, that specifies the name of the end user that submitted the print
 * job. A requesting user name is an arbitrary string defined by the client. The
 * printer does not put the client-specified {@code RequestingUserName}
 * attribute into the Print Job's attribute set; rather, the printer puts in a
 * {@link JobOriginatingUserName JobOriginatingUserName} attribute. This means
 * that services which support specifying a username with this attribute should
 * also report a {@code JobOriginatingUserName} in the job's attribute set. Note
 * that many print services may have a way to independently authenticate the
 * user name, and so may state support for a requesting user name, but in
 * practice will then report the user name authenticated by the service rather
 * than that specified via this attribute.
 * <p>
 * <b>IPP Compatibility:</b> The string value gives the IPP name value. The
 * locale gives the IPP natural language. The category name returned by
 * {@code getName()} gives the IPP attribute name.
 *
 * @author Alan Kaminsky
 */
public final class RequestingUserName   extends TextSyntax
    implements PrintRequestAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -2683049894310331454L;

    /**
     * Constructs a new requesting user name attribute with the given user name
     * and locale.
     *
     * @param  userName user name
     * @param  locale natural language of the text string. {@code null} is
     *         interpreted to mean the default locale as returned by
     *         {@code Locale.getDefault()}
     * @throws NullPointerException if {@code userName} is {@code null}
     */
    public RequestingUserName(String userName, Locale locale) {
        super (userName, locale);
    }

    /**
     * Returns whether this requesting user name attribute is equivalent to the
     * passed in object. To be equivalent, all of the following conditions must
     * be true:
     * <ol type=1>
     *   <li>{@code object} is not {@code null}.
     *   <li>{@code object} is an instance of class {@code RequestingUserName}.
     *   <li>This requesting user name attribute's underlying string and
     *   {@code object}'s underlying string are equal.
     *   <li>This requesting user name attribute's locale and {@code object}'s
     *   locale are equal.
     * </ol>
     *
     * @param  object {@code Object} to compare to
     * @return {@code true} if {@code object} is equivalent to this requesting
     *         user name attribute, {@code false} otherwise
     */
    public boolean equals(Object object) {
        return (super.equals(object) &&
                object instanceof RequestingUserName);
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code RequestingUserName}, the category is class
     * {@code RequestingUserName} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return RequestingUserName.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code RequestingUserName}, the category name is
     * {@code "requesting-user-name"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "requesting-user-name";
    }
}
