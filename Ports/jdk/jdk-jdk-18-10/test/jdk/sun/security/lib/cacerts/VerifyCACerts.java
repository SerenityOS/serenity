/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/**
 * @test
 * @bug 8189131 8198240 8191844 8189949 8191031 8196141 8204923 8195774 8199779
 *      8209452 8209506 8210432 8195793 8216577 8222089 8222133 8222137 8222136
 *      8223499 8225392 8232019 8234245 8233223 8225068 8225069 8243321 8243320
 *      8243559 8225072 8258630 8259312 8256421 8225081 8225082
 * @summary Check root CA entries in cacerts file
 */
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;

public class VerifyCACerts {

    private static final String CACERTS
            = System.getProperty("java.home") + File.separator + "lib"
            + File.separator + "security" + File.separator + "cacerts";

    // The numbers of certs now.
    private static final int COUNT = 90;

    // SHA-256 of cacerts, can be generated with
    // shasum -a 256 cacerts | sed -e 's/../&:/g' | tr '[:lower:]' '[:upper:]' | cut -c1-95
    private static final String CHECKSUM
            = "A2:36:27:B7:F6:99:7A:C7:7E:2D:55:47:66:57:08:3F:F8:8C:F2:28:77:29:30:40:A2:D3:6D:1A:B6:C7:32:6F";

    // Hex formatter to upper case with ":" delimiter
    private static final HexFormat HEX = HexFormat.ofDelimiter(":").withUpperCase();

    // map of cert alias to SHA-256 fingerprint
    @SuppressWarnings("serial")
    private static final Map<String, String> FINGERPRINT_MAP = new HashMap<>() {
        {
            put("actalisauthenticationrootca [jdk]",
                    "55:92:60:84:EC:96:3A:64:B9:6E:2A:BE:01:CE:0B:A8:6A:64:FB:FE:BC:C7:AA:B5:AF:C1:55:B3:7F:D7:60:66");
            put("buypassclass2ca [jdk]",
                    "9A:11:40:25:19:7C:5B:B9:5D:94:E6:3D:55:CD:43:79:08:47:B6:46:B2:3C:DF:11:AD:A4:A0:0E:FF:15:FB:48");
            put("buypassclass3ca [jdk]",
                    "ED:F7:EB:BC:A2:7A:2A:38:4D:38:7B:7D:40:10:C6:66:E2:ED:B4:84:3E:4C:29:B4:AE:1D:5B:93:32:E6:B2:4D");
            put("camerfirmachambersca [jdk]",
                    "06:3E:4A:FA:C4:91:DF:D3:32:F3:08:9B:85:42:E9:46:17:D8:93:D7:FE:94:4E:10:A7:93:7E:E2:9D:96:93:C0");
            put("camerfirmachambersignca [jdk]",
                    "13:63:35:43:93:34:A7:69:80:16:A0:D3:24:DE:72:28:4E:07:9D:7B:52:20:BB:8F:BD:74:78:16:EE:BE:BA:CA");
            put("camerfirmachamberscommerceca [jdk]",
                    "0C:25:8A:12:A5:67:4A:EF:25:F2:8B:A7:DC:FA:EC:EE:A3:48:E5:41:E6:F5:CC:4E:E6:3B:71:B3:61:60:6A:C3");
            put("certumca [jdk]",
                    "D8:E0:FE:BC:1D:B2:E3:8D:00:94:0F:37:D2:7D:41:34:4D:99:3E:73:4B:99:D5:65:6D:97:78:D4:D8:14:36:24");
            put("certumtrustednetworkca [jdk]",
                    "5C:58:46:8D:55:F5:8E:49:7E:74:39:82:D2:B5:00:10:B6:D1:65:37:4A:CF:83:A7:D4:A3:2D:B7:68:C4:40:8E");
            put("chunghwaepkirootca [jdk]",
                    "C0:A6:F4:DC:63:A2:4B:FD:CF:54:EF:2A:6A:08:2A:0A:72:DE:35:80:3E:2F:F5:FF:52:7A:E5:D8:72:06:DF:D5");
            put("comodorsaca [jdk]",
                    "52:F0:E1:C4:E5:8E:C6:29:29:1B:60:31:7F:07:46:71:B8:5D:7E:A8:0D:5B:07:27:34:63:53:4B:32:B4:02:34");
            put("comodoaaaca [jdk]",
                    "D7:A7:A0:FB:5D:7E:27:31:D7:71:E9:48:4E:BC:DE:F7:1D:5F:0C:3E:0A:29:48:78:2B:C8:3E:E0:EA:69:9E:F4");
            put("comodoeccca [jdk]",
                    "17:93:92:7A:06:14:54:97:89:AD:CE:2F:8F:34:F7:F0:B6:6D:0F:3A:E3:A3:B8:4D:21:EC:15:DB:BA:4F:AD:C7");
            put("usertrustrsaca [jdk]",
                    "E7:93:C9:B0:2F:D8:AA:13:E2:1C:31:22:8A:CC:B0:81:19:64:3B:74:9C:89:89:64:B1:74:6D:46:C3:D4:CB:D2");
            put("usertrusteccca [jdk]",
                    "4F:F4:60:D5:4B:9C:86:DA:BF:BC:FC:57:12:E0:40:0D:2B:ED:3F:BC:4D:4F:BD:AA:86:E0:6A:DC:D2:A9:AD:7A");
            put("utnuserfirstobjectca [jdk]",
                    "6F:FF:78:E4:00:A7:0C:11:01:1C:D8:59:77:C4:59:FB:5A:F9:6A:3D:F0:54:08:20:D0:F4:B8:60:78:75:E5:8F");
            put("addtrustexternalca [jdk]",
                    "68:7F:A4:51:38:22:78:FF:F0:C8:B1:1F:8D:43:D5:76:67:1C:6E:B2:BC:EA:B4:13:FB:83:D9:65:D0:6D:2F:F2");
            put("addtrustqualifiedca [jdk]",
                    "80:95:21:08:05:DB:4B:BC:35:5E:44:28:D8:FD:6E:C2:CD:E3:AB:5F:B9:7A:99:42:98:8E:B8:F4:DC:D0:60:16");
            put("baltimorecybertrustca [jdk]",
                    "16:AF:57:A9:F6:76:B0:AB:12:60:95:AA:5E:BA:DE:F2:2A:B3:11:19:D6:44:AC:95:CD:4B:93:DB:F3:F2:6A:EB");
            put("digicertglobalrootca [jdk]",
                    "43:48:A0:E9:44:4C:78:CB:26:5E:05:8D:5E:89:44:B4:D8:4F:96:62:BD:26:DB:25:7F:89:34:A4:43:C7:01:61");
            put("digicertglobalrootg2 [jdk]",
                    "CB:3C:CB:B7:60:31:E5:E0:13:8F:8D:D3:9A:23:F9:DE:47:FF:C3:5E:43:C1:14:4C:EA:27:D4:6A:5A:B1:CB:5F");
            put("digicertglobalrootg3 [jdk]",
                    "31:AD:66:48:F8:10:41:38:C7:38:F3:9E:A4:32:01:33:39:3E:3A:18:CC:02:29:6E:F9:7C:2A:C9:EF:67:31:D0");
            put("digicerttrustedrootg4 [jdk]",
                    "55:2F:7B:DC:F1:A7:AF:9E:6C:E6:72:01:7F:4F:12:AB:F7:72:40:C7:8E:76:1A:C2:03:D1:D9:D2:0A:C8:99:88");
            put("digicertassuredidrootca [jdk]",
                    "3E:90:99:B5:01:5E:8F:48:6C:00:BC:EA:9D:11:1E:E7:21:FA:BA:35:5A:89:BC:F1:DF:69:56:1E:3D:C6:32:5C");
            put("digicertassuredidg2 [jdk]",
                    "7D:05:EB:B6:82:33:9F:8C:94:51:EE:09:4E:EB:FE:FA:79:53:A1:14:ED:B2:F4:49:49:45:2F:AB:7D:2F:C1:85");
            put("digicertassuredidg3 [jdk]",
                    "7E:37:CB:8B:4C:47:09:0C:AB:36:55:1B:A6:F4:5D:B8:40:68:0F:BA:16:6A:95:2D:B1:00:71:7F:43:05:3F:C2");
            put("digicerthighassuranceevrootca [jdk]",
                    "74:31:E5:F4:C3:C1:CE:46:90:77:4F:0B:61:E0:54:40:88:3B:A9:A0:1E:D0:0B:A6:AB:D7:80:6E:D3:B1:18:CF");
            put("geotrustglobalca [jdk]",
                    "FF:85:6A:2D:25:1D:CD:88:D3:66:56:F4:50:12:67:98:CF:AB:AA:DE:40:79:9C:72:2D:E4:D2:B5:DB:36:A7:3A");
            put("geotrustprimaryca [jdk]",
                    "37:D5:10:06:C5:12:EA:AB:62:64:21:F1:EC:8C:92:01:3F:C5:F8:2A:E9:8E:E5:33:EB:46:19:B8:DE:B4:D0:6C");
            put("geotrustprimarycag2 [jdk]",
                    "5E:DB:7A:C4:3B:82:A0:6A:87:61:E8:D7:BE:49:79:EB:F2:61:1F:7D:D7:9B:F9:1C:1C:6B:56:6A:21:9E:D7:66");
            put("geotrustprimarycag3 [jdk]",
                    "B4:78:B8:12:25:0D:F8:78:63:5C:2A:A7:EC:7D:15:5E:AA:62:5E:E8:29:16:E2:CD:29:43:61:88:6C:D1:FB:D4");
            put("geotrustuniversalca [jdk]",
                    "A0:45:9B:9F:63:B2:25:59:F5:FA:5D:4C:6D:B3:F9:F7:2F:F1:93:42:03:35:78:F0:73:BF:1D:1B:46:CB:B9:12");
            put("thawteprimaryrootca [jdk]",
                    "8D:72:2F:81:A9:C1:13:C0:79:1D:F1:36:A2:96:6D:B2:6C:95:0A:97:1D:B4:6B:41:99:F4:EA:54:B7:8B:FB:9F");
            put("thawteprimaryrootcag2 [jdk]",
                    "A4:31:0D:50:AF:18:A6:44:71:90:37:2A:86:AF:AF:8B:95:1F:FB:43:1D:83:7F:1E:56:88:B4:59:71:ED:15:57");
            put("thawteprimaryrootcag3 [jdk]",
                    "4B:03:F4:58:07:AD:70:F2:1B:FC:2C:AE:71:C9:FD:E4:60:4C:06:4C:F5:FF:B6:86:BA:E5:DB:AA:D7:FD:D3:4C");
            put("verisignuniversalrootca [jdk]",
                    "23:99:56:11:27:A5:71:25:DE:8C:EF:EA:61:0D:DF:2F:A0:78:B5:C8:06:7F:4E:82:82:90:BF:B8:60:E8:4B:3C");
            put("verisignclass3g3ca [jdk]",
                    "EB:04:CF:5E:B1:F3:9A:FA:76:2F:2B:B1:20:F2:96:CB:A5:20:C1:B9:7D:B1:58:95:65:B8:1C:B9:A1:7B:72:44");
            put("verisignclass3g4ca [jdk]",
                    "69:DD:D7:EA:90:BB:57:C9:3E:13:5D:C8:5E:A6:FC:D5:48:0B:60:32:39:BD:C4:54:FC:75:8B:2A:26:CF:7F:79");
            put("verisignclass3g5ca [jdk]",
                    "9A:CF:AB:7E:43:C8:D8:80:D0:6B:26:2A:94:DE:EE:E4:B4:65:99:89:C3:D0:CA:F1:9B:AF:64:05:E4:1A:B7:DF");
            put("dtrustclass3ca2 [jdk]",
                    "49:E7:A4:42:AC:F0:EA:62:87:05:00:54:B5:25:64:B6:50:E4:F4:9E:42:E3:48:D6:AA:38:E0:39:E9:57:B1:C1");
            put("dtrustclass3ca2ev [jdk]",
                    "EE:C5:49:6B:98:8C:E9:86:25:B9:34:09:2E:EC:29:08:BE:D0:B0:F3:16:C2:D4:73:0C:84:EA:F1:F3:D3:48:81");
            put("identrustpublicca [jdk]",
                    "30:D0:89:5A:9A:44:8A:26:20:91:63:55:22:D1:F5:20:10:B5:86:7A:CA:E1:2C:78:EF:95:8F:D4:F4:38:9F:2F");
            put("identrustcommercial [jdk]",
                    "5D:56:49:9B:E4:D2:E0:8B:CF:CA:D0:8A:3E:38:72:3D:50:50:3B:DE:70:69:48:E4:2F:55:60:30:19:E5:28:AE");
            put("letsencryptisrgx1 [jdk]",
                    "96:BC:EC:06:26:49:76:F3:74:60:77:9A:CF:28:C5:A7:CF:E8:A3:C0:AA:E1:1A:8F:FC:EE:05:C0:BD:DF:08:C6");
            put("luxtrustglobalrootca [jdk]",
                    "A1:B2:DB:EB:64:E7:06:C6:16:9E:3C:41:18:B2:3B:AA:09:01:8A:84:27:66:6D:8B:F0:E2:88:91:EC:05:19:50");
            put("quovadisrootca [jdk]",
                    "A4:5E:DE:3B:BB:F0:9C:8A:E1:5C:72:EF:C0:72:68:D6:93:A2:1C:99:6F:D5:1E:67:CA:07:94:60:FD:6D:88:73");
            put("quovadisrootca1g3 [jdk]",
                    "8A:86:6F:D1:B2:76:B5:7E:57:8E:92:1C:65:82:8A:2B:ED:58:E9:F2:F2:88:05:41:34:B7:F1:F4:BF:C9:CC:74");
            put("quovadisrootca2 [jdk]",
                    "85:A0:DD:7D:D7:20:AD:B7:FF:05:F8:3D:54:2B:20:9D:C7:FF:45:28:F7:D6:77:B1:83:89:FE:A5:E5:C4:9E:86");
            put("quovadisrootca2g3 [jdk]",
                    "8F:E4:FB:0A:F9:3A:4D:0D:67:DB:0B:EB:B2:3E:37:C7:1B:F3:25:DC:BC:DD:24:0E:A0:4D:AF:58:B4:7E:18:40");
            put("quovadisrootca3 [jdk]",
                    "18:F1:FC:7F:20:5D:F8:AD:DD:EB:7F:E0:07:DD:57:E3:AF:37:5A:9C:4D:8D:73:54:6B:F4:F1:FE:D1:E1:8D:35");
            put("quovadisrootca3g3 [jdk]",
                    "88:EF:81:DE:20:2E:B0:18:45:2E:43:F8:64:72:5C:EA:5F:BD:1F:C2:D9:D2:05:73:07:09:C5:D8:B8:69:0F:46");
            put("secomscrootca1 [jdk]",
                    "E7:5E:72:ED:9F:56:0E:EC:6E:B4:80:00:73:A4:3F:C3:AD:19:19:5A:39:22:82:01:78:95:97:4A:99:02:6B:6C");
            put("secomscrootca2 [jdk]",
                    "51:3B:2C:EC:B8:10:D4:CD:E5:DD:85:39:1A:DF:C6:C2:DD:60:D8:7B:B7:36:D2:B5:21:48:4A:A4:7A:0E:BE:F6");
            put("swisssigngoldg2ca [jdk]",
                    "62:DD:0B:E9:B9:F5:0A:16:3E:A0:F8:E7:5C:05:3B:1E:CA:57:EA:55:C8:68:8F:64:7C:68:81:F2:C8:35:7B:95");
            put("swisssignplatinumg2ca [jdk]",
                    "3B:22:2E:56:67:11:E9:92:30:0D:C0:B1:5A:B9:47:3D:AF:DE:F8:C8:4D:0C:EF:7D:33:17:B4:C1:82:1D:14:36");
            put("swisssignsilverg2ca [jdk]",
                    "BE:6C:4D:A2:BB:B9:BA:59:B6:F3:93:97:68:37:42:46:C3:C0:05:99:3F:A9:8F:02:0D:1D:ED:BE:D4:8A:81:D5");
            put("securetrustca [jdk]",
                    "F1:C1:B5:0A:E5:A2:0D:D8:03:0E:C9:F6:BC:24:82:3D:D3:67:B5:25:57:59:B4:E7:1B:61:FC:E9:F7:37:5D:73");
            put("xrampglobalca [jdk]",
                    "CE:CD:DC:90:50:99:D8:DA:DF:C5:B1:D2:09:B7:37:CB:E2:C1:8C:FB:2C:10:C0:FF:0B:CF:0D:32:86:FC:1A:A2");
            put("godaddyrootg2ca [jdk]",
                    "45:14:0B:32:47:EB:9C:C8:C5:B4:F0:D7:B5:30:91:F7:32:92:08:9E:6E:5A:63:E2:74:9D:D3:AC:A9:19:8E:DA");
            put("godaddyclass2ca [jdk]",
                    "C3:84:6B:F2:4B:9E:93:CA:64:27:4C:0E:C6:7C:1E:CC:5E:02:4F:FC:AC:D2:D7:40:19:35:0E:81:FE:54:6A:E4");
            put("starfieldclass2ca [jdk]",
                    "14:65:FA:20:53:97:B8:76:FA:A6:F0:A9:95:8E:55:90:E4:0F:CC:7F:AA:4F:B7:C2:C8:67:75:21:FB:5F:B6:58");
            put("starfieldrootg2ca [jdk]",
                    "2C:E1:CB:0B:F9:D2:F9:E1:02:99:3F:BE:21:51:52:C3:B2:DD:0C:AB:DE:1C:68:E5:31:9B:83:91:54:DB:B7:F5");
            put("entrustrootcaec1 [jdk]",
                    "02:ED:0E:B2:8C:14:DA:45:16:5C:56:67:91:70:0D:64:51:D7:FB:56:F0:B2:AB:1D:3B:8E:B0:70:E5:6E:DF:F5");
            put("entrust2048ca [jdk]",
                    "6D:C4:71:72:E0:1C:BC:B0:BF:62:58:0D:89:5F:E2:B8:AC:9A:D4:F8:73:80:1E:0C:10:B9:C8:37:D2:1E:B1:77");
            put("entrustrootcag2 [jdk]",
                    "43:DF:57:74:B0:3E:7F:EF:5F:E4:0D:93:1A:7B:ED:F1:BB:2E:6B:42:73:8C:4E:6D:38:41:10:3D:3A:A7:F3:39");
            put("entrustevca [jdk]",
                    "73:C1:76:43:4F:1B:C6:D5:AD:F4:5B:0E:76:E7:27:28:7C:8D:E5:76:16:C1:E6:E6:14:1A:2B:2C:BC:7D:8E:4C");
            put("affirmtrustnetworkingca [jdk]",
                    "0A:81:EC:5A:92:97:77:F1:45:90:4A:F3:8D:5D:50:9F:66:B5:E2:C5:8F:CD:B5:31:05:8B:0E:17:F3:F0:B4:1B");
            put("affirmtrustpremiumca [jdk]",
                    "70:A7:3F:7F:37:6B:60:07:42:48:90:45:34:B1:14:82:D5:BF:0E:69:8E:CC:49:8D:F5:25:77:EB:F2:E9:3B:9A");
            put("affirmtrustcommercialca [jdk]",
                    "03:76:AB:1D:54:C5:F9:80:3C:E4:B2:E2:01:A0:EE:7E:EF:7B:57:B6:36:E8:A9:3C:9B:8D:48:60:C9:6F:5F:A7");
            put("affirmtrustpremiumeccca [jdk]",
                    "BD:71:FD:F6:DA:97:E4:CF:62:D1:64:7A:DD:25:81:B0:7D:79:AD:F8:39:7E:B4:EC:BA:9C:5E:84:88:82:14:23");
            put("ttelesecglobalrootclass3ca [jdk]",
                    "FD:73:DA:D3:1C:64:4F:F1:B4:3B:EF:0C:CD:DA:96:71:0B:9C:D9:87:5E:CA:7E:31:70:7A:F3:E9:6D:52:2B:BD");
            put("ttelesecglobalrootclass2ca [jdk]",
                    "91:E2:F5:78:8D:58:10:EB:A7:BA:58:73:7D:E1:54:8A:8E:CA:CD:01:45:98:BC:0B:14:3E:04:1B:17:05:25:52");
            put("starfieldservicesrootg2ca [jdk]",
                    "56:8D:69:05:A2:C8:87:08:A4:B3:02:51:90:ED:CF:ED:B1:97:4A:60:6A:13:C6:E5:29:0F:CB:2A:E6:3E:DA:B5");
            put("globalsignca [jdk]",
                    "EB:D4:10:40:E4:BB:3E:C7:42:C9:E3:81:D3:1E:F2:A4:1A:48:B6:68:5C:96:E7:CE:F3:C1:DF:6C:D4:33:1C:99");
            put("globalsignr3ca [jdk]",
                    "CB:B5:22:D7:B7:F1:27:AD:6A:01:13:86:5B:DF:1C:D4:10:2E:7D:07:59:AF:63:5A:7C:F4:72:0D:C9:63:C5:3B");
            put("globalsigneccrootcar5 [jdk]",
                    "17:9F:BC:14:8A:3D:D0:0F:D2:4E:A1:34:58:CC:43:BF:A7:F5:9C:81:82:D7:83:A5:13:F6:EB:EC:10:0C:89:24");
            put("globalsigneccrootcar4 [jdk]",
                    "BE:C9:49:11:C2:95:56:76:DB:6C:0A:55:09:86:D7:6E:3B:A0:05:66:7C:44:2C:97:62:B4:FB:B7:73:DE:22:8C");
            put("globalsignr2ca [jdk]",
                    "CA:42:DD:41:74:5F:D0:B8:1E:B9:02:36:2C:F9:D8:BF:71:9D:A1:BD:1B:1E:FC:94:6F:5B:4C:99:F4:2C:1B:9E");
            put("teliasonerarootcav1 [jdk]",
                    "DD:69:36:FE:21:F8:F0:77:C1:23:A1:A5:21:C1:22:24:F7:22:55:B7:3E:03:A7:26:06:93:E8:A2:4B:0F:A3:89");
            put("globalsignrootcar6 [jdk]",
                    "2C:AB:EA:FE:37:D0:6C:A2:2A:BA:73:91:C0:03:3D:25:98:29:52:C4:53:64:73:49:76:3A:3A:B5:AD:6C:CF:69");
            put("luxtrustglobalroot2ca [jdk]",
                    "54:45:5F:71:29:C2:0B:14:47:C4:18:F9:97:16:8F:24:C5:8F:C5:02:3B:F5:DA:5B:E2:EB:6E:1D:D8:90:2E:D5");
            put("amazonrootca1 [jdk]",
                    "8E:CD:E6:88:4F:3D:87:B1:12:5B:A3:1A:C3:FC:B1:3D:70:16:DE:7F:57:CC:90:4F:E1:CB:97:C6:AE:98:19:6E");
            put("amazonrootca2 [jdk]",
                    "1B:A5:B2:AA:8C:65:40:1A:82:96:01:18:F8:0B:EC:4F:62:30:4D:83:CE:C4:71:3A:19:C3:9C:01:1E:A4:6D:B4");
            put("amazonrootca3 [jdk]",
                    "18:CE:6C:FE:7B:F1:4E:60:B2:E3:47:B8:DF:E8:68:CB:31:D0:2E:BB:3A:DA:27:15:69:F5:03:43:B4:6D:B3:A4");
            put("amazonrootca4 [jdk]",
                    "E3:5D:28:41:9E:D0:20:25:CF:A6:90:38:CD:62:39:62:45:8D:A5:C6:95:FB:DE:A3:C2:2B:0B:FB:25:89:70:92");
            put("entrustrootcag4 [jdk]",
                    "DB:35:17:D1:F6:73:2A:2D:5A:B9:7C:53:3E:C7:07:79:EE:32:70:A6:2F:B4:AC:42:38:37:24:60:E6:F0:1E:88");
            put("sslrootrsaca [jdk]",
                    "85:66:6A:56:2E:E0:BE:5C:E9:25:C1:D8:89:0A:6F:76:A8:7E:C1:6D:4D:7D:5F:29:EA:74:19:CF:20:12:3B:69");
            put("sslrootevrsaca [jdk]",
                    "2E:7B:F1:6C:C2:24:85:A7:BB:E2:AA:86:96:75:07:61:B0:AE:39:BE:3B:2F:E9:D0:CC:6D:4E:F7:34:91:42:5C");
            put("sslrooteccca [jdk]",
                    "34:17:BB:06:CC:60:07:DA:1B:96:1C:92:0B:8A:B4:CE:3F:AD:82:0E:4A:A3:0B:9A:CB:C4:A7:4E:BD:CE:BC:65");
            put("haricarootca2015 [jdk]",
                    "A0:40:92:9A:02:CE:53:B4:AC:F4:F2:FF:C6:98:1C:E4:49:6F:75:5E:6D:45:FE:0B:2A:69:2B:CD:52:52:3F:36");
            put("haricaeccrootca2015 [jdk]",
                    "44:B5:45:AA:8A:25:E6:5A:73:CA:15:DC:27:FC:36:D2:4C:1C:B9:95:3A:06:65:39:B1:15:82:DC:48:7B:48:33");
        }
    };

    // Exception list to 90 days expiry policy
    // No error will be reported if certificate in this list expires
    @SuppressWarnings("serial")
    private static final HashSet<String> EXPIRY_EXC_ENTRIES = new HashSet<>() {
        {
            // Valid until: Tue Jul 09 14:40:36 EDT 2019
            add("utnuserfirstobjectca [jdk]");
            // Valid until: Sat May 30 10:38:31 GMT 2020
            add("addtrustexternalca [jdk]");
            // Valid until: Sat May 30 10:44:50 GMT 2020
            add("addtrustqualifiedca [jdk]");
            // Valid until: Wed Mar 17 02:51:37 PDT 2021
            add("luxtrustglobalrootca [jdk]");
            // Valid until: Wed Mar 17 11:33:33 PDT 2021
            add("quovadisrootca [jdk]");
        }
    };

    // Ninety days in milliseconds
    private static final long NINETY_DAYS = 7776000000L;

    private static boolean atLeastOneFailed = false;

    private static MessageDigest md;

    public static void main(String[] args) throws Exception {
        System.out.println("cacerts file: " + CACERTS);
        md = MessageDigest.getInstance("SHA-256");

        byte[] data = Files.readAllBytes(Path.of(CACERTS));
        String checksum = HEX.formatHex(md.digest(data));
        if (!checksum.equals(CHECKSUM)) {
            atLeastOneFailed = true;
            System.err.println("ERROR: wrong checksum\n" + checksum);
            System.err.println("Expected checksum\n" + CHECKSUM);
        }

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new ByteArrayInputStream(data), "changeit".toCharArray());

        // check the count of certs inside
        if (ks.size() != COUNT) {
            atLeastOneFailed = true;
            System.err.println("ERROR: " + ks.size() + " entries, should be "
                    + COUNT);
        }

        // check that all entries in the map are in the keystore
        for (String alias : FINGERPRINT_MAP.keySet()) {
            if (!ks.isCertificateEntry(alias)) {
                atLeastOneFailed = true;
                System.err.println("ERROR: " + alias + " is not in cacerts");
            }
        }

        // pull all the trusted self-signed CA certs out of the cacerts file
        // and verify their signatures
        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            System.out.println("\nVerifying " + alias);
            if (!ks.isCertificateEntry(alias)) {
                atLeastOneFailed = true;
                System.err.println("ERROR: " + alias
                        + " is not a trusted cert entry");
            }
            X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
            if (!checkFingerprint(alias, cert)) {
                atLeastOneFailed = true;
                System.err.println("ERROR: " + alias + " SHA-256 fingerprint is incorrect");
            }
            // Make sure cert can be self-verified
            try {
                cert.verify(cert.getPublicKey());
            } catch (Exception e) {
                atLeastOneFailed = true;
                System.err.println("ERROR: cert cannot be verified:"
                        + e.getMessage());
            }

            // Make sure cert is not expired or not yet valid
            try {
                cert.checkValidity();
            } catch (CertificateExpiredException cee) {
                if (!EXPIRY_EXC_ENTRIES.contains(alias)) {
                    atLeastOneFailed = true;
                    System.err.println("ERROR: cert is expired");
                }
            } catch (CertificateNotYetValidException cne) {
                atLeastOneFailed = true;
                System.err.println("ERROR: cert is not yet valid");
            }

            // If cert is within 90 days of expiring, mark as failure so
            // that cert can be scheduled to be removed/renewed.
            Date notAfter = cert.getNotAfter();
            if (notAfter.getTime() - System.currentTimeMillis() < NINETY_DAYS) {
                if (!EXPIRY_EXC_ENTRIES.contains(alias)) {
                    atLeastOneFailed = true;
                    System.err.println("ERROR: cert \"" + alias + "\" expiry \""
                            + notAfter.toString() + "\" will expire within 90 days");
                }
            }
        }

        if (atLeastOneFailed) {
            throw new Exception("At least one cacert test failed");
        }
    }

    private static boolean checkFingerprint(String alias, Certificate cert)
            throws Exception {
        String fingerprint = FINGERPRINT_MAP.get(alias);
        if (fingerprint == null) {
            // no entry for alias
            return true;
        }
        System.out.println("Checking fingerprint of " + alias);
        byte[] digest = md.digest(cert.getEncoded());
        return fingerprint.equals(HEX.formatHex(digest));
    }

}
