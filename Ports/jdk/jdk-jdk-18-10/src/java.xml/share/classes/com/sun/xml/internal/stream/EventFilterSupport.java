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

package com.sun.xml.internal.stream;

import java.util.NoSuchElementException;
import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

/**
 *
 * @author  Neeraj Bajaj, Sun Microsystems
 *
 */
public class EventFilterSupport extends EventReaderDelegate {

    //maintain a reference to EventFilter
    EventFilter fEventFilter ;
    /** Creates a new instance of EventFilterSupport */
    public EventFilterSupport(XMLEventReader eventReader, EventFilter eventFilter) {
        setParent(eventReader);
        fEventFilter = eventFilter;
    }

    public Object next(){
        try{
            return nextEvent();
        }catch(XMLStreamException ex){
            throw new NoSuchElementException();
        }
    }

    public boolean hasNext(){
        try{
            return peek() != null ? true : false ;
        }catch(XMLStreamException ex){
            return false;
        }
    }

    public XMLEvent nextEvent()throws XMLStreamException{
        while (super.hasNext()) {
            //get the next event by calling XMLEventReader
            XMLEvent event = super.nextEvent();

            //if this filter accepts this event then return this event
            if(fEventFilter.accept(event)){
                return event;
            }
        }
        throw new NoSuchElementException();
    }//nextEvent()

     public XMLEvent nextTag() throws XMLStreamException{
         while (super.hasNext()) {
             XMLEvent event = super.nextTag();
             //if the filter accepts this event return this event.
             if(fEventFilter.accept(event)){
                return event;
             }
         }
         throw new NoSuchElementException();
     }

     public XMLEvent peek() throws XMLStreamException{
         while (true) {
             XMLEvent event = super.peek();
             if(event == null)return null;
             //if the filter accepts this event return this event.
             if(fEventFilter.accept(event)){
                return event;
             }
             //call super.next(), and then peek again.
             super.next();
         }
     }

}//EventFilterSupport
