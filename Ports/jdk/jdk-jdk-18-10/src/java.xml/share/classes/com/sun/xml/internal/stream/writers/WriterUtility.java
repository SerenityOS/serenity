/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.xml.internal.stream.writers;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import jdk.xml.internal.SecuritySupport;

/**
 * Implements common xml writer functions.
 *
 * @author Neeraj Bajaj,K.Venugopal Sun Microsystems.
 */

public class WriterUtility {


    public static final String START_COMMENT = "<!--";
    public static final String END_COMMENT = "-->";
    public static final String DEFAULT_ENCODING = " encoding=\"utf-8\"";
    public static final String DEFAULT_XMLDECL ="<?xml version=\"1.0\" ?>";
    public static final String DEFAULT_XML_VERSION ="1.0";
    public static final char CLOSE_START_TAG = '>';
    public static final char OPEN_START_TAG = '<';
    public static final String OPEN_END_TAG ="</";
    public static final char CLOSE_END_TAG = '>';
    public static final String START_CDATA = "<![CDATA[";
    public static final String END_CDATA = "]]>";
    public static final String CLOSE_EMPTY_ELEMENT = "/>";
    public static final String SPACE = " ";
    public static final String UTF_8 = "utf-8";

    static final boolean DEBUG_XML_CONTENT = false;

    /**XXX: This feature is only used when writing element content values.
     * default value is 'true' however, if the feature is set to false
     * characters wont be escaped.
     * This feature has no effect when writing Attribute values, character would still be escaped.
     * I can't think of any reason why this would be useful when writing attribute values.
     * However, this can be reconsidered if there is any usecase.
     */
    boolean fEscapeCharacters = true ;

    /** Writer object*/
    Writer fWriter = null;

    //CharsetEncoder
    CharsetEncoder fEncoder ;

    public WriterUtility(){
        fEncoder = getDefaultEncoder();
    }


    /** Creates a new instance of WriterUtility */
    public WriterUtility(Writer writer) {
        fWriter = writer;
        if(writer instanceof OutputStreamWriter){
            String charset = ((OutputStreamWriter)writer).getEncoding();
            if(charset != null){
                fEncoder = Charset.forName(charset).newEncoder();
            }
        }else if(writer instanceof FileWriter){
            String charset = ((FileWriter)writer).getEncoding();
            if(charset != null){
                fEncoder = Charset.forName(charset).newEncoder();
            }
        }
        else{
            //attempt to retreive default fEncoderoder
            fEncoder = getDefaultEncoder();
        }
    }

    /**
     * sets the writer object
     * @param writer file to write into
     */
    public void  setWriter(Writer writer){
        fWriter = writer;
    }

    public void setEscapeCharacters(boolean escape){
        fEscapeCharacters = escape ;
    }

    public boolean getEscapeCharacters(){
        return fEscapeCharacters;
    }

    /**
     * writes xml content (characters and element content
     * @param content
     */
    public void writeXMLContent(char[] content, int start, int length) throws IOException{
        writeXMLContent(content, start, length, getEscapeCharacters());
    }

    /**
     * writes xml content (characters and element content
     * @param content
     */
    private void writeXMLContent(char[] content, int start, int length, boolean escapeCharacter) throws IOException{
        if(DEBUG_XML_CONTENT){
            System.out.println("content to write is " + new String(content, start, length));
        }
        int index;
        char ch;
        int sc;
        final int end = start + length ;
        //define startWritePos to track the position from where the character array data needs to be written
        //initialize this variable to start pos. indicating that no data has been written
        int startWritePos = start;

        for ( index = start ; index < end ; index++ ) {
            ch = content[ index ];

            if(fEncoder != null && !fEncoder.canEncode(ch)){
                //- write the data to the point we get this character
                fWriter.write(content, startWritePos, index - startWritePos );

                //escape this character
                fWriter.write( "&#x" );
                fWriter.write(Integer.toHexString(ch));
                fWriter.write( ';' );
                //increase the startWritePos by 1 indicating that next write should start from
                //one position ahead
                startWritePos = index + 1;

            }
            if(DEBUG_XML_CONTENT){
                System.out.println("startWritePos = " + startWritePos);
                System.out.println("index = " + index);
                System.out.println("start = " + start);
                System.out.println("end = " + end);
            }

            switch(ch){
                case '<' :{
                    if(escapeCharacter){
                        //this character needs to be escaped, write the data from the last write pos
                        fWriter.write(content, startWritePos, index - startWritePos);
                        fWriter.write("&lt;");
                        if(DEBUG_XML_CONTENT){
                            System.out.print(new String(content, startWritePos, index - startWritePos));
                            System.out.println("&lt;");
                        }
                        //increase the startWritePos by 1 indicating that next write should start from
                        //one position ahead
                        startWritePos = index + 1;
                    }
                    break;
                }
                case '&' :{
                    if(escapeCharacter){
                        //this character needs to be escaped, write the data from the last write pos
                        fWriter.write(content, startWritePos, index - startWritePos);
                        fWriter.write("&amp;");
                        if(DEBUG_XML_CONTENT){
                            System.out.print(new String(content,startWritePos, index - startWritePos));
                            System.out.println("&amp;");
                        }
                        //increase the startWritePos by 1 indicating that next write should start from
                        //one position ahead
                        startWritePos = index + 1;
                    }
                    break;
                }

                case '>': {
                    if(escapeCharacter){
                        //this character needs to be escaped, write the data from the last write pos
                        fWriter.write(content, startWritePos, index - startWritePos);
                        fWriter.write("&gt;");
                        if(DEBUG_XML_CONTENT){
                            System.out.print(new String(content,startWritePos, index - startWritePos));
                            System.out.println("&gt;");
                        }
                        //increase the startWritePos by 1 indicating that next write should start from
                        //one position ahead
                        startWritePos = index + 1;
                    }
                    break;
                }
            }
        }
        if(DEBUG_XML_CONTENT){
            System.out.println("out of the loop, writing " + new String(content, startWritePos, end - startWritePos));
        }
        //write any pending data
        fWriter.write(content, startWritePos, end - startWritePos);
    }

    /**
     * writes xml content (characters and element content
     * @param content
     */
    public void writeXMLContent(String content) throws IOException{
        if(content == null || content.length() == 0) return ;
        writeXMLContent(content.toCharArray(), 0, content.length());
    }


    /**
     * Write Attribute value to the underlying stream.
     *
     * @param value
     */

    public void  writeXMLAttributeValue(String value)throws IOException{
        writeXMLContent(value.toCharArray(), 0, value.length(), true);
    }

    private CharsetEncoder getDefaultEncoder(){
        try{
            String encoding = SecuritySupport.getSystemProperty("file.encoding");
            if(encoding != null){
                return Charset.forName(encoding).newEncoder();
            }
        }
        catch(Exception ex){
            //for any exception thrown , catch and continue
        }
        return null;
    }
}
