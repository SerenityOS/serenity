/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.taglet;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import com.sun.source.doctree.DocTree;
import jdk.javadoc.doclet.Taglet;
import static jdk.javadoc.doclet.Taglet.Location.*;

/**
 * An inline tag to conveniently insert a standard Incubating warning.  For
 * use by members in Incubator Modules.
 */
public class Incubating implements Taglet {

    /** Returns the set of locations in which a taglet may be used. */
    @Override
    public Set<Location> getAllowedLocations() {
        return EnumSet.of(OVERVIEW, MODULE, PACKAGE, TYPE);
    }

    @Override
    public boolean isInlineTag() {
        return true;
    }

    @Override
    public String getName() {
        return "Incubating";
    }

    private static final String MESSAGE =
        "<BR><b><a href=\"http://openjdk.java.net/jeps/11\">Incubating Feature.</a>"
                + " Will be removed in a future release.</b>";

    @Override
    public String toString(List<? extends DocTree> tags, Element elem) {
        return MESSAGE;
    }
}

