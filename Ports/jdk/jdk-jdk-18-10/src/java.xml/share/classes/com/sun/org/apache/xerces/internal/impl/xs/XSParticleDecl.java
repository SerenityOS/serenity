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

package com.sun.org.apache.xerces.internal.impl.xs;

import com.sun.org.apache.xerces.internal.impl.xs.util.XSObjectListImpl;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSNamespaceItem;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import com.sun.org.apache.xerces.internal.xs.XSParticle;
import com.sun.org.apache.xerces.internal.xs.XSTerm;

/**
 * Store schema particle declaration.
 *
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 *
 */
public class XSParticleDecl implements XSParticle {

    // types of particles
    public static final short PARTICLE_EMPTY        = 0;
    public static final short PARTICLE_ELEMENT      = 1;
    public static final short PARTICLE_WILDCARD     = 2;
    public static final short PARTICLE_MODELGROUP   = 3;
    public static final short PARTICLE_ZERO_OR_MORE = 4;
    public static final short PARTICLE_ZERO_OR_ONE  = 5;
    public static final short PARTICLE_ONE_OR_MORE  = 6;

    // type of the particle
    public short fType = PARTICLE_EMPTY;

    // term of the particle
    // for PARTICLE_ELEMENT : the element decl
    // for PARTICLE_WILDCARD: the wildcard decl
    // for PARTICLE_MODELGROUP: the model group
    public XSTerm fValue = null;

    // minimum occurrence of this particle
    public int fMinOccurs = 1;
    // maximum occurrence of this particle
    public int fMaxOccurs = 1;
    // optional annotation
    public XSObjectList fAnnotations = null;

    // clone this decl
    public XSParticleDecl makeClone() {
        XSParticleDecl particle = new XSParticleDecl();
        particle.fType = fType;
        particle.fMinOccurs = fMinOccurs;
        particle.fMaxOccurs = fMaxOccurs;
        particle.fDescription = fDescription;
        particle.fValue = fValue;
        particle.fAnnotations = fAnnotations;
        return particle;
    }

    /**
     * 3.9.6 Schema Component Constraint: Particle Emptiable
     * whether this particle is emptible
     */
    public boolean emptiable() {
        return minEffectiveTotalRange() == 0;
    }

    // whether this particle contains nothing
    public boolean isEmpty() {
        if (fType == PARTICLE_EMPTY)
             return true;
        if (fType == PARTICLE_ELEMENT || fType == PARTICLE_WILDCARD)
            return false;

        return ((XSModelGroupImpl)fValue).isEmpty();
    }

    /**
     * 3.8.6 Effective Total Range (all and sequence) and
     *       Effective Total Range (choice)
     * The following methods are used to return min/max range for a particle.
     * They are not exactly the same as it's described in the spec, but all the
     * values from the spec are retrievable by these methods.
     */
    public int minEffectiveTotalRange() {
        if (fType == XSParticleDecl.PARTICLE_EMPTY) {
            return 0;
        }
        if (fType == PARTICLE_MODELGROUP) {
            return ((XSModelGroupImpl)fValue).minEffectiveTotalRange() * fMinOccurs;
        }
        return fMinOccurs;
    }

    public int maxEffectiveTotalRange() {
        if (fType == XSParticleDecl.PARTICLE_EMPTY) {
            return 0;
        }
        if (fType == PARTICLE_MODELGROUP) {
            int max = ((XSModelGroupImpl)fValue).maxEffectiveTotalRange();
            if (max == SchemaSymbols.OCCURRENCE_UNBOUNDED)
                return SchemaSymbols.OCCURRENCE_UNBOUNDED;
            if (max != 0 && fMaxOccurs == SchemaSymbols.OCCURRENCE_UNBOUNDED)
                return SchemaSymbols.OCCURRENCE_UNBOUNDED;
            return max * fMaxOccurs;
        }
        return fMaxOccurs;
    }

    /**
     * get the string description of this particle
     */
    private String fDescription = null;
    public String toString() {
        if (fDescription == null) {
            StringBuffer buffer = new StringBuffer();
            appendParticle(buffer);
            if (!(fMinOccurs == 0 && fMaxOccurs == 0 ||
                  fMinOccurs == 1 && fMaxOccurs == 1)) {
                buffer.append('{').append(fMinOccurs);
                if (fMaxOccurs == SchemaSymbols.OCCURRENCE_UNBOUNDED)
                    buffer.append("-UNBOUNDED");
                else if (fMinOccurs != fMaxOccurs)
                    buffer.append('-').append(fMaxOccurs);
                buffer.append('}');
            }
            fDescription = buffer.toString();
        }
        return fDescription;
    }

    /**
     * append the string description of this particle to the string buffer
     * this is for error message.
     */
    void appendParticle(StringBuffer buffer) {
        switch (fType) {
        case PARTICLE_EMPTY:
            buffer.append("EMPTY");
            break;
        case PARTICLE_ELEMENT:
            buffer.append(fValue.toString());
            break;
        case PARTICLE_WILDCARD:
            buffer.append('(');
            buffer.append(fValue.toString());
            buffer.append(')');
            break;
        case PARTICLE_MODELGROUP:
            buffer.append(fValue.toString());
            break;
        }
    }

    public void reset(){
        fType = PARTICLE_EMPTY;
        fValue = null;
        fMinOccurs = 1;
        fMaxOccurs = 1;
        fDescription = null;
        fAnnotations = null;
    }

    /**
     * Get the type of the object, i.e ELEMENT_DECLARATION.
     */
    public short getType() {
        return XSConstants.PARTICLE;
    }

    /**
     * The <code>name</code> of this <code>XSObject</code> depending on the
     * <code>XSObject</code> type.
     */
    public String getName() {
        return null;
    }

    /**
     * The namespace URI of this node, or <code>null</code> if it is
     * unspecified.  defines how a namespace URI is attached to schema
     * components.
     */
    public String getNamespace() {
        return null;
    }

    /**
     * {min occurs} determines the minimum number of terms that can occur.
     */
    public int getMinOccurs() {
        return fMinOccurs;
    }

    /**
     * {max occurs} whether the maxOccurs value is unbounded.
     */
    public boolean getMaxOccursUnbounded() {
        return fMaxOccurs == SchemaSymbols.OCCURRENCE_UNBOUNDED;
    }

    /**
     * {max occurs} determines the maximum number of terms that can occur.
     */
    public int getMaxOccurs() {
        return fMaxOccurs;
    }

    /**
     * {term} One of a model group, a wildcard, or an element declaration.
     */
    public XSTerm getTerm() {
        return fValue;
    }

        /**
         * @see org.apache.xerces.xs.XSObject#getNamespaceItem()
         */
        public XSNamespaceItem getNamespaceItem() {
                return null;
        }

    /**
     * Optional. Annotations.
     */
    public XSObjectList getAnnotations() {
        return (fAnnotations != null) ? fAnnotations : XSObjectListImpl.EMPTY_LIST;
    }

} // class XSParticleDecl
