/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 6392086 8014725
  @summary tests that HTMLs of all supported native HTML formats are transfered
           properly
  @run main/othervm HTMLTransferTest
*/

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;

public class HTMLTransferTest {
    public static final int CODE_NOT_RETURNED = 100;
    public static final int CODE_CONSUMER_TEST_FAILED = 101;
    public static final int CODE_FAILURE = 102;
    public static DataFlavor[] HTMLFlavors = null;
    public static DataFlavor SyncFlavor = null;
    static {
        try{
            HTMLFlavors = new DataFlavor[] {
                new DataFlavor("text/html; document=selection; Class=" + InputStream.class.getName() + "; charset=UTF-8"),
                new DataFlavor("text/html; document=selection; Class=" + String.class.getName() + "; charset=UTF-8")
            };
            SyncFlavor = new DataFlavor(
                "application/x-java-serialized-object; class="
                + SyncMessage.class.getName()
                + "; charset=UTF-8"
            );
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private THTMLProducer imPr;
    private int returnCode = CODE_NOT_RETURNED;

    public static void main(final String[] args) {
        HTMLTransferTest app = new HTMLTransferTest();
        app.init();
        app.start();
    }

    public void init() {
        initImpl();

    } // init()

    private void initImpl() {
        imPr = new THTMLProducer();
        imPr.begin();
    }


    public void start() {
        try {
            String stFormats = "";

            String iniMsg = "Testing formats from the list:\n";
            for (int i = 0; i < HTMLTransferTest.HTMLFlavors.length; i++) {
                stFormats += "\"" + HTMLTransferTest.HTMLFlavors[i].getMimeType() + "\"\n";
            }
            System.out.println(iniMsg + stFormats);
            System.err.println("===>" + iniMsg + stFormats);

            String javaPath = System.getProperty("java.home", "");
            String cmd = javaPath + File.separator + "bin" + File.separator
                + "java -cp " + System.getProperty("test.classes", ".") +
                //+ "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000 "
                " THTMLConsumer"
                //+ stFormats
                ;

            Process process = Runtime.getRuntime().exec(cmd);
            ProcessResults pres = ProcessResults.doWaitFor(process);
            returnCode = pres.exitValue;

            if (pres.stderr != null && pres.stderr.length() > 0) {
                System.err.println("========= Child VM System.err ========");
                System.err.print(pres.stderr);
                System.err.println("======================================");
            }

            if (pres.stdout != null && pres.stdout.length() > 0) {
                System.err.println("========= Child VM System.out ========");
                System.err.print(pres.stdout);
                System.err.println("======================================");
            }
        } catch (Throwable e) {
            e.printStackTrace();
            //returnCode equals CODE_NOT_RETURNED
        }

        switch (returnCode) {
        case CODE_NOT_RETURNED:
            System.err.println("Child VM: failed to start");
            break;
        case CODE_FAILURE:
            System.err.println("Child VM: abnormal termination");
            break;
        case CODE_CONSUMER_TEST_FAILED:
            throw new RuntimeException("test failed: HTMLs in some " +
                "native formats are not transferred properly: " +
                "see output of child VM");
        default:
            boolean failed = false;
            String passedFormats = "";
            String failedFormats = "";

            for (int i = 0; i < imPr.passedArray.length; i++) {
               if (imPr.passedArray[i]) {
                   passedFormats += HTMLTransferTest.HTMLFlavors[i].getMimeType() + " ";
               } else {
                   failed = true;
                   failedFormats += HTMLTransferTest.HTMLFlavors[i].getMimeType() + " ";
               }
            }
            if (failed) {
                throw new RuntimeException(
                    "test failed: HTMLs in following "
                    + "native formats are not transferred properly: "
                    + failedFormats
                );
            } else {
                System.err.println(
                    "HTMLs in following native formats are "
                    + "transferred properly: "
                    + passedFormats
                );
            }
        }

    } // start()

} // class HTMLTransferTest

class SyncMessage implements Serializable {
    String msg;

    public SyncMessage(String sync) {
        this.msg = sync;
    }

    @Override
    public boolean equals(Object obj) {
        return this.msg.equals(((SyncMessage)obj).msg);
    }

    @Override
    public String toString() {
        return msg;
    }
}

class ProcessResults {
    public int exitValue;
    public String stdout;
    public String stderr;

    public ProcessResults() {
        exitValue = -1;
        stdout = "";
        stderr = "";
    }

    /**
     * Method to perform a "wait" for a process and return its exit value.
     * This is a workaround for <code>Process.waitFor()</code> never returning.
     */
    public static ProcessResults doWaitFor(Process p) {
        ProcessResults pres = new ProcessResults();

        InputStream in = null;
        InputStream err = null;

        try {
            in = p.getInputStream();
            err = p.getErrorStream();

            boolean finished = false;

            while (!finished) {
                try {
                    while (in.available() > 0) {
                        pres.stdout += (char)in.read();
                    }
                    while (err.available() > 0) {
                        pres.stderr += (char)err.read();
                    }
                    // Ask the process for its exitValue. If the process
                    // is not finished, an IllegalThreadStateException
                    // is thrown. If it is finished, we fall through and
                    // the variable finished is set to true.
                    pres.exitValue = p.exitValue();
                    finished  = true;
                }
                catch (IllegalThreadStateException e) {
                    // Process is not finished yet;
                    // Sleep a little to save on CPU cycles
                    Thread.currentThread().sleep(500);
                }
            }
            if (in != null) in.close();
            if (err != null) err.close();
        }
        catch (Throwable e) {
            System.err.println("doWaitFor(): unexpected exception");
            e.printStackTrace();
        }
        return pres;
    }
}


abstract class HTMLTransferer implements ClipboardOwner {

    static final SyncMessage S_PASSED = new SyncMessage("Y");
    static final SyncMessage S_FAILED = new SyncMessage("N");
    static final SyncMessage S_BEGIN = new SyncMessage("B");
    static final SyncMessage S_BEGIN_ANSWER = new SyncMessage("BA");
    static final SyncMessage S_END = new SyncMessage("E");



    Clipboard m_clipboard;

    HTMLTransferer() {
        m_clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    }


    abstract void notifyTransferSuccess(boolean status);


    static Object createTRInstance(int i) {
        try{
            String _htmlText =
                "The quick <font color='#78650d'>brown</font> <b>mouse</b> jumped over the lazy <b>cat</b>.";
            switch(i){
            case 0:
                return new ByteArrayInputStream(_htmlText.getBytes("utf-8"));
            case 1:
                return _htmlText;
            }
        }catch(UnsupportedEncodingException e){ e.printStackTrace(); }
        return null;
    }

    static byte[] getContent(InputStream is)
    {
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        try{
            int read;
            while( -1 != (read = is.read()) ){
                tmp.write(read);
            };
        } catch( IOException e ) {
            e.printStackTrace();
        }
        return tmp.toByteArray();
    }

    static void Dump(byte[] b){
        System.err.println( new String(b) );
    };

    void setClipboardContents(
        Transferable contents,
        ClipboardOwner owner
    ) {
        synchronized (m_clipboard) {
            boolean set = false;
            while (!set) {
                try {
                    m_clipboard.setContents(contents, owner);
                    set = true;
                } catch (IllegalStateException ise) {
                    try {
                        Thread.sleep(100);
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    Transferable getClipboardContents(Object requestor)
    {
        synchronized (m_clipboard) {
            while (true) {
                try {
                    Transferable t = m_clipboard.getContents(requestor);
                    return t;
                } catch (IllegalStateException ise) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}


class THTMLProducer extends HTMLTransferer {

    boolean[] passedArray;
    int fi = 0; // next format index
    private boolean isFirstCallOfLostOwnership = true;

    THTMLProducer() {
        passedArray = new boolean[HTMLTransferTest.HTMLFlavors.length];
    }

    void begin() {
        setClipboardContents(
            new HTMLSelection(
                HTMLTransferTest.SyncFlavor,
                S_BEGIN
            ),
            this
        );
    }

    public void lostOwnership(Clipboard cb, Transferable contents) {
        System.err.println("{PRODUCER: lost clipboard ownership");
        Transferable t = getClipboardContents(null);
        if (t.isDataFlavorSupported(HTMLTransferTest.SyncFlavor)) {
            SyncMessage msg = null;
            // for test going on if t.getTransferData() will throw an exception
            if (isFirstCallOfLostOwnership) {
                isFirstCallOfLostOwnership = false;
                msg = S_BEGIN_ANSWER;
            } else {
                msg = S_PASSED;
            }
            try {
                msg = (SyncMessage)t.getTransferData(HTMLTransferTest.SyncFlavor);
                System.err.println("++received message: " + msg);
            } catch (Exception e) {
                System.err.println("Can't getTransferData-message: " + e);
            }
            if( msg.equals(S_PASSED) ){
                notifyTransferSuccess(true);
            } else if( msg.equals(S_FAILED) ){
                notifyTransferSuccess(false);
            } else if (!msg.equals(S_BEGIN_ANSWER)) {
                throw new RuntimeException("wrong message in " +
                    "THTMLProducer.lostOwnership(): " + msg +
                    "  (possibly due to bug 4683804)");
            }
        } else {
            throw new RuntimeException(
                "DataFlavor.stringFlavor is not "
                + "suppurted by transferable in "
                + "THTMLProducer.lostOwnership()"
            );
        }

        if (fi < HTMLTransferTest.HTMLFlavors.length) {
            System.err.println(
                "testing native HTML format \""
                + HTMLTransferTest.HTMLFlavors[fi].getMimeType()
                + "\"..."
            );
            //leaveFormat( HTMLTransferTest.HTMLFlavors[fi].getMimeType() );
            setClipboardContents(
                new HTMLSelection(
                    HTMLTransferTest.HTMLFlavors[fi],
                    HTMLTransferer.createTRInstance(fi)
                ),
                this
            );
        } else {
            setClipboardContents(
                new HTMLSelection(
                    HTMLTransferTest.SyncFlavor,
                    S_END
                ),
                null
            );
        }
        System.err.println("}PRODUCER: lost clipboard ownership");
    }


    void notifyTransferSuccess(boolean status) {
        passedArray[fi] = status;
        fi++;
    }

}


class THTMLConsumer extends HTMLTransferer
{
    private static final Object LOCK = new Object();
    private static boolean failed;
    int fi = 0; // next format index

    public void lostOwnership(Clipboard cb, Transferable contents) {
        System.err.println("{CONSUMER: lost clipboard ownership");
        Transferable t = getClipboardContents(null);
        boolean bContinue = true;
        if(t.isDataFlavorSupported(HTMLTransferTest.SyncFlavor)) {
            try {
                SyncMessage msg = (SyncMessage)t.getTransferData(HTMLTransferTest.SyncFlavor);
                System.err.println("received message: " + msg);
                if(msg.equals(S_END)){
                    synchronized (LOCK) {
                        LOCK.notifyAll();
                    }
                    bContinue = false;
                }
            } catch (Exception e) {
                System.err.println("Can't getTransferData-message: " + e);
            }
        }
        if(bContinue){
            // all HTML formats have been processed
            System.err.println( "============================================================");
            System.err.println( "Put as " + HTMLTransferTest.HTMLFlavors[fi].getMimeType() );
            boolean bSuccess = false;
            for(int i = 0; i < HTMLTransferTest.HTMLFlavors.length; ++i) {
                System.err.println( "----------------------------------------------------------");
                if( t.isDataFlavorSupported(HTMLTransferTest.HTMLFlavors[i]) ){
                    Object im = null; //? HTML;
                    try {
                       im = t.getTransferData(HTMLTransferTest.HTMLFlavors[i]);
                       if (im == null) {
                           System.err.println("getTransferData returned null");
                       } else {
                            System.err.println( "Extract as " + HTMLTransferTest.HTMLFlavors[i].getMimeType() );
                            String stIn = "(unknown)", stOut = "(unknown)";
                            switch( i ){
                            case 0:
                                stIn = new String( getContent( (InputStream)HTMLTransferer.createTRInstance(i) ) );
                                stOut = new String( getContent((InputStream)im) );
                                bSuccess = stIn.equals(stOut);
                                break;
                            case 1:
                                stIn = (String)HTMLTransferer.createTRInstance(i);
                                stOut = (String)im;
                                int head = stOut.indexOf("<HTML><BODY>");
                                if (head >= 0) {
                                    stOut = stOut.substring(head + 12, stOut.length() - 14);
                                }
                                bSuccess = stIn.equals(stOut);
                                break;
                            default:
                                bSuccess = HTMLTransferer.createTRInstance(i).equals(im);
                                break;
                            };
                            System.err.println("in :" + stIn);
                            System.err.println("out:" + stOut);
                       };
                    } catch (Exception e) {
                        System.err.println("Can't getTransferData: " + e);
                    }
                    if(!bSuccess)
                        System.err.println("transferred DATA is different from initial DATA\n");
                } else {
                    System.err.println("Flavor is not supported by transferable:\n");
                    DataFlavor[] dfs = t.getTransferDataFlavors();
                    int ii;
                    for(ii = 0; ii < dfs.length; ++ii)
                        System.err.println("Supported:" + dfs[ii] + "\n");
                    dfs = HTMLTransferTest.HTMLFlavors;
                    for(ii = 0; ii < dfs.length; ++ii)
                        System.err.println("Accepted:" + dfs[ii] + "\n" );
                }
            }
            System.err.println( "----------------------------------------------------------");
            notifyTransferSuccess(bSuccess);
            System.err.println( "============================================================");
            ++fi;
        }
        System.err.println("}CONSUMER: lost clipboard ownership");
    }


    void notifyTransferSuccess(boolean status) {
        System.err.println(
            "format "
            + (status
                ? "passed"
                : "failed"
            )
            + "!!!"
        );
        setClipboardContents(
            new HTMLSelection(
                HTMLTransferTest.SyncFlavor,
                status
                    ? S_PASSED
                    : S_FAILED
            ),
            this
        );
    }


    public static void main(String[] args) {
        try {
            System.err.println("{CONSUMER: start");
            THTMLConsumer ic = new THTMLConsumer();
            ic.setClipboardContents(
                new HTMLSelection(
                    HTMLTransferTest.SyncFlavor,
                    S_BEGIN_ANSWER
                ),
                ic
            );
            synchronized (LOCK) {
                LOCK.wait();
            }
            System.err.println("}CONSUMER: start");
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(HTMLTransferTest.CODE_FAILURE);
        }
    }

}


/**
 * A <code>Transferable</code> which implements the capability required
 * to transfer an <code>HTML</code>.
 *
 * This <code>Transferable</code> properly supports
 * <code>HTMLTransferTest.HTMLFlavors</code>.
 * and all equivalent flavors.
 * No other <code>DataFlavor</code>s are supported.
 *
 * @see java.awt.datatransfer.HTMLTransferTest.HTMLFlavors
 */
class HTMLSelection implements Transferable {
    private DataFlavor m_flavor;
    private Object m_data;

    /**
     * Creates a <code>Transferable</code> capable of transferring
     * the specified <code>String</code>.
     */
    public HTMLSelection(
        DataFlavor flavor,
        Object data
    ){
        m_flavor = flavor;
        m_data = data;
    }

    /**
     * Returns an array of flavors in which this <code>Transferable</code>
     * can provide the data. <code>DataFlavor.stringFlavor</code>
     * is properly supported.
     * Support for <code>DataFlavor.plainTextFlavor</code> is
     * <b>deprecated</b>.
     *
     * @return an array of length one, whose element is <code>DataFlavor.
     *         HTMLTransferTest.HTMLFlavors</code>
     */
    public DataFlavor[] getTransferDataFlavors() {
        // returning flavors itself would allow client code to modify
        // our internal behavior
        return new DataFlavor[]{ m_flavor } ;
    }

    /**
     * Returns whether the requested flavor is supported by this
     * <code>Transferable</code>.
     *
     * @param flavor the requested flavor for the data
     * @return true if <code>flavor</code> is equal to
     *   <code>HTMLTransferTest.HTMLFlavors</code>;
     *   false if <code>flavor</code>
     *   is not one of the above flavors
     * @throws NullPointerException if flavor is <code>null</code>
     */
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        System.err.println("Have:" + flavor + " Can:" + m_flavor);
        if(flavor.equals(m_flavor))
            return true;
        return false;
    }

    /**
     * Returns the <code>Transferable</code>'s data in the requested
     * <code>DataFlavor</code> if possible. If the desired flavor is
     * <code>HTMLTransferTest.HTMLFlavors</code>, or an equivalent flavor,
     * the <code>HTML</code> representing the selection is
     * returned.
     *
     * @param flavor the requested flavor for the data
     * @return the data in the requested flavor, as outlined above
     * @throws UnsupportedFlavorException if the requested data flavor is
     *         not equivalent to <code>HTMLTransferTest.HTMLFlavors</code>
     * @throws IOException if an IOException occurs while retrieving the data.
     *         By default, <code>HTMLSelection</code> never throws
     *         this exception, but a subclass may.
     * @throws NullPointerException if flavor is <code>null</code>
     */
    public Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException, IOException
    {
        if (flavor.equals(m_flavor)) {
            return (Object)m_data;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

} // class HTMLSelection
