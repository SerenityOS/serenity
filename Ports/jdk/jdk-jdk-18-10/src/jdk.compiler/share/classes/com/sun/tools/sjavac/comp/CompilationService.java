/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.sjavac.comp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.main.Main.Result;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Dependencies;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.sjavac.Log;
import com.sun.tools.sjavac.Util;
import com.sun.tools.sjavac.comp.dependencies.NewDependencyCollector;
import com.sun.tools.sjavac.comp.dependencies.PublicApiCollector;
import com.sun.tools.sjavac.server.CompilationSubResult;
import com.sun.tools.sjavac.server.SysInfo;

/**
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class CompilationService {

    public SysInfo getSysInfo() {
        return new SysInfo(Runtime.getRuntime().availableProcessors(),
                           Runtime.getRuntime().maxMemory());
    }

    public CompilationSubResult compile(String protocolId,
                                     String invocationId,
                                     String[] args,
                                     List<File> explicitSources,
                                     Set<URI> sourcesToCompile,
                                     Set<URI> visibleSources) {

        JavacTool compiler = (JavacTool) ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            SmartFileManager sfm = new SmartFileManager(fm);
            Context context = new Context();

            Dependencies.GraphDependencies.preRegister(context);

            // Now setup the actual compilation
            CompilationSubResult compilationResult = new CompilationSubResult(Result.OK);

            // First deal with explicit source files on cmdline and in at file
            ListBuffer<JavaFileObject> explicitJFOs = new ListBuffer<>();
            for (JavaFileObject jfo : fm.getJavaFileObjectsFromFiles(explicitSources)) {
                explicitJFOs.append(SmartFileManager.locWrap(jfo, StandardLocation.SOURCE_PATH));
            }
            // Now deal with sources supplied as source_to_compile
            ListBuffer<File> sourcesToCompileFiles = new ListBuffer<>();
            for (URI u : sourcesToCompile)
                sourcesToCompileFiles.append(new File(u));

            for (JavaFileObject jfo : fm.getJavaFileObjectsFromFiles(sourcesToCompileFiles))
                explicitJFOs.append(SmartFileManager.locWrap(jfo, StandardLocation.SOURCE_PATH));

            // Create a log to capture compiler output
            StringWriter stderrLog = new StringWriter();
            Result result;
            PublicApiCollector pubApiCollector = new PublicApiCollector(context, explicitJFOs);
            PathAndPackageVerifier papVerifier = new PathAndPackageVerifier();
            NewDependencyCollector depsCollector = new NewDependencyCollector(context, explicitJFOs);
            try {
                if (explicitJFOs.size() > 0) {
                    sfm.setVisibleSources(visibleSources);
                    sfm.cleanArtifacts();

                    // Do the compilation!
                    JavacTaskImpl task =
                            (JavacTaskImpl) compiler.getTask(new PrintWriter(stderrLog),
                                                             sfm,
                                                             null,
                                                             Arrays.asList(args),
                                                             null,
                                                             explicitJFOs,
                                                             context);
                    sfm.setSymbolFileEnabled(!com.sun.tools.javac.util.Options.instance(context).isSet("ignore.symbol.file"));
                    task.addTaskListener(depsCollector);
                    task.addTaskListener(pubApiCollector);
                    task.addTaskListener(papVerifier);
                    logJavacInvocation(args);
                    result = task.doCall();
                    Log.debug("javac result: " + result);
                    sfm.flush();
                } else {
                    result = Result.ERROR;
                }
            } catch (Exception e) {
                Log.error(Util.getStackTrace(e));
                stderrLog.append(Util.getStackTrace(e));
                result = Result.ERROR;
            }

            compilationResult.packageArtifacts = sfm.getPackageArtifacts();

            if (papVerifier.errorsDiscovered()) {
                result = Result.ERROR;
            }

            compilationResult.packageDependencies = depsCollector.getDependencies(false);
            compilationResult.packageCpDependencies = depsCollector.getDependencies(true);

            compilationResult.packagePubapis = pubApiCollector.getPubApis(true);
            compilationResult.dependencyPubapis = pubApiCollector.getPubApis(false);
            compilationResult.stderr = stderrLog.toString();
            compilationResult.result = result;

            return compilationResult;
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private void logJavacInvocation(String[] args) {
        Log.debug("Invoking javac with args");
        Iterator<String> argIter = Arrays.asList(args).iterator();
        while (argIter.hasNext()) {
            String arg = argIter.next();
            String line = "    " + arg;
            if (arg.matches("\\-(d|cp|classpath|sourcepath|source|target)")
                    && argIter.hasNext()) {
                line += " " + argIter.next();
            }
            Log.debug(line);
        }
    }

}
