/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit.taglets;

import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;

import com.sun.source.doctree.DocTree;
import jdk.javadoc.internal.doclets.formats.html.markup.RawHtml;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;

import static jdk.javadoc.doclet.Taglet.Location.*;

/**
 * A taglet wrapper, allows the public taglet {@link jdk.javadoc.doclet.Taglet}
 * wrapped into an internal {@code Taglet} representation.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public final class UserTaglet implements Taglet {

    private final jdk.javadoc.doclet.Taglet userTaglet;

    public UserTaglet(jdk.javadoc.doclet.Taglet t) {
        userTaglet = t;
    }

    @Override
    public Set<jdk.javadoc.doclet.Taglet.Location> getAllowedLocations() {
        return userTaglet.getAllowedLocations();
    }

    @Override
    public boolean inField() {
        return userTaglet.getAllowedLocations().contains(FIELD);
    }

    @Override
    public boolean inConstructor() {
        return userTaglet.getAllowedLocations().contains(CONSTRUCTOR);
    }

    @Override
    public boolean inMethod() {
        return userTaglet.getAllowedLocations().contains(METHOD);
    }

    @Override
    public boolean inOverview() {
        return userTaglet.getAllowedLocations().contains(OVERVIEW);
    }

    @Override
    public boolean inModule() {
        return userTaglet.getAllowedLocations().contains(MODULE);
    }

    @Override
    public boolean inPackage() {
        return userTaglet.getAllowedLocations().contains(PACKAGE);
    }

    @Override
    public boolean inType() {
        return userTaglet.getAllowedLocations().contains(TYPE);
    }

    @Override
    public boolean isInlineTag() {
        return userTaglet.isInlineTag();
    }

    @Override
    public boolean isBlockTag() {
        return userTaglet.isBlockTag();
    }

    @Override
    public String getName() {
        return userTaglet.getName();
    }

    @Override
    public Content getInlineTagOutput(Element element, DocTree tag, TagletWriter writer) {
        Content output = writer.getOutputInstance();
        output.add(new RawHtml(userTaglet.toString(List.of(tag), element)));
        return output;
    }

    @Override
    public Content getAllBlockTagOutput(Element holder, TagletWriter writer) {
        Content output = writer.getOutputInstance();
        Utils utils = writer.configuration().utils;
        List<? extends DocTree> tags = utils.getBlockTags(holder, this);
        if (!tags.isEmpty()) {
            String tagString = userTaglet.toString(tags, holder);
            if (tagString != null) {
                output.add(new RawHtml(tagString));
            }
        }
        return output;
    }
}
