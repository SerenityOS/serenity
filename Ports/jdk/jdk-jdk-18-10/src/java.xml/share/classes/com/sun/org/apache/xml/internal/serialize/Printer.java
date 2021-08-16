/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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


// Sep 14, 2000:
//  Fixed serializer to report IO exception directly, instead at
//  the end of document processing.
//  Reported by Patrick Higgins <phiggins@transzap.com>


package com.sun.org.apache.xml.internal.serialize;


import java.io.Writer;
import java.io.StringWriter;
import java.io.IOException;


/**
 * The printer is responsible for sending text to the output stream
 * or writer. This class performs direct writing for efficiency.
 * {@link IndentPrinter} supports indentation and line wrapping by
 * extending this class.
 *
 * @author <a href="mailto:arkin@intalio.com">Assaf Arkin</a>
 *
 * @deprecated As of JDK 9, Xerces 2.9.0, Xerces DOM L3 Serializer implementation
 * is replaced by that of Xalan. Main class
 * {@link com.sun.org.apache.xml.internal.serialize.DOMSerializerImpl} is replaced
 * by {@link com.sun.org.apache.xml.internal.serializer.dom3.LSSerializerImpl}.
 */
@Deprecated
public class Printer
{


    /**
     * The output format associated with this serializer. This will never
     * be a null reference. If no format was passed to the constructor,
     * the default one for this document type will be used. The format
     * object is never changed by the serializer.
     */
    protected final OutputFormat _format;


    /**
     * The writer to which the document is written.
     */
    protected Writer             _writer;


    /**
     * The DTD writer. When we switch to DTD mode, all output is
     * accumulated in this DTD writer. When we switch out of it,
     * the output is obtained as a string. Must not be reset to
     * null until we're done with the document.
     */
    protected StringWriter       _dtdWriter;


    /**
     * Holds a reference to the document writer while we are
     * in DTD mode.
     */
    protected Writer          _docWriter;


    /**
     * Holds the exception thrown by the serializer.  Exceptions do not cause
     * the serializer to quit, but are held and one is thrown at the end.
     */
    protected IOException     _exception;


    /**
     * The size of the output buffer.
     */
    private static final int BufferSize = 4096;


    /**
     * Output buffer.
     */
    private final char[]  _buffer = new char[ BufferSize ];


    /**
     * Position within the output buffer.
     */
    private int           _pos = 0;


    public Printer( Writer writer, OutputFormat format)
    {
        _writer = writer;
        _format = format;
        _exception = null;
        _dtdWriter = null;
        _docWriter = null;
        _pos = 0;
    }


    public IOException getException()
    {
        return _exception;
    }


    /**
     * Called by any of the DTD handlers to enter DTD mode.
     * Once entered, all output will be accumulated in a string
     * that can be printed as part of the document's DTD.
     * This method may be called any number of time but will only
     * have affect the first time it's called. To exist DTD state
     * and get the accumulated DTD, call {@link #leaveDTD}.
     */
    public void enterDTD()
        throws IOException
    {
        // Can only enter DTD state once. Once we're out of DTD
        // state, can no longer re-enter it.
        if ( _dtdWriter == null ) {
            flushLine( false );

                        _dtdWriter = new StringWriter();
            _docWriter = _writer;
            _writer = _dtdWriter;
        }
    }


    /**
     * Called by the root element to leave DTD mode and if any
     * DTD parts were printer, will return a string with their
     * textual content.
     */
    public String leaveDTD()
        throws IOException
    {
        // Only works if we're going out of DTD mode.
        if ( _writer == _dtdWriter ) {
            flushLine( false );

                        _writer = _docWriter;
            return _dtdWriter.toString();
        } else
            return null;
    }


    public void printText( String text )
        throws IOException
    {
        try {
            int length = text.length();
            for ( int i = 0 ; i < length ; ++i ) {
                if ( _pos == BufferSize ) {
                    _writer.write( _buffer );
                    _pos = 0;
                }
                _buffer[ _pos ] = text.charAt( i );
                ++_pos;
            }
        } catch ( IOException except ) {
            // We don't throw an exception, but hold it
            // until the end of the document.
            if ( _exception == null )
                _exception = except;
            throw except;
        }
    }


    public void printText( StringBuffer text )
        throws IOException
    {
        try {
            int length = text.length();
            for ( int i = 0 ; i < length ; ++i ) {
                if ( _pos == BufferSize ) {
                    _writer.write( _buffer );
                    _pos = 0;
                }
                _buffer[ _pos ] = text.charAt( i );
                ++_pos;
            }
        } catch ( IOException except ) {
            // We don't throw an exception, but hold it
            // until the end of the document.
            if ( _exception == null )
                _exception = except;
            throw except;
        }
    }


    public void printText( char[] chars, int start, int length )
        throws IOException
    {
        try {
            while ( length-- > 0 ) {
                if ( _pos == BufferSize ) {
                    _writer.write( _buffer );
                    _pos = 0;
                }
                _buffer[ _pos ] = chars[ start ];
                ++start;
                ++_pos;
            }
        } catch ( IOException except ) {
            // We don't throw an exception, but hold it
            // until the end of the document.
            if ( _exception == null )
                _exception = except;
            throw except;
        }
    }


    public void printText( char ch )
        throws IOException
    {
        try {
            if ( _pos == BufferSize ) {
                _writer.write( _buffer );
                _pos = 0;
            }
            _buffer[ _pos ] = ch;
            ++_pos;
        } catch ( IOException except ) {
            // We don't throw an exception, but hold it
            // until the end of the document.
            if ( _exception == null )
                _exception = except;
            throw except;
        }
    }


    public void printSpace()
        throws IOException
    {
        try {
            if ( _pos == BufferSize ) {
                _writer.write( _buffer );
                _pos = 0;
            }
            _buffer[ _pos ] = ' ';
            ++_pos;
        } catch ( IOException except ) {
            // We don't throw an exception, but hold it
            // until the end of the document.
            if ( _exception == null )
                _exception = except;
            throw except;
        }
    }


    public void breakLine()
        throws IOException
    {
        try {
            if ( _pos == BufferSize ) {
                _writer.write( _buffer );
                _pos = 0;
            }
            _buffer[ _pos ] = '\n';
            ++_pos;
        } catch ( IOException except ) {
            // We don't throw an exception, but hold it
            // until the end of the document.
            if ( _exception == null )
                _exception = except;
            throw except;
        }
    }


    public void breakLine( boolean preserveSpace )
        throws IOException
    {
        breakLine();
    }


    public void flushLine( boolean preserveSpace )
        throws IOException
    {
        // Write anything left in the buffer into the writer.
        try {
            _writer.write( _buffer, 0, _pos );
        } catch ( IOException except ) {
            // We don't throw an exception, but hold it
            // until the end of the document.
            if ( _exception == null )
                _exception = except;
        }
        _pos = 0;
    }


    /**
     * Flush the output stream. Must be called when done printing
     * the document, otherwise some text might be buffered.
     */
    public void flush()
        throws IOException
    {
        try {
            _writer.write( _buffer, 0, _pos );
            _writer.flush();
        } catch ( IOException except ) {
            // We don't throw an exception, but hold it
            // until the end of the document.
            if ( _exception == null )
                _exception = except;
            throw except;
        }
        _pos = 0;
    }


    public void indent()
    {
        // NOOP
    }


    public void unindent()
    {
        // NOOP
    }


    public int getNextIndent()
    {
        return 0;
    }


    public void setNextIndent( int indent )
    {
    }


    public void setThisIndent( int indent )
    {
    }


}
