/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.spi.MixerProvider;

import com.sun.media.sound.JDK13Services;

/**
 * @test
 * @bug 4776511
 * @summary RFE: Setting the default MixerProvider. Test the retrieving of lines
 *          with defaut mixer properties.
 * @modules java.desktop/com.sun.media.sound
 */
public class DefaultMixers {

    private static final String ERROR_PROVIDER_CLASS_NAME = "abc";
    private static final String ERROR_INSTANCE_NAME = "def";

    private static final Class[] lineClasses = {
        SourceDataLine.class,
        TargetDataLine.class,
        Clip.class,
        Port.class,
    };

    public static void main(String[] args) throws Exception {
        boolean allOk = true;
        Mixer.Info[] infos;

        out("Testing Mixers retrieved via AudioSystem");
        infos = AudioSystem.getMixerInfo();
        allOk &= testMixers(infos, null);

        out("Testing MixerProviders");
        List providers = JDK13Services.getProviders(MixerProvider.class);
        for (int i = 0; i < providers.size(); i++) {
            MixerProvider provider = (MixerProvider) providers.get(i);
            infos = provider.getMixerInfo();
            allOk &= testMixers(infos, provider.getClass().getName());
        }

        if (! allOk) {
            throw new Exception("Test failed");
        } else {
            out("Test passed");
        }
    }

    private static boolean testMixers(Mixer.Info[] infos,
                                      String providerClassName) {
        boolean allOk = true;

        for (int i = 0; i < infos.length; i++) {
            Mixer mixer = null;
            try {
                mixer = AudioSystem.getMixer(infos[i]);
            } catch (NullPointerException e) {
                out("Exception thrown; Test NOT failed.");
                e.printStackTrace();
            }
            for (int j = 0; j < lineClasses.length; j++) {
                if (mixer.isLineSupported(new Line.Info(lineClasses[j]))) {
                    allOk &= testMixer(mixer, lineClasses[j],
                                       providerClassName);
                }
            }
        }
        return allOk;
    }

    private static boolean testMixer(Mixer mixer, Class lineType,
                                      String providerClassName) {
        boolean allOk = true;
        String instanceName = mixer.getMixerInfo().getName();

        // no error
        allOk &= testMixer(mixer, lineType,
                           providerClassName, instanceName);

        // erroneous provider class name, correct instance name
        allOk &= testMixer(mixer, lineType,
                           ERROR_PROVIDER_CLASS_NAME, instanceName);

        // erroneous provider class name, no instance name
        allOk &= testMixer(mixer, lineType,
                           ERROR_PROVIDER_CLASS_NAME, "");

        // erroneous provider class name, erroneous instance name
        allOk &= testMixer(mixer, lineType,
                           ERROR_PROVIDER_CLASS_NAME, ERROR_INSTANCE_NAME);

        return allOk;
    }

    private static boolean testMixer(Mixer mixer, Class lineType,
                                     String providerClassName,
                                     String instanceName) {
        boolean allOk = true;

        try {
            String propertyValue = (providerClassName != null) ? providerClassName: "" ;
            propertyValue += "#" + instanceName;
            out("property value: " + propertyValue);
            System.setProperty(lineType.getName(), propertyValue);
            Line line = null;
            Line.Info info = null;
            Line.Info[] infos;
            AudioFormat format = null;
            if (lineType == SourceDataLine.class || lineType == Clip.class) {
                infos = mixer.getSourceLineInfo();
                format = getFirstLinearFormat(infos);
                info = new DataLine.Info(lineType, format);
            } else if (lineType == TargetDataLine.class) {
                infos = mixer.getTargetLineInfo();
                format = getFirstLinearFormat(infos);
                info = new DataLine.Info(lineType, format);
            } else if (lineType == Port.class) {
                /* Actually, a Ports Mixer commonly has source infos
                   as well as target infos. We ignore this here, since we
                   just need a random one. */
                infos = mixer.getSourceLineInfo();
                for (int i = 0; i < infos.length; i++) {
                    if (infos[i] instanceof Port.Info) {
                        info = infos[i];
                        break;
                    }
                }
            }
            out("Line.Info: " + info);
            line = AudioSystem.getLine(info);
            out("line: " + line);
            if (! lineType.isInstance(line)) {
                out("type " + lineType + " failed: class should be '" +
                    lineType + "' but is '" + line.getClass() + "'!");
                allOk = false;
            }
        } catch (Exception e) {
            out("Exception thrown; Test NOT failed.");
            e.printStackTrace();
        }
        return allOk;
    }

    private static AudioFormat getFirstLinearFormat(Line.Info[] infos) {
        for (int i = 0; i < infos.length; i++) {
            if (infos[i] instanceof DataLine.Info) {
                AudioFormat[] formats = ((DataLine.Info) infos[i]).getFormats();
                for (int j = 0; j < formats.length; j++) {
                    AudioFormat.Encoding encoding = formats[j].getEncoding();
                    int sampleSizeInBits = formats[j].getSampleSizeInBits();
                    if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED) &&
                        sampleSizeInBits == 16 ||
                        encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED) &&
                        sampleSizeInBits == 16) {
                        return formats[j];
                    }
                }
            }
        }
        return null;
    }

    private static void out(String message) {
        System.out.println(message);
    }
}
