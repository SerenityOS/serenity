/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javax.naming.directory;

import javax.naming.*;

/**
 * The directory service interface, containing
 * methods for examining and updating attributes
 * associated with objects, and for searching the directory.
 *
 * <h2>Names</h2>
 * Each name passed as an argument to a {@code DirContext} method is relative
 * to that context.  The empty name is used to name the context itself.
 * The name parameter may never be null.
 * <p>
 * Most of the methods have overloaded versions with one taking a
 * <code>Name</code> parameter and one taking a <code>String</code>.
 * These overloaded versions are equivalent in that if
 * the <code>Name</code> and <code>String</code> parameters are just
 * different representations of the same name, then the overloaded
 * versions of the same methods behave the same.
 * In the method descriptions below, only one version is documented.
 * The second version instead has a link to the first:  the same
 * documentation applies to both.
 * <p>
 * See {@code Context} for a discussion on the interpretation of the
 * name argument to the {@code Context} methods. These same rules
 * apply to the name argument to the {@code DirContext} methods.
 *
 * <h2>Attribute Models</h2>
 * There are two basic models of what attributes should be
 * associated with.  First, attributes may be directly associated with a
 * DirContext object.
 * In this model, an attribute operation on the named object is
 * roughly equivalent
 * to a lookup on the name (which returns the DirContext object),
 * followed by the attribute operation invoked on the DirContext object
 * in which the caller supplies an empty name. The attributes can be viewed
 * as being stored along with the object (note that this does not imply that
 * the implementation must do so).
 * <p>
 * The second model is that attributes are associated with a
 * name (typically an atomic name) in a DirContext.
 * In this model, an attribute operation on the named object is
 * roughly equivalent to a lookup on the name of the parent DirContext of the
 * named object, followed by the attribute operation invoked on the parent
 * in which the caller supplies the terminal atomic name.
 * The attributes can be viewed as being stored in the parent DirContext
 * (again, this does not imply that the implementation must do so).
 * Objects that are not DirContexts can have attributes, as long as
 * their parents are DirContexts.
 * <p>
 * JNDI support both of these models.
 * It is up to the individual service providers to decide where to
 * "store" attributes.
 * JNDI clients are safest when they do not make assumptions about
 * whether an object's attributes are stored as part of the object, or stored
 * within the parent object and associated with the object's name.
 *
 * <h2>Attribute Type Names</h2>
 * In the {@code getAttributes()} and {@code search()} methods,
 * you can supply the attributes to return by supplying a list of
 * attribute names (strings).
 * The attributes that you get back might not have the same names as the
 * attribute names you have specified. This is because some directories
 * support features that cause them to return other attributes.  Such
 * features include attribute subclassing, attribute name synonyms, and
 * attribute language codes.
 * <p>
 * In attribute subclassing, attributes are defined in a class hierarchy.
 * In some directories, for example, the "name" attribute might be the
 * superclass of all name-related attributes, including "commonName" and
 * "surName".  Asking for the "name" attribute might return both the
 * "commonName" and "surName" attributes.
 * <p>
 * With attribute type synonyms, a directory can assign multiple names to
 * the same attribute. For example, "cn" and "commonName" might both
 * refer to the same attribute. Asking for "cn" might return the
 * "commonName" attribute.
 * <p>
 * Some directories support the language codes for attributes.
 * Asking such a directory for the "description" attribute, for example,
 * might return all of the following attributes:
 * <ul>
 * <li>description
 * <li>description;lang-en
 * <li>description;lang-de
 * <li>description;lang-fr
 * </ul>
 *
 *
 *<h2>Operational Attributes</h2>
 *<p>
 * Some directories have the notion of "operational attributes" which are
 * attributes associated with a directory object for administrative
 * purposes. An example of operational attributes is the access control
 * list for an object.
 * <p>
 * In the {@code getAttributes()} and {@code search()} methods,
 * you can specify that all attributes associated with the requested objects
 * be returned by supply {@code null} as the list of attributes to return.
 * The attributes returned do <em>not</em> include operational attributes.
 * In order to retrieve operational attributes, you must name them explicitly.
 *
 *
 * <h2>Named Context</h2>
 * <p>
 * There are certain methods in which the name must resolve to a context
 * (for example, when searching a single level context). The documentation
 * of such methods
 * use the term <em>named context</em> to describe their name parameter.
 * For these methods, if the named object is not a DirContext,
 * <code>NotContextException</code> is thrown.
 * Aside from these methods, there is no requirement that the
 * <em>named object</em> be a DirContext.
 *
 *<h2>Parameters</h2>
 *<p>
 * An {@code Attributes}, {@code SearchControls}, or array object
 * passed as a parameter to any method will not be modified by the
 * service provider.  The service provider may keep a reference to it
 * for the duration of the operation, including any enumeration of the
 * method's results and the processing of any referrals generated.
 * The caller should not modify the object during this time.
 * An {@code Attributes} object returned by any method is owned by
 * the caller.  The caller may subsequently modify it; the service
 * provider will not.
 *
 *<h2>Exceptions</h2>
 *<p>
 * All the methods in this interface can throw a NamingException or
 * any of its subclasses. See NamingException and their subclasses
 * for details on each exception.
 *
 * @author Rosanna Lee
 * @author Scott Seligman
 * @author R. Vasudevan
 *
 * @see javax.naming.Context
 * @since 1.3
 */

public interface DirContext extends Context {

    /**
     * Retrieves all of the attributes associated with a named object.
     * See the class description regarding attribute models, attribute
     * type names, and operational attributes.
     *
     * @param name
     *          the name of the object from which to retrieve attributes
     * @return  the set of attributes associated with <code>name</code>.
     *          Returns an empty attribute set if name has no attributes;
     *          never null.
     * @throws  NamingException if a naming exception is encountered
     *
     * @see #getAttributes(String)
     * @see #getAttributes(Name, String[])
     */
    public Attributes getAttributes(Name name) throws NamingException;

    /**
     * Retrieves all of the attributes associated with a named object.
     * See {@link #getAttributes(Name)} for details.
     *
     * @param name
     *          the name of the object from which to retrieve attributes
     * @return  the set of attributes associated with <code>name</code>
     *
     * @throws  NamingException if a naming exception is encountered
     */
    public Attributes getAttributes(String name) throws NamingException;

    /**
     * Retrieves selected attributes associated with a named object.
     * See the class description regarding attribute models, attribute
     * type names, and operational attributes.
     *
     * <p> If the object does not have an attribute
     * specified, the directory will ignore the nonexistent attribute
     * and return those requested attributes that the object does have.
     *
     * <p> A directory might return more attributes than was requested
     * (see <strong>Attribute Type Names</strong> in the class description),
     * but is not allowed to return arbitrary, unrelated attributes.
     *
     * <p> See also <strong>Operational Attributes</strong> in the class
     * description.
     *
     * @param name
     *          the name of the object from which to retrieve attributes
     * @param attrIds
     *          the identifiers of the attributes to retrieve.
     *          null indicates that all attributes should be retrieved;
     *          an empty array indicates that none should be retrieved.
     * @return  the requested attributes; never null
     *
     * @throws  NamingException if a naming exception is encountered
     */
    public Attributes getAttributes(Name name, String[] attrIds)
            throws NamingException;

    /**
     * Retrieves selected attributes associated with a named object.
     * See {@link #getAttributes(Name, String[])} for details.
     *
     * @param name
     *          The name of the object from which to retrieve attributes
     * @param attrIds
     *          the identifiers of the attributes to retrieve.
     *          null indicates that all attributes should be retrieved;
     *          an empty array indicates that none should be retrieved.
     * @return  the requested attributes; never null
     *
     * @throws  NamingException if a naming exception is encountered
     */
    public Attributes getAttributes(String name, String[] attrIds)
            throws NamingException;

    /**
     * This constant specifies to add an attribute with the specified values.
     * <p>
     * If attribute does not exist,
     * create the attribute.  The resulting attribute has a union of the
     * specified value set and the prior value set.
     * Adding an attribute with no value will throw
     * <code>InvalidAttributeValueException</code> if the attribute must have
     * at least  one value.  For a single-valued attribute where that attribute
     * already exists, throws <code>AttributeInUseException</code>.
     * If attempting to add more than one value to a single-valued attribute,
     * throws <code>InvalidAttributeValueException</code>.
     * <p>
     * The value of this constant is {@code 1}.
     *
     * @see ModificationItem
     * @see #modifyAttributes
     */
    public static final int ADD_ATTRIBUTE = 1;

    /**
     * This constant specifies to replace an attribute with specified values.
     *<p>
     * If attribute already exists,
     * replaces all existing values with new specified values.  If the
     * attribute does not exist, creates it.  If no value is specified,
     * deletes all the values of the attribute.
     * Removal of the last value will remove the attribute if the attribute
     * is required to have at least one value.  If
     * attempting to add more than one value to a single-valued attribute,
     * throws <code>InvalidAttributeValueException</code>.
     * <p>
     * The value of this constant is {@code 2}.
     *
     * @see ModificationItem
     * @see #modifyAttributes
     */
    public static final int REPLACE_ATTRIBUTE = 2;

    /**
     * This constant specifies to delete
     * the specified attribute values from the attribute.
     *<p>
     * The resulting attribute has the set difference of its prior value set
     * and the specified value set.
     * If no values are specified, deletes the entire attribute.
     * If the attribute does not exist, or if some or all members of the
     * specified value set do not exist, this absence may be ignored
     * and the operation succeeds, or a NamingException may be thrown to
     * indicate the absence.
     * Removal of the last value will remove the attribute if the
     * attribute is required to have at least one value.
     * <p>
     * The value of this constant is {@code 3}.
     *
     * @see ModificationItem
     * @see #modifyAttributes
     */
    public static final int REMOVE_ATTRIBUTE = 3;

    /**
     * Modifies the attributes associated with a named object.
     * The order of the modifications is not specified.  Where
     * possible, the modifications are performed atomically.
     *
     * @param name
     *          the name of the object whose attributes will be updated
     * @param mod_op
     *          the modification operation, one of:
     *                  <code>ADD_ATTRIBUTE</code>,
     *                  <code>REPLACE_ATTRIBUTE</code>,
     *                  <code>REMOVE_ATTRIBUTE</code>.
     * @param attrs
     *          the attributes to be used for the modification; may not be null
     *
     * @throws  AttributeModificationException if the modification cannot
     *          be completed successfully
     * @throws  NamingException if a naming exception is encountered
     *
     * @see #modifyAttributes(Name, ModificationItem[])
     */
    public void modifyAttributes(Name name, int mod_op, Attributes attrs)
            throws NamingException;

    /**
     * Modifies the attributes associated with a named object.
     * See {@link #modifyAttributes(Name, int, Attributes)} for details.
     *
     * @param name
     *          the name of the object whose attributes will be updated
     * @param mod_op
     *          the modification operation, one of:
     *                  <code>ADD_ATTRIBUTE</code>,
     *                  <code>REPLACE_ATTRIBUTE</code>,
     *                  <code>REMOVE_ATTRIBUTE</code>.
     * @param attrs
     *          the attributes to be used for the modification; may not be null
     *
     * @throws  AttributeModificationException if the modification cannot
     *          be completed successfully
     * @throws  NamingException if a naming exception is encountered
     */
    public void modifyAttributes(String name, int mod_op, Attributes attrs)
            throws NamingException;

    /**
     * Modifies the attributes associated with a named object using
     * an ordered list of modifications.
     * The modifications are performed
     * in the order specified.  Each modification specifies a
     * modification operation code and an attribute on which to
     * operate.  Where possible, the modifications are
     * performed atomically.
     *
     * @param name
     *          the name of the object whose attributes will be updated
     * @param mods
     *          an ordered sequence of modifications to be performed;
     *          may not be null
     *
     * @throws  AttributeModificationException if the modifications
     *          cannot be completed successfully
     * @throws  NamingException if a naming exception is encountered
     *
     * @see #modifyAttributes(Name, int, Attributes)
     * @see ModificationItem
     */
    public void modifyAttributes(Name name, ModificationItem[] mods)
            throws NamingException;

    /**
     * Modifies the attributes associated with a named object using
     * an ordered list of modifications.
     * See {@link #modifyAttributes(Name, ModificationItem[])} for details.
     *
     * @param name
     *          the name of the object whose attributes will be updated
     * @param mods
     *          an ordered sequence of modifications to be performed;
     *          may not be null
     *
     * @throws  AttributeModificationException if the modifications
     *          cannot be completed successfully
     * @throws  NamingException if a naming exception is encountered
     */
    public void modifyAttributes(String name, ModificationItem[] mods)
            throws NamingException;

    /**
     * Binds a name to an object, along with associated attributes.
     * If {@code attrs} is null, the resulting binding will have
     * the attributes associated with {@code obj} if {@code obj} is a
     * {@code DirContext}, and no attributes otherwise.
     * If {@code attrs} is non-null, the resulting binding will have
     * {@code attrs} as its attributes; any attributes associated with
     * {@code obj} are ignored.
     *
     * @param name
     *          the name to bind; may not be empty
     * @param obj
     *          the object to bind; possibly null
     * @param attrs
     *          the attributes to associate with the binding
     *
     * @throws  NameAlreadyBoundException if name is already bound
     * @throws  InvalidAttributesException if some "mandatory" attributes
     *          of the binding are not supplied
     * @throws  NamingException if a naming exception is encountered
     *
     * @see Context#bind(Name, Object)
     * @see #rebind(Name, Object, Attributes)
     */
    public void bind(Name name, Object obj, Attributes attrs)
            throws NamingException;

    /**
     * Binds a name to an object, along with associated attributes.
     * See {@link #bind(Name, Object, Attributes)} for details.
     *
     * @param name
     *          the name to bind; may not be empty
     * @param obj
     *          the object to bind; possibly null
     * @param attrs
     *          the attributes to associate with the binding
     *
     * @throws  NameAlreadyBoundException if name is already bound
     * @throws  InvalidAttributesException if some "mandatory" attributes
     *          of the binding are not supplied
     * @throws  NamingException if a naming exception is encountered
     */
    public void bind(String name, Object obj, Attributes attrs)
            throws NamingException;

    /**
     * Binds a name to an object, along with associated attributes,
     * overwriting any existing binding.
     * If {@code attrs} is null and {@code obj} is a {@code DirContext},
     * the attributes from {@code obj} are used.
     * If {@code attrs} is null and {@code obj} is not a {@code DirContext},
     * any existing attributes associated with the object already bound
     * in the directory remain unchanged.
     * If {@code attrs} is non-null, any existing attributes associated with
     * the object already bound in the directory are removed and {@code attrs}
     * is associated with the named object.  If {@code obj} is a
     * {@code DirContext} and {@code attrs} is non-null, the attributes
     * of {@code obj} are ignored.
     *
     * @param name
     *          the name to bind; may not be empty
     * @param obj
     *          the object to bind; possibly null
     * @param attrs
     *          the attributes to associate with the binding
     *
     * @throws  InvalidAttributesException if some "mandatory" attributes
     *          of the binding are not supplied
     * @throws  NamingException if a naming exception is encountered
     *
     * @see Context#bind(Name, Object)
     * @see #bind(Name, Object, Attributes)
     */
    public void rebind(Name name, Object obj, Attributes attrs)
            throws NamingException;

    /**
     * Binds a name to an object, along with associated attributes,
     * overwriting any existing binding.
     * See {@link #rebind(Name, Object, Attributes)} for details.
     *
     * @param name
     *          the name to bind; may not be empty
     * @param obj
     *          the object to bind; possibly null
     * @param attrs
     *          the attributes to associate with the binding
     *
     * @throws  InvalidAttributesException if some "mandatory" attributes
     *          of the binding are not supplied
     * @throws  NamingException if a naming exception is encountered
     */
    public void rebind(String name, Object obj, Attributes attrs)
            throws NamingException;

    /**
     * Creates and binds a new context, along with associated attributes.
     * This method creates a new subcontext with the given name, binds it in
     * the target context (that named by all but terminal atomic
     * component of the name), and associates the supplied attributes
     * with the newly created object.
     * All intermediate and target contexts must already exist.
     * If {@code attrs} is null, this method is equivalent to
     * {@code Context.createSubcontext()}.
     *
     * @param name
     *          the name of the context to create; may not be empty
     * @param attrs
     *          the attributes to associate with the newly created context
     * @return  the newly created context
     *
     * @throws  NameAlreadyBoundException if the name is already bound
     * @throws  InvalidAttributesException if <code>attrs</code> does not
     *          contain all the mandatory attributes required for creation
     * @throws  NamingException if a naming exception is encountered
     *
     * @see Context#createSubcontext(Name)
     */
    public DirContext createSubcontext(Name name, Attributes attrs)
            throws NamingException;

    /**
     * Creates and binds a new context, along with associated attributes.
     * See {@link #createSubcontext(Name, Attributes)} for details.
     *
     * @param name
     *          the name of the context to create; may not be empty
     * @param attrs
     *          the attributes to associate with the newly created context
     * @return  the newly created context
     *
     * @throws  NameAlreadyBoundException if the name is already bound
     * @throws  InvalidAttributesException if <code>attrs</code> does not
     *          contain all the mandatory attributes required for creation
     * @throws  NamingException if a naming exception is encountered
     */
    public DirContext createSubcontext(String name, Attributes attrs)
            throws NamingException;

// -------------------- schema operations

    /**
     * Retrieves the schema associated with the named object.
     * The schema describes rules regarding the structure of the namespace
     * and the attributes stored within it.  The schema
     * specifies what types of objects can be added to the directory and where
     * they can be added; what mandatory and optional attributes an object
     * can have. The range of support for schemas is directory-specific.
     *
     * <p> This method returns the root of the schema information tree
     * that is applicable to the named object. Several named objects
     * (or even an entire directory) might share the same schema.
     *
     * <p> Issues such as structure and contents of the schema tree,
     * permission to modify to the contents of the schema
     * tree, and the effect of such modifications on the directory
     * are dependent on the underlying directory.
     *
     * @param name
     *          the name of the object whose schema is to be retrieved
     * @return  the schema associated with the context; never null
     * @throws  OperationNotSupportedException if schema not supported
     * @throws  NamingException if a naming exception is encountered
     */
    public DirContext getSchema(Name name) throws NamingException;

    /**
     * Retrieves the schema associated with the named object.
     * See {@link #getSchema(Name)} for details.
     *
     * @param name
     *          the name of the object whose schema is to be retrieved
     * @return  the schema associated with the context; never null
     * @throws  OperationNotSupportedException if schema not supported
     * @throws  NamingException if a naming exception is encountered
     */
    public DirContext getSchema(String name) throws NamingException;

    /**
     * Retrieves a context containing the schema objects of the
     * named object's class definitions.
     *<p>
     * One category of information found in directory schemas is
     * <em>class definitions</em>.  An "object class" definition
     * specifies the object's <em>type</em> and what attributes (mandatory
     * and optional) the object must/can have. Note that the term
     * "object class" being referred to here is in the directory sense
     * rather than in the Java sense.
     * For example, if the named object is a directory object of
     * "Person" class, {@code getSchemaClassDefinition()} would return a
     * {@code DirContext} representing the (directory's) object class
     * definition of "Person".
     *<p>
     * The information that can be retrieved from an object class definition
     * is directory-dependent.
     *<p>
     * Prior to JNDI 1.2, this method
     * returned a single schema object representing the class definition of
     * the named object.
     * Since JNDI 1.2, this method returns a {@code DirContext} containing
     * all of the named object's class definitions.
     *
     * @param name
     *          the name of the object whose object class
     *          definition is to be retrieved
     * @return  the {@code DirContext} containing the named
     *          object's class definitions; never null
     *
     * @throws  OperationNotSupportedException if schema not supported
     * @throws  NamingException if a naming exception is encountered
     */
    public DirContext getSchemaClassDefinition(Name name)
            throws NamingException;

    /**
     * Retrieves a context containing the schema objects of the
     * named object's class definitions.
     * See {@link #getSchemaClassDefinition(Name)} for details.
     *
     * @param name
     *          the name of the object whose object class
     *          definition is to be retrieved
     * @return  the {@code DirContext} containing the named
     *          object's class definitions; never null
     *
     * @throws  OperationNotSupportedException if schema not supported
     * @throws  NamingException if a naming exception is encountered
     */
    public DirContext getSchemaClassDefinition(String name)
            throws NamingException;

// -------------------- search operations

    /**
     * Searches in a single context for objects that contain a
     * specified set of attributes, and retrieves selected attributes.
     * The search is performed using the default
     * <code>SearchControls</code> settings.
     * <p>
     * For an object to be selected, each attribute in
     * <code>matchingAttributes</code> must match some attribute of the
     * object.  If <code>matchingAttributes</code> is empty or
     * null, all objects in the target context are returned.
     *<p>
     * An attribute <em>A</em><sub>1</sub> in
     * <code>matchingAttributes</code> is considered to match an
     * attribute <em>A</em><sub>2</sub> of an object if
     * <em>A</em><sub>1</sub> and <em>A</em><sub>2</sub> have the same
     * identifier, and each value of <em>A</em><sub>1</sub> is equal
     * to some value of <em>A</em><sub>2</sub>.  This implies that the
     * order of values is not significant, and that
     * <em>A</em><sub>2</sub> may contain "extra" values not found in
     * <em>A</em><sub>1</sub> without affecting the comparison.  It
     * also implies that if <em>A</em><sub>1</sub> has no values, then
     * testing for a match is equivalent to testing for the presence
     * of an attribute <em>A</em><sub>2</sub> with the same
     * identifier.
     *<p>
     * The precise definition of "equality" used in comparing attribute values
     * is defined by the underlying directory service.  It might use the
     * <code>Object.equals</code> method, for example, or might use a schema
     * to specify a different equality operation.
     * For matching based on operations other than equality (such as
     * substring comparison) use the version of the <code>search</code>
     * method that takes a filter argument.
     * <p>
     * When changes are made to this {@code DirContext},
     * the effect on enumerations returned by prior calls to this method
     * is undefined.
     *<p>
     * If the object does not have the attribute
     * specified, the directory will ignore the nonexistent attribute
     * and return the requested attributes that the object does have.
     *<p>
     * A directory might return more attributes than was requested
     * (see <strong>Attribute Type Names</strong> in the class description),
     * but is not allowed to return arbitrary, unrelated attributes.
     *<p>
     * See also <strong>Operational Attributes</strong> in the class
     * description.
     *
     * @param name
     *          the name of the context to search
     * @param matchingAttributes
     *          the attributes to search for.  If empty or null,
     *          all objects in the target context are returned.
     * @param attributesToReturn
     *          the attributes to return.  null indicates that
     *          all attributes are to be returned;
     *          an empty array indicates that none are to be returned.
     * @return
     *          a non-null enumeration of {@code SearchResult} objects.
     *          Each {@code SearchResult} contains the attributes
     *          identified by <code>attributesToReturn</code>
     *          and the name of the corresponding object, named relative
     *          to the context named by <code>name</code>.
     * @throws  NamingException if a naming exception is encountered
     *
     * @see SearchControls
     * @see SearchResult
     * @see #search(Name, String, Object[], SearchControls)
     */
    public NamingEnumeration<SearchResult>
        search(Name name,
               Attributes matchingAttributes,
               String[] attributesToReturn)
        throws NamingException;

    /**
     * Searches in a single context for objects that contain a
     * specified set of attributes, and retrieves selected attributes.
     * See {@link #search(Name, Attributes, String[])} for details.
     *
     * @param name
     *          the name of the context to search
     * @param matchingAttributes
     *          the attributes to search for
     * @param attributesToReturn
     *          the attributes to return
     * @return  a non-null enumeration of {@code SearchResult} objects
     * @throws  NamingException if a naming exception is encountered
     */
    public NamingEnumeration<SearchResult>
        search(String name,
               Attributes matchingAttributes,
               String[] attributesToReturn)
        throws NamingException;

    /**
     * Searches in a single context for objects that contain a
     * specified set of attributes.
     * This method returns all the attributes of such objects.
     * It is equivalent to supplying null as
     * the {@code attributesToReturn} parameter to the method
     * <code>search(Name, Attributes, String[])</code>.
     * <br>
     * See {@link #search(Name, Attributes, String[])} for a full description.
     *
     * @param name
     *          the name of the context to search
     * @param matchingAttributes
     *          the attributes to search for
     * @return  an enumeration of {@code SearchResult} objects
     * @throws  NamingException if a naming exception is encountered
     *
     * @see #search(Name, Attributes, String[])
     */
    public NamingEnumeration<SearchResult>
        search(Name name, Attributes matchingAttributes)
        throws NamingException;

    /**
     * Searches in a single context for objects that contain a
     * specified set of attributes.
     * See {@link #search(Name, Attributes)} for details.
     *
     * @param name
     *          the name of the context to search
     * @param matchingAttributes
     *          the attributes to search for
     * @return  an enumeration of {@code SearchResult} objects
     * @throws  NamingException if a naming exception is encountered
     */
    public NamingEnumeration<SearchResult>
        search(String name, Attributes matchingAttributes)
        throws NamingException;

    /**
     * Searches in the named context or object for entries that satisfy the
     * given search filter.  Performs the search as specified by
     * the search controls.
     * <p>
     * The format and interpretation of <code>filter</code> follows RFC 2254
     * with the
     * following interpretations for <code>attr</code> and <code>value</code>
     * mentioned in the RFC.
     * <p>
     * <code>attr</code> is the attribute's identifier.
     * <p>
     * <code>value</code> is the string representation the attribute's value.
     * The translation of this string representation into the attribute's value
     * is directory-specific.
     * <p>
     * For the assertion "someCount=127", for example, <code>attr</code>
     * is "someCount" and <code>value</code> is "127".
     * The provider determines, based on the attribute ID ("someCount")
     * (and possibly its schema), that the attribute's value is an integer.
     * It then parses the string "127" appropriately.
     *<p>
     * Any non-ASCII characters in the filter string should be
     * represented by the appropriate Java (Unicode) characters, and
     * not encoded as UTF-8 octets.  Alternately, the
     * "backslash-hexcode" notation described in RFC 2254 may be used.
     *<p>
     * If the directory does not support a string representation of
     * some or all of its attributes, the form of <code>search</code> that
     * accepts filter arguments in the form of Objects can be used instead.
     * The service provider for such a directory would then translate
     * the filter arguments to its service-specific representation
     * for filter evaluation.
     * See <code>search(Name, String, Object[], SearchControls)</code>.
     * <p>
     * RFC 2254 defines certain operators for the filter, including substring
     * matches, equality, approximate match, greater than, less than.  These
     * operators are mapped to operators with corresponding semantics in the
     * underlying directory. For example, for the equals operator, suppose
     * the directory has a matching rule defining "equality" of the
     * attributes in the filter. This rule would be used for checking
     * equality of the attributes specified in the filter with the attributes
     * of objects in the directory. Similarly, if the directory has a
     * matching rule for ordering, this rule would be used for
     * making "greater than" and "less than" comparisons.
     *<p>
     * Not all of the operators defined in RFC 2254 are applicable to all
     * attributes.  When an operator is not applicable, the exception
     * <code>InvalidSearchFilterException</code> is thrown.
     * <p>
     * The result is returned in an enumeration of {@code SearchResult}s.
     * Each {@code SearchResult} contains the name of the object
     * and other information about the object (see SearchResult).
     * The name is either relative to the target context of the search
     * (which is named by the <code>name</code> parameter), or
     * it is a URL string. If the target context is included in
     * the enumeration (as is possible when
     * <code>cons</code> specifies a search scope of
     * <code>SearchControls.OBJECT_SCOPE</code> or
     * <code>SearchControls.SUBSTREE_SCOPE</code>), its name is the empty
     * string. The {@code SearchResult} may also contain attributes of the
     * matching object if the {@code cons} argument specified that attributes
     * be returned.
     *<p>
     * If the object does not have a requested attribute, that
     * nonexistent attribute will be ignored.  Those requested
     * attributes that the object does have will be returned.
     *<p>
     * A directory might return more attributes than were requested
     * (see <strong>Attribute Type Names</strong> in the class description)
     * but is not allowed to return arbitrary, unrelated attributes.
     *<p>
     * See also <strong>Operational Attributes</strong> in the class
     * description.
     *
     * @param name
     *          the name of the context or object to search
     * @param filter
     *          the filter expression to use for the search; may not be null
     * @param cons
     *          the search controls that control the search.  If null,
     *          the default search controls are used (equivalent
     *          to {@code (new SearchControls())}).
     * @return  an enumeration of {@code SearchResult}s of
     *          the objects that satisfy the filter; never null
     *
     * @throws  InvalidSearchFilterException if the search filter specified is
     *          not supported or understood by the underlying directory
     * @throws  InvalidSearchControlsException if the search controls
     *          contain invalid settings
     * @throws  NamingException if a naming exception is encountered
     *
     * @see #search(Name, String, Object[], SearchControls)
     * @see SearchControls
     * @see SearchResult
     */
    public NamingEnumeration<SearchResult>
        search(Name name,
               String filter,
               SearchControls cons)
        throws NamingException;

    /**
     * Searches in the named context or object for entries that satisfy the
     * given search filter.  Performs the search as specified by
     * the search controls.
     * See {@link #search(Name, String, SearchControls)} for details.
     *
     * @param name
     *          the name of the context or object to search
     * @param filter
     *          the filter expression to use for the search; may not be null
     * @param cons
     *          the search controls that control the search.  If null,
     *          the default search controls are used (equivalent
     *          to {@code (new SearchControls())}).
     *
     * @return  an enumeration of {@code SearchResult}s for
     *          the objects that satisfy the filter.
     * @throws  InvalidSearchFilterException if the search filter specified is
     *          not supported or understood by the underlying directory
     * @throws  InvalidSearchControlsException if the search controls
     *          contain invalid settings
     * @throws  NamingException if a naming exception is encountered
     */
    public NamingEnumeration<SearchResult>
        search(String name,
               String filter,
               SearchControls cons)
        throws NamingException;

    /**
     * Searches in the named context or object for entries that satisfy the
     * given search filter.  Performs the search as specified by
     * the search controls.
     *<p>
     * The interpretation of <code>filterExpr</code> is based on RFC
     * 2254.  It may additionally contain variables of the form
     * <code>{i}</code> -- where <code>i</code> is an integer -- that
     * refer to objects in the <code>filterArgs</code> array.  The
     * interpretation of <code>filterExpr</code> is otherwise
     * identical to that of the <code>filter</code> parameter of the
     * method <code>search(Name, String, SearchControls)</code>.
     *<p>
     * When a variable <code>{i}</code> appears in a search filter, it
     * indicates that the filter argument <code>filterArgs[i]</code>
     * is to be used in that place.  Such variables may be used
     * wherever an <em>attr</em>, <em>value</em>, or
     * <em>matchingrule</em> production appears in the filter grammar
     * of RFC 2254, section 4.  When a string-valued filter argument
     * is substituted for a variable, the filter is interpreted as if
     * the string were given in place of the variable, with any
     * characters having special significance within filters (such as
     * <code>'*'</code>) having been escaped according to the rules of
     * RFC 2254.
     *<p>
     * For directories that do not use a string representation for
     * some or all of their attributes, the filter argument
     * corresponding to an attribute value may be of a type other than
     * String.  Directories that support unstructured binary-valued
     * attributes, for example, should accept byte arrays as filter
     * arguments.  The interpretation (if any) of filter arguments of
     * any other type is determined by the service provider for that
     * directory, which maps the filter operations onto operations with
     * corresponding semantics in the underlying directory.
     *<p>
     * This method returns an enumeration of the results.
     * Each element in the enumeration contains the name of the object
     * and other information about the object (see <code>SearchResult</code>).
     * The name is either relative to the target context of the search
     * (which is named by the <code>name</code> parameter), or
     * it is a URL string. If the target context is included in
     * the enumeration (as is possible when
     * <code>cons</code> specifies a search scope of
     * <code>SearchControls.OBJECT_SCOPE</code> or
     * <code>SearchControls.SUBSTREE_SCOPE</code>),
     * its name is the empty string.
     *<p>
     * The {@code SearchResult} may also contain attributes of the matching
     * object if the {@code cons} argument specifies that attributes be
     * returned.
     *<p>
     * If the object does not have a requested attribute, that
     * nonexistent attribute will be ignored.  Those requested
     * attributes that the object does have will be returned.
     *<p>
     * A directory might return more attributes than were requested
     * (see <strong>Attribute Type Names</strong> in the class description)
     * but is not allowed to return arbitrary, unrelated attributes.
     *<p>
     * If a search filter with invalid variable substitutions is provided
     * to this method, the result is undefined.
     * When changes are made to this DirContext,
     * the effect on enumerations returned by prior calls to this method
     * is undefined.
     *<p>
     * See also <strong>Operational Attributes</strong> in the class
     * description.
     *
     * @param name
     *          the name of the context or object to search
     * @param filterExpr
     *          the filter expression to use for the search.
     *          The expression may contain variables of the
     *          form "<code>{i}</code>" where <code>i</code>
     *          is a nonnegative integer.  May not be null.
     * @param filterArgs
     *          the array of arguments to substitute for the variables
     *          in <code>filterExpr</code>.  The value of
     *          <code>filterArgs[i]</code> will replace each
     *          occurrence of "<code>{i}</code>".
     *          If null, equivalent to an empty array.
     * @param cons
     *          the search controls that control the search.  If null,
     *          the default search controls are used (equivalent
     *          to {@code (new SearchControls())}).
     * @return  an enumeration of {@code SearchResult}s of the objects
     *          that satisfy the filter; never null
     *
     * @throws  ArrayIndexOutOfBoundsException if {@code filterExpr} contains
     *          <code>{i}</code> expressions where <code>i</code> is outside
     *          the bounds of the array <code>filterArgs</code>
     * @throws  InvalidSearchControlsException if {@code cons} contains
     *          invalid settings
     * @throws  InvalidSearchFilterException if {@code filterExpr} with
     *          {@code filterArgs} represents an invalid search filter
     * @throws  NamingException if a naming exception is encountered
     *
     * @see #search(Name, Attributes, String[])
     * @see java.text.MessageFormat
     */
    public NamingEnumeration<SearchResult>
        search(Name name,
               String filterExpr,
               Object[] filterArgs,
               SearchControls cons)
        throws NamingException;

    /**
     * Searches in the named context or object for entries that satisfy the
     * given search filter.  Performs the search as specified by
     * the search controls.
     * See {@link #search(Name, String, Object[], SearchControls)} for details.
     *
     * @param name
     *          the name of the context or object to search
     * @param filterExpr
     *          the filter expression to use for the search.
     *          The expression may contain variables of the
     *          form "<code>{i}</code>" where <code>i</code>
     *          is a nonnegative integer.  May not be null.
     * @param filterArgs
     *          the array of arguments to substitute for the variables
     *          in <code>filterExpr</code>.  The value of
     *          <code>filterArgs[i]</code> will replace each
     *          occurrence of "<code>{i}</code>".
     *          If null, equivalent to an empty array.
     * @param cons
     *          the search controls that control the search.  If null,
     *          the default search controls are used (equivalent
     *          to {@code (new SearchControls())}).
     * @return  an enumeration of {@code SearchResult}s of the objects
     *          that satisfy the filter; never null
     *
     * @throws  ArrayIndexOutOfBoundsException if {@code filterExpr} contains
     *          <code>{i}</code> expressions where <code>i</code> is outside
     *          the bounds of the array <code>filterArgs</code>
     * @throws  InvalidSearchControlsException if {@code cons} contains
     *          invalid settings
     * @throws  InvalidSearchFilterException if {@code filterExpr} with
     *          {@code filterArgs} represents an invalid search filter
     * @throws  NamingException if a naming exception is encountered
     */
    public NamingEnumeration<SearchResult>
        search(String name,
               String filterExpr,
               Object[] filterArgs,
               SearchControls cons)
        throws NamingException;
}
