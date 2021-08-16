/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.print;

import javax.print.attribute.Attribute;

/**
 * Interface {@code AttributeException} is a mixin interface which a subclass of
 * {@link PrintException PrintException} can implement to report an error
 * condition involving one or more printing attributes that a particular Print
 * Service instance does not support. Either the attribute is not supported at
 * all, or the attribute is supported but the particular specified value is not
 * supported. The Print Service API does not define any print exception classes
 * that implement interface {@code AttributeException}, that being left to the
 * Print Service implementor's discretion.
 */
public interface AttributeException {

    /**
     * Returns the array of printing attribute classes for which the Print
     * Service instance does not support the attribute at all, or {@code null}
     * if there are no such attributes. The objects in the returned array are
     * classes that extend the base interface {@link Attribute Attribute}.
     *
     * @return unsupported attribute classes
     */
    public Class<?>[] getUnsupportedAttributes();

    /**
     * Returns the array of printing attributes for which the Print Service
     * instance supports the attribute but does not support that particular
     * value of the attribute, or {@code null} if there are no such attribute
     * values.
     *
     * @return unsupported attribute values
     */
    public Attribute[] getUnsupportedValues();
}
