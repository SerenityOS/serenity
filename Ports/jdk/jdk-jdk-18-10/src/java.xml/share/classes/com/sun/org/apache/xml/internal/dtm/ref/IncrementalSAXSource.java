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

package com.sun.org.apache.xml.internal.dtm.ref;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** <p>IncrementalSAXSource is an API that delivers a small number of
 * SAX events each time a request is made from a "controller"
 * coroutine.  See IncrementalSAXFilter and IncrementalSAXFilter_Xerces
 * for examples.
 *
 * Note that interaction is via the deliverMoreNodes
 * method, and therefore coroutine support is not exposed
 * here.</p>
 * */
public interface IncrementalSAXSource
{
  // ------------------------------------------------------------------
  // SAX Output API
  // ------------------------------------------------------------------

  /** Register a SAX-style content handler for us to output to
   */
  public void setContentHandler(ContentHandler handler);

  /**  Register a SAX-style lexical handler for us to output to
   */
  public void setLexicalHandler(org.xml.sax.ext.LexicalHandler handler);

  /**  Register a SAX-style DTD handler for us to output to
   */
  public void setDTDHandler(org.xml.sax.DTDHandler handler);

  // ------------------------------------------------------------------
  // Command Input API
  // ------------------------------------------------------------------

  /** deliverMoreNodes() is a simple API which tells the thread in which the
   * IncrementalSAXSource is running to deliver more events (true),
   * or stop delivering events and close out its input (false).
   *
   * This is intended to be called from one of our partner coroutines,
   * and serves to encapsulate the coroutine communication protocol.
   *
   * @param parsemore If true, tells the incremental SAX stream to deliver
   * another chunk of events. If false, finishes out the stream.
   *
   * @return Boolean.TRUE if the IncrementalSAXSource believes more data
   * may be available for further parsing. Boolean.FALSE if parsing
   * ran to completion, or was ended by deliverMoreNodes(false).
   * */
  public Object deliverMoreNodes (boolean parsemore);

  // ------------------------------------------------------------------
  // Parse Thread Convenience API
  // ------------------------------------------------------------------

  /** Launch an XMLReader's parsing operation, feeding events to this
   * IncrementalSAXSource. In some implementations, this may launch a
   * thread which runs the previously supplied XMLReader's parse() operation.
   * In others, it may do other forms of initialization.
   *
   * @throws SAXException is parse thread is already in progress
   * or parsing can not be started.
   * */
  public void startParse(InputSource source) throws SAXException;

} // class IncrementalSAXSource
