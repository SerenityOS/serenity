/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public final class MemoryClassLoader extends ClassLoader {
    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private final MemoryFileManager manager = new MemoryFileManager(this.compiler);

    public Class<?> compile(String name, String content) {
        compile(new Source(name, content));
        try {
            return findClass(name);
        }
        catch (ClassNotFoundException exception) {
            throw new Error(exception);
        }
    }

    public void compile(Source... sources) {
        List<Source> list = new ArrayList<Source>();
        if (sources != null) {
            for (Source source : sources) {
                if (source != null) {
                    list.add(source);
                }
            }
        }
        synchronized (this.manager) {
            this.compiler.getTask(null, this.manager, null, null, null, list).call();
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        synchronized (this.manager) {
            Output mc = this.manager.map.remove(name);
            if (mc != null) {
                byte[] array = mc.toByteArray();
                return defineClass(name, array, 0, array.length);
            }
        }
        return super.findClass(name);
    }

    private static final class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final Map<String, Output> map = new HashMap<String, Output>();

        MemoryFileManager(JavaCompiler compiler) {
            super(compiler.getStandardFileManager(null, null, null));
        }

        @Override
        public Output getJavaFileForOutput(Location location, String name, Kind kind, FileObject source) {
            Output mc = this.map.get(name);
            if (mc == null) {
                mc = new Output(name);
                this.map.put(name, mc);
            }
            return mc;
        }
    }

    private static class MemoryFileObject extends SimpleJavaFileObject {
        MemoryFileObject(String name, Kind kind) {
            super(toURI(name, kind.extension), kind);
        }

        private static URI toURI(String name, String extension) {
            try {
                return new URI("mfm:///" + name.replace('.', '/') + extension);
            }
            catch (URISyntaxException exception) {
                throw new Error(exception);
            }
        }
    }

    private static final class Output extends MemoryFileObject {
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Output(String name) {
            super(name, Kind.CLASS);
        }

        byte[] toByteArray() {
            return this.baos.toByteArray();
        }

        @Override
        public ByteArrayOutputStream openOutputStream() {
            this.baos.reset();
            return this.baos;
        }
    }

    public static final class Source extends MemoryFileObject {
        private final String content;

        Source(String name, String content) {
            super(name, Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignore) {
            return this.content;
        }
    }
}
