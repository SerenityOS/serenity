/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.file;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.main.OptionHelper;
import com.sun.tools.javac.main.OptionHelper.GrumpyHelper;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.util.Abort;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;

/**
 * Utility methods for building a file manager.
 * There are no references here to file-system specific objects such as
 * java.io.File or java.nio.file.Path.
 */
public abstract class BaseFileManager implements JavaFileManager {
    protected BaseFileManager(Charset charset) {
        this.charset = charset;
        byteBufferCache = new ByteBufferCache();
        locations = createLocations();
    }

    /**
     * Set the context for JavacPathFileManager.
     * @param context the context containing items to be associated with the file manager
     */
    public void setContext(Context context) {
        log = Log.instance(context);
        options = Options.instance(context);
        classLoaderClass = options.get("procloader");

        // Avoid initializing Lint
        boolean warn = options.isLintSet("path");
        locations.update(log, warn, FSInfo.instance(context));

        // Setting this option is an indication that close() should defer actually closing
        // the file manager until after a specified period of inactivity.
        // This is to accommodate clients which save references to Symbols created for use
        // within doclets or annotation processors, and which then attempt to use those
        // references after the tool exits, having closed any internally managed file manager.
        // Ideally, such clients should run the tool via the javax.tools API, providing their
        // own file manager, which can be closed by the client when all use of that file
        // manager is complete.
        // If the option has a numeric value, it will be interpreted as the duration,
        // in seconds, of the period of inactivity to wait for, before the file manager
        // is actually closed.
        // See also deferredClose().
        String s = options.get("fileManager.deferClose");
        if (s != null) {
            try {
                deferredCloseTimeout = (int) (Float.parseFloat(s) * 1000);
            } catch (NumberFormatException e) {
                deferredCloseTimeout = 60 * 1000;  // default: one minute, in millis
            }
        }
    }

    protected Locations createLocations() {
        return new Locations();
    }

    /**
     * The log to be used for error reporting.
     */
    public Log log;

    /**
     * User provided charset (through javax.tools).
     */
    protected Charset charset;

    protected Options options;

    protected String classLoaderClass;

    protected final Locations locations;

    /**
     * A flag for clients to use to indicate that this file manager should
     * be closed when it is no longer required.
     */
    public boolean autoClose;

    /**
     * Wait for a period of inactivity before calling close().
     * The length of the period of inactivity is given by {@code deferredCloseTimeout}
     */
    protected void deferredClose() {
        Thread t = new Thread(getClass().getName() + " DeferredClose") {
            @Override
            public void run() {
                try {
                    synchronized (BaseFileManager.this) {
                        long now = System.currentTimeMillis();
                        while (now < lastUsedTime + deferredCloseTimeout) {
                            BaseFileManager.this.wait(lastUsedTime + deferredCloseTimeout - now);
                            now = System.currentTimeMillis();
                        }
                        deferredCloseTimeout = 0;
                        close();
                    }
                } catch (InterruptedException e) {
                } catch (IOException e) {
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    synchronized void updateLastUsedTime() {
        if (deferredCloseTimeout > 0) { // avoid updating the time unnecessarily
            lastUsedTime = System.currentTimeMillis();
        }
    }

    private long lastUsedTime = System.currentTimeMillis();
    protected long deferredCloseTimeout = 0;

    public void clear() {
        new HashSet<>(options.keySet()).forEach(k -> options.remove(k));
    }

    protected ClassLoader getClassLoader(URL[] urls) {
        ClassLoader thisClassLoader = getClass().getClassLoader();

        // Allow the following to specify a closeable classloader
        // other than URLClassLoader.

        // 1: Allow client to specify the class to use via hidden option
        if (classLoaderClass != null) {
            try {
                Class<? extends ClassLoader> loader =
                        Class.forName(classLoaderClass).asSubclass(ClassLoader.class);
                Class<?>[] constrArgTypes = { URL[].class, ClassLoader.class };
                Constructor<? extends ClassLoader> constr = loader.getConstructor(constrArgTypes);
                return constr.newInstance(urls, thisClassLoader);
            } catch (ReflectiveOperationException t) {
                // ignore errors loading user-provided class loader, fall through
            }
        }
        return new URLClassLoader(urls, thisClassLoader);
    }

    public boolean isDefaultBootClassPath() {
        return locations.isDefaultBootClassPath();
    }

    public boolean isDefaultSystemModulesPath() {
        return locations.isDefaultSystemModulesPath();
    }

    // <editor-fold defaultstate="collapsed" desc="Option handling">
    @Override @DefinedBy(Api.COMPILER)
    public boolean handleOption(String current, Iterator<String> remaining) {
        OptionHelper helper = new GrumpyHelper(log) {
            @Override
            public String get(Option option) {
                return options.get(option);
            }

            @Override
            public void put(String name, String value) {
                options.put(name, value);
            }

            @Override
            public void remove(String name) {
                options.remove(name);
            }

            @Override
            public boolean handleFileManagerOption(Option option, String value) {
                return handleOption(option, value);
            }
        };

        Option o = Option.lookup(current, javacFileManagerOptions);
        if (o == null) {
            return false;
        }

        try {
            o.handleOption(helper, current, remaining);
        } catch (Option.InvalidValueException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        return true;
    }
    // where
        protected static final Set<Option> javacFileManagerOptions =
            Option.getJavacFileManagerOptions();

    @Override @DefinedBy(Api.COMPILER)
    public int isSupportedOption(String option) {
        Option o = Option.lookup(option, javacFileManagerOptions);
        return (o == null) ? -1 : o.hasArg() ? 1 : 0;
    }

    protected String multiReleaseValue;

    /**
     * Common back end for OptionHelper handleFileManagerOption.
     * @param option the option whose value to be set
     * @param value the value for the option
     * @return true if successful, and false otherwise
     */
    public boolean handleOption(Option option, String value) {
        switch (option) {
            case ENCODING:
                encodingName = value;
                return true;

            case MULTIRELEASE:
                multiReleaseValue = value;
                locations.setMultiReleaseValue(value);
                return true;

            default:
                return locations.handleOption(option, value);
        }
    }

    /**
     * Call handleOption for collection of options and corresponding values.
     * @param map a collection of options and corresponding values
     * @return true if all the calls are successful
     */
    public boolean handleOptions(Map<Option, String> map) {
        boolean ok = true;
        for (Map.Entry<Option, String> e: map.entrySet()) {
            try {
                ok = ok & handleOption(e.getKey(), e.getValue());
            } catch (IllegalArgumentException ex) {
                log.error(Errors.IllegalArgumentForOption(e.getKey().getPrimaryName(), ex.getMessage()));
                ok = false;
            }
        }
        return ok;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Encoding">
    private String encodingName;
    private String defaultEncodingName;
    private String getDefaultEncodingName() {
        if (defaultEncodingName == null) {
            defaultEncodingName = Charset.defaultCharset().name();
        }
        return defaultEncodingName;
    }

    public String getEncodingName() {
        return (encodingName != null) ? encodingName : getDefaultEncodingName();
    }

    public CharBuffer decode(ByteBuffer inbuf, boolean ignoreEncodingErrors) {
        String encName = getEncodingName();
        CharsetDecoder decoder;
        try {
            decoder = getDecoder(encName, ignoreEncodingErrors);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            log.error(Errors.UnsupportedEncoding(encName));
            return CharBuffer.allocate(1).flip();
        }

        // slightly overestimate the buffer size to avoid reallocation.
        float factor =
            decoder.averageCharsPerByte() * 0.8f +
            decoder.maxCharsPerByte() * 0.2f;
        CharBuffer dest = CharBuffer.
            allocate(10 + (int)(inbuf.remaining()*factor));

        while (true) {
            CoderResult result = decoder.decode(inbuf, dest, true);
            dest.flip();

            if (result.isUnderflow()) { // done reading
                // make sure there is at least one extra character
                if (dest.limit() == dest.capacity()) {
                    dest = CharBuffer.allocate(dest.capacity()+1).put(dest);
                    dest.flip();
                }
                return dest;
            } else if (result.isOverflow()) { // buffer too small; expand
                int newCapacity =
                    10 + dest.capacity() +
                    (int)(inbuf.remaining()*decoder.maxCharsPerByte());
                dest = CharBuffer.allocate(newCapacity).put(dest);
            } else if (result.isMalformed() || result.isUnmappable()) {
                // bad character in input
                StringBuilder unmappable = new StringBuilder();
                int len = result.length();

                for (int i = 0; i < len; i++) {
                    unmappable.append(String.format("%02X", inbuf.get()));
                }

                String charsetName = charset == null ? encName : charset.name();

                log.error(dest.limit(),
                          Errors.IllegalCharForEncoding(unmappable.toString(), charsetName));

                // undo the flip() to prepare the output buffer
                // for more translation
                dest.position(dest.limit());
                dest.limit(dest.capacity());
                dest.put((char)0xfffd); // backward compatible
            } else {
                throw new AssertionError(result);
            }
        }
        // unreached
    }

    public CharsetDecoder getDecoder(String encodingName, boolean ignoreEncodingErrors) {
        Charset cs = (this.charset == null)
            ? Charset.forName(encodingName)
            : this.charset;
        CharsetDecoder decoder = cs.newDecoder();

        CodingErrorAction action;
        if (ignoreEncodingErrors)
            action = CodingErrorAction.REPLACE;
        else
            action = CodingErrorAction.REPORT;

        return decoder
            .onMalformedInput(action)
            .onUnmappableCharacter(action);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="ByteBuffers">
    /**
     * Make a byte buffer from an input stream.
     * @param in the stream
     * @return a byte buffer containing the contents of the stream
     * @throws IOException if an error occurred while reading the stream
     */
    public ByteBuffer makeByteBuffer(InputStream in)
        throws IOException {
        int limit = in.available();
        if (limit < 1024) limit = 1024;
        ByteBuffer result = byteBufferCache.get(limit);
        int position = 0;
        while (in.available() != 0) {
            if (position >= limit)
                // expand buffer
                result = ByteBuffer.
                    allocate(limit <<= 1).
                    put(result.flip());
            int count = in.read(result.array(),
                position,
                limit - position);
            if (count < 0) break;
            result.position(position += count);
        }
        return result.flip();
    }

    public void recycleByteBuffer(ByteBuffer bb) {
        byteBufferCache.put(bb);
    }

    /**
     * A single-element cache of direct byte buffers.
     */
    private static class ByteBufferCache {
        private ByteBuffer cached;
        ByteBuffer get(int capacity) {
            if (capacity < 20480) capacity = 20480;
            ByteBuffer result =
                (cached != null && cached.capacity() >= capacity)
                ? cached.clear()
                : ByteBuffer.allocate(capacity + capacity>>1);
            cached = null;
            return result;
        }
        void put(ByteBuffer x) {
            cached = x;
        }
    }

    private final ByteBufferCache byteBufferCache;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Content cache">
    public CharBuffer getCachedContent(JavaFileObject file) {
        ContentCacheEntry e = contentCache.get(file);
        if (e == null)
            return null;

        if (!e.isValid(file)) {
            contentCache.remove(file);
            return null;
        }

        return e.getValue();
    }

    public void cache(JavaFileObject file, CharBuffer cb) {
        contentCache.put(file, new ContentCacheEntry(file, cb));
    }

    public void flushCache(JavaFileObject file) {
        contentCache.remove(file);
    }

    protected final Map<JavaFileObject, ContentCacheEntry> contentCache = new HashMap<>();

    protected static class ContentCacheEntry {
        final long timestamp;
        final SoftReference<CharBuffer> ref;

        ContentCacheEntry(JavaFileObject file, CharBuffer cb) {
            this.timestamp = file.getLastModified();
            this.ref = new SoftReference<>(cb);
        }

        boolean isValid(JavaFileObject file) {
            return timestamp == file.getLastModified();
        }

        CharBuffer getValue() {
            return ref.get();
        }
    }
    // </editor-fold>

    public static Kind getKind(Path path) {
        return getKind(path.getFileName().toString());
    }

    public static Kind getKind(String name) {
        if (name.endsWith(Kind.CLASS.extension))
            return Kind.CLASS;
        else if (name.endsWith(Kind.SOURCE.extension))
            return Kind.SOURCE;
        else if (name.endsWith(Kind.HTML.extension))
            return Kind.HTML;
        else
            return Kind.OTHER;
    }

    protected static <T> T nullCheck(T o) {
        return Objects.requireNonNull(o);
    }

    protected static <T> Collection<T> nullCheck(Collection<T> it) {
        for (T t : it)
            Objects.requireNonNull(t);
        return it;
    }
}
