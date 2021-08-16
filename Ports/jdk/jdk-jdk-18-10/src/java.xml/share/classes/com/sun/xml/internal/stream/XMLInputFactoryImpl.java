/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.xml.internal.stream;

import java.io.InputStream;
import java.io.Reader;

import javax.xml.stream.*;
import javax.xml.stream.util.XMLEventAllocator ;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import com.sun.org.apache.xerces.internal.impl.XMLStreamReaderImpl;
import com.sun.org.apache.xerces.internal.impl.PropertyManager;
import com.sun.org.apache.xerces.internal.impl.XMLStreamFilterImpl;
import com.sun.org.apache.xerces.internal.impl.Constants;

/** Factory Implementation for XMLInputFactory.
 * @author Neeraj Bajaj Sun Microsystems
 * @author K.Venugopal Sun Microsystems
 */

//xxx: Should we be reusing the XMLInputSource object
public class XMLInputFactoryImpl extends javax.xml.stream.XMLInputFactory {


    //List of supported properties and default values.
    private PropertyManager fPropertyManager = new PropertyManager(PropertyManager.CONTEXT_READER) ;
    private static final boolean DEBUG = false;

    //Maintain a reference to last reader instantiated.
    private XMLStreamReaderImpl fTempReader = null ;

    boolean fPropertyChanged = false;
    //no reader reuse by default
    boolean fReuseInstance = false;

    /** Creates a new instance of ZephryParserFactory */
    public XMLInputFactoryImpl() {

    }

    void initEventReader(){
        fPropertyChanged = true;
    }

    /**
     * @param inputstream
     * @throws XMLStreamException
     * @return
     */
    public XMLEventReader createXMLEventReader(InputStream inputstream) throws XMLStreamException {
        initEventReader();
        //delegate everything to XMLStreamReader
        return new XMLEventReaderImpl(createXMLStreamReader(inputstream));
    }

    public XMLEventReader createXMLEventReader(Reader reader) throws XMLStreamException {
        initEventReader();
        //delegate everything to XMLStreamReader
        return new XMLEventReaderImpl(createXMLStreamReader(reader));
    }

    public XMLEventReader createXMLEventReader(Source source) throws XMLStreamException {
        initEventReader();
        //delegate everything to XMLStreamReader
        return new XMLEventReaderImpl(createXMLStreamReader(source));
    }

    public XMLEventReader createXMLEventReader(String systemId, InputStream inputstream) throws XMLStreamException {
        initEventReader();
        //delegate everything to XMLStreamReader
        return new XMLEventReaderImpl(createXMLStreamReader(systemId, inputstream));
    }

    public XMLEventReader createXMLEventReader(java.io.InputStream stream, String encoding) throws XMLStreamException {
        initEventReader();
        //delegate everything to XMLStreamReader
        return new XMLEventReaderImpl(createXMLStreamReader(stream, encoding));
    }

    public XMLEventReader createXMLEventReader(String systemId, Reader reader) throws XMLStreamException {
        initEventReader();
        //delegate everything to XMLStreamReader
        return new XMLEventReaderImpl(createXMLStreamReader(systemId, reader));
    }

    /** Create a new XMLEventReader from an XMLStreamReader.  After being used
     * to construct the XMLEventReader instance returned from this method
     * the XMLStreamReader must not be used.
     * @param reader the XMLStreamReader to read from (may not be modified)
     * @return a new XMLEventReader
     * @throws XMLStreamException
     */
    public XMLEventReader createXMLEventReader(XMLStreamReader reader) throws XMLStreamException {

        //xxx: what do we do now -- instance is passed from the application
        //probably we should check if the state is at the start document,
        //eventreader call to next() should return START_DOCUMENT and
        //then delegate every call to underlying streamReader
        return new XMLEventReaderImpl(reader) ;
    }

    public XMLStreamReader createXMLStreamReader(InputStream inputstream) throws XMLStreamException {
        XMLInputSource inputSource = new XMLInputSource(null, null, null, inputstream, null);
        return getXMLStreamReaderImpl(inputSource);
    }

    public XMLStreamReader createXMLStreamReader(Reader reader) throws XMLStreamException {
        XMLInputSource inputSource = new XMLInputSource(null, null, null, reader, null);
        return getXMLStreamReaderImpl(inputSource);
    }

    public XMLStreamReader createXMLStreamReader(String systemId, Reader reader) throws XMLStreamException {
        XMLInputSource inputSource = new XMLInputSource(null,systemId,null,reader,null);
        return getXMLStreamReaderImpl(inputSource);
    }

    public XMLStreamReader createXMLStreamReader(Source source) throws XMLStreamException {
        return new XMLStreamReaderImpl(jaxpSourcetoXMLInputSource(source),
                new PropertyManager(fPropertyManager));
    }

    public XMLStreamReader createXMLStreamReader(String systemId, InputStream inputstream) throws XMLStreamException {
        XMLInputSource inputSource = new XMLInputSource(null,systemId,null,inputstream,null);
        return getXMLStreamReaderImpl(inputSource);
    }


    public XMLStreamReader createXMLStreamReader(InputStream inputstream, String encoding) throws XMLStreamException {
        XMLInputSource inputSource = new XMLInputSource(null,null,null,inputstream,encoding);
        return getXMLStreamReaderImpl(inputSource);
    }

    public XMLEventAllocator getEventAllocator() {
        return (XMLEventAllocator)getProperty(XMLInputFactory.ALLOCATOR);
    }

    public XMLReporter getXMLReporter() {
        return (XMLReporter)fPropertyManager.getProperty(XMLInputFactory.REPORTER);
    }

    public XMLResolver getXMLResolver() {
        Object object = fPropertyManager.getProperty(XMLInputFactory.RESOLVER);
        return (XMLResolver)object;
        //return (XMLResolver)fPropertyManager.getProperty(XMLInputFactory.RESOLVER);
    }

    public void setXMLReporter(XMLReporter xmlreporter) {
        fPropertyManager.setProperty(XMLInputFactory.REPORTER, xmlreporter);
    }

    public void setXMLResolver(XMLResolver xmlresolver) {
        fPropertyManager.setProperty(XMLInputFactory.RESOLVER, xmlresolver);
    }

    /** Create a filtered event reader that wraps the filter around the event reader
     * @param reader the event reader to wrap
     * @param filter the filter to apply to the event reader
     * @throws XMLStreamException
     */
    public XMLEventReader createFilteredReader(XMLEventReader reader, EventFilter filter) throws XMLStreamException {
        return new EventFilterSupport(reader, filter);
    }

    /** Create a filtered reader that wraps the filter around the reader
     * @param reader the reader to filter
     * @param filter the filter to apply to the reader
     * @throws XMLStreamException
     */
    public XMLStreamReader createFilteredReader(XMLStreamReader reader, StreamFilter filter) throws XMLStreamException {
        if( reader != null && filter != null )
            return new XMLStreamFilterImpl(reader,filter);

        return null;
    }



    /** Get the value of a feature/property from the underlying implementation
     * @param name The name of the property (may not be null)
     * @return The value of the property
     * @throws IllegalArgumentException if the property is not supported
     */
    public Object getProperty(java.lang.String name) throws java.lang.IllegalArgumentException {
        if(name == null){
            throw new IllegalArgumentException("Property not supported");
        }
        if(fPropertyManager.containsProperty(name))
            return fPropertyManager.getProperty(name);
        throw new IllegalArgumentException("Property not supported");
    }

    /** Query the set of fProperties that this factory supports.
     *
     * @param name The name of the property (may not be null)
     * @return true if the property is supported and false otherwise
     */
    public boolean isPropertySupported(String name) {
        if(name == null)
            return false ;
        else
            return fPropertyManager.containsProperty(name);
    }

    /** Set a user defined event allocator for events
     * @param allocator the user defined allocator
     */
    public void setEventAllocator(XMLEventAllocator allocator) {
        fPropertyManager.setProperty(XMLInputFactory.ALLOCATOR, allocator);
    }

    /** Allows the user to set specific feature/property on the underlying implementation. The underlying implementation
     * is not required to support every setting of every property in the specification and may use IllegalArgumentException
     * to signal that an unsupported property may not be set with the specified value.
     * @param name The name of the property (may not be null)
     * @param value The value of the property
     * @throws java.lang.IllegalArgumentException if the property is not supported
     */
    public void setProperty(java.lang.String name, Object value) throws java.lang.IllegalArgumentException {

        if(name == null || value == null || !fPropertyManager.containsProperty(name) ){
            throw new IllegalArgumentException("Property "+name+" is not supported");
        }
        if(name == Constants.REUSE_INSTANCE || name.equals(Constants.REUSE_INSTANCE)){
            fReuseInstance = ((Boolean)value).booleanValue();
            if(DEBUG)System.out.println("fReuseInstance is set to " + fReuseInstance);
        }else{//for any other property set the flag
            //REVISIT: Even in this case instance can be reused, by passing PropertyManager
            fPropertyChanged = true;
        }
        fPropertyManager.setProperty(name,value);
    }

    XMLStreamReader getXMLStreamReaderImpl(XMLInputSource inputSource) throws javax.xml.stream.XMLStreamException{
        //1. if the temp reader is null -- create the instance and return
        if(fTempReader == null){
            fPropertyChanged = false;
            return fTempReader = new XMLStreamReaderImpl(inputSource,
                    new PropertyManager(fPropertyManager));
        }
        //if factory is configured to reuse the instance & this instance can be reused
        //& the setProperty() hasn't been called
        if(fReuseInstance && fTempReader.canReuse() && !fPropertyChanged){
            if(DEBUG)System.out.println("Reusing the instance");
            //we can make setInputSource() call reset() and this way there wont be two function calls
            fTempReader.reset();
            fTempReader.setInputSource(inputSource);
            fPropertyChanged = false;
            return fTempReader;
        }else{
            fPropertyChanged = false;
            //just return the new instance.. note that we are not setting  fTempReader to the newly created instance
            return fTempReader = new XMLStreamReaderImpl(inputSource,
                    new PropertyManager(fPropertyManager));
        }
    }

    XMLInputSource jaxpSourcetoXMLInputSource(Source source){
         if(source instanceof StreamSource){
             StreamSource stSource = (StreamSource)source;
             String systemId = stSource.getSystemId();
             String publicId = stSource.getPublicId();
             InputStream istream = stSource.getInputStream();
             Reader reader = stSource.getReader();

             if(istream != null){
                 return new XMLInputSource(publicId, systemId, null, istream, null);
             }
             else if(reader != null){
                 return new XMLInputSource(publicId, systemId,null, reader, null);
             }else{
                 return new XMLInputSource(publicId, systemId, null, false);
             }
         }

         throw new UnsupportedOperationException("Cannot create " +
                "XMLStreamReader or XMLEventReader from a " +
                source.getClass().getName());
    }

}//XMLInputFactoryImpl
