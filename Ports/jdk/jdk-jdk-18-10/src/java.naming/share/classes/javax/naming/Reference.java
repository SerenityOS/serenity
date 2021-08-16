/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.naming;

import java.util.Vector;
import java.util.Enumeration;

/**
  * This class represents a reference to an object that is found outside of
  * the naming/directory system.
  *<p>
  * Reference provides a way of recording address information about
  * objects which themselves are not directly bound to the naming/directory system.
  *<p>
  * A Reference consists of an ordered list of addresses and class information
  * about the object being referenced.
  * Each address in the list identifies a communications endpoint
  * for the same conceptual object.  The "communications endpoint"
  * is information that indicates how to contact the object. It could
  * be, for example, a network address, a location in memory on the
  * local machine, another process on the same machine, etc.
  * The order of the addresses in the list may be of significance
  * to object factories that interpret the reference.
  *<p>
  * Multiple addresses may arise for
  * various reasons, such as replication or the object offering interfaces
  * over more than one communication mechanism.  The addresses are indexed
  * starting with zero.
  *<p>
  * A Reference also contains information to assist in creating an instance
  * of the object to which this Reference refers.  It contains the class name
  * of that object, and the class name and location of the factory to be used
  * to create the object.
  * The class factory location is a space-separated list of URLs representing
  * the class path used to load the factory.  When the factory class (or
  * any class or resource upon which it depends) needs to be loaded,
  * each URL is used (in order) to attempt to load the class.
  *<p>
  * A Reference instance is not synchronized against concurrent access by multiple
  * threads. Threads that need to access a single Reference concurrently should
  * synchronize amongst themselves and provide the necessary locking.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see RefAddr
  * @see StringRefAddr
  * @see BinaryRefAddr
  * @since 1.3
  */

  /*<p>
  * The serialized form of a Reference object consists of the class
  * name of the object being referenced (a String), a Vector of the
  * addresses (each a RefAddr), the name of the class factory (a
  * String), and the location of the class factory (a String).
*/


public class Reference implements Cloneable, java.io.Serializable {
    /**
     * Contains the fully-qualified name of the class of the object to which
     * this Reference refers.
     * @serial
     * @see java.lang.Class#getName
     */
    protected String className;
    /**
     * Contains the addresses contained in this Reference.
     * Initialized by constructor.
     * @serial
     */
    protected Vector<RefAddr> addrs = null;

    /**
     * Contains the name of the factory class for creating
     * an instance of the object to which this Reference refers.
     * Initialized to null.
     * @serial
     */
    protected String classFactory = null;

    /**
     * Contains the location of the factory class.
     * Initialized to null.
     * @serial
     */
    protected String classFactoryLocation = null;

    /**
      * Constructs a new reference for an object with class name 'className'.
      * Class factory and class factory location are set to null.
      * The newly created reference contains zero addresses.
      *
      * @param className The non-null class name of the object to which
      * this reference refers.
      */
    public Reference(String className) {
        this.className  = className;
        addrs = new Vector<>();
    }

    /**
      * Constructs a new reference for an object with class name 'className' and
      * an address.
      * Class factory and class factory location are set to null.
      *
      * @param className The non-null class name of the object to
      * which this reference refers.
      * @param addr The non-null address of the object.
      */
    public Reference(String className, RefAddr addr) {
        this.className = className;
        addrs = new Vector<>();
        addrs.addElement(addr);
    }

    /**
      * Constructs a new reference for an object with class name 'className',
      * and the class name and location of the object's factory.
      *
      * @param className The non-null class name of the object to which
      *                         this reference refers.
      * @param factory  The possibly null class name of the object's factory.
      * @param factoryLocation
      *         The possibly null location from which to load
      *         the factory (e.g. URL)
      * @see javax.naming.spi.ObjectFactory
      * @see javax.naming.spi.NamingManager#getObjectInstance
      */
    public Reference(String className, String factory, String factoryLocation) {
        this(className);
        classFactory = factory;
        classFactoryLocation = factoryLocation;
    }

    /**
      * Constructs a new reference for an object with class name 'className',
      * the class name and location of the object's factory, and the address for
      * the object.
      *
      * @param className The non-null class name of the object to
      *         which this reference refers.
      * @param factory  The possibly null class name of the object's factory.
      * @param factoryLocation  The possibly null location from which
      *                         to load the factory (e.g. URL)
      * @param addr     The non-null address of the object.
      * @see javax.naming.spi.ObjectFactory
      * @see javax.naming.spi.NamingManager#getObjectInstance
      */
    public Reference(String className, RefAddr addr,
                     String factory, String factoryLocation) {
        this(className, addr);
        classFactory = factory;
        classFactoryLocation = factoryLocation;
    }

    /**
      * Retrieves the class name of the object to which this reference refers.
      *
      * @return The non-null fully-qualified class name of the object.
      *         (e.g. "java.lang.String")
      */
    public String getClassName() {
        return className;
    }

    /**
      * Retrieves the class name of the factory of the object
      * to which this reference refers.
      *
      * @return The possibly null fully-qualified class name of the factory.
      *         (e.g. "java.lang.String")
      */
    public String getFactoryClassName() {
        return classFactory;
    }

    /**
      * Retrieves the location of the factory of the object
      * to which this reference refers.
      * If it is a codebase, then it is an ordered list of URLs,
      * separated by spaces, listing locations from where the factory
      * class definition should be loaded.
      *
      * @return The possibly null string containing the
      *                 location for loading in the factory's class.
      */
    public String getFactoryClassLocation() {
        return classFactoryLocation;
    }

    /**
      * Retrieves the first address that has the address type 'addrType'.
      * String.compareTo() is used to test the equality of the address types.
      *
      * @param addrType The non-null address type for which to find the address.
      * @return The address in this reference with address type 'addrType';
      *         null if no such address exists.
      */
    public RefAddr get(String addrType) {
        int len = addrs.size();
        RefAddr addr;
        for (int i = 0; i < len; i++) {
            addr = addrs.elementAt(i);
            if (addr.getType().compareTo(addrType) == 0)
                return addr;
        }
        return null;
    }

    /**
      * Retrieves the address at index posn.
      * @param posn The index of the address to retrieve.
      * @return The address at the 0-based index posn. It must be in the
      *         range [0,getAddressCount()).
      * @throws ArrayIndexOutOfBoundsException If posn not in the specified
      *         range.
      */
    public RefAddr get(int posn) {
        return addrs.elementAt(posn);
    }

    /**
      * Retrieves an enumeration of the addresses in this reference.
      * When addresses are added, changed or removed from this reference,
      * its effects on this enumeration are undefined.
      *
      * @return An non-null enumeration of the addresses
      *         ({@code RefAddr}) in this reference.
      *         If this reference has zero addresses, an enumeration with
      *         zero elements is returned.
      */
    public Enumeration<RefAddr> getAll() {
        return addrs.elements();
    }

    /**
      * Retrieves the number of addresses in this reference.
      *
      * @return The nonnegative number of addresses in this reference.
      */
    public int size() {
        return addrs.size();
    }

    /**
      * Adds an address to the end of the list of addresses.
      *
      * @param addr The non-null address to add.
      */
    public void add(RefAddr addr) {
        addrs.addElement(addr);
    }

    /**
      * Adds an address to the list of addresses at index posn.
      * All addresses at index posn or greater are shifted up
      * the list by one (away from index 0).
      *
      * @param posn The 0-based index of the list to insert addr.
      * @param addr The non-null address to add.
      * @throws ArrayIndexOutOfBoundsException If posn not in the specified
      *         range.
      */
    public void add(int posn, RefAddr addr) {
        addrs.insertElementAt(addr, posn);
    }

    /**
      * Deletes the address at index posn from the list of addresses.
      * All addresses at index greater than posn are shifted down
      * the list by one (towards index 0).
      *
      * @param posn The 0-based index of in address to delete.
      * @return The address removed.
      * @throws ArrayIndexOutOfBoundsException If posn not in the specified
      *         range.
      */
    public Object remove(int posn) {
        Object r = addrs.elementAt(posn);
        addrs.removeElementAt(posn);
        return r;
    }

    /**
      * Deletes all addresses from this reference.
      */
    public void clear() {
        addrs.setSize(0);
    }

    /**
      * Determines whether obj is a reference with the same addresses
      * (in same order) as this reference.
      * The addresses are checked using RefAddr.equals().
      * In addition to having the same addresses, the Reference also needs to
      * have the same class name as this reference.
      * The class factory and class factory location are not checked.
      * If obj is null or not an instance of Reference, null is returned.
      *
      * @param obj The possibly null object to check.
      * @return true if obj is equal to this reference; false otherwise.
      */
    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof Reference)) {
            Reference target = (Reference)obj;
            // ignore factory information
            if (target.className.equals(this.className) &&
                target.size() ==  this.size()) {
                Enumeration<RefAddr> mycomps = getAll();
                Enumeration<RefAddr> comps = target.getAll();
                while (mycomps.hasMoreElements())
                    if (!(mycomps.nextElement().equals(comps.nextElement())))
                        return false;
                return true;
            }
        }
        return false;
    }

    /**
      * Computes the hash code of this reference.
      * The hash code is the sum of the hash code of its addresses.
      *
      * @return A hash code of this reference as an int.
      */
    public int hashCode() {
        int hash = className.hashCode();
        for (Enumeration<RefAddr> e = getAll(); e.hasMoreElements();)
            hash += e.nextElement().hashCode();
        return hash;
    }

    /**
      * Generates the string representation of this reference.
      * The string consists of the class name to which this reference refers,
      * and the string representation of each of its addresses.
      * This representation is intended for display only and not to be parsed.
      *
      * @return The non-null string representation of this reference.
      */
    public String toString() {
        StringBuilder sb = new StringBuilder("Reference Class Name: " +
                                             className + "\n");
        int len = addrs.size();
        for (int i = 0; i < len; i++)
            sb.append(get(i).toString());

        return sb.toString();
    }

    /**
     * Makes a copy of this reference using its class name
     * list of addresses, class factory name and class factory location.
     * Changes to the newly created copy does not affect this Reference
     * and vice versa.
     */
    public Object clone() {
        Reference r = new Reference(className, classFactory, classFactoryLocation);
        Enumeration<RefAddr> a = getAll();
        r.addrs = new Vector<>();

        while (a.hasMoreElements())
            r.addrs.addElement(a.nextElement());
        return r;
    }
    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    private static final long serialVersionUID = -1673475790065791735L;
};
