/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.javac.code;

import java.util.HashMap;
import java.util.Map;

import com.sun.tools.javac.code.Kinds.Kind;
import com.sun.tools.javac.code.Scope.WriteableScope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.Completer;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.util.Context;

/** When a CompletionFailure is thrown when user code is running, it shouldn't be
 *  thrown out to the client code, but rather skipped, and then rethrown later if javac
 *  itself will complete the Symbol.
 *
 *  On all places where javac invokes client code (e.g. TaskListeners, annotation
 *  Processors), the {@code userCodeHandler} should be set using
 *  {@link DeferredCompletionFailureHandler#setHandler}, and the original handler
 *  should be restored when the control returns back to javac.
 *
 *  Implementations of API methods should use {@link Symbol#apiComplete()} instead of
 *  {@link Symbol#complete}, as the {@code apiComplete} method will invoke
 *  {@link DeferredCompletionFailureHandler#handleAPICompletionFailure }, which will
 *  catch the CompletionFailure and will either rethrow it or skip it, depending on
 *  the context.
 */
public class DeferredCompletionFailureHandler {

    protected static final Context.Key<DeferredCompletionFailureHandler> deferredCompletionFailureHandlerKey = new Context.Key<>();

    public static DeferredCompletionFailureHandler instance(Context context) {
        DeferredCompletionFailureHandler instance = context.get(deferredCompletionFailureHandlerKey);
        if (instance == null)
            instance = new DeferredCompletionFailureHandler(context);
        return instance;
    }

    public final Handler userCodeHandler = new Handler() {
        private final Map<ClassSymbol, FlipSymbolDescription> class2Flip = new HashMap<>();

        public void install() {
            class2Flip.values().forEach(f -> f.flip());
        }
        public void handleAPICompletionFailure(CompletionFailure cf) {
            //ignore
        }
        public void classSymbolCompleteFailed(ClassSymbol sym, Completer origCompleter) {
            class2Flip.put(sym, new FlipSymbolDescription(sym, new DeferredCompleter(origCompleter) {
                @Override public void complete(Symbol sym) throws CompletionFailure {
                    class2Flip.remove(sym);
                    super.complete(sym);
                }
            }));
        }
        public void classSymbolRemoved(ClassSymbol sym) {
            class2Flip.remove(sym);
        }
        public void uninstall() {
            class2Flip.values().forEach(f -> f.flip());
        }
    };

    public final Handler speculativeCodeHandler = new Handler() {
        private final Map<ClassSymbol, FlipSymbolDescription> class2Flip = new HashMap<>();

        public void install() {
        }
        public void handleAPICompletionFailure(CompletionFailure cf) {
            throw cf;
        }
        public void classSymbolCompleteFailed(ClassSymbol sym, Completer origCompleter) {
            class2Flip.put(sym, new FlipSymbolDescription(sym, new DeferredCompleter(origCompleter)));
        }
        public void classSymbolRemoved(ClassSymbol sym) {
            class2Flip.remove(sym);
        }
        public void uninstall() {
            class2Flip.values().forEach(f -> f.flip());
            class2Flip.clear();
        }
    };

    public final Handler javacCodeHandler = new Handler() {
        public void install() {
        }
        public void handleAPICompletionFailure(CompletionFailure cf) {
            throw cf;
        }
        public void classSymbolCompleteFailed(ClassSymbol sym, Completer origCompleter) {}
        public void classSymbolRemoved(ClassSymbol sym) {}
        public void uninstall() {
        }
    };

    private Handler handler = javacCodeHandler;

    protected DeferredCompletionFailureHandler(Context context) {
        context.put(deferredCompletionFailureHandlerKey, this);
    }

    public Handler setHandler(Handler h) {
        if (h == handler) return handler;

        handler.uninstall();
        Handler prev = handler;
        handler = h;
        handler.install();
        return prev;
    }

    public void handleAPICompletionFailure(CompletionFailure cf) {
        handler.handleAPICompletionFailure(cf);
    }

    public void classSymbolCompleteFailed(ClassSymbol sym, Completer origCompleter) {
        handler.classSymbolCompleteFailed(sym, origCompleter);
    }

    public void classSymbolRemoved(ClassSymbol sym) {
        handler.classSymbolRemoved(sym);
    }

    public boolean isDeferredCompleter(Completer c) {
        return c instanceof DeferredCompleter;
    }

    public interface Handler {
        public void install();
        public void handleAPICompletionFailure(CompletionFailure cf);
        public void classSymbolCompleteFailed(ClassSymbol sym, Completer origCompleter);
        public void classSymbolRemoved(ClassSymbol sym);
        public void uninstall();
    }

    private class DeferredCompleter implements Completer {

        private final Completer origCompleter;

        public DeferredCompleter(Completer origCompleter) {
            this.origCompleter = origCompleter;
        }

        @Override
        public void complete(Symbol sym) throws CompletionFailure {
            origCompleter.complete(sym);
        }
    }

    private static class FlipSymbolDescription {
        public final ClassSymbol sym;
        public Type type;
        public Kind kind;
        public WriteableScope members;
        public Completer completer;

        public FlipSymbolDescription(ClassSymbol sym, Completer completer) {
            this.sym = sym;
            this.type = sym.type;
            this.kind = sym.kind;
            this.members = null;
            this.completer = completer;
        }

        public void flip() {
            Type prevType = sym.type;
            sym.type = type;
            this.type = prevType;
            Kind prevKind = sym.kind;
            sym.kind = kind;
            this.kind = prevKind;
            Completer prevCompleter = sym.completer;
            sym.completer = completer;
            this.completer = prevCompleter;
            WriteableScope prevMembers = sym.members_field;
            sym.members_field = members;
            this.members = prevMembers;
        }

    }
}
