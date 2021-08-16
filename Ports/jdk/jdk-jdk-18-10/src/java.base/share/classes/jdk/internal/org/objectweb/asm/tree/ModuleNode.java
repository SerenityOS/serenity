/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package jdk.internal.org.objectweb.asm.tree;

import java.util.ArrayList;
import java.util.List;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ModuleVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

/**
 * A node that represents a module declaration.
 *
 * @author Remi Forax
 */
public class ModuleNode extends ModuleVisitor {

    /** The fully qualified name (using dots) of this module. */
    public String name;

    /**
      * The module's access flags, among {@code ACC_OPEN}, {@code ACC_SYNTHETIC} and {@code
      * ACC_MANDATED}.
      */
    public int access;

    /** The version of this module. May be {@literal null}. */
    public String version;

    /** The internal name of the main class of this module. May be {@literal null}. */
    public String mainClass;

    /** The internal name of the packages declared by this module. May be {@literal null}. */
    public List<String> packages;

    /** The dependencies of this module. May be {@literal null}. */
    public List<ModuleRequireNode> requires;

    /** The packages exported by this module. May be {@literal null}. */
    public List<ModuleExportNode> exports;

    /** The packages opened by this module. May be {@literal null}. */
    public List<ModuleOpenNode> opens;

    /** The internal names of the services used by this module. May be {@literal null}. */
    public List<String> uses;

    /** The services provided by this module. May be {@literal null}. */
    public List<ModuleProvideNode> provides;

    /**
      * Constructs a {@link ModuleNode}. <i>Subclasses must not use this constructor</i>. Instead, they
      * must use the {@link #ModuleNode(int,String,int,String,List,List,List,List,List)} version.
      *
      * @param name the fully qualified name (using dots) of the module.
      * @param access the module access flags, among {@code ACC_OPEN}, {@code ACC_SYNTHETIC} and {@code
      *     ACC_MANDATED}.
      * @param version the module version, or {@literal null}.
      * @throws IllegalStateException If a subclass calls this constructor.
      */
    public ModuleNode(final String name, final int access, final String version) {
        super(/* latest api = */ Opcodes.ASM8);
        if (getClass() != ModuleNode.class) {
            throw new IllegalStateException();
        }
        this.name = name;
        this.access = access;
        this.version = version;
    }

    // TODO(forax): why is there no 'mainClass' and 'packages' parameters in this constructor?
    /**
      * Constructs a {@link ModuleNode}.
      *
      * @param api the ASM API version implemented by this visitor. Must be one of {@link
      *     Opcodes#ASM6}, {@link Opcodes#ASM7} or {@link Opcodes#ASM8}.
      * @param name the fully qualified name (using dots) of the module.
      * @param access the module access flags, among {@code ACC_OPEN}, {@code ACC_SYNTHETIC} and {@code
      *     ACC_MANDATED}.
      * @param version the module version, or {@literal null}.
      * @param requires The dependencies of this module. May be {@literal null}.
      * @param exports The packages exported by this module. May be {@literal null}.
      * @param opens The packages opened by this module. May be {@literal null}.
      * @param uses The internal names of the services used by this module. May be {@literal null}.
      * @param provides The services provided by this module. May be {@literal null}.
      */
    public ModuleNode(
            final int api,
            final String name,
            final int access,
            final String version,
            final List<ModuleRequireNode> requires,
            final List<ModuleExportNode> exports,
            final List<ModuleOpenNode> opens,
            final List<String> uses,
            final List<ModuleProvideNode> provides) {
        super(api);
        this.name = name;
        this.access = access;
        this.version = version;
        this.requires = requires;
        this.exports = exports;
        this.opens = opens;
        this.uses = uses;
        this.provides = provides;
    }

    @Override
    public void visitMainClass(final String mainClass) {
        this.mainClass = mainClass;
    }

    @Override
    public void visitPackage(final String packaze) {
        if (packages == null) {
            packages = new ArrayList<>(5);
        }
        packages.add(packaze);
    }

    @Override
    public void visitRequire(final String module, final int access, final String version) {
        if (requires == null) {
            requires = new ArrayList<>(5);
        }
        requires.add(new ModuleRequireNode(module, access, version));
    }

    @Override
    public void visitExport(final String packaze, final int access, final String... modules) {
        if (exports == null) {
            exports = new ArrayList<>(5);
        }
        exports.add(new ModuleExportNode(packaze, access, Util.asArrayList(modules)));
    }

    @Override
    public void visitOpen(final String packaze, final int access, final String... modules) {
        if (opens == null) {
            opens = new ArrayList<>(5);
        }
        opens.add(new ModuleOpenNode(packaze, access, Util.asArrayList(modules)));
    }

    @Override
    public void visitUse(final String service) {
        if (uses == null) {
            uses = new ArrayList<>(5);
        }
        uses.add(service);
    }

    @Override
    public void visitProvide(final String service, final String... providers) {
        if (provides == null) {
            provides = new ArrayList<>(5);
        }
        provides.add(new ModuleProvideNode(service, Util.asArrayList(providers)));
    }

    @Override
    public void visitEnd() {
        // Nothing to do.
    }

    /**
      * Makes the given class visitor visit this module.
      *
      * @param classVisitor a class visitor.
      */
    public void accept(final ClassVisitor classVisitor) {
        ModuleVisitor moduleVisitor = classVisitor.visitModule(name, access, version);
        if (moduleVisitor == null) {
            return;
        }
        if (mainClass != null) {
            moduleVisitor.visitMainClass(mainClass);
        }
        if (packages != null) {
            for (int i = 0, n = packages.size(); i < n; i++) {
                moduleVisitor.visitPackage(packages.get(i));
            }
        }
        if (requires != null) {
            for (int i = 0, n = requires.size(); i < n; i++) {
                requires.get(i).accept(moduleVisitor);
            }
        }
        if (exports != null) {
            for (int i = 0, n = exports.size(); i < n; i++) {
                exports.get(i).accept(moduleVisitor);
            }
        }
        if (opens != null) {
            for (int i = 0, n = opens.size(); i < n; i++) {
                opens.get(i).accept(moduleVisitor);
            }
        }
        if (uses != null) {
            for (int i = 0, n = uses.size(); i < n; i++) {
                moduleVisitor.visitUse(uses.get(i));
            }
        }
        if (provides != null) {
            for (int i = 0, n = provides.size(); i < n; i++) {
                provides.get(i).accept(moduleVisitor);
            }
        }
    }
}
