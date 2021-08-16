/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import static javax.tools.Diagnostic.Kind.*;

import java.util.Set;

@SupportedAnnotationTypes({"*"})
public class HelloWorldAP extends AbstractProcessor {
    Messager msgr = null;
    Filer filer = null;
    boolean DONE=false;

    @Override
    public void init(ProcessingEnvironment penv) {
        processingEnv = penv;
        msgr=penv.getMessager();
        filer=penv.getFiler();
    }

    public boolean process(Set<? extends TypeElement> tes, RoundEnvironment renv ) {
        boolean ret = true;
        if(!renv.processingOver() && !DONE) {
            msgr.printMessage(NOTE, "running process to create HelloWorld.");
            try {
                Writer pw = filer.createSourceFile("HelloWorld").openWriter();
                pw.write("public class HelloWorld {\n");
                pw.write("  public static void main (String argv[]) {\n");
                pw.write("    System.out.println(\"Hello apt world.\");\n");
                pw.write("  }\n");
                pw.write("}\n");
                pw.flush();
                pw.close();

                OutputStream os = filer.createClassFile("HelloWorldAP").openOutputStream();
                // the easiest way to create a class file is to copy another one
                InputStream is = getClass().getResourceAsStream("HelloWorldAP.class");
                copy(is, os);
                is.close();
                os.flush();
                os.close();
                DONE=true;
            }
            catch (IOException ioe) {
                msgr.printMessage(ERROR, ioe.getMessage());
                ret = false;
            }
            catch (Exception e) {
                msgr.printMessage(ERROR, e.getMessage());
                ret = false;
            }
        }
        return ret;
    }

    void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf, 0, buf.length)) > 0)
            os.write(buf, 0, n);
    }
}
