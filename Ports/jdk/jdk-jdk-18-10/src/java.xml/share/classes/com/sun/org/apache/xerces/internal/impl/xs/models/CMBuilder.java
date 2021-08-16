/*
 * Copyright (c) 2006, 2017, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.impl.xs.models;

import com.sun.org.apache.xerces.internal.impl.dtd.models.CMNode;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.impl.xs.XSComplexTypeDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSDeclarationPool;
import com.sun.org.apache.xerces.internal.impl.xs.XSElementDecl;
import com.sun.org.apache.xerces.internal.impl.xs.XSModelGroupImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XSParticleDecl;

/**
 * This class constructs content models for a given grammar.
 *
 * @xerces.internal
 *
 * @author Elena Litani, IBM
 * @author Sandy Gao, IBM
 *
 * @LastModified: Nov 2017
 */
public class CMBuilder {

    // REVISIT: should update the decl pool to cache XSCM objects too
    private XSDeclarationPool fDeclPool = null;

    // It never changes, so a static member is good enough
    private static XSEmptyCM fEmptyCM = new XSEmptyCM();

    // needed for DFA construction
    private int fLeafCount;
    // needed for UPA
    private int fParticleCount;
    //Factory to create Bin, Uni, Leaf nodes
    private CMNodeFactory fNodeFactory ;

    public CMBuilder(CMNodeFactory nodeFactory) {
        fDeclPool = null;
        fNodeFactory = nodeFactory ;
    }

    public void setDeclPool(XSDeclarationPool declPool) {
        fDeclPool = declPool;
    }

    /**
     * Get content model for the a given type
     *
     * @param typeDecl  get content model for which complex type
     * @param forUPA    a flag indicating whether it is for UPA
     * @return          a content model validator
     */
    public XSCMValidator getContentModel(XSComplexTypeDecl typeDecl, boolean forUPA) {

        // for complex type with empty or simple content,
        // there is no content model validator
        short contentType = typeDecl.getContentType();
        if (contentType == XSComplexTypeDecl.CONTENTTYPE_SIMPLE ||
            contentType == XSComplexTypeDecl.CONTENTTYPE_EMPTY) {
            return null;
        }

        XSParticleDecl particle = (XSParticleDecl)typeDecl.getParticle();

        // if the content is element only or mixed, but no particle
        // is defined, return the empty content model
        if (particle == null)
            return fEmptyCM;

        // if the content model contains "all" model group,
        // we create an "all" content model, otherwise a DFA content model
        XSCMValidator cmValidator = null;
        if (particle.fType == XSParticleDecl.PARTICLE_MODELGROUP &&
            ((XSModelGroupImpl)particle.fValue).fCompositor == XSModelGroupImpl.MODELGROUP_ALL) {
            cmValidator = createAllCM(particle);
        }
        else {
            cmValidator = createDFACM(particle, forUPA);
        }

        //now we are throught building content model and have passed sucessfully of the nodecount check
        //if set by the application
        fNodeFactory.resetNodeCount() ;

        // if the validator returned is null, it means there is nothing in
        // the content model, so we return the empty content model.
        if (cmValidator == null)
            cmValidator = fEmptyCM;

        return cmValidator;
    }

    XSCMValidator createAllCM(XSParticleDecl particle) {
        if (particle.fMaxOccurs == 0)
            return null;

        // get the model group, and add all children of it to the content model
        XSModelGroupImpl group = (XSModelGroupImpl)particle.fValue;
        // create an all content model. the parameter indicates whether
        // the <all> itself is optional
        XSAllCM allContent = new XSAllCM(particle.fMinOccurs == 0, group.fParticleCount);
        for (int i = 0; i < group.fParticleCount; i++) {
            // add the element decl to the all content model
            allContent.addElement((XSElementDecl)group.fParticles[i].fValue,
            group.fParticles[i].fMinOccurs == 0);
        }
        return allContent;
    }

    XSCMValidator createDFACM(XSParticleDecl particle, boolean forUPA) {
        fLeafCount = 0;
        fParticleCount = 0;
        // convert particle tree to CM tree
        CMNode node = useRepeatingLeafNodes(particle) ? buildCompactSyntaxTree(particle) : buildSyntaxTree(particle, forUPA, true);
        if (node == null)
            return null;
        // build DFA content model from the CM tree
        return new XSDFACM(node, fLeafCount);
    }

    // 1. convert particle tree to CM tree:
    // 2. expand all occurrence values: a{n, unbounded} -> a, a, ..., a+
    //                                  a{n, m} -> a, a, ..., a?, a?, ...
    // 3. convert model groups (a, b, c, ...) or (a | b | c | ...) to
    //    binary tree: (((a,b),c),...) or (((a|b)|c)|...)
    // 4. make sure each leaf node (XSCMLeaf) has a distinct position
    private CMNode buildSyntaxTree(XSParticleDecl particle, boolean forUPA, boolean optimize) {

        int maxOccurs = particle.fMaxOccurs;
        int minOccurs = particle.fMinOccurs;

        boolean compactedForUPA = false;
        if (forUPA) {
            // When doing UPA, we reduce the size of the minOccurs/maxOccurs values to make
            // processing the DFA faster.  For UPA the exact values don't matter.
            if (minOccurs > 1) {
                if (maxOccurs > minOccurs || particle.getMaxOccursUnbounded()) {
                    minOccurs = 1;
                    compactedForUPA = true;
                }
                else { // maxOccurs == minOccurs
                    minOccurs = 2;
                    compactedForUPA = true;
                }
            }
            if (maxOccurs > 1) {
                maxOccurs = 2;
                compactedForUPA = true;
            }
        }

        short type = particle.fType;
        CMNode nodeRet = null;

        if ((type == XSParticleDecl.PARTICLE_WILDCARD) ||
            (type == XSParticleDecl.PARTICLE_ELEMENT)) {
            // (task 1) element and wildcard particles should be converted to
            // leaf nodes
            // REVISIT: Make a clone of the leaf particle, so that if there
            // are two references to the same group, we have two different
            // leaf particles for the same element or wildcard decl.
            // This is useful for checking UPA.
            nodeRet = fNodeFactory.getCMLeafNode(particle.fType, particle.fValue, fParticleCount++, fLeafCount++);
            // (task 2) expand occurrence values
            nodeRet = expandContentModel(nodeRet, minOccurs, maxOccurs, optimize);
            if (nodeRet != null) {
                nodeRet.setIsCompactUPAModel(compactedForUPA);
            }
        }
        else if (type == XSParticleDecl.PARTICLE_MODELGROUP) {
            // (task 1,3) convert model groups to binary trees
            XSModelGroupImpl group = (XSModelGroupImpl)particle.fValue;
            CMNode temp = null;
            // when the model group is a choice of more than one particles, but
            // only one of the particle is not empty, (for example
            // <choice>
            //   <sequence/>
            //   <element name="e"/>
            // </choice>
            // ) we can't not return that one particle ("e"). instead, we should
            // treat such particle as optional ("e?").
            // the following boolean variable is true when there are at least
            // 2 non-empty children.
            boolean twoChildren = false;
            for (int i = 0; i < group.fParticleCount; i++) {
                // first convert each child to a CM tree
                temp = buildSyntaxTree(group.fParticles[i],
                        forUPA,
                        optimize &&
                        minOccurs == 1 && maxOccurs == 1 &&
                        (group.fCompositor == XSModelGroupImpl.MODELGROUP_SEQUENCE ||
                         group.fParticleCount == 1));
                // then combine them using binary operation
                if (temp != null) {
                    compactedForUPA |= temp.isCompactedForUPA();
                    if (nodeRet == null) {
                        nodeRet = temp;
                    }
                    else {
                        nodeRet = fNodeFactory.getCMBinOpNode(group.fCompositor, nodeRet, temp);
                        // record the fact that there are at least 2 children
                        twoChildren = true;
                    }
                }
            }
            // (task 2) expand occurrence values
            if (nodeRet != null) {
                // when the group is "choice", there is only one non-empty
                // child, and the group had more than one children, we need
                // to create a zero-or-one (optional) node for the non-empty
                // particle.
                if (group.fCompositor == XSModelGroupImpl.MODELGROUP_CHOICE &&
                    !twoChildren && group.fParticleCount > 1) {
                    nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ZERO_OR_ONE, nodeRet);
                }
                nodeRet = expandContentModel(nodeRet, minOccurs, maxOccurs, false);
                nodeRet.setIsCompactUPAModel(compactedForUPA);
            }
        }

        return nodeRet;
    }

    // 2. expand all occurrence values: a{n, unbounded} -> a, a, ..., a+
    //                                  a{n, m} -> a, a, ..., a?, a?, ...
    // 4. make sure each leaf node (XSCMLeaf) has a distinct position
    private CMNode expandContentModel(CMNode node,
                                      int minOccurs, int maxOccurs, boolean optimize) {

        CMNode nodeRet = null;

        if (minOccurs==1 && maxOccurs==1) {
            nodeRet = node;
        }
        else if (minOccurs==0 && maxOccurs==1) {
            //zero or one
            nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ZERO_OR_ONE, node);
        }
        else if (minOccurs == 0 && maxOccurs==SchemaSymbols.OCCURRENCE_UNBOUNDED) {
            //zero or more
            nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ZERO_OR_MORE, node);
        }
        else if (minOccurs == 1 && maxOccurs==SchemaSymbols.OCCURRENCE_UNBOUNDED) {
            //one or more
            nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ONE_OR_MORE, node);
        }
        else if (optimize && node.type() == XSParticleDecl.PARTICLE_ELEMENT ||
                 node.type() == XSParticleDecl.PARTICLE_WILDCARD) {
            // Only for elements and wildcards, subsume e{n,m} and e{n,unbounded} to e*
            // or e+ and, once the DFA reaches a final state, check if the actual number
            // of elements is between minOccurs and maxOccurs. This new algorithm runs
            // in constant space.

            // TODO: What is the impact of this optimization on the PSVI?
            nodeRet = fNodeFactory.getCMUniOpNode(
                    minOccurs == 0 ? XSParticleDecl.PARTICLE_ZERO_OR_MORE
                        : XSParticleDecl.PARTICLE_ONE_OR_MORE, node);
            nodeRet.setUserData(new int[] { minOccurs, maxOccurs });
        }
        else if (maxOccurs == SchemaSymbols.OCCURRENCE_UNBOUNDED) {
            // => a,a,..,a+
            // create a+ node first, then put minOccurs-1 a's in front of it
            // for the first time "node" is used, we don't need to make a copy
            // and for other references to node, we make copies
            nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ONE_OR_MORE, node);
            // (task 4) we need to call copyNode here, so that we append
            // an entire new copy of the node (a subtree). this is to ensure
            // all leaf nodes have distinct position
            // we know that minOccurs > 1
            nodeRet = fNodeFactory.getCMBinOpNode(XSModelGroupImpl.MODELGROUP_SEQUENCE,
                                                  multiNodes(node, minOccurs-1, true), nodeRet);
        }
        else {
            // {n,m} => a,a,a,...(a),(a),...
            // first n a's, then m-n a?'s.
            // copyNode is called, for the same reason as above
            if (minOccurs > 0) {
                nodeRet = multiNodes(node, minOccurs, false);
            }
            if (maxOccurs > minOccurs) {
                node = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ZERO_OR_ONE, node);
                if (nodeRet == null) {
                    nodeRet = multiNodes(node, maxOccurs-minOccurs, false);
                }
                else {
                    nodeRet = fNodeFactory.getCMBinOpNode(XSModelGroupImpl.MODELGROUP_SEQUENCE,
                                                          nodeRet, multiNodes(node, maxOccurs-minOccurs, true));
                }
            }
        }

        return nodeRet;
    }

    private CMNode multiNodes(CMNode node, int num, boolean copyFirst) {
        if (num == 0) {
            return null;
        }
        if (num == 1) {
            return copyFirst ? copyNode(node) : node;
        }
        int num1 = num/2;
        return fNodeFactory.getCMBinOpNode(XSModelGroupImpl.MODELGROUP_SEQUENCE,
                                           multiNodes(node, num1, copyFirst),
                                           multiNodes(node, num-num1, true));
    }

    // 4. make sure each leaf node (XSCMLeaf) has a distinct position
    private CMNode copyNode(CMNode node) {
        int type = node.type();
        // for choice or sequence, copy the two subtrees, and combine them
        if (type == XSModelGroupImpl.MODELGROUP_CHOICE ||
            type == XSModelGroupImpl.MODELGROUP_SEQUENCE) {
            XSCMBinOp bin = (XSCMBinOp)node;
            node = fNodeFactory.getCMBinOpNode(type, copyNode(bin.getLeft()),
                                 copyNode(bin.getRight()));
        }
        // for ?+*, copy the subtree, and put it in a new ?+* node
        else if (type == XSParticleDecl.PARTICLE_ZERO_OR_MORE ||
                 type == XSParticleDecl.PARTICLE_ONE_OR_MORE ||
                 type == XSParticleDecl.PARTICLE_ZERO_OR_ONE) {
            XSCMUniOp uni = (XSCMUniOp)node;
            node = fNodeFactory.getCMUniOpNode(type, copyNode(uni.getChild()));
        }
        // for element/wildcard (leaf), make a new leaf node,
        // with a distinct position
        else if (type == XSParticleDecl.PARTICLE_ELEMENT ||
                 type == XSParticleDecl.PARTICLE_WILDCARD) {
            XSCMLeaf leaf = (XSCMLeaf)node;
            node = fNodeFactory.getCMLeafNode(leaf.type(), leaf.getLeaf(), leaf.getParticleId(), fLeafCount++);
        }

        return node;
    }

    // A special version of buildSyntaxTree() which builds a compact syntax tree
    // containing compound leaf nodes which carry occurence information. This method
    // for building the syntax tree is chosen over buildSyntaxTree() when
    // useRepeatingLeafNodes() returns true.
    private CMNode buildCompactSyntaxTree(XSParticleDecl particle) {
        int maxOccurs = particle.fMaxOccurs;
        int minOccurs = particle.fMinOccurs;
        short type = particle.fType;
        CMNode nodeRet = null;

        if ((type == XSParticleDecl.PARTICLE_WILDCARD) ||
            (type == XSParticleDecl.PARTICLE_ELEMENT)) {
            return buildCompactSyntaxTree2(particle, minOccurs, maxOccurs);
        }
        else if (type == XSParticleDecl.PARTICLE_MODELGROUP) {
            XSModelGroupImpl group = (XSModelGroupImpl)particle.fValue;
            if (group.fParticleCount == 1 && (minOccurs != 1 || maxOccurs != 1)) {
                return buildCompactSyntaxTree2(group.fParticles[0], minOccurs, maxOccurs);
            }
            else {
                CMNode temp = null;

                // when the model group is a choice of more than one particles, but
                // only one of the particle is not empty, (for example
                // <choice>
                //   <sequence/>
                //   <element name="e"/>
                // </choice>
                // ) we can't not return that one particle ("e"). instead, we should
                // treat such particle as optional ("e?").
                // the following int variable keeps track of the number of non-empty children
                int count = 0;
                for (int i = 0; i < group.fParticleCount; i++) {
                    // first convert each child to a CM tree
                    temp = buildCompactSyntaxTree(group.fParticles[i]);
                    // then combine them using binary operation
                    if (temp != null) {
                        ++count;
                        if (nodeRet == null) {
                            nodeRet = temp;
                        }
                        else {
                            nodeRet = fNodeFactory.getCMBinOpNode(group.fCompositor, nodeRet, temp);
                        }
                    }
                }
                if (nodeRet != null) {
                    // when the group is "choice" and the group has one or more empty children,
                    // we need to create a zero-or-one (optional) node for the non-empty particles.
                    if (group.fCompositor == XSModelGroupImpl.MODELGROUP_CHOICE && count < group.fParticleCount) {
                        nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ZERO_OR_ONE, nodeRet);
                    }
                }
            }
        }
        return nodeRet;
    }

    private CMNode buildCompactSyntaxTree2(XSParticleDecl particle, int minOccurs, int maxOccurs) {
        // Convert element and wildcard particles to leaf nodes. Wrap repeating particles in a CMUniOpNode.
        CMNode nodeRet = null;
        if (minOccurs == 1 && maxOccurs == 1) {
            nodeRet = fNodeFactory.getCMLeafNode(particle.fType, particle.fValue, fParticleCount++, fLeafCount++);
        }
        else if (minOccurs == 0 && maxOccurs == 1) {
            // zero or one
            nodeRet = fNodeFactory.getCMLeafNode(particle.fType, particle.fValue, fParticleCount++, fLeafCount++);
            nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ZERO_OR_ONE, nodeRet);
        }
        else if (minOccurs == 0 && maxOccurs==SchemaSymbols.OCCURRENCE_UNBOUNDED) {
            // zero or more
            nodeRet = fNodeFactory.getCMLeafNode(particle.fType, particle.fValue, fParticleCount++, fLeafCount++);
            nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ZERO_OR_MORE, nodeRet);
        }
        else if (minOccurs == 1 && maxOccurs==SchemaSymbols.OCCURRENCE_UNBOUNDED) {
            // one or more
            nodeRet = fNodeFactory.getCMLeafNode(particle.fType, particle.fValue, fParticleCount++, fLeafCount++);
            nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ONE_OR_MORE, nodeRet);
        }
        else {
            // {n,m}: Instead of expanding this out, create a compound leaf node which carries the
            // occurence information and wrap it in the appropriate CMUniOpNode.
            nodeRet = fNodeFactory.getCMRepeatingLeafNode(particle.fType, particle.fValue, minOccurs, maxOccurs, fParticleCount++, fLeafCount++);
            if (minOccurs == 0) {
                nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ZERO_OR_MORE, nodeRet);
            }
            else {
                nodeRet = fNodeFactory.getCMUniOpNode(XSParticleDecl.PARTICLE_ONE_OR_MORE, nodeRet);
            }
        }
        return nodeRet;
    }

    // This method checks if this particle can be transformed into a compact syntax
    // tree containing compound leaf nodes which carry occurence information. Currently
    // it returns true if each model group has minOccurs/maxOccurs == 1 or
    // contains only one element/wildcard particle with minOccurs/maxOccurs == 1.
    private boolean useRepeatingLeafNodes(XSParticleDecl particle) {
        int maxOccurs = particle.fMaxOccurs;
        int minOccurs = particle.fMinOccurs;
        short type = particle.fType;

        if (type == XSParticleDecl.PARTICLE_MODELGROUP) {
            XSModelGroupImpl group = (XSModelGroupImpl) particle.fValue;
            if (minOccurs != 1 || maxOccurs != 1) {
                if (group.fParticleCount == 1) {
                    XSParticleDecl particle2 = group.fParticles[0];
                    short type2 = particle2.fType;
                    return ((type2 == XSParticleDecl.PARTICLE_ELEMENT ||
                            type2 == XSParticleDecl.PARTICLE_WILDCARD) &&
                            particle2.fMinOccurs == 1 &&
                            particle2.fMaxOccurs == 1);
                }
                return (group.fParticleCount == 0);
            }
            for (int i = 0; i < group.fParticleCount; ++i) {
                if (!useRepeatingLeafNodes(group.fParticles[i])) {
                    return false;
                }
            }
        }
        return true;
    }
}
