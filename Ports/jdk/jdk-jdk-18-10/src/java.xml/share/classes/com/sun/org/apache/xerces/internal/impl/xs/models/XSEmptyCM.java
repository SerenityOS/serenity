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

import com.sun.org.apache.xerces.internal.impl.xs.SubstitutionGroupHandler;
import com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaException;
import com.sun.org.apache.xerces.internal.xni.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * XSEmptyCM is a derivative of the abstract content model base class that
 * handles a content model with no chilren (elements).
 *
 * This model validated on the way in.
 *
 * @xerces.internal
 *
 * @author Elena Litani, Lisa Martin
 * @author IBM
 * @LastModified: Oct 2017
 */
public class XSEmptyCM  implements XSCMValidator {

    //
    // Constants
    //

    // start the content model: did not see any children
    private static final short STATE_START = 0;

    private static final List<Object> EMPTY = new ArrayList<>(0);

    //
    // Data
    //

    //
    // XSCMValidator methods
    //

    /**
     * This methods to be called on entering a first element whose type
     * has this content model. It will return the initial state of the content model
     *
     * @return Start state of the content model
     */
    public int[] startContentModel(){
        return (new int[] {STATE_START});
    }


    /**
     * The method corresponds to one transaction in the content model.
     *
     * @param elementName the qualified name of the element
     * @param currentState Current state
     * @param subGroupHandler the substitution group handler
     * @return element index corresponding to the element from the Schema grammar
     */
    public Object oneTransition (QName elementName, int[] currentState, SubstitutionGroupHandler subGroupHandler){

        // error state
        if (currentState[0] < 0) {
            currentState[0] = XSCMValidator.SUBSEQUENT_ERROR;
            return null;
        }

        currentState[0] = XSCMValidator.FIRST_ERROR;
        return null;
    }


    /**
     * The method indicates the end of list of children
     *
     * @param currentState Current state of the content model
     * @return true if the last state was a valid final state
     */
    public boolean endContentModel (int[] currentState){
        boolean isFinal =  false;
        int state = currentState[0];

        // restore content model state:

        // error
        if (state < 0) {
            return false;
        }


        return true;
    }

    /**
     * check whether this content violates UPA constraint.
     *
     * @param subGroupHandler the substitution group handler
     * @return true if this content model contains other or list wildcard
     */
    public boolean checkUniqueParticleAttribution(SubstitutionGroupHandler subGroupHandler) throws XMLSchemaException {
        return false;
    }

    /**
     * Check which elements are valid to appear at this point. This method also
     * works if the state is in error, in which case it returns what should
     * have been seen.
     *
     * @param state  the current state
     * @return       a list whose entries are instances of
     *               either XSWildcardDecl or XSElementDecl.
     */
    public List<Object> whatCanGoHere(int[] state) {
        return EMPTY;
    }

    public List<String> checkMinMaxBounds() {
        return null;
    }

    public int [] occurenceInfo(int[] state) {
        return null;
    }

    public String getTermName(int termId) {
        return null;
    }

    public boolean isCompactedForUPA() {
        return false;
    }
} // class XSEmptyCM
