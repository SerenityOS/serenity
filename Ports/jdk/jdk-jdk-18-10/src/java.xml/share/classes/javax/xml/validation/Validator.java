/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.validation;

import java.io.IOException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;

import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * A processor that checks an XML document against {@link Schema}.
 *
 * <p>
 * A validator object is not thread-safe and not reentrant.
 * In other words, it is the application's responsibility to make
 * sure that one {@link Validator} object is not used from
 * more than one thread at any given time, and while the {@code validate}
 * method is invoked, applications may not recursively call
 * the {@code validate} method.
 *
 *
 * @author  Kohsuke Kawaguchi
 * @since 1.5
 */
public abstract class Validator {

    /**
     * Constructor for derived classes.
     *
     * <p>The constructor does nothing.
     *
     * <p>Derived classes must create {@link Validator} objects that have
     * {@code null} {@link ErrorHandler} and
     * {@code null} {@link LSResourceResolver}.
     */
    protected Validator() {
    }

        /**
         * Reset this {@code Validator} to its original configuration.
         *
         * <p>{@code Validator} is reset to the same state as when it was created with
         * {@link Schema#newValidator()}.
         * {@code reset()} is designed to allow the reuse of existing {@code Validator}s
         * thus saving resources associated with the creation of new {@code Validator}s.
         *
         * <p>The reset {@code Validator} is not guaranteed to have
         * the same {@link LSResourceResolver} or {@link ErrorHandler}
         * {@code Object}s, e.g. {@link Object#equals(Object obj)}.
         * It is guaranteed to have a functionally equal
         * {@code LSResourceResolver} and {@code ErrorHandler}.
         */
        public abstract void reset();

    /**
     * Validates the specified input.
     *
     * <p>This is just a convenience method for
     * {@link #validate(Source source, Result result)}
     * with {@code result} of {@code null}.
     *
     * @param source
     *      XML to be validated. Must be an XML document or
     *      XML element and must not be null. For backwards compatibility,
     *      the results of attempting to validate anything other than
     *      a document or element are implementation-dependent.
     *      Implementations must either recognize and process the input
     *      or throw an IllegalArgumentException.
     *
     * @throws IllegalArgumentException
     *      If the {@code Source}
     *      is an XML artifact that the implementation cannot
     *      validate (for example, a processing instruction).
     *
     * @throws SAXException
     *      If the {@link ErrorHandler} throws a {@link SAXException} or
     *      if a fatal error is found and the {@link ErrorHandler} returns
     *      normally.
     *
     * @throws IOException
     *      If the validator is processing a
     *      {@link javax.xml.transform.sax.SAXSource} and the
     *      underlying {@link org.xml.sax.XMLReader} throws an
     *      {@link IOException}.
     *
     *
     * @throws NullPointerException If {@code source} is
     *   {@code null}.
     *
     * @see #validate(Source source, Result result)
     */
    public void validate(Source source)
        throws SAXException, IOException {

        validate(source, null);
    }

    /**
     * Validates the specified input and send the augmented validation
     * result to the specified output.
     *
     * <p>This method places the following restrictions on the types of
     * the {@link Source}/{@link Result} accepted.
     *
     * <table class="plain">
     * <caption>{@code Source} / {@code Result} Accepted</caption>
     * <thead>
     *  <tr>
     *   <td></td>
     *   <th scope="col">{@link javax.xml.transform.stream.StreamSource}</th>
     *   <th scope="col">{@link javax.xml.transform.sax.SAXSource}</th>
     *   <th scope="col">{@link javax.xml.transform.dom.DOMSource}</th>
     *   <th scope="col">{@link javax.xml.transform.stax.StAXSource}</th>
     *  </tr>
     * </thead>
     * <tbody style="text-align:center">
     *  <tr>
     *   <th scope="row">{@code null}</th>
     *   <td>OK</td>
     *   <td>OK</td>
     *   <td>OK</td>
     *   <td>OK</td>
     *  </tr>
     *  <tr>
     *   <th scope="row">{@link javax.xml.transform.stream.StreamResult}</th>
     *   <td>OK</td>
     *   <td>{@code IllegalArgumentException}</td>
     *   <td>{@code IllegalArgumentException}</td>
     *   <td>{@code IllegalArgumentException}</td>
     *  </tr>
     *  <tr>
     *   <th scope="row">{@link javax.xml.transform.sax.SAXResult}</th>
     *   <td>{@code IllegalArgumentException}</td>
     *   <td>OK</td>
     *   <td>{@code IllegalArgumentException}</td>
     *   <td>{@code IllegalArgumentException}</td>
     *  </tr>
     *  <tr>
     *   <th scope="row">{@link javax.xml.transform.dom.DOMResult}</th>
     *   <td>{@code IllegalArgumentException}</td>
     *   <td>{@code IllegalArgumentException}</td>
     *   <td>OK</td>
     *   <td>{@code IllegalArgumentException}</td>
     *  </tr>
     *  <tr>
     *   <th scope="row">{@link javax.xml.transform.stax.StAXResult}</th>
     *   <td>{@code IllegalArgumentException}</td>
     *   <td>{@code IllegalArgumentException}</td>
     *   <td>{@code IllegalArgumentException}</td>
     *   <td>OK</td>
     *  </tr>
     * </tbody>
     * </table>
     *
     * <p>To validate one {@code Source} into another kind of
     * {@code Result}, use the identity transformer (see
     * {@link javax.xml.transform.TransformerFactory#newTransformer()}).
     *
     * <p>Errors found during the validation is sent to the specified
     * {@link ErrorHandler}.
     *
     * <p>If a document is valid, or if a document contains some errors
     * but none of them were fatal and the {@code ErrorHandler} didn't
     * throw any exception, then the method returns normally.
     *
     * @param source
     *      XML to be validated. Must be an XML document or
     *      XML element and must not be null. For backwards compatibility,
     *      the results of attempting to validate anything other than
     *      a document or element are implementation-dependent.
     *      Implementations must either recognize and process the input
     *      or throw an IllegalArgumentException.
     *
     * @param result
     *      The {@code Result} object that receives (possibly augmented)
     *      XML. This parameter can be null if the caller is not interested
     *      in it.
     *
     *      Note that when a {@code DOMResult} is used,
     *      a validator might just pass the same DOM node from
     *      {@code DOMSource} to {@code DOMResult}
     *      (in which case {@code source.getNode()==result.getNode()}),
     *      it might copy the entire DOM tree, or it might alter the
     *      node given by the source.
     *
     * @throws IllegalArgumentException
     *      If the {@code Result} type doesn't match the
     *      {@code Source} type or if the {@code Source}
     *      is an XML artifact that the implementation cannot
     *      validate (for example, a processing instruction).
     * @throws SAXException
     *      If the {@code ErrorHandler} throws a
     *      {@code SAXException} or
     *      if a fatal error is found and the {@code ErrorHandler} returns
     *      normally.
     * @throws IOException
     *      If the validator is processing a
     *      {@code SAXSource} and the
     *      underlying {@link org.xml.sax.XMLReader} throws an
     *      {@code IOException}.
     * @throws NullPointerException
     *      If the {@code source} parameter is {@code null}.
     *
     * @see #validate(Source source)
     */
    public abstract void validate(Source source, Result result)
        throws SAXException, IOException;

    /**
     * Sets the {@link ErrorHandler} to receive errors encountered
     * during the {@code validate} method invocation.
     *
     * <p>
     * Error handler can be used to customize the error handling process
     * during a validation. When an {@link ErrorHandler} is set,
     * errors found during the validation will be first sent
     * to the {@link ErrorHandler}.
     *
     * <p>
     * The error handler can abort further validation immediately
     * by throwing {@link SAXException} from the handler. Or for example
     * it can print an error to the screen and try to continue the
     * validation by returning normally from the {@link ErrorHandler}
     *
     * <p>
     * If any {@link Throwable} is thrown from an {@link ErrorHandler},
     * the caller of the {@code validate} method will be thrown
     * the same {@link Throwable} object.
     *
     * <p>
     * {@link Validator} is not allowed to
     * throw {@link SAXException} without first reporting it to
     * {@link ErrorHandler}.
     *
     * <p>
     * When the {@link ErrorHandler} is null, the implementation will
     * behave as if the following {@link ErrorHandler} is set:
     * <pre>
     * class DraconianErrorHandler implements {@link ErrorHandler} {
     *     public void fatalError( {@link org.xml.sax.SAXParseException} e ) throws {@link SAXException} {
     *         throw e;
     *     }
     *     public void error( {@link org.xml.sax.SAXParseException} e ) throws {@link SAXException} {
     *         throw e;
     *     }
     *     public void warning( {@link org.xml.sax.SAXParseException} e ) throws {@link SAXException} {
     *         // noop
     *     }
     * }
     * </pre>
     *
     * <p>
     * When a new {@link Validator} object is created, initially
     * this field is set to null.
     *
     * @param   errorHandler
     *      A new error handler to be set. This parameter can be null.
     */
    public abstract void setErrorHandler(ErrorHandler errorHandler);

    /**
     * Gets the current {@link ErrorHandler} set to this {@link Validator}.
     *
     * @return
     *      This method returns the object that was last set through
     *      the {@link #setErrorHandler(ErrorHandler)} method, or null
     *      if that method has never been called since this {@link Validator}
     *      has created.
     *
     * @see #setErrorHandler(ErrorHandler)
     */
    public abstract ErrorHandler getErrorHandler();

    /**
     * Sets the {@link LSResourceResolver} to customize
     * resource resolution while in a validation episode.
     *
     * <p>
     * {@link Validator} uses a {@link LSResourceResolver}
     * when it needs to locate external resources while a validation,
     * although exactly what constitutes "locating external resources" is
     * up to each schema language.
     *
     * <p>
     * When the {@link LSResourceResolver} is null, the implementation will
     * behave as if the following {@link LSResourceResolver} is set:
     * <pre>
     * class DumbLSResourceResolver implements {@link LSResourceResolver} {
     *     public {@link org.w3c.dom.ls.LSInput} resolveResource(
     *         String publicId, String systemId, String baseURI) {
     *
     *         return null; // always return null
     *     }
     * }
     * </pre>
     *
     * <p>
     * If a {@link LSResourceResolver} throws a {@link RuntimeException}
     *  (or instances of its derived classes),
     * then the {@link Validator} will abort the parsing and
     * the caller of the {@code validate} method will receive
     * the same {@link RuntimeException}.
     *
     * <p>
     * When a new {@link Validator} object is created, initially
     * this field is set to null.
     *
     * @param   resourceResolver
     *      A new resource resolver to be set. This parameter can be null.
     */
    public abstract void setResourceResolver(LSResourceResolver resourceResolver);

    /**
     * Gets the current {@link LSResourceResolver} set to this {@link Validator}.
     *
     * @return
     *      This method returns the object that was last set through
     *      the {@link #setResourceResolver(LSResourceResolver)} method, or null
     *      if that method has never been called since this {@link Validator}
     *      has created.
     *
     * @see #setErrorHandler(ErrorHandler)
     */
    public abstract LSResourceResolver getResourceResolver();



    /**
     * Look up the value of a feature flag.
     *
     * <p>The feature name is any fully-qualified URI.  It is
     * possible for a {@link Validator} to recognize a feature name but
     * temporarily be unable to return its value.
     * Some feature values may be available only in specific
     * contexts, such as before, during, or after a validation.
     *
     * <p>Implementors are free (and encouraged) to invent their own features,
     * using names built on their own URIs.
     *
     * @param name The feature name, which is a non-null fully-qualified URI.
     *
     * @return The current value of the feature (true or false).
     *
     * @throws SAXNotRecognizedException If the feature
     *   value can't be assigned or retrieved.
     * @throws SAXNotSupportedException When the
     *   {@link Validator} recognizes the feature name but
     *   cannot determine its value at this time.
     * @throws NullPointerException
     *   When the name parameter is null.
     *
     * @see #setFeature(String, boolean)
     */
    public boolean getFeature(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException {

        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }

        throw new SAXNotRecognizedException(name);
    }

    /**
     * Set the value of a feature flag.
     *
     * <p>
     * Feature can be used to control the way a {@link Validator}
     * parses schemas, although {@link Validator}s are not required
     * to recognize any specific feature names.
     *
     * <p>The feature name is any fully-qualified URI.  It is
     * possible for a {@link Validator} to expose a feature value but
     * to be unable to change the current value.
     * Some feature values may be immutable or mutable only
     * in specific contexts, such as before, during, or after
     * a validation.
     *
     * @param name The feature name, which is a non-null fully-qualified URI.
     * @param value The requested value of the feature (true or false).
     *
     * @throws SAXNotRecognizedException If the feature
     *   value can't be assigned or retrieved.
     * @throws SAXNotSupportedException When the
     *   {@link Validator} recognizes the feature name but
     *   cannot set the requested value.
     * @throws NullPointerException
     *   When the name parameter is null.
     *
     * @see #getFeature(String)
     */
    public void setFeature(String name, boolean value)
        throws SAXNotRecognizedException, SAXNotSupportedException {

        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }

        throw new SAXNotRecognizedException(name);
    }

    /**
     * Set the value of a property.
     *
     * <p>The property name is any fully-qualified URI.  It is
     * possible for a {@link Validator} to recognize a property name but
     * to be unable to change the current value.
     * Some property values may be immutable or mutable only
     * in specific contexts, such as before, during, or after
     * a validation.
     *
     * <p>
     * All implementations that implement JAXP 1.5 or newer are required to
     * support the {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_DTD} and
     * {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_SCHEMA} properties.
     *
     * <ul>
     *   <li>
     *      <p>Access to external DTDs in source or Schema file is restricted to
     *      the protocols specified by the {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_DTD}
     *      property.  If access is denied during validation due to the restriction
     *      of this property, {@link org.xml.sax.SAXException} will be thrown by the
     *      {@link #validate(Source)} method.
     *
     *      <p>Access to external reference set by the schemaLocation attribute is
     *      restricted to the protocols specified by the
     *      {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_SCHEMA} property.
     *      If access is denied during validation due to the restriction of this property,
     *      {@link org.xml.sax.SAXException} will be thrown by the
     *      {@link #validate(Source)} method.
     *   </li>
     * </ul>
     *
     * @param name The property name, which is a non-null fully-qualified URI.
     * @param object The requested value for the property.
     *
     * @throws SAXNotRecognizedException If the property
     *   value can't be assigned or retrieved.
     * @throws SAXNotSupportedException When the
     *   {@link Validator} recognizes the property name but
     *   cannot set the requested value.
     * @throws NullPointerException
     *   When the name parameter is null.
     */
    public void setProperty(String name, Object object)
        throws SAXNotRecognizedException, SAXNotSupportedException {

        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }

        throw new SAXNotRecognizedException(name);
    }

    /**
     * Look up the value of a property.
     *
     * <p>The property name is any fully-qualified URI.  It is
     * possible for a {@link Validator} to recognize a property name but
     * temporarily be unable to return its value.
     * Some property values may be available only in specific
     * contexts, such as before, during, or after a validation.
     *
     * <p>{@link Validator}s are not required to recognize any specific
     * property names.
     *
     * <p>Implementors are free (and encouraged) to invent their own properties,
     * using names built on their own URIs.
     *
     * @param name The property name, which is a non-null fully-qualified URI.
     *
     * @return The current value of the property.
     *
     * @throws SAXNotRecognizedException If the property
     *   value can't be assigned or retrieved.
     * @throws SAXNotSupportedException When the
     *   XMLReader recognizes the property name but
     *   cannot determine its value at this time.
     * @throws NullPointerException
     *   When the name parameter is null.
     *
     * @see #setProperty(String, Object)
     */
    public Object getProperty(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException {

        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }

        throw new SAXNotRecognizedException(name);
    }
}
