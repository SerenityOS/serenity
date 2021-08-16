/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javax.management.ObjectName;
import javax.management.InstanceNotFoundException;

import java.util.List;
import java.util.Map;

/**
 * The Relation Service is in charge of creating and deleting relation types
 * and relations, of handling the consistency and of providing query
 * mechanisms.
 *
 * @since 1.5
 */
public interface RelationServiceMBean {

    /**
     * Checks if the Relation Service is active.
     * Current condition is that the Relation Service must be registered in the
     * MBean Server
     *
     * @exception RelationServiceNotRegisteredException  if it is not
     * registered
     */
    public void isActive()
        throws RelationServiceNotRegisteredException;

    //
    // Accessors
    //

    /**
     * Returns the flag to indicate if when a notification is received for the
     * unregistration of an MBean referenced in a relation, if an immediate
     * "purge" of the relations (look for the relations no longer valid)
     * has to be performed, or if that will be performed only when the
     * purgeRelations method is explicitly called.
     * <P>true is immediate purge.
     *
     * @return true if purges are immediate.
     *
     * @see #setPurgeFlag
     */
    public boolean getPurgeFlag();

    /**
     * Sets the flag to indicate if when a notification is received for the
     * unregistration of an MBean referenced in a relation, if an immediate
     * "purge" of the relations (look for the relations no longer valid)
     * has to be performed, or if that will be performed only when the
     * purgeRelations method is explicitly called.
     * <P>true is immediate purge.
     *
     * @param purgeFlag  flag
     *
     * @see #getPurgeFlag
     */
    public void setPurgeFlag(boolean purgeFlag);

    //
    // Relation type handling
    //

    /**
     * Creates a relation type (RelationTypeSupport object) with given
     * role infos (provided by the RoleInfo objects), and adds it in the
     * Relation Service.
     *
     * @param relationTypeName  name of the relation type
     * @param roleInfoArray  array of role infos
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception InvalidRelationTypeException  If:
     * <P>- there is already a relation type with that name
     * <P>- the same name has been used for two different role infos
     * <P>- no role info provided
     * <P>- one null role info provided
     */
    public void createRelationType(String relationTypeName,
                                   RoleInfo[] roleInfoArray)
        throws IllegalArgumentException,
               InvalidRelationTypeException;

    /**
     * Adds given object as a relation type. The object is expected to
     * implement the RelationType interface.
     *
     * @param relationTypeObj  relation type object (implementing the
     * RelationType interface)
     *
     * @exception IllegalArgumentException  if null parameter or if
     * {@link RelationType#getRelationTypeName
     * relationTypeObj.getRelationTypeName()} returns null.
     * @exception InvalidRelationTypeException  if there is already a relation
     * type with that name
     */
    public void addRelationType(RelationType relationTypeObj)
        throws IllegalArgumentException,
               InvalidRelationTypeException;

    /**
     * Retrieves names of all known relation types.
     *
     * @return ArrayList of relation type names (Strings)
     */
    public List<String> getAllRelationTypeNames();

    /**
     * Retrieves list of role infos (RoleInfo objects) of a given relation
     * type.
     *
     * @param relationTypeName  name of relation type
     *
     * @return ArrayList of RoleInfo.
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationTypeNotFoundException  if there is no relation type
     * with that name.
     */
    public List<RoleInfo> getRoleInfos(String relationTypeName)
        throws IllegalArgumentException,
               RelationTypeNotFoundException;

    /**
     * Retrieves role info for given role of a given relation type.
     *
     * @param relationTypeName  name of relation type
     * @param roleInfoName  name of role
     *
     * @return RoleInfo object.
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationTypeNotFoundException  if the relation type is not
     * known in the Relation Service
     * @exception RoleInfoNotFoundException  if the role is not part of the
     * relation type.
     */
    public RoleInfo getRoleInfo(String relationTypeName,
                                String roleInfoName)
        throws IllegalArgumentException,
               RelationTypeNotFoundException,
               RoleInfoNotFoundException;

    /**
     * Removes given relation type from Relation Service.
     * <P>The relation objects of that type will be removed from the
     * Relation Service.
     *
     * @param relationTypeName  name of the relation type to be removed
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationTypeNotFoundException  If there is no relation type
     * with that name
     */
    public void removeRelationType(String relationTypeName)
        throws RelationServiceNotRegisteredException,
               IllegalArgumentException,
               RelationTypeNotFoundException;

    //
    // Relation handling
    //

    /**
     * Creates a simple relation (represented by a RelationSupport object) of
     * given relation type, and adds it in the Relation Service.
     * <P>Roles are initialized according to the role list provided in
     * parameter. The ones not initialized in this way are set to an empty
     * ArrayList of ObjectNames.
     * <P>A RelationNotification, with type RELATION_BASIC_CREATION, is sent.
     *
     * @param relationId  relation identifier, to identify uniquely the relation
     * inside the Relation Service
     * @param relationTypeName  name of the relation type (has to be created
     * in the Relation Service)
     * @param roleList  role list to initialize roles of the relation (can
     * be null).
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception IllegalArgumentException  if null parameter
     * @exception RoleNotFoundException  if a value is provided for a role
     * that does not exist in the relation type
     * @exception InvalidRelationIdException  if relation id already used
     * @exception RelationTypeNotFoundException  if relation type not known in
     * Relation Service
     * @exception InvalidRoleValueException if:
     * <P>- the same role name is used for two different roles
     * <P>- the number of referenced MBeans in given value is less than
     * expected minimum degree
     * <P>- the number of referenced MBeans in provided value exceeds expected
     * maximum degree
     * <P>- one referenced MBean in the value is not an Object of the MBean
     * class expected for that role
     * <P>- an MBean provided for that role does not exist
     */
    public void createRelation(String relationId,
                               String relationTypeName,
                               RoleList roleList)
        throws RelationServiceNotRegisteredException,
               IllegalArgumentException,
               RoleNotFoundException,
               InvalidRelationIdException,
               RelationTypeNotFoundException,
               InvalidRoleValueException;

    /**
     * Adds an MBean created by the user (and registered by him in the MBean
     * Server) as a relation in the Relation Service.
     * <P>To be added as a relation, the MBean must conform to the
     * following:
     * <P>- implement the Relation interface
     * <P>- have for RelationService ObjectName the ObjectName of current
     * Relation Service
     * <P>- have a relation id that is unique and unused in current Relation Service
     * <P>- have for relation type a relation type created in the Relation
     * Service
     * <P>- have roles conforming to the role info provided in the relation
     * type.
     *
     * @param relationObjectName  ObjectName of the relation MBean to be added.
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception NoSuchMethodException  If the MBean does not implement the
     * Relation interface
     * @exception InvalidRelationIdException  if:
     * <P>- no relation identifier in MBean
     * <P>- the relation identifier is already used in the Relation Service
     * @exception InstanceNotFoundException  if the MBean for given ObjectName
     * has not been registered
     * @exception InvalidRelationServiceException  if:
     * <P>- no Relation Service name in MBean
     * <P>- the Relation Service name in the MBean is not the one of the
     * current Relation Service
     * @exception RelationTypeNotFoundException  if:
     * <P>- no relation type name in MBean
     * <P>- the relation type name in MBean does not correspond to a relation
     * type created in the Relation Service
     * @exception InvalidRoleValueException  if:
     * <P>- the number of referenced MBeans in a role is less than
     * expected minimum degree
     * <P>- the number of referenced MBeans in a role exceeds expected
     * maximum degree
     * <P>- one referenced MBean in the value is not an Object of the MBean
     * class expected for that role
     * <P>- an MBean provided for a role does not exist
     * @exception RoleNotFoundException  if a value is provided for a role
     * that does not exist in the relation type
     */
    public void addRelation(ObjectName relationObjectName)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException,
               NoSuchMethodException,
               InvalidRelationIdException,
               InstanceNotFoundException,
               InvalidRelationServiceException,
               RelationTypeNotFoundException,
               RoleNotFoundException,
               InvalidRoleValueException;

    /**
     * If the relation is represented by an MBean (created by the user and
     * added as a relation in the Relation Service), returns the ObjectName of
     * the MBean.
     *
     * @param relationId  relation id identifying the relation
     *
     * @return ObjectName of the corresponding relation MBean, or null if
     * the relation is not an MBean.
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException there is no relation associated
     * to that id
     */
    public ObjectName isRelationMBean(String relationId)
        throws IllegalArgumentException,
               RelationNotFoundException;

    /**
     * Returns the relation id associated to the given ObjectName if the
     * MBean has been added as a relation in the Relation Service.
     *
     * @param objectName  ObjectName of supposed relation
     *
     * @return relation id (String) or null (if the ObjectName is not a
     * relation handled by the Relation Service)
     *
     * @exception IllegalArgumentException  if null parameter
     */
    public String isRelation(ObjectName objectName)
        throws IllegalArgumentException;

    /**
     * Checks if there is a relation identified in Relation Service with given
     * relation id.
     *
     * @param relationId  relation id identifying the relation
     *
     * @return boolean: true if there is a relation, false else
     *
     * @exception IllegalArgumentException  if null parameter
     */
    public Boolean hasRelation(String relationId)
        throws IllegalArgumentException;

    /**
     * Returns all the relation ids for all the relations handled by the
     * Relation Service.
     *
     * @return ArrayList of String
     */
    public List<String> getAllRelationIds();

    /**
     * Checks if given Role can be read in a relation of the given type.
     *
     * @param roleName  name of role to be checked
     * @param relationTypeName  name of the relation type
     *
     * @return an Integer wrapping an integer corresponding to possible
     * problems represented as constants in RoleUnresolved:
     * <P>- 0 if role can be read
     * <P>- integer corresponding to RoleStatus.NO_ROLE_WITH_NAME
     * <P>- integer corresponding to RoleStatus.ROLE_NOT_READABLE
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationTypeNotFoundException  if the relation type is not
     * known in the Relation Service
     */
    public Integer checkRoleReading(String roleName,
                                    String relationTypeName)
        throws IllegalArgumentException,
               RelationTypeNotFoundException;

    /**
     * Checks if given Role can be set in a relation of given type.
     *
     * @param role  role to be checked
     * @param relationTypeName  name of relation type
     * @param initFlag  flag to specify that the checking is done for the
     * initialization of a role, write access shall not be verified.
     *
     * @return an Integer wrapping an integer corresponding to possible
     * problems represented as constants in RoleUnresolved:
     * <P>- 0 if role can be set
     * <P>- integer corresponding to RoleStatus.NO_ROLE_WITH_NAME
     * <P>- integer for RoleStatus.ROLE_NOT_WRITABLE
     * <P>- integer for RoleStatus.LESS_THAN_MIN_ROLE_DEGREE
     * <P>- integer for RoleStatus.MORE_THAN_MAX_ROLE_DEGREE
     * <P>- integer for RoleStatus.REF_MBEAN_OF_INCORRECT_CLASS
     * <P>- integer for RoleStatus.REF_MBEAN_NOT_REGISTERED
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationTypeNotFoundException  if unknown relation type
     */
    public Integer checkRoleWriting(Role role,
                                    String relationTypeName,
                                    Boolean initFlag)
        throws IllegalArgumentException,
               RelationTypeNotFoundException;

    /**
     * Sends a notification (RelationNotification) for a relation creation.
     * The notification type is:
     * <P>- RelationNotification.RELATION_BASIC_CREATION if the relation is an
     * object internal to the Relation Service
     * <P>- RelationNotification.RELATION_MBEAN_CREATION if the relation is a
     * MBean added as a relation.
     * <P>The source object is the Relation Service itself.
     * <P>It is called in Relation Service createRelation() and
     * addRelation() methods.
     *
     * @param relationId  relation identifier of the updated relation
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if there is no relation for given
     * relation id
     */
    public void sendRelationCreationNotification(String relationId)
        throws IllegalArgumentException,
               RelationNotFoundException;

    /**
     * Sends a notification (RelationNotification) for a role update in the
     * given relation. The notification type is:
     * <P>- RelationNotification.RELATION_BASIC_UPDATE if the relation is an
     * object internal to the Relation Service
     * <P>- RelationNotification.RELATION_MBEAN_UPDATE if the relation is a
     * MBean added as a relation.
     * <P>The source object is the Relation Service itself.
     * <P>It is called in relation MBean setRole() (for given role) and
     * setRoles() (for each role) methods (implementation provided in
     * RelationSupport class).
     * <P>It is also called in Relation Service setRole() (for given role) and
     * setRoles() (for each role) methods.
     *
     * @param relationId  relation identifier of the updated relation
     * @param newRole  new role (name and new value)
     * @param oldRoleValue  old role value (List of ObjectName objects)
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if there is no relation for given
     * relation id
     */
    public void sendRoleUpdateNotification(String relationId,
                                           Role newRole,
                                           List<ObjectName> oldRoleValue)
        throws IllegalArgumentException,
               RelationNotFoundException;

    /**
     * Sends a notification (RelationNotification) for a relation removal.
     * The notification type is:
     * <P>- RelationNotification.RELATION_BASIC_REMOVAL if the relation is an
     * object internal to the Relation Service
     * <P>- RelationNotification.RELATION_MBEAN_REMOVAL if the relation is a
     * MBean added as a relation.
     * <P>The source object is the Relation Service itself.
     * <P>It is called in Relation Service removeRelation() method.
     *
     * @param relationId  relation identifier of the updated relation
     * @param unregMBeanList  List of ObjectNames of MBeans expected
     * to be unregistered due to relation removal (can be null)
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if there is no relation for given
     * relation id
     */
    public void sendRelationRemovalNotification(String relationId,
                                                List<ObjectName> unregMBeanList)
        throws IllegalArgumentException,
               RelationNotFoundException;

    /**
     * Handles update of the Relation Service role map for the update of given
     * role in given relation.
     * <P>It is called in relation MBean setRole() (for given role) and
     * setRoles() (for each role) methods (implementation provided in
     * RelationSupport class).
     * <P>It is also called in Relation Service setRole() (for given role) and
     * setRoles() (for each role) methods.
     * <P>To allow the Relation Service to maintain the consistency (in case
     * of MBean unregistration) and to be able to perform queries, this method
     * must be called when a role is updated.
     *
     * @param relationId  relation identifier of the updated relation
     * @param newRole  new role (name and new value)
     * @param oldRoleValue  old role value (List of ObjectName objects)
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception RelationNotFoundException  if no relation for given id.
     */
    public void updateRoleMap(String relationId,
                              Role newRole,
                              List<ObjectName> oldRoleValue)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException,
               RelationNotFoundException;

    /**
     * Removes given relation from the Relation Service.
     * <P>A RelationNotification notification is sent, its type being:
     * <P>- RelationNotification.RELATION_BASIC_REMOVAL if the relation was
     * only internal to the Relation Service
     * <P>- RelationNotification.RELATION_MBEAN_REMOVAL if the relation is
     * registered as an MBean.
     * <P>For MBeans referenced in such relation, nothing will be done,
     *
     * @param relationId  relation id of the relation to be removed
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if no relation corresponding to
     * given relation id
     */
    public void removeRelation(String relationId)
        throws RelationServiceNotRegisteredException,
               IllegalArgumentException,
               RelationNotFoundException;

    /**
     * Purges the relations.
     *
     * <P>Depending on the purgeFlag value, this method is either called
     * automatically when a notification is received for the unregistration of
     * an MBean referenced in a relation (if the flag is set to true), or not
     * (if the flag is set to false).
     * <P>In that case it is up to the user to call it to maintain the
     * consistency of the relations. To be kept in mind that if an MBean is
     * unregistered and the purge not done immediately, if the ObjectName is
     * reused and assigned to another MBean referenced in a relation, calling
     * manually this purgeRelations() method will cause trouble, as will
     * consider the ObjectName as corresponding to the unregistered MBean, not
     * seeing the new one.
     *
     * <P>The behavior depends on the cardinality of the role where the
     * unregistered MBean is referenced:
     * <P>- if removing one MBean reference in the role makes its number of
     * references less than the minimum degree, the relation has to be removed.
     * <P>- if the remaining number of references after removing the MBean
     * reference is still in the cardinality range, keep the relation and
     * update it calling its handleMBeanUnregistration() callback.
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server.
     */
    public void purgeRelations()
        throws RelationServiceNotRegisteredException;

    /**
     * Retrieves the relations where a given MBean is referenced.
     * <P>This corresponds to the CIM "References" and "ReferenceNames"
     * operations.
     *
     * @param mbeanName  ObjectName of MBean
     * @param relationTypeName  can be null; if specified, only the relations
     * of that type will be considered in the search. Else all relation types
     * are considered.
     * @param roleName  can be null; if specified, only the relations
     * where the MBean is referenced in that role will be returned. Else all
     * roles are considered.
     *
     * @return an HashMap, where the keys are the relation ids of the relations
     * where the MBean is referenced, and the value is, for each key,
     * an ArrayList of role names (as an MBean can be referenced in several
     * roles in the same relation).
     *
     * @exception IllegalArgumentException  if null parameter
     */
    public Map<String,List<String>>
        findReferencingRelations(ObjectName mbeanName,
                                 String relationTypeName,
                                 String roleName)
            throws IllegalArgumentException;

    /**
     * Retrieves the MBeans associated to given one in a relation.
     * <P>This corresponds to CIM Associators and AssociatorNames operations.
     *
     * @param mbeanName  ObjectName of MBean
     * @param relationTypeName  can be null; if specified, only the relations
     * of that type will be considered in the search. Else all
     * relation types are considered.
     * @param roleName  can be null; if specified, only the relations
     * where the MBean is referenced in that role will be considered. Else all
     * roles are considered.
     *
     * @return an HashMap, where the keys are the ObjectNames of the MBeans
     * associated to given MBean, and the value is, for each key, an ArrayList
     * of the relation ids of the relations where the key MBean is
     * associated to given one (as they can be associated in several different
     * relations).
     *
     * @exception IllegalArgumentException  if null parameter
     */
    public Map<ObjectName,List<String>>
        findAssociatedMBeans(ObjectName mbeanName,
                             String relationTypeName,
                             String roleName)
            throws IllegalArgumentException;

    /**
     * Returns the relation ids for relations of the given type.
     *
     * @param relationTypeName  relation type name
     *
     * @return an ArrayList of relation ids.
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationTypeNotFoundException  if there is no relation type
     * with that name.
     */
    public List<String> findRelationsOfType(String relationTypeName)
        throws IllegalArgumentException,
               RelationTypeNotFoundException;

    /**
     * Retrieves role value for given role name in given relation.
     *
     * @param relationId  relation id
     * @param roleName  name of role
     *
     * @return the ArrayList of ObjectName objects being the role value
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if no relation with given id
     * @exception RoleNotFoundException  if:
     * <P>- there is no role with given name
     * <P>or
     * <P>- the role is not readable.
     *
     * @see #setRole
     */
    public List<ObjectName> getRole(String relationId,
                                    String roleName)
        throws RelationServiceNotRegisteredException,
               IllegalArgumentException,
               RelationNotFoundException,
               RoleNotFoundException;

    /**
     * Retrieves values of roles with given names in given relation.
     *
     * @param relationId  relation id
     * @param roleNameArray  array of names of roles to be retrieved
     *
     * @return a RoleResult object, including a RoleList (for roles
     * successfully retrieved) and a RoleUnresolvedList (for roles not
     * retrieved).
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if no relation with given id
     *
     * @see #setRoles
     */
    public RoleResult getRoles(String relationId,
                               String[] roleNameArray)
        throws RelationServiceNotRegisteredException,
               IllegalArgumentException,
               RelationNotFoundException;

    /**
     * Returns all roles present in the relation.
     *
     * @param relationId  relation id
     *
     * @return a RoleResult object, including a RoleList (for roles
     * successfully retrieved) and a RoleUnresolvedList (for roles not
     * readable).
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if no relation for given id
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     */
    public RoleResult getAllRoles(String relationId)
        throws IllegalArgumentException,
               RelationNotFoundException,
               RelationServiceNotRegisteredException;

    /**
     * Retrieves the number of MBeans currently referenced in the
     * given role.
     *
     * @param relationId  relation id
     * @param roleName  name of role
     *
     * @return the number of currently referenced MBeans in that role
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if no relation with given id
     * @exception RoleNotFoundException  if there is no role with given name
     */
    public Integer getRoleCardinality(String relationId,
                                      String roleName)
        throws IllegalArgumentException,
               RelationNotFoundException,
               RoleNotFoundException;

    /**
     * Sets the given role in given relation.
     * <P>Will check the role according to its corresponding role definition
     * provided in relation's relation type
     * <P>The Relation Service will keep track of the change to keep the
     * consistency of relations by handling referenced MBean deregistrations.
     *
     * @param relationId  relation id
     * @param role  role to be set (name and new value)
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if no relation with given id
     * @exception RoleNotFoundException  if:
     * <P>- internal relation
     * <P>and
     * <P>- the role does not exist or is not writable
     * @exception InvalidRoleValueException  if internal relation and value
     * provided for role is not valid:
     * <P>- the number of referenced MBeans in given value is less than
     * expected minimum degree
     * <P>or
     * <P>- the number of referenced MBeans in provided value exceeds expected
     * maximum degree
     * <P>or
     * <P>- one referenced MBean in the value is not an Object of the MBean
     * class expected for that role
     * <P>or
     * <P>- an MBean provided for that role does not exist
     * @exception RelationTypeNotFoundException  if unknown relation type
     *
     * @see #getRole
     */
    public void setRole(String relationId,
                        Role role)
        throws RelationServiceNotRegisteredException,
               IllegalArgumentException,
               RelationNotFoundException,
               RoleNotFoundException,
               InvalidRoleValueException,
               RelationTypeNotFoundException;

    /**
     * Sets the given roles in given relation.
     * <P>Will check the role according to its corresponding role definition
     * provided in relation's relation type
     * <P>The Relation Service keeps track of the changes to keep the
     * consistency of relations by handling referenced MBean deregistrations.
     *
     * @param relationId  relation id
     * @param roleList  list of roles to be set
     *
     * @return a RoleResult object, including a RoleList (for roles
     * successfully set) and a RoleUnresolvedList (for roles not
     * set).
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if no relation with given id
     *
     * @see #getRoles
     */
    public RoleResult setRoles(String relationId,
                               RoleList roleList)
        throws RelationServiceNotRegisteredException,
               IllegalArgumentException,
               RelationNotFoundException;

    /**
     * Retrieves MBeans referenced in the various roles of the relation.
     *
     * @param relationId  relation id
     *
     * @return a HashMap mapping:
     * <P> ObjectName {@literal ->} ArrayList of String (role
     * names)
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if no relation for given
     * relation id
     */
    public Map<ObjectName,List<String>> getReferencedMBeans(String relationId)
        throws IllegalArgumentException,
               RelationNotFoundException;

    /**
     * Returns name of associated relation type for given relation.
     *
     * @param relationId  relation id
     *
     * @return the name of the associated relation type.
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if no relation for given
     * relation id
     */
    public String getRelationTypeName(String relationId)
        throws IllegalArgumentException,
               RelationNotFoundException;
}
