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

import java.io.IOException;

/**
 * It is unfortunate that java.io.Writer is a class rather than an interface.
 * The serializer has a number of classes that extend java.io.Writer
 * and which send their ouput to a yet another wrapped Writer or OutputStream.
 *
 * The purpose of this interface is to force such classes to over-ride all of
 * the important methods defined on the java.io.Writer class, namely these:
 * <code>
 * write(int val)
 * write(char[] chars)
 * write(char[] chars, int start, int count)
 * write(String chars)
 * write(String chars, int start, int count)
 * flush()
 * close()
 * </code>
 * In this manner nothing will accidentally go directly to
 * the base class rather than to the wrapped Writer or OutputStream.
 *
 * The purpose of this class is to have a uniform way of chaining the output of one writer to
 * the next writer in the chain. In addition there are methods to obtain the Writer or
 * OutputStream that this object sends its output to.
 *
 * This interface is only for internal use withing the serializer.
 * @xsl.usage internal
 */
interface WriterChain
{
    /** This method forces us to over-ride the method defined in java.io.Writer */
    public void write(int val) throws IOException;
    /** This method forces us to over-ride the method defined in java.io.Writer */
    public void write(char[] chars) throws IOException;
    /** This method forces us to over-ride the method defined in java.io.Writer */
    public void write(char[] chars, int start, int count) throws IOException;
    /** This method forces us to over-ride the method defined in java.io.Writer */
    public void write(String chars) throws IOException;
    /** This method forces us to over-ride the method defined in java.io.Writer */
    public void write(String chars, int start, int count) throws IOException;
    /** This method forces us to over-ride the method defined in java.io.Writer */
    public void flush() throws IOException;
    /** This method forces us to over-ride the method defined in java.io.Writer */
    public void close() throws IOException;

    /**
     * If this method returns null, getOutputStream() must return non-null.
     * Get the writer that this writer sends its output to.
     *
     * It is possible that the Writer returned by this method does not
     * implement the WriterChain interface.
     */
    public java.io.Writer getWriter();

    /**
     * If this method returns null, getWriter() must return non-null.
     * Get the OutputStream that this writer sends its output to.
     */
    public java.io.OutputStream getOutputStream();
}
