/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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



import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLStreamException;
import com.sun.org.apache.xerces.internal.impl.msg.XMLMessageFormatter;
import com.sun.org.apache.xerces.internal.util.MessageFormatter;
import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.XNIException;

import com.sun.org.apache.xerces.internal.impl.PropertyManager;
import com.sun.org.apache.xerces.internal.impl.XMLErrorReporter;

/**
 *
 * @author  neeraj
 */

public class StaxErrorReporter extends XMLErrorReporter {

    protected XMLReporter fXMLReporter = null ;

    /** Creates a new instance of StaxErrorReporter */
    public StaxErrorReporter(PropertyManager propertyManager) {
        super();
        putMessageFormatter(XMLMessageFormatter.XML_DOMAIN, new XMLMessageFormatter());
        reset(propertyManager);
    }

    /** Creates a new instance of StaxErrorReporter
     * If this constructor is used to create the object, one must invoke reset() on this object.
     */
    public StaxErrorReporter() {
        super();
        putMessageFormatter(XMLMessageFormatter.XML_DOMAIN, new XMLMessageFormatter());
    }

    /**
     *One must call reset before using any of the function.
     */
    public void reset(PropertyManager propertyManager){
        fXMLReporter = (XMLReporter)propertyManager.getProperty(XMLInputFactory.REPORTER);
    }
    /**
     * Reports an error at a specific location.
     *
     * @param location  The error location.
     * @param domain    The error domain.
     * @param key       The key of the error message.
     * @param arguments The replacement arguments for the error message,
     *                  if needed.
     * @param severity  The severity of the error.
     *
     * @see #SEVERITY_WARNING
     * @see #SEVERITY_ERROR
     * @see #SEVERITY_FATAL_ERROR
     */
    public String reportError(XMLLocator location,
    String domain, String key, Object[] arguments,
    short severity) throws XNIException {
        // format error message and create parse exception
        MessageFormatter messageFormatter = getMessageFormatter(domain);
        String message;
        if (messageFormatter != null) {
            message = messageFormatter.formatMessage(fLocale, key, arguments);
        }
        else {
            StringBuffer str = new StringBuffer();
            str.append(domain);
            str.append('#');
            str.append(key);
            int argCount = arguments != null ? arguments.length : 0;
            if (argCount > 0) {
                str.append('?');
                for (int i = 0; i < argCount; i++) {
                    str.append(arguments[i]);
                    if (i < argCount -1) {
                        str.append('&');
                    }
                }
            }
            message = str.toString();
        }



        //no reporter was specified
        /**
         * if (reporter == null) {
         * reporter = new DefaultStaxErrorReporter();
         * }
         */

        // call error handler
        switch (severity) {
            case SEVERITY_WARNING: {
                try{
                    if(fXMLReporter!= null){
                        fXMLReporter.report(message, "WARNING", null, convertToStaxLocation(location) );
                    }
                }catch(XMLStreamException ex){
                    //what we should be doing ?? if the user throws XMLStreamException
                    //REVISIT:
                    throw new XNIException(ex);
                }
                break;
            }
            case SEVERITY_ERROR: {
                try{
                    if(fXMLReporter!= null){
                        fXMLReporter.report(message, "ERROR", null, convertToStaxLocation(location) );
                    }
                }catch(XMLStreamException ex){
                    //what we should be doing ?? if the user throws XMLStreamException
                    //REVISIT:
                    throw new XNIException(ex);
                }
                break;
            }
            case SEVERITY_FATAL_ERROR: {
                if (!fContinueAfterFatalError) {
                    throw new XNIException(message);
                }
                break;
            }
        }
        return message;
    }


    Location convertToStaxLocation(final XMLLocator location){
        return new Location(){
            public int getColumnNumber(){
                return location.getColumnNumber();
            }

            public int getLineNumber(){
                return location.getLineNumber();
            }

            public String getPublicId(){
                return location.getPublicId();
            }

            public String getSystemId(){
                return location.getLiteralSystemId();
            }

            public int getCharacterOffset(){
                return location.getCharacterOffset();
            }
            public String getLocationURI(){
                return "";
            }

        };
    }

}
