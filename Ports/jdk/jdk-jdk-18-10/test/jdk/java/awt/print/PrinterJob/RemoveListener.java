/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test 1.1 01/05/17
 * @bug 4459889
 * @summary No NullPointerException should occur.
 * @run main RemoveListener
*/
import javax.print.*;
import javax.print.attribute.*;
import javax.print.event.*;
import javax.print.attribute.standard.*;

public class RemoveListener {
    public static void main(String[] args){
        PrintService[] pservices = PrintServiceLookup.lookupPrintServices(null, null);
        if (pservices.length == 0){
            return;
        }
        DocPrintJob pj = pservices[0].createPrintJob();
        PrintJobAttributeSet aset = new HashPrintJobAttributeSet();
        aset.add(JobState.PROCESSING);
        PrintJobAttributeListener listener = new PJAListener();
        pj.addPrintJobAttributeListener(listener, aset);
        pj.removePrintJobAttributeListener(listener);
        return;
    }
}

class PJAListener implements PrintJobAttributeListener {
    public void attributeUpdate(PrintJobAttributeEvent pjae){
        return;
    }
}
