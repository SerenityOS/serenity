/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.parser;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;

/**
 *  A utility class to parse a string in a doc comment containing a
 *  reference to an API element, such as a type, field or method.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ReferenceParser {
    /**
     * An object to contain the result of parsing a reference to an API element.
     * Any, but not all, of the member fields may be null.
     */
    public static class Reference {
        public final JCTree.JCExpression moduleName;
        /** The type, if any, in the signature. */
        public final JCTree qualExpr;
        /** The member name, if any, in the signature. */
        public final Name member;
        /** The parameter types, if any, in the signature. */
        public final List<JCTree> paramTypes;

        Reference(JCTree.JCExpression moduleName, JCTree qualExpr, Name member, List<JCTree> paramTypes) {
            this.moduleName = moduleName;
            this.qualExpr = qualExpr;
            this.member = member;
            this.paramTypes = paramTypes;
        }
    }

    /**
     * An exception that indicates an error occurred while parsing a signature.
     */
    public static class ParseException extends Exception {
        private static final long serialVersionUID = 0;
        ParseException(String message) {
            super(message);
        }
    }

    private final ParserFactory fac;

    /**
     * Create a parser object to parse reference signatures.
     * @param fac a factory for parsing Java source code.
     */
    public ReferenceParser(ParserFactory fac) {
        this.fac = fac;
    }

    /**
     * Parse a reference to an API element as may be found in doc comment.
     * @param sig the signature to be parsed
     * @return a {@code Reference} object containing the result of parsing the signature
     * @throws ParseException if there is an error while parsing the signature
     */
    public Reference parse(String sig) throws ParseException {

        // Break sig apart into moduleName qualifiedExpr member paramTypes.
        JCTree.JCExpression moduleName;
        JCTree qualExpr;
        Name member;
        List<JCTree> paramTypes;

        Log.DeferredDiagnosticHandler deferredDiagnosticHandler
                = new Log.DeferredDiagnosticHandler(fac.log);

        try {
            int slash = sig.indexOf("/");
            int hash = sig.indexOf("#", slash + 1);
            int lparen = sig.indexOf("(", Math.max(slash, hash) + 1);
            if (slash > -1) {
                moduleName = parseModule(sig.substring(0, slash));
            } else {
                moduleName = null;
            }
            if (slash > 0 && sig.length() == slash + 1) {
                qualExpr = null;
                member = null;
            } else if (hash == -1) {
                if (lparen == -1) {
                    qualExpr = parseType(sig.substring(slash + 1));
                    member = null;
                } else {
                    qualExpr = null;
                    member = parseMember(sig.substring(slash + 1, lparen));
                }
            } else {
                qualExpr = (hash == slash + 1) ? null : parseType(sig.substring(slash + 1, hash));
                if (lparen == -1)
                    member = parseMember(sig.substring(hash + 1));
                else
                    member = parseMember(sig.substring(hash + 1, lparen));
            }

            if (lparen < 0) {
                paramTypes = null;
            } else {
                int rparen = sig.indexOf(")", lparen);
                if (rparen != sig.length() - 1)
                    throw new ParseException("dc.ref.bad.parens");
                paramTypes = parseParams(sig.substring(lparen + 1, rparen));
            }

            if (!deferredDiagnosticHandler.getDiagnostics().isEmpty())
                throw new ParseException("dc.ref.syntax.error");

        } finally {
            fac.log.popDiagnosticHandler(deferredDiagnosticHandler);
        }

        return new Reference(moduleName, qualExpr, member, paramTypes);
    }

    private JCTree.JCExpression parseModule(String s) throws ParseException {
        JavacParser p = fac.newParser(s, false, false, false);
        JCTree.JCExpression expr = p.qualident(false);
        if (p.token().kind != TokenKind.EOF)
            throw new ParseException("dc.ref.unexpected.input");
        return expr;
    }

    private JCTree parseType(String s) throws ParseException {
        JavacParser p = fac.newParser(s, false, false, false);
        JCTree tree = p.parseType();
        if (p.token().kind != TokenKind.EOF)
            throw new ParseException("dc.ref.unexpected.input");
        return tree;
    }

    private Name parseMember(String s) throws ParseException {
        JavacParser p = fac.newParser(s, false, false, false);
        Name name = p.ident();
        if (p.token().kind != TokenKind.EOF)
            throw new ParseException("dc.ref.unexpected.input");
        return name;
    }

    private List<JCTree> parseParams(String s) throws ParseException {
        if (s.trim().isEmpty())
            return List.nil();

        JavacParser p = fac.newParser(s.replace("...", "[]"), false, false, false);
        ListBuffer<JCTree> paramTypes = new ListBuffer<>();
        paramTypes.add(p.parseType());

        if (p.token().kind == TokenKind.IDENTIFIER)
            p.nextToken();

        while (p.token().kind == TokenKind.COMMA) {
            p.nextToken();
            paramTypes.add(p.parseType());

            if (p.token().kind == TokenKind.IDENTIFIER)
                p.nextToken();
        }

        if (p.token().kind != TokenKind.EOF)
            throw new ParseException("dc.ref.unexpected.input");

        if (new TypeAnnotationFinder().scan(paramTypes, null) != null)
            throw new ParseException("dc.ref.annotations.not.allowed");

        return paramTypes.toList();
    }

    static class TypeAnnotationFinder extends TreeScanner<Tree, Void> {
        @Override
        public Tree visitAnnotatedType(AnnotatedTypeTree t, Void ignore) {
            return t;
        }

        @Override
        public Tree reduce(Tree t1, Tree t2) {
            return t1 != null ? t1 : t2;
        }
    }
}