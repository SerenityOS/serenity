/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.util;

import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;

/**
 * Stores grammars in a pool associated to a specific key. This grammar pool
 * implementation stores two types of grammars: those keyed by the root element
 * name, and those keyed by the grammar's target namespace.
 *
 * This is the default implementation of the GrammarPool interface.
 * As we move forward, this will become more function-rich and robust.
 *
 * @author Jeffrey Rodriguez, IBM
 * @author Andy Clark, IBM
 * @author Neil Graham, IBM
 * @author Pavani Mukthipudi, Sun Microsystems
 * @author Neeraj Bajaj, SUN Microsystems
 *
 */
public class XMLGrammarPoolImpl implements XMLGrammarPool {

    //
    // Constants
    //

    /** Default size. */
    protected static final int TABLE_SIZE = 11;

    //
    // Data
    //

    /** Grammars. */
    protected Entry[] fGrammars = null;

    // whether this pool is locked
    protected boolean fPoolIsLocked;

    // the number of grammars in the pool
    protected int fGrammarCount = 0;

    private static final boolean DEBUG = false ;

    //
    // Constructors
    //

    /** Constructs a grammar pool with a default number of buckets. */
    public XMLGrammarPoolImpl() {
        fGrammars = new Entry[TABLE_SIZE];
        fPoolIsLocked = false;
    } // <init>()

    /** Constructs a grammar pool with a specified number of buckets. */
    public XMLGrammarPoolImpl(int initialCapacity) {
        fGrammars = new Entry[initialCapacity];
        fPoolIsLocked = false;
    }

    //
    // XMLGrammarPool methods
    //

    /* <p> Retrieve the initial known set of grammars. This method is
     * called by a validator before the validation starts. The application
     * can provide an initial set of grammars available to the current
     * validation attempt. </p>
     *
     * @param grammarType The type of the grammar, from the
     *                    <code>com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription</code>
     *                    interface.
     * @return            The set of grammars the validator may put in its "bucket"
     */
    public Grammar [] retrieveInitialGrammarSet (String grammarType) {
        synchronized (fGrammars) {
            int grammarSize = fGrammars.length ;
            Grammar [] tempGrammars = new Grammar[fGrammarCount];
            int pos = 0;
            for (int i = 0; i < grammarSize; i++) {
                for (Entry e = fGrammars[i]; e != null; e = e.next) {
                    if (e.desc.getGrammarType().equals(grammarType)) {
                        tempGrammars[pos++] = e.grammar;
                    }
                }
            }
            Grammar[] toReturn = new Grammar[pos];
            System.arraycopy(tempGrammars, 0, toReturn, 0, pos);
            return toReturn;
        }
    } // retrieveInitialGrammarSet (String): Grammar[]

    /* <p> Return the final set of grammars that the validator ended up
     * with. This method is called after the validation finishes. The
     * application may then choose to cache some of the returned grammars.</p>
     * <p>In this implementation, we make our choice based on whether this object
     * is "locked"--that is, whether the application has instructed
     * us not to accept any new grammars.</p>
     *
     * @param grammarType The type of the grammars being returned;
     * @param grammars    An array containing the set of grammars being
     *                    returned; order is not significant.
     */
    public void cacheGrammars(String grammarType, Grammar[] grammars) {
        if(!fPoolIsLocked) {
            for (int i = 0; i < grammars.length; i++) {
                if(DEBUG) {
                    System.out.println("CACHED GRAMMAR " + (i+1) ) ;
                    Grammar temp = grammars[i] ;
                    //print(temp.getGrammarDescription());
                }
                putGrammar(grammars[i]);
            }
        }
    } // cacheGrammars(String, Grammar[]);

    /* <p> This method requests that the application retrieve a grammar
     * corresponding to the given GrammarIdentifier from its cache.
     * If it cannot do so it must return null; the parser will then
     * call the EntityResolver. </p>
     * <strong>An application must not call its EntityResolver itself
     * from this method; this may result in infinite recursions.</strong>
     *
     * This implementation chooses to use the root element name to identify a DTD grammar
     * and the target namespace to identify a Schema grammar.
     *
     * @param desc The description of the Grammar being requested.
     * @return     The Grammar corresponding to this description or null if
     *             no such Grammar is known.
     */
    public Grammar retrieveGrammar(XMLGrammarDescription desc) {
        if(DEBUG){
            System.out.println("RETRIEVING GRAMMAR FROM THE APPLICATION WITH FOLLOWING DESCRIPTION :");
            //print(desc);
        }
        return getGrammar(desc);
    } // retrieveGrammar(XMLGrammarDescription):  Grammar

    //
    // Public methods
    //

    /**
     * Puts the specified grammar into the grammar pool and associates it to
     * its root element name or its target namespace.
     *
     * @param grammar The Grammar.
     */
    public void putGrammar(Grammar grammar) {
        if(!fPoolIsLocked) {
            synchronized (fGrammars) {
                XMLGrammarDescription desc = grammar.getGrammarDescription();
                int hash = hashCode(desc);
                int index = (hash & 0x7FFFFFFF) % fGrammars.length;
                for (Entry entry = fGrammars[index]; entry != null; entry = entry.next) {
                    if (entry.hash == hash && equals(entry.desc, desc)) {
                        entry.grammar = grammar;
                        return;
                    }
                }
                // create a new entry
                Entry entry = new Entry(hash, desc, grammar, fGrammars[index]);
                fGrammars[index] = entry;
                fGrammarCount++;
            }
        }
    } // putGrammar(Grammar)

    /**
     * Returns the grammar associated to the specified grammar description.
     * Currently, the root element name is used as the key for DTD grammars
     * and the target namespace  is used as the key for Schema grammars.
     *
     * @param desc The Grammar Description.
     */
    public Grammar getGrammar(XMLGrammarDescription desc) {
        synchronized (fGrammars) {
            int hash = hashCode(desc);
        int index = (hash & 0x7FFFFFFF) % fGrammars.length;
        for (Entry entry = fGrammars[index] ; entry != null ; entry = entry.next) {
            if ((entry.hash == hash) && equals(entry.desc, desc)) {
                return entry.grammar;
            }
        }
        return null;
    }
    } // getGrammar(XMLGrammarDescription):Grammar

    /**
     * Removes the grammar associated to the specified grammar description from the
     * grammar pool and returns the removed grammar. Currently, the root element name
     * is used as the key for DTD grammars and the target namespace  is used
     * as the key for Schema grammars.
     *
     * @param desc The Grammar Description.
     * @return     The removed grammar.
     */
    public Grammar removeGrammar(XMLGrammarDescription desc) {
        synchronized (fGrammars) {
            int hash = hashCode(desc);
        int index = (hash & 0x7FFFFFFF) % fGrammars.length;
        for (Entry entry = fGrammars[index], prev = null ; entry != null ; prev = entry, entry = entry.next) {
            if ((entry.hash == hash) && equals(entry.desc, desc)) {
                if (prev != null) {
                        prev.next = entry.next;
            }
            else {
                fGrammars[index] = entry.next;
            }
                Grammar tempGrammar = entry.grammar;
                entry.grammar = null;
                fGrammarCount--;
                return tempGrammar;
            }
        }
        return null;
        }
    } // removeGrammar(XMLGrammarDescription):Grammar

    /**
     * Returns true if the grammar pool contains a grammar associated
     * to the specified grammar description. Currently, the root element name
     * is used as the key for DTD grammars and the target namespace  is used
     * as the key for Schema grammars.
     *
     * @param desc The Grammar Description.
     */
    public boolean containsGrammar(XMLGrammarDescription desc) {
        synchronized (fGrammars) {
            int hash = hashCode(desc);
        int index = (hash & 0x7FFFFFFF) % fGrammars.length;
        for (Entry entry = fGrammars[index] ; entry != null ; entry = entry.next) {
            if ((entry.hash == hash) && equals(entry.desc, desc)) {
                return true;
            }
        }
        return false;
    }
    } // containsGrammar(XMLGrammarDescription):boolean

    /* <p> Sets this grammar pool to a "locked" state--i.e.,
     * no new grammars will be added until it is "unlocked".
     */
    public void lockPool() {
        fPoolIsLocked = true;
    } // lockPool()

    /* <p> Sets this grammar pool to an "unlocked" state--i.e.,
     * new grammars will be added when putGrammar or cacheGrammars
     * are called.
     */
    public void unlockPool() {
        fPoolIsLocked = false;
    } // unlockPool()

    /*
     * <p>This method clears the pool-i.e., removes references
     * to all the grammars in it.</p>
     */
    public void clear() {
        for (int i=0; i<fGrammars.length; i++) {
            if(fGrammars[i] != null) {
                fGrammars[i].clear();
                fGrammars[i] = null;
            }
        }
        fGrammarCount = 0;
    } // clear()

    /**
     * This method checks whether two grammars are the same. Currently, we compare
     * the root element names for DTD grammars and the target namespaces for Schema grammars.
     * The application can override this behaviour and add its own logic.
     *
     * @param desc1 The grammar description
     * @param desc2 The grammar description of the grammar to be compared to
     * @return      True if the grammars are equal, otherwise false
     */
    public boolean equals(XMLGrammarDescription desc1, XMLGrammarDescription desc2) {
        return desc1.equals(desc2);
    }

    /**
     * Returns the hash code value for the given grammar description.
     *
     * @param desc The grammar description
     * @return     The hash code value
     */
    public int hashCode(XMLGrammarDescription desc) {
        return desc.hashCode();
    }

    /**
     * This class is a grammar pool entry. Each entry acts as a node
     * in a linked list.
     */
    protected static final class Entry {
        public int hash;
        public XMLGrammarDescription desc;
        public Grammar grammar;
        public Entry next;

        protected Entry(int hash, XMLGrammarDescription desc, Grammar grammar, Entry next) {
            this.hash = hash;
            this.desc = desc;
            this.grammar = grammar;
            this.next = next;
        }

        // clear this entry; useful to promote garbage collection
        // since reduces reference count of objects to be destroyed
        protected void clear () {
            desc = null;
            grammar = null;
            if(next != null) {
                next.clear();
                next = null;
            }
        } // clear()
    } // class Entry

    /* For DTD build we can't import here XSDDescription. Thus, this method is commented out.. */
    /* public void print(XMLGrammarDescription description){
        if(description.getGrammarType().equals(XMLGrammarDescription.XML_DTD)){

        }
        else if(description.getGrammarType().equals(XMLGrammarDescription.XML_SCHEMA)){
            XSDDescription schema = (XSDDescription)description ;
            System.out.println("Context = " + schema.getContextType());
            System.out.println("TargetNamespace = " + schema.getTargetNamespace());
            String [] temp = schema.getLocationHints();

            for (int i = 0 ; (temp != null && i < temp.length) ; i++){
                System.out.println("LocationHint " + i + " = "+ temp[i]);
            }

            System.out.println("Triggering Component = " + schema.getTriggeringComponent());
            System.out.println("EnclosingElementName =" + schema.getEnclosingElementName());

        }

    }//print
    */

} // class XMLGrammarPoolImpl
