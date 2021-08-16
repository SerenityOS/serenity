/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.parser.Tokens.Comment.CommentStyle;
import com.sun.tools.javac.parser.Tokens.Token;
import static com.sun.tools.javac.parser.Tokens.TokenKind.CLASS;
import static com.sun.tools.javac.parser.Tokens.TokenKind.COLON;
import static com.sun.tools.javac.parser.Tokens.TokenKind.ENUM;
import static com.sun.tools.javac.parser.Tokens.TokenKind.EOF;
import static com.sun.tools.javac.parser.Tokens.TokenKind.IMPORT;
import static com.sun.tools.javac.parser.Tokens.TokenKind.INTERFACE;
import static com.sun.tools.javac.parser.Tokens.TokenKind.LPAREN;
import static com.sun.tools.javac.parser.Tokens.TokenKind.MONKEYS_AT;
import static com.sun.tools.javac.parser.Tokens.TokenKind.PACKAGE;
import static com.sun.tools.javac.parser.Tokens.TokenKind.SEMI;
import static com.sun.tools.javac.parser.Tokens.TokenKind.VOID;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCPackageDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.Tag;
import static com.sun.tools.javac.tree.JCTree.Tag.IDENT;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Position;
import com.sun.tools.javac.resources.CompilerProperties.Errors;

/**
 *
 * @author Robert Field
 */
class TrialParser extends JavacParser {

    public TrialParser(ParserFactory fac,
            com.sun.tools.javac.parser.Lexer S,
            boolean keepDocComments,
            boolean keepLineMap,
            boolean keepEndPositions) {
        super(fac, S, keepDocComments, keepLineMap, keepEndPositions);
    }

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

        if (token.kind == PACKAGE) {
            int packagePos = token.pos;
            List<JCAnnotation> annotations = List.nil();
            seenPackage = true;
            if (mods != null) {
                checkNoMods(mods.flags);
                annotations = mods.annotations;
                mods = null;
            }
            nextToken();
            JCExpression pid = qualident(false);
            accept(SEMI);
            JCPackageDecl pd = F.at(packagePos).PackageDecl(annotations, pid);
            attach(pd, firstToken.comment(CommentStyle.JAVADOC));
            storeEnd(pd, token.pos);
            defs.append(pd);
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
                break;
            } else {
                Comment docComment = token.comment(CommentStyle.JAVADOC);
                if (firstTypeDecl && !seenImport && !seenPackage) {
                    docComment = firstToken.comment(CommentStyle.JAVADOC);
                }
                List<? extends JCTree> udefs = aUnit(mods, docComment);
                for (JCTree def : udefs) {
                    defs.append(def);
                }
                mods = null;
                firstTypeDecl = false;
                break;
            }
        }
        List<JCTree> rdefs = defs.toList();
        class TrialUnit extends JCCompilationUnit {

            public TrialUnit(List<JCTree> defs) {
                super(defs);
            }
        }
        JCCompilationUnit toplevel = new TrialUnit(rdefs);
        if (rdefs.isEmpty()) {
            storeEnd(toplevel, S.prevToken().endPos);
        }
        toplevel.lineMap = S.getLineMap();
        this.endPosTable.setParser(null); // remove reference to parser
        toplevel.endPositions = this.endPosTable;
        return toplevel;
    }

    List<? extends JCTree> aUnit(JCModifiers pmods, Comment dc) {
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
            case SWITCH:
            case SYNCHRONIZED:
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
            default:
                JCModifiers mods = modifiersOpt(pmods);
                if (token.kind == CLASS
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
                        t = term(EXPR | TYPE);
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
                                    ? List.<JCTree>of(toP(F.at(pos).MethodDef(mods, name, t, typarams,
                                                            List.<JCVariableDecl>nil(), List.<JCExpression>nil(), null, null)))
                                    : null;
                            return List.<JCTree>of(syntaxError(token.pos, err, Errors.Expected(LPAREN)));
                        }
                    } else if (!typarams.isEmpty()) {
                        // type parameters on non-variable non-method -- error
                        return List.<JCTree>of(syntaxError(token.pos, Errors.IllegalStartOfType));
                    } else {
                        // expression-statement or expression to evaluate
                        accept(SEMI);
                        JCExpressionStatement expr = toP(F.at(pos).Exec(t));
                        return List.<JCTree>of(expr);
                    }

                }
        }
    }
}
