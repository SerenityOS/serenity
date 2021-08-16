/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * The base exception for unexpected processing errors.  This Exception
 * class is used to report well-formedness errors as well as unexpected
 * processing conditions.
 * @version 1.0
 * @author Copyright (c) 2009 by Oracle Corporation. All Rights Reserved.
 * @since 1.6
 */

public class XMLStreamException extends Exception {
  private static final long serialVersionUID = 2018819321811497362L;

  /**
   * The nested exception.
   */
  protected Throwable nested;

  /**
   * The location of the error.
   */
  protected Location location;

  /**
   * Default constructor
   */
  public XMLStreamException(){
    super();
  }

  /**
   * Construct an exception with the assocated message.
   *
   * @param msg the message to report
   */
  public XMLStreamException(String msg) {
    super(msg);
  }

  /**
   * Construct an exception with the assocated exception
   *
   * @param th a nested exception
   */
  public XMLStreamException(Throwable th) {
      super(th);
    nested = th;
  }

  /**
   * Construct an exception with the assocated message and exception
   *
   * @param th a nested exception
   * @param msg the message to report
   */
  public XMLStreamException(String msg, Throwable th) {
    super(msg, th);
    nested = th;
  }

  /**
   * Construct an exception with the assocated message, exception and location.
   *
   * @param th a nested exception
   * @param msg the message to report
   * @param location the location of the error
   */
  public XMLStreamException(String msg, Location location, Throwable th) {
    super("ParseError at [row,col]:["+location.getLineNumber()+","+
          location.getColumnNumber()+"]\n"+
          "Message: "+msg);
    nested = th;
    this.location = location;
  }

  /**
   * Construct an exception with the assocated message, exception and location.
   *
   * @param msg the message to report
   * @param location the location of the error
   */
  public XMLStreamException(String msg,
                            Location location) {
    super("ParseError at [row,col]:["+location.getLineNumber()+","+
          location.getColumnNumber()+"]\n"+
          "Message: "+msg);
    this.location = location;
  }


  /**
   * Gets the nested exception.
   *
   * @return Nested exception
   */
  public Throwable getNestedException() {
    return nested;
  }

  /**
   * Gets the location of the exception
   *
   * @return the location of the exception, may be null if none is available
   */
  public Location getLocation() {
    return location;
  }

}
