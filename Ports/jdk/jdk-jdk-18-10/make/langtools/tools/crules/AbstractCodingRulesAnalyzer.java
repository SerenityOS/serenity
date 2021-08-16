/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package crules;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticType;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;
import com.sun.tools.javac.util.RawDiagnosticFormatter;

import static com.sun.source.util.TaskEvent.Kind;

public abstract class AbstractCodingRulesAnalyzer {

    private   final Log log;
    private   final boolean rawDiagnostics;
    private   final JCDiagnostic.Factory diags;
    private   final Options options;
    protected final Messages messages;
    protected final Symtab syms;
    protected final JavacElements elements;
    protected final JavacTypes types;
    protected TreeScanner treeVisitor;
    protected Kind eventKind;

    public AbstractCodingRulesAnalyzer(JavacTask task) {
        BasicJavacTask impl = (BasicJavacTask)task;
        Context context = impl.getContext();
        log = Log.instance(context);
        options = Options.instance(context);
        rawDiagnostics = options.isSet("rawDiagnostics");
        diags = JCDiagnostic.Factory.instance(context);
        messages = new Messages();
        syms = Symtab.instance(context);
        elements = JavacElements.instance(context);
        types = JavacTypes.instance(context);
    }

    protected class Messages {
        ResourceBundle bundle;

        Messages() {
            String name = getClass().getPackage().getName() + ".resources.crules";
            bundle = ResourceBundle.getBundle(name, Locale.ENGLISH);
        }

        public void error(JCTree tree, String code, Object... args) {
            String msg;
            if (rawDiagnostics) {
                RawDiagnosticFormatter f = new RawDiagnosticFormatter(options);
                msg = f.formatMessage(diags.create(DiagnosticType.FRAGMENT, log.currentSource(),
                                                   tree.pos(), code, args), null);
            } else {
                msg = (code == null) ? (String) args[0] : localize(code, args);
            }
            log.error(tree, "proc.messager", msg.toString());
        }

        private String localize(String code, Object... args) {
            String msg = bundle.getString(code);
            if (msg == null) {
                StringBuilder sb = new StringBuilder();
                sb.append("message file broken: code=").append(code);
                if (args.length > 0) {
                    sb.append(" arguments={0}");
                    for (int i = 1; i < args.length; i++) {
                        sb.append(", {").append(i).append("}");
                    }
                }
                msg = sb.toString();
            }
            return MessageFormat.format(msg, args);
        }
    }

}
