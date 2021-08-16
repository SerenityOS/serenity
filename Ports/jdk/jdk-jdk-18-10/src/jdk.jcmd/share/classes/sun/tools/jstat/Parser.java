/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jstat;

import java.io.*;
import java.util.*;

/**
 * A class implementing a simple predictive parser for output format
 * specification language for the jstat command.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class Parser {

    private static boolean pdebug = Boolean.getBoolean("jstat.parser.debug");
    private static boolean ldebug = Boolean.getBoolean("jstat.lex.debug");

    private static final char OPENBLOCK = '{';
    private static final char CLOSEBLOCK = '}';
    private static final char DOUBLEQUOTE = '"';
    private static final char PERCENT_CHAR = '%';
    private static final char OPENPAREN = '(';
    private static final char CLOSEPAREN = ')';

    private static final char OPERATOR_PLUS = '+';
    private static final char OPERATOR_MINUS = '-';
    private static final char OPERATOR_MULTIPLY = '*';
    private static final char OPERATOR_DIVIDE = '/';

    private static final String OPTION = "option";
    private static final String COLUMN = "column";
    private static final String DATA = "data";
    private static final String HEADER = "header";
    private static final String WIDTH = "width";
    private static final String FORMAT = "format";
    private static final String ALIGN = "align";
    private static final String SCALE = "scale";
    private static final String REQUIRED = "required";

    private static final String START = OPTION;

    private static final Set<String> scaleKeyWords = Scale.keySet();
    private static final Set<String> alignKeyWords = Alignment.keySet();
    private static final Set<String> boolKeyWords = Set.of("true", "false");
    private static String[] otherKeyWords = {
        OPTION, COLUMN, DATA, HEADER, WIDTH, FORMAT, ALIGN, SCALE, REQUIRED
    };

    private static char[] infixOps = {
        OPERATOR_PLUS, OPERATOR_MINUS, OPERATOR_MULTIPLY, OPERATOR_DIVIDE
    };

    private static char[] delimiters = {
        OPENBLOCK, CLOSEBLOCK, PERCENT_CHAR, OPENPAREN, CLOSEPAREN
    };


    private static Set<String> reservedWords;

    private StreamTokenizer st;
    private String filename;
    private Token lookahead;
    private Token previous;
    private int columnCount;
    private OptionFormat optionFormat;

    public Parser(String filename) throws FileNotFoundException {
        this.filename = filename;
        Reader r = new BufferedReader(new FileReader(filename));
    }

    public Parser(Reader r) {
        st = new StreamTokenizer(r);

        // allow both c++ style comments
        st.ordinaryChar('/');
        st.wordChars('_','_');
        st.slashSlashComments(true);
        st.slashStarComments(true);

        reservedWords = new HashSet<String>();
        for (int i = 0; i < otherKeyWords.length; i++) {
            reservedWords.add(otherKeyWords[i]);
        }

        for (int i = 0; i < delimiters.length; i++ ) {
            st.ordinaryChar(delimiters[i]);
        }

        for (int i = 0; i < infixOps.length; i++ ) {
            st.ordinaryChar(infixOps[i]);
        }
    }

    /**
     * push back the lookahead token and restore the lookahead token
     * to the previous token.
     */
    private void pushBack() {
        lookahead = previous;
        st.pushBack();
    }

    /**
     * retrieve the next token, placing the token value in the lookahead
     * member variable, storing its previous value in the previous member
     * variable.
     */
    private void nextToken() throws ParserException, IOException {
        int t = st.nextToken();
        previous = lookahead;
        lookahead = new Token(st.ttype, st.sval, st.nval);
        log(ldebug, "lookahead = " + lookahead);
    }

    /**
     * match one of the token values in the given set of key words
     * token is assumed to be of type TT_WORD, and the set is assumed
     * to contain String objects.
     */
    private Token matchOne(Set<String> keyWords) throws ParserException, IOException {
        if ((lookahead.ttype == StreamTokenizer.TT_WORD)
                && keyWords.contains(lookahead.sval)) {
            Token t = lookahead;
            nextToken();
            return t;
        }
        throw new SyntaxException(st.lineno(), keyWords, lookahead);
    }

    /**
     * match a token with TT_TYPE=type, and the token value is a given sequence
     * of characters.
     */
    private void match(int ttype, String token)
                 throws ParserException, IOException {
        if (lookahead.ttype == ttype && lookahead.sval.compareTo(token) == 0) {
            nextToken();
        } else {
           throw new SyntaxException(st.lineno(), new Token(ttype, token),
                                     lookahead);
        }
    }

    /**
     * match a token with TT_TYPE=type
     */
    private void match(int ttype) throws ParserException, IOException {
        if (lookahead.ttype == ttype) {
            nextToken();
        } else {
           throw new SyntaxException(st.lineno(), new Token(ttype), lookahead);
        }
    }

    /**
     * match a token with TT_TYPE=char, where the token value is the given char.
     */
    private void match(char ttype) throws ParserException, IOException {
      if (lookahead.ttype == (int)ttype) {
          nextToken();
      }
      else {
          throw new SyntaxException(st.lineno(), new Token((int)ttype),
                                    lookahead);
      }
    }

    /**
     * match a token with TT_TYPE='"', where the token value is a sequence
     * of characters between matching quote characters.
     */
    private void matchQuotedString() throws ParserException, IOException {
        match(DOUBLEQUOTE);
    }

    /**
     * match a TT_NUMBER token that matches a parsed number value
     */
    private void matchNumber() throws ParserException, IOException {
        match(StreamTokenizer.TT_NUMBER);
    }

    /**
     * match a TT_WORD token that matches an arbitrary, not quoted token.
     */
    private void matchID() throws ParserException, IOException {
        match(StreamTokenizer.TT_WORD);
    }

    /**
     * match a TT_WORD token that matches the given string
     */
    private void match(String token) throws ParserException, IOException {
        match(StreamTokenizer.TT_WORD, token);
    }

    /**
     * determine if the given word is a reserved key word
     */
    private boolean isReservedWord(String word) {
        return reservedWords.contains(word);
    }

    /**
     * determine if the give work is a reserved key word
     */
    private boolean isInfixOperator(char op) {
        for (int i = 0; i < infixOps.length; i++) {
            if (op == infixOps[i]) {
                return true;
            }
        }
        return false;
    }

    /**
     * scalestmt -> 'scale' scalespec
     * scalespec -> <see above scaleTerminals array>
     */
    private void scaleStmt(ColumnFormat cf)
                 throws ParserException, IOException {
        match(SCALE);
        Token t = matchOne(scaleKeyWords);
        cf.setScale(Scale.toScale(t.sval));
        String scaleString = t.sval;
        log(pdebug, "Parsed: scale -> " + scaleString);
    }

    /**
     * alignstmt -> 'align' alignspec
     * alignspec -> <see above alignTerminals array>
     */
    private void alignStmt(ColumnFormat cf)
                 throws ParserException, IOException {
        match(ALIGN);
        Token t = matchOne(alignKeyWords);
        cf.setAlignment(Alignment.toAlignment(t.sval));
        String alignString = t.sval;
        log(pdebug, "Parsed: align -> " + alignString);
    }

    /**
     * headerstmt -> 'header' quotedstring
     */
    private void headerStmt(ColumnFormat cf)
                 throws ParserException, IOException {
        match(HEADER);
        String headerString = lookahead.sval;
        matchQuotedString();
        cf.setHeader(headerString);
        log(pdebug, "Parsed: header -> " + headerString);
    }

    /**
     * widthstmt -> 'width' integer
     */
    private void widthStmt(ColumnFormat cf)
                 throws ParserException, IOException {
        match(WIDTH);
        double width = lookahead.nval;
        matchNumber();
        cf.setWidth((int)width);
        log(pdebug, "Parsed: width -> " + width );
    }

    /**
     * formatstmt -> 'format' quotedstring
     */
    private void formatStmt(ColumnFormat cf)
                 throws ParserException, IOException {
        match(FORMAT);
        String formatString = lookahead.sval;
        matchQuotedString();
        cf.setFormat(formatString);
        log(pdebug, "Parsed: format -> " + formatString);
    }

    /**
     *  Primary -> Literal | Identifier | '(' Expression ')'
     */
    private Expression primary() throws ParserException, IOException {
        Expression e = null;

        switch (lookahead.ttype) {
        case OPENPAREN:
            match(OPENPAREN);
            e = expression();
            match(CLOSEPAREN);
            break;
        case StreamTokenizer.TT_WORD:
            String s = lookahead.sval;
            if (isReservedWord(s)) {
                throw new SyntaxException(st.lineno(), "IDENTIFIER",
                                          "Reserved Word: " + lookahead.sval);
            }
            matchID();
            e = new Identifier(s);
            log(pdebug, "Parsed: ID -> " + s);
            break;
        case StreamTokenizer.TT_NUMBER:
            double literal = lookahead.nval;
            matchNumber();
            e = new Literal(Double.valueOf(literal));
            log(pdebug, "Parsed: number -> " + literal);
            break;
        default:
            throw new SyntaxException(st.lineno(), "IDENTIFIER", lookahead);
        }
        log(pdebug, "Parsed: primary -> " + e);
        return e;
    }

    /**
     * Unary -> ('+'|'-') Unary | Primary
     */
    private Expression unary() throws ParserException, IOException {
        Expression e = null;
        Operator op = null;

        while (true) {
            switch (lookahead.ttype) {
            case OPERATOR_PLUS:
                match(OPERATOR_PLUS);
                op = Operator.PLUS;
                break;
            case OPERATOR_MINUS:
                match(OPERATOR_MINUS);
                op = Operator.MINUS;
                break;
            default:
                e = primary();
                log(pdebug, "Parsed: unary -> " + e);
                return e;
            }
            Expression e1 = new Expression();
            e1.setOperator(op);
            e1.setRight(e);
            log(pdebug, "Parsed: unary -> " + e1);
            e1.setLeft(new Literal(Double.valueOf(0)));
            e = e1;
        }
    }

    /**
     *  MultExpression -> Unary (('*' | '/') Unary)*
     */
    private Expression multExpression() throws ParserException, IOException {
        Expression e = unary();
        Operator op = null;

        while (true) {
            switch (lookahead.ttype) {
            case OPERATOR_MULTIPLY:
                match(OPERATOR_MULTIPLY);
                op = Operator.MULTIPLY;
                break;
            case OPERATOR_DIVIDE:
                match(OPERATOR_DIVIDE);
                op = Operator.DIVIDE;
                break;
            default:
                log(pdebug, "Parsed: multExpression -> " + e);
                return e;
            }
            Expression e1 = new Expression();
            e1.setOperator(op);
            e1.setLeft(e);
            e1.setRight(unary());
            e = e1;
            log(pdebug, "Parsed: multExpression -> " + e);
        }
    }

    /**
     *  AddExpression -> MultExpression (('+' | '-') MultExpression)*
     */
    private Expression addExpression() throws ParserException, IOException {
        Expression e = multExpression();
        Operator op = null;

        while (true) {
            switch (lookahead.ttype) {
            case OPERATOR_PLUS:
                match(OPERATOR_PLUS);
                op = Operator.PLUS;
                break;
            case OPERATOR_MINUS:
                match(OPERATOR_MINUS);
                op = Operator.MINUS;
                break;
            default:
                log(pdebug, "Parsed: addExpression -> " + e);
                return e;
            }
            Expression e1 = new Expression();
            e1.setOperator(op);
            e1.setLeft(e);
            e1.setRight(multExpression());
            e = e1;
            log(pdebug, "Parsed: addExpression -> " + e);
        }
    }

    /**
     *  Expression -> AddExpression
     */
    private Expression expression() throws ParserException, IOException {
        Expression e = addExpression();
        log(pdebug, "Parsed: expression -> " + e);
        return e;
    }

    /**
     * datastmt -> 'data' expression
     */
    private void dataStmt(ColumnFormat cf) throws ParserException, IOException {
        match(DATA);
        Expression e = expression();
        cf.setExpression(e);
        log(pdebug, "Parsed: data -> " + e);
    }

    /**
     * requiredstmt -> 'required' expression
     */
    private void requiredStmt(ColumnFormat cf) throws ParserException, IOException {
        match(REQUIRED);
        Token t = matchOne(boolKeyWords);
        cf.setRequired(Boolean.parseBoolean(t.sval));
        log(pdebug, "Parsed: required -> " + cf.isRequired());
    }

    /**
     * statementlist -> optionalstmt statementlist
     * optionalstmt -> 'data' expression
     *                 'header' quotedstring
     *                 'width' integer
     *                 'format' formatstring
     *                 'align' alignspec
     *                 'scale' scalespec
     *                 'required' boolean
     */
    private void statementList(ColumnFormat cf)
                 throws ParserException, IOException {
        while (true) {
            if (lookahead.ttype != StreamTokenizer.TT_WORD) {
                return;
            }

            if (lookahead.sval.compareTo(DATA) == 0) {
                dataStmt(cf);
            } else if (lookahead.sval.compareTo(HEADER) == 0) {
                headerStmt(cf);
            } else if (lookahead.sval.compareTo(WIDTH) == 0) {
                widthStmt(cf);
            } else if (lookahead.sval.compareTo(FORMAT) == 0) {
                formatStmt(cf);
            } else if (lookahead.sval.compareTo(ALIGN) == 0) {
                alignStmt(cf);
            } else if (lookahead.sval.compareTo(SCALE) == 0) {
                scaleStmt(cf);
            } else if (lookahead.sval.compareTo(REQUIRED) == 0) {
                requiredStmt(cf);
            } else {
                return;
            }
        }
    }

    /**
     * optionlist -> columspec optionlist
     *               null
     * columspec -> 'column' '{' statementlist '}'
     */
    private void optionList(OptionFormat of)
                 throws ParserException, IOException {
        while (true) {
            if (lookahead.ttype != StreamTokenizer.TT_WORD) {
                return;
            }

            match(COLUMN);
            match(OPENBLOCK);
            ColumnFormat cf = new ColumnFormat(columnCount++);
            statementList(cf);
              match(CLOSEBLOCK);
            cf.validate();
            of.addSubFormat(cf);
        }
    }

    /**
     * optionstmt -> 'option' ID '{' optionlist '}'
     */
    private OptionFormat optionStmt() throws ParserException, IOException {
        match(OPTION);
        String optionName=lookahead.sval;
        matchID();
        match(OPENBLOCK);
        OptionFormat of = new OptionFormat(optionName);
        optionList(of);
        match(CLOSEBLOCK);
        return of;
    }

    /**
     * parse the specification for the given option identifier
     */
    public OptionFormat parse(String option)
                        throws ParserException, IOException {
        nextToken();

        /*
         * this search stops on the first occurance of an option
         * statement with a name matching the given option. Any
         * duplicate options are ignored.
         */
        while (lookahead.ttype != StreamTokenizer.TT_EOF) {
            // look for the start symbol
            if ((lookahead.ttype != StreamTokenizer.TT_WORD)
                    || (lookahead.sval.compareTo(START) != 0)) {
                // skip tokens until a start symbol is found
                nextToken();
                continue;
            }

            // check if the option name is the one we are interested in
            match(START);

            if ((lookahead.ttype == StreamTokenizer.TT_WORD)
                    && (lookahead.sval.compareTo(option) == 0)) {
                // this is the one we are looking for, parse it
                pushBack();
                return optionStmt();
            } else {
                // not what we are looking for, start skipping tokens
                nextToken();
            }
        }
        return null;
    }

    public Set<OptionFormat> parseOptions() throws ParserException, IOException {
        Set<OptionFormat> options = new HashSet<OptionFormat>();

        nextToken();

        while (lookahead.ttype != StreamTokenizer.TT_EOF) {
            // look for the start symbol
            if ((lookahead.ttype != StreamTokenizer.TT_WORD)
                    || (lookahead.sval.compareTo(START) != 0)) {
                // skip tokens until a start symbol is found
                nextToken();
                continue;
            }

            // note: if a duplicate option statement exists, then
            // first one encountered is the chosen definition.
            OptionFormat of = optionStmt();
            options.add(of);
        }
        return options;
    }

    OptionFormat getOptionFormat() {
       return optionFormat;
    }

    private void log(boolean logging, String s) {
        if (logging) {
            System.out.println(s);
        }
    }
}
