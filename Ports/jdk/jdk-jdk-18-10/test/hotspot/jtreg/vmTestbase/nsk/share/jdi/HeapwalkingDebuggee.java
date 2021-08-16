/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.jdi;

import java.io.*;
import java.util.*;
import java.lang.reflect.Method;
import nsk.share.Log;
import nsk.share.ObjectInstancesManager;
import nsk.share.TestBug;
import nsk.share.jpda.DebugeeArgumentHandler;
import nsk.share.jpda.IOPipe;

/*
 * Debuggee class used in tests for heapwalking(tests for VirtualMachine.instanceCounts, ReferenceType.instances, ObjectReference.referrers).
 * Handle commands related to creation of objects instances with given reference type
 * and given referrers number, use for this purposes nsk.share.ObjectInstancesManager.
 */
public class HeapwalkingDebuggee extends AbstractJDIDebuggee {
    protected ObjectInstancesManager objectInstancesManager;

    // reference of this type should be included in ObjectReference.referringObjects
    public static Set<String> includedIntoReferrersCountTypes = new HashSet<String>();

    // reference of this type should be included in ReferenceType.instances
    public static Set<String> includedIntoInstancesCountTypes = new HashSet<String>();

    static {
        includedIntoInstancesCountTypes.add(ObjectInstancesManager.STRONG_REFERENCE);
        includedIntoInstancesCountTypes.add(ObjectInstancesManager.WEAK_REFERENCE);
        includedIntoInstancesCountTypes.add(ObjectInstancesManager.SOFT_REFERENCE);
        includedIntoInstancesCountTypes.add(ObjectInstancesManager.PHANTOM_REFERENCE);
        includedIntoInstancesCountTypes.add(ObjectInstancesManager.JNI_GLOBAL_REFERENCE);
        includedIntoInstancesCountTypes.add(ObjectInstancesManager.JNI_LOCAL_REFERENCE);

        includedIntoReferrersCountTypes.add(ObjectInstancesManager.STRONG_REFERENCE);
        includedIntoReferrersCountTypes.add(ObjectInstancesManager.WEAK_REFERENCE);
        includedIntoReferrersCountTypes.add(ObjectInstancesManager.SOFT_REFERENCE);
        includedIntoReferrersCountTypes.add(ObjectInstancesManager.PHANTOM_REFERENCE);
    }

    //create number instance of class with given name, command format: createInstances:class_name:instance_count[:referrer_count:referrer_type]
    static public final String COMMAND_CREATE_INSTANCES = "createInstances";

    //'delete'(make unreachable) number instance of class with given name, command format: deleteInstances:class_name:instance_count:referrer_count
    static public final String COMMAND_DELETE_INSTANCES = "deleteInstances";

    //delete number referrers
    static public final String COMMAND_DELETE_REFERRERS = "deleteReferrers";

    //create instance with all type referrers
    static public final String COMMAND_CREATE_ALL_TYPE_REFERENCES = "createAllTypeReferences";

    // check jfr is active process
    public static boolean isJFRActive;

    protected void init(String args[]) {
        super.init(args);
        objectInstancesManager = new ObjectInstancesManager(log);
        isJFRActive = isJFRActive();
    }

    public void initDebuggee(DebugeeArgumentHandler argHandler, Log log, IOPipe pipe, String args[], boolean callExit) {
        super.initDebuggee(argHandler, log, pipe, args, callExit);
        objectInstancesManager = new ObjectInstancesManager(log);
    }

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        try {
            StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(command));
            tokenizer.whitespaceChars(':', ':');
            tokenizer.wordChars('_', '_');
            tokenizer.wordChars('$', '$');
            tokenizer.wordChars('[', ']');
            tokenizer.wordChars('|', '|');

            if (command.startsWith(COMMAND_CREATE_INSTANCES)) {
                //createInstances:class_name:instance_count[:referrer_count:referrer_type]

                tokenizer.nextToken();

                if (tokenizer.nextToken() != StreamTokenizer.TT_WORD)
                    throw new TestBug("Invalid command format: " + command);

                String className = tokenizer.sval;

                if (tokenizer.nextToken() != StreamTokenizer.TT_NUMBER)
                    throw new TestBug("Invalid command format: " + command);

                int instanceCounts = (int) tokenizer.nval;

                int referrerCount = 1;
                Set<String> referrerType = new HashSet<String>();

                if (tokenizer.nextToken() == StreamTokenizer.TT_NUMBER) {
                    referrerCount = (int) tokenizer.nval;

                    if (tokenizer.nextToken() == StreamTokenizer.TT_WORD)
                        referrerType.addAll(Arrays.asList(tokenizer.sval.split("\\|")));
                }
                if (referrerType.isEmpty()) {
                    referrerType.add(ObjectInstancesManager.STRONG_REFERENCE);
                }

                objectInstancesManager.createReferences(instanceCounts, className, referrerCount, referrerType);

                return true;
            } else if (command.startsWith(COMMAND_DELETE_INSTANCES)) {
                //deleteInstances:class_name:instance_count:referrer_count

                tokenizer.nextToken();

                if (tokenizer.nextToken() != StreamTokenizer.TT_WORD)
                    throw new TestBug("Invalid command format: " + command);

                String className = tokenizer.sval;

                if (tokenizer.nextToken() != StreamTokenizer.TT_NUMBER)
                    throw new TestBug("Invalid command format: " + command);

                int instanceCounts = (int) tokenizer.nval;

                objectInstancesManager.deleteAllReferrers(instanceCounts, className);

                return true;
            } else if (command.startsWith(COMMAND_DELETE_REFERRERS)) {
                tokenizer.nextToken();

                if (tokenizer.nextToken() != StreamTokenizer.TT_WORD)
                    throw new TestBug("Invalid command format: " + command);

                String className = tokenizer.sval;

                if (tokenizer.nextToken() != StreamTokenizer.TT_NUMBER)
                    throw new TestBug("Invalid command format: " + command);

                int referrersCount = (int) tokenizer.nval;

                Set<String> referrerTypes = new HashSet<String>();
                if (tokenizer.nextToken() == StreamTokenizer.TT_WORD) {
                    referrerTypes.addAll(Arrays.asList(tokenizer.sval.split("\\|")));
                }

                objectInstancesManager.deleteReferrers(className, referrersCount, referrerTypes);

                return true;
            } else if (command.startsWith(COMMAND_CREATE_ALL_TYPE_REFERENCES)) {
                tokenizer.nextToken();

                if (tokenizer.nextToken() != StreamTokenizer.TT_WORD)
                    throw new TestBug("Invalid command format: " + command);

                String className = tokenizer.sval;

                if (tokenizer.nextToken() != StreamTokenizer.TT_NUMBER)
                    throw new TestBug("Invalid command format: " + command);

                int instanceCounts = (int) tokenizer.nval;

                objectInstancesManager.createAllTypeReferences(className, instanceCounts);

                return true;
            }
        } catch (IOException e) {
            throw new TestBug("Invalid command format: " + command);
        }

        return false;
    }

    // check if jfr is initialized
    public static boolean isJFRActive() {
        try {
            Class cls = Class.forName("jdk.jfr.FlightRecorder");
            Method method = cls.getDeclaredMethod("isInitialized", new Class[0]);
            return (Boolean)method.invoke(cls, new Object[0]);
        } catch(Exception e) {
            return false;
        }
    }

    // is reference with given type should be included in ObjectReference.referringObjects
    static public boolean isIncludedIntoReferrersCount(String referenceType) {
        if (!ObjectInstancesManager.allReferenceTypes.contains(referenceType)) {
            throw new TestBug("Invalid reference type: " + referenceType);
        }

        return includedIntoReferrersCountTypes.contains(referenceType);
    }

    // is reference with given type should be included in ReferenceType.instances
    static public boolean isIncludedIntoInstancesCount(String referenceType) {
        if (!ObjectInstancesManager.allReferenceTypes.contains(referenceType)) {
            throw new TestBug("Invalid reference type: " + referenceType);
        }

        return includedIntoInstancesCountTypes.contains(referenceType);
    }

    public static void main(String args[]) {
        HeapwalkingDebuggee debuggee = new HeapwalkingDebuggee();
        debuggee.init(args);
        debuggee.doTest();
    }
}
