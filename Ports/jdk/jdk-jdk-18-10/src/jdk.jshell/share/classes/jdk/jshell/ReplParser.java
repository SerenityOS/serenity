/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jshell;

import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.parser.Tokens.Comment.CommentStyle;
import com.sun.tools.javac.parser.Tokens.Token;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import static com.sun.tools.javac.parser.Tokens.TokenKind.CLASS;
import static com.sun.tools.javac.parser.Tokens.TokenKind.COLON;
import static com.sun.tools.javac.parser.Tokens.TokenKind.ENUM;
import static com.sun.tools.javac.parser.Tokens.TokenKind.EOF;
import static com.sun.tools.javac.parser.Tokens.TokenKind.IMPORT;
import static com.sun.tools.javac.parser.Tokens.TokenKind.INTERFACE;
import static com.sun.tools.javac.parser.Tokens.TokenKind.LPAREN;
import static com.sun.tools.javac.parser.Tokens.TokenKind.MONKEYS_AT;
import static com.sun.tools.javac.parser.Tokens.TokenKind.SEMI;
import static com.sun.tools.javac.parser.Tokens.TokenKind.SWITCH;
import static com.sun.tools.javac.parser.Tokens.TokenKind.VOID;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.Tag;
import static com.sun.tools.javac.tree.JCTree.Tag.IDENT;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Position;

/**
 * This is a subclass of JavacParser which overrides one method with a modified
 * verson of that method designed to allow parsing of one "snippet" of Java
 * code without the surrounding context of class, method, etc.
 * Accepts an expression, a statement, an import, or the declaration of a
 * method, variable, or type (class, interface, ...).
 */
class ReplParser extends JavacParser {

    // force starting in expression mode
    private final boolean forceExpression;
    private final Source source;

    public ReplParser(ReplParserFactory fac,
            com.sun.tools.javac.parser.Lexer S,
            boolean keepDocComments,
            boolean keepLineMap,
            boolean keepEndPositions,
            boolean forceExpression) {
        super(fac, S, keepDocComments, keepLineMap, keepEndPositions);
        this.forceExpression = forceExpression;
        this.source = fac.source;
    }

    /**
     * As faithful a clone of the overridden method as possible while still
     * achieving the goal of allowing the parse of a stand-alone snippet.
     * As a result, some variables are assigned and never used, tests are
     * always true, loops don't, etc.  This is to allow easy transition as the
     * underlying method changes.
     * @return a snippet wrapped in a compilation unit
     */
    @Override
    public JCCompilationUnit parseCompilationUnit() {
        Token firstToken = token;
        JCModifiers mods = null;
        boolean seenImport = false;
        boolean seenPackage = false;
        ListBuffer<JCTree> defs = new ListBuffer<>();
        if (token.kind == MONKEYS_AT) {
            mods = modifiersOpt();
        }

        boolean firstTypeDecl = true;
        while (token.kind != EOF) {
            if (token.pos > 0 && token.pos <= endPosTable.errorEndPos) {
                // error recovery
                skip(true, false, false, false);
                if (token.kind == EOF) {
                    break;
                }
            }
            if (mods == null && token.kind == IMPORT) {
                seenImport = true;
                defs.append(importDeclaration());
            } else {
                Comment docComment = token.comment(CommentStyle.JAVADOC);
                if (firstTypeDecl && !seenImport && !seenPackage) {
                    docComment = firstToken.comment(CommentStyle.JAVADOC);
                }
                List<? extends JCTree> udefs = replUnit(mods, docComment);
               // if (def instanceof JCExpressionStatement)
                //     def = ((JCExpressionStatement)def).expr;
                for (JCTree def : udefs) {
                    defs.append(def);
                }
                mods = null;
                firstTypeDecl = false;
            }
            break;  // Remove to process more than one snippet
        }
        List<JCTree> rdefs = defs.toList();
        class ReplUnit extends JCCompilationUnit {

            public ReplUnit(List<JCTree> defs) {
                super(defs);
            }
        }
        JCCompilationUnit toplevel = new ReplUnit(rdefs);
        if (rdefs.isEmpty()) {
            storeEnd(toplevel, S.prevToken().endPos);
        }
        toplevel.lineMap = S.getLineMap();
        this.endPosTable.setParser(null); // remove reference to parser
        toplevel.endPositions = this.endPosTable;
        return toplevel;
    }

    @SuppressWarnings("fallthrough")
     List<? extends JCTree> replUnit(JCModifiers pmods, Comment dc) {
        switch (token.kind) {
            case EOF:
                return List.nil();
            case RBRACE:
            case CASE:
            case DEFAULT:
                // These are illegal, fall through to handle as illegal statement
            case LBRACE:
            case IF:
            case FOR:
            case WHILE:
            case DO:
            case TRY:
            case RETURN:
            case THROW:
            case BREAK:
            case CONTINUE:
            case SEMI:
            case ELSE:
            case FINALLY:
            case CATCH:
            case ASSERT:
                return List.<JCTree>of(parseStatement());
            case SYNCHRONIZED:
                if (peekToken(LPAREN)) {
                    return List.<JCTree>of(parseStatement());
                }
                //fall-through
            case SWITCH:
                if (token.kind == SWITCH && !Feature.SWITCH_EXPRESSION.allowedInSource(source)) {
                    return List.<JCTree>of(parseStatement());
                }
                //fall-through
            default:
                JCModifiers mods = modifiersOpt(pmods);
                if (token.kind == CLASS
                        || isRecordStart()
                        || token.kind == INTERFACE
                        || token.kind == ENUM) {
                    return List.<JCTree>of(classOrRecordOrInterfaceOrEnumDeclaration(mods, dc));
                } else {
                    int pos = token.pos;
                    List<JCTypeParameter> typarams = typeParametersOpt();
                    // if there are type parameters but no modifiers, save the start
                    // position of the method in the modifiers.
                    if (typarams.nonEmpty() && mods.pos == Position.NOPOS) {
                        mods.pos = pos;
                        storeEnd(mods, pos);
                    }
                    List<JCAnnotation> annosAfterParams = annotationsOpt(Tag.ANNOTATION);

                    if (annosAfterParams.nonEmpty()) {
                        checkSourceLevel(annosAfterParams.head.pos, Feature.ANNOTATIONS_AFTER_TYPE_PARAMS);
                        mods.annotations = mods.annotations.appendList(annosAfterParams);
                        if (mods.pos == Position.NOPOS) {
                            mods.pos = mods.annotations.head.pos;
                        }
                    }

                    Token prevToken = token;
                    pos = token.pos;
                    JCExpression t;
                    boolean isVoid = token.kind == VOID;
                    if (isVoid) {
                        t = to(F.at(pos).TypeIdent(TypeTag.VOID));
                        nextToken();
                    } else {
                        // return type of method, declared type of variable, or an expression
                        // unless expression is being forced
                        t = term(forceExpression
                                ? EXPR
                                : EXPR | TYPE);
                    }
                    if (token.kind == COLON && t.hasTag(IDENT)) {
                        // labelled statement
                        nextToken();
                        JCStatement stat = parseStatement();
                        return List.<JCTree>of(F.at(pos).Labelled(prevToken.name(), stat));
                    } else if ((isVoid || (lastmode & TYPE) != 0) && LAX_IDENTIFIER.test(token.kind)) {
                        // we have "Type Ident", so we can assume it is variable or method declaration
                        pos = token.pos;
                        Name name = ident();
                        if (token.kind == LPAREN) {
                        // method declaration
                            //mods.flags |= Flags.STATIC;
                            return List.of(methodDeclaratorRest(
                                    pos, mods, t, name, typarams,
                                    false, isVoid, false, dc));
                        } else if (!isVoid && typarams.isEmpty()) {
                        // variable declaration
                            //mods.flags |= Flags.STATIC;
                            List<JCTree> defs
                                    = variableDeclaratorsRest(pos, mods, t, name, false, dc,
                                            new ListBuffer<JCTree>(), true).toList();
                            accept(SEMI);
                            storeEnd(defs.last(), S.prevToken().endPos);
                            return defs;
                        } else {
                            // malformed declaration, return error
                            pos = token.pos;
                            List<JCTree> err = isVoid
                                    ? List.of(toP(F.at(pos).MethodDef(mods, name, t, typarams,
                                                            List.nil(), List.nil(), null, null)))
                                    : null;
                            return List.<JCTree>of(syntaxError(token.pos, err, Errors.Expected(LPAREN)));
                        }
                    } else if (!typarams.isEmpty()) {
                        // type parameters on non-variable non-method -- error
                        return List.<JCTree>of(syntaxError(token.pos, Errors.IllegalStartOfType));
                    } else {
                        // expression-statement or expression to evaluate
                        JCExpressionStatement expr = toP(F.at(pos).Exec(t));
                        return List.<JCTree>of(expr);
                    }

                }
        }
    }
}
