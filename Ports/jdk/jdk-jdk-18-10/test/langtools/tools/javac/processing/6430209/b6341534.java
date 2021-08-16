/*
 * Copyright (c) 2006, 2011, Oracle and/or its affiliates. All rights reserved.
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

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import static javax.lang.model.util.ElementFilter.*;
import static javax.tools.Diagnostic.Kind.*;
import java.util.*;
import java.util.Set;

public class b6341534 extends JavacTestingAbstractProcessor {
    static int r = 0;

    //Create directory 'dir1' and a test class in dir1
    public boolean process(Set<? extends TypeElement> tes, RoundEnvironment renv)
    {
        if(!renv.errorRaised() &&  !renv.processingOver()){
            r++;
            for( TypeElement t : typesIn(renv.getRootElements()) )
                System.out.println("Round"+r+ ": " + t.toString());

            try {
                PackageElement PE = eltUtils.getPackageElement("dir1");
                List<? extends Element> LEE = PE.getEnclosedElements(); //should not crash here
                for(Element e : LEE)
                    System.out.println("found " + e.toString() + " in dir1.");
            }
            catch(NullPointerException npe) {
                messager.printMessage(ERROR,npe.toString());
                //npe.printStackTrace();
                return false;
            }
        }
        // on round 1, expect errorRaised == false && processingOver == false
        // on round 2, expect errorRaised == true && processingOver == true
        if( renv.errorRaised() != renv.processingOver()) {
            messager.printMessage(ERROR, "FAILED: round:" + r
                + ", errorRaised:" + renv.errorRaised()
                + ", processingOver:" + renv.processingOver());
        }
        return true;
    }
}
