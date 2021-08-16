/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.xpath;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import org.xml.sax.InputSource;

/**
 * {@code XPath} provides access to the XPath evaluation environment and expressions.
 * The XPath evaluation is affected by the factors described in the following table.
 *
 * <a id="XPath-evaluation"></a>
 * <table class="striped">
 *    <caption>Evaluation of XPath Expressions</caption>
 *    <thead>
 *      <tr>
 *        <th scope="col">Factor</th>
 *        <th scope="col">Behavior</th>
 *      </tr>
 *    </thead>
 *    <tbody>
 *    <tr>
 *      <th scope="row">context</th>
 *      <td>
 *        The type of the context is implementation-dependent. If the value is
 *        null, the operation must have no dependency on the context, otherwise
 *        an XPathExpressionException will be thrown.
 *
 *        For the purposes of evaluating XPath expressions, a DocumentFragment
 *        is treated like a Document node.
 *      </td>
 *    </tr>
 *    <tr>
 *      <th scope="row">variables</th>
 *      <td>
 *        If the expression contains a variable reference, its value will be found through the {@link XPathVariableResolver}
 *        set with {@link #setXPathVariableResolver(XPathVariableResolver resolver)}.
 *        An {@link XPathExpressionException} is raised if the variable resolver is undefined or
 *        the resolver returns {@code null} for the variable.
 *        The value of a variable must be immutable through the course of any single evaluation.
 *      </td>
 *    </tr>
 *    <tr>
 *      <th scope="row">functions</th>
 *      <td>
 *        If the expression contains a function reference, the function will be found through the {@link XPathFunctionResolver}
 *        set with {@link #setXPathFunctionResolver(XPathFunctionResolver resolver)}.
 *        An {@link XPathExpressionException} is raised if the function resolver is undefined or
 *        the function resolver returns {@code null} for the function.
 *      </td>
 *    </tr>
 *    <tr>
 *      <th scope="row">QNames</th>
 *      <td>
 *        QNames in the expression are resolved against the XPath namespace context
 *        set with {@link #setNamespaceContext(NamespaceContext nsContext)}.
 *      </td>
 *    </tr>
 *    <tr>
 *      <th scope="row">result</th>
 *      <td>
 *        This result of evaluating an expression is converted to an instance of the desired return type.
 *        Valid return types are defined in {@link XPathConstants}.
 *        Conversion to the return type follows XPath conversion rules.
 *      </td>
 *    </tr>
 *    </tbody>
 * </table>
 *
 * <p>An XPath object is not thread-safe and not reentrant.
 * In other words, it is the application's responsibility to make
 * sure that one {@link XPath} object is not used from
 * more than one thread at any given time, and while the {@code evaluate}
 * method is invoked, applications may not recursively call
 * the {@code evaluate} method.
 *
 * @author  Norman Walsh
 * @author  Jeff Suttor
 * @see <a href="http://www.w3.org/TR/xpath">XML Path Language (XPath) Version 1.0</a>
 * @since 1.5
 */
public interface XPath {


    /**
     * Reset this {@code XPath} to its original configuration.
     *
     * <p>{@code XPath} is reset to the same state as when it was created with
     * {@link XPathFactory#newXPath()}.
     * {@code reset()} is designed to allow the reuse of existing {@code XPath}s
     * thus saving resources associated with the creation of new {@code XPath}s.
     *
     * <p>The reset {@code XPath} is not guaranteed to have the same
     * {@link XPathFunctionResolver}, {@link XPathVariableResolver}
     * or {@link NamespaceContext} {@code Object}s, e.g. {@link Object#equals(Object obj)}.
     * It is guaranteed to have a functionally equal {@code XPathFunctionResolver},
     * {@code XPathVariableResolver} and {@code NamespaceContext}.
     */
    public void reset();

    /**
     * Establish a variable resolver.
     *
     * <p>A {@code NullPointerException} is thrown if {@code resolver} is {@code null}.
     *
     * @param resolver Variable resolver.
     *
     * @throws NullPointerException If {@code resolver} is {@code null}.
     */
    public void setXPathVariableResolver(XPathVariableResolver resolver);

    /**
       * Return the current variable resolver.
       *
       * <p>{@code null} is returned in no variable resolver is in effect.
       *
       * @return Current variable resolver.
       */
    public XPathVariableResolver getXPathVariableResolver();

    /**
       * Establish a function resolver.
       *
       * <p>A {@code NullPointerException} is thrown if {@code resolver} is {@code null}.
       *
       * @param resolver XPath function resolver.
       *
       * @throws NullPointerException If {@code resolver} is {@code null}.
       */
    public void setXPathFunctionResolver(XPathFunctionResolver resolver);

    /**
       * Return the current function resolver.
       * <p>
       * {@code null} is returned in no function resolver is in effect.
       *
       * @return Current function resolver.
       */
    public XPathFunctionResolver getXPathFunctionResolver();

    /**
       * Establish a namespace context.
       *
       * <p>A {@code NullPointerException} is thrown if {@code nsContext} is {@code null}.
       *
       * @param nsContext Namespace context to use.
       *
       * @throws NullPointerException If {@code nsContext} is {@code null}.
       */
    public void setNamespaceContext(NamespaceContext nsContext);

    /**
       * Return the current namespace context.
       *
       * <p>{@code null} is returned in no namespace context is in effect.
       *
       * @return Current Namespace context.
       */
    public NamespaceContext getNamespaceContext();

    /**
       * Compile an XPath expression for later evaluation.
       *
       * <p>If {@code expression} contains any {@link XPathFunction}s,
       * they must be available via the {@link XPathFunctionResolver}.
       * An {@link XPathExpressionException} will be thrown if the
       * {@code XPathFunction}
       * cannot be resovled with the {@code XPathFunctionResolver}.
       *
       * <p>If {@code expression} contains any variables, the
       * {@link XPathVariableResolver} in effect
       * <strong>at compile time</strong> will be used to resolve them.
       *
       * @param expression The XPath expression.
       *
       * @return Compiled XPath expression.

       * @throws XPathExpressionException If {@code expression} cannot be compiled.
       * @throws NullPointerException If {@code expression} is {@code null}.
       */
    public XPathExpression compile(String expression)
        throws XPathExpressionException;

    /**
     * Evaluate an {@code XPath} expression in the specified context and
     * return the result as the specified type.
     *
     * <p>
     * See <a href="#XPath-evaluation">Evaluation of XPath Expressions</a>
     * for context item evaluation, variable, function and {@code QName} resolution
     * and return type conversion.
     * <p>
     * The parameter {@code item} represents the context the XPath expression
     * will be operated on. The type of the context is implementation-dependent.
     * If the value is {@code null}, the operation must have no dependency on
     * the context, otherwise an XPathExpressionException will be thrown.
     *
     * @implNote
     * The type of the context is usually {@link org.w3c.dom.Node}.
     *
     * @param expression The XPath expression.
     * @param item The context the XPath expression will be evaluated in.
     * @param returnType The result type expected to be returned by the XPath expression.
     *
     * @return The result of evaluating an XPath expression as an {@code Object} of {@code returnType}.
     *
     * @throws XPathExpressionException If {@code expression} cannot be evaluated.
     * @throws IllegalArgumentException If {@code returnType} is not one of the types defined in {@link XPathConstants} (
     * {@link XPathConstants#NUMBER NUMBER},
     * {@link XPathConstants#STRING STRING},
     * {@link XPathConstants#BOOLEAN BOOLEAN},
     * {@link XPathConstants#NODE NODE} or
     * {@link XPathConstants#NODESET NODESET}).
     * @throws NullPointerException If {@code expression or returnType} is {@code null}.
     */
    public Object evaluate(String expression, Object item, QName returnType)
        throws XPathExpressionException;

    /**
     * Evaluate an XPath expression in the specified context and return the result as a {@code String}.
     *
     * <p>This method calls {@link #evaluate(String expression, Object item, QName returnType)} with a {@code returnType} of
     * {@link XPathConstants#STRING}.
     *
     * <p>See <a href="#XPath-evaluation">Evaluation of XPath Expressions</a> for context item evaluation,
     * variable, function and QName resolution and return type conversion.
     *
     * <p>
     * The parameter {@code item} represents the context the XPath expression
     * will be operated on. The type of the context is implementation-dependent.
     * If the value is {@code null}, the operation must have no dependency on
     * the context, otherwise an XPathExpressionException will be thrown.
     *
     * @implNote
     * The type of the context is usually {@link org.w3c.dom.Node}.
     *
     * @param expression The XPath expression.
     * @param item The context the XPath expression will be evaluated in.
     *
     * @return The result of evaluating an XPath expression as a {@code String}.
     *
     * @throws XPathExpressionException If {@code expression} cannot be evaluated.
     * @throws NullPointerException If {@code expression} is {@code null}.
     */
    public String evaluate(String expression, Object item)
        throws XPathExpressionException;

    /**
     * Evaluate an XPath expression in the context of the specified {@code InputSource}
     * and return the result as the specified type.
     *
     * <p>This method builds a data model for the {@link InputSource} and calls
     * {@link #evaluate(String expression, Object item, QName returnType)} on the resulting document object.
     *
     * <p>See <a href="#XPath-evaluation">Evaluation of XPath Expressions</a> for context item evaluation,
     * variable, function and QName resolution and return type conversion.
     *
     * @param expression The XPath expression.
     * @param source The input source of the document to evaluate over.
     * @param returnType The desired return type.
     *
     * @return The {@code Object} that encapsulates the result of evaluating the expression.
     *
     * @throws XPathExpressionException If expression cannot be evaluated.
     * @throws IllegalArgumentException If {@code returnType} is not one of the types defined in {@link XPathConstants}.
     * @throws NullPointerException If {@code expression, source or returnType} is {@code null}.
     */
    public Object evaluate(
        String expression,
        InputSource source,
        QName returnType)
        throws XPathExpressionException;

    /**
     * Evaluate an XPath expression in the context of the specified {@code InputSource}
     * and return the result as a {@code String}.
     *
     * <p>This method calls {@link #evaluate(String expression, InputSource source, QName returnType)} with a
     * {@code returnType} of {@link XPathConstants#STRING}.
     *
     * <p>See <a href="#XPath-evaluation">Evaluation of XPath Expressions</a> for context item evaluation,
     * variable, function and QName resolution and return type conversion.
     *
     * @param expression The XPath expression.
     * @param source The {@code InputSource} of the document to evaluate over.
     *
     * @return The {@code String} that is the result of evaluating the expression and
     *   converting the result to a {@code String}.
     *
     * @throws XPathExpressionException If expression cannot be evaluated.
     * @throws NullPointerException If {@code expression or source} is {@code null}.
     */
    public String evaluate(String expression, InputSource source)
        throws XPathExpressionException;

    /**
     * Evaluate an XPath expression in the specified context and return
     * the result with the type specified through the {@code class type}
     *
     * <p>
     * The parameter {@code item} represents the context the XPath expression
     * will be operated on. The type of the context is implementation-dependent.
     * If the value is {@code null}, the operation must have no dependency on
     * the context, otherwise an XPathExpressionException will be thrown.
     *
     * @implNote
     * The type of the context is usually {@link org.w3c.dom.Node}.
     *
     * @implSpec
     * The default implementation in the XPath API is equivalent to:
     * <pre> {@code
     *     (T)evaluate(expression, item,
     *           XPathEvaluationResult.XPathResultType.getQNameType(type));
     * }</pre>
     *
     * Since the {@code evaluate} method does not support the
     * {@link XPathEvaluationResult.XPathResultType#ANY ANY} type, specifying
     * XPathEvaluationResult as the type will result in IllegalArgumentException.
     * Any implementation supporting the
     * {@link XPathEvaluationResult.XPathResultType#ANY ANY} type must override
     * this method.
     *
     * @param <T> The class type that will be returned by the XPath expression.
     * @param expression The XPath expression.
     * @param item The context the XPath expression will be evaluated in.
     * @param type The class type expected to be returned by the XPath expression,
     * must be one of the types described in section
     * <a href="package-summary.html#XPath.Datatypes.Class">3.2 Class types</a>
     * in the package summary.
     *
     *
     * @return The result of evaluating the expression.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     * @throws IllegalArgumentException If {@code type} is not of the types
     * corresponding to the types defined in the {@link XPathEvaluationResult.XPathResultType},
     * or XPathEvaluationResult is specified as the type but an implementation supporting the
     * {@link XPathEvaluationResult.XPathResultType#ANY ANY} type is not available.
     * @throws NullPointerException If {@code expression or type} is {@code null}.
     *
     * @since 9
     */
    default <T>T evaluateExpression(String expression, Object item, Class<T> type)
        throws XPathExpressionException {
        return type.cast(evaluate(expression, item,
                XPathEvaluationResult.XPathResultType.getQNameType(type)));
    }

    /**
     * Evaluate an XPath expression in the specified context. This is equivalent to
     * calling {@link #evaluateExpression(String expression, Object item, Class type)}
     * with type {@link XPathEvaluationResult}:
     * <pre> {@code
     *     evaluateExpression(expression, item, XPathEvaluationResult.class);
     * }</pre>
     * <p>
     * The parameter {@code item} represents the context the XPath expression
     * will be operated on. The type of the context is implementation-dependent.
     * If the value is {@code null}, the operation must have no dependency on
     * the context, otherwise an XPathExpressionException will be thrown.
     *
     * @implNote
     * The type of the context is usually {@link org.w3c.dom.Node}.
     *
     * @implSpec
     * The default implementation in the XPath API is equivalent to:
     * <pre> {@code
     *     evaluateExpression(expression, item, XPathEvaluationResult.class);
     * }</pre>
     *
     * Since the {@code evaluate} method does not support the
     * {@link XPathEvaluationResult.XPathResultType#ANY ANY}
     * type, the default implementation of this method will always throw an
     * IllegalArgumentException. Any implementation supporting the
     * {@link XPathEvaluationResult.XPathResultType#ANY ANY} type must therefore
     * override this method.
     *
     * @param expression The XPath expression.
     * @param item The context the XPath expression will be evaluated in.
     *
     * @return The result of evaluating the expression.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     * @throws IllegalArgumentException If the implementation of this method
     * does not support the
     * {@link XPathEvaluationResult.XPathResultType#ANY ANY} type.
     * @throws NullPointerException If {@code expression} is {@code null}.
     *
     * @since 9
     */
    default XPathEvaluationResult<?> evaluateExpression(String expression, Object item)
        throws XPathExpressionException
    {
        return evaluateExpression(expression, item, XPathEvaluationResult.class);
    }

    /**
     * Evaluate an XPath expression in the context of the specified {@code source}
     * and return the result as specified.
     * <p>
     * This method builds a data model for the {@link InputSource} and calls
     * {@link #evaluateExpression(String expression, Object item, Class type)}
     * on the resulting document object. The data model is usually
     * {@link org.w3c.dom.Document}
     *
     * @implSpec
     * The default implementation in the XPath API is equivalent to:
     * <pre> {@code
           (T)evaluate(expression, source,
                XPathEvaluationResult.XPathResultType.getQNameType(type));
     * }</pre>
     *
     * Since the {@code evaluate} method does not support the
     * {@link XPathEvaluationResult.XPathResultType#ANY ANY} type, specifying
     * XPathEvaluationResult as the type will result in IllegalArgumentException.
     * Any implementation supporting the
     * {@link XPathEvaluationResult.XPathResultType#ANY ANY} type must override
     * this method.
     *
     * @param <T> The class type that will be returned by the XPath expression.
     * @param expression The XPath expression.
     * @param source The input source of the document to evaluate over.
     * @param type The class type expected to be returned by the XPath expression,
     * must be one of the types described in section
     * <a href="package-summary.html#XPath.Datatypes.Class">3.2 Class types</a>
     * in the package summary.
     *
     * @return The result of evaluating the expression.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     * @throws IllegalArgumentException If {@code type} is not of the types
     * corresponding to the types defined in the {@link XPathEvaluationResult.XPathResultType
     * XPathResultType}, or XPathEvaluationResult is specified as the type but an
     * implementation supporting the
     * {@link XPathEvaluationResult.XPathResultType#ANY ANY} type is not available.
     * @throws NullPointerException If {@code expression, source or type}is {@code null}.
     *
     * @since 9
     */
    default <T>T evaluateExpression(String expression, InputSource source, Class<T> type)
        throws XPathExpressionException
    {
        return type.cast(evaluate(expression, source,
                XPathEvaluationResult.XPathResultType.getQNameType(type)));
    }

    /**
     * Evaluate an XPath expression in the specified context. This is equivalent to
     * calling {@link #evaluateExpression(String expression, Object item, Class type)}
     * with type {@link XPathEvaluationResult}:
     * <pre> {@code
     *     evaluateExpression(expression, item, XPathEvaluationResult.class);
     * }</pre>
     *
     * @implSpec
     * The default implementation in the XPath API is equivalent to:
     * <pre> {@code
     *     evaluateExpression(expression, source, XPathEvaluationResult.class);
     * }</pre>
     *
     * Since the {@code evaluate} method does not support the
     * {@link XPathEvaluationResult.XPathResultType#ANY ANY}
     * type, the default implementation of this method will always throw an
     * IllegalArgumentException. Any implementation supporting the
     * {@link XPathEvaluationResult.XPathResultType#ANY ANY} type must therefore
     * override this method.
     *
     * @param expression The XPath expression.
     * @param source The input source of the document to evaluate over.
     *
     * @return The result of evaluating the expression.
     *
     * @throws XPathExpressionException If the expression cannot be evaluated.
     * @throws IllegalArgumentException If the implementation of this method
     * does not support the
     * {@link XPathEvaluationResult.XPathResultType#ANY ANY} type.
     * @throws NullPointerException If {@code expression or source} is {@code null}.
     *
     * @since 9
     */
    default XPathEvaluationResult<?> evaluateExpression(String expression, InputSource source)
        throws XPathExpressionException
    {
        return evaluateExpression(expression, source, XPathEvaluationResult.class);
    }
}
