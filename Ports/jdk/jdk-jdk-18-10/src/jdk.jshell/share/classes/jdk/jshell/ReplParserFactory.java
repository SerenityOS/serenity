/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.util.Context;

/**
 *
 * @author Robert Field
 */
class ReplParserFactory extends ParserFactory {

    // force starting in expression mode
    private final boolean forceExpression;

    public static void preRegister(Context context, boolean forceExpression) {
        class Mark {}
        if (context.get(Mark.class) == null) { //don't register the factory if Context is reused
            context.put(parserFactoryKey, (Context.Factory<ParserFactory>)
                    (c -> new ReplParserFactory(c, forceExpression)));
            context.put(Mark.class, new Mark());
        }
    }

    private final ScannerFactory scannerFactory;
            final Source source;

    protected ReplParserFactory(Context context, boolean forceExpression) {
        super(context);
        this.forceExpression = forceExpression;
        this.scannerFactory = ScannerFactory.instance(context);
        this.source = Source.instance(context);
    }

    @Override
    public JavacParser newParser(CharSequence input, boolean keepDocComments, boolean keepEndPos, boolean keepLineMap) {
        com.sun.tools.javac.parser.Lexer lexer = scannerFactory.newScanner(input, keepDocComments);
        return new ReplParser(this, lexer, keepDocComments, keepLineMap, keepEndPos, forceExpression);
    }

    @Override
    public JavacParser newParser(CharSequence input, boolean keepDocComments, boolean keepEndPos, boolean keepLineMap, boolean parseModuleInfo) {
        return newParser(input, keepDocComments, keepEndPos, keepLineMap);
    }
}
