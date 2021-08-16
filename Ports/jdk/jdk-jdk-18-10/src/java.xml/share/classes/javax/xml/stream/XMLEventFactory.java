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
import com.sun.xml.internal.stream.events.XMLEventFactoryImpl;
import java.util.Iterator;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.events.*;
/**
 * This interface defines a utility class for creating instances of
 * XMLEvents
 * @version 1.2
 * @author Copyright (c) 2009 by Oracle Corporation. All Rights Reserved.
 * @see javax.xml.stream.events.StartElement
 * @see javax.xml.stream.events.EndElement
 * @see javax.xml.stream.events.ProcessingInstruction
 * @see javax.xml.stream.events.Comment
 * @see javax.xml.stream.events.Characters
 * @see javax.xml.stream.events.StartDocument
 * @see javax.xml.stream.events.EndDocument
 * @see javax.xml.stream.events.DTD
 * @since 1.6
 */
public abstract class XMLEventFactory {
    static final String JAXPFACTORYID = "javax.xml.stream.XMLEventFactory";
    static final String DEFAULIMPL = "com.sun.xml.internal.stream.events.XMLEventFactoryImpl";

    /**
     * Protected constructor to prevent instantiation.
     * Use {@link #newFactory()} instead.
     */
    protected XMLEventFactory(){}

   /**
   * Creates a new instance of the {@code XMLEventFactory} builtin
   * system-default implementation.
   *
   * @return A new instance of the {@code XMLEventFactory} builtin
   *         system-default implementation.
   *
   * @since 9
   */
  public static XMLEventFactory newDefaultFactory() {
      return new XMLEventFactoryImpl();
  }

  /**
   * Creates a new instance of the factory in exactly the same manner as the
   * {@link #newFactory()} method.
   *
   * @return an instance of the {@code XMLEventFactory}
   * @throws FactoryConfigurationError if an instance of this factory cannot be loaded
   */
  public static XMLEventFactory newInstance()
    throws FactoryConfigurationError
  {
    return FactoryFinder.find(XMLEventFactory.class, DEFAULIMPL);
  }

  /**
   * Creates a new instance of the factory. This method uses the
   * <a href="../../../module-summary.html#LookupMechanism">JAXP Lookup Mechanism</a>
   * to determine the {@code XMLEventFactory} implementation class to load.
   * <p>
   * Once an application has obtained a reference to a {@code XMLEventFactory}, it
   * can use the factory to configure and obtain stream instances.
   *
   * @return an instance of the {@code XMLEventFactory}
   * @throws FactoryConfigurationError in case of {@linkplain
   *   java.util.ServiceConfigurationError service configuration error} or if
   *   the implementation is not available or cannot be instantiated.
   */
  public static XMLEventFactory newFactory()
    throws FactoryConfigurationError
  {
    return FactoryFinder.find(XMLEventFactory.class, DEFAULIMPL);
  }

  /**
   * Create a new instance of the factory
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
   *              #newFactory(java.lang.String, java.lang.ClassLoader)}
   *              method defines no changes in behavior.
   */
  @Deprecated(since="1.7")
  public static XMLEventFactory newInstance(String factoryId,
          ClassLoader classLoader)
          throws FactoryConfigurationError {
      //do not fallback if given classloader can't find the class, throw exception
      return FactoryFinder.find(XMLEventFactory.class, factoryId, classLoader, null);
  }

  /**
   * Create a new instance of the factory.
   * If the classLoader argument is null, then the ContextClassLoader is used.
   * <p>
   * This method uses the following ordered lookup procedure to determine
   * the XMLEventFactory implementation class to load:
   * <ul>
   * <li>
   *   Use the value of the system property identified by {@code factoryId}.
   * </li>
   * <li>
   *   <p>
   *   Use the configuration file "stax.properties". The file is in standard
   *   {@link java.util.Properties} format and typically located in the
   *   conf directory of the Java installation. It contains the fully qualified
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
   *   If {@code factoryId} is "javax.xml.stream.XMLEventFactory",
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
   */
  public static XMLEventFactory newFactory(String factoryId,
                                           ClassLoader classLoader)
          throws FactoryConfigurationError {
      //do not fallback if given classloader can't find the class, throw exception
      return FactoryFinder.find(XMLEventFactory.class, factoryId, classLoader, null);
  }

 /**
   * This method allows setting of the Location on each event that
   * is created by this factory.  The values are copied by value into
   * the events created by this factory.  To reset the location
   * information set the location to null.
   * @param location the location to set on each event created
   */
  public abstract void setLocation(Location location);

  /**
   * Create a new Attribute
   * @param prefix the prefix of this attribute, may not be null
   * @param namespaceURI the attribute value is set to this value, may not be null
   * @param localName the local name of the XML name of the attribute, localName cannot be null
   * @param value the attribute value to set, may not be null
   * @return the Attribute with specified values
   */
  public abstract Attribute createAttribute(String prefix, String namespaceURI, String localName, String value);

  /**
   * Create a new Attribute
   * @param localName the local name of the XML name of the attribute, localName cannot be null
   * @param value the attribute value to set, may not be null
   * @return the Attribute with specified values
   */
  public abstract Attribute createAttribute(String localName, String value);

  /**
   * Create a new Attribute
   * @param name the qualified name of the attribute, may not be null
   * @param value the attribute value to set, may not be null
   * @return the Attribute with specified values
   */
  public abstract Attribute createAttribute(QName name, String value);

  /**
   * Create a new default Namespace
   * @param namespaceURI the default namespace uri
   * @return the Namespace with the specified value
   */
  public abstract Namespace createNamespace(String namespaceURI);

  /**
   * Create a new Namespace
   * @param prefix the prefix of this namespace, may not be null
   * @param namespaceUri the attribute value is set to this value, may not be null
   * @return the Namespace with the specified values
   */
  public abstract Namespace createNamespace(String prefix, String namespaceUri);

  /**
   * Create a new StartElement.  Namespaces can be added to this StartElement
   * by passing in an Iterator that walks over a set of Namespace interfaces.
   * Attributes can be added to this StartElement by passing an iterator
   * that walks over a set of Attribute interfaces.
   *
   * @param name the qualified name of the attribute, may not be null
   * @param attributes an optional unordered set of objects that
   * implement Attribute to add to the new StartElement, may be null
   * @param namespaces an optional unordered set of objects that
   * implement Namespace to add to the new StartElement, may be null
   * @return an instance of the requested StartElement
   */
  public abstract StartElement createStartElement(QName name,
                                                  Iterator<? extends Attribute> attributes,
                                                  Iterator<? extends Namespace> namespaces);

  /**
   * Create a new StartElement.  This defaults the NamespaceContext to
   * an empty NamespaceContext.  Querying this event for its namespaces or
   * attributes will result in an empty iterator being returned.
   *
   * @param namespaceUri the uri of the QName of the new StartElement
   * @param localName the local name of the QName of the new StartElement
   * @param prefix the prefix of the QName of the new StartElement
   * @return an instance of the requested StartElement
   */
  public abstract StartElement createStartElement(String prefix,
                                                  String namespaceUri,
                                                  String localName);
  /**
   * Create a new StartElement.  Namespaces can be added to this StartElement
   * by passing in an Iterator that walks over a set of Namespace interfaces.
   * Attributes can be added to this StartElement by passing an iterator
   * that walks over a set of Attribute interfaces.
   *
   * @param namespaceUri the uri of the QName of the new StartElement
   * @param localName the local name of the QName of the new StartElement
   * @param prefix the prefix of the QName of the new StartElement
   * @param attributes an unordered set of objects that implement
   * Attribute to add to the new StartElement
   * @param namespaces an unordered set of objects that implement
   * Namespace to add to the new StartElement
   * @return an instance of the requested StartElement
   */
  public abstract StartElement createStartElement(String prefix,
                                                  String namespaceUri,
                                                  String localName,
                                                  Iterator<? extends Attribute> attributes,
                                                  Iterator<? extends Namespace> namespaces
                                                  );
  /**
   * Create a new StartElement.  Namespaces can be added to this StartElement
   * by passing in an Iterator that walks over a set of Namespace interfaces.
   * Attributes can be added to this StartElement by passing an iterator
   * that walks over a set of Attribute interfaces.
   *
   * @param namespaceUri the uri of the QName of the new StartElement
   * @param localName the local name of the QName of the new StartElement
   * @param prefix the prefix of the QName of the new StartElement
   * @param attributes an unordered set of objects that implement
   * Attribute to add to the new StartElement, may be null
   * @param namespaces an unordered set of objects that implement
   * Namespace to add to the new StartElement, may be null
   * @param context the namespace context of this element
   * @return an instance of the requested StartElement
   */
  public abstract StartElement createStartElement(String prefix,
                                                  String namespaceUri,
                                                  String localName,
                                                  Iterator<? extends Attribute> attributes,
                                                  Iterator<? extends Namespace> namespaces,
                                                  NamespaceContext context
                                                  );

  /**
   * Create a new EndElement
   * @param name the qualified name of the EndElement
   * @param namespaces an optional unordered set of objects that
   * implement Namespace that have gone out of scope, may be null
   * @return an instance of the requested EndElement
   */
  public abstract EndElement createEndElement(QName name,
                                              Iterator<? extends Namespace> namespaces);

  /**
   * Create a new EndElement
   * @param namespaceUri the uri of the QName of the new StartElement
   * @param localName the local name of the QName of the new StartElement
   * @param prefix the prefix of the QName of the new StartElement
   * @return an instance of the requested EndElement
   */
  public abstract EndElement createEndElement(String prefix,
                                              String namespaceUri,
                                              String localName);
  /**
   * Create a new EndElement
   * @param namespaceUri the uri of the QName of the new StartElement
   * @param localName the local name of the QName of the new StartElement
   * @param prefix the prefix of the QName of the new StartElement
   * @param namespaces an unordered set of objects that implement
   * Namespace that have gone out of scope, may be null
   * @return an instance of the requested EndElement
   */
  public abstract EndElement createEndElement(String prefix,
                                              String namespaceUri,
                                              String localName,
                                              Iterator<? extends Namespace> namespaces);

  /**
   * Create a Characters event, this method does not check if the content
   * is all whitespace.  To create a space event use #createSpace(String)
   * @param content the string to create
   * @return a Characters event
   */
  public abstract Characters createCharacters(String content);

  /**
   * Create a Characters event with the CData flag set to true
   * @param content the string to create
   * @return a Characters event
   */
  public abstract Characters createCData(String content);

  /**
   * Create a Characters event with the isSpace flag set to true
   * @param content the content of the space to create
   * @return a Characters event
   */
  public abstract Characters createSpace(String content);
  /**
   * Create an ignorable space
   * @param content the space to create
   * @return a Characters event
   */
  public abstract Characters createIgnorableSpace(String content);

  /**
   * Creates a new instance of a StartDocument event
   * @return a StartDocument event
   */
  public abstract StartDocument createStartDocument();

  /**
   * Creates a new instance of a StartDocument event
   *
   * @param encoding the encoding style
   * @param version the XML version
   * @param standalone the status of standalone may be set to "true" or "false"
   * @return a StartDocument event
   */
  public abstract StartDocument createStartDocument(String encoding,
                                                  String version,
                                                  boolean standalone);

  /**
   * Creates a new instance of a StartDocument event
   *
   * @param encoding the encoding style
   * @param version the XML version
   * @return a StartDocument event
   */
  public abstract StartDocument createStartDocument(String encoding,
                                                  String version);

  /**
   * Creates a new instance of a StartDocument event
   *
   * @param encoding the encoding style
   * @return a StartDocument event
   */
  public abstract StartDocument createStartDocument(String encoding);

  /**
   * Creates a new instance of an EndDocument event
   * @return an EndDocument event
   */
  public abstract EndDocument createEndDocument();

  /** Creates a new instance of a EntityReference event
   *
   * @param name The name of the reference
   * @param declaration the declaration for the event
   * @return an EntityReference event
   */
  public abstract EntityReference createEntityReference(String name,
                                                        EntityDeclaration declaration);
  /**
   * Create a comment.
   * @param text The text of the comment
   * @return a Comment event
   */
  public abstract Comment createComment(String text);

  /**
   * Create a processing instruction
   * @param target The target of the processing instruction
   * @param data The text of the processing instruction
   * @return a ProcessingInstruction event
   */
  public abstract ProcessingInstruction createProcessingInstruction(String target,
                                                                   String data);

  /**
   * Create a document type definition event
   * This string contains the entire document type declaration that matches
   * the doctypedecl in the XML 1.0 specification
   * @param dtd the text of the document type definition
   * @return a DTD event
   */
  public abstract DTD createDTD(String dtd);
}
