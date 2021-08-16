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

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/*
 * This class create/delete reference with given type.
 *
 * Supported reference types are:
 * - strong
 * - soft
 * - weak
 * - phantom
 * - jni local
 * - jni global
 * - jni weak
 */
public class ReferringObject
{
        static
        {
                System.loadLibrary("JNIreferences");
        }

        public final static int maxJNIGlobalReferences = 1000;
        public final static int maxJNIWeakReferences = 1000;

        private Object reference;

        private String referenceType;

        //used for storing jni global and jni weak references
        private int referenceIndex;

        public ReferringObject(Object object, String referenceType)
        {
                this.referenceType = referenceType;

                if(referenceType.equals(ObjectInstancesManager.STRONG_REFERENCE))
                {
                        createStrongReference(object);
                }
                else
                if(referenceType.equals(ObjectInstancesManager.SOFT_REFERENCE))
                {
                        createSoftReference(object);
                }
                else
                if(referenceType.equals(ObjectInstancesManager.WEAK_REFERENCE))
                {
                        createWeakReference(object);
                }
                else
                if(referenceType.equals(ObjectInstancesManager.PHANTOM_REFERENCE))
                {
                        createPhantomReference(object);
                }
                else
                if(referenceType.equals(ObjectInstancesManager.JNI_GLOBAL_REFERENCE))
                {
                        createJNIGlobalReference(object);
                }
                else
                if(referenceType.equals(ObjectInstancesManager.JNI_LOCAL_REFERENCE))
                {
                        createJNILocalReference(object);
                }
                else
                if(referenceType.equals(ObjectInstancesManager.JNI_WEAK_REFERENCE))
                {
                        createJNIWeakReference(object);
                }
                else
                        throw new IllegalArgumentException("Invalid reference type: " + referenceType);
        }

        public void delete()
        {
                if(referenceType == null)
                {
                        throw new TestBug("Reference type is null");
                }

                if(referenceType.equals(ObjectInstancesManager.SOFT_REFERENCE))
                {
                        if(reference == null)
                        {
                                throw new TestBug("Reference is null for SoftReference");
                        }

                        if(((SoftReference)reference).get() == null)
                        {
                     //           throw new TestBug("Test execution error: SoftReference was collected");
                        }
                }
                else
                if(referenceType.equals(ObjectInstancesManager.WEAK_REFERENCE))
                {
                        if(reference == null)
                        {
                                throw new TestBug("Reference is null for WeakReference");
                        }

                        if(((WeakReference)reference).get() == null)
                        {
                       //         throw new TestBug("Test execution error: WeakReference was collected");
                        }
                }
                else
                if(referenceType.equals(ObjectInstancesManager.PHANTOM_REFERENCE))
                {
                        if(reference == null)
                        {
                                throw new TestBug("Reference is null for PhantomReference");
                        }
                }
                else
                if(referenceType.equals(ObjectInstancesManager.JNI_GLOBAL_REFERENCE))
                {
                        deleteJNIGlobalReferenceNative(referenceIndex);
                }
                else
                if(referenceType.equals(ObjectInstancesManager.JNI_LOCAL_REFERENCE))
                {
                        deleteJNILocalReference();
                }
                else
                if(referenceType.equals(ObjectInstancesManager.JNI_WEAK_REFERENCE))
                {
                    try {
                        deleteJNIWeakReferenceNative(referenceIndex);
                    } catch (Throwable t)
                    {

                    }
                }

                reference = null;
        }

        private void createStrongReference(Object object)
        {
                reference = object;
        }

        private void createSoftReference(Object object)
        {
                reference = new SoftReference<Object>(object);
        }

        private void createWeakReference(Object object)
        {
                reference = new WeakReference<Object>(object);
        }

        private void createPhantomReference(Object object)
        {
                reference = new PhantomReference<Object>(object, new ReferenceQueue<Object>());
        }

        private void createJNIGlobalReference(Object object)
        {
                referenceIndex = createJNIGlobalReferenceNative(object, maxJNIGlobalReferences);

                if(referenceIndex < 0)
                {
                        throw new TestBug("Error on creation of JNI_Global reference, Possible number of JNI_Global references exceeded max available value!");
                }
        }

        /*
         * Since jni local reference valid only for duration of native method call, to create jni local reference
         * special thread is created which enter in native method, create jni local reference and wait
         */
        private void createJNILocalReference(Object object)
        {
                this.reference = object;

                jniLocalReferenceThread = new JNILocalReferenceThread();
                jniLocalReferenceThread.start();

                // wait till JNI local reference will be created
                jniLocalReferenceThread.createWhicket.waitFor();

                reference = null;
        }

        private void deleteJNILocalReference()
        {
                // notify JNI method that JNI local reference is not needed any more and could be released
                jniLocalReferenceThread.deleteWhicket.unlock();

                try
                {
                        jniLocalReferenceThread.join(1000 * 60 * 2);

                        if(jniLocalReferenceThread.isAlive())
                        {
                                throw new TestBug("JNI_Local_Reference thread can't finish execution");
                        }
                }
                catch(InterruptedException e)
                {
                        throw new TestBug("deleteJNILocalReference was interrupted");
                }
        }

        private void createJNIWeakReference(Object object)
        {
                referenceIndex = createJNIWeakReferenceNative(object, maxJNIWeakReferences);

                if(referenceIndex < 0)
                {
                        throw new TestBug("Error on creation of JNI_Weak reference. Possible number of JNI_Weak references exceeded max available value!");
                }
        }

        class JNILocalReferenceThread
                extends Thread
        {
                Wicket createWhicket = new Wicket();
                Wicket deleteWhicket = new Wicket();

                public void run()
                {
                        createJNILocalReferenceNative(reference, createWhicket, deleteWhicket);
                }
        }

        private JNILocalReferenceThread jniLocalReferenceThread;

        private native int createJNIGlobalReferenceNative(Object object, int maxJNIGlobalReferences);

        private native void deleteJNIGlobalReferenceNative(int index);

        private native void createJNILocalReferenceNative(Object object, Wicket createWhicket, Wicket deleteWhicket);

        private native int createJNIWeakReferenceNative(Object object, int maxJNIWeakReferences);

        private native void deleteJNIWeakReferenceNative(int index);
}
