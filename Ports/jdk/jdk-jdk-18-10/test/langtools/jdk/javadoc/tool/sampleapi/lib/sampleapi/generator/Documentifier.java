/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package sampleapi.generator;

import java.util.ArrayList;
import java.util.Set;
import javax.lang.model.element.Modifier;

import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.DocCommentTable;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.parser.Tokens.Token;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.parser.Tokens.Comment.CommentStyle;
import com.sun.source.tree.Tree.Kind;

import sampleapi.util.*;

class Documentifier {

    static Documentifier instance;

    final DocCommentGenerator docGen;
    final ScannerFactory scanners;

    private Documentifier(Context context) {
        docGen = new DocCommentGenerator();
        scanners = ScannerFactory.instance(context);
    }

    public static Documentifier instance(Context context) {
        if (instance == null)
            instance = new Documentifier(context);
        return instance;
    }

    private DocCommentTable curDocComments;

    public void documentify(JCCompilationUnit topLevel, boolean isFxStyle) {
        JCClassDecl base = (JCClassDecl)topLevel.getTypeDecls().get(0);
        curDocComments = new PoorDocCommentTable();
        documentifyBase(base, true, isFxStyle);
        topLevel.docComments = curDocComments;
    }

    private void documentifyBase(JCClassDecl base, boolean isTopLevel, boolean isFxStyle) {
        // add doc comment to class itself
        Comment comm = comment(docGen.getBaseComment(base, isTopLevel));
        curDocComments.putComment(base, comm);

        // add doc comments to members
        for (JCTree member : base.getMembers()) {
            switch (member.getTag()) {
                case VARDEF:
                    documentifyField(base, (JCVariableDecl)member, isFxStyle);
                    break;
                case METHODDEF:
                    documentifyMethod(base, (JCMethodDecl)member, isFxStyle);
                    break;
                case CLASSDEF:
                    documentifyBase((JCClassDecl)member, false, isFxStyle);
                    break;
            }
        }
    }

    private void documentifyField(JCClassDecl base, JCVariableDecl field, boolean isFxStyle) {
        Kind baseKind = base.getKind();
        Set<Modifier> fieldMods = field.getModifiers().getFlags();
        String doc = (baseKind == Kind.ENUM
                      && fieldMods.contains(Modifier.PUBLIC)
                      && fieldMods.contains(Modifier.STATIC)
                      && fieldMods.contains(Modifier.FINAL)) ?
                     docGen.getConstComment() :
                     docGen.getFieldComment(base, field, isFxStyle);
        Comment comm = comment(doc);
        curDocComments.putComment(field, comm);
    }

    private void documentifyMethod(JCClassDecl base, JCMethodDecl method, boolean isFxStyle) {
        Comment comm = comment(docGen.getMethodComment(base, method, isFxStyle));
        curDocComments.putComment(method, comm);
    }

    private Comment comment(String docString) {
        StringBuilder docComment = new StringBuilder()
                                   .append("/**")
                                   .append(docString)
                                   .append("*/");
        Scanner scanner = scanners.newScanner(docComment, true);
        scanner.nextToken();
        Token token = scanner.token();
        return token.comment(CommentStyle.JAVADOC);
    }

    // provide package comment data ONLY
    public DocCommentGenerator getDocGenerator() {
        return docGen;
    }
}
