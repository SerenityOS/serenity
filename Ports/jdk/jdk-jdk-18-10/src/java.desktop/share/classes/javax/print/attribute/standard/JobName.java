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
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.TextSyntax;

/**
 * Class {@code JobName} is a printing attribute class, a text attribute, that
 * specifies the name of a print job. A job's name is an arbitrary string
 * defined by the client. It does not need to be unique between different jobs.
 * A Print Job's {@code JobName} attribute is set to the value supplied by the
 * client in the Print Request's attribute set. If, however, the client does not
 * supply a {@code JobName} attribute in the Print Request, the printer, when it
 * creates the Print Job, must generate a {@code JobName}. The printer should
 * generate the value of the Print Job's {@code JobName} attribute from the
 * first of the following sources that produces a value: (1) the
 * {@link DocumentName DocumentName} attribute of the first (or only) doc in the
 * job, (2) the {@code URL} of the first (or only) doc in the job, if the doc's
 * print data representation object is a {@code URL}, or (3) any other piece of
 * Print Job specific and/or document content information.
 * <p>
 * <b>IPP Compatibility:</b> The string value gives the IPP name value. The
 * locale gives the IPP natural language. The category name returned by
 * {@code getName()} gives the IPP attribute name.
 *
 * @author Alan Kaminsky
 */
public final class JobName extends TextSyntax
        implements PrintRequestAttribute, PrintJobAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 4660359192078689545L;

    /**
     * Constructs a new job name attribute with the given job name and locale.
     *
     * @param  jobName job name
     * @param  locale natural language of the text string. {@code null} is
     *         interpreted to mean the default locale as returned by
     *         {@code Locale.getDefault()}
     * @throws NullPointerException if {@code jobName} is {@code null}
     */
    public JobName(String jobName, Locale locale) {
        super (jobName, locale);
    }

    /**
     * Returns whether this job name attribute is equivalent to the passed in
     * object. To be equivalent, all of the following conditions must be true:
     * <ol type=1>
     *   <li>{@code object} is not {@code null}.
     *   <li>{@code object} is an instance of class {@code JobName}.
     *   <li>This job name attribute's underlying string and {@code object}'s
     *   underlying string are equal.
     *   <li>This job name attribute's locale and {@code object}'s locale are
     *   equal.
     * </ol>
     *
     * @param  object {@code Object} to compare to
     * @return {@code true} if {@code object} is equivalent to this job name
     *         attribute, {@code false} otherwise
     */
    public boolean equals(Object object) {
        return (super.equals(object) && object instanceof JobName);
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code JobName}, the category is class {@code JobName} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return JobName.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code JobName}, the category name is {@code "job-name"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "job-name";
    }
}
