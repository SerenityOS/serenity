/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class FieldAnnotationsApp {
    @MyAnnotation(name="myField1",  value="myValue1")
    public String myField1 = null;

    @MyAnnotation(name="myField2",  value="myValue2")
    public String myField2 = null;

    public static void main(String args[]) throws Exception {
        for (int i=1; i<=2; i++) {
            Field field = FieldAnnotationsApp.class.getField("myField" + i);
            Annotation[] annotations = field.getDeclaredAnnotations();

            for (Annotation anno : annotations){
                if (anno instanceof MyAnnotation){
                    MyAnnotation myAnno = (MyAnnotation) anno;
                    String name = myAnno.name();
                    String value = myAnno.value();

                    System.out.println("Field         : " + field.getName());
                    System.out.println("  myAnno.name : " + name);
                    System.out.println("  myAnno.value: " + value);

                    if (!(name.equals("myField" + i) && value.equals("myValue" + i))) {
                        throw new Exception("Unexpected annotation values: " + i + " = " + value);
                    }
                }
            }
        }
        System.out.println("Field annotations are OK.");
    }
}
