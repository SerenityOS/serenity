/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.api;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.Processor;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.ParameterNameProvider;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskListener;
import com.sun.tools.doclint.DocLint;
import com.sun.tools.javac.code.MissingInfoHandler;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.platform.PlatformDescription;
import com.sun.tools.javac.platform.PlatformDescription.PluginInfo;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.resources.CompilerProperties.Warnings;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.ModuleHelper;
import com.sun.tools.javac.util.Options;
import com.sun.tools.javac.util.PropagatedException;

/**
 * Provides basic functionality for implementations of JavacTask.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own
 * risk.  This code and its internal interfaces are subject to change
 * or deletion without notice.</b></p>
 */
public class BasicJavacTask extends JavacTask {
    protected Context context;
    protected Options options;
    private TaskListener taskListener;

    public static JavacTask instance(Context context) {
        JavacTask instance = context.get(JavacTask.class);
        if (instance == null)
            instance = new BasicJavacTask(context, true);
        return instance;
    }

    public BasicJavacTask(Context c, boolean register) {
        context = c;
        options = Options.instance(c);
        if (register)
            context.put(JavacTask.class, this);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Iterable<? extends CompilationUnitTree> parse() {
        throw new IllegalStateException();
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Iterable<? extends Element> analyze() {
        throw new IllegalStateException();
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Iterable<? extends JavaFileObject> generate() {
        throw new IllegalStateException();
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public void setTaskListener(TaskListener tl) {
        MultiTaskListener mtl = MultiTaskListener.instance(context);
        if (taskListener != null)
            mtl.remove(taskListener);
        if (tl != null)
            mtl.add(tl);
        taskListener = tl;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public void addTaskListener(TaskListener taskListener) {
        MultiTaskListener mtl = MultiTaskListener.instance(context);
        mtl.add(taskListener);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public void removeTaskListener(TaskListener taskListener) {
        MultiTaskListener mtl = MultiTaskListener.instance(context);
        mtl.remove(taskListener);
    }

    @Override
    public void setParameterNameProvider(ParameterNameProvider handler) {
        MissingInfoHandler.instance(context).setDelegate(handler);
    }

    public Collection<TaskListener> getTaskListeners() {
        MultiTaskListener mtl = MultiTaskListener.instance(context);
        return mtl.getTaskListeners();
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public TypeMirror getTypeMirror(Iterable<? extends Tree> path) {
        // TODO: Should complete attribution if necessary
        Tree last = null;
        for (Tree node : path) {
            last = Objects.requireNonNull(node);
        }
        if (last == null) {
            throw new IllegalArgumentException("empty path");
        }
        return ((JCTree) last).type;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Elements getElements() {
        if (context == null)
            throw new IllegalStateException();
        return JavacElements.instance(context);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Types getTypes() {
        if (context == null)
            throw new IllegalStateException();
        return JavacTypes.instance(context);
    }

    @Override @DefinedBy(Api.COMPILER)
    public void addModules(Iterable<String> moduleNames) {
        throw new IllegalStateException();
    }

    @Override @DefinedBy(Api.COMPILER)
    public void setProcessors(Iterable<? extends Processor> processors) {
        throw new IllegalStateException();
    }

    @Override @DefinedBy(Api.COMPILER)
    public void setLocale(Locale locale) {
        throw new IllegalStateException();
    }

    @Override @DefinedBy(Api.COMPILER)
    public Boolean call() {
        throw new IllegalStateException();
    }

    /**
     * For internal use only.
     * This method will be removed without warning.
     * @return the context
     */
    public Context getContext() {
        return context;
    }

    public void initPlugins(Set<List<String>> pluginOpts) {
        PlatformDescription platformProvider = context.get(PlatformDescription.class);

        if (platformProvider != null) {
            for (PluginInfo<Plugin> pluginDesc : platformProvider.getPlugins()) {
                java.util.List<String> options =
                        pluginDesc.getOptions().entrySet().stream()
                                .map(e -> e.getKey() + "=" + e.getValue())
                                .toList();
                try {
                    initPlugin(pluginDesc.getPlugin(), options.toArray(new String[options.size()]));
                } catch (RuntimeException ex) {
                    throw new PropagatedException(ex);
                }
            }
        }

        Set<List<String>> pluginsToCall = new LinkedHashSet<>(pluginOpts);
        JavacProcessingEnvironment pEnv = JavacProcessingEnvironment.instance(context);
        ServiceLoader<Plugin> sl = pEnv.getServiceLoader(Plugin.class);
        Set<Plugin> autoStart = new LinkedHashSet<>();
        for (Plugin plugin : sl) {
            if (plugin.autoStart()) {
                autoStart.add(plugin);
            }
            for (List<String> p : pluginsToCall) {
                if (plugin.getName().equals(p.head)) {
                    pluginsToCall.remove(p);
                    autoStart.remove(plugin);
                    try {
                        initPlugin(plugin, p.tail.toArray(new String[p.tail.size()]));
                    } catch (RuntimeException ex) {
                        throw new PropagatedException(ex);
                    }
                    break;
                }
            }
        }
        for (List<String> p : pluginsToCall) {
            Log.instance(context).error(Errors.PluginNotFound(p.head));
        }
        for (Plugin plugin : autoStart) {
            try {
                initPlugin(plugin, new String[0]);
            } catch (RuntimeException ex) {
                throw new PropagatedException(ex);
            }

        }
    }

    private void initPlugin(Plugin p, String... args) {
        Module m = p.getClass().getModule();
        if (m.isNamed() && options.isSet("accessInternalAPI")) {
            ModuleHelper.addExports(getClass().getModule(), m);
        }
        p.init(this, args);
    }

    public void initDocLint(List<String> docLintOpts) {
        if (docLintOpts.isEmpty())
            return;
        try {
            DocLint.newDocLint().init(this, docLintOpts.toArray(new String[docLintOpts.size()]));
            JavaCompiler.instance(context).keepComments = true;
        } catch (IllegalStateException e) {
            Log.instance(context).warning(Warnings.DoclintNotAvailable);
        }
    }
}
