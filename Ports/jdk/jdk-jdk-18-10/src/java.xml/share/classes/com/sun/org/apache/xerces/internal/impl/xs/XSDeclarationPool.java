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

import com.sun.org.apache.xerces.internal.impl.dv.xs.SchemaDVFactoryImpl;
import com.sun.org.apache.xerces.internal.impl.dv.xs.XSSimpleTypeDecl;

/**
 * This class is pool that enables caching of XML Schema declaration objects.
 * Before a compiled grammar object is garbage collected,
 * the implementation will add all XML Schema component
 * declarations to the pool.
 * Note: The cashing mechanism is not implemented yet.
 *
 * @xerces.internal
 *
 * @author Elena Litani, IBM
 */
public final class XSDeclarationPool {
    /** Chunk shift (8). */
    private static final int CHUNK_SHIFT = 8; // 2^8 = 256

    /** Chunk size (1 << CHUNK_SHIFT). */
    private static final int CHUNK_SIZE = 1 << CHUNK_SHIFT;

    /** Chunk mask (CHUNK_SIZE - 1). */
    private static final int CHUNK_MASK = CHUNK_SIZE - 1;

    /** Initial chunk count (). */
    private static final int INITIAL_CHUNK_COUNT = (1 << (10 - CHUNK_SHIFT)); // 2^10 = 1k

    /** Element declaration pool*/
    private XSElementDecl fElementDecl[][] = new XSElementDecl[INITIAL_CHUNK_COUNT][];
    private int fElementDeclIndex = 0;

    /** Particle declaration pool */
    private XSParticleDecl fParticleDecl[][] = new XSParticleDecl[INITIAL_CHUNK_COUNT][];
    private int fParticleDeclIndex = 0;

    /** Particle declaration pool */
    private XSModelGroupImpl fModelGroup[][] = new XSModelGroupImpl[INITIAL_CHUNK_COUNT][];
    private int fModelGroupIndex = 0;

    /** Attribute declaration pool */
    private XSAttributeDecl fAttrDecl[][] = new XSAttributeDecl[INITIAL_CHUNK_COUNT][];
    private int fAttrDeclIndex = 0;

    /** ComplexType declaration pool */
    private XSComplexTypeDecl fCTDecl[][] = new XSComplexTypeDecl[INITIAL_CHUNK_COUNT][];
    private int fCTDeclIndex = 0;

    /** SimpleType declaration pool */
    private XSSimpleTypeDecl fSTDecl[][] = new XSSimpleTypeDecl[INITIAL_CHUNK_COUNT][];
    private int fSTDeclIndex = 0;

    /** AttributeUse declaration pool */
    private XSAttributeUseImpl fAttributeUse[][] = new XSAttributeUseImpl[INITIAL_CHUNK_COUNT][];
    private int fAttributeUseIndex = 0;

    private SchemaDVFactoryImpl dvFactory;
    public void setDVFactory(SchemaDVFactoryImpl dvFactory) {
        this.dvFactory = dvFactory;
    }

    public final  XSElementDecl getElementDecl(){
        int     chunk       = fElementDeclIndex >> CHUNK_SHIFT;
        int     index       = fElementDeclIndex &  CHUNK_MASK;
        ensureElementDeclCapacity(chunk);
        if (fElementDecl[chunk][index] == null) {
            fElementDecl[chunk][index] = new XSElementDecl();
        } else {
            fElementDecl[chunk][index].reset();
        }
        fElementDeclIndex++;
        return fElementDecl[chunk][index];
    }

    public final XSAttributeDecl getAttributeDecl(){
        int     chunk       = fAttrDeclIndex >> CHUNK_SHIFT;
        int     index       = fAttrDeclIndex &  CHUNK_MASK;
        ensureAttrDeclCapacity(chunk);
        if (fAttrDecl[chunk][index] == null) {
            fAttrDecl[chunk][index] = new XSAttributeDecl();
        } else {
            fAttrDecl[chunk][index].reset();
        }
        fAttrDeclIndex++;
        return fAttrDecl[chunk][index];

    }

    public final XSAttributeUseImpl getAttributeUse(){
        int     chunk       = fAttributeUseIndex >> CHUNK_SHIFT;
        int     index       = fAttributeUseIndex &  CHUNK_MASK;
        ensureAttributeUseCapacity(chunk);
        if (fAttributeUse[chunk][index] == null) {
            fAttributeUse[chunk][index] = new XSAttributeUseImpl();
        } else {
            fAttributeUse[chunk][index].reset();
        }
        fAttributeUseIndex++;
        return fAttributeUse[chunk][index];

    }

    public final XSComplexTypeDecl getComplexTypeDecl(){
        int     chunk       = fCTDeclIndex >> CHUNK_SHIFT;
        int     index       = fCTDeclIndex &  CHUNK_MASK;
        ensureCTDeclCapacity(chunk);
        if (fCTDecl[chunk][index] == null) {

            fCTDecl[chunk][index] = new XSComplexTypeDecl();
        } else {
            fCTDecl[chunk][index].reset();
        }
        fCTDeclIndex++;
        return fCTDecl[chunk][index];
    }

    public final XSSimpleTypeDecl getSimpleTypeDecl(){
        int     chunk       = fSTDeclIndex >> CHUNK_SHIFT;
        int     index       = fSTDeclIndex &  CHUNK_MASK;
        ensureSTDeclCapacity(chunk);
        if (fSTDecl[chunk][index] == null) {
            fSTDecl[chunk][index] = dvFactory.newXSSimpleTypeDecl();
        } else {
            fSTDecl[chunk][index].reset();
        }
        fSTDeclIndex++;
        return fSTDecl[chunk][index];

    }

    public final XSParticleDecl getParticleDecl(){
        int     chunk       = fParticleDeclIndex >> CHUNK_SHIFT;
        int     index       = fParticleDeclIndex &  CHUNK_MASK;
        ensureParticleDeclCapacity(chunk);
        if (fParticleDecl[chunk][index] == null) {
            fParticleDecl[chunk][index] = new XSParticleDecl();
        } else {
            fParticleDecl[chunk][index].reset();
        }
        fParticleDeclIndex++;
        return fParticleDecl[chunk][index];
    }

    public final XSModelGroupImpl getModelGroup(){
        int     chunk       = fModelGroupIndex >> CHUNK_SHIFT;
        int     index       = fModelGroupIndex &  CHUNK_MASK;
        ensureModelGroupCapacity(chunk);
        if (fModelGroup[chunk][index] == null) {
            fModelGroup[chunk][index] = new XSModelGroupImpl();
        } else {
            fModelGroup[chunk][index].reset();
        }
        fModelGroupIndex++;
        return fModelGroup[chunk][index];
    }

    // REVISIT: do we need decl pool for group declarations, attribute group,
    //          notations?
    //          it seems like each schema would use a small number of those
    //          components, so it probably is not worth keeping those components
    //          in the pool.

    private boolean ensureElementDeclCapacity(int chunk) {
        if (chunk >= fElementDecl.length) {
            fElementDecl = resize(fElementDecl, fElementDecl.length * 2);
        } else if (fElementDecl[chunk] != null) {
            return false;
        }

        fElementDecl[chunk] = new XSElementDecl[CHUNK_SIZE];
        return true;
    }

    private static XSElementDecl[][] resize(XSElementDecl array[][], int newsize) {
        XSElementDecl newarray[][] = new XSElementDecl[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private boolean ensureParticleDeclCapacity(int chunk) {
        if (chunk >= fParticleDecl.length) {
            fParticleDecl = resize(fParticleDecl, fParticleDecl.length * 2);
        } else if (fParticleDecl[chunk] != null) {
            return false;
        }

        fParticleDecl[chunk] = new XSParticleDecl[CHUNK_SIZE];
        return true;
    }

    private boolean ensureModelGroupCapacity(int chunk) {
        if (chunk >= fModelGroup.length) {
            fModelGroup = resize(fModelGroup, fModelGroup.length * 2);
        } else if (fModelGroup[chunk] != null) {
            return false;
        }

        fModelGroup[chunk] = new XSModelGroupImpl[CHUNK_SIZE];
        return true;
    }

    private static XSParticleDecl[][] resize(XSParticleDecl array[][], int newsize) {
        XSParticleDecl newarray[][] = new XSParticleDecl[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static XSModelGroupImpl[][] resize(XSModelGroupImpl array[][], int newsize) {
        XSModelGroupImpl newarray[][] = new XSModelGroupImpl[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private boolean ensureAttrDeclCapacity(int chunk) {
        if (chunk >= fAttrDecl.length) {
            fAttrDecl = resize(fAttrDecl, fAttrDecl.length * 2);
        } else if (fAttrDecl[chunk] != null) {
            return false;
        }

        fAttrDecl[chunk] = new XSAttributeDecl[CHUNK_SIZE];
        return true;
    }

    private static XSAttributeDecl[][] resize(XSAttributeDecl array[][], int newsize) {
        XSAttributeDecl newarray[][] = new XSAttributeDecl[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private boolean ensureAttributeUseCapacity(int chunk) {
        if (chunk >= fAttributeUse.length) {
            fAttributeUse = resize(fAttributeUse, fAttributeUse.length * 2);
        } else if (fAttributeUse[chunk] != null) {
            return false;
        }

        fAttributeUse[chunk] = new XSAttributeUseImpl[CHUNK_SIZE];
        return true;
    }

    private static XSAttributeUseImpl[][] resize(XSAttributeUseImpl array[][], int newsize) {
        XSAttributeUseImpl newarray[][] = new XSAttributeUseImpl[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private boolean ensureSTDeclCapacity(int chunk) {
        if (chunk >= fSTDecl.length) {
            fSTDecl = resize(fSTDecl, fSTDecl.length * 2);
        } else if (fSTDecl[chunk] != null) {
            return false;
        }

        fSTDecl[chunk] = new XSSimpleTypeDecl[CHUNK_SIZE];
        return true;
    }

    private static XSSimpleTypeDecl[][] resize(XSSimpleTypeDecl array[][], int newsize) {
        XSSimpleTypeDecl newarray[][] = new XSSimpleTypeDecl[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private boolean ensureCTDeclCapacity(int chunk) {

        if (chunk >= fCTDecl.length) {
            fCTDecl = resize(fCTDecl, fCTDecl.length * 2);
        } else if (fCTDecl[chunk] != null){
            return false;
        }

        fCTDecl[chunk] = new XSComplexTypeDecl[CHUNK_SIZE];
        return true;
    }

    private static XSComplexTypeDecl[][] resize(XSComplexTypeDecl array[][], int newsize) {
        XSComplexTypeDecl newarray[][] = new XSComplexTypeDecl[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }



    public void reset(){
        fElementDeclIndex = 0;
        fParticleDeclIndex = 0;
        fModelGroupIndex = 0;
        fSTDeclIndex = 0;
        fCTDeclIndex = 0;
        fAttrDeclIndex = 0;
        fAttributeUseIndex = 0;
    }


}
