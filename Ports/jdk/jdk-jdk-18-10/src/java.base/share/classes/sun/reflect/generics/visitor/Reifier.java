/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
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

package sun.reflect.generics.visitor;


import java.lang.reflect.Type;
import java.util.List;
import java.util.Iterator;
import sun.reflect.generics.tree.*;
import sun.reflect.generics.factory.*;



/**
 * Visitor that converts AST to reified types.
 */
public class Reifier implements TypeTreeVisitor<Type> {
    private Type resultType;
    private final GenericsFactory factory;

    private Reifier(GenericsFactory f){
        factory = f;
    }

    private GenericsFactory getFactory(){ return factory;}

    /**
     * Factory method. The resulting visitor will convert an AST
     * representing generic signatures into corresponding reflective
     * objects, using the provided factory, {@code f}.
     * @param f - a factory that can be used to manufacture reflective
     * objects returned by this visitor
     * @return A visitor that can be used to reify ASTs representing
     * generic type information into reflective objects
     */
    public static Reifier make(GenericsFactory f){
        return new Reifier(f);
    }

    // Helper method. Visits an array of TypeArgument and produces
    // reified Type array.
    private Type[] reifyTypeArguments(TypeArgument[] tas) {
        Type[] ts = new Type[tas.length];
        for (int i = 0; i < tas.length; i++) {
            tas[i].accept(this);
            ts[i] = resultType;
        }
        return ts;
    }


    /**
     * Accessor for the result of the last visit by this visitor,
     * @return The type computed by this visitor based on its last
     * visit
     */
    public Type getResult() { assert resultType != null;return resultType;}

    public void visitFormalTypeParameter(FormalTypeParameter ftp){
        resultType = getFactory().makeTypeVariable(ftp.getName(),
                                                   ftp.getBounds());
    }


    public void visitClassTypeSignature(ClassTypeSignature ct){
        // This method examines the pathname stored in ct, which has the form
        // n1.n2...nk<targs>....
        // where n1 ... nk-1 might not exist OR
        // nk might not exist (but not both). It may be that k equals 1.
        // The idea is that nk is the simple class type name that has
        // any type parameters associated with it.
        //  We process this path in two phases.
        //  First, we scan until we reach nk (if it exists).
        //  If nk does not exist, this identifies a raw class n1 ... nk-1
        // which we can return.
        // if nk does exist, we begin the 2nd phase.
        // Here nk defines a parameterized type. Every further step nj (j > k)
        // down the path must also be represented as a parameterized type,
        // whose owner is the representation of the previous step in the path,
        // n{j-1}.

        // extract iterator on list of simple class type sigs
        List<SimpleClassTypeSignature> scts = ct.getPath();
        assert(!scts.isEmpty());
        Iterator<SimpleClassTypeSignature> iter = scts.iterator();
        SimpleClassTypeSignature sc = iter.next();
        StringBuilder n = new StringBuilder(sc.getName());
        boolean dollar = sc.getDollar();

        // phase 1: iterate over simple class types until
        // we are either done or we hit one with non-empty type parameters
        while (iter.hasNext() && sc.getTypeArguments().length == 0) {
            sc = iter.next();
            dollar = sc.getDollar();
            n.append(dollar?"$":".").append(sc.getName());
        }

        // Now, either sc is the last element of the list, or
        // it has type arguments (or both)
        assert(!(iter.hasNext()) || (sc.getTypeArguments().length > 0));
        // Create the raw type
        Type c = getFactory().makeNamedType(n.toString());
        // if there are no type arguments
        if (sc.getTypeArguments().length == 0) {
            //we have surely reached the end of the path
            assert(!iter.hasNext());
            resultType = c; // the result is the raw type
        } else {
            assert(sc.getTypeArguments().length > 0);
            // otherwise, we have type arguments, so we create a parameterized
            // type, whose declaration is the raw type c, and whose owner is
            // the declaring class of c (if any). This latter fact is indicated
            // by passing null as the owner.
            // First, we reify the type arguments
            Type[] pts = reifyTypeArguments(sc.getTypeArguments());

            Type owner = getFactory().makeParameterizedType(c, pts, null);
            // phase 2: iterate over remaining simple class types
            dollar =false;
            while (iter.hasNext()) {
                sc = iter.next();
                dollar = sc.getDollar();
                n.append(dollar?"$":".").append(sc.getName()); // build up raw class name
                c = getFactory().makeNamedType(n.toString()); // obtain raw class
                pts = reifyTypeArguments(sc.getTypeArguments());// reify params
                // Create a parameterized type, based on type args, raw type
                // and previous owner
                owner = getFactory().makeParameterizedType(c, pts, owner);
            }
            resultType = owner;
        }
    }

    public void visitArrayTypeSignature(ArrayTypeSignature a){
        // extract and reify component type
        a.getComponentType().accept(this);
        Type ct = resultType;
        resultType = getFactory().makeArrayType(ct);
    }

    public void visitTypeVariableSignature(TypeVariableSignature tv){
        resultType = getFactory().findTypeVariable(tv.getIdentifier());
    }

    public void visitWildcard(Wildcard w){
        resultType = getFactory().makeWildcard(w.getUpperBounds(),
                                               w.getLowerBounds());
    }

    public void visitSimpleClassTypeSignature(SimpleClassTypeSignature sct){
        resultType = getFactory().makeNamedType(sct.getName());
    }

    public void visitBottomSignature(BottomSignature b){

    }

    public void visitByteSignature(ByteSignature b){
        resultType = getFactory().makeByte();
    }

    public void visitBooleanSignature(BooleanSignature b){
        resultType = getFactory().makeBool();
    }

    public void visitShortSignature(ShortSignature s){
        resultType = getFactory().makeShort();
    }

    public void visitCharSignature(CharSignature c){
        resultType = getFactory().makeChar();
    }

    public void visitIntSignature(IntSignature i){
        resultType = getFactory().makeInt();
    }

    public void visitLongSignature(LongSignature l){
        resultType = getFactory().makeLong();
    }

    public void visitFloatSignature(FloatSignature f){
        resultType = getFactory().makeFloat();
    }

    public void visitDoubleSignature(DoubleSignature d){
        resultType = getFactory().makeDouble();
    }

    public void visitVoidDescriptor(VoidDescriptor v){
        resultType = getFactory().makeVoid();
    }


}
