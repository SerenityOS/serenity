/*
 * Copyright (c) 2012 SAP SE. All rights reserved.
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
 * @test FieldMonitor.java
 * @bug 7158988
 * @requires vm.jvmti
 * @summary verify jvm does not crash while debugging
 * @run compile TestPostFieldModification.java
 * @run main/othervm FieldMonitor
 * @author axel.siebenborn@sap.com
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.Field;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ModificationWatchpointEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMStartEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.ModificationWatchpointRequest;

public class FieldMonitor {

  public static final String CLASS_NAME = "TestPostFieldModification";
  public static final String FIELD_NAME = "value";
  public static final String ARGUMENTS = "-Xshare:off -Xlog:gc";

  public static void main(String[] args)
      throws IOException, InterruptedException {

    //VirtualMachine vm = launchTarget(sb.toString());
    VirtualMachine vm = launchTarget(CLASS_NAME);

    System.out.println("Vm launched");

    // process events
    EventQueue eventQueue = vm.eventQueue();
    // resume the vm

    Process process = vm.process();


    // Copy target's output and error to our output and error.
    Thread outThread = new StreamRedirectThread("out reader", process.getInputStream());
    Thread errThread = new StreamRedirectThread("error reader", process.getErrorStream());

    errThread.start();
    outThread.start();

    boolean connected = true;
    int watched = 0;
    while (connected) {
        try {
            EventSet eventSet = eventQueue.remove();
            for (Event event : eventSet) {
              System.out.println("FieldMonitor-main receives: "+event);
              if (event instanceof VMStartEvent) {
                addClassWatch(vm);
              } else if (event instanceof VMDeathEvent
                  || event instanceof VMDisconnectEvent) {
                // exit
                connected = false;
              } else if (event instanceof ClassPrepareEvent) {
                // watch field on loaded class
                System.out.println("ClassPrepareEvent");
                ClassPrepareEvent classPrepEvent = (ClassPrepareEvent) event;
                ReferenceType refType = classPrepEvent
                    .referenceType();
                addFieldWatch(vm, refType);
              } else if (event instanceof ModificationWatchpointEvent) {
                watched++;
                System.out.println("sleep for 500 ms");
                Thread.sleep(500);

                ModificationWatchpointEvent modEvent = (ModificationWatchpointEvent) event;
                System.out.println("old="
                    + modEvent.valueCurrent());
                System.out.println("new=" + modEvent.valueToBe());
              }
            }
            System.out.println("resume...");
            eventSet.resume();
        } catch (com.sun.jdi.VMDisconnectedException exc) {
            // Guess this means it's not connected anymore,
            // sometimes this happens and everything else hangs, just return.
            return;
        }
    }
    // Shutdown begins when event thread terminates
    try {
        errThread.join(); // Make sure output is forwarded
        outThread.join();
    } catch (InterruptedException exc) {
        // we don't interrupt
    }

    if (watched != 11) { // init + 10 modifications in TestPostFieldModification class
        throw new Error("Expected to receive 11 times ModificationWatchpointEvent, but got "+watched);
    }
  }

  /**
   * Find a com.sun.jdi.CommandLineLaunch connector
   */
  static LaunchingConnector findLaunchingConnector() {
    List <Connector> connectors = Bootstrap.virtualMachineManager().allConnectors();
    Iterator <Connector> iter = connectors.iterator();
    while (iter.hasNext()) {
      Connector connector = iter.next();
      if (connector.name().equals("com.sun.jdi.CommandLineLaunch")) {
        return (LaunchingConnector)connector;
      }
    }
    throw new Error("No launching connector");
  }
  /**
   * Return the launching connector's arguments.
   */
 static Map <String,Connector.Argument> connectorArguments(LaunchingConnector connector, String mainArgs) {
      Map<String,Connector.Argument> arguments = connector.defaultArguments();
      for (String key : arguments.keySet()) {
        System.out.println(key);
      }

      Connector.Argument mainArg = (Connector.Argument)arguments.get("main");
      if (mainArg == null) {
          throw new Error("Bad launching connector");
      }
      mainArg.setValue(mainArgs);

      Connector.Argument optionsArg = (Connector.Argument)arguments.get("options");
      if (optionsArg == null) {
        throw new Error("Bad launching connector");
      }
      optionsArg.setValue(ARGUMENTS);
      return arguments;
  }

 static VirtualMachine launchTarget(String mainArgs) {
    LaunchingConnector connector = findLaunchingConnector();
    Map  arguments = connectorArguments(connector, mainArgs);
    try {
        return (VirtualMachine) connector.launch(arguments);
    } catch (IOException exc) {
        throw new Error("Unable to launch target VM: " + exc);
    } catch (IllegalConnectorArgumentsException exc) {
        throw new Error("Internal error: " + exc);
    } catch (VMStartException exc) {
        throw new Error("Target VM failed to initialize: " +
                        exc.getMessage());
    }
}


  private static void addClassWatch(VirtualMachine vm) {
    EventRequestManager erm = vm.eventRequestManager();
    ClassPrepareRequest classPrepareRequest = erm
        .createClassPrepareRequest();
    classPrepareRequest.addClassFilter(CLASS_NAME);
    classPrepareRequest.setEnabled(true);
  }


  private static void addFieldWatch(VirtualMachine vm,
      ReferenceType refType) {
    EventRequestManager erm = vm.eventRequestManager();
    Field field = refType.fieldByName(FIELD_NAME);
    ModificationWatchpointRequest modificationWatchpointRequest = erm
        .createModificationWatchpointRequest(field);
    modificationWatchpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
    modificationWatchpointRequest.setEnabled(true);
  }
}

class StreamRedirectThread extends Thread {

  private final BufferedReader in;

  private static final int BUFFER_SIZE = 2048;

  /**
   * Set up for copy.
   * @param name  Name of the thread
   * @param in    Stream to copy from
   * @param out   Stream to copy to
   */
  StreamRedirectThread(String name, InputStream in) {
    super(name);
    this.in = new BufferedReader(new InputStreamReader(in));
  }

  /**
   * Copy.
   */
  public void run() {
    try {
      String line;
        while ((line = in.readLine ()) != null) {
          System.out.println ("testvm: " + line);
      }
     System.out.flush();
    } catch(IOException exc) {
      System.err.println("Child I/O Transfer - " + exc);
    }
  }
}
