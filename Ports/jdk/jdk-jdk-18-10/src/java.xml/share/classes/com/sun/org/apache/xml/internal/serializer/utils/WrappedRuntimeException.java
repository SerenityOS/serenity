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

package com.sun.org.apache.xml.internal.serializer.utils;

/**
 * This class is for throwing important checked exceptions
 * over non-checked methods.  It should be used with care,
 * and in limited circumstances.
 *
 * This class is a copy of the one in com.sun.org.apache.xml.internal.utils.
 * It exists to cut the serializers dependancy on that package.
 *
 * This class is not a public API, it is only public because it is
 * used by com.sun.org.apache.xml.internal.serializer.
 * @xsl.usage internal
 */
public final class WrappedRuntimeException extends RuntimeException
{
    static final long serialVersionUID = 7140414456714658073L;

  /** Primary checked exception.
   *  @serial          */
  private Exception m_exception;

  /**
   * Construct a WrappedRuntimeException from a
   * checked exception.
   *
   * @param e Primary checked exception
   */
  public WrappedRuntimeException(Exception e)
  {

    super(e.getMessage());

    m_exception = e;
  }

  /**
   * Constructor WrappedRuntimeException
   *
   *
   * @param msg Exception information.
   * @param e Primary checked exception
   */
  public WrappedRuntimeException(String msg, Exception e)
  {

    super(msg);

    m_exception = e;
  }

  /**
   * Get the checked exception that this runtime exception wraps.
   *
   * @return The primary checked exception
   */
  public Exception getException()
  {
    return m_exception;
  }
}
