/*
 * Copyright (c) 2001, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4468109 8021583
 * @summary Test for printing AUTOSENSE DocFlavor.  No exception should be thrown.
 * @run main PrintAutoSenseData
*/

import java.io.*;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.net.URL;

public class PrintAutoSenseData
{
  private DocFlavor flavor = DocFlavor.URL.AUTOSENSE; //represents the docflavor.
  private PrintService[] service = PrintServiceLookup.lookupPrintServices(flavor, null);


  public PrintAutoSenseData()
  {
     if (service.length == 0)
     {
        System.out.println("No print service available...");
        return;
     }

     System.out.println("selected PrintService: " + this.service[0]);
     if (service[0].isDocFlavorSupported(flavor)) {
         System.out.println("DocFlavor.URL.AUTOSENSE supported");
     } else {
         System.out.println("DocFlavor.URL.AUTOSENSE not supported. Testing aborted !!");
         return;
     }

     DocPrintJob job = service[0].createPrintJob();
     this.print();
  }

  // The print method prints sample file with DocFlavor.URL.AUTOSENSE.
  void print()
  {
         String fileName = "./sample.txt";
         DocPrintJob job = service[0].createPrintJob();

         // The representation class is a URL.
         System.out.println("printing " + fileName + " using doc flavor: " + this.flavor);
         System.out.println("Rep. class name: " + this.flavor.getRepresentationClassName() + " MimeType: " + this.flavor.getMimeType());

         Doc doc = new URLDoc(fileName, this.flavor);
         HashPrintRequestAttributeSet prSet =
             new HashPrintRequestAttributeSet();
         prSet.add(new Destination(new File("./dest.prn").toURI()));
         //print the document.
         try {
            job.print(doc, prSet);
         } catch ( Exception e ) {
            e.printStackTrace();
         }
  }

  public static void main(String[] args) {
     new PrintAutoSenseData();
  }

}

/* This class is for reading autosense data with URL representation class */

class URLDoc implements Doc
{
   protected String fileName = null;
   protected DocFlavor flavor = null;
   protected Object printData = null;
   protected InputStream instream = null;

   public URLDoc(String filename, DocFlavor docFlavor)
   {
      this.fileName = filename;
      this.flavor = docFlavor;
   }

   public DocFlavor getDocFlavor() {
       return DocFlavor.URL.AUTOSENSE;
   }

   public DocAttributeSet getAttributes()
   {
       HashDocAttributeSet hset = new HashDocAttributeSet();
       return hset;
   }

   public Object getPrintData()
   {
     if ( this.printData == null )
     {
        this.printData = URLDoc.class.getResource(this.fileName);
        System.out.println("getPrintData(): " + this.printData);
     }
     return this.printData;
   }

   public Reader getReaderForText()
   {
     return null;
   }

   public InputStream getStreamForBytes()
   {
     System.out.println("getStreamForBytes(): " + this.printData);
     try
     {
        if ( (this.printData != null) && (this.printData instanceof URL) )
        {
           this.instream = ((URL)this.printData).openStream();
        }
        if (this.instream == null)
        {
           URL url = URLDoc.class.getResource(this.fileName);
           this.instream = url.openStream();
        }
      }
      catch ( IOException ie )
      {
         System.out.println("URLDoc: exception: " + ie.toString());
      }
      return this.instream;
   }
}
