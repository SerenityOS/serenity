/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
import javax.lang.model.element.ModuleElement;
import com.sun.source.doctree.DocTree;
import jdk.javadoc.doclet.Taglet;
import static jdk.javadoc.doclet.Taglet.Location.*;

/**
 * A block tag to optionally insert a reference to a module graph.
 */
public class ModuleGraph implements Taglet {
    private static final boolean enableModuleGraph =
        Boolean.getBoolean("enableModuleGraph");

    /** Returns the set of locations in which a taglet may be used. */
    @Override
    public Set<Location> getAllowedLocations() {
        return EnumSet.of(MODULE);
    }

    @Override
    public boolean isInlineTag() {
        return false;
    }

    @Override
    public String getName() {
        return "moduleGraph";
    }

    @Override
    public String toString(List<? extends DocTree> tags, Element element) {
        if (!enableModuleGraph) {
            return "";
        }

        String moduleName = ((ModuleElement) element).getQualifiedName().toString();
        String imageFile = "module-graph.svg";
        int thumbnailHeight = -1;
        String hoverImage = "";
        if (!moduleName.equals("java.base")) {
            thumbnailHeight = 100; // also appears in the stylesheet
            hoverImage = "<span>"
                + getImage(moduleName, imageFile, -1, true)
                + "</span>";
        }
        return "<dt>Module Graph:</dt>"
            + "<dd>"
            + "<a class=\"module-graph\" href=\"" + imageFile + "\">"
            + getImage(moduleName, imageFile, thumbnailHeight, false)
            + hoverImage
            + "</a>"
            + "</dd>";
    }

    private static final String VERTICAL_ALIGN = "vertical-align:top";
    private static final String BORDER = "border: solid lightgray 1px;";

    private String getImage(String moduleName, String file, int height, boolean useBorder) {
        return String.format("<img style=\"%s\" alt=\"Module graph for %s\" src=\"%s\"%s>",
                             useBorder ? BORDER + " " + VERTICAL_ALIGN : VERTICAL_ALIGN,
                             moduleName,
                             file,
                             (height <= 0 ? "" : " height=\"" + height + "\""));
    }
}
