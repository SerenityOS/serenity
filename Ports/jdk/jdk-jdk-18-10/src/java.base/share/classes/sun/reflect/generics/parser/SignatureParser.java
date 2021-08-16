/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sun.reflect.generics.parser;

import java.lang.reflect.GenericSignatureFormatError;
import java.util.*;
import sun.reflect.generics.tree.*;

/**
 * Parser for type signatures, as defined in the Java Virtual
 * Machine Specification (JVMS) chapter 4.
 * Converts the signatures into an abstract syntax tree (AST) representation.
 * See the package sun.reflect.generics.tree for details of the AST.
 */
public class SignatureParser {
    // The input is conceptually a character stream (though currently it's
    // a string). This is slightly different than traditional parsers,
    // because there is no lexical scanner performing tokenization.
    // Having a separate tokenizer does not fit with the nature of the
    // input format.
    // Other than the absence of a tokenizer, this parser is a classic
    // recursive descent parser. Its structure corresponds as closely
    // as possible to the grammar in the JVMS.
    //
    // A note on asserts vs. errors: The code contains assertions
    // in situations that should never occur. An assertion failure
    // indicates a failure of the parser logic. A common pattern
    // is an assertion that the current input is a particular
    // character. This is often paired with a separate check
    // that this is the case, which seems redundant. For example:
    //
    // assert(current() != x);
    // if (current != x {error("expected an x");
    //
    // where x is some character constant.
    // The assertion indicates, that, as currently written,
    // the code should never reach this point unless the input is an
    // x. On the other hand, the test is there to check the legality
    // of the input wrt to a given production. It may be that at a later
    // time the code might be called directly, and if the input is
    // invalid, the parser should flag an error in accordance
    // with its logic.

    private String input; // the input signature
    private int index;    // index into the input
    private int mark;     // index of mark
    // used to mark end of input
    private static final char EOI = ':';
    private static final boolean DEBUG = false;

    // private constructor - enforces use of static factory
    private SignatureParser(){}

    // prepares parser for new parsing session
    private void init(String s) {
        input = s;
        mark = index = 0;
    }

    // Utility methods.

    // Most parsing routines use the following routines to access the
    // input stream, and advance it as necessary.
    // This makes it easy to adapt the parser to operate on streams
    // of various kinds as well as strings.

    // returns current element of the input
    private char current(){
        assert(index <= input.length());
        return index < input.length() ? input.charAt(index) : EOI;
    }

    // advance the input
    private void advance(){
        assert(index <= input.length());
        if (index < input.length()) index++;
    }

    // mark current position
    private void mark() {
        mark = index;
    }

    // For debugging, prints current character to the end of the input.
    private String remainder() {
        return input.substring(index);
    }

    // returns a substring of input from mark (inclusive)
    // to current position (exclusive)
    private String markToCurrent() {
        return input.substring(mark, index);
    }

    // Error handling routine. Encapsulates error handling.
    // Takes a string error message as argument.
    // Currently throws a GenericSignatureFormatError.

    private Error error(String errorMsg) {
        return new GenericSignatureFormatError("Signature Parse error: " + errorMsg +
                                               "\n\tRemaining input: " + remainder());
    }

    /**
     * Verify the parse has made forward progress; throw an exception
     * if no progress.
     */
    private void progress(int startingPosition) {
        if (index <= startingPosition)
            throw error("Failure to make progress!");
    }

    /**
     * Static factory method. Produces a parser instance.
     * @return an instance of {@code SignatureParser}
     */
    public static SignatureParser make() {
        return new SignatureParser();
    }

    /**
     * Parses a class signature (as defined in the JVMS, chapter 4)
     * and produces an abstract syntax tree representing it.
     * @param s a string representing the input class signature
     * @return An abstract syntax tree for a class signature
     * corresponding to the input string
     * @throws GenericSignatureFormatError if the input is not a valid
     * class signature
     */
    public ClassSignature parseClassSig(String s) {
        if (DEBUG) System.out.println("Parsing class sig:" + s);
        init(s);
        return parseClassSignature();
    }

    /**
     * Parses a method signature (as defined in the JVMS, chapter 4)
     * and produces an abstract syntax tree representing it.
     * @param s a string representing the input method signature
     * @return An abstract syntax tree for a method signature
     * corresponding to the input string
     * @throws GenericSignatureFormatError if the input is not a valid
     * method signature
     */
    public MethodTypeSignature parseMethodSig(String s) {
        if (DEBUG) System.out.println("Parsing method sig:" + s);
        init(s);
        return parseMethodTypeSignature();
    }


    /**
     * Parses a type signature
     * and produces an abstract syntax tree representing it.
     *
     * @param s a string representing the input type signature
     * @return An abstract syntax tree for a type signature
     * corresponding to the input string
     * @throws GenericSignatureFormatError if the input is not a valid
     * type signature
     */
    public TypeSignature parseTypeSig(String s) {
        if (DEBUG) System.out.println("Parsing type sig:" + s);
        init(s);
        return parseTypeSignature();
    }

    // Parsing routines.
    // As a rule, the parsing routines access the input using the
    // utilities current() and advance().
    // The convention is that when a parsing routine is invoked
    // it expects the current input to be the first character it should parse
    // and when it completes parsing, it leaves the input at the first
    // character after the input parses.

    /*
     * Note on grammar conventions: a trailing "*" matches zero or
     * more occurrences, a trailing "+" matches one or more occurrences,
     * "_opt" indicates an optional component.
     */

    /**
     * ClassSignature:
     *     FormalTypeParameters_opt SuperclassSignature SuperinterfaceSignature*
     */
    private ClassSignature parseClassSignature() {
        // parse a class signature based on the implicit input.
        assert(index == 0);
        return ClassSignature.make(parseZeroOrMoreFormalTypeParameters(),
                                   parseClassTypeSignature(), // Only rule for SuperclassSignature
                                   parseSuperInterfaces());
    }

    private FormalTypeParameter[] parseZeroOrMoreFormalTypeParameters(){
        if (current() == '<') {
            return parseFormalTypeParameters();
        } else {
            return new FormalTypeParameter[0];
        }
    }

    /**
     * FormalTypeParameters:
     *     "<" FormalTypeParameter+ ">"
     */
    private FormalTypeParameter[] parseFormalTypeParameters(){
        List<FormalTypeParameter> ftps = new ArrayList<>(3);
        assert(current() == '<'); // should not have been called at all
        if (current() != '<') { throw error("expected '<'");}
        advance();
        ftps.add(parseFormalTypeParameter());
        while (current() != '>') {
            int startingPosition = index;
            ftps.add(parseFormalTypeParameter());
            progress(startingPosition);
        }
        advance();
        return ftps.toArray(new FormalTypeParameter[ftps.size()]);
    }

    /**
     * FormalTypeParameter:
     *     Identifier ClassBound InterfaceBound*
     */
    private FormalTypeParameter parseFormalTypeParameter(){
        String id = parseIdentifier();
        FieldTypeSignature[] bs = parseBounds();
        return FormalTypeParameter.make(id, bs);
    }

    private String parseIdentifier() {
        mark();
        skipIdentifier();
        return markToCurrent();
    }

    private void skipIdentifier() {
        char c = current();
        while (c != ';' && c != '.' && c != '/' &&
               c != '[' && c != ':' && c != '>' &&
               c != '<' && !Character.isWhitespace(c)) {
            advance();
            c = current();
        }
    }

    /**
     * FieldTypeSignature:
     *     ClassTypeSignature
     *     ArrayTypeSignature
     *     TypeVariableSignature
     */
    private FieldTypeSignature parseFieldTypeSignature() {
        return parseFieldTypeSignature(true);
    }

    private FieldTypeSignature parseFieldTypeSignature(boolean allowArrays) {
        switch(current()) {
        case 'L':
           return parseClassTypeSignature();
        case 'T':
            return parseTypeVariableSignature();
        case '[':
            if (allowArrays)
                return parseArrayTypeSignature();
            else
                throw error("Array signature not allowed here.");
        default: throw error("Expected Field Type Signature");
        }
    }

    /**
     * ClassTypeSignature:
     *     "L" PackageSpecifier_opt SimpleClassTypeSignature ClassTypeSignatureSuffix* ";"
     */
    private ClassTypeSignature parseClassTypeSignature(){
        assert(current() == 'L');
        if (current() != 'L') { throw error("expected a class type");}
        advance();
        List<SimpleClassTypeSignature> scts = new ArrayList<>(5);
        scts.add(parsePackageNameAndSimpleClassTypeSignature());

        parseClassTypeSignatureSuffix(scts);
        if (current() != ';')
            throw error("expected ';' got '" + current() + "'");

        advance();
        return ClassTypeSignature.make(scts);
    }

    /**
     * PackageSpecifier:
     *     Identifier "/" PackageSpecifier*
     */
    private SimpleClassTypeSignature parsePackageNameAndSimpleClassTypeSignature() {
        // Parse both any optional leading PackageSpecifier as well as
        // the following SimpleClassTypeSignature.

        mark();
        skipIdentifier();
        while (current() == '/') {
            advance();
            skipIdentifier();
        }
        String id = markToCurrent().replace('/', '.');

        switch (current()) {
        case ';':
            return SimpleClassTypeSignature.make(id, false, new TypeArgument[0]); // all done!
        case '<':
            if (DEBUG) System.out.println("\t remainder: " + remainder());
            return SimpleClassTypeSignature.make(id, false, parseTypeArguments());
        default:
            throw error("expected '<' or ';' but got " + current());
        }
    }

    /**
     * SimpleClassTypeSignature:
     *     Identifier TypeArguments_opt
     */
    private SimpleClassTypeSignature parseSimpleClassTypeSignature(boolean dollar){
        String id = parseIdentifier();
        char c = current();

        switch (c) {
        case ';':
        case '.':
            return SimpleClassTypeSignature.make(id, dollar, new TypeArgument[0]) ;
        case '<':
            return SimpleClassTypeSignature.make(id, dollar, parseTypeArguments());
        default:
            throw error("expected '<' or ';' or '.', got '" + c + "'.");
        }
    }

    /**
     * ClassTypeSignatureSuffix:
     *     "." SimpleClassTypeSignature
     */
    private void parseClassTypeSignatureSuffix(List<SimpleClassTypeSignature> scts) {
        while (current() == '.') {
            advance();
            scts.add(parseSimpleClassTypeSignature(true));
        }
    }

    /**
     * TypeArguments:
     *     "<" TypeArgument+ ">"
     */
    private TypeArgument[] parseTypeArguments() {
        List<TypeArgument> tas = new ArrayList<>(3);
        assert(current() == '<');
        if (current() != '<') { throw error("expected '<'");}
        advance();
        tas.add(parseTypeArgument());
        while (current() != '>') {
                //(matches(current(),  '+', '-', 'L', '[', 'T', '*')) {
            tas.add(parseTypeArgument());
        }
        advance();
        return tas.toArray(new TypeArgument[tas.size()]);
    }

    /**
     * TypeArgument:
     *     WildcardIndicator_opt FieldTypeSignature
     *     "*"
     */
    private TypeArgument parseTypeArgument() {
        FieldTypeSignature[] ub, lb;
        ub = new FieldTypeSignature[1];
        lb = new FieldTypeSignature[1];
        TypeArgument[] ta = new TypeArgument[0];
        char c = current();
        switch (c) {
        case '+': {
            advance();
            ub[0] = parseFieldTypeSignature();
            lb[0] = BottomSignature.make(); // bottom
            return Wildcard.make(ub, lb);
        }
        case '*':{
            advance();
            ub[0] = SimpleClassTypeSignature.make("java.lang.Object", false, ta);
            lb[0] = BottomSignature.make(); // bottom
            return Wildcard.make(ub, lb);
        }
        case '-': {
            advance();
            lb[0] = parseFieldTypeSignature();
            ub[0] = SimpleClassTypeSignature.make("java.lang.Object", false, ta);
            return Wildcard.make(ub, lb);
        }
        default:
            return parseFieldTypeSignature();
        }
    }

    /**
     * TypeVariableSignature:
     *     "T" Identifier ";"
     */
    private TypeVariableSignature parseTypeVariableSignature() {
        assert(current() == 'T');
        if (current() != 'T') { throw error("expected a type variable usage");}
        advance();
        TypeVariableSignature ts = TypeVariableSignature.make(parseIdentifier());
        if (current() != ';') {
            throw error("; expected in signature of type variable named" +
                  ts.getIdentifier());
        }
        advance();
        return ts;
    }

    /**
     * ArrayTypeSignature:
     *     "[" TypeSignature
     */
    private ArrayTypeSignature parseArrayTypeSignature() {
        if (current() != '[') {throw error("expected array type signature");}
        advance();
        return ArrayTypeSignature.make(parseTypeSignature());
    }

    /**
     * TypeSignature:
     *     FieldTypeSignature
     *     BaseType
     */
    private TypeSignature parseTypeSignature() {
        switch (current()) {
        case 'B':
        case 'C':
        case 'D':
        case 'F':
        case 'I':
        case 'J':
        case 'S':
        case 'Z':
            return parseBaseType();

        default:
            return parseFieldTypeSignature();
        }
    }

    private BaseType parseBaseType() {
        switch(current()) {
        case 'B':
            advance();
            return ByteSignature.make();
        case 'C':
            advance();
            return CharSignature.make();
        case 'D':
            advance();
            return DoubleSignature.make();
        case 'F':
            advance();
            return FloatSignature.make();
        case 'I':
            advance();
            return IntSignature.make();
        case 'J':
            advance();
            return LongSignature.make();
        case 'S':
            advance();
            return ShortSignature.make();
        case 'Z':
            advance();
            return BooleanSignature.make();
        default: {
            assert(false);
            throw error("expected primitive type");
        }
        }
    }

    /**
     * ClassBound:
     *     ":" FieldTypeSignature_opt
     *
     * InterfaceBound:
     *     ":" FieldTypeSignature
     */
    private FieldTypeSignature[] parseBounds() {
        List<FieldTypeSignature> fts = new ArrayList<>(3);

        if (current() == ':') {
            advance();
            switch(current()) {
            case ':': // empty class bound
                break;

            default: // parse class bound
                fts.add(parseFieldTypeSignature());
            }

            // zero or more interface bounds
            while (current() == ':') {
                advance();
                fts.add(parseFieldTypeSignature());
            }
        } else
            error("Bound expected");

        return fts.toArray(new FieldTypeSignature[fts.size()]);
    }

    /**
     * SuperclassSignature:
     *     ClassTypeSignature
     */
    private ClassTypeSignature[] parseSuperInterfaces() {
        List<ClassTypeSignature> cts = new ArrayList<>(5);
        while(current() == 'L') {
            cts.add(parseClassTypeSignature());
        }
        return cts.toArray(new ClassTypeSignature[cts.size()]);
    }


    /**
     * MethodTypeSignature:
     *     FormalTypeParameters_opt "(" TypeSignature* ")" ReturnType ThrowsSignature*
     */
    private MethodTypeSignature parseMethodTypeSignature() {
        // Parse a method signature based on the implicit input.
        FieldTypeSignature[] ets;

        assert(index == 0);
        return MethodTypeSignature.make(parseZeroOrMoreFormalTypeParameters(),
                                        parseFormalParameters(),
                                        parseReturnType(),
                                        parseZeroOrMoreThrowsSignatures());
    }

    // "(" TypeSignature* ")"
    private TypeSignature[] parseFormalParameters() {
        if (current() != '(') {throw error("expected '('");}
        advance();
        TypeSignature[] pts = parseZeroOrMoreTypeSignatures();
        if (current() != ')') {throw error("expected ')'");}
        advance();
        return pts;
    }

    // TypeSignature*
    private TypeSignature[] parseZeroOrMoreTypeSignatures() {
        List<TypeSignature> ts = new ArrayList<>();
        boolean stop = false;
        while (!stop) {
            switch(current()) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'Z':
            case 'L':
            case 'T':
            case '[': {
                ts.add(parseTypeSignature());
                break;
            }
            default: stop = true;
            }
        }
        return ts.toArray(new TypeSignature[ts.size()]);
    }

    /**
     * ReturnType:
     *     TypeSignature
     *     VoidDescriptor
     */
    private ReturnType parseReturnType(){
        if (current() == 'V') {
            advance();
            return VoidDescriptor.make();
        } else
            return parseTypeSignature();
    }

    // ThrowSignature*
    private FieldTypeSignature[] parseZeroOrMoreThrowsSignatures(){
        List<FieldTypeSignature> ets = new ArrayList<>(3);
        while( current() == '^') {
            ets.add(parseThrowsSignature());
        }
        return ets.toArray(new FieldTypeSignature[ets.size()]);
    }

    /**
     * ThrowsSignature:
     *     "^" ClassTypeSignature
     *     "^" TypeVariableSignature
     */
    private FieldTypeSignature parseThrowsSignature() {
        assert(current() == '^');
        if (current() != '^') { throw error("expected throws signature");}
        advance();
        return parseFieldTypeSignature(false);
    }
 }
