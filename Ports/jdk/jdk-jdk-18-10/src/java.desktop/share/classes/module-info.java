/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Defines the AWT and Swing user interface toolkits, plus APIs for
 * accessibility, audio, imaging, printing, and JavaBeans.
 * <p>
 * The documentation in this module includes links to external overviews,
 * tutorials, examples, guides, media format specifications, and other similar
 * documentation. These links are meant to be informative to the reader and
 * nothing more. Information at these external resources, no matter the hosting
 * or the author, is not part of Java Platform API specification unless
 * explicitly stated to be so.
 *
 * @uses java.awt.im.spi.InputMethodDescriptor
 * @uses javax.accessibility.AccessibilityProvider
 * @uses javax.imageio.spi.ImageInputStreamSpi
 * @uses javax.imageio.spi.ImageOutputStreamSpi
 * @uses javax.imageio.spi.ImageReaderSpi
 * @uses javax.imageio.spi.ImageTranscoderSpi
 * @uses javax.imageio.spi.ImageWriterSpi
 * @uses javax.print.PrintServiceLookup
 * @uses javax.print.StreamPrintServiceFactory
 * @uses javax.sound.midi.spi.MidiDeviceProvider
 * @uses javax.sound.midi.spi.MidiFileReader
 * @uses javax.sound.midi.spi.MidiFileWriter
 * @uses javax.sound.midi.spi.SoundbankReader
 * @uses javax.sound.sampled.spi.AudioFileReader
 * @uses javax.sound.sampled.spi.AudioFileWriter
 * @uses javax.sound.sampled.spi.FormatConversionProvider
 * @uses javax.sound.sampled.spi.MixerProvider
 *
 * @moduleGraph
 * @since 9
 */
module java.desktop {
    requires java.prefs;

    requires transitive java.datatransfer;
    requires transitive java.xml;

    exports java.applet;
    exports java.awt;
    exports java.awt.color;
    exports java.awt.desktop;
    exports java.awt.dnd;
    exports java.awt.event;
    exports java.awt.font;
    exports java.awt.geom;
    exports java.awt.im;
    exports java.awt.im.spi;
    exports java.awt.image;
    exports java.awt.image.renderable;
    exports java.awt.print;
    exports java.beans;
    exports java.beans.beancontext;
    exports javax.accessibility;
    exports javax.imageio;
    exports javax.imageio.event;
    exports javax.imageio.metadata;
    exports javax.imageio.plugins.bmp;
    exports javax.imageio.plugins.jpeg;
    exports javax.imageio.plugins.tiff;
    exports javax.imageio.spi;
    exports javax.imageio.stream;
    exports javax.print;
    exports javax.print.attribute;
    exports javax.print.attribute.standard;
    exports javax.print.event;
    exports javax.sound.midi;
    exports javax.sound.midi.spi;
    exports javax.sound.sampled;
    exports javax.sound.sampled.spi;
    exports javax.swing;
    exports javax.swing.border;
    exports javax.swing.colorchooser;
    exports javax.swing.event;
    exports javax.swing.filechooser;
    exports javax.swing.plaf;
    exports javax.swing.plaf.basic;
    exports javax.swing.plaf.metal;
    exports javax.swing.plaf.multi;
    exports javax.swing.plaf.nimbus;
    exports javax.swing.plaf.synth;
    exports javax.swing.table;
    exports javax.swing.text;
    exports javax.swing.text.html;
    exports javax.swing.text.html.parser;
    exports javax.swing.text.rtf;
    exports javax.swing.tree;
    exports javax.swing.undo;

    // qualified exports may be inserted at build time
    // see make/GensrcModuleInfo.gmk
    exports sun.awt to
        jdk.accessibility,
        jdk.unsupported.desktop;

    exports java.awt.dnd.peer to jdk.unsupported.desktop;
    exports sun.awt.dnd to jdk.unsupported.desktop;
    exports sun.swing to jdk.unsupported.desktop;

    opens javax.swing.plaf.basic to
        jdk.jconsole;

    uses java.awt.im.spi.InputMethodDescriptor;
    uses javax.accessibility.AccessibilityProvider;
    uses javax.imageio.spi.ImageInputStreamSpi;
    uses javax.imageio.spi.ImageOutputStreamSpi;
    uses javax.imageio.spi.ImageReaderSpi;
    uses javax.imageio.spi.ImageTranscoderSpi;
    uses javax.imageio.spi.ImageWriterSpi;
    uses javax.print.PrintServiceLookup;
    uses javax.print.StreamPrintServiceFactory;
    uses javax.sound.midi.spi.MidiDeviceProvider;
    uses javax.sound.midi.spi.MidiFileReader;
    uses javax.sound.midi.spi.MidiFileWriter;
    uses javax.sound.midi.spi.SoundbankReader;
    uses javax.sound.sampled.spi.AudioFileReader;
    uses javax.sound.sampled.spi.AudioFileWriter;
    uses javax.sound.sampled.spi.FormatConversionProvider;
    uses javax.sound.sampled.spi.MixerProvider;

    uses sun.swing.InteropProvider;

    provides sun.datatransfer.DesktopDatatransferService with
        sun.awt.datatransfer.DesktopDatatransferServiceImpl;

    provides java.net.ContentHandlerFactory with
        sun.awt.www.content.MultimediaContentHandlers;

    provides javax.print.PrintServiceLookup with
        sun.print.PrintServiceLookupProvider;

    provides javax.print.StreamPrintServiceFactory with
        sun.print.PSStreamPrinterFactory;

    provides javax.sound.midi.spi.MidiDeviceProvider with
        com.sun.media.sound.MidiInDeviceProvider,
        com.sun.media.sound.MidiOutDeviceProvider,
        com.sun.media.sound.RealTimeSequencerProvider,
        com.sun.media.sound.SoftProvider;

    provides javax.sound.midi.spi.MidiFileReader with
        com.sun.media.sound.StandardMidiFileReader;

    provides javax.sound.midi.spi.MidiFileWriter with
        com.sun.media.sound.StandardMidiFileWriter;

    provides javax.sound.midi.spi.SoundbankReader with
        com.sun.media.sound.AudioFileSoundbankReader,
        com.sun.media.sound.DLSSoundbankReader,
        com.sun.media.sound.JARSoundbankReader,
        com.sun.media.sound.SF2SoundbankReader;

    provides javax.sound.sampled.spi.AudioFileReader with
        com.sun.media.sound.AiffFileReader,
        com.sun.media.sound.AuFileReader,
        com.sun.media.sound.SoftMidiAudioFileReader,
        com.sun.media.sound.WaveFileReader,
        com.sun.media.sound.WaveFloatFileReader,
        com.sun.media.sound.WaveExtensibleFileReader;

    provides javax.sound.sampled.spi.AudioFileWriter with
        com.sun.media.sound.AiffFileWriter,
        com.sun.media.sound.AuFileWriter,
        com.sun.media.sound.WaveFileWriter,
        com.sun.media.sound.WaveFloatFileWriter;

    provides javax.sound.sampled.spi.FormatConversionProvider with
        com.sun.media.sound.AlawCodec,
        com.sun.media.sound.AudioFloatFormatConverter,
        com.sun.media.sound.PCMtoPCMCodec,
        com.sun.media.sound.UlawCodec;

    provides javax.sound.sampled.spi.MixerProvider with
        com.sun.media.sound.DirectAudioDeviceProvider,
        com.sun.media.sound.PortMixerProvider;
}
