/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashAttributeSet;
import javax.print.attribute.standard.PrinterName;

/*
 * @test
 * @bug 8032693
 * @key printer
 * @summary Test that lpstat and JDK agree whether there are printers.
 */
public class CountPrintServices {

  public static void main(String[] args) throws Exception {
    String os = System.getProperty("os.name").toLowerCase();
    System.out.println("OS is " + os);
    if (!os.equals("linux")) {
        System.out.println("Linux specific test. No need to continue");
        return;
    }
    PrintService services[] =
        PrintServiceLookup.lookupPrintServices(null, null);
    if (services.length > 0) {
       System.out.println("Services found. No need to test further.");
       return;
    }
    String[] lpcmd = { "lpstat", "-a" };
    Process proc = Runtime.getRuntime().exec(lpcmd);
    proc.waitFor();
    InputStreamReader ir = new InputStreamReader(proc.getInputStream());
    BufferedReader br = new BufferedReader(ir);
    int count = 0;
    String printer;
    while ((printer = br.readLine()) != null) {
       System.out.println("lpstat:: " + printer);
       count++;
    }
    if (count > 0) {
        throw new RuntimeException("Services exist, but not found by JDK.");
    }
 }
}

