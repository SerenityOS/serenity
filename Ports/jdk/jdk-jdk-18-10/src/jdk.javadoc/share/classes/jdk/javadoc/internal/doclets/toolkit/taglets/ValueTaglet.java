/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.EnumSet;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

import com.sun.source.doctree.DocTree;
import jdk.javadoc.doclet.Taglet.Location;
import jdk.javadoc.internal.doclets.toolkit.BaseConfiguration;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.Messages;
import jdk.javadoc.internal.doclets.toolkit.util.CommentHelper;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;

/**
 * An inline Taglet representing the value tag. This tag should only be used with
 * constant fields that have a value.  It is used to access the value of constant
 * fields.  This inline tag has an optional field name parameter.  If the name is
 * specified, the constant value is retrieved from the specified field.  A link
 * is also created to the specified field.  If a name is not specified, the value
 * is retrieved for the field that the inline tag appears on.  The name is specified
 * in the following format:  [fully qualified class name]#[constant field name].
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */

public class ValueTaglet extends BaseTaglet {

    /**
     * Construct a new ValueTaglet.
     */
    public ValueTaglet() {
        super(DocTree.Kind.VALUE, true, EnumSet.allOf(Location.class));
    }

    /**
     * Returns the referenced field or a null if the value tag
     * is empty or the reference is invalid.
     *
     * @param holder the tag holder.
     * @param config the  configuration of the doclet.
     * @param tag the value tag.
     *
     * @return the referenced field or null.
     */
    private VariableElement getVariableElement(Element holder, BaseConfiguration config, DocTree tag) {
        CommentHelper ch = config.utils.getCommentHelper(holder);
        String signature = ch.getReferencedSignature(tag);

        Element e = signature == null
                ? holder
                : ch.getReferencedMember(tag);

        return (e != null && config.utils.isVariableElement(e))
                ? (VariableElement) e
                : null;
    }

    @Override
    public Content getInlineTagOutput(Element holder, DocTree tag, TagletWriter writer) {
        BaseConfiguration configuration = writer.configuration();
        Utils utils = configuration.utils;
        Messages messages = configuration.getMessages();
        VariableElement field = getVariableElement(holder, configuration, tag);
        if (field == null) {
            if (tag.toString().isEmpty()) {
                //Invalid use of @value
                messages.warning(holder,
                        "doclet.value_tag_invalid_use");
            } else {
                //Reference is unknown.
                messages.warning(holder,
                        "doclet.value_tag_invalid_reference", tag.toString());
            }
        } else if (field.getConstantValue() != null) {
            return writer.valueTagOutput(field,
                utils.constantValueExpression(field),
                // TODO: investigate and cleanup
                // in the j.l.m world, equals will not be accurate
                // !field.equals(tag.holder())
                !utils.elementsEqual(field, holder)
            );
        } else {
            //Referenced field is not a constant.
            messages.warning(holder,
                "doclet.value_tag_invalid_constant", utils.getSimpleName(field));
        }
        return writer.getOutputInstance();
    }
}
