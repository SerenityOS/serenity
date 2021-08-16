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
import java.io.OutputStream;
import java.io.Writer;

/**
 * This class wraps the real writer, it only purpose is to send
 * CHARACTERTOSTREAM events to the trace listener.
 * Each method immediately sends the call to the wrapped writer unchanged, but
 * in addition it collects characters to be issued to a trace listener.
 *
 * In this way the trace
 * listener knows what characters have been written to the output Writer.
 *
 * There may still be differences in what the trace events say is going to the
 * output writer and what is really going there. These differences will be due
 * to the fact that this class is UTF-8 encoding before emiting the trace event
 * and the underlying writer may not be UTF-8 encoding. There may also be
 * encoding differences.  So the main pupose of this class is to provide a
 * resonable facsimile of the true output.
 *
 * @xsl.usage internal
 */
final class SerializerTraceWriter extends Writer implements WriterChain
{

    /** The real writer to immediately write to.
     * This reference may be null, in which case nothing is written out, but
     * only the trace events are fired for output.
     */
    private final java.io.Writer m_writer;

    /** The tracer to send events to */
    private final SerializerTrace m_tracer;

    /** The size of the internal buffer, just to keep too many
     * events from being sent to the tracer
     */
    private int buf_length;

    /**
     * Internal buffer to collect the characters to go to the trace listener.
     *
     */
    private byte buf[];

    /**
     * How many bytes have been collected and still need to go to trace
     * listener.
     */
    private int count;

    /**
     * Creates or replaces the internal buffer, and makes sure it has a few
     * extra bytes slight overflow of the last UTF8 encoded character.
     * @param size
     */
    private void setBufferSize(int size)
    {
        buf = new byte[size + 3];
        buf_length = size;
        count = 0;
    }

    /**
     * Constructor.
     * If the writer passed in is null, then this SerializerTraceWriter will
     * only signal trace events of what would have been written to that writer.
     * If the writer passed in is not null then the trace events will mirror
     * what is going to that writer. In this way tools, such as a debugger, can
     * gather information on what is being written out.
     *
     * @param out the Writer to write to (possibly null)
     * @param tracer the tracer to inform that characters are being written
     */
    public SerializerTraceWriter(Writer out, SerializerTrace tracer)
    {
        m_writer = out;
        m_tracer = tracer;
        setBufferSize(1024);
    }

    /**
     * Flush out the collected characters by sending them to the trace
     * listener.  These characters are never written to the real writer
     * (m_writer) because that has already happened with every method
     * call. This method simple informs the listener of what has already
     * happened.
     * @throws IOException
     */
    private void flushBuffer() throws IOException
    {

        // Just for tracing purposes
        if (count > 0)
        {
            char[] chars = new char[count];
            for(int i=0; i<count; i++)
                chars[i] = (char) buf[i];

            if (m_tracer != null)
                m_tracer.fireGenerateEvent(
                    SerializerTrace.EVENTTYPE_OUTPUT_CHARACTERS,
                    chars,
                    0,
                    chars.length);

            count = 0;
        }
    }

    /**
     * Flush the internal buffer and flush the Writer
     * @see java.io.Writer#flush()
     */
    public void flush() throws java.io.IOException
    {
        // send to the real writer
        if (m_writer != null)
            m_writer.flush();

        // from here on just for tracing purposes
        flushBuffer();
    }

    /**
     * Flush the internal buffer and close the Writer
     * @see java.io.Writer#close()
     */
    public void close() throws java.io.IOException
    {
        // send to the real writer
        if (m_writer != null)
            m_writer.close();

        // from here on just for tracing purposes
        flushBuffer();
    }


    /**
     * Write a single character.  The character to be written is contained in
     * the 16 low-order bits of the given integer value; the 16 high-order bits
     * are ignored.
     *
     * <p> Subclasses that intend to support efficient single-character output
     * should override this method.
     *
     * @param c  int specifying a character to be written.
     * @exception  IOException  If an I/O error occurs
     */
    public void write(final int c) throws IOException
    {
        // send to the real writer
        if (m_writer != null)
            m_writer.write(c);

        // ---------- from here on just collect for tracing purposes

        /* If we are close to the end of the buffer then flush it.
         * Remember the buffer can hold a few more characters than buf_length
         */
        if (count >= buf_length)
            flushBuffer();

        if (c < 0x80)
        {
            buf[count++] = (byte) (c);
        }
        else if (c < 0x800)
        {
            buf[count++] = (byte) (0xc0 + (c >> 6));
            buf[count++] = (byte) (0x80 + (c & 0x3f));
        }
        else
        {
            buf[count++] = (byte) (0xe0 + (c >> 12));
            buf[count++] = (byte) (0x80 + ((c >> 6) & 0x3f));
            buf[count++] = (byte) (0x80 + (c & 0x3f));
        }
    }

    /**
     * Write a portion of an array of characters.
     *
     * @param  chars  Array of characters
     * @param  start   Offset from which to start writing characters
     * @param  length   Number of characters to write
     *
     * @exception  IOException  If an I/O error occurs
     *
     * @throws java.io.IOException
     */
    public void write(final char chars[], final int start, final int length)
        throws java.io.IOException
    {
        // send to the real writer
        if (m_writer != null)
            m_writer.write(chars, start, length);

        // from here on just collect for tracing purposes
        int lengthx3 = (length << 1) + length;

        if (lengthx3 >= buf_length)
        {

            /* If the request length exceeds the size of the output buffer,
              * flush the output buffer and make the buffer bigger to handle.
              */

            flushBuffer();
            setBufferSize(2 * lengthx3);

        }

        if (lengthx3 > buf_length - count)
        {
            flushBuffer();
        }

        final int n = length + start;
        for (int i = start; i < n; i++)
        {
            final char c = chars[i];

            if (c < 0x80)
                buf[count++] = (byte) (c);
            else if (c < 0x800)
            {
                buf[count++] = (byte) (0xc0 + (c >> 6));
                buf[count++] = (byte) (0x80 + (c & 0x3f));
            }
            else
            {
                buf[count++] = (byte) (0xe0 + (c >> 12));
                buf[count++] = (byte) (0x80 + ((c >> 6) & 0x3f));
                buf[count++] = (byte) (0x80 + (c & 0x3f));
            }
        }

    }

    /**
     * Write a string.
     *
     * @param  s  String to be written
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write(final String s) throws IOException
    {
        // send to the real writer
        if (m_writer != null)
            m_writer.write(s);

        // from here on just collect for tracing purposes
        final int length = s.length();

        // We multiply the length by three since this is the maximum length
        // of the characters that we can put into the buffer.  It is possible
        // for each Unicode character to expand to three bytes.

        int lengthx3 = (length << 1) + length;

        if (lengthx3 >= buf_length)
        {

            /* If the request length exceeds the size of the output buffer,
              * flush the output buffer and make the buffer bigger to handle.
              */

            flushBuffer();
            setBufferSize(2 * lengthx3);
        }

        if (lengthx3 > buf_length - count)
        {
            flushBuffer();
        }

        for (int i = 0; i < length; i++)
        {
            final char c = s.charAt(i);

            if (c < 0x80)
                buf[count++] = (byte) (c);
            else if (c < 0x800)
            {
                buf[count++] = (byte) (0xc0 + (c >> 6));
                buf[count++] = (byte) (0x80 + (c & 0x3f));
            }
            else
            {
                buf[count++] = (byte) (0xe0 + (c >> 12));
                buf[count++] = (byte) (0x80 + ((c >> 6) & 0x3f));
                buf[count++] = (byte) (0x80 + (c & 0x3f));
            }
        }
    }

    /**
     * Get the writer that this one directly wraps.
     */
    public Writer getWriter()
    {
        return m_writer;
    }

    /**
     * Get the OutputStream that is the at the end of the
     * chain of writers.
     */
    public OutputStream getOutputStream()
    {
        OutputStream retval = null;
        if (m_writer instanceof WriterChain)
            retval = ((WriterChain) m_writer).getOutputStream();
        return retval;
    }
}
