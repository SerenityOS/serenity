/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.sound;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.SequenceInputStream;
import java.util.Objects;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * AU file writer.
 *
 * @author Jan Borgersen
 */
public final class AuFileWriter extends SunFileWriter {

    /**
     * Value for length field if length is not known.
     */
    private static final int UNKNOWN_SIZE = -1;

    /**
     * Constructs a new AuFileWriter object.
     */
    public AuFileWriter() {
        super(new Type[]{Type.AU});
    }

    @Override
    public Type[] getAudioFileTypes(AudioInputStream stream) {

        Type[] filetypes = new Type[types.length];
        System.arraycopy(types, 0, filetypes, 0, types.length);

        // make sure we can write this stream
        AudioFormat format = stream.getFormat();
        AudioFormat.Encoding encoding = format.getEncoding();

        if (AudioFormat.Encoding.ALAW.equals(encoding)
                || AudioFormat.Encoding.ULAW.equals(encoding)
                || AudioFormat.Encoding.PCM_SIGNED.equals(encoding)
                || AudioFormat.Encoding.PCM_UNSIGNED.equals(encoding)
                || AudioFormat.Encoding.PCM_FLOAT.equals(encoding)) {
            return filetypes;
        }

        return new Type[0];
    }

    @Override
    public int write(AudioInputStream stream, Type fileType, OutputStream out) throws IOException {
        Objects.requireNonNull(stream);
        Objects.requireNonNull(fileType);
        Objects.requireNonNull(out);

        // we must know the total data length to calculate the file length
        //$$fb 2001-07-13: fix for bug 4351296: do not throw an exception
        //if( stream.getFrameLength() == AudioSystem.NOT_SPECIFIED ) {
        //      throw new IOException("stream length not specified");
        //}

        // throws IllegalArgumentException if not supported
        AuFileFormat auFileFormat = (AuFileFormat)getAudioFileFormat(fileType, stream);
        return writeAuFile(stream, auFileFormat, out);
    }

    @Override
    public int write(AudioInputStream stream, Type fileType, File out) throws IOException {
        Objects.requireNonNull(stream);
        Objects.requireNonNull(fileType);
        Objects.requireNonNull(out);

        // throws IllegalArgumentException if not supported
        AuFileFormat auFileFormat = (AuFileFormat)getAudioFileFormat(fileType, stream);

        // first write the file without worrying about length fields
        final int bytesWritten;
        try (final FileOutputStream fos = new FileOutputStream(out);
             final BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            bytesWritten = writeAuFile(stream, auFileFormat, bos);
        }

        // now, if length fields were not specified, calculate them,
        // open as a random access file, write the appropriate fields,
        // close again....
        if( auFileFormat.getByteLength()== AudioSystem.NOT_SPECIFIED ) {

            // $$kk: 10.22.99: jan: please either implement this or throw an exception!
            // $$fb: 2001-07-13: done. Fixes Bug 4479981
            try (final RandomAccessFile raf = new RandomAccessFile(out, "rw")) {
                if (raf.length() <= 0x7FFFFFFFl) {
                    // skip AU magic and data offset field
                    raf.skipBytes(8);
                    raf.writeInt(bytesWritten - AuFileFormat.AU_HEADERSIZE);
                    // that's all
                }
            }
        }

        return bytesWritten;
    }

    // -------------------------------------------------------------

    /**
     * Returns the AudioFileFormat describing the file that will be written from this AudioInputStream.
     * Throws IllegalArgumentException if not supported.
     */
    private AudioFileFormat getAudioFileFormat(Type type, AudioInputStream stream) {
        if (!isFileTypeSupported(type, stream)) {
            throw new IllegalArgumentException("File type " + type + " not supported.");
        }

        AudioFormat streamFormat = stream.getFormat();
        AudioFormat.Encoding encoding = streamFormat.getEncoding();

        if (AudioFormat.Encoding.PCM_UNSIGNED.equals(encoding)) {
            encoding = AudioFormat.Encoding.PCM_SIGNED;
        }

        // We always write big endian au files, this is by far the standard
        AudioFormat format = new AudioFormat(encoding,
                                             streamFormat.getSampleRate(),
                                             streamFormat.getSampleSizeInBits(),
                                             streamFormat.getChannels(),
                                             streamFormat.getFrameSize(),
                                             streamFormat.getFrameRate(), true);

        int fileSize;
        if (stream.getFrameLength() != AudioSystem.NOT_SPECIFIED) {
            fileSize = (int)stream.getFrameLength()*streamFormat.getFrameSize() + AuFileFormat.AU_HEADERSIZE;
        } else {
            fileSize = AudioSystem.NOT_SPECIFIED;
        }

        return new AuFileFormat(Type.AU, fileSize, format,
                                (int) stream.getFrameLength());
    }

    private InputStream getFileStream(AuFileFormat auFileFormat, AudioInputStream audioStream) throws IOException {

        // private method ... assumes auFileFormat is a supported file type

        AudioFormat format            = auFileFormat.getFormat();

        int headerSize     = AuFileFormat.AU_HEADERSIZE;
        long dataSize       = auFileFormat.getFrameLength();
        //$$fb fix for Bug 4351296
        //int dataSizeInBytes = dataSize * format.getFrameSize();
        long dataSizeInBytes = (dataSize==AudioSystem.NOT_SPECIFIED)?UNKNOWN_SIZE:dataSize * format.getFrameSize();
        if (dataSizeInBytes>0x7FFFFFFFl) {
            dataSizeInBytes=UNKNOWN_SIZE;
        }
        int auType = auFileFormat.getAuType();
        int sampleRate     = (int)format.getSampleRate();
        int channels       = format.getChannels();

        // if we need to do any format conversion, we do it here.
        //$$ fb 2001-07-13: Bug 4391108
        audioStream = AudioSystem.getAudioInputStream(format, audioStream);

        final byte[] header;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(AuFileFormat.AU_SUN_MAGIC);
            dos.writeInt(headerSize);
            dos.writeInt((int) dataSizeInBytes);
            dos.writeInt(auType);
            dos.writeInt(sampleRate);
            dos.writeInt(channels);
            header = baos.toByteArray();
        }
        // Now create a new InputStream from headerStream and the InputStream
        // in audioStream
        return new SequenceInputStream(new ByteArrayInputStream(header),
                                       new NoCloseInputStream(audioStream));
    }

    private int writeAuFile(AudioInputStream in, AuFileFormat auFileFormat,
                            OutputStream out) throws IOException {

        int bytesRead = 0;
        int bytesWritten = 0;
        InputStream fileStream = getFileStream(auFileFormat, in);
        byte[] buffer = new byte[bisBufferSize];
        int maxLength = auFileFormat.getByteLength();

        while( (bytesRead = fileStream.read( buffer )) >= 0 ) {
            if (maxLength>0) {
                if( bytesRead < maxLength ) {
                    out.write( buffer, 0, bytesRead );
                    bytesWritten += bytesRead;
                    maxLength -= bytesRead;
                } else {
                    out.write( buffer, 0, maxLength );
                    bytesWritten += maxLength;
                    maxLength = 0;
                    break;
                }
            } else {
                out.write( buffer, 0, bytesRead );
                bytesWritten += bytesRead;
            }
        }

        return bytesWritten;
    }
}
