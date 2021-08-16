/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Type;
import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.parser.SignatureParser;
import sun.reflect.generics.tree.FieldTypeSignature;
import sun.reflect.generics.tree.MethodTypeSignature;
import sun.reflect.generics.tree.TypeSignature;
import sun.reflect.generics.visitor.Reifier;



/**
 * This class represents the generic type information for a constructor.
 * The code is not dependent on a particular reflective implementation.
 * It is designed to be used unchanged by at least core reflection and JDI.
 */
public class ConstructorRepository
    extends GenericDeclRepository<MethodTypeSignature> {

    /** The generic parameter types.  Lazily initialized. */
    private volatile Type[] parameterTypes;

    /** The generic exception types.  Lazily initialized. */
    private volatile Type[] exceptionTypes;

    // protected, to enforce use of static factory yet allow subclassing
    protected ConstructorRepository(String rawSig, GenericsFactory f) {
      super(rawSig, f);
    }

    protected MethodTypeSignature parse(String s) {
        return SignatureParser.make().parseMethodSig(s);
    }

    /**
     * Static factory method.
     * @param rawSig - the generic signature of the reflective object
     * that this repository is servicing
     * @param f - a factory that will provide instances of reflective
     * objects when this repository converts its AST
     * @return a {@code ConstructorRepository} that manages the generic type
     * information represented in the signature {@code rawSig}
     */
    public static ConstructorRepository make(String rawSig, GenericsFactory f) {
        return new ConstructorRepository(rawSig, f);
    }

    /*
     * When queried for a particular piece of type information, the
     * general pattern is to consult the corresponding cached value.
     * If the corresponding field is non-null, it is returned.
     * If not, it is created lazily. This is done by selecting the appropriate
     * part of the tree and transforming it into a reflective object
     * using a visitor, which is created by feeding it the factory
     * with which the repository was created.
     */

    public Type[] getParameterTypes() {
        Type[] value = parameterTypes;
        if (value == null) {
            value = computeParameterTypes();
            parameterTypes = value;
        }
        return value.clone();
    }

    public Type[] getExceptionTypes() {
        Type[] value = exceptionTypes;
        if (value == null) {
            value = computeExceptionTypes();
            exceptionTypes = value;
        }
        return value.clone();
    }

    private Type[] computeParameterTypes() {
        // first, extract parameter type subtree(s) from AST
        TypeSignature[] pts = getTree().getParameterTypes();
        // create array to store reified subtree(s)
        int length = pts.length;
        Type[] parameterTypes = new Type[length];
        // reify all subtrees
        for (int i = 0; i < length; i++) {
            Reifier r = getReifier(); // obtain visitor
            pts[i].accept(r); // reify subtree
            // extract result from visitor and store it
            parameterTypes[i] = r.getResult();
        }
        return parameterTypes;
    }

    private Type[] computeExceptionTypes() {
        // first, extract exception type subtree(s) from AST
        FieldTypeSignature[] ets = getTree().getExceptionTypes();
        // create array to store reified subtree(s)
        int length = ets.length;
        Type[] exceptionTypes = new Type[length];
        // reify all subtrees
        for (int i = 0; i < length; i++) {
            Reifier r = getReifier(); // obtain visitor
            ets[i].accept(r); // reify subtree
            // extract result from visitor and store it
            exceptionTypes[i] = r.getResult();
        }
        return exceptionTypes;
    }
}
