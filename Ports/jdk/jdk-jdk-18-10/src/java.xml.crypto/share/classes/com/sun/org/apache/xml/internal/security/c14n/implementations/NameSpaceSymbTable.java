/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sun.org.apache.xml.internal.security.c14n.implementations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


import org.w3c.dom.Attr;
import org.w3c.dom.Node;

/**
 * A stack based Symbol Table.
 *<br>For speed reasons all the symbols are introduced in the same map,
 * and at the same time in a list so it can be removed when the frame is pop back.
 */
public class NameSpaceSymbTable {

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(NameSpaceSymbTable.class);

    private static final String XMLNS = "xmlns";
    private static final SymbMap initialMap = new SymbMap();

    static {
        NameSpaceSymbEntry ne = new NameSpaceSymbEntry("", null, true, XMLNS);
        ne.lastrendered = "";
        initialMap.put(XMLNS, ne);
    }

    /**The map betwen prefix-> entry table. */
    private SymbMap symb;

    /**The stacks for removing the definitions when doing pop.*/
    private final List<SymbMap> level = new ArrayList<>();
    private boolean cloned = true;

    /**
     * Default constractor
     **/
    public NameSpaceSymbTable() {
        //Insert the default binding for xmlns.
        try {
            symb = initialMap.clone();
        } catch (CloneNotSupportedException e) {
            LOG.error("Error cloning the initial map");
        }
    }

    /**
     * Get all the unrendered nodes in the name space.
     * For Inclusive rendering
     * @param result the list where to fill the unrendered xmlns definitions.
     **/
    public void getUnrenderedNodes(Collection<Attr> result) {
        Iterator<NameSpaceSymbEntry> it = symb.entrySet().iterator();
        while (it.hasNext()) {
            NameSpaceSymbEntry n = it.next();
            //put them rendered?
            if (!n.rendered && n.n != null) {
                n = n.clone();
                needsClone();
                symb.put(n.prefix, n);
                n.lastrendered = n.uri;
                n.rendered = true;

                result.add(n.n);
            }
        }
    }

    /**
     * Push a frame for visible namespace.
     * For Inclusive rendering.
     **/
    public void outputNodePush() {
        push();
    }

    /**
     * Pop a frame for visible namespace.
     **/
    public void outputNodePop() {
        pop();
    }

    /**
     * Push a frame for a node.
     * Inclusive or Exclusive.
     **/
    public void push() {
        //Put the number of namespace definitions in the stack.
        level.add(null);
        cloned = false;
    }

    /**
     * Pop a frame.
     * Inclusive or Exclusive.
     **/
    public void pop() {
        int size = level.size() - 1;
        Object ob = level.remove(size);
        if (ob != null) {
            symb = (SymbMap)ob;
            if (size == 0) {
                cloned = false;
            } else {
                cloned = level.get(size - 1) != symb;
            }
        } else {
            cloned = false;
        }
    }

    final void needsClone() {
        if (!cloned) {
            level.set(level.size() - 1, symb);
            try {
                symb = symb.clone();
            } catch (CloneNotSupportedException e) {
                LOG.error("Error cloning the symbol map");
            }
            cloned = true;
        }
    }


    /**
     * Gets the attribute node that defines the binding for the prefix.
     * @param prefix the prefix to obtain the attribute.
     * @return null if there is no need to render the prefix. Otherwise the node of
     * definition.
     **/
    public Attr getMapping(String prefix) {
        NameSpaceSymbEntry entry = symb.get(prefix);
        if (entry == null) {
            //There is no definition for the prefix(a bug?).
            return null;
        }
        if (entry.rendered) {
            //No need to render an entry already rendered.
            return null;
        }
        // Mark this entry as render.
        entry = entry.clone();
        needsClone();
        symb.put(prefix, entry);
        entry.rendered = true;
        entry.lastrendered = entry.uri;
        // Return the node for outputing.
        return entry.n;
    }

    /**
     * Gets a definition without mark it as render.
     * For render in exclusive c14n the namespaces in the include prefixes.
     * @param prefix The prefix whose definition is neaded.
     * @return the attr to render, null if there is no need to render
     **/
    public Attr getMappingWithoutRendered(String prefix) {
        NameSpaceSymbEntry entry = symb.get(prefix);
        if (entry == null) {
            return null;
        }
        if (entry.rendered) {
            return null;
        }
        return entry.n;
    }

    /**
     * Adds the mapping for a prefix.
     * @param prefix the prefix of definition
     * @param uri the Uri of the definition
     * @param n the attribute that have the definition
     * @return true if there is already defined.
     **/
    public boolean addMapping(String prefix, String uri, Attr n) {
        NameSpaceSymbEntry ob = symb.get(prefix);
        if (ob != null && uri.equals(ob.uri)) {
            //If we have it previously defined. Don't keep working.
            return false;
        }
        //Creates and entry in the table for this new definition.
        NameSpaceSymbEntry ne = new NameSpaceSymbEntry(uri, n, false, prefix);
        needsClone();
        symb.put(prefix, ne);
        if (ob != null) {
            //We have a previous definition store it for the pop.
            //Check if a previous definition(not the inmidiatly one) has been rendered.
            ne.lastrendered = ob.lastrendered;
            if (ob.lastrendered != null && ob.lastrendered.equals(uri)) {
                //Yes it is. Mark as rendered.
                ne.rendered = true;
            }
        }
        return true;
    }

    /**
     * Adds a definition and mark it as render.
     * For inclusive c14n.
     * @param prefix the prefix of definition
     * @param uri the Uri of the definition
     * @param n the attribute that have the definition
     * @return the attr to render, null if there is no need to render
     **/
    public Node addMappingAndRender(String prefix, String uri, Attr n) {
        NameSpaceSymbEntry ob = symb.get(prefix);

        if (ob != null && uri.equals(ob.uri)) {
            if (!ob.rendered) {
                ob = ob.clone();
                needsClone();
                symb.put(prefix, ob);
                ob.lastrendered = uri;
                ob.rendered = true;
                return ob.n;
            }
            return null;
        }

        NameSpaceSymbEntry ne = new NameSpaceSymbEntry(uri, n, true, prefix);
        ne.lastrendered = uri;
        needsClone();
        symb.put(prefix, ne);
        if (ob != null && ob.lastrendered != null && ob.lastrendered.equals(uri)) {
            ne.rendered = true;
            return null;
        }
        return ne.n;
    }

    public int getLevel() {
        return level.size();
    }

    public void removeMapping(String prefix) {
        NameSpaceSymbEntry ob = symb.get(prefix);

        if (ob != null) {
            needsClone();
            symb.put(prefix, null);
        }
    }

    public void removeMappingIfNotRender(String prefix) {
        NameSpaceSymbEntry ob = symb.get(prefix);

        if (ob != null && !ob.rendered) {
            needsClone();
            symb.put(prefix, null);
        }
    }

    public boolean removeMappingIfRender(String prefix) {
        NameSpaceSymbEntry ob = symb.get(prefix);

        if (ob != null && ob.rendered) {
            needsClone();
            symb.put(prefix, null);
        }
        return false;
    }
}

/**
 * The internal structure of NameSpaceSymbTable.
 **/
class NameSpaceSymbEntry implements Cloneable {

    final String prefix;

    /**The URI that the prefix defines */
    final String uri;

    /**The attribute to include.*/
    final Attr n;

    /**The last output in the URI for this prefix (This for speed reason).*/
    String lastrendered = null;

    /**This prefix-URI has been already render or not.*/
    boolean rendered = false;

    NameSpaceSymbEntry(String name, Attr n, boolean rendered, String prefix) {
        this.uri = name;
        this.rendered = rendered;
        this.n = n;
        this.prefix = prefix;
    }

    /** {@inheritDoc} */
    public NameSpaceSymbEntry clone() { //NOPMD
        try {
            return (NameSpaceSymbEntry)super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}

class SymbMap implements Cloneable {
    int free = 23;
    NameSpaceSymbEntry[] entries;
    String[] keys;

    SymbMap() {
        entries = new NameSpaceSymbEntry[free];
        keys = new String[free];
    }

    void put(String key, NameSpaceSymbEntry value) {
        int index = index(key);
        Object oldKey = keys[index];
        keys[index] = key;
        entries[index] = value;
        if ((oldKey == null || !oldKey.equals(key)) && --free == 0) {
            free = entries.length;
            int newCapacity = free << 2;
            rehash(newCapacity);
        }
    }

    List<NameSpaceSymbEntry> entrySet() {
        List<NameSpaceSymbEntry> a = new ArrayList<>();
        for (int i = 0;i < entries.length;i++) {
            if (entries[i] != null && !"".equals(entries[i].uri)) {
                a.add(entries[i]);
            }
        }
        return a;
    }

    protected int index(Object obj) {
        Object[] set = keys;
        int length = set.length;
        //abs of index
        int index = (obj.hashCode() & 0x7fffffff) % length;
        Object cur = set[index];

        if (cur == null || cur.equals(obj)) {
            return index;
        }
        length--;
        do {
            index = index == length ? 0 : ++index;
            cur = set[index];
        } while (cur != null && !cur.equals(obj));
        return index;
    }

    /**
     * rehashes the map to the new capacity.
     *
     * @param newCapacity an {@code int} value
     */
    protected void rehash(int newCapacity) {
        int oldCapacity = keys.length;
        String[] oldKeys = keys;
        NameSpaceSymbEntry oldVals[] = entries;

        keys = new String[newCapacity];
        entries = new NameSpaceSymbEntry[newCapacity];

        for (int i = oldCapacity; i-- > 0;) {
            if (oldKeys[i] != null) {
                String o = oldKeys[i];
                int index = index(o);
                keys[index] = o;
                entries[index] = oldVals[i];
            }
        }
    }

    NameSpaceSymbEntry get(String key) {
        return entries[index(key)];
    }

    @Override
    public SymbMap clone() throws CloneNotSupportedException  {
        SymbMap copy = (SymbMap) super.clone();
        copy.entries = new NameSpaceSymbEntry[entries.length];
        System.arraycopy(entries, 0, copy.entries, 0, entries.length);
        copy.keys = new String[keys.length];
        System.arraycopy(keys, 0, copy.keys, 0, keys.length);

        return copy;
    }
}
