/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.print;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.print.DocFlavor;
import javax.print.MultiDocPrintService;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.PrinterURI;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.nio.file.Files;

/*
 * Remind: This class uses solaris commands. We also need a linux
 * version
 */
public class PrintServiceLookupProvider extends PrintServiceLookup
    implements BackgroundServiceLookup, Runnable {

    /* Remind: the current implementation is static, as its assumed
     * its preferable to minimize creation of PrintService instances.
     * Later we should add logic to add/remove services on the fly which
     * will take a hit of needing to regather the list of services.
     */
    private String defaultPrinter;
    private PrintService defaultPrintService;
    private PrintService[] printServices; /* includes the default printer */
    private Vector<BackgroundLookupListener> lookupListeners = null;
    private static String debugPrefix = "PrintServiceLookupProvider>> ";
    private static boolean pollServices = true;
    private static final int DEFAULT_MINREFRESH = 120;  // 2 minutes
    private static int minRefreshTime = DEFAULT_MINREFRESH;

    @SuppressWarnings("removal")
    static String osname = java.security.AccessController.doPrivileged(
            new sun.security.action.GetPropertyAction("os.name"));

    // List of commands used to deal with the printer queues on AIX
    String[] lpNameComAix = {
      "/usr/bin/lsallq",
      "/usr/bin/lpstat -W -p|/usr/bin/expand|/usr/bin/cut -f1 -d' '",
      "/usr/bin/lpstat -W -d|/usr/bin/expand|/usr/bin/cut -f1 -d' '",
      "/usr/bin/lpstat -W -v"
    };
    private static final int aix_lsallq = 0;
    private static final int aix_lpstat_p = 1;
    private static final int aix_lpstat_d = 2;
    private static final int aix_lpstat_v = 3;
    private static int aix_defaultPrinterEnumeration = aix_lsallq;

    static {
        /* The system property "sun.java2d.print.polling"
         * can be used to force the printing code to poll or not poll
         * for PrintServices.
         */
        @SuppressWarnings("removal")
        String pollStr = java.security.AccessController.doPrivileged(
            new sun.security.action.GetPropertyAction("sun.java2d.print.polling"));

        if (pollStr != null) {
            if (pollStr.equalsIgnoreCase("true")) {
                pollServices = true;
            } else if (pollStr.equalsIgnoreCase("false")) {
                pollServices = false;
            }
        }

        /* The system property "sun.java2d.print.minRefreshTime"
         * can be used to specify minimum refresh time (in seconds)
         * for polling PrintServices.  The default is 120.
         */
        @SuppressWarnings("removal")
        String refreshTimeStr = java.security.AccessController.doPrivileged(
            new sun.security.action.GetPropertyAction(
                "sun.java2d.print.minRefreshTime"));

        if (refreshTimeStr != null) {
            try {
                minRefreshTime = (Integer.valueOf(refreshTimeStr)).intValue();
            } catch (NumberFormatException e) {
            }
            if (minRefreshTime < DEFAULT_MINREFRESH) {
                minRefreshTime = DEFAULT_MINREFRESH;
            }
        }

        /* The system property "sun.java2d.print.aix.lpstat"
         * can be used to force the usage of 'lpstat -p' to enumerate all
         * printer queues. By default we use 'lsallq', because 'lpstat -p' can
         * take lots of time if thousands of printers are attached to a server.
         */
        if (isAIX()) {
            @SuppressWarnings("removal")
            String aixPrinterEnumerator = java.security.AccessController.doPrivileged(
                new sun.security.action.GetPropertyAction("sun.java2d.print.aix.lpstat"));

            if (aixPrinterEnumerator != null) {
                if (aixPrinterEnumerator.equalsIgnoreCase("lpstat")) {
                    aix_defaultPrinterEnumeration = aix_lpstat_p;
                } else if (aixPrinterEnumerator.equalsIgnoreCase("lsallq")) {
                    aix_defaultPrinterEnumeration = aix_lsallq;
                }
            }
        }
    }

    static boolean isMac() {
        return osname.startsWith("Mac");
    }

    static boolean isLinux() {
        return (osname.equals("Linux"));
    }

    static boolean isBSD() {
        return (osname.equals("Linux") ||
                osname.contains("OS X"));
    }

    static boolean isAIX() {
        return osname.equals("AIX");
    }

    static final int UNINITIALIZED = -1;
    static final int BSD_LPD = 0;
    static final int BSD_LPD_NG = 1;

    static int cmdIndex = UNINITIALIZED;

    String[] lpcFirstCom = {
        "/usr/sbin/lpc status | grep : | sed -ne '1,1 s/://p'",
        "/usr/sbin/lpc status | grep -E '^[ 0-9a-zA-Z_-]*@' | awk -F'@' '{print $1}'"
    };

    String[] lpcAllCom = {
        "/usr/sbin/lpc status all | grep : | sed -e 's/://'",
        "/usr/sbin/lpc status all | grep -E '^[ 0-9a-zA-Z_-]*@' | awk -F'@' '{print $1}' | sort"
    };

    String[] lpcNameCom = {
        "| grep : | sed -ne 's/://p'",
        "| grep -E '^[ 0-9a-zA-Z_-]*@' | awk -F'@' '{print $1}'"
    };


    static int getBSDCommandIndex() {
        String command  = "/usr/sbin/lpc status all";
        String[] names = execCmd(command);

        if ((names == null) || (names.length == 0)) {
            return BSD_LPD_NG;
        }

        for (int i=0; i<names.length; i++) {
            if (names[i].indexOf('@') != -1) {
                return BSD_LPD_NG;
            }
        }

        return BSD_LPD;
    }


    public PrintServiceLookupProvider() {
        // start the printer listener thread
        if (pollServices) {
            Thread thr = new Thread(null, new PrinterChangeListener(),
                                    "PrinterListener", 0, false);
            thr.setDaemon(true);
            thr.start();
            IPPPrintService.debug_println(debugPrefix+"polling turned on");
        }
    }

    /* Want the PrintService which is default print service to have
     * equality of reference with the equivalent in list of print services
     * This isn't required by the API and there's a risk doing this will
     * lead people to assume its guaranteed.
     */
    public synchronized PrintService[] getPrintServices() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPrintJobAccess();
        }

        if (printServices == null || !pollServices) {
            refreshServices();
        }
        if (printServices == null) {
            return new PrintService[0];
        } else {
            return printServices.clone();
        }
    }

    private int addPrintServiceToList(ArrayList<PrintService> printerList, PrintService ps) {
        int index = printerList.indexOf(ps);
        // Check if PrintService with same name is already in the list.
        if (CUPSPrinter.isCupsRunning() && index != -1) {
            // Bug in Linux: Duplicate entry of a remote printer
            // and treats it as local printer but it is returning wrong
            // information when queried using IPP. Workaround is to remove it.
            // Even CUPS ignores these entries as shown in lpstat or using
            // their web configuration.
            PrinterURI uri = ps.getAttribute(PrinterURI.class);
            if (uri.getURI().getHost().equals("localhost")) {
                IPPPrintService.debug_println(debugPrefix+"duplicate PrintService, ignoring the new local printer: "+ps);
                return index;  // Do not add this.
            }
            PrintService oldPS = printerList.get(index);
            uri = oldPS.getAttribute(PrinterURI.class);
            if (uri.getURI().getHost().equals("localhost")) {
                IPPPrintService.debug_println(debugPrefix+"duplicate PrintService, removing existing local printer: "+oldPS);
                printerList.remove(oldPS);
            } else {
                return index;
            }
        }
        printerList.add(ps);
        return (printerList.size() - 1);
    }


    // refreshes "printServices"
    public synchronized void refreshServices() {
        /* excludes the default printer */
        String[] printers = null; // array of printer names
        String[] printerURIs = null; //array of printer URIs

        try {
            getDefaultPrintService();
        } catch (Throwable t) {
            IPPPrintService.debug_println(debugPrefix+
              "Exception getting default printer : " + t);
        }
        if (CUPSPrinter.isCupsRunning()) {
            try {
                printerURIs = CUPSPrinter.getAllPrinters();
                IPPPrintService.debug_println("CUPS URIs = " + printerURIs);
                if (printerURIs != null) {
                    for (int p = 0; p < printerURIs.length; p++) {
                       IPPPrintService.debug_println("URI="+printerURIs[p]);
                    }
                }
            } catch (Throwable t) {
            IPPPrintService.debug_println(debugPrefix+
              "Exception getting all CUPS printers : " + t);
            }
            if ((printerURIs != null) && (printerURIs.length > 0)) {
                printers = new String[printerURIs.length];
                for (int i=0; i<printerURIs.length; i++) {
                    int lastIndex = printerURIs[i].lastIndexOf("/");
                    printers[i] = printerURIs[i].substring(lastIndex+1);
                }
            }
        } else {
            if (isMac()) {
                printers = getAllPrinterNamesSysV();
            } else if (isAIX()) {
                printers = getAllPrinterNamesAIX();
            } else { //BSD
                printers = getAllPrinterNamesBSD();
            }
        }

        if (printers == null) {
            if (defaultPrintService != null) {
                printServices = new PrintService[1];
                printServices[0] = defaultPrintService;
            } else {
                printServices = null;
            }
            return;
        }

        ArrayList<PrintService> printerList = new ArrayList<>();
        int defaultIndex = -1;
        for (int p=0; p<printers.length; p++) {
            if (printers[p] == null) {
                continue;
            }
            if ((defaultPrintService != null)
                && printers[p].equals(getPrinterDestName(defaultPrintService))) {
                defaultIndex = addPrintServiceToList(printerList, defaultPrintService);
            } else {
                if (printServices == null) {
                    IPPPrintService.debug_println(debugPrefix+
                                                  "total# of printers = "+printers.length);

                    if (CUPSPrinter.isCupsRunning()) {
                        try {
                            addPrintServiceToList(printerList,
                                                  new IPPPrintService(printers[p],
                                                                   printerURIs[p],
                                                                   true));
                        } catch (Exception e) {
                            IPPPrintService.debug_println(debugPrefix+
                                                          " getAllPrinters Exception "+
                                                          e);

                        }
                    } else {
                        printerList.add(new UnixPrintService(printers[p]));
                    }
                } else {
                    int j;
                    for (j=0; j<printServices.length; j++) {
                        if (printServices[j] != null) {
                            if (printers[p].equals(getPrinterDestName(printServices[j]))) {
                                printerList.add(printServices[j]);
                                printServices[j] = null;
                                break;
                            }
                        }
                    }

                    if (j == printServices.length) {      // not found?
                        if (CUPSPrinter.isCupsRunning()) {
                            try {
                                addPrintServiceToList(printerList,
                                             new IPPPrintService(printers[p],
                                                                 printerURIs[p],
                                                                 true));
                            } catch (Exception e) {
                                IPPPrintService.debug_println(debugPrefix+
                                                              " getAllPrinters Exception "+
                                                              e);

                            }
                        } else {
                            printerList.add(new UnixPrintService(printers[p]));
                        }
                    }
                }
            }
        }

        // Look for deleted services and invalidate these
        if (printServices != null) {
            for (int j=0; j < printServices.length; j++) {
                if ((printServices[j] instanceof UnixPrintService) &&
                    (!printServices[j].equals(defaultPrintService))) {
                    ((UnixPrintService)printServices[j]).invalidateService();
                }
            }
        }

        //if defaultService is not found in printerList
        if (defaultIndex == -1 && defaultPrintService != null) {
            defaultIndex = addPrintServiceToList(printerList, defaultPrintService);
        }

        printServices = printerList.toArray(new PrintService[] {});

        // swap default with the first in the list
        if (defaultIndex > 0) {
            PrintService saveService = printServices[0];
            printServices[0] = printServices[defaultIndex];
            printServices[defaultIndex] = saveService;
        }
    }

    private boolean matchesAttributes(PrintService service,
                                      PrintServiceAttributeSet attributes) {

        Attribute [] attrs =  attributes.toArray();
        for (int i=0; i<attrs.length; i++) {
            @SuppressWarnings("unchecked")
            Attribute serviceAttr
                = service.getAttribute((Class<PrintServiceAttribute>)attrs[i].getCategory());
            if (serviceAttr == null || !serviceAttr.equals(attrs[i])) {
                return false;
            }
        }
        return true;
    }

      /* This checks for validity of the printer name before passing as
       * parameter to a shell command.
       */
      private boolean checkPrinterName(String s) {
        char c;

        for (int i=0; i < s.length(); i++) {
          c = s.charAt(i);
          if (Character.isLetterOrDigit(c) ||
              c == '-' || c == '_' || c == '.' || c == '/') {
            continue;
          } else {
            return false;
          }
        }
        return true;
      }

    /*
     * Gets the printer name compatible with the list of printers returned by
     * the system when we query default or all the available printers.
     */
    private String getPrinterDestName(PrintService ps) {
        if (isMac()) {
            return ((IPPPrintService)ps).getDest();
        }
        return ps.getName();
    }

    /* On a network with many (hundreds) of network printers, it
     * can save several seconds if you know all you want is a particular
     * printer, to ask for that printer rather than retrieving all printers.
     */
    private PrintService getServiceByName(PrinterName nameAttr) {
        String name = nameAttr.getValue();
        if (name == null || name.isEmpty() || !checkPrinterName(name)) {
            return null;
        }
        /* check if all printers are already available */
        if (printServices != null) {
            for (PrintService printService : printServices) {
                PrinterName printerName = printService.getAttribute(PrinterName.class);
                if (printerName.getValue().equals(name)) {
                    return printService;
                }
            }
        }
        /* take CUPS into account first */
        if (CUPSPrinter.isCupsRunning()) {
            try {
                return new IPPPrintService(name,
                                           new URL("http://"+
                                                   CUPSPrinter.getServer()+":"+
                                                   CUPSPrinter.getPort()+"/"+
                                                   name));
            } catch (Exception e) {
                IPPPrintService.debug_println(debugPrefix+
                                              " getServiceByName Exception "+
                                              e);
            }
        }
        /* fallback if nothing not having a printer at this point */
        PrintService printer = null;
        if (isMac()) {
            printer = getNamedPrinterNameSysV(name);
        } else if (isAIX()) {
            printer = getNamedPrinterNameAIX(name);
        } else {
            printer = getNamedPrinterNameBSD(name);
        }
        return printer;
    }

    private PrintService[]
        getPrintServices(PrintServiceAttributeSet serviceSet) {

        if (serviceSet == null || serviceSet.isEmpty()) {
            return getPrintServices();
        }

        /* Typically expect that if a service attribute is specified that
         * its a printer name and there ought to be only one match.
         * Directly retrieve that service and confirm
         * that it meets the other requirements.
         * If printer name isn't mentioned then go a slow path checking
         * all printers if they meet the reqiremements.
         */
        PrintService[] services;
        PrinterName name = (PrinterName)serviceSet.get(PrinterName.class);
        PrintService defService;
        if (name != null && (defService = getDefaultPrintService()) != null) {
            /* To avoid execing a unix command  see if the client is asking
             * for the default printer by name, since we already have that
             * initialised.
             */

            PrinterName defName = defService.getAttribute(PrinterName.class);

            if (defName != null && name.equals(defName)) {
                if (matchesAttributes(defService, serviceSet)) {
                    services = new PrintService[1];
                    services[0] = defService;
                    return services;
                } else {
                    return new PrintService[0];
                }
            } else {
                /* Its not the default service */
                PrintService service = getServiceByName(name);
                if (service != null &&
                    matchesAttributes(service, serviceSet)) {
                    services = new PrintService[1];
                    services[0] = service;
                    return services;
                } else {
                    return new PrintService[0];
                }
            }
        } else {
            /* specified service attributes don't include a name.*/
            Vector<PrintService> matchedServices = new Vector<>();
            services = getPrintServices();
            for (int i = 0; i< services.length; i++) {
                if (matchesAttributes(services[i], serviceSet)) {
                    matchedServices.add(services[i]);
                }
            }
            services = new PrintService[matchedServices.size()];
            for (int i = 0; i< services.length; i++) {
                services[i] = matchedServices.elementAt(i);
            }
            return services;
        }
    }

    /*
     * If service attributes are specified then there must be additional
     * filtering.
     */
    public PrintService[] getPrintServices(DocFlavor flavor,
                                           AttributeSet attributes) {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
          security.checkPrintJobAccess();
        }
        PrintRequestAttributeSet requestSet = null;
        PrintServiceAttributeSet serviceSet = null;

        if (attributes != null && !attributes.isEmpty()) {

            requestSet = new HashPrintRequestAttributeSet();
            serviceSet = new HashPrintServiceAttributeSet();

            Attribute[] attrs = attributes.toArray();
            for (int i=0; i<attrs.length; i++) {
                if (attrs[i] instanceof PrintRequestAttribute) {
                    requestSet.add(attrs[i]);
                } else if (attrs[i] instanceof PrintServiceAttribute) {
                    serviceSet.add(attrs[i]);
                }
            }
        }

        PrintService[] services = getPrintServices(serviceSet);
        if (services.length == 0) {
            return services;
        }

        if (CUPSPrinter.isCupsRunning()) {
            ArrayList<PrintService> matchingServices = new ArrayList<>();
            for (int i=0; i<services.length; i++) {
                try {
                    if (services[i].
                        getUnsupportedAttributes(flavor, requestSet) == null) {
                        matchingServices.add(services[i]);
                    }
                } catch (IllegalArgumentException e) {
                }
            }
            services = new PrintService[matchingServices.size()];
            return matchingServices.toArray(services);

        } else {
            // We only need to compare 1 PrintService because all
            // UnixPrintServices are the same anyway.  We will not use
            // default PrintService because it might be null.
            PrintService service = services[0];
            if ((flavor == null ||
                 service.isDocFlavorSupported(flavor)) &&
                 service.getUnsupportedAttributes(flavor, requestSet) == null)
            {
                return services;
            } else {
                return new PrintService[0];
            }
        }
    }

    /*
     * return empty array as don't support multi docs
     */
    public MultiDocPrintService[]
        getMultiDocPrintServices(DocFlavor[] flavors,
                                 AttributeSet attributes) {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
          security.checkPrintJobAccess();
        }
        return new MultiDocPrintService[0];
    }


    public synchronized PrintService getDefaultPrintService() {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
          security.checkPrintJobAccess();
        }

        // clear defaultPrintService
        defaultPrintService = null;
        String psuri = null;

        IPPPrintService.debug_println("isRunning ? "+
                                      (CUPSPrinter.isCupsRunning()));
        if (CUPSPrinter.isCupsRunning()) {
            String[] printerInfo = CUPSPrinter.getDefaultPrinter();
            if (printerInfo != null && printerInfo.length >= 2) {
                defaultPrinter = printerInfo[0];
                psuri = printerInfo[1];
            }
        } else {
            if (isMac()) {
                defaultPrinter = getDefaultPrinterNameSysV();
            } else if (isAIX()) {
                defaultPrinter = getDefaultPrinterNameAIX();
            } else {
                defaultPrinter = getDefaultPrinterNameBSD();
            }
        }
        if (defaultPrinter == null) {
            return null;
        }
        defaultPrintService = null;
        if (printServices != null) {
            for (int j=0; j<printServices.length; j++) {
                if (defaultPrinter.equals(getPrinterDestName(printServices[j]))) {
                    defaultPrintService = printServices[j];
                    break;
                }
            }
        }
        if (defaultPrintService == null) {
            if (CUPSPrinter.isCupsRunning()) {
                try {
                    PrintService defaultPS;
                    if ((psuri != null) && !psuri.startsWith("file")) {
                        defaultPS = new IPPPrintService(defaultPrinter,
                                                        psuri, true);
                    } else {
                        defaultPS = new IPPPrintService(defaultPrinter,
                                            new URL("http://"+
                                                    CUPSPrinter.getServer()+":"+
                                                    CUPSPrinter.getPort()+"/"+
                                                    defaultPrinter));
                    }
                    defaultPrintService = defaultPS;
                } catch (Exception e) {
                }
            } else {
                defaultPrintService = new UnixPrintService(defaultPrinter);
            }
        }

        return defaultPrintService;
    }

    public synchronized void
        getServicesInbackground(BackgroundLookupListener listener) {
        if (printServices != null) {
            listener.notifyServices(printServices);
        } else {
            if (lookupListeners == null) {
                lookupListeners = new Vector<>();
                lookupListeners.add(listener);
                Thread lookupThread = new Thread(this);
                lookupThread.start();
            } else {
                lookupListeners.add(listener);
            }
        }
    }

    /* This method isn't used in most cases because we rely on code in
     * javax.print.PrintServiceLookup. This is needed just for the cases
     * where those interfaces are by-passed.
     */
    private PrintService[] copyOf(PrintService[] inArr) {
        if (inArr == null || inArr.length == 0) {
            return inArr;
        } else {
            PrintService []outArr = new PrintService[inArr.length];
            System.arraycopy(inArr, 0, outArr, 0, inArr.length);
            return outArr;
        }
    }

    public void run() {
        PrintService[] services = getPrintServices();
        synchronized (this) {
            BackgroundLookupListener listener;
            for (int i=0; i<lookupListeners.size(); i++) {
                listener = lookupListeners.elementAt(i);
                listener.notifyServices(copyOf(services));
            }
            lookupListeners = null;
        }
    }

    private String getDefaultPrinterNameBSD() {
        if (cmdIndex == UNINITIALIZED) {
            cmdIndex = getBSDCommandIndex();
        }
        String[] names = execCmd(lpcFirstCom[cmdIndex]);
        if (names == null || names.length == 0) {
            return null;
        }

        if ((cmdIndex==BSD_LPD_NG) &&
            (names[0].startsWith("missingprinter"))) {
            return null;
        }
        return names[0];
    }

    private PrintService getNamedPrinterNameBSD(String name) {
      if (cmdIndex == UNINITIALIZED) {
        cmdIndex = getBSDCommandIndex();
      }
      String command = "/usr/sbin/lpc status " + name + lpcNameCom[cmdIndex];
      String[] result = execCmd(command);

      if (result == null || !(result[0].equals(name))) {
          return null;
      }
      return new UnixPrintService(name);
    }

    private String[] getAllPrinterNamesBSD() {
        if (cmdIndex == UNINITIALIZED) {
            cmdIndex = getBSDCommandIndex();
        }
        String[] names = execCmd(lpcAllCom[cmdIndex]);
        if (names == null || names.length == 0) {
          return null;
        }
        return names;
    }

    static String getDefaultPrinterNameSysV() {
        String defaultPrinter = "lp";
        String command = "/usr/bin/lpstat -d";

        String [] names = execCmd(command);
        if (names == null || names.length == 0) {
            return defaultPrinter;
        } else {
            int index = names[0].indexOf(":");
            if (index == -1  || (names[0].length() <= index+1)) {
                return null;
            } else {
                String name = names[0].substring(index+1).trim();
                if (name.length() == 0) {
                    return null;
                } else {
                    return name;
                }
            }
        }
    }

    private PrintService getNamedPrinterNameSysV(String name) {

        String command = "/usr/bin/lpstat -v " + name;
        String []result = execCmd(command);

        if (result == null || result[0].indexOf("unknown printer") > 0) {
            return null;
        } else {
            return new UnixPrintService(name);
        }
    }

    private String[] getAllPrinterNamesSysV() {
        String defaultPrinter = "lp";
        String command = "/usr/bin/lpstat -v|/usr/bin/expand|/usr/bin/cut -f3 -d' ' |/usr/bin/cut -f1 -d':' | /usr/bin/sort";

        String [] names = execCmd(command);
        ArrayList<String> printerNames = new ArrayList<>();
        for (int i=0; i < names.length; i++) {
            if (!names[i].equals("_default") &&
                !names[i].equals(defaultPrinter) &&
                !names[i].isEmpty()) {
                printerNames.add(names[i]);
            }
        }
        return printerNames.toArray(new String[printerNames.size()]);
    }

    private String getDefaultPrinterNameAIX() {
        String[] names = execCmd(lpNameComAix[aix_lpstat_d]);
        // Remove headers and bogus entries added by remote printers.
        names = UnixPrintService.filterPrinterNamesAIX(names);
        if (names == null || names.length != 1) {
            // No default printer found
            return null;
        } else {
            return names[0];
        }
    }

    private PrintService getNamedPrinterNameAIX(String name) {
        // On AIX there should be no blank after '-v'.
        String[] result = execCmd(lpNameComAix[aix_lpstat_v] + name);
        // Remove headers and bogus entries added by remote printers.
        result = UnixPrintService.filterPrinterNamesAIX(result);
        if (result == null || result.length != 1) {
            return null;
        } else {
            return new UnixPrintService(name);
        }
    }

    private String[] getAllPrinterNamesAIX() {
        // Determine all printers of the system.
        String [] names = execCmd(lpNameComAix[aix_defaultPrinterEnumeration]);

        // Remove headers and bogus entries added by remote printers.
        names = UnixPrintService.filterPrinterNamesAIX(names);

        ArrayList<String> printerNames = new ArrayList<String>();
        for ( int i=0; i < names.length; i++) {
            printerNames.add(names[i]);
        }
        return printerNames.toArray(new String[printerNames.size()]);
    }

    @SuppressWarnings("removal")
    static String[] execCmd(final String command) {
        ArrayList<String> results = null;
        try {
            final String[] cmd = new String[3];
            if (isAIX()) {
                cmd[0] = "/usr/bin/sh";
                cmd[1] = "-c";
                cmd[2] = "env LC_ALL=C " + command;
            } else {
                cmd[0] = "/bin/sh";
                cmd[1] = "-c";
                cmd[2] = "LC_ALL=C " + command;
            }

            results = AccessController.doPrivileged(
                new PrivilegedExceptionAction<ArrayList<String>>() {
                    public ArrayList<String> run() throws IOException {

                        Process proc;
                        BufferedReader bufferedReader = null;
                        File f = Files.createTempFile("prn","xc").toFile();
                        cmd[2] = cmd[2]+">"+f.getAbsolutePath();

                        proc = Runtime.getRuntime().exec(cmd);
                        try {
                            boolean done = false; // in case of interrupt.
                            while (!done) {
                                try {
                                    proc.waitFor();
                                    done = true;
                                } catch (InterruptedException e) {
                                }
                            }

                            if (proc.exitValue() == 0) {
                                FileReader reader = new FileReader(f);
                                bufferedReader = new BufferedReader(reader);
                                String line;
                                ArrayList<String> results = new ArrayList<>();
                                while ((line = bufferedReader.readLine())
                                       != null) {
                                    results.add(line);
                                }
                                return results;
                            }
                        } finally {
                            f.delete();
                            // promptly close all streams.
                            if (bufferedReader != null) {
                                bufferedReader.close();
                            }
                            proc.getInputStream().close();
                            proc.getErrorStream().close();
                            proc.getOutputStream().close();
                        }
                        return null;
                    }
                });
        } catch (PrivilegedActionException e) {
        }
        if (results == null) {
            return new String[0];
        } else {
            return results.toArray(new String[results.size()]);
        }
    }

    private class PrinterChangeListener implements Runnable {

        @Override
        public void run() {
            int refreshSecs;
            while (true) {
                try {
                    refreshServices();
                } catch (Exception se) {
                    IPPPrintService.debug_println(debugPrefix+"Exception in refresh thread.");
                    break;
                }

                if ((printServices != null) &&
                    (printServices.length > minRefreshTime)) {
                    // compute new refresh time 1 printer = 1 sec
                    refreshSecs = printServices.length;
                } else {
                    refreshSecs = minRefreshTime;
                }
                try {
                    Thread.sleep(refreshSecs * 1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}
