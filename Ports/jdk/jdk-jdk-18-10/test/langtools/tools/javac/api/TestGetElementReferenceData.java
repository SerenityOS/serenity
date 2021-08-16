/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

package test/*getElement:PACKAGE:test*/.nested/*getElement:PACKAGE:test.nested*/;
/*getElement:PACKAGE:test.nested*/
import java.lang.annotation.*;
import static test.nested.TestGetElementReferenceData.Sub.*;

public class TestGetElementReferenceData {

    private static void test() {
        StringBuilder/*getElement:CLASS:java.lang.StringBuilder*/ sb = new/*getElement:CONSTRUCTOR:java.lang.StringBuilder()*/ StringBuilder();
        sb/*getElement:LOCAL_VARIABLE:sb*/.append/*getElement:METHOD:java.lang.StringBuilder.append(int)*/(0);
        sb.reverse( /*getElement:METHOD:java.lang.StringBuilder.reverse()*/);
        java.util.List< /*getElement:INTERFACE:java.util.List*/ String> l;
        utility/*getElement:METHOD:test.nested.TestGetElementReferenceData.Base.utility()*/();
        target(TestGetElementReferenceData :: test/*getElement:METHOD:test.nested.TestGetElementReferenceData.test()*/);
        Object/*getElement:CLASS:java.lang.Object*/ o = null;
        if (o/*getElement:LOCAL_VARIABLE:o*/ instanceof String/*getElement:CLASS:java.lang.String*/ str/*getElement:BINDING_VARIABLE:str*/) ;
    }
    private static void target(Runnable r) { r.run(); }
    public static class Base {
        public static void utility() {}
    }
    public static class Sub extends @TypeAnnotation( /*getElement:ANNOTATION_TYPE:test.nested.TestGetElementReferenceData.TypeAnnotation*/) Base {
    }
   @Deprecated( /*getElement:ANNOTATION_TYPE:java.lang.Deprecated*/)
    public static class TypeParam<TT/*getElement:TYPE_PARAMETER:TT*/> {
    }
    @Target(ElementType.TYPE_USE)
    @interface TypeAnnotation {
    }
}
