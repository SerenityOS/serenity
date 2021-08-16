/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Taglet;
import static jdk.javadoc.doclet.Taglet.Location.*;

import com.sun.source.doctree.DocTree;

/**
 * A taglet to test access to a taglet's context.
 */
public class InfoTaglet implements Taglet {
    private DocletEnvironment env;
    private Doclet doclet;

    @Override
    public void init(DocletEnvironment env, Doclet doclet) {
        this.env = env;
        this.doclet = doclet;
    }

    @Override
    public Set<Location> getAllowedLocations() {
        return EnumSet.of(TYPE);
    }

    @Override
    public boolean isInlineTag() {
        return false;
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String toString(List<? extends DocTree> tags, Element element) {
        // The content lines below are primarily to help verify the element
        // and the values passed to init.
        return "<dt>"
                +"<span class=\"simpleTagLabel\">Info:</span>\n"
                + "</dt>"
                + "<dd>"
                + "<ul>\n"
                + "<li>Element: " + element.getKind() + " " + element.getSimpleName() + "\n"
                + "<li>Element supertypes: " +
                        env.getTypeUtils().directSupertypes(element.asType()) + "\n"
                + "<li>Doclet: " + doclet.getClass() + "\n"
                + "</ul>\n"
                + "</dd>";
    }
}

