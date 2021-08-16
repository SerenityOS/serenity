/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.Locale;

import javax.imageio.IIOException;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;

public class DummyReaderPluginSpi extends ImageReaderSpi {

    private static String [] writerSpiNames =
        {"DummyWriterPluginSpi"};
    public static String[] formatNames = {"test_5076692", "TEST_5076692"};
    public static String[] entensions = {"test_5076692"};
    public static String[] mimeType = {"image/test_5076692"};

    private boolean registered = false;

    public DummyReaderPluginSpi() {
        super("Sun Microsystems, Inc.",
              "1.0",
              formatNames,
              entensions,
              mimeType,
              "DummyPluginReader",
              STANDARD_INPUT_TYPE,
              writerSpiNames,
              false,
              null, null, null, null,
              false,
              "",
              "",
              null, null);
    }

    public void onRegistration(ServiceRegistry registry,
                               Class<?> category) {
        if (registered) {
            return;
        }

        System.getProperty("test.5076692.property", "not found");

        registered = true;
    }

    public String getDescription(Locale locale) {
        return "Standard Dummy Image Reader";
    }

    public boolean canDecodeInput(Object source) throws IOException {
        return false;
    }

    public ImageReader createReaderInstance(Object extension)
        throws IIOException {
        return null;
    }
}
