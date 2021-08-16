/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Per-screen XSETTINGS.
 */
public class XSettings {

    /**
     */
    private long serial = -1;


    /**
     * Update these settings with {@code data} obtained from
     * XSETTINGS manager.
     *
     * @param data settings data obtained from
     *     {@code _XSETTINGS_SETTINGS} window property of the
     *     settings manager.
     * @return a {@code Map} of changed settings.
     */
    public Map<String, Object> update(byte[] data) {
        return (new Update(data)).update();
    }


    /**
     * TBS ...
     */
    class Update {

        /* byte order mark */
        private static final int LITTLE_ENDIAN = 0;
        private static final int BIG_ENDIAN    = 1;

        /* setting type */
        private static final int TYPE_INTEGER = 0;
        private static final int TYPE_STRING  = 1;
        private static final int TYPE_COLOR   = 2;

        private byte[] data;
        private int dlen;
        private int idx;
        private boolean isLittle;
        private long serial = -1;
        private int nsettings = 0;
        private boolean isValid;

        private HashMap<String, Object> updatedSettings;


        /**
         * Construct an Update object for the data read from
         * {@code _XSETTINGS_SETTINGS} property of the XSETTINGS
         * selection owner.
         *
         * @param data {@code _XSETTINGS_SETTINGS} contents.
         */
        Update(byte[] data) {
            this.data = data;

            dlen = data.length;
            if (dlen < 12) {
                // XXX: debug trace?
                return;
            }

            // first byte gives endianness of the data
            // next 3 bytes are unused (pad to 32 bit)
            idx = 0;
            isLittle = (getCARD8() == LITTLE_ENDIAN);

            idx = 4;
            serial = getCARD32();

            // N_SETTINGS is actually CARD32 (i.e. unsigned), but
            // since java doesn't have an unsigned int type, and
            // N_SETTINGS cannot realistically exceed 2^31 (so we
            // gonna use int anyway), just read it as INT32.
            idx = 8;
            nsettings = getINT32();

            updatedSettings = new HashMap<>();

            isValid = true;
        }


        private void needBytes(int n)
            throws IndexOutOfBoundsException
        {
            if (idx + n <= dlen) {
                return;
            }

            throw new IndexOutOfBoundsException("at " + idx
                                                + " need " + n
                                                + " length " + dlen);
        }


        private int getCARD8()
            throws IndexOutOfBoundsException
        {
            needBytes(1);

            int val = data[idx] & 0xff;

            ++idx;
            return val;
        }


        private int getCARD16()
            throws IndexOutOfBoundsException
        {
            needBytes(2);

            int val;
            if (isLittle) {
                val = ((data[idx + 0] & 0xff)      )
                    | ((data[idx + 1] & 0xff) <<  8);
            } else {
                val = ((data[idx + 0] & 0xff) <<  8)
                    | ((data[idx + 1] & 0xff)      );
            }

            idx += 2;
            return val;
        }


        private int getINT32()
            throws IndexOutOfBoundsException
        {
            needBytes(4);

            int val;
            if (isLittle) {
                val = ((data[idx + 0] & 0xff)      )
                    | ((data[idx + 1] & 0xff) <<  8)
                    | ((data[idx + 2] & 0xff) << 16)
                    | ((data[idx + 3] & 0xff) << 24);
            } else {
                val = ((data[idx + 0] & 0xff) << 24)
                    | ((data[idx + 1] & 0xff) << 16)
                    | ((data[idx + 2] & 0xff) <<  8)
                    | ((data[idx + 3] & 0xff) <<  0);
            }

            idx += 4;
            return val;
        }


        private long getCARD32()
            throws IndexOutOfBoundsException
        {
            return getINT32() & 0x00000000ffffffffL;
        }


        private String getString(int len)
            throws IndexOutOfBoundsException
        {
            needBytes(len);

            String str = new String(data, idx, len, UTF_8);

            idx = (idx + len + 3) & ~0x3;
            return str;
        }


        /**
         * Update settings.
         */
        public Map<String, Object> update() {
            if (!isValid) {
                return null;
            }

            synchronized (XSettings.this) {
                long currentSerial = XSettings.this.serial;

                if (this.serial <= currentSerial) {
                    return null;
                }

                for (int i = 0; i < nsettings && idx < dlen; ++i) {
                    updateOne(currentSerial);
                }

                XSettings.this.serial = this.serial;
            }

            return updatedSettings;
        }


        /**
         * Parses a particular x setting.
         *
         * @exception IndexOutOfBoundsException if there isn't enough
         *     data for a setting.
         */
        private void updateOne(long currentSerial)
            throws IndexOutOfBoundsException,
                   IllegalArgumentException
        {
            int type = getCARD8();
            ++idx;              // pad to next CARD16

            // save position of the property name, skip to serial
            int nameLen = getCARD16();
            int nameIdx = idx;

            // check if we should bother
            idx = (idx + nameLen + 3) & ~0x3; // pad to 32 bit
            long lastChanged = getCARD32();

            // Avoid constructing garbage for properties that has not
            // changed, skip the data for this property.
            if (lastChanged <= currentSerial) { // skip
                if (type == TYPE_INTEGER) {
                    idx += 4;
                } else if (type == TYPE_STRING) {
                    int len = getINT32();
                    idx = (idx + len + 3) & ~0x3;
                } else if (type == TYPE_COLOR) {
                    idx += 8;   // 4 CARD16
                } else {
                    throw new IllegalArgumentException("Unknown type: "
                                                       + type);
                }

                return;
            }

            idx = nameIdx;
            String name = getString(nameLen);
            idx += 4;           // skip serial, parsed above

            Object value = null;
            if (type == TYPE_INTEGER) {
                value = Integer.valueOf(getINT32());
            }
            else if (type == TYPE_STRING) {
                value = getString(getINT32());
            }
            else if (type == TYPE_COLOR) {
                int r = getCARD16();
                int g = getCARD16();
                int b = getCARD16();
                int a = getCARD16();

                value = new Color(r / 65535.0f,
                                  g / 65535.0f,
                                  b / 65535.0f,
                                  a / 65535.0f);
            }
            else {
                throw new IllegalArgumentException("Unknown type: " + type);
            }

            if (name == null) {
                // dtrace???
                return;
            }

            updatedSettings.put(name, value);
        }

    } // class XSettings.Update
}
