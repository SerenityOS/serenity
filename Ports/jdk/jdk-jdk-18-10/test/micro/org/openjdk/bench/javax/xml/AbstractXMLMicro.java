/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package org.openjdk.bench.javax.xml;

import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base class with functionality and constants used by the XML micros.
 */
@State(Scope.Benchmark)
public abstract class AbstractXMLMicro {

  public static final String BUILDIMPL = "build-impl.xml";
  public static final String LOGCOMP = "log_comp.xml";
  public static final String MESSAGE12 = "message_12.xml";
  public static final String MSGATTACH = "msgAttach.xml";
  public static final String REZ = "reZ003vExc23082309.xml";

  protected static final ConcurrentHashMap<String, byte[]> byteCache = new ConcurrentHashMap<>();

  @Param({BUILDIMPL,LOGCOMP,MESSAGE12,MSGATTACH,REZ})
  protected String doc;

  /**
   * Gets a given InputStream as a byte-array.
   *
   * @param is stream to read from
   * @return byte-array
   * @throws IOException if things go crazy-crazy
   */
  private static byte[] getBytes(InputStream is) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] b = new byte[1024];

    int available = is.available();
    while (available > 0) {
      int read = Math.min(b.length, available);

      int actuallyRead = is.read(b, 0, read);
      baos.write(b, 0, actuallyRead);

      available = is.available();
    }

    is.close();
    byte array[] = baos.toByteArray();
    baos.close();

    return array;
  }

  /**
   * Gets a given resource as a byte-array.
   *
   * @param name resource to fetch
   * @return byte-array
   * @throws IOException if things go crazy-crazy
   * @throws URISyntaxException if resource given doesn't match syntax
   */
  protected byte[] getFileBytesFromResource(String name) throws IOException, URISyntaxException {
    byte[] bytes = byteCache.get(name);
    if (bytes == null) {
      bytes = getBytes(this.getClass().getResourceAsStream("/"
              + AbstractXMLMicro.class.getPackage().getName().replace(".", "/")
              + "/" + name));
      byteCache.put(name, bytes);
    }
    return bytes;
  }

}
