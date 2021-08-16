/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.sun.xml.internal.stream;

import java.io.InputStream;
import java.io.Reader;
import java.io.IOException;

import com.sun.xml.internal.stream.util.BufferAllocator;
import com.sun.xml.internal.stream.util.ThreadLocalBufferAllocator;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;

/**
 * Entity information.
 *
 * @author
 */
public abstract class Entity {

    //
    // Data
    //

    //xxx why dont we declare the type of entities, like assign integer for external/ internal etc..

    /** Entity name. */
    public String name;

    // whether this entity's declaration was found in the internal
    // or external subset
    public boolean inExternalSubset;

    //
    // Constructors
    //

    /** Default constructor. */
    public Entity() {
        clear();
    } // <init>()

    /** Constructs an entity. */
    public Entity(String name, boolean inExternalSubset) {
        this.name = name;
        this.inExternalSubset = inExternalSubset;
    } // <init>(String)

    //
    // Public methods
    //

    /** Returns true if this entity was declared in the external subset. */
    public boolean isEntityDeclInExternalSubset() {
        return inExternalSubset;
    }

    /** Returns true if this is an external entity. */
    public abstract boolean isExternal();

    /** Returns true if this is an unparsed entity. */
    public abstract boolean isUnparsed();

    /** Clears the entity. */
    public void clear() {
        name = null;
        inExternalSubset = false;
    } // clear()

    /** Sets the values of the entity. */
    public void setValues(Entity entity) {
        name = entity.name;
        inExternalSubset = entity.inExternalSubset;
    } // setValues(Entity)


    /**
     * Internal entity.
     *
     * @author nb131165
     */
    public static class InternalEntity
            extends Entity {

        //
        // Data
        //

        /** Text value of entity. */
        public String text;

        //
        // Constructors
        //

        /** Default constructor. */
        public InternalEntity() {
            clear();
        } // <init>()

        /** Constructs an internal entity. */
        public InternalEntity(String name, String text, boolean inExternalSubset) {
            super(name,inExternalSubset);
            this.text = text;
        } // <init>(String,String)

        //
        // Entity methods
        //

        /** Returns true if this is an external entity. */
        public final boolean isExternal() {
            return false;
        } // isExternal():boolean

        /** Returns true if this is an unparsed entity. */
        public final boolean isUnparsed() {
            return false;
        } // isUnparsed():boolean

        /** Clears the entity. */
        public void clear() {
            super.clear();
            text = null;
        } // clear()

        /** Sets the values of the entity. */
        public void setValues(Entity entity) {
            super.setValues(entity);
            text = null;
        } // setValues(Entity)

        /** Sets the values of the entity. */
        public void setValues(InternalEntity entity) {
            super.setValues(entity);
            text = entity.text;
        } // setValues(InternalEntity)

    } // class InternalEntity

    /**
     * External entity.
     *
     * @author nb131165
     */
    public  static class ExternalEntity
            extends Entity {

        //
        // Data
        //

        /** container for all relevant entity location information. */
        public XMLResourceIdentifier entityLocation;

        /** Notation name for unparsed entity. */
        public String notation;

        //
        // Constructors
        //

        /** Default constructor. */
        public ExternalEntity() {
            clear();
        } // <init>()

        /** Constructs an internal entity. */
        public ExternalEntity(String name, XMLResourceIdentifier entityLocation,
                String notation, boolean inExternalSubset) {
            super(name,inExternalSubset);
            this.entityLocation = entityLocation;
            this.notation = notation;
        } // <init>(String,XMLResourceIdentifier, String)

        //
        // Entity methods
        //

        /** Returns true if this is an external entity. */
        public final boolean isExternal() {
            return true;
        } // isExternal():boolean

        /** Returns true if this is an unparsed entity. */
        public final boolean isUnparsed() {
            return notation != null;
        } // isUnparsed():boolean

        /** Clears the entity. */
        public void clear() {
            super.clear();
            entityLocation = null;
            notation = null;
        } // clear()

        /** Sets the values of the entity. */
        public void setValues(Entity entity) {
            super.setValues(entity);
            entityLocation = null;
            notation = null;
        } // setValues(Entity)

        /** Sets the values of the entity. */
        public void setValues(ExternalEntity entity) {
            super.setValues(entity);
            entityLocation = entity.entityLocation;
            notation = entity.notation;
        } // setValues(ExternalEntity)

    } // class ExternalEntity

    /**
     * Entity state.
     *
     * @author nb131165
     */
    public static class ScannedEntity
            extends Entity {


        /** Default buffer size (4096). */
        public static final int DEFAULT_BUFFER_SIZE = 8192;
        //4096;

        /**
         * Buffer size. We get this value from a property. The default size
         * is used if the input buffer size property is not specified.
         * REVISIT: do we need a property for internal entity buffer size?
         */
        public int fBufferSize = DEFAULT_BUFFER_SIZE;

        /** Default buffer size before we've finished with the XMLDecl:  */
        public static final int DEFAULT_XMLDECL_BUFFER_SIZE = 28;

        /** Default internal entity buffer size (1024). */
        public static final int DEFAULT_INTERNAL_BUFFER_SIZE = 1024;

        //
        // Data
        //

        // i/o

        /** XXX let these field remain public right now, though we have defined methods for them.
         * Input stream. */
        public InputStream stream;

        /** XXX let these field remain public right now, though we have defined methods for them.
         * Reader. */
        public Reader reader;

        // locator information

        /** entity location information */
        public XMLResourceIdentifier entityLocation;

        // encoding

        /** Auto-detected encoding. */
        public String encoding;

        // status

        /** True if in a literal.  */
        public boolean literal;

        // whether this is an external or internal scanned entity
        public boolean isExternal;

        //each 'external' parsed entity may have xml/text declaration containing version information
        public String  version ;

        // buffer

        /** Character buffer. */
        public char[] ch = null;

        /** Position in character buffer at any point of time. */
        public int position;

        /** Count of characters present in buffer. */
        public int count;

        /** Line number. */
        public int lineNumber = 1;

        /** Column number. */
        public int columnNumber = 1;

        /** Encoding has been set externally for eg: using DOMInput*/
        boolean declaredEncoding = false;

        // status

        /**
         * Encoding has been set externally, for example
         * using a SAX InputSource or a DOM LSInput.
         */
        boolean externallySpecifiedEncoding = false;

        /** XML version. **/
        public String xmlVersion = "1.0";

        /** This variable is used to calculate the current position in the XML stream.
         * Note that fCurrentEntity.position maintains the position relative to
         * the buffer.
         *  At any point of time absolute position in the XML stream can be calculated
         *  as fTotalCountTillLastLoad + fCurrentEntity.position
         */
        public int fTotalCountTillLastLoad ;

        /** This variable stores the number of characters read during the load()
         * operation. It is used to calculate fTotalCountTillLastLoad
         */
        public  int fLastCount ;

        /** Base character offset for computing absolute character offset. */
        public int baseCharOffset;

        /** Start position in character buffer. */
        public int startPosition;

        // to allow the reader/inputStream to behave efficiently:
        public boolean mayReadChunks;

        // to know that prolog is read
        public boolean xmlDeclChunkRead = false;

        // flag to indicate whether the Entity is a General Entity
        public boolean isGE = false;

        /** returns the name of the current encoding
         *  @return current encoding name
         */
        public String getEncodingName(){
            return encoding ;
        }

        /**each 'external' parsed entity may have xml/text declaration containing version information
         * @return String version of the entity, for an internal entity version would be null
         */
        public String getEntityVersion(){
            return version ;
        }

        /** each 'external' parsed entity may have xml/text declaration containing version information
         * @param String version of the external parsed entity
         */
        public void setEntityVersion(String version){
            this.version = version ;
        }

        /**  Returns the java.io.Reader associated with this entity.Readers are used
         * to read from the file. Readers wrap any particular  InputStream that was
         * used to open the entity.
         * @return java.io.Reader Reader associated with this entity
         */
        public Reader getEntityReader(){
            return reader;
        }


        /** if entity was opened using the stream, return the associated inputstream
         * with this entity
         *@return java.io.InputStream InputStream associated with this entity
         */
        public InputStream getEntityInputStream(){
            return stream;
        }

        //
        // Constructors
        //

        /** Constructs a scanned entity. */
        public ScannedEntity(boolean isGE, String name,
                XMLResourceIdentifier entityLocation,
                InputStream stream, Reader reader,
                String encoding, boolean literal, boolean mayReadChunks, boolean isExternal) {
            this.isGE = isGE;
            this.name = name ;
            this.entityLocation = entityLocation;
            this.stream = stream;
            this.reader = reader;
            this.encoding = encoding;
            this.literal = literal;
            this.mayReadChunks = mayReadChunks;
            this.isExternal = isExternal;
            final int size = isExternal ? fBufferSize : DEFAULT_INTERNAL_BUFFER_SIZE;
            BufferAllocator ba = ThreadLocalBufferAllocator.getBufferAllocator();
            ch = ba.getCharBuffer(size);
            if (ch == null) {
                this.ch = new char[size];
            }
        } // <init>(StringXMLResourceIdentifier,InputStream,Reader,String,boolean, boolean)

        /**
         * Release any resources associated with this entity.
         */
        public void close() throws IOException {
            BufferAllocator ba = ThreadLocalBufferAllocator.getBufferAllocator();
            ba.returnCharBuffer(ch);
            ch = null;
            reader.close();
        }

        //
        // Entity methods
        //

        /** Returns whether the encoding of this entity was externally specified. **/
        public boolean isEncodingExternallySpecified() {
            return externallySpecifiedEncoding;
        }

        /** Sets whether the encoding of this entity was externally specified. **/
        public void setEncodingExternallySpecified(boolean value) {
            externallySpecifiedEncoding = value;
        }

        public boolean isDeclaredEncoding() {
            return declaredEncoding;
        }

        public void setDeclaredEncoding(boolean value) {
            declaredEncoding = value;
        }

        /** Returns true if this is an external entity. */
        public final boolean isExternal() {
            return isExternal;
        } // isExternal():boolean

        /** Returns true if this is an unparsed entity. */
        public final boolean isUnparsed() {
            return false;
        } // isUnparsed():boolean

        //
        // Object methods
        //

        /** Returns a string representation of this object. */
        public String toString() {

            StringBuffer str = new StringBuffer();
            str.append("name=\""+name+'"');
            str.append(",ch="+ new String(ch));
            str.append(",position="+position);
            str.append(",count="+count);
            return str.toString();

        } // toString():String

    } // class ScannedEntity

} // class Entity
