/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Objects;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Soundbank;
import javax.sound.midi.spi.SoundbankReader;

import sun.reflect.misc.ReflectUtil;

/**
 * JarSoundbankReader is used to read soundbank object from jar files.
 *
 * @author Karl Helgason
 */
public final class JARSoundbankReader extends SoundbankReader {

    private static boolean isZIP(URL url) {
        boolean ok = false;
        try {
            InputStream stream = url.openStream();
            try {
                byte[] buff = new byte[4];
                ok = stream.read(buff) == 4;
                if (ok) {
                    ok =  (buff[0] == 0x50
                        && buff[1] == 0x4b
                        && buff[2] == 0x03
                        && buff[3] == 0x04);
                }
            } finally {
                stream.close();
            }
        } catch (IOException e) {
        }
        return ok;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Soundbank getSoundbank(URL url)
            throws InvalidMidiDataException, IOException {
        if (!isZIP(url))
            return null;
        ArrayList<Soundbank> soundbanks = new ArrayList<>();
        URLClassLoader ucl = URLClassLoader.newInstance(new URL[]{url});
        InputStream stream = ucl.getResourceAsStream(
                "META-INF/services/javax.sound.midi.Soundbank");
        if (stream == null)
            return null;
        try
        {
            BufferedReader r = new BufferedReader(new InputStreamReader(stream));
            String line = r.readLine();
            while (line != null) {
                if (!line.startsWith("#")) {
                    try {
                        Class<?> c = Class.forName(line.trim(), false, ucl);
                        if (Soundbank.class.isAssignableFrom(c)) {
                            ReflectUtil.checkPackageAccess(c);
                            Object o = c.newInstance();
                            soundbanks.add((Soundbank) o);
                        }
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
                line = r.readLine();
            }
        }
        finally
        {
            stream.close();
        }
        if (soundbanks.size() == 0)
            return null;
        if (soundbanks.size() == 1)
            return soundbanks.get(0);
        SimpleSoundbank sbk = new SimpleSoundbank();
        for (Soundbank soundbank : soundbanks)
            sbk.addAllInstruments(soundbank);
        return sbk;
    }

    @Override
    public Soundbank getSoundbank(InputStream stream)
            throws InvalidMidiDataException, IOException {
        Objects.requireNonNull(stream);
        return null;
    }

    @Override
    public Soundbank getSoundbank(File file)
            throws InvalidMidiDataException, IOException {
        return getSoundbank(file.toURI().toURL());
    }
}
