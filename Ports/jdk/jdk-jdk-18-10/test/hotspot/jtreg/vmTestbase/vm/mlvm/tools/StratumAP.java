/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.tools;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.io.File;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import nsk.share.jdi.sde.SmapGenerator;
import nsk.share.jdi.sde.SmapStratum;
import vm.mlvm.share.Stratum;
import vm.mlvm.tools.StratumAPTreeVisitor.StratumLineInfo;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

@SupportedAnnotationTypes("vm.mlvm.share.Stratum")
public class StratumAP extends AbstractProcessor {

    public static final String SMAP_EXT = ".smap";
    private boolean verbose = false;

    private Trees trees;
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        trees = Trees.instance(processingEnv);
        verbose = Boolean.parseBoolean(processingEnv.getOptions().get("verbose"));
    }

    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return true;
        }

        for (Element e : roundEnv.getElementsAnnotatedWith(Stratum.class)) {
            Stratum s = e.getAnnotation(Stratum.class);
            if (s == null) {
                continue;
            }

            TreePath tp = trees.getPath(e);

            StratumAPTreeVisitor visitor = new StratumAPTreeVisitor();
            visitor.scan(tp, trees);

            String stratumName = s.stratumName();
            String stratumSourceFileName = s.stratumSourceFileName();

            SmapStratum st = new SmapStratum(stratumName);
            st.addFile(stratumSourceFileName);

            Set<StratumLineInfo> lines = visitor.strata.get(stratumName);
            StringBuffer stratumSource = new StringBuffer();
            if (lines != null) {
                int curStratumLine = 1;
                for (StratumLineInfo lineInfo : lines) {
                    for (int i = -1; i <= 1; i++)
                        st.addLineData(curStratumLine,
                                       stratumSourceFileName,
                                       1,
                                       lineInfo.getJavaLineNum() + i,
                                       1);

                    stratumSource.append(lineInfo.getStratumSourceLine()).append("\n");
                    ++curStratumLine;
                }
            }

            if (verbose) {
                System.out.println("Strata:\n" + visitor.strata + "\n\nSource:\n" + stratumSource);
            }

            CompilationUnitTree compUnit = tp.getCompilationUnit();
            String pkgName = compUnit.getPackageName().toString();
            Filer filer = processingEnv.getFiler();

            SmapGenerator gen = new SmapGenerator();
            gen.addStratum(st, false);

            try {
                FileObject stratumFile = filer.createResource(StandardLocation.CLASS_OUTPUT, pkgName, stratumSourceFileName, e);

                if (verbose) {
                    System.out.println("Writing " + stratumFile.toUri());
                }

                Writer writer = stratumFile.openWriter();
                try {
                    writer.append(stratumSource);
                } finally {
                    writer.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return false;
            }

            String sourceFileName =
                compUnit.getSourceFile().getName()
                .replaceAll("^.*\\" + File.separatorChar, "");

            gen.setOutputFileName(sourceFileName);

            if (verbose) {
                System.out.println(gen.getString() + "\n");
            }

            String smapFileName =
                sourceFileName
                .replaceAll("\\..*$", "")
                + SMAP_EXT;

            try {
                FileObject smapFile = filer.createResource(StandardLocation.CLASS_OUTPUT, pkgName, smapFileName, e);

                if (verbose) {
                    System.out.println("Writing " + smapFile.toUri());
                }

                Writer writer = smapFile.openWriter();
                try {
                    writer.append(gen.getString());
                } finally {
                    writer.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return false;
            }
        }

        return true;
    }
}
