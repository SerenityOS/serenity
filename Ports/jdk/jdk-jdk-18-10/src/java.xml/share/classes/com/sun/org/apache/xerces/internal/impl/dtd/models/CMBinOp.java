/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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

package com.sun.org.apache.xerces.internal.impl.dtd.models;

import com.sun.org.apache.xerces.internal.impl.dtd.XMLContentSpec;

/**
 * Content model Bin-Op node.
 *
 * @xerces.internal
 *
 */
public class CMBinOp extends CMNode
{
    // -------------------------------------------------------------------
    //  Constructors
    // -------------------------------------------------------------------
    public CMBinOp(int type, CMNode leftNode, CMNode rightNode)
    {
        super(type);

        // Insure that its one of the types we require
        if ((type() != XMLContentSpec.CONTENTSPECNODE_CHOICE)
        &&  (type() != XMLContentSpec.CONTENTSPECNODE_SEQ))
        {
            throw new RuntimeException("ImplementationMessages.VAL_BST");
        }

        // Store the nodes and init any data that needs it
        fLeftChild = leftNode;
        fRightChild = rightNode;
    }


    // -------------------------------------------------------------------
    //  Package, final methods
    // -------------------------------------------------------------------
    final CMNode getLeft()
    {
        return fLeftChild;
    }

    final CMNode getRight()
    {
        return fRightChild;
    }


    // -------------------------------------------------------------------
    //  Package, inherited methods
    // -------------------------------------------------------------------
    public boolean isNullable()
    {
        //
        //  If its an alternation, then if either child is nullable then
        //  this node is nullable. If its a concatenation, then both of
        //  them have to be nullable.
        //
        if (type() == XMLContentSpec.CONTENTSPECNODE_CHOICE)
            return (fLeftChild.isNullable() || fRightChild.isNullable());
        else if (type() == XMLContentSpec.CONTENTSPECNODE_SEQ)
            return (fLeftChild.isNullable() && fRightChild.isNullable());
        else
            throw new RuntimeException("ImplementationMessages.VAL_BST");
    }


    // -------------------------------------------------------------------
    //  Protected, inherited methods
    // -------------------------------------------------------------------
    protected void calcFirstPos(CMStateSet toSet)
    {
        if (type() == XMLContentSpec.CONTENTSPECNODE_CHOICE)
        {
            // Its the the union of the first positions of our children.
            toSet.setTo(fLeftChild.firstPos());
            toSet.union(fRightChild.firstPos());
        }
         else if (type() == XMLContentSpec.CONTENTSPECNODE_SEQ)
        {
            //
            //  If our left child is nullable, then its the union of our
            //  children's first positions. Else is our left child's first
            //  positions.
            //
            toSet.setTo(fLeftChild.firstPos());
            if (fLeftChild.isNullable())
                toSet.union(fRightChild.firstPos());
        }
         else
        {
            throw new RuntimeException("ImplementationMessages.VAL_BST");
        }
    }

    protected void calcLastPos(CMStateSet toSet)
    {
        if (type() == XMLContentSpec.CONTENTSPECNODE_CHOICE)
        {
            // Its the the union of the first positions of our children.
            toSet.setTo(fLeftChild.lastPos());
            toSet.union(fRightChild.lastPos());
        }
         else if (type() == XMLContentSpec.CONTENTSPECNODE_SEQ)
        {
            //
            //  If our right child is nullable, then its the union of our
            //  children's last positions. Else is our right child's last
            //  positions.
            //
            toSet.setTo(fRightChild.lastPos());
            if (fRightChild.isNullable())
                toSet.union(fLeftChild.lastPos());
        }
         else
        {
            throw new RuntimeException("ImplementationMessages.VAL_BST");
        }
    }


    // -------------------------------------------------------------------
    //  Private data members
    //
    //  fLeftChild
    //  fRightChild
    //      These are the references to the two nodes that are on either
    //      side of this binary operation.
    // -------------------------------------------------------------------
    private CMNode  fLeftChild;
    private CMNode  fRightChild;
};
