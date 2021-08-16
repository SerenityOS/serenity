/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.sjavac.pubapi;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public class PubApiTypeParam implements Serializable {

    private static final long serialVersionUID = 8899204612014329162L;

    private final String identifier;
    private final List<TypeDesc> bounds;

    public PubApiTypeParam(String identifier, List<TypeDesc> bounds) {
        this.identifier = identifier;
        this.bounds = bounds;
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass())
            return false;
        PubApiTypeParam other = (PubApiTypeParam) obj;
        return identifier.equals(other.identifier)
            && bounds.equals(other.bounds);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode() ^ bounds.hashCode();
    }

    public String asString() {
        if (bounds.isEmpty())
            return identifier;
        String boundsStr = bounds.stream()
                                 .map(TypeDesc::encodeAsString)
                                 .collect(Collectors.joining(" & "));
        return identifier + " extends " + boundsStr;
    }

    @Override
    public String toString() {
        return String.format("%s[id: %s, bounds: %s]",
                             getClass().getSimpleName(),
                             identifier,
                             bounds);
    }
}
