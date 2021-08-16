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
 * Content model Uni-Op node.
 *
 * @xerces.internal
 *
 */
public class CMUniOp extends CMNode
{
    // -------------------------------------------------------------------
    //  Constructors
    // -------------------------------------------------------------------
    public CMUniOp(int type, CMNode childNode)
    {
        super(type);

        // Insure that its one of the types we require
        if ((type() != XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE)
        &&  (type() != XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE)
        &&  (type() != XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE))
        {
            throw new RuntimeException("ImplementationMessages.VAL_UST");
        }

        // Store the node and init any data that needs it
        fChild = childNode;
    }


    // -------------------------------------------------------------------
    //  Package, final methods
    // -------------------------------------------------------------------
    final CMNode getChild()
    {
        return fChild;
    }


    // -------------------------------------------------------------------
    //  Package, inherited methods
    // -------------------------------------------------------------------
    public boolean isNullable()
    {
        //
        //  For debugging purposes, make sure we got rid of all non '*'
        //  repetitions. Otherwise, '*' style nodes are always nullable.
        //
        if (type() == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE)
            return fChild.isNullable();
        else
            return true;
    }


    // -------------------------------------------------------------------
    //  Protected, inherited methods
    // -------------------------------------------------------------------
    protected void calcFirstPos(CMStateSet toSet)
    {
        // Its just based on our child node's first pos
        toSet.setTo(fChild.firstPos());
    }

    protected void calcLastPos(CMStateSet toSet)
    {
        // Its just based on our child node's last pos
        toSet.setTo(fChild.lastPos());
    }


    // -------------------------------------------------------------------
    //  Private data members
    //
    //  fChild
    //      This is the reference to the one child that we have for this
    //      unary operation.
    // -------------------------------------------------------------------
    private CMNode  fChild;
};
