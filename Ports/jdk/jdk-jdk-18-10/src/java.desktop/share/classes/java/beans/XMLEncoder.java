/*
 * Copyright (c) 2000, 2015, Oracle and/or its affiliates. All rights reserved.
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
package java.beans;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * The {@code XMLEncoder} class is a complementary alternative to
 * the {@code ObjectOutputStream} and can used to generate
 * a textual representation of a <em>JavaBean</em> in the same
 * way that the {@code ObjectOutputStream} can
 * be used to create binary representation of {@code Serializable}
 * objects. For example, the following fragment can be used to create
 * a textual representation the supplied <em>JavaBean</em>
 * and all its properties:
 * <pre>
 *       XMLEncoder e = new XMLEncoder(
 *                          new BufferedOutputStream(
 *                              new FileOutputStream("Test.xml")));
 *       e.writeObject(new JButton("Hello, world"));
 *       e.close();
 * </pre>
 * Despite the similarity of their APIs, the {@code XMLEncoder}
 * class is exclusively designed for the purpose of archiving graphs
 * of <em>JavaBean</em>s as textual representations of their public
 * properties. Like Java source files, documents written this way
 * have a natural immunity to changes in the implementations of the classes
 * involved. The {@code ObjectOutputStream} continues to be recommended
 * for interprocess communication and general purpose serialization.
 * <p>
 * The {@code XMLEncoder} class provides a default denotation for
 * <em>JavaBean</em>s in which they are represented as XML documents
 * complying with version 1.0 of the XML specification and the
 * UTF-8 character encoding of the Unicode/ISO 10646 character set.
 * The XML documents produced by the {@code XMLEncoder} class are:
 * <ul>
 * <li>
 * <em>Portable and version resilient</em>: they have no dependencies
 * on the private implementation of any class and so, like Java source
 * files, they may be exchanged between environments which may have
 * different versions of some of the classes and between VMs from
 * different vendors.
 * <li>
 * <em>Structurally compact</em>: The {@code XMLEncoder} class
 * uses a <em>redundancy elimination</em> algorithm internally so that the
 * default values of a Bean's properties are not written to the stream.
 * <li>
 * <em>Fault tolerant</em>: Non-structural errors in the file,
 * caused either by damage to the file or by API changes
 * made to classes in an archive remain localized
 * so that a reader can report the error and continue to load the parts
 * of the document which were not affected by the error.
 * </ul>
 * <p>
 * Below is an example of an XML archive containing
 * some user interface components from the <em>swing</em> toolkit:
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;java version="1.0" class="java.beans.XMLDecoder"&gt;
 * &lt;object class="javax.swing.JFrame"&gt;
 *   &lt;void property="name"&gt;
 *     &lt;string&gt;frame1&lt;/string&gt;
 *   &lt;/void&gt;
 *   &lt;void property="bounds"&gt;
 *     &lt;object class="java.awt.Rectangle"&gt;
 *       &lt;int&gt;0&lt;/int&gt;
 *       &lt;int&gt;0&lt;/int&gt;
 *       &lt;int&gt;200&lt;/int&gt;
 *       &lt;int&gt;200&lt;/int&gt;
 *     &lt;/object&gt;
 *   &lt;/void&gt;
 *   &lt;void property="contentPane"&gt;
 *     &lt;void method="add"&gt;
 *       &lt;object class="javax.swing.JButton"&gt;
 *         &lt;void property="label"&gt;
 *           &lt;string&gt;Hello&lt;/string&gt;
 *         &lt;/void&gt;
 *       &lt;/object&gt;
 *     &lt;/void&gt;
 *   &lt;/void&gt;
 *   &lt;void property="visible"&gt;
 *     &lt;boolean&gt;true&lt;/boolean&gt;
 *   &lt;/void&gt;
 * &lt;/object&gt;
 * &lt;/java&gt;
 * </pre>
 * The XML syntax uses the following conventions:
 * <ul>
 * <li>
 * Each element represents a method call.
 * <li>
 * The "object" tag denotes an <em>expression</em> whose value is
 * to be used as the argument to the enclosing element.
 * <li>
 * The "void" tag denotes a <em>statement</em> which will
 * be executed, but whose result will not be used as an
 * argument to the enclosing method.
 * <li>
 * Elements which contain elements use those elements as arguments,
 * unless they have the tag: "void".
 * <li>
 * The name of the method is denoted by the "method" attribute.
 * <li>
 * XML's standard "id" and "idref" attributes are used to make
 * references to previous expressions - so as to deal with
 * circularities in the object graph.
 * <li>
 * The "class" attribute is used to specify the target of a static
 * method or constructor explicitly; its value being the fully
 * qualified name of the class.
 * <li>
 * Elements with the "void" tag are executed using
 * the outer context as the target if no target is defined
 * by a "class" attribute.
 * <li>
 * Java's String class is treated specially and is
 * written &lt;string&gt;Hello, world&lt;/string&gt; where
 * the characters of the string are converted to bytes
 * using the UTF-8 character encoding.
 * </ul>
 * <p>
 * Although all object graphs may be written using just these three
 * tags, the following definitions are included so that common
 * data structures can be expressed more concisely:
 * <ul>
 * <li>
 * The default method name is "new".
 * <li>
 * A reference to a java class is written in the form
 *  &lt;class&gt;javax.swing.JButton&lt;/class&gt;.
 * <li>
 * Instances of the wrapper classes for Java's primitive types are written
 * using the name of the primitive type as the tag. For example, an
 * instance of the {@code Integer} class could be written:
 * &lt;int&gt;123&lt;/int&gt;. Note that the {@code XMLEncoder} class
 * uses Java's reflection package in which the conversion between
 * Java's primitive types and their associated "wrapper classes"
 * is handled internally. The API for the {@code XMLEncoder} class
 * itself deals only with {@code Object}s.
 * <li>
 * In an element representing a nullary method whose name
 * starts with "get", the "method" attribute is replaced
 * with a "property" attribute whose value is given by removing
 * the "get" prefix and decapitalizing the result.
 * <li>
 * In an element representing a monadic method whose name
 * starts with "set", the "method" attribute is replaced
 * with a "property" attribute whose value is given by removing
 * the "set" prefix and decapitalizing the result.
 * <li>
 * In an element representing a method named "get" taking one
 * integer argument, the "method" attribute is replaced
 * with an "index" attribute whose value the value of the
 * first argument.
 * <li>
 * In an element representing a method named "set" taking two arguments,
 * the first of which is an integer, the "method" attribute is replaced
 * with an "index" attribute whose value the value of the
 * first argument.
 * <li>
 * A reference to an array is written using the "array"
 * tag. The "class" and "length" attributes specify the
 * sub-type of the array and its length respectively.
 * </ul>
 *
 *<p>
 * For more information you might also want to check out
 * <a href="http://www.oracle.com/technetwork/java/persistence4-140124.html">
 * Using XMLEncoder</a>,
 * an article in <em>The Swing Connection.</em>
 * @see XMLDecoder
 * @see java.io.ObjectOutputStream
 *
 * @since 1.4
 *
 * @author Philip Milne
 */
public class XMLEncoder extends Encoder implements AutoCloseable {

    private final CharsetEncoder encoder;
    private final String charset;
    private final boolean declaration;

    private OutputStreamWriter out;
    private Object owner;
    private int indentation = 0;
    private boolean internal = false;
    private Map<Object, ValueData> valueToExpression;
    private Map<Object, List<Statement>> targetToStatementList;
    private boolean preambleWritten = false;
    private NameGenerator nameGenerator;

    private class ValueData {
        public int refs = 0;
        public boolean marked = false; // Marked -> refs > 0 unless ref was a target.
        public String name = null;
        public Expression exp = null;
    }

    /**
     * Creates a new XML encoder to write out <em>JavaBeans</em>
     * to the stream {@code out} using an XML encoding.
     *
     * @param out  the stream to which the XML representation of
     *             the objects will be written
     *
     * @throws  IllegalArgumentException
     *          if {@code out} is {@code null}
     *
     * @see XMLDecoder#XMLDecoder(InputStream)
     */
    public XMLEncoder(OutputStream out) {
        this(out, "UTF-8", true, 0);
    }

    /**
     * Creates a new XML encoder to write out <em>JavaBeans</em>
     * to the stream {@code out} using the given {@code charset}
     * starting from the given {@code indentation}.
     *
     * @param out          the stream to which the XML representation of
     *                     the objects will be written
     * @param charset      the name of the requested charset;
     *                     may be either a canonical name or an alias
     * @param declaration  whether the XML declaration should be generated;
     *                     set this to {@code false}
     *                     when embedding the contents in another XML document
     * @param indentation  the number of space characters to indent the entire XML document by
     *
     * @throws  IllegalArgumentException
     *          if {@code out} or {@code charset} is {@code null},
     *          or if {@code indentation} is less than 0
     *
     * @throws  IllegalCharsetNameException
     *          if {@code charset} name is illegal
     *
     * @throws  UnsupportedCharsetException
     *          if no support for the named charset is available
     *          in this instance of the Java virtual machine
     *
     * @throws  UnsupportedOperationException
     *          if loaded charset does not support encoding
     *
     * @see Charset#forName(String)
     *
     * @since 1.7
     */
    public XMLEncoder(OutputStream out, String charset, boolean declaration, int indentation) {
        if (out == null) {
            throw new IllegalArgumentException("the output stream cannot be null");
        }
        if (indentation < 0) {
            throw new IllegalArgumentException("the indentation must be >= 0");
        }
        Charset cs = Charset.forName(charset);
        this.encoder = cs.newEncoder();
        this.charset = charset;
        this.declaration = declaration;
        this.indentation = indentation;
        this.out = new OutputStreamWriter(out, cs.newEncoder());
        valueToExpression = new IdentityHashMap<>();
        targetToStatementList = new IdentityHashMap<>();
        nameGenerator = new NameGenerator();
    }

    /**
     * Sets the owner of this encoder to {@code owner}.
     *
     * @param owner The owner of this encoder.
     *
     * @see #getOwner
     */
    public void setOwner(Object owner) {
        this.owner = owner;
        writeExpression(new Expression(this, "getOwner", new Object[0]));
    }

    /**
     * Gets the owner of this encoder.
     *
     * @return The owner of this encoder.
     *
     * @see #setOwner
     */
    public Object getOwner() {
        return owner;
    }

    /**
     * Write an XML representation of the specified object to the output.
     *
     * @param o The object to be written to the stream.
     *
     * @see XMLDecoder#readObject
     */
    public void writeObject(Object o) {
        if (internal) {
            super.writeObject(o);
        }
        else {
            writeStatement(new Statement(this, "writeObject", new Object[]{o}));
        }
    }

    private List<Statement> statementList(Object target) {
        List<Statement> list = targetToStatementList.get(target);
        if (list == null) {
            list = new ArrayList<>();
            targetToStatementList.put(target, list);
        }
        return list;
    }


    private void mark(Object o, boolean isArgument) {
        if (o == null || o == this) {
            return;
        }
        ValueData d = getValueData(o);
        Expression exp = d.exp;
        // Do not mark liternal strings. Other strings, which might,
        // for example, come from resource bundles should still be marked.
        if (o.getClass() == String.class && exp == null) {
            return;
        }

        // Bump the reference counts of all arguments
        if (isArgument) {
            d.refs++;
        }
        if (d.marked) {
            return;
        }
        d.marked = true;
        Object target = exp.getTarget();
        mark(exp);
        if (!(target instanceof Class)) {
            statementList(target).add(exp);
            // Pending: Why does the reference count need to
            // be incremented here?
            d.refs++;
        }
    }

    private void mark(Statement stm) {
        Object[] args = stm.getArguments();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            mark(arg, true);
        }
        mark(stm.getTarget(), stm instanceof Expression);
    }


    /**
     * Records the Statement so that the Encoder will
     * produce the actual output when the stream is flushed.
     * <P>
     * This method should only be invoked within the context
     * of initializing a persistence delegate.
     *
     * @param oldStm The statement that will be written
     *               to the stream.
     * @see java.beans.PersistenceDelegate#initialize
     */
    public void writeStatement(Statement oldStm) {
        // System.out.println("XMLEncoder::writeStatement: " + oldStm);
        boolean internal = this.internal;
        this.internal = true;
        try {
            super.writeStatement(oldStm);
            /*
               Note we must do the mark first as we may
               require the results of previous values in
               this context for this statement.
               Test case is:
                   os.setOwner(this);
                   os.writeObject(this);
            */
            mark(oldStm);
            Object target = oldStm.getTarget();
            if (target instanceof Field) {
                String method = oldStm.getMethodName();
                Object[] args = oldStm.getArguments();
                if ((method == null) || (args == null)) {
                }
                else if (method.equals("get") && (args.length == 1)) {
                    target = args[0];
                }
                else if (method.equals("set") && (args.length == 2)) {
                    target = args[0];
                }
            }
            statementList(target).add(oldStm);
        }
        catch (Exception e) {
            getExceptionListener().exceptionThrown(new Exception("XMLEncoder: discarding statement " + oldStm, e));
        }
        this.internal = internal;
    }


    /**
     * Records the Expression so that the Encoder will
     * produce the actual output when the stream is flushed.
     * <P>
     * This method should only be invoked within the context of
     * initializing a persistence delegate or setting up an encoder to
     * read from a resource bundle.
     * <P>
     * For more information about using resource bundles with the
     * XMLEncoder, see
     * <a href="http://www.oracle.com/technetwork/java/persistence4-140124.html#i18n">
     * Creating Internationalized Applications</a>,
     *
     * @param oldExp The expression that will be written
     *               to the stream.
     * @see java.beans.PersistenceDelegate#initialize
     */
    public void writeExpression(Expression oldExp) {
        boolean internal = this.internal;
        this.internal = true;
        Object oldValue = getValue(oldExp);
        if (get(oldValue) == null || (oldValue instanceof String && !internal)) {
            getValueData(oldValue).exp = oldExp;
            super.writeExpression(oldExp);
        }
        this.internal = internal;
    }

    /**
     * This method writes out the preamble associated with the
     * XML encoding if it has not been written already and
     * then writes out all of the values that been
     * written to the stream since the last time {@code flush}
     * was called. After flushing, all internal references to the
     * values that were written to this stream are cleared.
     */
    public void flush() {
        if (!preambleWritten) { // Don't do this in constructor - it throws ... pending.
            if (this.declaration) {
                writeln("<?xml version=" + quote("1.0") +
                            " encoding=" + quote(this.charset) + "?>");
            }
            writeln("<java version=" + quote(System.getProperty("java.version")) +
                           " class=" + quote(XMLDecoder.class.getName()) + ">");
            preambleWritten = true;
        }
        indentation++;
        List<Statement> statements = statementList(this);
        while (!statements.isEmpty()) {
            Statement s = statements.remove(0);
            if ("writeObject".equals(s.getMethodName())) {
                outputValue(s.getArguments()[0], this, true);
            }
            else {
                outputStatement(s, this, false);
            }
        }
        indentation--;

        Statement statement = getMissedStatement();
        while (statement != null) {
            outputStatement(statement, this, false);
            statement = getMissedStatement();
        }

        try {
            out.flush();
        }
        catch (IOException e) {
            getExceptionListener().exceptionThrown(e);
        }
        clear();
    }

    void clear() {
        super.clear();
        nameGenerator.clear();
        valueToExpression.clear();
        targetToStatementList.clear();
    }

    Statement getMissedStatement() {
        for (List<Statement> statements : this.targetToStatementList.values()) {
            for (int i = 0; i < statements.size(); i++) {
                if (Statement.class == statements.get(i).getClass()) {
                    return statements.remove(i);
                }
            }
        }
        return null;
    }


    /**
     * This method calls {@code flush}, writes the closing
     * postamble and then closes the output stream associated
     * with this stream.
     */
    public void close() {
        flush();
        writeln("</java>");
        try {
            out.close();
        }
        catch (IOException e) {
            getExceptionListener().exceptionThrown(e);
        }
    }

    private String quote(String s) {
        return "\"" + s + "\"";
    }

    private ValueData getValueData(Object o) {
        ValueData d = valueToExpression.get(o);
        if (d == null) {
            d = new ValueData();
            valueToExpression.put(o, d);
        }
        return d;
    }

    /**
     * Returns {@code true} if the argument,
     * a Unicode code point, is valid in XML documents.
     * Unicode characters fit into the low sixteen bits of a Unicode code point,
     * and pairs of Unicode <em>surrogate characters</em> can be combined
     * to encode Unicode code point in documents containing only Unicode.
     * (The {@code char} datatype in the Java Programming Language
     * represents Unicode characters, including unpaired surrogates.)
     * <par>
     * [2] Char ::= #x0009 | #x000A | #x000D
     *            | [#x0020-#xD7FF]
     *            | [#xE000-#xFFFD]
     *            | [#x10000-#x10ffff]
     * </par>
     *
     * @param code  the 32-bit Unicode code point being tested
     * @return  {@code true} if the Unicode code point is valid,
     *          {@code false} otherwise
     */
    private static boolean isValidCharCode(int code) {
        return (0x0020 <= code && code <= 0xD7FF)
            || (0x000A == code)
            || (0x0009 == code)
            || (0x000D == code)
            || (0xE000 <= code && code <= 0xFFFD)
            || (0x10000 <= code && code <= 0x10ffff);
    }

    private void writeln(String exp) {
        try {
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < indentation; i++) {
                sb.append(' ');
            }
            sb.append(exp);
            sb.append('\n');
            this.out.write(sb.toString());
        }
        catch (IOException e) {
            getExceptionListener().exceptionThrown(e);
        }
    }

    private void outputValue(Object value, Object outer, boolean isArgument) {
        if (value == null) {
            writeln("<null/>");
            return;
        }

        if (value instanceof Class) {
            writeln("<class>" + ((Class)value).getName() + "</class>");
            return;
        }

        ValueData d = getValueData(value);
        if (d.exp != null) {
            Object target = d.exp.getTarget();
            String methodName = d.exp.getMethodName();

            if (target == null || methodName == null) {
                throw new NullPointerException((target == null ? "target" :
                                                "methodName") + " should not be null");
            }

            if (isArgument && target instanceof Field && methodName.equals("get")) {
                Field f = (Field) target;
                if (Modifier.isStatic(f.getModifiers())) {
                    writeln("<object class=" + quote(f.getDeclaringClass().getName()) +
                            " field=" + quote(f.getName()) + "/>");
                    return;
                }
            }

            Class<?> primitiveType = primitiveTypeFor(value.getClass());
            if (primitiveType != null && target == value.getClass() &&
                methodName.equals("new")) {
                String primitiveTypeName = primitiveType.getName();
                // Make sure that character types are quoted correctly.
                if (primitiveType == Character.TYPE) {
                    char code = ((Character) value).charValue();
                    if (!isValidCharCode(code)) {
                        writeln(createString(code));
                        return;
                    }
                    value = quoteCharCode(code);
                    if (value == null) {
                        value = Character.valueOf(code);
                    }
                }
                writeln("<" + primitiveTypeName + ">" + value + "</" +
                        primitiveTypeName + ">");
                return;
            }

        } else if (value instanceof String) {
            writeln(createString((String) value));
            return;
        }

        if (d.name != null) {
            if (isArgument) {
                writeln("<object idref=" + quote(d.name) + "/>");
            }
            else {
                outputXML("void", " idref=" + quote(d.name), value);
            }
        }
        else if (d.exp != null) {
            outputStatement(d.exp, outer, isArgument);
        }
    }

    private static String quoteCharCode(int code) {
        switch(code) {
          case '&':  return "&amp;";
          case '<':  return "&lt;";
          case '>':  return "&gt;";
          case '"':  return "&quot;";
          case '\'': return "&apos;";
          case '\r': return "&#13;";
          default:   return null;
        }
    }

    private static String createString(int code) {
        return "<char code=\"#" + Integer.toString(code, 16) + "\"/>";
    }

    private String createString(String string) {
        StringBuilder sb = new StringBuilder();
        sb.append("<string>");
        int index = 0;
        while (index < string.length()) {
            int point = string.codePointAt(index);
            int count = Character.charCount(point);

            if (isValidCharCode(point) && this.encoder.canEncode(string.substring(index, index + count))) {
                String value = quoteCharCode(point);
                if (value != null) {
                    sb.append(value);
                } else {
                    sb.appendCodePoint(point);
                }
                index += count;
            } else {
                sb.append(createString(string.charAt(index)));
                index++;
            }
        }
        sb.append("</string>");
        return sb.toString();
    }

    private void outputStatement(Statement exp, Object outer, boolean isArgument) {
        Object target = exp.getTarget();
        String methodName = exp.getMethodName();

        if (target == null || methodName == null) {
            throw new NullPointerException((target == null ? "target" :
                                            "methodName") + " should not be null");
        }

        Object[] args = exp.getArguments();
        boolean expression = exp.getClass() == Expression.class;
        Object value = (expression) ? getValue((Expression)exp) : null;

        String tag = (expression && isArgument) ? "object" : "void";
        String attributes = "";
        ValueData d = getValueData(value);

        // Special cases for targets.
        if (target == outer) {
        }
        else if (target == Array.class && methodName.equals("newInstance")) {
            tag = "array";
            attributes = attributes + " class=" + quote(((Class)args[0]).getName());
            attributes = attributes + " length=" + quote(args[1].toString());
            args = new Object[]{};
        }
        else if (target.getClass() == Class.class) {
            attributes = attributes + " class=" + quote(((Class)target).getName());
        }
        else {
            d.refs = 2;
            if (d.name == null) {
                getValueData(target).refs++;
                List<Statement> statements = statementList(target);
                if (!statements.contains(exp)) {
                    statements.add(exp);
                }
                outputValue(target, outer, false);
            }
            if (expression) {
                outputValue(value, outer, isArgument);
            }
            return;
        }
        if (expression && (d.refs > 1)) {
            String instanceName = nameGenerator.instanceName(value);
            d.name = instanceName;
            attributes = attributes + " id=" + quote(instanceName);
        }

        // Special cases for methods.
        if ((!expression && methodName.equals("set") && args.length == 2 &&
             args[0] instanceof Integer) ||
             (expression && methodName.equals("get") && args.length == 1 &&
              args[0] instanceof Integer)) {
            attributes = attributes + " index=" + quote(args[0].toString());
            args = (args.length == 1) ? new Object[]{} : new Object[]{args[1]};
        }
        else if ((!expression && methodName.startsWith("set") && args.length == 1) ||
                 (expression && methodName.startsWith("get") && args.length == 0)) {
            if (3 < methodName.length()) {
                attributes = attributes + " property=" +
                    quote(Introspector.decapitalize(methodName.substring(3)));
            }
        }
        else if (!methodName.equals("new") && !methodName.equals("newInstance")) {
            attributes = attributes + " method=" + quote(methodName);
        }
        outputXML(tag, attributes, value, args);
    }

    private void outputXML(String tag, String attributes, Object value, Object... args) {
        List<Statement> statements = statementList(value);
        // Use XML's short form when there is no body.
        if (args.length == 0 && statements.size() == 0) {
            writeln("<" + tag + attributes + "/>");
            return;
        }

        writeln("<" + tag + attributes + ">");
        indentation++;

        for(int i = 0; i < args.length; i++) {
            outputValue(args[i], null, true);
        }

        while (!statements.isEmpty()) {
            Statement s = statements.remove(0);
            outputStatement(s, value, false);
        }

        indentation--;
        writeln("</" + tag + ">");
    }

    @SuppressWarnings("rawtypes")
    static Class primitiveTypeFor(Class wrapper) {
        if (wrapper == Boolean.class) return Boolean.TYPE;
        if (wrapper == Byte.class) return Byte.TYPE;
        if (wrapper == Character.class) return Character.TYPE;
        if (wrapper == Short.class) return Short.TYPE;
        if (wrapper == Integer.class) return Integer.TYPE;
        if (wrapper == Long.class) return Long.TYPE;
        if (wrapper == Float.class) return Float.TYPE;
        if (wrapper == Double.class) return Double.TYPE;
        if (wrapper == Void.class) return Void.TYPE;
        return null;
    }
}
