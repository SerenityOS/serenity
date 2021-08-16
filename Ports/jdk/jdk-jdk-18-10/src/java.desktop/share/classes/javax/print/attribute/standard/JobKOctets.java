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
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Class {@code JobKOctets} is an integer valued printing attribute class that
 * specifies the total size of the document(s) in K octets, i.e., in units of
 * 1024 octets requested to be processed in the job. The value must be rounded
 * up, so that a job between 1 and 1024 octets must be indicated as being 1K
 * octets, 1025 to 2048 must be 2K octets, etc. For a multidoc print job (a job
 * with multiple documents), the {@code JobKOctets} value is computed by adding
 * up the individual documents' sizes in octets, then rounding up to the next K
 * octets value.
 * <p>
 * The {@code JobKOctets} attribute describes the size of the job. This
 * attribute is not intended to be a counter; it is intended to be useful
 * routing and scheduling information if known. The printer may try to compute
 * the {@code JobKOctets} attribute's value if it is not supplied in the Print
 * Request. Even if the client does supply a value for the {@code JobKOctets}
 * attribute in the Print Request, the printer may choose to change the value if
 * the printer is able to compute a value which is more accurate than the client
 * supplied value. The printer may be able to determine the correct value for
 * the {@code JobKOctets} attribute either right at job submission time or at
 * any later point in time.
 * <p>
 * The {@code JobKOctets} value must not include the multiplicative factors
 * contributed by the number of copies specified by the {@link Copies Copies}
 * attribute, independent of whether the device can process multiple copies
 * without making multiple passes over the job or document data and independent
 * of whether the output is collated or not. Thus the value is independent of
 * the implementation and indicates the size of the document(s) measured in K
 * octets independent of the number of copies.
 * <p>
 * The {@code JobKOctets} value must also not include the multiplicative factor
 * due to a copies instruction embedded in the document data. If the document
 * data actually includes replications of the document data, this value will
 * include such replication. In other words, this value is always the size of
 * the source document data, rather than a measure of the hardcopy output to be
 * produced.
 * <p>
 * The size of a doc is computed based on the print data representation class as
 * specified by the doc's {@link javax.print.DocFlavor DocFlavor}, as shown in
 * the table below.
 *
 * <table class="striped">
 * <caption>Table showing computation of doc sizes</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Representation Class
 *     <th scope="col">Document Size
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row">{@code byte[]}
 *     <td>Length of the byte array
 *   <tr>
 *     <th scope="row">{@code java.io.InputStream}
 *     <td>Number of bytes read from the stream
 *   <tr>
 *     <th scope="row">{@code char[]}
 *     <td>Length of the character array x 2
 *   <tr>
 *     <th scope="row">{@code java.lang.String}
 *     <td>Length of the string x 2
 *   <tr>
 *     <th scope="row">{@code java.io.Reader}
 *     <td>Number of characters read from the stream x 2
 *   <tr>
 *     <th scope="row">{@code java.net.URL}
 *     <td>Number of bytes read from the file at the given {@code URL} address
 *   <tr>
 *     <th scope="row">{@code java.awt.image.renderable.RenderableImage}
 *     <td>Implementation dependent&#42;
 *   <tr>
 *     <th scope="row">{@code java.awt.print.Printable}
 *     <td>Implementation dependent&#42;
 *   <tr>
 *     <th scope="row">{@code java.awt.print.Pageable}
 *     <td>Implementation dependent&#42;
 * </tbody>
 * </table>
 * <p>
 * &#42; In these cases the Print Service itself generates the print data sent
 * to the printer. If the Print Service supports the {@code JobKOctets}
 * attribute, for these cases the Print Service itself must calculate the size
 * of the print data, replacing any {@code JobKOctets} value the client
 * specified.
 * <p>
 * <b>IPP Compatibility:</b> The integer value gives the IPP integer value. The
 * category name returned by {@code getName()} gives the IPP attribute name.
 *
 * @author Alan Kaminsky
 * @see JobKOctetsSupported
 * @see JobKOctetsProcessed
 * @see JobImpressions
 * @see JobMediaSheets
 */
public final class JobKOctets   extends IntegerSyntax
        implements PrintRequestAttribute, PrintJobAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -8959710146498202869L;

    /**
     * Construct a new job K octets attribute with the given integer value.
     *
     * @param  value Integer value
     * @throws IllegalArgumentException if {@code value} is negative
     */
    public JobKOctets(int value) {
        super (value, 0, Integer.MAX_VALUE);
    }

    /**
     * Returns whether this job K octets attribute is equivalent to the passed
     * in object. To be equivalent, all of the following conditions must be
     * true:
     * <ol type=1>
     *   <li>{@code object} is not {@code null}.
     *   <li>{@code object} is an instance of class {@code JobKOctets}.
     *   <li>This job K octets attribute's value and {@code object}'s value are
     *   equal.
     * </ol>
     *
     * @param  object {@code Object} to compare to
     * @return {@code true} if {@code object} is equivalent to this job K octets
     *         attribute, {@code false} otherwise
     */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof JobKOctets;
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code JobKOctets}, the category is class
     * {@code JobKOctets} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return JobKOctets.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code JobKOctets}, the category name is
     * {@code "job-k-octets"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "job-k-octets";
    }
}
