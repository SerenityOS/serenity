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

package javax.xml.validation;

import com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory;
import java.io.File;
import java.net.URL;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import jdk.xml.internal.SecuritySupport;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;

/**
 * Factory that creates {@link Schema} objects. Entry-point to
 * the validation API.
 *
 * <p>
 * {@link SchemaFactory} is a schema compiler. It reads external
 * representations of schemas and prepares them for validation.
 *
 * <p>
 * The {@link SchemaFactory} class is not thread-safe. In other words,
 * it is the application's responsibility to ensure that at most
 * one thread is using a {@link SchemaFactory} object at any
 * given moment. Implementations are encouraged to mark methods
 * as {@code synchronized} to protect themselves from broken clients.
 *
 * <p>
 * {@link SchemaFactory} is not re-entrant. While one of the
 * {@code newSchema} methods is being invoked, applications
 * may not attempt to recursively invoke the {@code newSchema} method,
 * even from the same thread.
 *
 * <h2><a id="schemaLanguage"></a>Schema Language</h2>
 * <p>
 * This spec uses a namespace URI to designate a schema language.
 * The following table shows the values defined by this specification.
 * <p>
 * To be compliant with the spec, the implementation
 * is only required to support W3C XML Schema 1.0. However,
 * if it chooses to support other schema languages listed here,
 * it must conform to the relevant behaviors described in this spec.
 *
 * <p>
 * Schema languages not listed here are expected to
 * introduce their own URIs to represent themselves.
 * The {@link SchemaFactory} class is capable of locating other
 * implementations for other schema languages at run-time.
 *
 * <p>
 * Note that because the XML DTD is strongly tied to the parsing process
 * and has a significant effect on the parsing process, it is impossible
 * to define the DTD validation as a process independent from parsing.
 * For this reason, this specification does not define the semantics for
 * the XML DTD. This doesn't prohibit implementors from implementing it
 * in a way they see fit, but <em>users are warned that any DTD
 * validation implemented on this interface necessarily deviate from
 * the XML DTD semantics as defined in the XML 1.0</em>.
 *
 * <table class="striped">
 *   <caption>URIs for Supported Schema languages</caption>
 *   <thead>
 *     <tr>
 *       <th scope="col">value</th>
 *       <th scope="col">language</th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <th scope="row">{@link javax.xml.XMLConstants#W3C_XML_SCHEMA_NS_URI} ("{@code http://www.w3.org/2001/XMLSchema}")</th>
 *       <td><a href="http://www.w3.org/TR/xmlschema-1">W3C XML Schema 1.0</a></td>
 *     </tr>
 *     <tr>
 *       <th scope="row">{@link javax.xml.XMLConstants#RELAXNG_NS_URI} ("{@code http://relaxng.org/ns/structure/1.0}")</th>
 *       <td><a href="http://www.relaxng.org/">RELAX NG 1.0</a></td>
 *     </tr>
 *   </tbody>
 * </table>
 *
 * @author  Kohsuke Kawaguchi
 * @author  Neeraj Bajaj
 *
 * @since 1.5
 */
public abstract class SchemaFactory {

    /**
     * Constructor for derived classes.
     *
     * <p>The constructor does nothing.
     *
     * <p>Derived classes must create {@link SchemaFactory} objects that have
     * {@code null} {@link ErrorHandler} and
     * {@code null} {@link LSResourceResolver}.
     */
    protected SchemaFactory() {
    }

    /**
     * Creates a new instance of the {@code SchemaFactory} builtin
     * system-default implementation.
     *
     * @implSpec The {@code SchemaFactory} builtin
     * system-default implementation is only required to support the
     * <a href="http://www.w3.org/TR/xmlschema-1">W3C XML Schema 1.0</a>,
     * but may support additional <a href="#schemaLanguage">schema languages</a>.
     *
     * @return A new instance of the {@code SchemaFactory} builtin
     *         system-default implementation.
     *
     * @since 9
     */
    public static SchemaFactory newDefaultInstance() {
        return new XMLSchemaFactory();
    }

    /**
     * Obtains a new instance of a {@code SchemaFactory} that supports
     * the specified schema language. This method uses the
     * <a href="../../../module-summary.html#LookupMechanism">JAXP Lookup Mechanism</a>
     * to determine and load the {@code SchemaFactory} implementation that supports
     * the specified schema language.
     *
     * <h4>Tip for Trouble-shooting:</h4>
     * <p>See {@link java.util.Properties#load(java.io.InputStream)} for
     * exactly how a property file is parsed. In particular, colons ':'
     * need to be escaped in a property file, so make sure schema language
     * URIs are properly escaped in it. For example:
     * <pre>
     * http\://www.w3.org/2001/XMLSchema=org.acme.foo.XSSchemaFactory
     * </pre>
     *
     * @param schemaLanguage
     *      Specifies the schema language which the returned
     *      SchemaFactory will understand. See
     *      <a href="#schemaLanguage">the list of available
     *      schema languages</a> for the possible values.
     *
     * @return New instance of a {@code SchemaFactory}
     *
     * @throws IllegalArgumentException
     *      If no implementation of the schema language is available.
     * @throws NullPointerException
     *      If the {@code schemaLanguage} parameter is null.
     * @throws SchemaFactoryConfigurationError
     *      If a configuration error is encountered.
     *
     * @see #newInstance(String schemaLanguage, String factoryClassName, ClassLoader classLoader)
     */
    public static SchemaFactory newInstance(String schemaLanguage) {
        ClassLoader cl;
        cl = SecuritySupport.getContextClassLoader();

        if (cl == null) {
            //cl = ClassLoader.getSystemClassLoader();
            //use the current class loader
            cl = SchemaFactory.class.getClassLoader();
        }

        SchemaFactory f = new SchemaFactoryFinder(cl).newFactory(schemaLanguage);
        if (f == null) {
            throw new IllegalArgumentException(
                    "No SchemaFactory"
                    + " that implements the schema language specified by: " + schemaLanguage
                    + " could be loaded");
        }
        return f;
    }

    /**
     * Obtain a new instance of a {@code SchemaFactory} from class name. {@code SchemaFactory}
     * is returned if specified factory class name supports the specified schema language.
     * This function is useful when there are multiple providers in the classpath.
     * It gives more control to the application as it can specify which provider
     * should be loaded.
     *
     * <h4>Tip for Trouble-shooting</h4>
     * <p>Setting the {@code jaxp.debug} system property will cause
     * this method to print a lot of debug messages
     * to {@code System.err} about what it is doing and where it is looking at.
     *
     * <p> If you have problems try:
     * <pre>
     * java -Djaxp.debug=1 YourProgram ....
     * </pre>
     *
     * @param schemaLanguage Specifies the schema language which the returned
     *                          {@code SchemaFactory} will understand. See
     *                          <a href="#schemaLanguage">the list of available
     *                          schema languages</a> for the possible values.
     *
     * @param factoryClassName fully qualified factory class name that provides implementation of {@code javax.xml.validation.SchemaFactory}.
     *
     * @param classLoader {@code ClassLoader} used to load the factory class. If {@code null}
     *                     current {@code Thread}'s context classLoader is used to load the factory class.
     *
     * @return New instance of a {@code SchemaFactory}
     *
     * @throws IllegalArgumentException
     *                   if {@code factoryClassName} is {@code null}, or
     *                   the factory class cannot be loaded, instantiated or doesn't
     *                   support the schema language specified in {@code schemLanguage}
     *                   parameter.
     *
     * @throws NullPointerException
     *      If the {@code schemaLanguage} parameter is null.
     *
     * @see #newInstance(String schemaLanguage)
     *
     * @since 1.6
     */
    public static SchemaFactory newInstance(String schemaLanguage, String factoryClassName, ClassLoader classLoader){
        ClassLoader cl = classLoader;

        if (cl == null) {
            cl = SecuritySupport.getContextClassLoader();
        }

        SchemaFactory f = new SchemaFactoryFinder(cl).createInstance(factoryClassName);
        if (f == null) {
            throw new IllegalArgumentException(
                    "Factory " + factoryClassName
                    + " could not be loaded to implement the schema language specified by: " + schemaLanguage);
        }
        //if this factory supports the given schemalanguage return this factory else thrown exception
        if(f.isSchemaLanguageSupported(schemaLanguage)){
            return f;
        }else{
            throw new IllegalArgumentException(
                    "Factory " + f.getClass().getName()
                    + " does not implement the schema language specified by: " + schemaLanguage);
        }

    }

    /**
     * Is specified schema supported by this {@code SchemaFactory}?
     *
     * @param schemaLanguage Specifies the schema language which the returned {@code SchemaFactory} will understand.
     *    {@code schemaLanguage} must specify a <a href="#schemaLanguage">valid</a> schema language.
     *
     * @return {@code true} if {@code SchemaFactory} supports {@code schemaLanguage}, else {@code false}.
     *
     * @throws NullPointerException If {@code schemaLanguage} is {@code null}.
     * @throws IllegalArgumentException If {@code schemaLanguage.length() == 0}
     *   or {@code schemaLanguage} does not specify a <a href="#schemaLanguage">valid</a> schema language.
     */
    public abstract boolean isSchemaLanguageSupported(String schemaLanguage);

    /**
     * Look up the value of a feature flag.
     *
     * <p>The feature name is any fully-qualified URI.  It is
     * possible for a {@link SchemaFactory} to recognize a feature name but
     * temporarily be unable to return its value.
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
     *   {@link SchemaFactory} recognizes the feature name but
     *   cannot determine its value at this time.
     * @throws NullPointerException If {@code name} is {@code null}.
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
     * Set a feature for this {@code SchemaFactory},
     * {@link Schema}s created by this factory, and by extension,
     * {@link Validator}s and {@link ValidatorHandler}s created by
     * those {@link Schema}s.
     *
     * <p>Implementors and developers should pay particular attention
     * to how the special {@link Schema} object returned by {@link
     * #newSchema()} is processed. In some cases, for example, when the
     * {@code SchemaFactory} and the class actually loading the
     * schema come from different implementations, it may not be possible
     * for {@code SchemaFactory} features to be inherited automatically.
     * Developers should
     * make sure that features, such as secure processing, are explicitly
     * set in both places.
     *
     * <p>The feature name is any fully-qualified URI. It is
     * possible for a {@link SchemaFactory} to expose a feature value but
     * to be unable to change the current value.
     *
     * <p>All implementations are required to support the {@link javax.xml.XMLConstants#FEATURE_SECURE_PROCESSING} feature.
     * When the feature is:
     * <ul>
     *   <li>
     *     {@code true}: the implementation will limit XML processing to conform to implementation limits.
     *     Examples include entity expansion limits and XML Schema constructs that would consume large amounts of resources.
     *     If XML processing is limited for security reasons, it will be reported via a call to the registered
     *    {@link ErrorHandler#fatalError(SAXParseException exception)}.
     *     See {@link #setErrorHandler(ErrorHandler errorHandler)}.
     *   </li>
     *   <li>
     *     {@code false}: the implementation will processing XML according to the XML specifications without
     *     regard to possible implementation limits.
     *   </li>
     * </ul>
     *
     * @param name The feature name, which is a non-null fully-qualified URI.
     * @param value The requested value of the feature (true or false).
     *
     * @throws SAXNotRecognizedException If the feature
     *   value can't be assigned or retrieved.
     * @throws SAXNotSupportedException When the
     *   {@link SchemaFactory} recognizes the feature name but
     *   cannot set the requested value.
     * @throws NullPointerException If {@code name} is {@code null}.
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
     * <p>The property name is any fully-qualified URI. It is
     * possible for a {@link SchemaFactory} to recognize a property name but
     * to be unable to change the current value.
     *
     * <p>
     * All implementations that implement JAXP 1.5 or newer are required to
     * support the {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_DTD} and
     * {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_SCHEMA} properties.
     *
     * <ul>
     *   <li>
     *      <p>Access to external DTDs in Schema files is restricted to the protocols
     *      specified by the {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_DTD} property.
     *      If access is denied during the creation of new Schema due to the restriction
     *      of this property, {@link org.xml.sax.SAXException} will be thrown by the
     *      {@link #newSchema(Source)} or {@link #newSchema(File)}
     *      or {@link #newSchema(URL)} or {@link #newSchema(Source[])} method.
     *
     *      <p>Access to external DTDs in xml source files is restricted to the protocols
     *      specified by the {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_DTD} property.
     *      If access is denied during validation due to the restriction
     *      of this property, {@link org.xml.sax.SAXException} will be thrown by the
     *      {@link javax.xml.validation.Validator#validate(Source)} or
     *      {@link javax.xml.validation.Validator#validate(Source, Result)} method.
     *
     *      <p>Access to external reference set by the schemaLocation attribute is
     *      restricted to the protocols specified by the
     *      {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_SCHEMA} property.
     *      If access is denied during validation due to the restriction of this property,
     *      {@link org.xml.sax.SAXException} will be thrown by the
     *      {@link javax.xml.validation.Validator#validate(Source)} or
     *      {@link javax.xml.validation.Validator#validate(Source, Result)} method.
     *
     *      <p>Access to external reference set by the Import
     *      and Include element is restricted to the protocols specified by the
     *      {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_SCHEMA} property.
     *      If access is denied during the creation of new Schema due to the restriction
     *      of this property, {@link org.xml.sax.SAXException} will be thrown by the
     *      {@link #newSchema(Source)} or {@link #newSchema(File)}
     *      or {@link #newSchema(URL)} or {@link #newSchema(Source[])} method.
     *   </li>
     * </ul>
     *
     * @param name The property name, which is a non-null fully-qualified URI.
     * @param object The requested value for the property.
     *
     * @throws SAXNotRecognizedException If the property
     *   value can't be assigned or retrieved.
     * @throws SAXNotSupportedException When the
     *   {@link SchemaFactory} recognizes the property name but
     *   cannot set the requested value.
     * @throws NullPointerException If {@code name} is {@code null}.
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
     * possible for a {@link SchemaFactory} to recognize a property name but
     * temporarily be unable to return its value.
     *
     * <p>{@link SchemaFactory}s are not required to recognize any specific
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
     * @throws NullPointerException If {@code name} is {@code null}.
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

    /**
     * Sets the {@link ErrorHandler} to receive errors encountered
     * during the {@code newSchema} method invocation.
     *
     * <p>
     * Error handler can be used to customize the error handling process
     * during schema parsing. When an {@link ErrorHandler} is set,
     * errors found during the parsing of schemas will be first sent
     * to the {@link ErrorHandler}.
     *
     * <p>
     * The error handler can abort the parsing of a schema immediately
     * by throwing {@link SAXException} from the handler. Or for example
     * it can print an error to the screen and try to continue the
     * processing by returning normally from the {@link ErrorHandler}
     *
     * <p>
     * If any {@link Throwable} (or instances of its derived classes)
     * is thrown from an {@link ErrorHandler},
     * the caller of the {@code newSchema} method will be thrown
     * the same {@link Throwable} object.
     *
     * <p>
     * {@link SchemaFactory} is not allowed to
     * throw {@link SAXException} without first reporting it to
     * {@link ErrorHandler}.
     *
     * <p>
     * Applications can call this method even during a {@link Schema}
     * is being parsed.
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
     * When a new {@link SchemaFactory} object is created, initially
     * this field is set to null. This field will <em>NOT</em> be
     * inherited to {@link Schema}s, {@link Validator}s, or
     * {@link ValidatorHandler}s that are created from this {@link SchemaFactory}.
     *
     * @param errorHandler A new error handler to be set.
     *   This parameter can be {@code null}.
     */
    public abstract void setErrorHandler(ErrorHandler errorHandler);

    /**
     * Gets the current {@link ErrorHandler} set to this {@link SchemaFactory}.
     *
     * @return
     *      This method returns the object that was last set through
     *      the {@link #setErrorHandler(ErrorHandler)} method, or null
     *      if that method has never been called since this {@link SchemaFactory}
     *      has created.
     *
     * @see #setErrorHandler(ErrorHandler)
     */
    public abstract ErrorHandler getErrorHandler();

    /**
     * Sets the {@link LSResourceResolver} to customize
     * resource resolution when parsing schemas.
     *
     * <p>
     * {@link SchemaFactory} uses a {@link LSResourceResolver}
     * when it needs to locate external resources while parsing schemas,
     * although exactly what constitutes "locating external resources" is
     * up to each schema language. For example, for W3C XML Schema,
     * this includes files {@code <include>}d or {@code <import>}ed,
     * and DTD referenced from schema files, etc.
     *
     * <p>
     * Applications can call this method even during a {@link Schema}
     * is being parsed.
     *
     * <p>
     * When the {@link LSResourceResolver} is null, the implementation will
     * behave as if the following {@link LSResourceResolver} is set:
     * <pre>
     * class DumbDOMResourceResolver implements {@link LSResourceResolver} {
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
     * then the {@link SchemaFactory} will abort the parsing and
     * the caller of the {@code newSchema} method will receive
     * the same {@link RuntimeException}.
     *
     * <p>
     * When a new {@link SchemaFactory} object is created, initially
     * this field is set to null.  This field will <em>NOT</em> be
     * inherited to {@link Schema}s, {@link Validator}s, or
     * {@link ValidatorHandler}s that are created from this {@link SchemaFactory}.
     *
     * @param   resourceResolver
     *      A new resource resolver to be set. This parameter can be null.
     */
    public abstract void setResourceResolver(LSResourceResolver resourceResolver);

    /**
     * Gets the current {@link LSResourceResolver} set to this {@link SchemaFactory}.
     *
     * @return
     *      This method returns the object that was last set through
     *      the {@link #setResourceResolver(LSResourceResolver)} method, or null
     *      if that method has never been called since this {@link SchemaFactory}
     *      has created.
     *
     * @see #setErrorHandler(ErrorHandler)
     */
    public abstract LSResourceResolver getResourceResolver();

    /**
     * Parses the specified source as a schema and returns it as a schema.
     *
     * <p>This is a convenience method for {@link #newSchema(Source[] schemas)}.
     *
     * @param schema Source that represents a schema.
     *
     * @return New {@code Schema} from parsing {@code schema}.
     *
     * @throws SAXException If a SAX error occurs during parsing.
     * @throws NullPointerException if {@code schema} is null.
     */
    public Schema newSchema(Source schema) throws SAXException {
        return newSchema(new Source[]{schema});
    }

    /**
     * Parses the specified {@code File} as a schema and returns it as a {@code Schema}.
     *
     * <p>This is a convenience method for {@link #newSchema(Source schema)}.
     *
     * @param schema File that represents a schema.
     *
     * @return New {@code Schema} from parsing {@code schema}.
     *
     * @throws SAXException If a SAX error occurs during parsing.
     * @throws NullPointerException if {@code schema} is null.
     */
    public Schema newSchema(File schema) throws SAXException {
        return newSchema(new StreamSource(schema));
    }

    /**
     * Parses the specified {@code URL} as a schema and returns it as a {@code Schema}.
     *
     * <p>This is a convenience method for {@link #newSchema(Source schema)}.
     *
     * @param schema {@code URL} that represents a schema.
     *
     * @return New {@code Schema} from parsing {@code schema}.
     *
     * @throws SAXException If a SAX error occurs during parsing.
     * @throws NullPointerException if {@code schema} is null.
     */
    public Schema newSchema(URL schema) throws SAXException {
        return newSchema(new StreamSource(schema.toExternalForm()));
    }

    /**
     * Parses the specified source(s) as a schema and returns it as a schema.
     *
     * <p>
     * The callee will read all the {@link Source}s and combine them into a
     * single schema. The exact semantics of the combination depends on the schema
     * language that this {@link SchemaFactory} object is created for.
     *
     * <p>
     * When an {@link ErrorHandler} is set, the callee will report all the errors
     * found in sources to the handler. If the handler throws an exception, it will
     * abort the schema compilation and the same exception will be thrown from
     * this method. Also, after an error is reported to a handler, the callee is allowed
     * to abort the further processing by throwing it. If an error handler is not set,
     * the callee will throw the first error it finds in the sources.
     *
     * <h4>W3C XML Schema 1.0</h4>
     * <p>
     * The resulting schema contains components from the specified sources.
     * The same result would be achieved if all these sources were
     * imported, using appropriate values for schemaLocation and namespace,
     * into a single schema document with a different targetNamespace
     * and no components of its own, if the import elements were given
     * in the same order as the sources.  Section 4.2.3 of the XML Schema
     * recommendation describes the options processors have in this
     * regard.  While a processor should be consistent in its treatment of
     * JAXP schema sources and XML Schema imports, the behaviour between
     * JAXP-compliant parsers may vary; in particular, parsers may choose
     * to ignore all but the first {@code <import>} for a given namespace,
     * regardless of information provided in schemaLocation.
     *
     * <p>
     * If the parsed set of schemas includes error(s) as
     * specified in the section 5.1 of the XML Schema spec, then
     * the error must be reported to the {@link ErrorHandler}.
     *
     * <h4>RELAX NG</h4>
     *
     * <p>For RELAX NG, this method must throw {@link UnsupportedOperationException}
     * if {@code schemas.length!=1}.
     *
     *
     * @param schemas
     *      inputs to be parsed. {@link SchemaFactory} is required
     *      to recognize {@link javax.xml.transform.sax.SAXSource},
     *      {@link StreamSource},
     *      {@link javax.xml.transform.stax.StAXSource},
     *      and {@link javax.xml.transform.dom.DOMSource}.
     *      Input schemas must be XML documents or
     *      XML elements and must not be null. For backwards compatibility,
     *      the results of passing anything other than
     *      a document or element are implementation-dependent.
     *      Implementations must either recognize and process the input
     *      or thrown an IllegalArgumentException.
     *
     * @return
     *      Always return a non-null valid {@link Schema} object.
     *      Note that when an error has been reported, there is no
     *      guarantee that the returned {@link Schema} object is
     *      meaningful.
     *
     * @throws SAXException
     *      If an error is found during processing the specified inputs.
     *      When an {@link ErrorHandler} is set, errors are reported to
     *      there first. See {@link #setErrorHandler(ErrorHandler)}.
     * @throws NullPointerException
     *      If the {@code schemas} parameter itself is null or
     *      any item in the array is null.
     * @throws IllegalArgumentException
     *      If any item in the array is not recognized by this method.
     * @throws UnsupportedOperationException
     *      If the schema language doesn't support this operation.
     */
    public abstract Schema newSchema(Source[] schemas) throws SAXException;

    /**
     * Creates a special {@link Schema} object.
     *
     * <p>The exact semantics of the returned {@link Schema} object
     * depend on the schema language for which this {@link SchemaFactory}
     * is created.
     *
     * <p>Also, implementations are allowed to use implementation-specific
     * property/feature to alter the semantics of this method.
     *
     * <p>Implementors and developers should pay particular attention
     * to how the features set on this {@link SchemaFactory} are
     * processed by this special {@link Schema}.
     * In some cases, for example, when the
     * {@link SchemaFactory} and the class actually loading the
     * schema come from different implementations, it may not be possible
     * for {@link SchemaFactory} features to be inherited automatically.
     * Developers should
     * make sure that features, such as secure processing, are explicitly
     * set in both places.
     *
     * <h4>W3C XML Schema 1.0</h4>
     * <p>
     * For XML Schema, this method creates a {@link Schema} object that
     * performs validation by using location hints specified in documents.
     *
     * <p>
     * The returned {@link Schema} object assumes that if documents
     * refer to the same URL in the schema location hints,
     * they will always resolve to the same schema document. This
     * asusmption allows implementations to reuse parsed results of
     * schema documents so that multiple validations against the same
     * schema will run faster.
     *
     * <p>
     * Note that the use of schema location hints introduces a
     * vulnerability to denial-of-service attacks.
     *
     *
     * <h4>RELAX NG</h4>
     * <p>
     * RELAX NG does not support this operation.
     *
     * @return
     *      Always return non-null valid {@link Schema} object.
     *
     * @throws UnsupportedOperationException
     *      If this operation is not supported by the callee.
     * @throws SAXException
     *      If this operation is supported but failed for some reason.
     */
    public abstract Schema newSchema() throws SAXException;
}
