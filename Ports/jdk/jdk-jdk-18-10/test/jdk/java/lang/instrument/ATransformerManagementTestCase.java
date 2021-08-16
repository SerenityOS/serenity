/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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


import java.io.*;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.*;


/**
 * Simple tests for the TransformerManager
 *
 */
public abstract class
ATransformerManagementTestCase
    extends AInstrumentationTestCase
{
    private static final String redefinedClassName = "DummyClass";

    protected int   kModSamples = 2;
    public final    ClassFileTransformer[] kTransformerSamples = new ClassFileTransformer[]
        {
            new MyClassFileTransformer(         Integer.toString(0)),
            new MyClassFileTransformer( Integer.toString(1)),
            new MyClassFileTransformer(         Integer.toString(2)),
            new MyClassFileTransformer( Integer.toString(3)),
            new MyClassFileTransformer(         Integer.toString(4)),
            new MyClassFileTransformer( Integer.toString(5)),
            new MyClassFileTransformer(         Integer.toString(6)),
            new MyClassFileTransformer( Integer.toString(7)),
            new MyClassFileTransformer(         Integer.toString(8)),
            new MyClassFileTransformer( Integer.toString(9)),
            new MyClassFileTransformer(         Integer.toString(10)),
            new MyClassFileTransformer( Integer.toString(11)),
            new MyClassFileTransformer( Integer.toString(12)),
            new MyClassFileTransformer( Integer.toString(13)),
            new MyClassFileTransformer(         Integer.toString(14)),
            new MyClassFileTransformer( Integer.toString(15)),
            new MyClassFileTransformer(         Integer.toString(16)),
            new MyClassFileTransformer(         Integer.toString(17)),
            new MyClassFileTransformer( Integer.toString(18)),
        };

    private ArrayList           fTransformers;       // The list of transformers
    private int                 fTransformerIndex;   // The number of transformers encountered
    private String              fDelayedFailure;     // Set non-null if failed in transformer


    /**
     * Constructor for ATransformerManagementTestCase.
     */
    public ATransformerManagementTestCase(String name)
    {
        super(name);
    }


    /**
     * Returns one of the sample transformers
     * @return a random transformer
     */
    protected ClassFileTransformer
    getRandomTransformer()
    {
        int randIndex = (int)Math.floor(Math.random() * kTransformerSamples.length);
        verbosePrint("Choosing random transformer #" + randIndex);
        return kTransformerSamples[randIndex];
    }

    /**
     * Method addTransformerToManager.
     * @param manager
     * @param transformer
     */
    protected void
    addTransformerToManager(
        Instrumentation         manager,
        ClassFileTransformer    transformer)
    {
        addTransformerToManager(manager, transformer, false);
    }

    /**
     * Method addTransformerToManager.
     * @param manager
     * @param transformer
     * @param canRetransform
     */
    protected void
    addTransformerToManager(
        Instrumentation         manager,
        ClassFileTransformer    transformer,
        boolean                 canRetransform)
    {
        if (transformer != null)
        {
            fTransformers.add(transformer);
        }
        // workaroud for JDK-8264667: create log message before addTransformer()
        String msg = "Added transformer " + transformer
            + " with canRetransform=" + canRetransform;
        manager.addTransformer(transformer, canRetransform);
        verbosePrint(msg);
    }

    /**
     * Remove transformer from manager and list
     * @param manager
     * @param transformer
     */
    protected void
    removeTransformerFromManager(
        Instrumentation manager,
        ClassFileTransformer transformer)
    {
        assertTrue("Transformer not found in manager ("+transformer+")", manager.removeTransformer(transformer));

        if (transformer != null)
        {
            fTransformers.remove(transformer);
        }
        verbosePrint("Removed transformer " + transformer);
    }

    /**
     * Decrements the transformer index as well as removes transformer
     * @param fInst         manager
     * @param transformer       transformer to remove
     * @param decrementIndex    the tranformer index gets out of sync with transformers
     *                          that are removed from the manager
     */
    protected void
    removeTransformerFromManager(   Instrumentation manager,
                                    ClassFileTransformer transformer,
                                    boolean decrementIndex)
    {
        removeTransformerFromManager(manager, transformer);
        if (decrementIndex)
        {
            fTransformerIndex--;
            verbosePrint("removeTransformerFromManager fTransformerIndex decremented to: " +
                         fTransformerIndex);
        }
    }

    /**
     * verify transformer by asserting that the number of transforms that occured
     * is the same as the number of valid transformers added to the list.
     * @param manager
     */
    protected void
    verifyTransformers(Instrumentation manager)
    {
        File f = new File(System.getProperty("test.classes", "."), redefinedClassName + ".class");
        System.out.println("Reading test class from " + f);
        try
        {
            InputStream redefineStream = new FileInputStream(f);
            byte[] bytes = NamedBuffer.loadBufferFromStream(redefineStream);
            ClassDefinition cd = new ClassDefinition(DummyClass.class, bytes);
            fInst.redefineClasses(new ClassDefinition[]{ cd });
            verbosePrint("verifyTransformers redefined " + redefinedClassName);
        }
        catch (IOException e)
        {
            fail("Could not load the class: " + redefinedClassName);
        }
        catch (ClassNotFoundException e)
        {
            fail("Could not find the class: " + redefinedClassName);
        }
        catch (UnmodifiableClassException e)
        {
            fail("Could not modify the class: " + redefinedClassName);
        }

        // Report any delayed failures
        assertTrue(fDelayedFailure, fDelayedFailure == null);

        assertEquals("The number of transformers that were run does not match the expected number added to manager",
                        fTransformers.size(), fTransformerIndex);
    }


    /**
     * Asserts that the transformer being checked by the manager is the correct
     * one (as far as order goes) and updates the number of transformers that have
     * been called.  Note that since this is being called inside of a transformer,
     * a standard assert (which throws an exception) cannot be used since it would
     * simply cancel the transformation and otherwise be ignored.  Instead, note
     * the failure for delayed processing.
     * @param ClassFileTransformer
     */
    private void
    checkInTransformer(ClassFileTransformer transformer)
    {
        verbosePrint("checkInTransformer: " + transformer);
        if (fDelayedFailure == null)
        {
            if (fTransformers.size() <= fTransformerIndex)
            {
                String msg = "The number of transformers that have checked in (" +(fTransformerIndex+1) +
                    ") is greater number of tranformers created ("+fTransformers.size()+")";
                fDelayedFailure = msg;
                System.err.println("Delayed failure: " + msg);
                verbosePrint("Delayed failure: " + msg);
            }
            if (!fTransformers.get(fTransformerIndex).equals(transformer))
            {
                String msg = "Transformer " + fTransformers.get(fTransformerIndex) +
                    " should be the same as " + transformer;
                fDelayedFailure = msg;
                System.err.println("Delayed failure: " + msg);
                verbosePrint("Delayed failure: " + msg);
            }
            fTransformerIndex++;
            verbosePrint("fTransformerIndex incremented to: " + fTransformerIndex);
        }
    }

    /**
     * Create a new manager, a new transformer list, and initializes the number of transformers
     * indexed to zero.
     */
    protected void
    setUp()
        throws Exception
    {
        super.setUp();
        fTransformers = new ArrayList();
        fTransformerIndex = 0;
        fDelayedFailure = null;
        verbosePrint("setUp completed");
    }

    /**
     * Sets the manager and transformers to null so that setUp needs to update.
     */
    protected void
    tearDown()
        throws Exception
    {
        verbosePrint("tearDown beginning");
        fTransformers = null;
        super.tearDown();
    }

    /*
     *  Simple transformer that registers when it transforms
     */
    public class MyClassFileTransformer extends SimpleIdentityTransformer {
        private final String fID;

        public MyClassFileTransformer(String id) {
            super();
            fID = id;
        }

        public String toString() {
            return MyClassFileTransformer.this.getClass().getName() + fID;
        }

        public byte[]
        transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain    protectionDomain,
            byte[] classfileBuffer) {

            // The transform testing is triggered by redefine, ignore others
            if (classBeingRedefined != null) checkInTransformer(MyClassFileTransformer.this);

            return super.transform(     loader,
                                        className,
                                        classBeingRedefined,
                                        protectionDomain,
                                        classfileBuffer);
        }

        public byte[]
        transform(
            Module module,
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain    protectionDomain,
            byte[] classfileBuffer) {

            // The transform testing is triggered by redefine, ignore others
            if (classBeingRedefined != null) checkInTransformer(MyClassFileTransformer.this);

            return super.transform(     module,
                                        loader,
                                        className,
                                        classBeingRedefined,
                                        protectionDomain,
                                        classfileBuffer);
        }
    }


    /**
     * Class loader that does nothing
     */
    public class MyClassLoader extends ClassLoader
    {
        /**
         * Constructor for MyClassLoader.
         */
        public MyClassLoader()
        {
            super();
        }

    }

    public String debug_byteArrayToString(byte[] b) {
        if (b == null) return "null";

        StringBuffer buf = new StringBuffer();
        buf.append("byte[");
        buf.append(b.length);
        buf.append("] (");
        for (int i = 0; i < b.length; i++)
        {
            buf.append(b[i]);
            buf.append(",");
        }
        buf.deleteCharAt(buf.length()-1);
        buf.append(")");

        return buf.toString();
    }
}
