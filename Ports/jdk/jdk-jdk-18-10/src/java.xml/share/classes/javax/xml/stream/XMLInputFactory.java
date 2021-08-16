/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.stream;

import com.sun.xml.internal.stream.XMLInputFactoryImpl;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.Source;

/**
 * Defines an abstract implementation of a factory for getting streams.
 *
 * The following table defines the standard properties of this specification.
 * Each property varies in the level of support required by each implementation.
 * The level of support required is described in the 'Required' column.
 *
 *   <table class="striped">
 *    <caption>Configuration Parameters</caption>
 *    <thead>
 *      <tr>
 *        <th scope="col">Property Name</th>
 *        <th scope="col">Behavior</th>
 *        <th scope="col">Return type</th>
 *        <th scope="col">Default Value</th>
 *        <th scope="col">Required</th>
 *      </tr>
 *    </thead>
 *    <tbody>
 * <tr><th scope="row">javax.xml.stream.isValidating</th><td>Turns on/off implementation specific DTD validation</td><td>Boolean</td><td>False</td><td>No</td></tr>
 * <tr><th scope="row">javax.xml.stream.isNamespaceAware</th><td>Turns on/off namespace processing for XML 1.0 support</td><td>Boolean</td><td>True</td><td>True (required) / False (optional)</td></tr>
 * <tr><th scope="row">javax.xml.stream.isCoalescing</th><td>Requires the processor to coalesce adjacent character data</td><td>Boolean</td><td>False</td><td>Yes</td></tr>
 * <tr><th scope="row">javax.xml.stream.isReplacingEntityReferences</th><td>replace internal entity references with their replacement text and report them as characters</td><td>Boolean</td><td>True</td><td>Yes</td></tr>
 *<tr><th scope="row">javax.xml.stream.isSupportingExternalEntities</th><td>Resolve external parsed entities</td><td>Boolean</td><td>Unspecified</td><td>Yes</td></tr>
 *<tr><th scope="row">javax.xml.stream.supportDTD</th><td>Use this property to request processors that do not support DTDs</td><td>Boolean</td><td>True</td><td>Yes</td></tr>
 *<tr><th scope="row">javax.xml.stream.reporter</th><td>sets/gets the impl of the XMLReporter </td><td>javax.xml.stream.XMLReporter</td><td>Null</td><td>Yes</td></tr>
 *<tr><th scope="row">javax.xml.stream.resolver</th><td>sets/gets the impl of the XMLResolver interface</td><td>javax.xml.stream.XMLResolver</td><td>Null</td><td>Yes</td></tr>
 *<tr><th scope="row">javax.xml.stream.allocator</th><td>sets/gets the impl of the XMLEventAllocator interface</td><td>javax.xml.stream.util.XMLEventAllocator</td><td>Null</td><td>Yes</td></tr>
 *    </tbody>
 *  </table>
 *
 *
 * @version 1.2
 * @author Copyright (c) 2009, 2015 by Oracle Corporation. All Rights Reserved.
 * @see XMLOutputFactory
 * @see XMLEventReader
 * @see XMLStreamReader
 * @see EventFilter
 * @see XMLReporter
 * @see XMLResolver
 * @see javax.xml.stream.util.XMLEventAllocator
 * @since 1.6
 */

public abstract class XMLInputFactory {
  /**
   * The property used to turn on/off namespace support,
   * this is to support XML 1.0 documents,
   * only the true setting must be supported
   */
  public static final String IS_NAMESPACE_AWARE=
    "javax.xml.stream.isNamespaceAware";

  /**
   * The property used to turn on/off implementation specific validation
   */
  public static final String IS_VALIDATING=
    "javax.xml.stream.isValidating";

  /**
   * The property that requires the parser to coalesce adjacent character data sections
   */
  public static final String IS_COALESCING=
    "javax.xml.stream.isCoalescing";

  /**
   * Requires the parser to replace internal
   * entity references with their replacement
   * text and report them as characters
   */
  public static final String IS_REPLACING_ENTITY_REFERENCES=
    "javax.xml.stream.isReplacingEntityReferences";

  /**
   *  The property that requires the parser to resolve external parsed entities
   */
  public static final String IS_SUPPORTING_EXTERNAL_ENTITIES=
    "javax.xml.stream.isSupportingExternalEntities";

  /**
   *  The property that requires the parser to support DTDs
   */
  public static final String SUPPORT_DTD=
    "javax.xml.stream.supportDTD";

  /**
   * The property used to
   * set/get the implementation of the XMLReporter interface
   */
  public static final String REPORTER=
    "javax.xml.stream.reporter";

  /**
   * The property used to set/get the implementation of the XMLResolver
   */
  public static final String RESOLVER=
    "javax.xml.stream.resolver";

  /**
   * The property used to set/get the implementation of the allocator
   */
  public static final String ALLOCATOR=
    "javax.xml.stream.allocator";

  static final String DEFAULIMPL = "com.sun.xml.internal.stream.XMLInputFactoryImpl";

    /**
     * Protected constructor to prevent instantiation.
     * Use {@link #newFactory()} instead.
     */
  protected XMLInputFactory(){}

  /**
   * Creates a new instance of the {@code XMLInputFactory} builtin
   * system-default implementation.
   *
   * @return A new instance of the {@code XMLInputFactory} builtin
   *         system-default implementation.
   *
   * @since 9
   */
  public static XMLInputFactory newDefaultFactory() {
      return new XMLInputFactoryImpl();
  }

  /**
   * Creates a new instance of the factory in exactly the same manner as the
   * {@link #newFactory()} method.
   * @return an instance of the {@code XMLInputFactory}
   * @throws FactoryConfigurationError if an instance of this factory cannot be loaded
   */
  public static XMLInputFactory newInstance()
    throws FactoryConfigurationError
  {
    return FactoryFinder.find(XMLInputFactory.class, DEFAULIMPL);
  }

  /**
   * Creates a new instance of the factory. This method uses the
   * <a href="../../../module-summary.html#LookupMechanism">JAXP Lookup Mechanism</a>
   * to determine the {@code XMLInputFactory} implementation class to load.
   * <p>
   * Once an application has obtained a reference to a {@code XMLInputFactory}, it
   * can use the factory to configure and obtain stream instances.
   *
   * @return an instance of the {@code XMLInputFactory}
   * @throws FactoryConfigurationError in case of {@linkplain
   *   java.util.ServiceConfigurationError service configuration error} or if
   *   the implementation is not available or cannot be instantiated.
   */
  public static XMLInputFactory newFactory()
    throws FactoryConfigurationError
  {
    return FactoryFinder.find(XMLInputFactory.class, DEFAULIMPL);
  }

  /**
   * Create a new instance of the factory.
   *
   * @param factoryId             Name of the factory to find, same as
   *                              a property name
   * @param classLoader           classLoader to use
   * @return the factory implementation
   * @throws FactoryConfigurationError if an instance of this factory cannot be loaded
   *
   * @deprecated  This method has been deprecated to maintain API consistency.
   *              All newInstance methods have been replaced with corresponding
   *              newFactory methods. The replacement {@link
   *              #newFactory(java.lang.String, java.lang.ClassLoader)} method
   *              defines no changes in behavior.
   */
  @Deprecated(since="1.7")
  public static XMLInputFactory newInstance(String factoryId,
          ClassLoader classLoader)
          throws FactoryConfigurationError {
      //do not fallback if given classloader can't find the class, throw exception
      return FactoryFinder.find(XMLInputFactory.class, factoryId, classLoader, null);
  }

  /**
   * Create a new instance of the factory.
   * If the classLoader argument is null, then the ContextClassLoader is used.
   * <p>
   * This method uses the following ordered lookup procedure to determine
   * the XMLInputFactory implementation class to load:
   * <ul>
   * <li>
   *   <p>
   *   Use the value of the system property identified by {@code factoryId}.
   * </li>
   * <li>
   *   <p>
   *   Use the configuration file "stax.properties". The file is in standard
   *   {@link java.util.Properties} format and typically located in the
   *   {@code conf} directory of the Java installation. It contains the fully qualified
   *   name of the implementation class with the key being the system property
   *   defined above.
   *
   *   <p>
   *   The stax.properties file is read only once by the implementation
   *   and its values are then cached for future use.  If the file does not exist
   *   when the first attempt is made to read from it, no further attempts are
   *   made to check for its existence.  It is not possible to change the value
   *   of any property in stax.properties after it has been read for the first time.
   *
   *   <p>
   *   Use the jaxp configuration file "jaxp.properties". The file is in the same
   *   format as stax.properties and will only be read if stax.properties does
   *   not exist.
   * </li>
   * <li>
   *   <p>
   *   If {@code factoryId} is "javax.xml.stream.XMLInputFactory",
   *   use the service-provider loading facility, defined by the
   *   {@link java.util.ServiceLoader} class, to attempt to {@linkplain
   *   java.util.ServiceLoader#load(java.lang.Class, java.lang.ClassLoader) locate and load}
   *   an implementation of the service using the specified {@code ClassLoader}.
   *   If {@code classLoader} is null, the {@linkplain
   *   java.util.ServiceLoader#load(java.lang.Class) default loading mechanism} will apply:
   *   That is, the service-provider loading facility will use the {@linkplain
   *   java.lang.Thread#getContextClassLoader() current thread's context class loader}
   *   to attempt to load the service. If the context class
   *   loader is null, the {@linkplain
   *   ClassLoader#getSystemClassLoader() system class loader} will be used.
   * </li>
   * <li>
   *   <p>
   *   Otherwise, throws a {@link FactoryConfigurationError}.
   * </li>
   * </ul>
   *
   * <p>
   * Note that this is a new method that replaces the deprecated
   *   {@link #newInstance(java.lang.String, java.lang.ClassLoader)
   *   newInstance(String factoryId, ClassLoader classLoader)} method.
   * No changes in behavior are defined by this replacement method relative
   * to the deprecated method.
   *
   *
   * @apiNote The parameter factoryId defined here is inconsistent with that
   * of other JAXP factories where the first parameter is fully qualified
   * factory class name that provides implementation of the factory.
   *
   * @param factoryId             Name of the factory to find, same as
   *                              a property name
   * @param classLoader           classLoader to use
   * @return the factory implementation
   * @throws FactoryConfigurationError in case of {@linkplain
   *   java.util.ServiceConfigurationError service configuration error} or if
   *   the implementation is not available or cannot be instantiated.
   * @throws FactoryConfigurationError if an instance of this factory cannot be loaded
   */
  public static XMLInputFactory newFactory(String factoryId,
          ClassLoader classLoader)
          throws FactoryConfigurationError {
      //do not fallback if given classloader can't find the class, throw exception
      return FactoryFinder.find(XMLInputFactory.class, factoryId, classLoader, null);
  }

  /**
   * Create a new XMLStreamReader from a reader.
   * @param reader the XML data to read from
   * @return an instance of the {@code XMLStreamReader}
   * @throws XMLStreamException if an error occurs
   */
  public abstract XMLStreamReader createXMLStreamReader(java.io.Reader reader)
    throws XMLStreamException;

  /**
   * Create a new XMLStreamReader from a JAXP source.  This method is optional.
   * @param source the source to read from
   * @return an instance of the {@code XMLStreamReader}
   * @throws UnsupportedOperationException if this method is not
   * supported by this XMLInputFactory
   * @throws XMLStreamException if an error occurs
   */
  public abstract XMLStreamReader createXMLStreamReader(Source source)
    throws XMLStreamException;

  /**
   * Create a new XMLStreamReader from a java.io.InputStream.
   * @param stream the InputStream to read from
   * @return an instance of the {@code XMLStreamReader}
   * @throws XMLStreamException if an error occurs
   */
  public abstract XMLStreamReader createXMLStreamReader(java.io.InputStream stream)
    throws XMLStreamException;

  /**
   * Create a new XMLStreamReader from a java.io.InputStream.
   * @param stream the InputStream to read from
   * @param encoding the character encoding of the stream
   * @return an instance of the {@code XMLStreamReader}
   * @throws XMLStreamException if an error occurs
   */
  public abstract XMLStreamReader createXMLStreamReader(java.io.InputStream stream, String encoding)
    throws XMLStreamException;

  /**
   * Create a new XMLStreamReader from a java.io.InputStream.
   * @param systemId the system ID of the stream
   * @param stream the InputStream to read from
   * @return an instance of the {@code XMLStreamReader}
   * @throws XMLStreamException if an error occurs
   */
  public abstract XMLStreamReader createXMLStreamReader(String systemId, java.io.InputStream stream)
    throws XMLStreamException;

  /**
   * Create a new XMLStreamReader from a java.io.InputStream.
   * @param systemId the system ID of the stream
   * @param reader the InputStream to read from
   * @return an instance of the {@code XMLStreamReader}
   * @throws XMLStreamException if an error occurs
   */
  public abstract XMLStreamReader createXMLStreamReader(String systemId, java.io.Reader reader)
    throws XMLStreamException;

  /**
   * Create a new XMLEventReader from a reader.
   * @param reader the XML data to read from
   * @return an instance of the {@code XMLEventReader}
   * @throws XMLStreamException if an error occurs
   */
  public abstract XMLEventReader createXMLEventReader(java.io.Reader reader)
    throws XMLStreamException;

  /**
   * Create a new XMLEventReader from a reader.
   * @param systemId the system ID of the input
   * @param reader the XML data to read from
   * @return an instance of the {@code XMLEventReader}
   * @throws XMLStreamException if an error occurs
   */
  public abstract XMLEventReader createXMLEventReader(String systemId, java.io.Reader reader)
    throws XMLStreamException;

  /**
   * Create a new XMLEventReader from an XMLStreamReader.  After being used
   * to construct the XMLEventReader instance returned from this method
   * the XMLStreamReader must not be used.
   * @param reader the XMLStreamReader to read from (may not be modified)
   * @return a new XMLEventReader
   * @throws XMLStreamException if an error occurs
   */
  public abstract XMLEventReader createXMLEventReader(XMLStreamReader reader)
    throws XMLStreamException;

  /**
   * Create a new XMLEventReader from a JAXP source.
   * Support of this method is optional.
   * @param source the source to read from
   * @return an instance of the {@code XMLEventReader}
   * @throws XMLStreamException if an error occurs
   * @throws UnsupportedOperationException if this method is not
   * supported by this XMLInputFactory
   */
  public abstract XMLEventReader createXMLEventReader(Source source)
    throws XMLStreamException;

  /**
   * Create a new XMLEventReader from a java.io.InputStream
   * @param stream the InputStream to read from
   * @return an instance of the {@code XMLEventReader}
   * @throws XMLStreamException if an error occurs
   */
  public abstract XMLEventReader createXMLEventReader(java.io.InputStream stream)
    throws XMLStreamException;

  /**
   * Create a new XMLEventReader from a java.io.InputStream
   * @param stream the InputStream to read from
   * @param encoding the character encoding of the stream
   * @return an instance of the {@code XMLEventReader}
   * @throws XMLStreamException if an error occurs
   */
  public abstract XMLEventReader createXMLEventReader(java.io.InputStream stream, String encoding)
    throws XMLStreamException;

  /**
   * Create a new XMLEventReader from a java.io.InputStream
   * @param systemId the system ID of the stream
   * @param stream the InputStream to read from
   * @return an instance of the {@code XMLEventReader}
   * @throws XMLStreamException if an error occurs
   */
  public abstract XMLEventReader createXMLEventReader(String systemId, java.io.InputStream stream)
    throws XMLStreamException;

  /**
   * Create a filtered reader that wraps the filter around the reader
   * @param reader the reader to filter
   * @param filter the filter to apply to the reader
   * @return an instance of the {@code XMLEventReader}
   * @throws XMLStreamException if an error occurs
   */
  public abstract XMLStreamReader createFilteredReader(XMLStreamReader reader, StreamFilter filter)
    throws XMLStreamException;

  /**
   * Create a filtered event reader that wraps the filter around the event reader
   * @param reader the event reader to wrap
   * @param filter the filter to apply to the event reader
   * @return an instance of the {@code XMLEventReader}
   * @throws XMLStreamException if an error occurs
   */
  public abstract XMLEventReader createFilteredReader(XMLEventReader reader, EventFilter filter)
    throws XMLStreamException;

  /**
   * The resolver that will be set on any XMLStreamReader or XMLEventReader created
   * by this factory instance.
   * @return an instance of the {@code XMLResolver}
   */
  public abstract XMLResolver getXMLResolver();

  /**
   * The resolver that will be set on any XMLStreamReader or XMLEventReader created
   * by this factory instance.
   * @param resolver the resolver to use to resolve references
   */
  public abstract void  setXMLResolver(XMLResolver resolver);

  /**
   * The reporter that will be set on any XMLStreamReader or XMLEventReader created
   * by this factory instance.
   * @return an instance of the {@code XMLReporter}
   */
  public abstract XMLReporter getXMLReporter();

  /**
   * The reporter that will be set on any XMLStreamReader or XMLEventReader created
   * by this factory instance.
   * @param reporter the resolver to use to report non fatal errors
   */
  public abstract void setXMLReporter(XMLReporter reporter);

  /**
   * Allows the user to set specific feature/property on the underlying
   * implementation. The underlying implementation is not required to support
   * every setting of every property in the specification and may use
   * IllegalArgumentException to signal that an unsupported property may not be
   * set with the specified value.
   * <p>
   * All implementations that implement JAXP 1.5 or newer are required to
   * support the {@link javax.xml.XMLConstants#ACCESS_EXTERNAL_DTD} property.
   * <ul>
   *   <li>
   *        <p>
   *        Access to external DTDs, external Entity References is restricted to the
   *        protocols specified by the property. If access is denied during parsing
   *        due to the restriction of this property, {@link javax.xml.stream.XMLStreamException}
   *        will be thrown by the {@link javax.xml.stream.XMLStreamReader#next()} or
   *        {@link javax.xml.stream.XMLEventReader#nextEvent()} method.
   *
   *   </li>
   * </ul>
   * @param name The name of the property (may not be null)
   * @param value The value of the property
   * @throws java.lang.IllegalArgumentException if the property is not supported
   */
  public abstract void setProperty(java.lang.String name, Object value)
    throws java.lang.IllegalArgumentException;

  /**
   * Get the value of a feature/property from the underlying implementation
   * @param name The name of the property (may not be null)
   * @return The value of the property
   * @throws IllegalArgumentException if the property is not supported
   */
  public abstract Object getProperty(java.lang.String name)
    throws java.lang.IllegalArgumentException;


  /**
   * Query the set of properties that this factory supports.
   *
   * @param name The name of the property (may not be null)
   * @return true if the property is supported and false otherwise
   */
  public abstract boolean isPropertySupported(String name);

  /**
   * Set a user defined event allocator for events
   * @param allocator the user defined allocator
   */
  public abstract void setEventAllocator(XMLEventAllocator allocator);

  /**
   * Gets the allocator used by streams created with this factory
   * @return an instance of the {@code XMLEventAllocator}
   */
  public abstract XMLEventAllocator getEventAllocator();

}
