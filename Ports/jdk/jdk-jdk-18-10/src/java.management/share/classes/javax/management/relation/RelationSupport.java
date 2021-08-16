/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.relation;



import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

import java.util.concurrent.atomic.AtomicBoolean;
import static com.sun.jmx.defaults.JmxProperties.RELATION_LOGGER;
import static com.sun.jmx.mbeanserver.Util.cast;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * A RelationSupport object is used internally by the Relation Service to
 * represent simple relations (only roles, no properties or methods), with an
 * unlimited number of roles, of any relation type. As internal representation,
 * it is not exposed to the user.
 * <P>RelationSupport class conforms to the design patterns of standard MBean. So
 * the user can decide to instantiate a RelationSupport object himself as
 * a MBean (as it follows the MBean design patterns), to register it in the
 * MBean Server, and then to add it in the Relation Service.
 * <P>The user can also, when creating his own MBean relation class, have it
 * extending RelationSupport, to retrieve the implementations of required
 * interfaces (see below).
 * <P>It is also possible to have in a user relation MBean class a member
 * being a RelationSupport object, and to implement the required interfaces by
 * delegating all to this member.
 * <P> RelationSupport implements the Relation interface (to be handled by the
 * Relation Service).
 * <P>It implements also the MBeanRegistration interface to be able to retrieve
 * the MBean Server where it is registered (if registered as a MBean) to access
 * to its Relation Service.
 *
 * @since 1.5
 */
public class RelationSupport
    implements RelationSupportMBean, MBeanRegistration {

    //
    // Private members
    //

    // Relation identifier (expected to be unique in the Relation Service where
    // the RelationSupport object will be added)
    private String myRelId = null;

    // ObjectName of the Relation Service where the relation will be added
    // REQUIRED if the RelationSupport is created by the user to be registered as
    // a MBean, as it will have to access the Relation Service via the MBean
    // Server to perform the check regarding the relation type.
    // Is null if current object is directly created by the Relation Service,
    // as the object will directly access it.
    private ObjectName myRelServiceName = null;

    // Reference to the MBean Server where the Relation Service is
    // registered
    // REQUIRED if the RelationSupport is created by the user to be registered as
    // a MBean, as it will have to access the Relation Service via the MBean
    // Server to perform the check regarding the relation type.
    // If the Relationbase object is created by the Relation Service (use of
    // createRelation() method), this is null as not needed, direct access to
    // the Relation Service.
    // If the Relationbase object is created by the user and registered as a
    // MBean, this is set by the preRegister() method below.
    private MBeanServer myRelServiceMBeanServer = null;

    // Relation type name (must be known in the Relation Service where the
    // relation will be added)
    private String myRelTypeName = null;

    // Role map, mapping <role-name> -> <Role>
    // Initialized by role list in the constructor, then updated:
    // - if the relation is a MBean, via setRole() and setRoles() methods, or
    //   via Relation Service setRole() and setRoles() methods
    // - if the relation is internal to the Relation Service, via
    //   setRoleInt() and setRolesInt() methods.
    private final Map<String,Role> myRoleName2ValueMap = new HashMap<String,Role>();

    // Flag to indicate if the object has been added in the Relation Service
    private final AtomicBoolean myInRelServFlg = new AtomicBoolean();

    //
    // Constructors
    //

    /**
     * Creates a {@code RelationSupport} object.
     * <P>This constructor has to be used when the RelationSupport object will
     * be registered as a MBean by the user, or when creating a user relation
     * MBean whose class extends RelationSupport.
     * <P>Nothing is done at the Relation Service level, i.e.
     * the {@code RelationSupport} object is not added to the
     * {@code RelationService} and no checks are performed to
     * see if the provided values are correct.
     * The object is always created, EXCEPT if:
     * <P>- any of the required parameters is {@code null}.
     * <P>- the same name is used for two roles.
     * <P>To be handled as a relation, the {@code RelationSupport} object has
     * to be added to the Relation Service using the Relation Service method
     * addRelation().
     *
     * @param relationId  relation identifier, to identify the relation in the
     * Relation Service.
     * <P>Expected to be unique in the given Relation Service.
     * @param relationServiceName  ObjectName of the Relation Service where
     * the relation will be registered.
     * <P>This parameter is required as it is the Relation Service that is
     * aware of the definition of the relation type of the given relation,
     * so that will be able to check update operations (set).
     * @param relationTypeName  Name of relation type.
     * <P>Expected to have been created in the given Relation Service.
     * @param list  list of roles (Role objects) to initialize the
     * relation. Can be {@code null}.
     * <P>Expected to conform to relation info in associated relation type.
     *
     * @exception InvalidRoleValueException  if the same name is used for two
     * roles.
     * @exception IllegalArgumentException  if any of the required parameters
     * (relation id, relation service ObjectName, or relation type name) is
     * {@code null}.
     */
    public RelationSupport(String relationId,
                        ObjectName relationServiceName,
                        String relationTypeName,
                        RoleList list)
        throws InvalidRoleValueException,
               IllegalArgumentException {

        super();

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        // Can throw InvalidRoleValueException and IllegalArgumentException
        initMembers(relationId,
                    relationServiceName,
                    null,
                    relationTypeName,
                    list);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
    }

    /**
     * Creates a {@code RelationSupport} object.
     * <P>This constructor has to be used when the user relation MBean
     * implements the interfaces expected to be supported by a relation by
     * delegating to a RelationSupport object.
     * <P>This object needs to know the Relation Service expected to handle the
     * relation. So it has to know the MBean Server where the Relation Service
     * is registered.
     * <P>According to a limitation, a relation MBean must be registered in the
     * same MBean Server as the Relation Service expected to handle it. So the
     * user relation MBean has to be created and registered, and then the
     * wrapped RelationSupport object can be created within the identified MBean
     * Server.
     * <P>Nothing is done at the Relation Service level, i.e.
     * the {@code RelationSupport} object is not added to the
     * {@code RelationService} and no checks are performed to
     * see if the provided values are correct.
     * The object is always created, EXCEPT if:
     * <P>- any of the required parameters is {@code null}.
     * <P>- the same name is used for two roles.
     * <P>To be handled as a relation, the {@code RelationSupport} object has
     * to be added to the Relation Service using the Relation Service method
     * addRelation().
     *
     * @param relationId  relation identifier, to identify the relation in the
     * Relation Service.
     * <P>Expected to be unique in the given Relation Service.
     * @param relationServiceName  ObjectName of the Relation Service where
     * the relation will be registered.
     * <P>This parameter is required as it is the Relation Service that is
     * aware of the definition of the relation type of the given relation,
     * so that will be able to check update operations (set).
     * @param relationServiceMBeanServer  MBean Server where the wrapping MBean
     * is or will be registered.
     * <P>Expected to be the MBean Server where the Relation Service is or will
     * be registered.
     * @param relationTypeName  Name of relation type.
     * <P>Expected to have been created in the given Relation Service.
     * @param list  list of roles (Role objects) to initialize the
     * relation. Can be {@code null}.
     * <P>Expected to conform to relation info in associated relation type.
     *
     * @exception InvalidRoleValueException  if the same name is used for two
     * roles.
     * @exception IllegalArgumentException  if any of the required parameters
     * (relation id, relation service ObjectName, relation service MBeanServer,
     * or relation type name) is {@code null}.
     */
    public RelationSupport(String relationId,
                        ObjectName relationServiceName,
                        MBeanServer relationServiceMBeanServer,
                        String relationTypeName,
                        RoleList list)
        throws InvalidRoleValueException,
               IllegalArgumentException {

        super();

        if (relationServiceMBeanServer == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        // Can throw InvalidRoleValueException and
        // IllegalArgumentException
        initMembers(relationId,
                    relationServiceName,
                    relationServiceMBeanServer,
                    relationTypeName,
                    list);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
    }

    //
    // Relation Interface
    //

    /**
     * Retrieves role value for given role name.
     * <P>Checks if the role exists and is readable according to the relation
     * type.
     *
     * @param roleName  name of role
     *
     * @return the ArrayList of ObjectName objects being the role value
     *
     * @exception IllegalArgumentException  if null role name
     * @exception RoleNotFoundException  if:
     * <P>- there is no role with given name
     * <P>- the role is not readable.
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     *
     * @see #setRole
     */
    public List<ObjectName> getRole(String roleName)
        throws IllegalArgumentException,
               RoleNotFoundException,
               RelationServiceNotRegisteredException {

        if (roleName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", roleName);

        // Can throw RoleNotFoundException and
        // RelationServiceNotRegisteredException
        List<ObjectName> result = cast(
            getRoleInt(roleName, false, null, false));

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Retrieves values of roles with given names.
     * <P>Checks for each role if it exists and is readable according to the
     * relation type.
     *
     * @param roleNameArray  array of names of roles to be retrieved
     *
     * @return a RoleResult object, including a RoleList (for roles
     * successfully retrieved) and a RoleUnresolvedList (for roles not
     * retrieved).
     *
     * @exception IllegalArgumentException  if null role name
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     *
     * @see #setRoles
     */
    public RoleResult getRoles(String[] roleNameArray)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException {

        if (roleNameArray == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        // Can throw RelationServiceNotRegisteredException
        RoleResult result = getRolesInt(roleNameArray, false, null);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Returns all roles present in the relation.
     *
     * @return a RoleResult object, including a RoleList (for roles
     * successfully retrieved) and a RoleUnresolvedList (for roles not
     * readable).
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     */
    public RoleResult getAllRoles()
        throws RelationServiceNotRegisteredException {

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        RoleResult result = null;
        try {
            result = getAllRolesInt(false, null);
        } catch (IllegalArgumentException exc) {
            // OK : Invalid parameters, ignore...
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Returns all roles in the relation without checking read mode.
     *
     * @return a RoleList
     */
    public RoleList retrieveAllRoles() {

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        RoleList result;
        synchronized(myRoleName2ValueMap) {
            result =
                new RoleList(new ArrayList<Role>(myRoleName2ValueMap.values()));
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Returns the number of MBeans currently referenced in the given role.
     *
     * @param roleName  name of role
     *
     * @return the number of currently referenced MBeans in that role
     *
     * @exception IllegalArgumentException  if null role name
     * @exception RoleNotFoundException  if there is no role with given name
     */
    public Integer getRoleCardinality(String roleName)
        throws IllegalArgumentException,
               RoleNotFoundException {

        if (roleName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", roleName);

        // Try to retrieve the role
        Role role;
        synchronized(myRoleName2ValueMap) {
            // No null Role is allowed, so direct use of get()
            role = (myRoleName2ValueMap.get(roleName));
        }
        if (role == null) {
            int pbType = RoleStatus.NO_ROLE_WITH_NAME;
            // Will throw a RoleNotFoundException
            //
            // Will not throw InvalidRoleValueException, so catch it for the
            // compiler
            try {
                RelationService.throwRoleProblemException(pbType,
                                                          roleName);
            } catch (InvalidRoleValueException exc) {
                // OK : Do not throw InvalidRoleValueException as
                //      a RoleNotFoundException will be thrown.
            }
        }

        List<ObjectName> roleValue = role.getRoleValue();

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return roleValue.size();
    }

    /**
     * Sets the given role.
     * <P>Will check the role according to its corresponding role definition
     * provided in relation's relation type
     * <P>Will send a notification (RelationNotification with type
     * RELATION_BASIC_UPDATE or RELATION_MBEAN_UPDATE, depending if the
     * relation is a MBean or not).
     *
     * @param role  role to be set (name and new value)
     *
     * @exception IllegalArgumentException  if null role
     * @exception RoleNotFoundException  if there is no role with the supplied
     * role's name or if the role is not writable (no test on the write access
     * mode performed when initializing the role)
     * @exception InvalidRoleValueException  if value provided for
     * role is not valid, i.e.:
     * <P>- the number of referenced MBeans in given value is less than
     * expected minimum degree
     * <P>- the number of referenced MBeans in provided value exceeds expected
     * maximum degree
     * <P>- one referenced MBean in the value is not an Object of the MBean
     * class expected for that role
     * <P>- a MBean provided for that role does not exist
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception RelationTypeNotFoundException  if the relation type has not
     * been declared in the Relation Service
     * @exception RelationNotFoundException  if the relation has not been
     * added in the Relation Service.
     *
     * @see #getRole
     */
    public void setRole(Role role)
        throws IllegalArgumentException,
               RoleNotFoundException,
               RelationTypeNotFoundException,
               InvalidRoleValueException,
               RelationServiceNotRegisteredException,
               RelationNotFoundException {

        if (role == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", role);

        // Will return null :)
        Object result = setRoleInt(role, false, null, false);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    /**
     * Sets the given roles.
     * <P>Will check the role according to its corresponding role definition
     * provided in relation's relation type
     * <P>Will send one notification (RelationNotification with type
     * RELATION_BASIC_UPDATE or RELATION_MBEAN_UPDATE, depending if the
     * relation is a MBean or not) per updated role.
     *
     * @param list  list of roles to be set
     *
     * @return a RoleResult object, including a RoleList (for roles
     * successfully set) and a RoleUnresolvedList (for roles not
     * set).
     *
     * @exception IllegalArgumentException  if null role list
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception RelationTypeNotFoundException  if the relation type has not
     * been declared in the Relation Service.
     * @exception RelationNotFoundException  if the relation MBean has not been
     * added in the Relation Service.
     *
     * @see #getRoles
     */
    public RoleResult setRoles(RoleList list)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException,
               RelationTypeNotFoundException,
               RelationNotFoundException {

        if (list == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", list);

        RoleResult result = setRolesInt(list, false, null);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Callback used by the Relation Service when a MBean referenced in a role
     * is unregistered.
     * <P>The Relation Service will call this method to let the relation
     * take action to reflect the impact of such unregistration.
     * <P>BEWARE. the user is not expected to call this method.
     * <P>Current implementation is to set the role with its current value
     * (list of ObjectNames of referenced MBeans) without the unregistered
     * one.
     *
     * @param objectName  ObjectName of unregistered MBean
     * @param roleName  name of role where the MBean is referenced
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RoleNotFoundException  if role does not exist in the
     * relation or is not writable
     * @exception InvalidRoleValueException  if role value does not conform to
     * the associated role info (this will never happen when called from the
     * Relation Service)
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception RelationTypeNotFoundException  if the relation type has not
     * been declared in the Relation Service.
     * @exception RelationNotFoundException  if this method is called for a
     * relation MBean not added in the Relation Service.
     */
    public void handleMBeanUnregistration(ObjectName objectName,
                                          String roleName)
        throws IllegalArgumentException,
               RoleNotFoundException,
               InvalidRoleValueException,
               RelationServiceNotRegisteredException,
               RelationTypeNotFoundException,
               RelationNotFoundException {

        if (objectName == null || roleName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1}", objectName, roleName);

        // Can throw RoleNotFoundException, InvalidRoleValueException,
        // or RelationTypeNotFoundException
        handleMBeanUnregistrationInt(objectName,
                                     roleName,
                                     false,
                                     null);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    /**
     * Retrieves MBeans referenced in the various roles of the relation.
     *
     * @return a HashMap mapping:
     * <P> ObjectName {@literal ->} ArrayList of String (role names)
     */
    public Map<ObjectName,List<String>> getReferencedMBeans() {

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        Map<ObjectName,List<String>> refMBeanMap =
            new HashMap<ObjectName,List<String>>();

        synchronized(myRoleName2ValueMap) {

            for (Role currRole : myRoleName2ValueMap.values()) {

                String currRoleName = currRole.getRoleName();
                // Retrieves ObjectNames of MBeans referenced in current role
                List<ObjectName> currRefMBeanList = currRole.getRoleValue();

                for (ObjectName currRoleObjName : currRefMBeanList) {

                    // Sees if current MBean has been already referenced in
                    // roles already seen
                    List<String> mbeanRoleNameList =
                        refMBeanMap.get(currRoleObjName);

                    boolean newRefFlg = false;
                    if (mbeanRoleNameList == null) {
                        newRefFlg = true;
                        mbeanRoleNameList = new ArrayList<String>();
                    }
                    mbeanRoleNameList.add(currRoleName);
                    if (newRefFlg) {
                        refMBeanMap.put(currRoleObjName, mbeanRoleNameList);
                    }
                }
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return refMBeanMap;
    }

    /**
     * Returns name of associated relation type.
     */
    public String getRelationTypeName() {
        return myRelTypeName;
    }

    /**
     * Returns ObjectName of the Relation Service handling the relation.
     *
     * @return the ObjectName of the Relation Service.
     */
    public ObjectName getRelationServiceName() {
        return myRelServiceName;
    }

    /**
     * Returns relation identifier (used to uniquely identify the relation
     * inside the Relation Service).
     *
     * @return the relation id.
     */
    public String getRelationId() {
        return myRelId;
    }

    //
    // MBeanRegistration interface
    //

    // Pre-registration: retrieves the MBean Server (useful to access to the
    // Relation Service)
    // This is the way to retrieve the MBean Server when the relation object is
    // a MBean created by the user outside of the Relation Service.
    //
    // No exception thrown.
    public ObjectName preRegister(MBeanServer server,
                                  ObjectName name)
        throws Exception {

        myRelServiceMBeanServer = server;
        return name;
    }

    // Post-registration: does nothing
    public void postRegister(Boolean registrationDone) {
        return;
    }

    // Pre-unregistration: does nothing
    public void preDeregister()
        throws Exception {
        return;
    }

    // Post-unregistration: does nothing
    public void postDeregister() {
        return;
    }

    //
    // Others
    //

    /**
     * Returns an internal flag specifying if the object is still handled by
     * the Relation Service.
     */
    public Boolean isInRelationService() {
        return myInRelServFlg.get();
    }

    public void setRelationServiceManagementFlag(Boolean flag)
        throws IllegalArgumentException {

        if (flag == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }
        myInRelServFlg.set(flag);
    }

    //
    // Misc
    //

    // Gets the role with given name
    // Checks if the role exists and is readable according to the relation
    // type.
    //
    // This method is called in getRole() above.
    // It is also called in the Relation Service getRole() method.
    // It is also called in getRolesInt() below (used for getRoles() above
    // and for Relation Service getRoles() method).
    //
    // Depending on parameters reflecting its use (either in the scope of
    // getting a single role or of getting several roles), will return:
    // - in case of success:
    //   - for single role retrieval, the ArrayList of ObjectNames being the
    //     role value
    //   - for multi-role retrieval, the Role object itself
    // - in case of failure (except critical exceptions):
    //   - for single role retrieval, if role does not exist or is not
    //     readable, an RoleNotFoundException exception is raised
    //   - for multi-role retrieval, a RoleUnresolved object
    //
    // -param roleName  name of role to be retrieved
    // -param relationServCallFlg  true if call from the Relation Service; this
    //  will happen if the current RelationSupport object has been created by
    //  the Relation Service (via createRelation()) method, so direct access is
    //  possible.
    // -param relationServ  reference to Relation Service object, if object
    //  created by Relation Service.
    // -param multiRoleFlg  true if getting the role in the scope of a
    //  multiple retrieval.
    //
    // -return:
    //  - for single role retrieval (multiRoleFlg false):
    //    - ArrayList of ObjectName objects, value of role with given name, if
    //      the role can be retrieved
    //    - raise a RoleNotFoundException exception else
    //  - for multi-role retrieval (multiRoleFlg true):
    //    - the Role object for given role name if role can be retrieved
    //    - a RoleUnresolved object with problem.
    //
    // -exception IllegalArgumentException  if null parameter
    // -exception RoleNotFoundException  if multiRoleFlg is false and:
    //  - there is no role with given name
    //  or
    //  - the role is not readable.
    // -exception RelationServiceNotRegisteredException  if the Relation
    //  Service is not registered in the MBean Server
    Object getRoleInt(String roleName,
                      boolean relationServCallFlg,
                      RelationService relationServ,
                      boolean multiRoleFlg)
        throws IllegalArgumentException,
               RoleNotFoundException,
               RelationServiceNotRegisteredException {

        if (roleName == null ||
            (relationServCallFlg && relationServ == null)) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", roleName);

        int pbType = 0;

        Role role;
        synchronized(myRoleName2ValueMap) {
            // No null Role is allowed, so direct use of get()
            role = (myRoleName2ValueMap.get(roleName));
        }

        if (role == null) {
                pbType = RoleStatus.NO_ROLE_WITH_NAME;

        } else {
            // Checks if the role is readable
            Integer status;

            if (relationServCallFlg) {

                // Call from the Relation Service, so direct access to it,
                // avoiding MBean Server
                // Shall not throw a RelationTypeNotFoundException
                try {
                    status = relationServ.checkRoleReading(roleName,
                                                         myRelTypeName);
                } catch (RelationTypeNotFoundException exc) {
                    throw new RuntimeException(exc.getMessage());
                }

            } else {

                // Call from getRole() method above
                // So we have a MBean. We must access the Relation Service
                // via the MBean Server.
                Object[] params = new Object[2];
                params[0] = roleName;
                params[1] = myRelTypeName;
                String[] signature = new String[2];
                signature[0] = "java.lang.String";
                signature[1] = "java.lang.String";
                // Can throw InstanceNotFoundException if the Relation
                // Service is not registered (to be catched in any case and
                // transformed into RelationServiceNotRegisteredException).
                //
                // Shall not throw a MBeanException, or a ReflectionException
                // or an InstanceNotFoundException
                try {
                    status = (Integer)
                        (myRelServiceMBeanServer.invoke(myRelServiceName,
                                                        "checkRoleReading",
                                                        params,
                                                        signature));
                } catch (MBeanException exc1) {
                    throw new RuntimeException("incorrect relation type");
                } catch (ReflectionException exc2) {
                    throw new RuntimeException(exc2.getMessage());
                } catch (InstanceNotFoundException exc3) {
                    throw new RelationServiceNotRegisteredException(
                                                            exc3.getMessage());
                }
            }

            pbType = status.intValue();
        }

        Object result;

        if (pbType == 0) {
            // Role can be retrieved

            if (!(multiRoleFlg)) {
                // Single role retrieved: returns its value
                // Note: no need to test if role value (list) not null before
                //       cloning, null value not allowed, empty list if
                //       nothing.
                result = new ArrayList<ObjectName>(role.getRoleValue());

            } else {
                // Role retrieved during multi-role retrieval: returns the
                // role
                result = (Role)(role.clone());
            }

        } else {
            // Role not retrieved

            if (!(multiRoleFlg)) {
                // Problem when retrieving a simple role: either role not
                // found or not readable, so raises a RoleNotFoundException.
                try {
                    RelationService.throwRoleProblemException(pbType,
                                                              roleName);
                    // To keep compiler happy :)
                    return null;
                } catch (InvalidRoleValueException exc) {
                    throw new RuntimeException(exc.getMessage());
                }

            } else {
                // Problem when retrieving a role in a multi-role retrieval:
                // returns a RoleUnresolved object
                result = new RoleUnresolved(roleName, null, pbType);
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    // Gets the given roles
    // For each role, verifies if the role exists and is readable according to
    // the relation type.
    //
    // This method is called in getRoles() above and in Relation Service
    // getRoles() method.
    //
    // -param roleNameArray  array of names of roles to be retrieved
    // -param relationServCallFlg  true if call from the Relation Service; this
    //  will happen if the current RelationSupport object has been created by
    //  the Relation Service (via createRelation()) method, so direct access is
    //  possible.
    // -param relationServ  reference to Relation Service object, if object
    //  created by Relation Service.
    //
    // -return a RoleResult object
    //
    // -exception IllegalArgumentException  if null parameter
    // -exception RelationServiceNotRegisteredException  if the Relation
    //  Service is not registered in the MBean Server
    RoleResult getRolesInt(String[] roleNameArray,
                           boolean relationServCallFlg,
                           RelationService relationServ)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException {

        if (roleNameArray == null ||
            (relationServCallFlg && relationServ == null)) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        RoleList roleList = new RoleList();
        RoleUnresolvedList roleUnresList = new RoleUnresolvedList();

        for (int i = 0; i < roleNameArray.length; i++) {
            String currRoleName = roleNameArray[i];

            Object currResult;

            // Can throw RelationServiceNotRegisteredException
            //
            // RoleNotFoundException: not possible but catch it for compiler :)
            try {
                currResult = getRoleInt(currRoleName,
                                        relationServCallFlg,
                                        relationServ,
                                        true);

            } catch (RoleNotFoundException exc) {
                return null; // :)
            }

            if (currResult instanceof Role) {
                // Can throw IllegalArgumentException if role is null
                // (normally should not happen :(
                try {
                    roleList.add((Role)currResult);
                } catch (IllegalArgumentException exc) {
                    throw new RuntimeException(exc.getMessage());
                }

            } else if (currResult instanceof RoleUnresolved) {
                // Can throw IllegalArgumentException if role is null
                // (normally should not happen :(
                try {
                    roleUnresList.add((RoleUnresolved)currResult);
                } catch (IllegalArgumentException exc) {
                    throw new RuntimeException(exc.getMessage());
                }
            }
        }

        RoleResult result = new RoleResult(roleList, roleUnresList);
        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    // Returns all roles present in the relation
    //
    // -return a RoleResult object, including a RoleList (for roles
    //  successfully retrieved) and a RoleUnresolvedList (for roles not
    //  readable).
    //
    // -exception IllegalArgumentException if null parameter
    // -exception RelationServiceNotRegisteredException  if the Relation
    //  Service is not registered in the MBean Server
    //
    RoleResult getAllRolesInt(boolean relationServCallFlg,
                              RelationService relationServ)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException {

        if (relationServCallFlg && relationServ == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        List<String> roleNameList;
        synchronized(myRoleName2ValueMap) {
            roleNameList =
                new ArrayList<String>(myRoleName2ValueMap.keySet());
        }
        String[] roleNames = new String[roleNameList.size()];
        roleNameList.toArray(roleNames);

        RoleResult result = getRolesInt(roleNames,
                                        relationServCallFlg,
                                        relationServ);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    // Sets the role with given value
    //
    // This method is called in setRole() above.
    // It is also called by the Relation Service setRole() method.
    // It is also called in setRolesInt() method below (used in setRoles()
    // above and in RelationService setRoles() method).
    //
    // Will check the role according to its corresponding role definition
    // provided in relation's relation type
    // Will send a notification (RelationNotification with type
    // RELATION_BASIC_UPDATE or RELATION_MBEAN_UPDATE, depending if the
    // relation is a MBean or not) if not initialization of role.
    //
    // -param aRole  role to be set (name and new value)
    // -param relationServCallFlg  true if call from the Relation Service; this
    //  will happen if the current RelationSupport object has been created by
    //  the Relation Service (via createRelation()) method, so direct access is
    //  possible.
    // -param relationServ  reference to Relation Service object, if internal
    //  relation
    // -param multiRoleFlg  true if getting the role in the scope of a
    //  multiple retrieval.
    //
    // -return (except other "critical" exceptions):
    //  - for single role retrieval (multiRoleFlg false):
    //    - null if the role has been set
    //    - raise an InvalidRoleValueException
    // else
    //  - for multi-role retrieval (multiRoleFlg true):
    //    - the Role object for given role name if role has been set
    //    - a RoleUnresolved object with problem else.
    //
    // -exception IllegalArgumentException if null parameter
    // -exception RoleNotFoundException  if multiRoleFlg is false and:
    //  - internal relation and the role does not exist
    //  or
    //  - existing role (i.e. not initializing it) and the role is not
    //    writable.
    // -exception InvalidRoleValueException  ifmultiRoleFlg is false and
    //  value provided for:
    //   - the number of referenced MBeans in given value is less than
    //     expected minimum degree
    //   or
    //   - the number of referenced MBeans in provided value exceeds expected
    //     maximum degree
    //   or
    //   - one referenced MBean in the value is not an Object of the MBean
    //     class expected for that role
    //   or
    //   - a MBean provided for that role does not exist
    // -exception RelationServiceNotRegisteredException  if the Relation
    //  Service is not registered in the MBean Server
    // -exception RelationTypeNotFoundException  if relation type unknown
    // -exception RelationNotFoundException  if a relation MBean has not been
    //  added in the Relation Service
    Object setRoleInt(Role aRole,
                      boolean relationServCallFlg,
                      RelationService relationServ,
                      boolean multiRoleFlg)
        throws IllegalArgumentException,
               RoleNotFoundException,
               InvalidRoleValueException,
               RelationServiceNotRegisteredException,
               RelationTypeNotFoundException,
               RelationNotFoundException {

        if (aRole == null ||
            (relationServCallFlg && relationServ == null)) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2} {3}",
                            aRole, relationServCallFlg, relationServ,
                            multiRoleFlg);

        String roleName = aRole.getRoleName();
        int pbType = 0;

        // Checks if role exists in the relation
        // No error if the role does not exist in the relation, to be able to
        // handle initialization of role when creating the relation
        // (roles provided in the RoleList parameter are directly set but
        // roles automatically initialized are set using setRole())
        Role role;
        synchronized(myRoleName2ValueMap) {
            role = (myRoleName2ValueMap.get(roleName));
        }

        List<ObjectName> oldRoleValue;
        Boolean initFlg;

        if (role == null) {
            initFlg = true;
            oldRoleValue = new ArrayList<ObjectName>();

        } else {
            initFlg = false;
            oldRoleValue = role.getRoleValue();
        }

        // Checks if the role can be set: is writable (except if
        // initialization) and correct value
        try {
            Integer status;

            if (relationServCallFlg) {

                // Call from the Relation Service, so direct access to it,
                // avoiding MBean Server
                //
                // Shall not raise a RelationTypeNotFoundException
                status = relationServ.checkRoleWriting(aRole,
                                                     myRelTypeName,
                                                     initFlg);

            } else {

                // Call from setRole() method above
                // So we have a MBean. We must access the Relation Service
                // via the MBean Server.
                Object[] params = new Object[3];
                params[0] = aRole;
                params[1] = myRelTypeName;
                params[2] = initFlg;
                String[] signature = new String[3];
                signature[0] = "javax.management.relation.Role";
                signature[1] = "java.lang.String";
                signature[2] = "java.lang.Boolean";
                // Can throw InstanceNotFoundException if the Relation Service
                // is not registered (to be transformed into
                // RelationServiceNotRegisteredException in any case).
                //
                // Can throw a MBeanException wrapping a
                // RelationTypeNotFoundException:
                // throw wrapped exception.
                //
                // Shall not throw a ReflectionException
                status = (Integer)
                    (myRelServiceMBeanServer.invoke(myRelServiceName,
                                                    "checkRoleWriting",
                                                    params,
                                                    signature));
            }

            pbType = status.intValue();

        } catch (MBeanException exc2) {

            // Retrieves underlying exception
            Exception wrappedExc = exc2.getTargetException();
            if (wrappedExc instanceof RelationTypeNotFoundException) {
                throw ((RelationTypeNotFoundException)wrappedExc);

            } else {
                throw new RuntimeException(wrappedExc.getMessage());
            }

        } catch (ReflectionException exc3) {
            throw new RuntimeException(exc3.getMessage());

        } catch (RelationTypeNotFoundException exc4) {
            throw new RuntimeException(exc4.getMessage());

        } catch (InstanceNotFoundException exc5) {
            throw new RelationServiceNotRegisteredException(exc5.getMessage());
        }

        Object result = null;

        if (pbType == 0) {
            // Role can be set
            if (!(initFlg.booleanValue())) {

                // Not initializing the role
                // If role being initialized:
                // - do not send an update notification
                // - do not try to update internal map of Relation Service
                //   listing referenced MBeans, as role is initialized to an
                //   empty list

                // Sends a notification (RelationNotification)
                // Can throw a RelationNotFoundException
                sendRoleUpdateNotification(aRole,
                                           oldRoleValue,
                                           relationServCallFlg,
                                           relationServ);

                // Updates the role map of the Relation Service
                // Can throw RelationNotFoundException
                updateRelationServiceMap(aRole,
                                         oldRoleValue,
                                         relationServCallFlg,
                                         relationServ);

            }

            // Sets the role
            synchronized(myRoleName2ValueMap) {
                myRoleName2ValueMap.put(roleName,
                                        (Role)(aRole.clone()));
            }

            // Single role set: returns null: nothing to set in result

            if (multiRoleFlg) {
                // Multi-roles retrieval: returns the role
                result = aRole;
            }

        } else {

            // Role not set

            if (!(multiRoleFlg)) {
                // Problem when setting a simple role: either role not
                // found, not writable, or incorrect value:
                // raises appropriate exception, RoleNotFoundException or
                // InvalidRoleValueException
                RelationService.throwRoleProblemException(pbType,
                                                          roleName);
                // To keep compiler happy :)
                return null;

            } else {
                // Problem when retrieving a role in a multi-role retrieval:
                // returns a RoleUnresolved object
                result = new RoleUnresolved(roleName,
                                            aRole.getRoleValue(),
                                            pbType);
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    // Requires the Relation Service to send a notification
    // RelationNotification, with type being either:
    // - RelationNotification.RELATION_BASIC_UPDATE if the updated relation is
    //   a relation internal to the Relation Service
    // - RelationNotification.RELATION_MBEAN_UPDATE if the updated relation is
    //   a relation MBean.
    //
    // -param newRole  new role
    // -param oldRoleValue  old role value (ArrayList of ObjectNames)
    // -param relationServCallFlg  true if call from the Relation Service; this
    //  will happen if the current RelationSupport object has been created by
    //  the Relation Service (via createRelation()) method, so direct access is
    //  possible.
    // -param relationServ  reference to Relation Service object, if object
    //  created by Relation Service.
    //
    // -exception IllegalArgumentException  if null parameter provided
    // -exception RelationServiceNotRegisteredException  if the Relation
    //  Service is not registered in the MBean Server
    // -exception RelationNotFoundException if:
    //  - relation MBean
    //  and
    //  - it has not been added into the Relation Service
    private void sendRoleUpdateNotification(Role newRole,
                                            List<ObjectName> oldRoleValue,
                                            boolean relationServCallFlg,
                                            RelationService relationServ)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException,
               RelationNotFoundException {

        if (newRole == null ||
            oldRoleValue == null ||
            (relationServCallFlg && relationServ == null)) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2} {3}",
                            newRole, oldRoleValue, relationServCallFlg,
                            relationServ);

        if (relationServCallFlg) {
            // Direct call to the Relation Service
            // Shall not throw a RelationNotFoundException for an internal
            // relation
            try {
                relationServ.sendRoleUpdateNotification(myRelId,
                                                      newRole,
                                                      oldRoleValue);
            } catch (RelationNotFoundException exc) {
                throw new RuntimeException(exc.getMessage());
            }

        } else {

            Object[] params = new Object[3];
            params[0] = myRelId;
            params[1] = newRole;
            params[2] = oldRoleValue;
            String[] signature = new String[3];
            signature[0] = "java.lang.String";
            signature[1] = "javax.management.relation.Role";
            signature[2] = "java.util.List";

            // Can throw InstanceNotFoundException if the Relation Service
            // is not registered (to be transformed).
            //
            // Can throw a MBeanException wrapping a
            // RelationNotFoundException (to be raised in any case): wrapped
            // exception to be thrown
            //
            // Shall not throw a ReflectionException
            try {
                myRelServiceMBeanServer.invoke(myRelServiceName,
                                               "sendRoleUpdateNotification",
                                               params,
                                               signature);
            } catch (ReflectionException exc1) {
                throw new RuntimeException(exc1.getMessage());
            } catch (InstanceNotFoundException exc2) {
                throw new RelationServiceNotRegisteredException(
                                                            exc2.getMessage());
            } catch (MBeanException exc3) {
                Exception wrappedExc = exc3.getTargetException();
                if (wrappedExc instanceof RelationNotFoundException) {
                    throw ((RelationNotFoundException)wrappedExc);
                } else {
                    throw new RuntimeException(wrappedExc.getMessage());
                }
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    // Requires the Relation Service to update its internal map handling
    // MBeans referenced in relations.
    // The Relation Service will also update its recording as a listener to
    // be informed about unregistration of new referenced MBeans, and no longer
    // informed of MBeans no longer referenced.
    //
    // -param newRole  new role
    // -param oldRoleValue  old role value (ArrayList of ObjectNames)
    // -param relationServCallFlg  true if call from the Relation Service; this
    //  will happen if the current RelationSupport object has been created by
    //  the Relation Service (via createRelation()) method, so direct access is
    //  possible.
    // -param relationServ  reference to Relation Service object, if object
    //  created by Relation Service.
    //
    // -exception IllegalArgumentException  if null parameter
    // -exception RelationServiceNotRegisteredException  if the Relation
    //  Service is not registered in the MBean Server
    // -exception RelationNotFoundException if:
    //  - relation MBean
    //  and
    //  - the relation is not added in the Relation Service
    private void updateRelationServiceMap(Role newRole,
                                          List<ObjectName> oldRoleValue,
                                          boolean relationServCallFlg,
                                          RelationService relationServ)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException,
               RelationNotFoundException {

        if (newRole == null ||
            oldRoleValue == null ||
            (relationServCallFlg && relationServ == null)) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2} {3}",
                            newRole, oldRoleValue, relationServCallFlg,
                            relationServ);

        if (relationServCallFlg) {
            // Direct call to the Relation Service
            // Shall not throw a RelationNotFoundException
            try {
                relationServ.updateRoleMap(myRelId,
                                         newRole,
                                         oldRoleValue);
            } catch (RelationNotFoundException exc) {
                throw new RuntimeException(exc.getMessage());
            }

        } else {
            Object[] params = new Object[3];
            params[0] = myRelId;
            params[1] = newRole;
            params[2] = oldRoleValue;
            String[] signature = new String[3];
            signature[0] = "java.lang.String";
            signature[1] = "javax.management.relation.Role";
            signature[2] = "java.util.List";
            // Can throw InstanceNotFoundException if the Relation Service
            // is not registered (to be transformed).
            // Can throw a MBeanException wrapping a RelationNotFoundException:
            // wrapped exception to be thrown
            //
            // Shall not throw a ReflectionException
            try {
                myRelServiceMBeanServer.invoke(myRelServiceName,
                                               "updateRoleMap",
                                               params,
                                               signature);
            } catch (ReflectionException exc1) {
                throw new RuntimeException(exc1.getMessage());
            } catch (InstanceNotFoundException exc2) {
                throw new
                     RelationServiceNotRegisteredException(exc2.getMessage());
            } catch (MBeanException exc3) {
                Exception wrappedExc = exc3.getTargetException();
                if (wrappedExc instanceof RelationNotFoundException) {
                    throw ((RelationNotFoundException)wrappedExc);
                } else {
                    throw new RuntimeException(wrappedExc.getMessage());
                }
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    // Sets the given roles
    // For each role:
    // - will check the role according to its corresponding role definition
    //   provided in relation's relation type
    // - will send a notification (RelationNotification with type
    //   RELATION_BASIC_UPDATE or RELATION_MBEAN_UPDATE, depending if the
    //   relation is a MBean or not) for each updated role.
    //
    // This method is called in setRoles() above and in Relation Service
    // setRoles() method.
    //
    // -param list  list of roles to be set
    // -param relationServCallFlg  true if call from the Relation Service; this
    //  will happen if the current RelationSupport object has been created by
    //  the Relation Service (via createRelation()) method, so direct access is
    //  possible.
    // -param relationServ  reference to Relation Service object, if object
    //  created by Relation Service.
    //
    // -return a RoleResult object
    //
    // -exception IllegalArgumentException  if null parameter
    // -exception RelationServiceNotRegisteredException  if the Relation
    //  Service is not registered in the MBean Server
    // -exception RelationTypeNotFoundException if:
    //  - relation MBean
    //  and
    //  - unknown relation type
    // -exception RelationNotFoundException if:
    //  - relation MBean
    // and
    // - not added in the RS
    RoleResult setRolesInt(RoleList list,
                           boolean relationServCallFlg,
                           RelationService relationServ)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException,
               RelationTypeNotFoundException,
               RelationNotFoundException {

        if (list == null ||
            (relationServCallFlg && relationServ == null)) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2}",
                            list, relationServCallFlg, relationServ);

        RoleList roleList = new RoleList();
        RoleUnresolvedList roleUnresList = new RoleUnresolvedList();

        for (Role currRole : list.asList()) {

            Object currResult = null;
            // Can throw:
            // RelationServiceNotRegisteredException,
            // RelationTypeNotFoundException
            //
            // Will not throw, due to parameters, RoleNotFoundException or
            // InvalidRoleValueException, but catch them to keep compiler
            // happy
            try {
                currResult = setRoleInt(currRole,
                                        relationServCallFlg,
                                        relationServ,
                                        true);
            } catch (RoleNotFoundException exc1) {
                // OK : Do not throw a RoleNotFoundException.
            } catch (InvalidRoleValueException exc2) {
                // OK : Do not throw an InvalidRoleValueException.
            }

            if (currResult instanceof Role) {
                // Can throw IllegalArgumentException if role is null
                // (normally should not happen :(
                try {
                    roleList.add((Role)currResult);
                } catch (IllegalArgumentException exc) {
                    throw new RuntimeException(exc.getMessage());
                }

            } else if (currResult instanceof RoleUnresolved) {
                // Can throw IllegalArgumentException if role is null
                // (normally should not happen :(
                try {
                    roleUnresList.add((RoleUnresolved)currResult);
                } catch (IllegalArgumentException exc) {
                    throw new RuntimeException(exc.getMessage());
                }
            }
        }

        RoleResult result = new RoleResult(roleList, roleUnresList);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    // Initializes all members
    //
    // -param relationId  relation identifier, to identify the relation in the
    // Relation Service.
    // Expected to be unique in the given Relation Service.
    // -param relationServiceName  ObjectName of the Relation Service where
    // the relation will be registered.
    // It is required as this is the Relation Service that is aware of the
    // definition of the relation type of given relation, so that will be able
    // to check update operations (set). Direct access via the Relation
    // Service (RelationService.setRole()) do not need this information but
    // as any user relation is a MBean, setRole() is part of its management
    // interface and can be called directly on the user relation MBean. So the
    // user relation MBean must be aware of the Relation Service where it will
    // be added.
    // -param relationTypeName  Name of relation type.
    // Expected to have been created in given Relation Service.
    // -param list  list of roles (Role objects) to initialized the
    // relation. Can be null.
    // Expected to conform to relation info in associated relation type.
    //
    // -exception InvalidRoleValueException  if the same name is used for two
    //  roles.
    // -exception IllegalArgumentException  if a required value (Relation
    //  Service Object Name, etc.) is not provided as parameter.
    private void initMembers(String relationId,
                             ObjectName relationServiceName,
                             MBeanServer relationServiceMBeanServer,
                             String relationTypeName,
                             RoleList list)
        throws InvalidRoleValueException,
               IllegalArgumentException {

        if (relationId == null ||
            relationServiceName == null ||
            relationTypeName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2} {3} {4}",
                            relationId, relationServiceName,
                            relationServiceMBeanServer, relationTypeName, list);

        myRelId = relationId;
        myRelServiceName = relationServiceName;
        myRelServiceMBeanServer = relationServiceMBeanServer;
        myRelTypeName = relationTypeName;
        // Can throw InvalidRoleValueException
        initRoleMap(list);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    // Initialize the internal role map from given RoleList parameter
    //
    // -param list  role list. Can be null.
    //  As it is a RoleList object, it cannot include null (rejected).
    //
    // -exception InvalidRoleValueException  if the same role name is used for
    //  several roles.
    //
    private void initRoleMap(RoleList list)
        throws InvalidRoleValueException {

        if (list == null) {
            return;
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", list);

        synchronized(myRoleName2ValueMap) {

            for (Role currRole : list.asList()) {

                // No need to check if role is null, it is not allowed to store
                // a null role in a RoleList :)
                String currRoleName = currRole.getRoleName();

                if (myRoleName2ValueMap.containsKey(currRoleName)) {
                    // Role already provided in current list
                    StringBuilder excMsgStrB = new StringBuilder("Role name ");
                    excMsgStrB.append(currRoleName);
                    excMsgStrB.append(" used for two roles.");
                    throw new InvalidRoleValueException(excMsgStrB.toString());
                }

                myRoleName2ValueMap.put(currRoleName,
                                        (Role)(currRole.clone()));
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    // Callback used by the Relation Service when a MBean referenced in a role
    // is unregistered.
    // The Relation Service will call this method to let the relation
    // take action to reflect the impact of such unregistration.
    // Current implementation is to set the role with its current value
    // (list of ObjectNames of referenced MBeans) without the unregistered
    // one.
    //
    // -param objectName  ObjectName of unregistered MBean
    // -param roleName  name of role where the MBean is referenced
    // -param relationServCallFlg  true if call from the Relation Service; this
    //  will happen if the current RelationSupport object has been created by
    //  the Relation Service (via createRelation()) method, so direct access is
    //  possible.
    // -param relationServ  reference to Relation Service object, if internal
    //  relation
    //
    // -exception IllegalArgumentException if null parameter
    // -exception RoleNotFoundException  if:
    //  - the role does not exist
    //  or
    //  - role not writable.
    // -exception InvalidRoleValueException  if value provided for:
    //   - the number of referenced MBeans in given value is less than
    //     expected minimum degree
    //   or
    //   - the number of referenced MBeans in provided value exceeds expected
    //     maximum degree
    //   or
    //   - one referenced MBean in the value is not an Object of the MBean
    //     class expected for that role
    //   or
    //   - a MBean provided for that role does not exist
    // -exception RelationServiceNotRegisteredException  if the Relation
    //  Service is not registered in the MBean Server
    // -exception RelationTypeNotFoundException if unknown relation type
    // -exception RelationNotFoundException if current relation has not been
    //  added in the RS
    void handleMBeanUnregistrationInt(ObjectName objectName,
                                      String roleName,
                                      boolean relationServCallFlg,
                                      RelationService relationServ)
        throws IllegalArgumentException,
               RoleNotFoundException,
               InvalidRoleValueException,
               RelationServiceNotRegisteredException,
               RelationTypeNotFoundException,
               RelationNotFoundException {

        if (objectName == null ||
            roleName == null ||
            (relationServCallFlg && relationServ == null)) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2} {3}",
                            objectName, roleName, relationServCallFlg,
                            relationServ);

        // Retrieves current role value
        Role role;
        synchronized(myRoleName2ValueMap) {
            role = (myRoleName2ValueMap.get(roleName));
        }

        if (role == null) {
            StringBuilder excMsgStrB = new StringBuilder();
            String excMsg = "No role with name ";
            excMsgStrB.append(excMsg);
            excMsgStrB.append(roleName);
            throw new RoleNotFoundException(excMsgStrB.toString());
        }
        List<ObjectName> currRoleValue = role.getRoleValue();

        // Note: no need to test if list not null before cloning, null value
        //       not allowed for role value.
        List<ObjectName> newRoleValue = new ArrayList<ObjectName>(currRoleValue);
        newRoleValue.remove(objectName);
        Role newRole = new Role(roleName, newRoleValue);

        // Can throw InvalidRoleValueException,
        // RelationTypeNotFoundException
        // (RoleNotFoundException already detected)
        Object result =
            setRoleInt(newRole, relationServCallFlg, relationServ, false);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

}
