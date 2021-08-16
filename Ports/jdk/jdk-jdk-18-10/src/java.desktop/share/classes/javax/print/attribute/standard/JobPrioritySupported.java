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
import javax.print.attribute.SupportedValuesAttribute;

/**
 * Class {@code JobPrioritySupported} is an integer valued printing attribute
 * class that specifies whether a Print Service instance supports the
 * {@link JobPriority JobPriority} attribute and the number of different job
 * priority levels supported.
 * <p>
 * The client can always specify any {@link JobPriority JobPriority} value from
 * 1 to 100 for a job. However, the Print Service instance may support fewer
 * than 100 different job priority levels. If this is the case, the Print
 * Service instance automatically maps the client-specified job priority value
 * to one of the supported job priority levels, dividing the 100 job priority
 * values equally among the available job priority levels.
 * <p>
 * <b>IPP Compatibility:</b> The integer value gives the IPP integer value. The
 * category name returned by {@code getName()} gives the IPP attribute name.
 *
 * @author Alan Kaminsky
 */
public final class JobPrioritySupported extends IntegerSyntax
    implements SupportedValuesAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 2564840378013555894L;

    /**
     * Construct a new job priority supported attribute with the given integer
     * value.
     *
     * @param  value number of different job priority levels supported
     * @throws IllegalArgumentException if {@code value} is less than 1 or
     *         greater than 100
     */
    public JobPrioritySupported(int value) {
        super (value, 1, 100);
    }

    /**
     * Returns whether this job priority supported attribute is equivalent to
     * the passed in object. To be equivalent, all of the following conditions
     * must be true:
     * <ol type=1>
     *   <li>{@code object} is not {@code null}.
     *   <li>{@code object} is an instance of class
     *   {@code JobPrioritySupported}.
     *   <li>This job priority supported attribute's value and {@code object}'s
     *   value are equal.
     * </ol>
     *
     * @param  object {@code Object} to compare to
     * @return {@code true} if {@code object} is equivalent to this job priority
     *         supported attribute, {@code false} otherwise
     */
    public boolean equals (Object object) {

        return (super.equals(object) &&
               object instanceof JobPrioritySupported);
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code JobPrioritySupported}, the category is class
     * {@code JobPrioritySupported} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return JobPrioritySupported.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code JobPrioritySupported}, the category name is
     * {@code "job-priority-supported"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "job-priority-supported";
    }
}
