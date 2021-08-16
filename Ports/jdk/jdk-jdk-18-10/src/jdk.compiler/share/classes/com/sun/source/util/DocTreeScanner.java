/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * A DocTreeVisitor that visits all the child tree nodes.
 * To visit nodes of a particular type, just override the
 * corresponding visitXYZ method.
 * Inside your method, call super.visitXYZ to visit descendant
 * nodes.
 *
 * <p>Here is an example to count the number of erroneous nodes in a tree:
 * <pre>
 *   class CountErrors extends DocTreeScanner&lt;Integer,Void&gt; {
 *      {@literal @}Override
 *      public Integer visitErroneous(ErroneousTree node, Void p) {
 *          return 1;
 *      }
 *      {@literal @}Override
 *      public Integer reduce(Integer r1, Integer r2) {
 *          return (r1 == null ? 0 : r1) + (r2 == null ? 0 : r2);
 *      }
 *   }
 * </pre>
 *
 * @implSpec
 * <p>The default implementation of the visitXYZ methods will determine
 * a result as follows:
 * <ul>
 * <li>If the node being visited has no children, the result will be {@code null}.
 * <li>If the node being visited has one child, the result will be the
 * result of calling {@code scan} with that child. The child may be a simple node
 * or itself a list of nodes.
 * <li>If the node being visited has more than one child, the result will
 * be determined by calling {@code scan} with each child in turn, and then combining the
 * result of each scan after the first with the cumulative result
 * so far, as determined by the {@link #reduce} method. Each child may be either
 * a simple node or a list of nodes. The default behavior of the {@code reduce}
 * method is such that the result of the visitXYZ method will be the result of
 * the last child scanned.
 * </ul>
 *
 * @since 1.8
 */
public class DocTreeScanner<R,P> implements DocTreeVisitor<R,P> {
    /**
     * Constructs a {@code DocTreeScanner}.
     */
    public DocTreeScanner() {}

    /**
     * Scans a single node.
     * @param node the node to be scanned
     * @param p a parameter value passed to the visit method
     * @return the result value from the visit method
     */
    public R scan(DocTree node, P p) {
        return (node == null) ? null : node.accept(this, p);
    }

    private R scanAndReduce(DocTree node, P p, R r) {
        return reduce(scan(node, p), r);
    }

    /**
     * Scans a sequence of nodes.
     * @param nodes the nodes to be scanned
     * @param p a parameter value to be passed to the visit method for each node
     * @return the combined return value from the visit methods.
     *      The values are combined using the {@link #reduce reduce} method.
     */
    public R scan(Iterable<? extends DocTree> nodes, P p) {
        R r = null;
        if (nodes != null) {
            boolean first = true;
            for (DocTree node : nodes) {
                r = (first ? scan(node, p) : scanAndReduce(node, p, r));
                first = false;
            }
        }
        return r;
    }

    private R scanAndReduce(Iterable<? extends DocTree> nodes, P p, R r) {
        return reduce(scan(nodes, p), r);
    }

    /**
     * Reduces two results into a combined result.
     * The default implementation is to return the first parameter.
     * The general contract of the method is that it may take any action whatsoever.
     * @param r1 the first of the values to be combined
     * @param r2 the second of the values to be combined
     * @return the result of combining the two parameters
     */
    public R reduce(R r1, R r2) {
        return r1;
    }


/* ***************************************************************************
 * Visitor methods
 ****************************************************************************/

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitAttribute(AttributeTree node, P p) {
        return scan(node.getValue(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitAuthor(AuthorTree node, P p) {
        return scan(node.getName(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitComment(CommentTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitDeprecated(DeprecatedTree node, P p) {
        return scan(node.getBody(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitDocComment(DocCommentTree node, P p) {
        R r = scan(node.getFirstSentence(), p);
        r = scanAndReduce(node.getBody(), p, r);
        r = scanAndReduce(node.getBlockTags(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitDocRoot(DocRootTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitDocType(DocTypeTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitEndElement(EndElementTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitEntity(EntityTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitErroneous(ErroneousTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitHidden(HiddenTree node, P p) {
        return scan(node.getBody(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitIdentifier(IdentifierTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitIndex(IndexTree node, P p) {
        R r = scan(node.getSearchTerm(), p);
        r = scanAndReduce(node.getDescription(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitInheritDoc(InheritDocTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitLink(LinkTree node, P p) {
        R r = scan(node.getReference(), p);
        r = scanAndReduce(node.getLabel(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitLiteral(LiteralTree node, P p) {
        return scan(node.getBody(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitParam(ParamTree node, P p) {
        R r = scan(node.getName(), p);
        r = scanAndReduce(node.getDescription(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitProvides(ProvidesTree node, P p) {
        R r = scan(node.getServiceType(), p);
        r = scanAndReduce(node.getDescription(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitReference(ReferenceTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitReturn(ReturnTree node, P p) {
        return scan(node.getDescription(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitSee(SeeTree node, P p) {
        return scan(node.getReference(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitSerial(SerialTree node, P p) {
        return scan(node.getDescription(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitSerialData(SerialDataTree node, P p) {
        return scan(node.getDescription(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitSerialField(SerialFieldTree node, P p) {
        R r = scan(node.getName(), p);
        r = scanAndReduce(node.getType(), p, r);
        r = scanAndReduce(node.getDescription(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitSince(SinceTree node, P p) {
        return scan(node.getBody(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitStartElement(StartElementTree node, P p) {
        return scan(node.getAttributes(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     * @since 10
     */
    @Override
    public R visitSummary(SummaryTree node, P p) {
        return scan(node.getSummary(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     * @since 12
     */
    @Override
    public R visitSystemProperty(SystemPropertyTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitText(TextTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitThrows(ThrowsTree node, P p) {
        R r = scan(node.getExceptionName(), p);
        r = scanAndReduce(node.getDescription(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitUnknownBlockTag(UnknownBlockTagTree node, P p) {
        return scan(node.getContent(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitUnknownInlineTag(UnknownInlineTagTree node, P p) {
        return scan(node.getContent(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitUses(UsesTree node, P p) {
        R r = scan(node.getServiceType(), p);
        r = scanAndReduce(node.getDescription(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitValue(ValueTree node, P p) {
        return scan(node.getReference(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitVersion(VersionTree node, P p) {
        return scan(node.getBody(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitOther(DocTree node, P p) {
        return null;
    }

}
