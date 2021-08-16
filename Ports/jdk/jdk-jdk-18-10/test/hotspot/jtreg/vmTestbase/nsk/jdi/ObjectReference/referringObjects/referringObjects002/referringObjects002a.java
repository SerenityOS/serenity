/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jdi.ObjectReference.referringObjects.referringObjects002;

import nsk.share.ClassUnloader;
import nsk.share.ObjectInstancesManager;
import nsk.share.ReferringObject;
import nsk.share.TestBug;
import nsk.share.jdi.HeapwalkingDebuggee;
import java.io.*;
import java.util.*;

/*
 * Class create and save given number of class instances
 */
class Instances {
    Instances(Class<?> klass, int instanceCount) {
        try {
            for (int i = 0; i < instanceCount; i++)
                instances.add(klass.newInstance());
        } catch (Throwable t) {
            throw new TestBug("Unexpected error during test class instantiation: " + t);
        }
    }

    private List<Object> instances = new ArrayList<Object>();
}

/*
 * Debuggee class handle request for loading classes with custom classloader and creating class instances
 */
public class referringObjects002a extends HeapwalkingDebuggee {
    private Map<String, Instances> classesInstances = new HashMap<String, Instances>();

    private List<ReferringObject> classReferrers = new ArrayList<ReferringObject>();

    final static public String COMMAND_DELETE_CLASS_OBJECT_REFERRERS = "deleteClassObjectReferrers";

    public static void main(String args[]) {
        new referringObjects002a().doTest(args);
    }

    // create references of all possible types to class object
    public void createClassObjectReferrers(String className, int instanceCount) {
        ClassUnloader unloader = loadedClasses.get(className);

        if (unloader == null)
            throw new TestBug("Unloader is null for class: " + className);

        Class<?> klass = unloader.getLoadedClass();

        if (klass == null)
            throw new TestBug("Unloader return null class object, for classname: " + className);

        classesInstances.put(className, new Instances(klass, instanceCount));

        for (String referenceType : ObjectInstancesManager.allReferenceTypes)
            classReferrers.add(new ReferringObject(klass, referenceType));
    }

    // delete all created references to class object
    public void deleteClassObjectReferrers() {
        classesInstances = null;

        for (ReferringObject referrer : classReferrers)
            referrer.delete();

        classReferrers.clear();
    }

    public boolean parseCommand(String command) {
        if (super.parseCommand(command)) {
            // need do some additional actions for COMMAND_LOAD_CLASS
            if (!command.startsWith(COMMAND_LOAD_CLASS))
                return true;
        }

        try {
            StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(command));
            tokenizer.whitespaceChars(':', ':');

            if (command.startsWith(COMMAND_LOAD_CLASS)) {
                tokenizer.nextToken();

                if (tokenizer.nextToken() != StreamTokenizer.TT_WORD)
                    throw new IllegalArgumentException("Invalid command format");

                String className = tokenizer.sval;

                if (tokenizer.nextToken() != StreamTokenizer.TT_NUMBER)
                    throw new IllegalArgumentException("Invalid command format");

                int instanceCount = (int) tokenizer.nval;

                createClassObjectReferrers(className, instanceCount);

                return true;
            } else if (command.equals(COMMAND_DELETE_CLASS_OBJECT_REFERRERS)) {
                deleteClassObjectReferrers();

                return true;
            }
        } catch (IOException e) {
            throw new TestBug("Invalid command format: " + command);
        }

        return false;
    }

}
