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

import java.util.Enumeration;
import java.util.Properties;

/**
 * This class represents a compound name -- a name from
 * a hierarchical name space.
 * Each component in a compound name is an atomic name.
 * <p>
 * The components of a compound name are numbered.  The indexes of a
 * compound name with N components range from 0 up to, but not including, N.
 * This range may be written as [0,N).
 * The most significant component is at index 0.
 * An empty compound name has no components.
 *
 * <h2>Compound Name Syntax</h2>
 * The syntax of a compound name is specified using a set of properties:
 *<dl>
 *  <dt>jndi.syntax.direction
 *  <dd>Direction for parsing ("right_to_left", "left_to_right", "flat").
 *      If unspecified, defaults to "flat", which means the namespace is flat
 *      with no hierarchical structure.
 *
 *  <dt>jndi.syntax.separator
 *  <dd>Separator between atomic name components.
 *      Required unless direction is "flat".
 *
 *  <dt>jndi.syntax.ignorecase
 *  <dd>If present, "true" means ignore the case when comparing name
 *      components. If its value is not "true", or if the property is not
 *      present, case is considered when comparing name components.
 *
 *  <dt>jndi.syntax.escape
 *  <dd>If present, specifies the escape string for overriding separator,
 *      escapes and quotes.
 *
 *  <dt>jndi.syntax.beginquote
 *  <dd>If present, specifies the string delimiting start of a quoted string.
 *
 *  <dt>jndi.syntax.endquote
 *  <dd>String delimiting end of quoted string.
 *      If present, specifies the string delimiting the end of a quoted string.
 *      If not present, use syntax.beginquote as end quote.
 *  <dt>jndi.syntax.beginquote2
 *  <dd>Alternative set of begin/end quotes.
 *
 *  <dt>jndi.syntax.endquote2
 *  <dd>Alternative set of begin/end quotes.
 *
 *  <dt>jndi.syntax.trimblanks
 *  <dd>If present, "true" means trim any leading and trailing whitespaces
 *      in a name component for comparison purposes. If its value is not
 *      "true", or if the property is not present, blanks are significant.
 *  <dt>jndi.syntax.separator.ava
 *  <dd>If present, specifies the string that separates
 *      attribute-value-assertions when specifying multiple attribute/value
 *      pairs. (e.g. ","  in age=65,gender=male).
 *  <dt>jndi.syntax.separator.typeval
 *  <dd>If present, specifies the string that separates attribute
 *              from value (e.g. "=" in "age=65")
 *</dl>
 * These properties are interpreted according to the following rules:
 *<ol>
 *<li>
 * In a string without quotes or escapes, any instance of the
 * separator delimits two atomic names. Each atomic name is referred
 * to as a <em>component</em>.
 *<li>
 * A separator, quote or escape is escaped if preceded immediately
 * (on the left) by the escape.
 *<li>
 * If there are two sets of quotes, a specific begin-quote must be matched
 * by its corresponding end-quote.
 *<li>
 * A non-escaped begin-quote which precedes a component must be
 * matched by a non-escaped end-quote at the end of the component.
 * A component thus quoted is referred to as a
 * <em>quoted component</em>. It is parsed by
 * removing the being- and end- quotes, and by treating the intervening
 * characters as ordinary characters unless one of the rules involving
 * quoted components listed below applies.
 *<li>
 * Quotes embedded in non-quoted components are treated as ordinary strings
 * and need not be matched.
 *<li>
 * A separator that is escaped or appears between non-escaped
 * quotes is treated as an ordinary string and not a separator.
 *<li>
 * An escape string within a quoted component acts as an escape only when
 * followed by the corresponding end-quote string.
 * This can be used to embed an escaped quote within a quoted component.
 *<li>
 * An escaped escape string is not treated as an escape string.
 *<li>
 * An escape string that does not precede a meta string (quotes or separator)
 * and is not at the end of a component is treated as an ordinary string.
 *<li>
 * A leading separator (the compound name string begins with
 * a separator) denotes a leading empty atomic component (consisting
 * of an empty string).
 * A trailing separator (the compound name string ends with
 * a separator) denotes a trailing empty atomic component.
 * Adjacent separators denote an empty atomic component.
 *</ol>
 * <p>
 * The string form of the compound name follows the syntax described above.
 * When the components of the compound name are turned into their
 * string representation, the reserved syntax rules described above are
 * applied (e.g. embedded separators are escaped or quoted)
 * so that when the same string is parsed, it will yield the same components
 * of the original compound name.
 *
 *<h2>Multithreaded Access</h2>
 * A {@code CompoundName} instance is not synchronized against concurrent
 * multithreaded access. Multiple threads trying to access and modify a
 * {@code CompoundName} should lock the object.
 *
 * @author Rosanna Lee
 * @author Scott Seligman
 * @since 1.3
 */

public class CompoundName implements Name {

    /**
     * Implementation of this compound name. This field is initialized by the
     * constructors and cannot be null.
     */
    private transient NameImpl impl;
    /**
      * Syntax properties for this compound name.
      * This field is initialized by the constructors and cannot be null.
      * It should be treated as a read-only variable by subclasses.
      * Any necessary changes to mySyntax should be made within constructors
      * and not after the compound name has been instantiated.
      */
    protected transient Properties mySyntax;

    /**
      * Constructs a new compound name instance using the components
      * specified in comps and syntax. This protected method is intended
      * to be used by subclasses of CompoundName when they override
      * methods such as clone(), getPrefix(), getSuffix().
      *
      * @param comps  A non-null enumeration of the components to add.
      *   Each element of the enumeration is of class String.
      *               The enumeration will be consumed to extract its
      *               elements.
      * @param syntax   A non-null properties that specify the syntax of
      *                 this compound name. See class description for
      *                 contents of properties.
      */
    protected CompoundName(Enumeration<String> comps, Properties syntax) {
        if (syntax == null) {
            throw new NullPointerException();
        }
        mySyntax = syntax;
        impl = new NameImpl(syntax, comps);
    }

    /**
      * Constructs a new compound name instance by parsing the string n
      * using the syntax specified by the syntax properties supplied.
      *
      * @param  n       The non-null string to parse.
      * @param syntax   A non-null list of properties that specify the syntax of
      *                 this compound name.  See class description for
      *                 contents of properties.
      * @throws         InvalidNameException If 'n' violates the syntax specified
      *                 by {@code syntax}.
      */
    public CompoundName(String n, Properties syntax) throws InvalidNameException {
        if (syntax == null) {
            throw new NullPointerException();
        }
        mySyntax = syntax;
        impl = new NameImpl(syntax, n);
    }

    /**
      * Generates the string representation of this compound name, using
      * the syntax rules of the compound name. The syntax rules
      * are described in the class description.
      * An empty component is represented by an empty string.
      *
      * The string representation thus generated can be passed to
      * the CompoundName constructor with the same syntax properties
      * to create a new equivalent compound name.
      *
      * @return A non-null string representation of this compound name.
      */
    public String toString() {
        return (impl.toString());
    }

    /**
      * Determines whether obj is syntactically equal to this compound name.
      * If obj is null or not a CompoundName, false is returned.
      * Two compound names are equal if each component in one is "equal"
      * to the corresponding component in the other.
      *<p>
      * Equality is also defined in terms of the syntax of this compound name.
      * The default implementation of CompoundName uses the syntax properties
      * jndi.syntax.ignorecase and jndi.syntax.trimblanks when comparing
      * two components for equality.  If case is ignored, two strings
      * with the same sequence of characters but with different cases
      * are considered equal. If blanks are being trimmed, leading and trailing
      * blanks are ignored for the purpose of the comparison.
      *<p>
      * Both compound names must have the same number of components.
      *<p>
      * Implementation note: Currently the syntax properties of the two compound
      * names are not compared for equality. They might be in the future.
      *
      * @param  obj     The possibly null object to compare against.
      * @return true if obj is equal to this compound name, false otherwise.
      * @see #compareTo(java.lang.Object obj)
      */
    public boolean equals(Object obj) {
        // %%% check syntax too?
        return (obj != null &&
                obj instanceof CompoundName &&
                impl.equals(((CompoundName)obj).impl));
    }

    /**
      * Computes the hash code of this compound name.
      * The hash code is the sum of the hash codes of the "canonicalized"
      * forms of individual components of this compound name.
      * Each component is "canonicalized" according to the
      * compound name's syntax before its hash code is computed.
      * For a case-insensitive name, for example, the uppercased form of
      * a name has the same hash code as its lowercased equivalent.
      *
      * @return An int representing the hash code of this name.
      */
    public int hashCode() {
        return impl.hashCode();
    }

    /**
      * Creates a copy of this compound name.
      * Changes to the components of this compound name won't
      * affect the new copy and vice versa.
      * The clone and this compound name share the same syntax.
      *
      * @return A non-null copy of this compound name.
      */
    public Object clone() {
        return (new CompoundName(getAll(), mySyntax));
    }

    /**
     * Compares this CompoundName with the specified Object for order.
     * Returns a
     * negative integer, zero, or a positive integer as this Name is less
     * than, equal to, or greater than the given Object.
     * <p>
     * If obj is null or not an instance of CompoundName, ClassCastException
     * is thrown.
     * <p>
     * See equals() for what it means for two compound names to be equal.
     * If two compound names are equal, 0 is returned.
     *<p>
     * Ordering of compound names depend on the syntax of the compound name.
     * By default, they follow lexicographical rules for string comparison
     * with the extension that this applies to all the components in the
     * compound name and that comparison of individual components is
     * affected by the jndi.syntax.ignorecase and jndi.syntax.trimblanks
     * properties, identical to how they affect equals().
     * If this compound name is "lexicographically" lesser than obj,
     * a negative number is returned.
     * If this compound name is "lexicographically" greater than obj,
     * a positive number is returned.
     *<p>
     * Implementation note: Currently the syntax properties of the two compound
     * names are not compared when checking order. They might be in the future.
     * @param   obj     The non-null object to compare against.
     * @return  a negative integer, zero, or a positive integer as this Name
     *          is less than, equal to, or greater than the given Object.
     * @throws ClassCastException if obj is not a CompoundName.
     * @see #equals(java.lang.Object)
     */
    public int compareTo(Object obj) {
        if (!(obj instanceof CompoundName)) {
            throw new ClassCastException("Not a CompoundName");
        }
        return impl.compareTo(((CompoundName)obj).impl);
    }

    /**
      * Retrieves the number of components in this compound name.
      *
      * @return The nonnegative number of components in this compound name.
      */
    public int size() {
        return (impl.size());
    }

    /**
      * Determines whether this compound name is empty.
      * A compound name is empty if it has zero components.
      *
      * @return true if this compound name is empty, false otherwise.
      */
    public boolean isEmpty() {
        return (impl.isEmpty());
    }

    /**
      * Retrieves the components of this compound name as an enumeration
      * of strings.
      * The effects of updates to this compound name on this enumeration
      * is undefined.
      *
      * @return A non-null enumeration of the components of this
      * compound name. Each element of the enumeration is of class String.
      */
    public Enumeration<String> getAll() {
        return (impl.getAll());
    }

    /**
      * Retrieves a component of this compound name.
      *
      * @param  posn    The 0-based index of the component to retrieve.
      *                 Must be in the range [0,size()).
      * @return The component at index posn.
      * @throws ArrayIndexOutOfBoundsException if posn is outside the
      *         specified range.
      */
    public String get(int posn) {
        return (impl.get(posn));
    }

    /**
      * Creates a compound name whose components consist of a prefix of the
      * components in this compound name.
      * The result and this compound name share the same syntax.
      * Subsequent changes to
      * this compound name do not affect the name that is returned and
      * vice versa.
      *
      * @param  posn    The 0-based index of the component at which to stop.
      *                 Must be in the range [0,size()].
      * @return A compound name consisting of the components at indexes in
      *         the range [0,posn).
      * @throws ArrayIndexOutOfBoundsException
      *         If posn is outside the specified range.
      */
    public Name getPrefix(int posn) {
        Enumeration<String> comps = impl.getPrefix(posn);
        return (new CompoundName(comps, mySyntax));
    }

    /**
      * Creates a compound name whose components consist of a suffix of the
      * components in this compound name.
      * The result and this compound name share the same syntax.
      * Subsequent changes to
      * this compound name do not affect the name that is returned.
      *
      * @param  posn    The 0-based index of the component at which to start.
      *                 Must be in the range [0,size()].
      * @return A compound name consisting of the components at indexes in
      *         the range [posn,size()).  If posn is equal to
      *         size(), an empty compound name is returned.
      * @throws ArrayIndexOutOfBoundsException
      *         If posn is outside the specified range.
      */
    public Name getSuffix(int posn) {
        Enumeration<String> comps = impl.getSuffix(posn);
        return (new CompoundName(comps, mySyntax));
    }

    /**
      * Determines whether a compound name is a prefix of this compound name.
      * A compound name 'n' is a prefix if it is equal to
      * getPrefix(n.size())--in other words, this compound name
      * starts with 'n'.
      * If n is null or not a compound name, false is returned.
      *<p>
      * Implementation note: Currently the syntax properties of n
      *  are not used when doing the comparison. They might be in the future.
      * @param  n       The possibly null compound name to check.
      * @return true if n is a CompoundName and
      *                 is a prefix of this compound name, false otherwise.
      */
    public boolean startsWith(Name n) {
        if (n instanceof CompoundName) {
            return (impl.startsWith(n.size(), n.getAll()));
        } else {
            return false;
        }
    }

    /**
      * Determines whether a compound name is a suffix of this compound name.
      * A compound name 'n' is a suffix if it is equal to
      * getSuffix(size()-n.size())--in other words, this
      * compound name ends with 'n'.
      * If n is null or not a compound name, false is returned.
      *<p>
      * Implementation note: Currently the syntax properties of n
      *  are not used when doing the comparison. They might be in the future.
      * @param  n       The possibly null compound name to check.
      * @return true if n is a CompoundName and
      *         is a suffix of this compound name, false otherwise.
      */
    public boolean endsWith(Name n) {
        if (n instanceof CompoundName) {
            return (impl.endsWith(n.size(), n.getAll()));
        } else {
            return false;
        }
    }

    /**
      * Adds the components of a compound name -- in order -- to the end of
      * this compound name.
      *<p>
      * Implementation note: Currently the syntax properties of suffix
      *  is not used or checked. They might be in the future.
      * @param suffix   The non-null components to add.
      * @return The updated CompoundName, not a new one. Cannot be null.
      * @throws InvalidNameException If suffix is not a compound name,
      *         or if the addition of the components violates the syntax
      *         of this compound name (e.g. exceeding number of components).
      */
    public Name addAll(Name suffix) throws InvalidNameException {
        if (suffix instanceof CompoundName) {
            impl.addAll(suffix.getAll());
            return this;
        } else {
            throw new InvalidNameException("Not a compound name: " +
                suffix.toString());
        }
    }

    /**
      * Adds the components of a compound name -- in order -- at a specified
      * position within this compound name.
      * Components of this compound name at or after the index of the first
      * new component are shifted up (away from index 0)
      * to accommodate the new components.
      *<p>
      * Implementation note: Currently the syntax properties of suffix
      *  is not used or checked. They might be in the future.
      *
      * @param n        The non-null components to add.
      * @param posn     The index in this name at which to add the new
      *                 components.  Must be in the range [0,size()].
      * @return The updated CompoundName, not a new one. Cannot be null.
      * @throws ArrayIndexOutOfBoundsException
      *         If posn is outside the specified range.
      * @throws InvalidNameException If n is not a compound name,
      *         or if the addition of the components violates the syntax
      *         of this compound name (e.g. exceeding number of components).
      */
    public Name addAll(int posn, Name n) throws InvalidNameException {
        if (n instanceof CompoundName) {
            impl.addAll(posn, n.getAll());
            return this;
        } else {
            throw new InvalidNameException("Not a compound name: " +
                n.toString());
        }
    }

    /**
      * Adds a single component to the end of this compound name.
      *
      * @param comp     The non-null component to add.
      * @return The updated CompoundName, not a new one. Cannot be null.
      * @throws InvalidNameException If adding comp at end of the name
      *         would violate the compound name's syntax.
      */
    public Name add(String comp) throws InvalidNameException{
        impl.add(comp);
        return this;
    }

    /**
      * Adds a single component at a specified position within this
      * compound name.
      * Components of this compound name at or after the index of the new
      * component are shifted up by one (away from index 0)
      * to accommodate the new component.
      *
      * @param  comp    The non-null component to add.
      * @param  posn    The index at which to add the new component.
      *                 Must be in the range [0,size()].
      * @throws ArrayIndexOutOfBoundsException
      *         If posn is outside the specified range.
      * @return The updated CompoundName, not a new one. Cannot be null.
      * @throws InvalidNameException If adding comp at the specified position
      *         would violate the compound name's syntax.
      */
    public Name add(int posn, String comp) throws InvalidNameException{
        impl.add(posn, comp);
        return this;
    }

    /**
      * Deletes a component from this compound name.
      * The component of this compound name at position 'posn' is removed,
      * and components at indices greater than 'posn'
      * are shifted down (towards index 0) by one.
      *
      * @param  posn    The index of the component to delete.
      *                 Must be in the range [0,size()).
      * @return The component removed (a String).
      * @throws ArrayIndexOutOfBoundsException
      *         If posn is outside the specified range (includes case where
      *         compound name is empty).
      * @throws InvalidNameException If deleting the component
      *         would violate the compound name's syntax.
      */
    public Object remove(int posn) throws InvalidNameException {
        return impl.remove(posn);
    }

    /**
     * The writeObject method is called to save the state of the
     * {@code CompoundName} to a stream.
     *
     * @serialData The syntax {@code Properties}, followed by
     * the number of components (an {@code int}), and the individual
     * components (each a {@code String}).
     *
     * @param s the {@code ObjectOutputStream} to write to
     * @throws java.io.IOException if an I/O error occurs
     */
    @java.io.Serial
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        // Overridden to avoid implementation dependency
        s.writeObject(mySyntax);
        s.writeInt(size());
        Enumeration<String> comps = getAll();
        while (comps.hasMoreElements()) {
            s.writeObject(comps.nextElement());
        }
    }

    /**
     * The readObject method is called to restore the state of
     * the {@code CompoundName} from a stream.
     *
     * See {@code writeObject} for a description of the serial form.
     *
     * @param s the {@code ObjectInputStream} to read from
     * @throws java.io.IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class of a serialized object
     *         could not be found
     */
    @java.io.Serial
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        // Overridden to avoid implementation dependency.
        mySyntax = (Properties)s.readObject();
        impl = new NameImpl(mySyntax);
        int n = s.readInt();    // number of components
        try {
            while (--n >= 0) {
                add((String)s.readObject());
            }
        } catch (InvalidNameException e) {
            throw (new java.io.StreamCorruptedException("Invalid name"));
        }
    }

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    @java.io.Serial
    private static final long serialVersionUID = 3513100557083972036L;

/*
//   For testing

    public static void main(String[] args) {
        Properties dotSyntax = new Properties();
        dotSyntax.put("jndi.syntax.direction", "right_to_left");
        dotSyntax.put("jndi.syntax.separator", ".");
        dotSyntax.put("jndi.syntax.ignorecase", "true");
        dotSyntax.put("jndi.syntax.escape", "\\");
//      dotSyntax.put("jndi.syntax.beginquote", "\"");
//      dotSyntax.put("jndi.syntax.beginquote2", "'");

        Name first = null;
        try {
            for (int i = 0; i < args.length; i++) {
                Name name;
                Enumeration e;
                System.out.println("Given name: " + args[i]);
                name = new CompoundName(args[i], dotSyntax);
                if (first == null) {
                    first = name;
                }
                e = name.getComponents();
                while (e.hasMoreElements()) {
                    System.out.println("Element: " + e.nextElement());
                }
                System.out.println("Constructed name: " + name.toString());

                System.out.println("Compare " + first.toString() + " with "
                    + name.toString() + " = " + first.compareTo(name));
            }
        } catch (Exception ne) {
            ne.printStackTrace();
        }
    }
*/
}
