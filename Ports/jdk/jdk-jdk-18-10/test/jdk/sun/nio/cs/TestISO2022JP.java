/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4626545 4879522 4913711 4119445 8042125 8211382
 * @summary Check full coverage encode/decode for ISO-2022-JP
 * @modules jdk.charsets
 */

/*
 * Tests the NIO converter for J2RE >= 1.4.1
 * since the default converter used by String
 * API is the NIO converter sun.nio.cs.ext.ISO2022_JP
 */

import java.io.*;
import java.util.Arrays;

public class TestISO2022JP {

    private final static String US_ASCII =
        "\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007" +
        "\b\t\n\u000B\f\r" +
        "\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017" +
        "\u0018\u0019\u001A\u001C\u001D\u001E\u001F" +
        "\u0020\u0021\"\u0023\u0024\u0025\u0026\'" +
        "\u0028\u0029\u002A\u002B\u002C\u002D\u002E\u002F" +
        "\u0030\u0031\u0032\u0033\u0034\u0035\u0036\u0037" +
        "\u0038\u0039\u003A\u003B\u003C\u003D\u003E\u003F" +
        "\u0040\u0041\u0042\u0043\u0044\u0045\u0046\u0047" +
        "\u0048\u0049\u004A\u004B\u004C\u004D\u004E\u004F" +
        "\u0050\u0051\u0052\u0053\u0054\u0055\u0056\u0057" +
        "\u0058\u0059\u005A\u005B\\\u005D\u005E\u005F" +
        "\u0060\u0061\u0062\u0063\u0064\u0065\u0066\u0067" +
        "\u0068\u0069\u006A\u006B\u006C\u006D\u006E\u006F" +
        "\u0070\u0071\u0072\u0073\u0074\u0075\u0076\u0077" +
        "\u0078\u0079\u007A\u007B\u007C\u007D\u007E\u00A5\u203E";

     // Subset of chars sourced from JISX0208:1983

     private final static String JISX0208SUBSET =
        "u3000\u3001\u3002\uFF0C\uFF0E\u30FB\uFF1A" +
        "\uFF1B\uFF1F\uFF01\u309B\u309C\u00B4\uFF40\u00A8" +
        "\uFF3E\uFFE3\uFF3F\u30FD\u30FE\u309D\u309E\u3003" +
        "\u4EDD\u3005\u3006\u3007\u30FC\u2014\u2010\uFF0F" +
        "\uFF3C\u301C\u2016\uFF5C\u2026\u2025\u2018\u2019" +
        "\u5C05\u5C07\u5C08\u5C0D\u5C13\u5C20\u5C22\u5C28" +
        "\u5C38\u5C39\u5C41\u5C46\u5C4E\u5C53\u5C50\u5C4F" +
        "\u5B71\u5C6C\u5C6E\u4E62\u5C76\u5C79\u5C8C\u5C91" +
        "\u5C94\u599B\u5CAB\u5CBB\u5CB6\u5CBC\u5CB7\u5CC5" +
        "\u5CBE\u5CC7\u5CD9\u5CE9\u5CFD\u5CFA\u5CED\u5D8C" +
        "\u5CEA\u5D0B\u5D15\u5D17\u5D5C\u5D1F\u5D1B\u5D11" +
        "\u5D14\u5D22\u5D1A\u5D19\u5D18\u5D4C\u5D52\u5D4E" +
        "\u5D4B\u5D6C\u5D73\u5D76\u5D87\u5D84\u5D82\u5DA2" +
        "\u5D9D\u5DAC\u5DAE\u5DBD\u5D90\u5DB7\u5DBC\u5DC9" +
        "\u5DCD\u5DD3\u5DD2\u5DD6\u5DDB\u5DEB\u5DF2\u5DF5" +
        "\u5E0B\u5E1A\u5E19\u5E11\u5E1B\u5E36\u5E37\u5E44" +
        "\u5E43\u5E40\u5E4E\u5E57\u5E54\u5E5F\u5E62\u5E64" +
        "\u5E47\u5E75\u5E76\u5E7A\u9EBC\u5E7F\u5EA0\u5EC1" +
        "\u5EC2\u5EC8\u5ED0\u5ECF\u5ED6\u5EE3\u5EDD\u5EDA" +
        "\u5EDB\u5EE2\u5EE1\u5EE8\u5EE9\u5EEC\u5EF1\u5EF3" +
        "\u5EF0\u5EF4\u5EF8\u5EFE\u5F03\u5F09\u5F5D\u5F5C" +
        "\u5F0B\u5F11\u5F16\u5F29\u5F2D\u5F38\u5F41\u5F48" +
        "\u5F4C\u5F4E\u5F2F\u5F51\u5F56\u5F57\u5F59\u5F61" +
        "\u5F6D\u5F73\u5F77\u5F83\u5F82\u5F7F\u5F8A\u5F88" +
        "\u5F91\u5F87\u5F9E\u5F99\u5F98\u5FA0\u5FA8\u5FAD" +
        "\u5FBC\u5FD6\u5FFB\u5FE4\u5FF8\u5FF1\u5FDD\u60B3" +
        "\u5FFF\u6021\u6060\u6019\u6010\u6029\u600E\u6031" +
        "\u62EE\u62F1\u6327\u6302\u6308\u62EF\u62F5\u6350" +
        "\u633E\u634D\u641C\u634F\u6396\u638E\u6380\u63AB" +
        "\u6376\u63A3\u638F\u6389\u639F\u63B5\u636B\u6369" +
        "\u63BE\u63E9\u63C0\u63C6\u63E3\u63C9\u63D2\u63F6" +
        "\u63C4\u6416\u6434\u6406\u6413\u6426\u6436\u651D" +
        "\u6417\u6428\u640F\u6467\u646F\u6476\u644E\u652A" +
        "\u6495\u6493\u64A5\u64A9\u6488\u64BC\u64DA\u64D2" +
        "\u64C5\u64C7\u64BB\u64D8\u64C2\u64F1\u64E7\u8209" +
        "\u64E0\u64E1\u62AC\u64E3\u64EF\u652C\u64F6\u64F4" +
        "\u64F2\u64FA\u6500\u64FD\u6518\u651C\u6505\u6524" +
        "\u6523\u652B\u6534\u6535\u6537\u6536\u6538\u754B" +
        "\u6741\u6738\u6737\u6746\u675E\u6760\u6759\u6763" +
        "\u6764\u6789\u6770\u67A9\u677C\u676A\u678C\u678B" +
        "\u67A6\u67A1\u6785\u67B7\u67EF\u67B4\u67EC\u67B3" +
        "\u67E9\u67B8\u67E4\u67DE\u67DD\u67E2\u67EE\u67B9" +
        "\u67CE\u67C6\u67E7\u6A9C\u681E\u6846\u6829\u6840" +
        "\u684D\u6832\u684E\u68B3\u682B\u6859\u6863\u6877" +
        "\u687F\u689F\u688F\u68AD\u6894\u689D\u689B\u6883" +
        "\u6AAE\u68B9\u6874\u68B5\u68A0\u68BA\u690F\u688D" +
        "\u687E\u6901\u68CA\u6908\u68D8\u6922\u6926\u68E1" +
        "\u690C\u68CD\u68D4\u68E7\u68D5\u6936\u6912\u6904" +
        "\u68D7\u68E3\u6925\u68F9\u68E0\u68EF\u6928\u692A" +
        "\u691A\u6923\u6921\u68C6\u6979\u6977\u695C\u6978" +
        "\u6CD7\u6CC5\u6CDD\u6CAE\u6CB1\u6CBE\u6CBA\u6CDB" +
        "\u6CEF\u6CD9\u6CEA\u6D1F\u884D\u6D36\u6D2B\u6D3D" +
        "\u6D38\u6D19\u6D35\u6D33\u6D12\u6D0C\u6D63\u6D93" +
        "\u6D64\u6D5A\u6D79\u6D59\u6D8E\u6D95\u6FE4\u6D85" +
        "\u6DF9\u6E15\u6E0A\u6DB5\u6DC7\u6DE6\u6DB8\u6DC6" +
        "\u6DEC\u6DDE\u6DCC\u6DE8\u6DD2\u6DC5\u6DFA\u6DD9" +
        "\u724B\u7258\u7274\u727E\u7282\u7281\u7287\u7292" +
        "\u7296\u72A2\u72A7\u72B9\u72B2\u72C3\u72C6\u72C4" +
        "\u9D59\u9D72\u9D89\u9D87\u9DAB\u9D6F\u9D7A\u9D9A" +
        "\u9DA4\u9DA9\u9DB2\u9DC4\u9DC1\u9DBB\u9DB8\u9DBA" +
        "\u9DC6\u9DCF\u9DC2\u9DD9\u9DD3\u9DF8\u9DE6\u9DED" +
        "\u9DEF\u9DFD\u9E1A\u9E1B\u9E1E\u9E75\u9E79\u9E7D" +
        "\u9E81\u9E88\u9E8B\u9E8C\u9E92\u9E95\u9E91\u9E9D" +
        "\u9EA5\u9EA9\u9EB8\u9EAA\u9EAD\u9761\u9ECC\u9ECE" +
        "\u9ECF\u9ED0\u9ED4\u9EDC\u9EDE\u9EDD\u9EE0\u9EE5" +
        "\u9EE8\u9EEF\u9EF4\u9EF6\u9EF7\u9EF9\u9EFB\u9EFC" +
        "\u9EFD\u9F07\u9F08\u76B7\u9F15\u9F21\u9F2C\u9F3E" +
        "\u9F4A\u9F52\u9F54\u9F63\u9F5F\u9F60\u9F61\u9F66" +
        "\u9F67\u9F6C\u9F6A\u9F77\u9F72\u9F76\u9F95\u9F9C" +
        "\u9FA0\u582F\u69C7\u9059\u7464\u51DC\u7199";

    final static String JISX0202KATAKANA =
        "\uFF61\uFF62\uFF63\uFF64" +
        "\uFF65\uFF66\uFF67\uFF68\uFF69\uFF6A\uFF6B\uFF6C" +
        "\uFF6D\uFF6E\uFF6F\uFF70\uFF71\uFF72\uFF73\uFF74" +
        "\uFF75\uFF76\uFF77\uFF78\uFF79\uFF7A\uFF7B\uFF7C" +
        "\uFF7D\uFF7E\uFF7F\uFF80\uFF81\uFF82\uFF83\uFF84" +
        "\uFF85\uFF86\uFF87\uFF88\uFF89\uFF8A\uFF8B\uFF8C" +
        "\uFF8D\uFF8E\uFF8F\uFF90\uFF91\uFF92\uFF93\uFF94" +
        "\uFF95\uFF96\uFF97\uFF98\uFF99\uFF9A\uFF9B\uFF9C" +
        "\uFF9D\uFF9E\uFF9F";


    final static byte[] expectedBytes1 = {
        (byte) 0x0, (byte) 0x1, (byte) 0x2, (byte) 0x3,
        (byte) 0x4, (byte) 0x5, (byte) 0x6, (byte) 0x7,
        (byte) 0x8, (byte) 0x9, (byte) 0xa, (byte) 0xb,
        (byte) 0xc, (byte) 0xd,
        (byte) 0x10, (byte) 0x11, (byte) 0x12, (byte) 0x13,
        (byte) 0x14, (byte) 0x15, (byte) 0x16, (byte) 0x17,
        (byte) 0x18, (byte) 0x19, (byte) 0x1a,
        (byte) 0x1c, (byte) 0x1d, (byte) 0x1e, (byte) 0x1f,
        (byte) 0x20, (byte) 0x21, (byte) 0x22, (byte) 0x23,
        (byte) 0x24, (byte) 0x25, (byte) 0x26, (byte) 0x27,
        (byte) 0x28, (byte) 0x29, (byte) 0x2a, (byte) 0x2b,
        (byte) 0x2c, (byte) 0x2d, (byte) 0x2e, (byte) 0x2f,
        (byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33,
        (byte) 0x34, (byte) 0x35, (byte) 0x36, (byte) 0x37,
        (byte) 0x38, (byte) 0x39, (byte) 0x3a, (byte) 0x3b,
        (byte) 0x3c, (byte) 0x3d, (byte) 0x3e, (byte) 0x3f,
        (byte) 0x40, (byte) 0x41, (byte) 0x42, (byte) 0x43,
        (byte) 0x44, (byte) 0x45, (byte) 0x46, (byte) 0x47,
        (byte) 0x48, (byte) 0x49, (byte) 0x4a, (byte) 0x4b,
        (byte) 0x4c, (byte) 0x4d, (byte) 0x4e, (byte) 0x4f,
        (byte) 0x50, (byte) 0x51, (byte) 0x52, (byte) 0x53,
        (byte) 0x54, (byte) 0x55, (byte) 0x56, (byte) 0x57,
        (byte) 0x58, (byte) 0x59, (byte) 0x5a, (byte) 0x5b,
        (byte) 0x5c, (byte) 0x5d, (byte) 0x5e, (byte) 0x5f,
        (byte) 0x60, (byte) 0x61, (byte) 0x62, (byte) 0x63,
        (byte) 0x64, (byte) 0x65, (byte) 0x66, (byte) 0x67,
        (byte) 0x68, (byte) 0x69, (byte) 0x6a, (byte) 0x6b,
        (byte) 0x6c, (byte) 0x6d, (byte) 0x6e, (byte) 0x6f,
        (byte) 0x70, (byte) 0x71, (byte) 0x72, (byte) 0x73,
        (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77,
        (byte) 0x78, (byte) 0x79, (byte) 0x7a, (byte) 0x7b,
        (byte) 0x7c, (byte) 0x7d, (byte) 0x7e, (byte) 0x1b,
        (byte) 0x28, (byte) 0x4a, (byte) 0x5c, (byte) 0x7e,
        (byte) 0x1b, (byte) 0x28, (byte) 0x42, (byte) 0x75,
        (byte) 0x33, (byte) 0x30, (byte) 0x30, (byte) 0x30,
        (byte) 0x1b, (byte) 0x24, (byte) 0x42, (byte) 0x21,
        (byte) 0x22, (byte) 0x21, (byte) 0x23, (byte) 0x21,
        (byte) 0x24, (byte) 0x21, (byte) 0x25, (byte) 0x21,
        (byte) 0x26, (byte) 0x21, (byte) 0x27, (byte) 0x21,
        (byte) 0x28, (byte) 0x21, (byte) 0x29, (byte) 0x21,
        (byte) 0x2a, (byte) 0x21, (byte) 0x2b, (byte) 0x21,
        (byte) 0x2c, (byte) 0x21, (byte) 0x2d, (byte) 0x21,
        (byte) 0x2e, (byte) 0x21, (byte) 0x2f, (byte) 0x21,
        (byte) 0x30, (byte) 0x21, (byte) 0x31, (byte) 0x21,
        (byte) 0x32, (byte) 0x21, (byte) 0x33, (byte) 0x21,
        (byte) 0x34, (byte) 0x21, (byte) 0x35, (byte) 0x21,
        (byte) 0x36, (byte) 0x21, (byte) 0x37, (byte) 0x21,
        (byte) 0x38, (byte) 0x21, (byte) 0x39, (byte) 0x21,
        (byte) 0x3a, (byte) 0x21, (byte) 0x3b, (byte) 0x21,
        (byte) 0x3c, (byte) 0x21, (byte) 0x3d, (byte) 0x21,
        (byte) 0x3e, (byte) 0x21, (byte) 0x3f, (byte) 0x21,
        (byte) 0x40, (byte) 0x21, (byte) 0x41, (byte) 0x21,
        (byte) 0x42, (byte) 0x21, (byte) 0x43, (byte) 0x21,
        (byte) 0x44, (byte) 0x21, (byte) 0x45, (byte) 0x21,
        (byte) 0x46, (byte) 0x21, (byte) 0x47, (byte) 0x55,
        (byte) 0x71, (byte) 0x55, (byte) 0x72, (byte) 0x55,
        (byte) 0x73, (byte) 0x55, (byte) 0x74, (byte) 0x55,
        (byte) 0x75, (byte) 0x55, (byte) 0x76, (byte) 0x55,
        (byte) 0x77, (byte) 0x55, (byte) 0x78, (byte) 0x55,
        (byte) 0x79, (byte) 0x55, (byte) 0x7a, (byte) 0x55,
        (byte) 0x7b, (byte) 0x55, (byte) 0x7c, (byte) 0x55,
        (byte) 0x7d, (byte) 0x55, (byte) 0x7e, (byte) 0x56,
        (byte) 0x21, (byte) 0x56, (byte) 0x22, (byte) 0x56,
        (byte) 0x23, (byte) 0x56, (byte) 0x24, (byte) 0x56,
        (byte) 0x25, (byte) 0x56, (byte) 0x26, (byte) 0x56,
        (byte) 0x27, (byte) 0x56, (byte) 0x28, (byte) 0x56,
        (byte) 0x29, (byte) 0x56, (byte) 0x2a, (byte) 0x56,
        (byte) 0x2b, (byte) 0x56, (byte) 0x2c, (byte) 0x56,
        (byte) 0x2d, (byte) 0x56, (byte) 0x2e, (byte) 0x56,
        (byte) 0x2f, (byte) 0x56, (byte) 0x30, (byte) 0x56,
        (byte) 0x31, (byte) 0x56, (byte) 0x32, (byte) 0x56,
        (byte) 0x33, (byte) 0x56, (byte) 0x34, (byte) 0x56,
        (byte) 0x35, (byte) 0x56, (byte) 0x36, (byte) 0x56,
        (byte) 0x37, (byte) 0x56, (byte) 0x38, (byte) 0x56,
        (byte) 0x39, (byte) 0x56, (byte) 0x3a, (byte) 0x56,
        (byte) 0x3b, (byte) 0x56, (byte) 0x3c, (byte) 0x56,
        (byte) 0x3d, (byte) 0x56, (byte) 0x3e, (byte) 0x56,
        (byte) 0x3f, (byte) 0x56, (byte) 0x40, (byte) 0x56,
        (byte) 0x41, (byte) 0x56, (byte) 0x42, (byte) 0x56,
        (byte) 0x43, (byte) 0x56, (byte) 0x44, (byte) 0x56,
        (byte) 0x45, (byte) 0x56, (byte) 0x46, (byte) 0x56,
        (byte) 0x47, (byte) 0x56, (byte) 0x48, (byte) 0x56,
        (byte) 0x49, (byte) 0x56, (byte) 0x4a, (byte) 0x56,
        (byte) 0x4b, (byte) 0x56, (byte) 0x4c, (byte) 0x56,
        (byte) 0x4d, (byte) 0x56, (byte) 0x4e, (byte) 0x56,
        (byte) 0x4f, (byte) 0x56, (byte) 0x50, (byte) 0x56,
        (byte) 0x51, (byte) 0x56, (byte) 0x52, (byte) 0x56,
        (byte) 0x53, (byte) 0x56, (byte) 0x54, (byte) 0x56,
        (byte) 0x55, (byte) 0x56, (byte) 0x56, (byte) 0x56,
        (byte) 0x57, (byte) 0x56, (byte) 0x58, (byte) 0x56,
        (byte) 0x59, (byte) 0x56, (byte) 0x5a, (byte) 0x56,
        (byte) 0x5b, (byte) 0x56, (byte) 0x5c, (byte) 0x56,
        (byte) 0x5d, (byte) 0x56, (byte) 0x5e, (byte) 0x56,
        (byte) 0x5f, (byte) 0x56, (byte) 0x60, (byte) 0x56,
        (byte) 0x61, (byte) 0x56, (byte) 0x62, (byte) 0x56,
        (byte) 0x63, (byte) 0x56, (byte) 0x64, (byte) 0x56,
        (byte) 0x65, (byte) 0x56, (byte) 0x66, (byte) 0x56,
        (byte) 0x67, (byte) 0x56, (byte) 0x68, (byte) 0x56,
        (byte) 0x69, (byte) 0x56, (byte) 0x6a, (byte) 0x56,
        (byte) 0x6b, (byte) 0x56, (byte) 0x6c, (byte) 0x56,
        (byte) 0x6d, (byte) 0x56, (byte) 0x6e, (byte) 0x56,
        (byte) 0x6f, (byte) 0x56, (byte) 0x70, (byte) 0x56,
        (byte) 0x71, (byte) 0x56, (byte) 0x72, (byte) 0x56,
        (byte) 0x73, (byte) 0x56, (byte) 0x74, (byte) 0x56,
        (byte) 0x75, (byte) 0x56, (byte) 0x76, (byte) 0x56,
        (byte) 0x77, (byte) 0x56, (byte) 0x78, (byte) 0x56,
        (byte) 0x79, (byte) 0x56, (byte) 0x7a, (byte) 0x56,
        (byte) 0x7b, (byte) 0x56, (byte) 0x7c, (byte) 0x56,
        (byte) 0x7d, (byte) 0x56, (byte) 0x7e, (byte) 0x57,
        (byte) 0x21, (byte) 0x57, (byte) 0x22, (byte) 0x57,
        (byte) 0x23, (byte) 0x57, (byte) 0x24, (byte) 0x57,
        (byte) 0x25, (byte) 0x57, (byte) 0x26, (byte) 0x57,
        (byte) 0x27, (byte) 0x57, (byte) 0x28, (byte) 0x57,
        (byte) 0x29, (byte) 0x57, (byte) 0x2a, (byte) 0x57,
        (byte) 0x2b, (byte) 0x57, (byte) 0x2c, (byte) 0x57,
        (byte) 0x2d, (byte) 0x57, (byte) 0x2e, (byte) 0x57,
        (byte) 0x2f, (byte) 0x57, (byte) 0x30, (byte) 0x57,
        (byte) 0x31, (byte) 0x57, (byte) 0x32, (byte) 0x57,
        (byte) 0x33, (byte) 0x57, (byte) 0x34, (byte) 0x57,
        (byte) 0x35, (byte) 0x57, (byte) 0x36, (byte) 0x57,
        (byte) 0x37, (byte) 0x57, (byte) 0x38, (byte) 0x57,
        (byte) 0x39, (byte) 0x57, (byte) 0x3a, (byte) 0x57,
        (byte) 0x3b, (byte) 0x57, (byte) 0x3c, (byte) 0x57,
        (byte) 0x3d, (byte) 0x57, (byte) 0x3e, (byte) 0x57,
        (byte) 0x3f, (byte) 0x57, (byte) 0x40, (byte) 0x57,
        (byte) 0x41, (byte) 0x57, (byte) 0x42, (byte) 0x57,
        (byte) 0x43, (byte) 0x57, (byte) 0x44, (byte) 0x57,
        (byte) 0x45, (byte) 0x57, (byte) 0x46, (byte) 0x57,
        (byte) 0x47, (byte) 0x57, (byte) 0x48, (byte) 0x57,
        (byte) 0x49, (byte) 0x57, (byte) 0x4a, (byte) 0x57,
        (byte) 0x4b, (byte) 0x57, (byte) 0x4c, (byte) 0x57,
        (byte) 0x4d, (byte) 0x57, (byte) 0x4e, (byte) 0x57,
        (byte) 0x4f, (byte) 0x57, (byte) 0x50, (byte) 0x57,
        (byte) 0x51, (byte) 0x57, (byte) 0x52, (byte) 0x57,
        (byte) 0x53, (byte) 0x57, (byte) 0x54, (byte) 0x57,
        (byte) 0x55, (byte) 0x57, (byte) 0x56, (byte) 0x57,
        (byte) 0x57, (byte) 0x57, (byte) 0x58, (byte) 0x57,
        (byte) 0x59, (byte) 0x57, (byte) 0x5a, (byte) 0x57,
        (byte) 0x5b, (byte) 0x57, (byte) 0x5c, (byte) 0x57,
        (byte) 0x5d, (byte) 0x57, (byte) 0x5e, (byte) 0x57,
        (byte) 0x5f, (byte) 0x57, (byte) 0x60, (byte) 0x57,
        (byte) 0x61, (byte) 0x57, (byte) 0x62, (byte) 0x57,
        (byte) 0x63, (byte) 0x57, (byte) 0x64, (byte) 0x59,
        (byte) 0x49, (byte) 0x59, (byte) 0x4a, (byte) 0x59,
        (byte) 0x4b, (byte) 0x59, (byte) 0x4c, (byte) 0x59,
        (byte) 0x4d, (byte) 0x59, (byte) 0x4e, (byte) 0x59,
        (byte) 0x4f, (byte) 0x59, (byte) 0x50, (byte) 0x59,
        (byte) 0x51, (byte) 0x59, (byte) 0x52, (byte) 0x59,
        (byte) 0x53, (byte) 0x59, (byte) 0x54, (byte) 0x59,
        (byte) 0x55, (byte) 0x59, (byte) 0x56, (byte) 0x59,
        (byte) 0x57, (byte) 0x59, (byte) 0x58, (byte) 0x59,
        (byte) 0x59, (byte) 0x59, (byte) 0x5a, (byte) 0x59,
        (byte) 0x5b, (byte) 0x59, (byte) 0x5c, (byte) 0x59,
        (byte) 0x5d, (byte) 0x59, (byte) 0x5e, (byte) 0x59,
        (byte) 0x5f, (byte) 0x59, (byte) 0x60, (byte) 0x59,
        (byte) 0x61, (byte) 0x59, (byte) 0x62, (byte) 0x59,
        (byte) 0x63, (byte) 0x59, (byte) 0x64, (byte) 0x59,
        (byte) 0x65, (byte) 0x59, (byte) 0x66, (byte) 0x59,
        (byte) 0x67, (byte) 0x59, (byte) 0x68, (byte) 0x59,
        (byte) 0x69, (byte) 0x59, (byte) 0x6a, (byte) 0x59,
        (byte) 0x6b, (byte) 0x59, (byte) 0x6c, (byte) 0x59,
        (byte) 0x6d, (byte) 0x59, (byte) 0x6e, (byte) 0x59,
        (byte) 0x6f, (byte) 0x59, (byte) 0x70, (byte) 0x59,
        (byte) 0x71, (byte) 0x59, (byte) 0x72, (byte) 0x59,
        (byte) 0x73, (byte) 0x59, (byte) 0x74, (byte) 0x59,
        (byte) 0x75, (byte) 0x59, (byte) 0x76, (byte) 0x59,
        (byte) 0x77, (byte) 0x59, (byte) 0x78, (byte) 0x59,
        (byte) 0x79, (byte) 0x59, (byte) 0x7a, (byte) 0x59,
        (byte) 0x7b, (byte) 0x59, (byte) 0x7c, (byte) 0x59,
        (byte) 0x7d, (byte) 0x59, (byte) 0x7e, (byte) 0x5a,
        (byte) 0x21, (byte) 0x5a, (byte) 0x22, (byte) 0x5a,
        (byte) 0x23, (byte) 0x5a, (byte) 0x24, (byte) 0x5a,
        (byte) 0x25, (byte) 0x5a, (byte) 0x26, (byte) 0x5a,
        (byte) 0x27, (byte) 0x5a, (byte) 0x28, (byte) 0x5a,
        (byte) 0x29, (byte) 0x5a, (byte) 0x2a, (byte) 0x5a,
        (byte) 0x2b, (byte) 0x5a, (byte) 0x2c, (byte) 0x5a,
        (byte) 0x2d, (byte) 0x5a, (byte) 0x2e, (byte) 0x5a,
        (byte) 0x2f, (byte) 0x5a, (byte) 0x30, (byte) 0x5a,
        (byte) 0x31, (byte) 0x5a, (byte) 0x32, (byte) 0x5a,
        (byte) 0x33, (byte) 0x5a, (byte) 0x34, (byte) 0x5a,
        (byte) 0x35, (byte) 0x5a, (byte) 0x36, (byte) 0x5a,
        (byte) 0x37, (byte) 0x5a, (byte) 0x38, (byte) 0x5a,
        (byte) 0x39, (byte) 0x5a, (byte) 0x3a, (byte) 0x5a,
        (byte) 0x3b, (byte) 0x5a, (byte) 0x3c, (byte) 0x5a,
        (byte) 0x3d, (byte) 0x5a, (byte) 0x3e, (byte) 0x5a,
        (byte) 0x3f, (byte) 0x5a, (byte) 0x40, (byte) 0x5a,
        (byte) 0x41, (byte) 0x5a, (byte) 0x42, (byte) 0x5b,
        (byte) 0x35, (byte) 0x5b, (byte) 0x36, (byte) 0x5b,
        (byte) 0x37, (byte) 0x5b, (byte) 0x38, (byte) 0x5b,
        (byte) 0x39, (byte) 0x5b, (byte) 0x3a, (byte) 0x5b,
        (byte) 0x3b, (byte) 0x5b, (byte) 0x3c, (byte) 0x5b,
        (byte) 0x3d, (byte) 0x5b, (byte) 0x3e, (byte) 0x5b,
        (byte) 0x3f, (byte) 0x5b, (byte) 0x40, (byte) 0x5b,
        (byte) 0x41, (byte) 0x5b, (byte) 0x42, (byte) 0x5b,
        (byte) 0x43, (byte) 0x5b, (byte) 0x44, (byte) 0x5b,
        (byte) 0x45, (byte) 0x5b, (byte) 0x46, (byte) 0x5b,
        (byte) 0x47, (byte) 0x5b, (byte) 0x48, (byte) 0x5b,
        (byte) 0x49, (byte) 0x5b, (byte) 0x4a, (byte) 0x5b,
        (byte) 0x4b, (byte) 0x5b, (byte) 0x4c, (byte) 0x5b,
        (byte) 0x4d, (byte) 0x5b, (byte) 0x4e, (byte) 0x5b,
        (byte) 0x4f, (byte) 0x5b, (byte) 0x50, (byte) 0x5b,
        (byte) 0x51, (byte) 0x5b, (byte) 0x52, (byte) 0x5b,
        (byte) 0x53, (byte) 0x5b, (byte) 0x54, (byte) 0x5b,
        (byte) 0x55, (byte) 0x5b, (byte) 0x56, (byte) 0x5b,
        (byte) 0x57, (byte) 0x5b, (byte) 0x58, (byte) 0x5b,
        (byte) 0x59, (byte) 0x5b, (byte) 0x5a, (byte) 0x5b,
        (byte) 0x5b, (byte) 0x5b, (byte) 0x5c, (byte) 0x5b,
        (byte) 0x5d, (byte) 0x5b, (byte) 0x5e, (byte) 0x5b,
        (byte) 0x5f, (byte) 0x5b, (byte) 0x60, (byte) 0x5b,
        (byte) 0x61, (byte) 0x5b, (byte) 0x62, (byte) 0x5b,
        (byte) 0x63, (byte) 0x5b, (byte) 0x64, (byte) 0x5b,
        (byte) 0x65, (byte) 0x5b, (byte) 0x66, (byte) 0x5b,
        (byte) 0x67, (byte) 0x5b, (byte) 0x68, (byte) 0x5b,
        (byte) 0x69, (byte) 0x5b, (byte) 0x6a, (byte) 0x5b,
        (byte) 0x6b, (byte) 0x5b, (byte) 0x6c, (byte) 0x5b,
        (byte) 0x6d, (byte) 0x5b, (byte) 0x6e, (byte) 0x5b,
        (byte) 0x6f, (byte) 0x5b, (byte) 0x70, (byte) 0x5b,
        (byte) 0x71, (byte) 0x5b, (byte) 0x72, (byte) 0x5b,
        (byte) 0x73, (byte) 0x5b, (byte) 0x74, (byte) 0x5b,
        (byte) 0x75, (byte) 0x5b, (byte) 0x76, (byte) 0x5b,
        (byte) 0x77, (byte) 0x5b, (byte) 0x78, (byte) 0x5b,
        (byte) 0x79, (byte) 0x5b, (byte) 0x7a, (byte) 0x5b,
        (byte) 0x7b, (byte) 0x5b, (byte) 0x7c, (byte) 0x5b,
        (byte) 0x7d, (byte) 0x5b, (byte) 0x7e, (byte) 0x5c,
        (byte) 0x21, (byte) 0x5c, (byte) 0x22, (byte) 0x5c,
        (byte) 0x23, (byte) 0x5c, (byte) 0x24, (byte) 0x5c,
        (byte) 0x25, (byte) 0x5c, (byte) 0x26, (byte) 0x5c,
        (byte) 0x27, (byte) 0x5c, (byte) 0x28, (byte) 0x5c,
        (byte) 0x29, (byte) 0x5c, (byte) 0x2a, (byte) 0x5c,
        (byte) 0x2b, (byte) 0x5c, (byte) 0x2c, (byte) 0x5c,
        (byte) 0x2d, (byte) 0x5c, (byte) 0x2e, (byte) 0x5c,
        (byte) 0x2f, (byte) 0x5c, (byte) 0x30, (byte) 0x5c,
        (byte) 0x31, (byte) 0x5c, (byte) 0x32, (byte) 0x5c,
        (byte) 0x33, (byte) 0x5c, (byte) 0x34, (byte) 0x5c,
        (byte) 0x35, (byte) 0x5c, (byte) 0x36, (byte) 0x5d,
        (byte) 0x79, (byte) 0x5d, (byte) 0x7a, (byte) 0x5d,
        (byte) 0x7b, (byte) 0x5d, (byte) 0x7c, (byte) 0x5d,
        (byte) 0x7d, (byte) 0x5d, (byte) 0x7e, (byte) 0x5e,
        (byte) 0x21, (byte) 0x5e, (byte) 0x22, (byte) 0x5e,
        (byte) 0x23, (byte) 0x5e, (byte) 0x24, (byte) 0x5e,
        (byte) 0x25, (byte) 0x5e, (byte) 0x26, (byte) 0x5e,
        (byte) 0x27, (byte) 0x5e, (byte) 0x28, (byte) 0x5e,
        (byte) 0x29, (byte) 0x5e, (byte) 0x2a, (byte) 0x5e,
        (byte) 0x2b, (byte) 0x5e, (byte) 0x2c, (byte) 0x5e,
        (byte) 0x2d, (byte) 0x5e, (byte) 0x2e, (byte) 0x5e,
        (byte) 0x2f, (byte) 0x5e, (byte) 0x30, (byte) 0x5e,
        (byte) 0x31, (byte) 0x5e, (byte) 0x32, (byte) 0x5e,
        (byte) 0x33, (byte) 0x5e, (byte) 0x34, (byte) 0x5e,
        (byte) 0x35, (byte) 0x5e, (byte) 0x36, (byte) 0x5e,
        (byte) 0x37, (byte) 0x5e, (byte) 0x38, (byte) 0x5e,
        (byte) 0x39, (byte) 0x5e, (byte) 0x3a, (byte) 0x5e,
        (byte) 0x3b, (byte) 0x5e, (byte) 0x3c, (byte) 0x5e,
        (byte) 0x3d, (byte) 0x5e, (byte) 0x3e, (byte) 0x5e,
        (byte) 0x3f, (byte) 0x5e, (byte) 0x40, (byte) 0x5e,
        (byte) 0x41, (byte) 0x5e, (byte) 0x42, (byte) 0x5e,
        (byte) 0x43, (byte) 0x5e, (byte) 0x44, (byte) 0x5e,
        (byte) 0x45, (byte) 0x5e, (byte) 0x46, (byte) 0x5e,
        (byte) 0x47, (byte) 0x5e, (byte) 0x48, (byte) 0x5e,
        (byte) 0x49, (byte) 0x5e, (byte) 0x4a, (byte) 0x60,
        (byte) 0x30, (byte) 0x60, (byte) 0x31, (byte) 0x60,
        (byte) 0x32, (byte) 0x60, (byte) 0x33, (byte) 0x60,
        (byte) 0x34, (byte) 0x60, (byte) 0x35, (byte) 0x60,
        (byte) 0x36, (byte) 0x60, (byte) 0x37, (byte) 0x60,
        (byte) 0x38, (byte) 0x60, (byte) 0x39, (byte) 0x60,
        (byte) 0x3a, (byte) 0x60, (byte) 0x3b, (byte) 0x60,
        (byte) 0x3c, (byte) 0x60, (byte) 0x3d, (byte) 0x60,
        (byte) 0x3e, (byte) 0x60, (byte) 0x3f, (byte) 0x73,
        (byte) 0x26, (byte) 0x73, (byte) 0x27, (byte) 0x73,
        (byte) 0x28, (byte) 0x73, (byte) 0x29, (byte) 0x73,
        (byte) 0x2a, (byte) 0x73, (byte) 0x2b, (byte) 0x73,
        (byte) 0x2c, (byte) 0x73, (byte) 0x2d, (byte) 0x73,
        (byte) 0x2e, (byte) 0x73, (byte) 0x2f, (byte) 0x73,
        (byte) 0x30, (byte) 0x73, (byte) 0x31, (byte) 0x73,
        (byte) 0x32, (byte) 0x73, (byte) 0x33, (byte) 0x73,
        (byte) 0x34, (byte) 0x73, (byte) 0x35, (byte) 0x73,
        (byte) 0x36, (byte) 0x73, (byte) 0x37, (byte) 0x73,
        (byte) 0x38, (byte) 0x73, (byte) 0x39, (byte) 0x73,
        (byte) 0x3a, (byte) 0x73, (byte) 0x3b, (byte) 0x73,
        (byte) 0x3c, (byte) 0x73, (byte) 0x3d, (byte) 0x73,
        (byte) 0x3e, (byte) 0x73, (byte) 0x3f, (byte) 0x73,
        (byte) 0x40, (byte) 0x73, (byte) 0x41, (byte) 0x73,
        (byte) 0x42, (byte) 0x73, (byte) 0x43, (byte) 0x73,
        (byte) 0x44, (byte) 0x73, (byte) 0x45, (byte) 0x73,
        (byte) 0x46, (byte) 0x73, (byte) 0x47, (byte) 0x73,
        (byte) 0x48, (byte) 0x73, (byte) 0x49, (byte) 0x73,
        (byte) 0x4a, (byte) 0x73, (byte) 0x4b, (byte) 0x73,
        (byte) 0x4c, (byte) 0x73, (byte) 0x4d, (byte) 0x73,
        (byte) 0x4e, (byte) 0x73, (byte) 0x4f, (byte) 0x73,
        (byte) 0x50, (byte) 0x73, (byte) 0x51, (byte) 0x73,
        (byte) 0x52, (byte) 0x73, (byte) 0x53, (byte) 0x73,
        (byte) 0x54, (byte) 0x73, (byte) 0x55, (byte) 0x73,
        (byte) 0x56, (byte) 0x73, (byte) 0x57, (byte) 0x73,
        (byte) 0x58, (byte) 0x73, (byte) 0x59, (byte) 0x73,
        (byte) 0x5a, (byte) 0x73, (byte) 0x5b, (byte) 0x73,
        (byte) 0x5c, (byte) 0x73, (byte) 0x5d, (byte) 0x73,
        (byte) 0x5e, (byte) 0x73, (byte) 0x5f, (byte) 0x73,
        (byte) 0x60, (byte) 0x73, (byte) 0x61, (byte) 0x73,
        (byte) 0x62, (byte) 0x73, (byte) 0x63, (byte) 0x73,
        (byte) 0x64, (byte) 0x73, (byte) 0x65, (byte) 0x73,
        (byte) 0x66, (byte) 0x73, (byte) 0x67, (byte) 0x73,
        (byte) 0x68, (byte) 0x73, (byte) 0x69, (byte) 0x73,
        (byte) 0x6a, (byte) 0x73, (byte) 0x6b, (byte) 0x73,
        (byte) 0x6c, (byte) 0x73, (byte) 0x6d, (byte) 0x73,
        (byte) 0x6e, (byte) 0x73, (byte) 0x6f, (byte) 0x73,
        (byte) 0x70, (byte) 0x73, (byte) 0x71, (byte) 0x73,
        (byte) 0x72, (byte) 0x73, (byte) 0x73, (byte) 0x73,
        (byte) 0x74, (byte) 0x73, (byte) 0x75, (byte) 0x73,
        (byte) 0x76, (byte) 0x73, (byte) 0x77, (byte) 0x73,
        (byte) 0x78, (byte) 0x73, (byte) 0x79, (byte) 0x73,
        (byte) 0x7a, (byte) 0x73, (byte) 0x7b, (byte) 0x73,
        (byte) 0x7c, (byte) 0x73, (byte) 0x7d, (byte) 0x73,
        (byte) 0x7e, (byte) 0x74, (byte) 0x21, (byte) 0x74,
        (byte) 0x22, (byte) 0x74, (byte) 0x23, (byte) 0x74,
        (byte) 0x24, (byte) 0x74, (byte) 0x25, (byte) 0x74,
        (byte) 0x26, (byte) 0x1b, (byte) 0x28, (byte) 0x49,
        (byte) 0x21, (byte) 0x22, (byte) 0x23, (byte) 0x24,
        (byte) 0x25, (byte) 0x26, (byte) 0x27, (byte) 0x28,
        (byte) 0x29, (byte) 0x2a, (byte) 0x2b, (byte) 0x2c,
        (byte) 0x2d, (byte) 0x2e, (byte) 0x2f, (byte) 0x30,
        (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34,
        (byte) 0x35, (byte) 0x36, (byte) 0x37, (byte) 0x38,
        (byte) 0x39, (byte) 0x3a, (byte) 0x3b, (byte) 0x3c,
        (byte) 0x3d, (byte) 0x3e, (byte) 0x3f, (byte) 0x40,
        (byte) 0x41, (byte) 0x42, (byte) 0x43, (byte) 0x44,
        (byte) 0x45, (byte) 0x46, (byte) 0x47, (byte) 0x48,
        (byte) 0x49, (byte) 0x4a, (byte) 0x4b, (byte) 0x4c,
        (byte) 0x4d, (byte) 0x4e, (byte) 0x4f, (byte) 0x50,
        (byte) 0x51, (byte) 0x52, (byte) 0x53, (byte) 0x54,
        (byte) 0x55, (byte) 0x56, (byte) 0x57, (byte) 0x58,
        (byte) 0x59, (byte) 0x5a, (byte) 0x5b, (byte) 0x5c,
        (byte) 0x5d, (byte) 0x5e, (byte) 0x5f, (byte) 0x1b,
        (byte) 0x28, (byte) 0x42 };

    private final static String MIXEDCONTENT =
        "JA\u3000\u3002\u0062\uFF64PAN" +
        "\uFF0C\uFF0E\u00A5\uFF65\uFF66X\u203E" +
        "\u30FB\uFF67\u203E";

    static byte[] mixedBytesExpected = {
        (byte) 0x4a, (byte) 0x41, (byte) 0x1b, (byte) 0x24,
        (byte) 0x42, (byte) 0x21, (byte) 0x21, (byte) 0x21,
        (byte) 0x23, (byte) 0x1b, (byte) 0x28, (byte) 0x42,
        (byte) 0x62, (byte) 0x1b, (byte) 0x28, (byte) 0x49,
        (byte) 0x24, (byte) 0x1b, (byte) 0x28, (byte) 0x42,
        (byte) 0x50, (byte) 0x41, (byte) 0x4e, (byte) 0x1b,
        (byte) 0x24, (byte) 0x42, (byte) 0x21, (byte) 0x24,
        (byte) 0x21, (byte) 0x25, (byte) 0x1b, (byte) 0x28,
        (byte) 0x4a, (byte) 0x5c, (byte) 0x1b, (byte) 0x28,
        (byte) 0x49, (byte) 0x25, (byte) 0x26, (byte) 0x1b,
        (byte) 0x28, (byte) 0x42, (byte) 0x58, (byte) 0x1b,
        (byte) 0x28, (byte) 0x4a, (byte) 0x7e, (byte) 0x1b,
        (byte) 0x24, (byte) 0x42, (byte) 0x21, (byte) 0x26,
        (byte) 0x1b, (byte) 0x28, (byte) 0x49, (byte) 0x27,
        (byte) 0x1b, (byte) 0x28, (byte) 0x4a, (byte) 0x7e,
        (byte) 0x1b, (byte) 0x28, (byte) 0x42  };

    static byte[] repeatingEscapes = {
        (byte) 0x4a, (byte) 0x41, (byte) 0x1b, (byte) 0x24,
        (byte) 0x42, (byte)0x1b, (byte)0x24, (byte)0x42,
        (byte) 0x21, (byte) 0x21, (byte) 0x21,
        (byte) 0x23, (byte) 0x1b, (byte) 0x28, (byte) 0x42,
        // embedded repeated iso-2022 escapes (see bugID 4879522)
        (byte)0x1b, (byte)0x28, (byte)0x42,
        (byte) 0x62, (byte) 0x1b, (byte) 0x28, (byte) 0x49,
        (byte)0x0f, (byte)0x0e, (byte)0x0f,
        (byte)0x1b, (byte)0x28, (byte)0x49,
        (byte) 0x24, (byte) 0x1b, (byte) 0x28, (byte) 0x42,
        (byte) 0x50, (byte) 0x41, (byte) 0x4e,
        // embedded shift chars (see bugID 4879522)
        (byte)0x0e, (byte)0x0f,
        (byte) 0x1b,
        (byte) 0x24, (byte) 0x42, (byte) 0x21, (byte) 0x24,
        (byte) 0x21, (byte) 0x25, (byte) 0x1b, (byte) 0x28,
        (byte) 0x4a, (byte) 0x5c, (byte) 0x1b, (byte) 0x28,
        (byte) 0x49, (byte) 0x25, (byte) 0x26, (byte) 0x1b,
        (byte) 0x28, (byte) 0x42, (byte) 0x58, (byte) 0x1b,
        (byte) 0x28, (byte) 0x4a, (byte) 0x7e, (byte) 0x1b,
        (byte) 0x24, (byte) 0x42, (byte) 0x21, (byte) 0x26,
        (byte) 0x1b, (byte) 0x28, (byte) 0x49, (byte) 0x27,
        (byte) 0x1b, (byte) 0x28, (byte) 0x4a, (byte) 0x7e,
        (byte) 0x1b, (byte) 0x28, (byte) 0x42  };


    private static String JISX0212 =
        "\u02d8\u6896\u9fa5";

    private static byte[] expectedBytes_JISX0212 = {
        (byte)0x1b, (byte)0x24, (byte)0x28, (byte)0x44,
        (byte)0x22, (byte)0x2f, (byte)0x43, (byte)0x6f,
        (byte)0x6d, (byte)0x63,
        (byte)0x1b, (byte)0x28, (byte)0x42
    };

    /*
     * Tests the roundtrip integrity and expected encoding
     * correctness for a String containing a substantial
     * subset of ISO-2022-JP/ISO-2022-JP-2 encodeable chars
     */

    private static void roundTrip(String testStr, byte[] expectBytes,
                                  String csName)
    throws Exception {
        byte[] encodedBytes = testStr.getBytes(csName);

        if (encodedBytes.length != expectBytes.length) {
            throw new Exception(csName + " Encoder error");
        }

        for (int i = 0; i < expectBytes.length; i++) {
            if (encodedBytes[i] != expectBytes[i])  {
                throw new Exception(csName + " Encoder error");
            }
        }
        String decoded = new String(encodedBytes, csName);

        if (!decoded.equals(testStr)) {
            throw new Exception(csName + " Decoder error");
        }
        String decoded2 = new String(repeatingEscapes, csName);
        if (!decoded2.equals(MIXEDCONTENT)) {
            throw new Exception(csName + " Decoder error");
        }
     }

    public static void main(String[] args) throws Exception {

        // Long String containing sequential chars
        // ASCII/yen/tilde/jisx0208 chars/katakana chars

        String testStr1 = US_ASCII +
                        JISX0208SUBSET + JISX0202KATAKANA;
        roundTrip(testStr1, expectedBytes1, "ISO-2022-JP");
        roundTrip(testStr1, expectedBytes1, "ISO-2022-JP-2");
        roundTrip(JISX0212, expectedBytes_JISX0212, "ISO-2022-JP-2");

        // mixed chars which encode to the supported codesets
        // of ISO-2022-JP/ISO-2022-JP-2

        String testStr2 = MIXEDCONTENT;
        roundTrip(testStr2 , mixedBytesExpected, "ISO-2022-JP");
        roundTrip(testStr2 , mixedBytesExpected, "ISO-2022-JP-2");

        String decoded2 = new String(repeatingEscapes, "ISO-2022-JP");
        if (!decoded2.equals(MIXEDCONTENT)) {
            throw new Exception("ISO-2022-JP Decoder error");
        }

        decoded2 = new String(repeatingEscapes, "ISO-2022-JP-2");
        if (!decoded2.equals(MIXEDCONTENT)) {
            throw new Exception("ISO-2022-JP-2 Decoder error");
        }

        // Test for bugID 4913711
        // ISO-2022-JP encoding of a single input char yields
        // 8 output bytes. Prior to fix for 4913711 the
        // max bytes per char value was underspecified as 5.0
        // and the code below would have thrown a BufferOverflow
        // exception. This test validates the fix for 4913711

        String testStr3 = "\u3042";
        byte[] expected = { (byte)0x1b, (byte)0x24, (byte)0x42,
                            (byte)0x24, (byte)0x22, (byte)0x1b,
                            (byte)0x28, (byte)0x42 };
        byte[] encoded = testStr3.getBytes("ISO-2022-JP");
        for (int i = 0; i < expected.length; i++) {
            if (encoded[i] != expected[i])
               throw new Exception("ISO-2022-JP Decoder error");
        }

        // Test for 7 c2b codepoints in ms932 iso2022jp
        String testStr4 = "\u00b8\u00b7\u00af\u00ab\u00bb\u3094\u00b5";
        expected = new byte[] {
                     (byte)0x1b, (byte)0x24, (byte)0x42,
                     (byte)0x21, (byte)0x24,
                     (byte)0x21, (byte)0x26,
                     (byte)0x21, (byte)0x31,
                     (byte)0x22, (byte)0x63,
                     (byte)0x22, (byte)0x64,
                     (byte)0x25, (byte)0x74,
                     (byte)0x26, (byte)0x4c,
                     (byte)0x1b, (byte)0x28, (byte)0x42 };
        encoded = testStr4.getBytes("x-windows-iso2022jp");
        if (!Arrays.equals(encoded, expected)) {
               throw new Exception("MSISO2022JP Encoder error");
        }
        // Test for 10 non-roundtrip characters in ms932 iso2022jp
        encoded = new byte[] {
            (byte)0x1B, (byte)0x24, (byte)0x42,
            (byte)0x22, (byte)0x4C,
            (byte)0x22, (byte)0x5D,
            (byte)0x22, (byte)0x65,
            (byte)0x22, (byte)0x69,
            (byte)0x2D, (byte)0x70,
            (byte)0x2D, (byte)0x71,
            (byte)0x2D, (byte)0x77,
            (byte)0x2D, (byte)0x7A,
            (byte)0x2D, (byte)0x7B,
            (byte)0x2D, (byte)0x7C,
            (byte)0x1B, (byte)0x28, (byte)0x42,
        };
        String expectedStr = "\uffe2\u22a5\u221a\u222b\u2252\u2261\u2220\u2235\u2229\u222a";
        if (!new String(encoded, "x-windows-iso2022jp").equals(expectedStr)) {
               throw new Exception("MSISO2022JP Decoder error");
        }
        // Test for 11 iso2022jp decoder
        encoded = new byte[] {
            (byte)0x1B, (byte)0x28, (byte)0x49, (byte)0x60,
            (byte)0x1B, (byte)0x28, (byte)0x42,
        };
        String unexpectedStr = "\uffa0";
        expectedStr = "\ufffd";
        if (new String(encoded, "ISO2022JP").equals(unexpectedStr)) {
               throw new Exception("ISO2022JP Decoder error: \\uFFA0");
        } else if (!new String(encoded, "ISO2022JP").equals(expectedStr)) {
               throw new Exception("ISO2022JP Decoder error: \\uFFFD");
        }
    }
}
