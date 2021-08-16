/*
 * Copyright (c) 2001, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.comp;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import javax.tools.JavaFileObject;

import com.sun.tools.javac.util.Context;

/** A queue of all as yet unattributed classes.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Todo extends AbstractQueue<Env<AttrContext>> {
    /** The context key for the todo list. */
    protected static final Context.Key<Todo> todoKey = new Context.Key<>();

    /** Get the Todo instance for this context. */
    public static Todo instance(Context context) {
        Todo instance = context.get(todoKey);
        if (instance == null)
            instance = new Todo(context);
        return instance;
    }

    /** Create a new todo list. */
    protected Todo(Context context) {
        context.put(todoKey, this);
    }

    public void append(Env<AttrContext> env) {
        add(env);
    }

    @Override
    public Iterator<Env<AttrContext>> iterator() {
        return contents.iterator();
    }

    @Override
    public int size() {
        return contents.size();
    }

    public boolean offer(Env<AttrContext> e) {
        if (contents.add(e)) {
            if (contentsByFile != null)
                addByFile(e);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes all unattributed classes except those belonging to the given
     * collection of files.
     *
     * @param sourceFiles The source files of the classes to keep.
     */
    public void retainFiles(Collection<? extends JavaFileObject> sourceFiles) {
        for (Iterator<Env<AttrContext>> it = contents.iterator(); it.hasNext(); ) {
            Env<AttrContext> env = it.next();
            if (!sourceFiles.contains(env.toplevel.sourcefile)) {
                if (contentsByFile != null) removeByFile(env);
                it.remove();
            }
        }
    }

    public Env<AttrContext> poll() {
        if (size() == 0)
            return null;
        Env<AttrContext> env = contents.remove(0);
        if (contentsByFile != null)
            removeByFile(env);
        return env;
    }

    public Env<AttrContext> peek() {
        return (size() == 0 ? null : contents.get(0));
    }

    public Queue<Queue<Env<AttrContext>>> groupByFile() {
        if (contentsByFile == null) {
            contentsByFile = new LinkedList<>();
            for (Env<AttrContext> env: contents) {
                addByFile(env);
            }
        }
        return contentsByFile;
    }

    private void addByFile(Env<AttrContext> env) {
        JavaFileObject file = env.toplevel.sourcefile;
        if (fileMap == null)
            fileMap = new HashMap<>();
        FileQueue fq = fileMap.get(file);
        if (fq == null) {
            fq = new FileQueue();
            fileMap.put(file, fq);
            contentsByFile.add(fq);
        }
        fq.fileContents.add(env);
    }

    private void removeByFile(Env<AttrContext> env) {
        JavaFileObject file = env.toplevel.sourcefile;
        FileQueue fq = fileMap.get(file);
        if (fq == null)
            return;
        if (fq.fileContents.remove(env)) {
            if (fq.isEmpty()) {
                fileMap.remove(file);
                contentsByFile.remove(fq);
            }
        }
    }

    LinkedList<Env<AttrContext>> contents = new LinkedList<>();
    LinkedList<Queue<Env<AttrContext>>> contentsByFile;
    Map<JavaFileObject, FileQueue> fileMap;

    class FileQueue extends AbstractQueue<Env<AttrContext>> {
        @Override
        public Iterator<Env<AttrContext>> iterator() {
            return fileContents.iterator();
        }

        @Override
        public int size() {
            return fileContents.size();
        }

        public boolean offer(Env<AttrContext> e) {
            if (fileContents.offer(e)) {
                contents.add(e);
                return true;
            }
            return false;
        }

        public Env<AttrContext> poll() {
            if (fileContents.size() == 0)
                return null;
            Env<AttrContext> env = fileContents.remove(0);
            contents.remove(env);
            return env;
        }

        public Env<AttrContext> peek() {
            return (fileContents.size() == 0 ? null : fileContents.get(0));
        }

        LinkedList<Env<AttrContext>> fileContents = new LinkedList<>();
    }
}
