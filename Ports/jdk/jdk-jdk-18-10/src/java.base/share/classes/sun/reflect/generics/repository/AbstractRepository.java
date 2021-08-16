/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

package sun.reflect.generics.repository;

import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.tree.Tree;
import sun.reflect.generics.visitor.Reifier;


/**
 * Abstract superclass for representing the generic type information for
 * a reflective entity.
 * The code is not dependent on a particular reflective implementation.
 * It is designed to be used unchanged by at least core reflection and JDI.
 */
public abstract class AbstractRepository<T extends Tree> {

    // A factory used to produce reflective objects. Provided when the
    //repository is created. Will vary across implementations.
    private final GenericsFactory factory;

    private final T tree; // the AST for the generic type info

    //accessors
    private GenericsFactory getFactory() { return factory;}

    /**
     * Accessor for {@code tree}.
     * @return the cached AST this repository holds
     */
    protected T getTree(){ return tree;}

    /**
     * Returns a {@code Reifier} used to convert parts of the
     * AST into reflective objects.
     * @return a {@code Reifier} used to convert parts of the
     * AST into reflective objects
     */
    protected Reifier getReifier(){return Reifier.make(getFactory());}

    /**
     * Constructor. Should only be used by subclasses. Concrete subclasses
     * should make their constructors private and provide public factory
     * methods.
     * @param rawSig - the generic signature of the reflective object
     * that this repository is servicing
     * @param f - a factory that will provide instances of reflective
     * objects when this repository converts its AST
     */
    protected AbstractRepository(String rawSig, GenericsFactory f) {
        tree = parse(rawSig);
        factory = f;
    }

    /**
     * Returns the AST for the generic type info of this entity.
     * @param s - a string representing the generic signature of this
     * entity
     * @return the AST for the generic type info of this entity.
     */
    protected abstract T parse(String s);
}
