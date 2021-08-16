/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.util;

import java.security.Key;
import java.util.Date;
import java.util.Set;

/**
 * This interface contains parameters for checking against constraints that
 * extend past the publicly available parameters in
 * java.security.AlgorithmConstraints.
 */
public interface ConstraintsParameters {

    /**
     * Returns true if a certificate chains back to a trusted JDK root CA.
     */
    boolean anchorIsJdkCA();

    /**
     * Returns the set of keys that should be checked against the
     * constraints, or an empty set if there are no keys to be checked.
     */
    Set<Key> getKeys();

    /**
     * Returns the date that should be checked against the constraints, or
     * null if not set.
     */
    Date getDate();

    /**
     * Returns the Validator variant.
     */
    String getVariant();

    /**
     * Returns an extended message used in exceptions. See
     * DisabledAlgorithmConstraints for usage.
     */
    String extendedExceptionMsg();
}
