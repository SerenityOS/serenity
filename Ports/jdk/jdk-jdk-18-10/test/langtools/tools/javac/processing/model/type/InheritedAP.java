/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * @test
 * @bug 8024513
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build JavacTestingAbstractProcessor InheritedAP
 * @compile -cp . -processor InheritedAP -proc:only InheritedAP.java
 * @summary NPE in annotation processing
 */
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import java.lang.annotation.*;
import static javax.lang.model.type.TypeKind.*;
import static javax.lang.model.SourceVersion.*;
import static javax.lang.model.util.ElementFilter.*;

@SupportedAnnotationTypes("testclass")
public class InheritedAP extends JavacTestingAbstractProcessor {
    static Types types;
    public void init(ProcessingEnvironment penv) {super.init(penv);}
    public static Types getTypes() { return types; }

    public boolean process(Set<? extends TypeElement> typeElementSet,RoundEnvironment renv) {
        if ( renv.errorRaised()) { System.out.println("Error!"); return false; }
        if ( typeElementSet.size() <=0 && typesIn(renv.getRootElements()).size() <= 0 ) {
            return true;
        }
        types=processingEnv.getTypeUtils();
        for (TypeElement typeElem: typesIn(renv.getRootElements())) {
            if (typeElem.getAnnotation(testclass.class) != null) {
                new LocalElementScanner( new SimpleTypeMirrorVisitor()).scan(typeElem, null);
            }
        }
        return true ;
    }
}

class SimpleTypeMirrorVisitor extends JavacTestingAbstractProcessor.SimpleTypeVisitor<Void, Void> {
    protected Void defaultAction(TypeMirror mirror, Void p ) {
        try {
            System.out.println( "InheritedAP.getTypes().directSupertypes( "+mirror.toString()+" );" );
            InheritedAP.getTypes().directSupertypes(mirror);
            System.out.println("PASS");
        }catch(java.lang.IllegalArgumentException iae) {/*stuff*/ }
        return p;
    }
}

class LocalElementScanner <T extends JavacTestingAbstractProcessor.SimpleTypeVisitor<Void, Void> >
                    extends JavacTestingAbstractProcessor.ElementScanner<Void, Void> {
    JavacTestingAbstractProcessor.SimpleTypeVisitor<Void, Void> typeVisitor;

    public LocalElementScanner(T typeVisitor) { this.typeVisitor=typeVisitor;}

    @Override
    public Void scan(Element e, Void p) {
         if (e instanceof TypeElement ) {
            TypeElement te = (TypeElement) e;
            te.getSuperclass().accept(typeVisitor,p);
        }
        return p;
    }
}


@interface testclass { }

@testclass
@interface iface { }
