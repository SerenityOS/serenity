/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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

package com.sun.org.apache.xml.internal.serializer;

import org.xml.sax.Attributes;

/**
 * This interface defines a set of integer constants that identify trace event
 * types.
 *
 * @xsl.usage internal
 */

public interface SerializerTrace {

  /**
   * Event type generated when a document begins.
   *
   */
  public static final int EVENTTYPE_STARTDOCUMENT = 1;

  /**
   * Event type generated when a document ends.
   */
  public static final int EVENTTYPE_ENDDOCUMENT = 2;

  /**
   * Event type generated when an element begins (after the attributes have been processed but before the children have been added).
   */
  public static final int EVENTTYPE_STARTELEMENT = 3;

  /**
   * Event type generated when an element ends, after it's children have been added.
   */
  public static final int EVENTTYPE_ENDELEMENT = 4;

  /**
   * Event type generated for character data (CDATA and Ignorable Whitespace have their own events).
   */
  public static final int EVENTTYPE_CHARACTERS = 5;

  /**
   * Event type generated for ignorable whitespace (I'm not sure how much this is actually called.
   */
  public static final int EVENTTYPE_IGNORABLEWHITESPACE = 6;

  /**
   * Event type generated for processing instructions.
   */
  public static final int EVENTTYPE_PI = 7;

  /**
   * Event type generated after a comment has been added.
   */
  public static final int EVENTTYPE_COMMENT = 8;

  /**
   * Event type generate after an entity ref is created.
   */
  public static final int EVENTTYPE_ENTITYREF = 9;

  /**
   * Event type generated after CDATA is generated.
   */
  public static final int EVENTTYPE_CDATA = 10;

  /**
   * Event type generated when characters might be written to an output stream,
   *  but  these characters never are. They will ultimately be written out via
   * EVENTTYPE_OUTPUT_CHARACTERS. This type is used as attributes are collected.
   * Whenever the attributes change this event type is fired. At the very end
   * however, when the attributes do not change anymore and are going to be
   * ouput to the document the real characters will be written out using the
   * EVENTTYPE_OUTPUT_CHARACTERS.
   */
  public static final int EVENTTYPE_OUTPUT_PSEUDO_CHARACTERS = 11;

  /**
   * Event type generated when characters are written to an output stream.
   */
  public static final int EVENTTYPE_OUTPUT_CHARACTERS = 12;


  /**
   * Tell if trace listeners are present.
   *
   * @return True if there are trace listeners
   */
  public boolean hasTraceListeners();

  /**
   * Fire startDocument, endDocument events.
   *
   * @param eventType One of the EVENTTYPE_XXX constants.
   */
  public void fireGenerateEvent(int eventType);

  /**
   * Fire startElement, endElement events.
   *
   * @param eventType One of the EVENTTYPE_XXX constants.
   * @param name The name of the element.
   * @param atts The SAX attribute list.
   */
  public void fireGenerateEvent(int eventType, String name, Attributes atts);

  /**
   * Fire characters, cdata events.
   *
   * @param eventType One of the EVENTTYPE_XXX constants.
   * @param ch The char array from the SAX event.
   * @param start The start offset to be used in the char array.
   * @param length The end offset to be used in the chara array.
   */
  public void fireGenerateEvent(int eventType, char ch[], int start, int length);

  /**
   * Fire processingInstruction events.
   *
   * @param eventType One of the EVENTTYPE_XXX constants.
   * @param name The name of the processing instruction.
   * @param data The processing instruction data.
   */
  public void fireGenerateEvent(int eventType, String name, String data);


  /**
   * Fire comment and entity ref events.
   *
   * @param eventType One of the EVENTTYPE_XXX constants.
   * @param data The comment or entity ref data.
   */
  public void fireGenerateEvent(int eventType, String data);

}
