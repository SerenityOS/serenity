/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.source.util;

import com.sun.source.doctree.*;

/**
 * A simple visitor for tree nodes.
 *
 * @param <R> the return type of this visitor's methods.  Use {@link
 *            Void} for visitors that do not need to return results.
 * @param <P> the type of the additional parameter to this visitor's
 *            methods.  Use {@code Void} for visitors that do not need an
 *            additional parameter.
 *
 * @since 1.8
 */
public class SimpleDocTreeVisitor<R,P> implements DocTreeVisitor<R, P> {
    /**
     * The default value, returned by the {@link #defaultAction default action}.
     */
    protected final R DEFAULT_VALUE;

    /**
     * Creates a visitor, with a DEFAULT_VALUE of {@code null}.
     */
    protected SimpleDocTreeVisitor() {
        DEFAULT_VALUE = null;
    }

    /**
     * Creates a visitor, with a specified DEFAULT_VALUE.
     * @param defaultValue the default value to be returned by the default action
     */
    protected SimpleDocTreeVisitor(R defaultValue) {
        DEFAULT_VALUE = defaultValue;
    }

    /**
     * The default action, used by all visit methods that are not overridden.
     * @param node the node being visited
     * @param p the parameter value passed to the visit method
     * @return the result value to be returned from the visit method
     */
    protected R defaultAction(DocTree node, P p) {
        return DEFAULT_VALUE;
    }

    /**
     * Invokes the appropriate visit method specific to the type of the node.
     * @param node the node on which to dispatch
     * @param p a parameter to be passed to the appropriate visit method
     * @return the value returns from the appropriate visit method
     */
    public final R visit(DocTree node, P p) {
        return (node == null) ? null : node.accept(this, p);
    }

    /**
     * Invokes the appropriate visit method on each of a sequence of nodes.
     * @param nodes the nodes on which to dispatch
     * @param p a parameter value to be passed to each appropriate visit method
     * @return the value return from the last of the visit methods, or null
     *      if none were called
     */
    public final R visit(Iterable<? extends DocTree> nodes, P p) {
        R r = null;
        if (nodes != null) {
            for (DocTree node : nodes)
                r = visit(node, p);
        }
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitAttribute(AttributeTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitAuthor(AuthorTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitComment(CommentTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitDeprecated(DeprecatedTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitDocComment(DocCommentTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitDocRoot(DocRootTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     * @since 10
     */
    @Override
    public R visitDocType(DocTypeTree node, P p) { return defaultAction(node, p); }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitEndElement(EndElementTree node, P p) { return defaultAction(node, p);}

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitEntity(EntityTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitErroneous(ErroneousTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return the result of {@code defaultAction}
     *
     * @since 9
     */
    @Override
    public R visitHidden(HiddenTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitIdentifier(IdentifierTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     *
     * @since 9
     */
    @Override
    public R visitIndex(IndexTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitInheritDoc(InheritDocTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitLink(LinkTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitLiteral(LiteralTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitParam(ParamTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     *
     * @since 9
     */
    @Override
    public R visitProvides(ProvidesTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitReference(ReferenceTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitReturn(ReturnTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitSee(SeeTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitSerial(SerialTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitSerialData(SerialDataTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitSerialField(SerialFieldTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitSince(SinceTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitStartElement(StartElementTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     * @since 10
     */
    @Override
    public R visitSummary(SummaryTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     * @since 12
     */
    @Override
    public R visitSystemProperty(SystemPropertyTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitText(TextTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitThrows(ThrowsTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitUnknownBlockTag(UnknownBlockTagTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitUnknownInlineTag(UnknownInlineTagTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     *
     * @since 9
     */
    @Override
    public R visitUses(UsesTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitValue(ValueTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitVersion(VersionTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitOther(DocTree node, P p) {
        return defaultAction(node, p);
    }

}
