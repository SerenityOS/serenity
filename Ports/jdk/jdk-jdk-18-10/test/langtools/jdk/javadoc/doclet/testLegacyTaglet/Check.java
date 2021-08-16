/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;

import com.sun.source.doctree.DocTree;
import jdk.javadoc.doclet.Taglet;

public class Check implements Taglet {

    private static final String TAG_NAME = "check";

    private final EnumSet<Location> allowedSet = EnumSet.allOf(Location.class);

    @Override
    public Set<Taglet.Location> getAllowedLocations() {
        return allowedSet;
    }

    /**
     * Return false since the tag is not an inline tag.
     *
     * @return false since the tag is not an inline tag.
     */
    @Override
    public boolean isInlineTag() {
        return false;
    }

    /**
     * Return the name of this custom tag.
     *
     * @return the name of this tag.
     */
    @Override
    public String getName() {
        return TAG_NAME;
    }

    /**
     * Given a list of DocTrees representing this custom tag, return its string
     * representation.
     *
     * @param tags the array of tags representing this custom tag.
     * @param element the declaration to which the enclosing comment belongs
     * @return null to test if the javadoc throws an exception or not.
     */
    @Override
    public String toString(List<? extends DocTree> tags, Element element) {
        return null;
    }
}
