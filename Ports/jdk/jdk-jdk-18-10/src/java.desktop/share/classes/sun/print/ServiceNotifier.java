/*
 * Copyright (c) 2000, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Vector;

import javax.print.PrintService;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.event.PrintServiceAttributeEvent;
import javax.print.event.PrintServiceAttributeListener;

/*
 * A utility class usable by all print services for managing listeners
 * The services create an instance and delegate the listener callback
 * management to this class. The ServiceNotifier calls back to the service
 * to obtain the state of the attributes and notifies the listeners of
 * any changes.
 */
class ServiceNotifier extends Thread {

    private PrintService service;
    private Vector<PrintServiceAttributeListener> listeners;
    private boolean stop = false;
    private PrintServiceAttributeSet lastSet;

    /*
     * If adding any other constructors, always call the 5-args
     * super-class constructor passing "false" for inherit-locals.
     */
    ServiceNotifier(PrintService service) {
        super(null, null, service.getName() + " notifier", 0, false);
        this.service = service;
        listeners = new Vector<>();
        try {
              setPriority(Thread.NORM_PRIORITY-1);
              setDaemon(true);
              start();
        } catch (SecurityException e) {
        }
    }

    void addListener(PrintServiceAttributeListener listener) {
        synchronized (this) {
            if (listener == null || listeners == null) {
                return;
            }
            listeners.add(listener);
        }
    }

   void removeListener(PrintServiceAttributeListener listener) {
         synchronized (this) {
            if (listener == null || listeners == null) {
                return;
            }
            listeners.remove(listener);
        }
    }

   boolean isEmpty() {
     return (listeners == null || listeners.isEmpty());
   }

   void stopNotifier() {
      stop = true;
   }

    /* If a service submits a job it may call this method which may prompt
     * immediate notification of listeners.
     */
    void wake() {
        try {
            interrupt();
        } catch (SecurityException e) {
        }
    }

   /* A heuristic is used to calculate sleep time.
     * 10 times the time taken to loop through all the listeners, with
     * a minimum of 15 seconds. Ensures this won't take more than 10%
     * of available time.
     */
    public void run() {

       long minSleepTime = 15000;
       long sleepTime = 2000;
       HashPrintServiceAttributeSet attrs;
       PrintServiceAttributeEvent attrEvent;
       PrintServiceAttributeListener listener;
       PrintServiceAttributeSet psa;

       while (!stop) {
           try {
                Thread.sleep(sleepTime);
           } catch (InterruptedException e) {
           }
           synchronized (this) {
               if (listeners == null) {
                   continue;
               }
               long startTime = System.currentTimeMillis();
               if (listeners != null) {
                    if (service instanceof AttributeUpdater) {
                       psa =
                          ((AttributeUpdater)service).getUpdatedAttributes();
                    } else {
                       psa = service.getAttributes();
                    }
                    if (psa != null && !psa.isEmpty()) {
                        for (int i = 0; i < listeners.size() ; i++) {
                            listener = listeners.elementAt(i);
                            attrs =
                                new HashPrintServiceAttributeSet(psa);
                            attrEvent =
                                new PrintServiceAttributeEvent(service, attrs);
                            listener.attributeUpdate(attrEvent);
                        }
                    }
               }
               sleepTime = (System.currentTimeMillis()-startTime)*10;
               if (sleepTime < minSleepTime) {
                   sleepTime = minSleepTime;
               }
           }
       }
    }

}
