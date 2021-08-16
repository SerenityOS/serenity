/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share;

import java.util.*;

/*
 *  Class create/delete instances with given reference type and given referrers number
 */
public class ObjectInstancesManager
{
        public static String STRONG_REFERENCE = "STRONG";
        public static String WEAK_REFERENCE = "WEAK";
        public static String SOFT_REFERENCE = "SOFT";
        public static String PHANTOM_REFERENCE = "PHANTOM";
        public static String JNI_GLOBAL_REFERENCE = "JNI_GLOBAL";
        public static String JNI_LOCAL_REFERENCE = "JNI_LOCAL";
        public static String JNI_WEAK_REFERENCE = "JNI_WEAK";

        // used to create references of all types
        private static String USE_ALL_REFERENCE_TYPES = "ALL_REFERENCE_TYPES";

        private Map<String, Collection<ReferringObjectSet>> instances = new TreeMap<String, Collection<ReferringObjectSet>>();

        public static Set<String> primitiveArrayClassNames = new HashSet<String>();

        static
        {
                primitiveArrayClassNames.add("boolean[]");
                primitiveArrayClassNames.add("byte[]");
                primitiveArrayClassNames.add("char[]");
                primitiveArrayClassNames.add("int[]");
                primitiveArrayClassNames.add("long[]");
                primitiveArrayClassNames.add("float[]");
                primitiveArrayClassNames.add("double[]");
        }


        public static Set<String> allReferenceTypes = new HashSet<String>();

        static
        {
                allReferenceTypes.add(ObjectInstancesManager.STRONG_REFERENCE);
                allReferenceTypes.add(ObjectInstancesManager.WEAK_REFERENCE);
                allReferenceTypes.add(ObjectInstancesManager.SOFT_REFERENCE);
                allReferenceTypes.add(ObjectInstancesManager.PHANTOM_REFERENCE);
                allReferenceTypes.add(ObjectInstancesManager.JNI_GLOBAL_REFERENCE);
                allReferenceTypes.add(ObjectInstancesManager.JNI_LOCAL_REFERENCE);
                allReferenceTypes.add(ObjectInstancesManager.JNI_WEAK_REFERENCE);
        }

        public static boolean isWeak(String type) {
            return !(type.equals(ObjectInstancesManager.JNI_GLOBAL_REFERENCE)
                    || type.equals(ObjectInstancesManager.JNI_LOCAL_REFERENCE)
                    || type.equals(ObjectInstancesManager.STRONG_REFERENCE));

        }

        public static Log log;

        public ObjectInstancesManager(Log log)
        {
                ObjectInstancesManager.log = log;
        }

        // delete a given number of referrers
        public void deleteReferrers(String className, int referrersCount, Set<String> referrerTypes)
        {
                Collection<ReferringObjectSet> objectInstances;

                objectInstances = instances.get(className);

                if(objectInstances == null)
                {
                        log.display("Error command 'deleteObjectInstances' is requsted: instances of class " + className + " was not created");
                        System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
                        return;
                }

                Iterator<ReferringObjectSet> iterator = objectInstances.iterator();

                while(iterator.hasNext())
                {
                        ReferringObjectSet debugeeObjectReference = iterator.next();
                        if (referrerTypes.isEmpty() || referrerTypes.contains(debugeeObjectReference.getReferenceType())) {
                                debugeeObjectReference.delete(referrersCount);

                                if(debugeeObjectReference.getReferrerCount() == 0)
                                        iterator.remove();
                        }
                }
        }

        // delete all object referrers, it is equal to make object unreacheable
        public void deleteAllReferrers(int count, String className)
        {
                Collection<ReferringObjectSet> objectInstances;

                objectInstances = instances.get(className);

                if(objectInstances == null)
                {
                        throw new TestBug("Command 'deleteObjectInstances' is requsted: instances of class " + className + " was not created");
                }

                Iterator<ReferringObjectSet> iterator = objectInstances.iterator();

                if(count == 0)
                        count = objectInstances.size();

                for(int i = 0; i < count; i++)
                {
                        ReferringObjectSet debugeeObjectReference = iterator.next();
                        debugeeObjectReference.deleteAll();

                        iterator.remove();
                }
        }

        // create object instance with referrers of all possible types
        public void createAllTypeReferences(String className, int count)
        {
                createReferences(count, className, 1, allReferenceTypes);
        }

        // create a given number of object instances with given number of referrers
        public void createReferences(int count, String className, int referrerCount, Set<String> referrerTypes)
        {
                Collection<ReferringObjectSet> objectInstances;

                Class klass = null;

                if(!primitiveArrayClassNames.contains(className))
                {
                        try
                        {
                                klass = Class.forName(className);
                        }
                        catch(ClassNotFoundException e)
                        {
                                log.display("Can't find class: " + className);
                                System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
                                return;
                        }
                }

                objectInstances = instances.get(className);

                if(objectInstances == null)
                {
                        objectInstances = new ArrayList<ReferringObjectSet>();
                        instances.put(className, objectInstances);
                }

                for(int i = 0; i < count; i++)
                {
                        try
                        {
                                Object instance;

                                if(!primitiveArrayClassNames.contains(className))
                                {
                                        instance = klass.newInstance();
                                }
                                else
                                {
                                        instance = createPrimitiveTypeArray(className);
                                }

                                for(String type : referrerTypes) {
                                        objectInstances.add(new ReferringObjectSet(instance, referrerCount, type));
                                }
                        }
                        catch(Exception e)
                        {
                                log.display("Unexpected exception: " + e);
                                System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
                        }
                }
        }

        public Object createPrimitiveTypeArray(String typeName)
        {
                int arraySize = 1;

                if(typeName.equals("boolean[]"))
                        return new boolean[arraySize];
                else
                if(typeName.equals("byte[]"))
                        return new byte[arraySize];
                else
                if(typeName.equals("char[]"))
                        return new char[arraySize];
                else
                if(typeName.equals("int[]"))
                        return new int[arraySize];
                else
                if(typeName.equals("long[]"))
                        return new long[arraySize];
                else
                if(typeName.equals("float[]"))
                        return new float[arraySize];
                else
                if(typeName.equals("double[]"))
                        return new double[arraySize];
                else
                {
                        throw new TestBug("Invalid primitive type array type name: " + typeName);
                }
        }

}
