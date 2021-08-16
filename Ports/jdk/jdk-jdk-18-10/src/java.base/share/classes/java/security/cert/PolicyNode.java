/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.security.cert;

import java.util.Iterator;
import java.util.Set;

/**
 * An immutable valid policy tree node as defined by the PKIX certification
 * path validation algorithm.
 *
 * <p>One of the outputs of the PKIX certification path validation
 * algorithm is a valid policy tree, which includes the policies that
 * were determined to be valid, how this determination was reached,
 * and any policy qualifiers encountered. This tree is of depth
 * <i>n</i>, where <i>n</i> is the length of the certification
 * path that has been validated.
 *
 * <p>Most applications will not need to examine the valid policy tree.
 * They can achieve their policy processing goals by setting the
 * policy-related parameters in {@code PKIXParameters}. However,
 * the valid policy tree is available for more sophisticated applications,
 * especially those that process policy qualifiers.
 *
 * <p>{@link PKIXCertPathValidatorResult#getPolicyTree()
 * PKIXCertPathValidatorResult.getPolicyTree} returns the root node of the
 * valid policy tree. The tree can be traversed using the
 * {@link #getChildren getChildren} and {@link #getParent getParent} methods.
 * Data about a particular node can be retrieved using other methods of
 * {@code PolicyNode}.
 *
 * <p><b>Concurrent Access</b>
 * <p>All {@code PolicyNode} objects must be immutable and
 * thread-safe. Multiple threads may concurrently invoke the methods defined
 * in this class on a single {@code PolicyNode} object (or more than one)
 * with no ill effects. This stipulation applies to all public fields and
 * methods of this class and any added or overridden by subclasses.
 *
 * @since       1.4
 * @author      Sean Mullan
 */
public interface PolicyNode {

    /**
     * Returns the parent of this node, or {@code null} if this is the
     * root node.
     *
     * @return the parent of this node, or {@code null} if this is the
     * root node
     */
    PolicyNode getParent();

    /**
     * Returns an iterator over the children of this node. Any attempts to
     * modify the children of this node through the
     * {@code Iterator}'s remove method must throw an
     * {@code UnsupportedOperationException}.
     *
     * @return an iterator over the children of this node
     */
    Iterator<? extends PolicyNode> getChildren();

    /**
     * Returns the depth of this node in the valid policy tree.
     *
     * @return the depth of this node (0 for the root node, 1 for its
     * children, and so on)
     */
    int getDepth();

    /**
     * Returns the valid policy represented by this node.
     *
     * @return the {@code String} OID of the valid policy
     * represented by this node. For the root node, this method always returns
     * the special anyPolicy OID: "2.5.29.32.0".
     */
    String getValidPolicy();

    /**
     * Returns the set of policy qualifiers associated with the
     * valid policy represented by this node.
     *
     * @return an immutable {@code Set} of
     * {@code PolicyQualifierInfo}s. For the root node, this
     * is always an empty {@code Set}.
     */
    Set<? extends PolicyQualifierInfo> getPolicyQualifiers();

    /**
     * Returns the set of expected policies that would satisfy this
     * node's valid policy in the next certificate to be processed.
     *
     * @return an immutable {@code Set} of expected policy
     * {@code String} OIDs. For the root node, this method
     * always returns a {@code Set} with one element, the
     * special anyPolicy OID: "2.5.29.32.0".
     */
    Set<String> getExpectedPolicies();

    /**
     * Returns the criticality indicator of the certificate policy extension
     * in the most recently processed certificate.
     *
     * @return {@code true} if extension marked critical,
     * {@code false} otherwise. For the root node, {@code false}
     * is always returned.
     */
    boolean isCritical();
}
