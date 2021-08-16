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

import java.util.List;
import java.util.Map;

import javax.management.ObjectName;

/**
 * This interface has to be implemented by any MBean class expected to
 * represent a relation managed using the Relation Service.
 * <P>Simple relations, i.e. having only roles, no properties or methods, can
 * be created directly by the Relation Service (represented as RelationSupport
 * objects, internally handled by the Relation Service).
 * <P>If the user wants to represent more complex relations, involving
 * properties and/or methods, he has to provide his own class implementing the
 * Relation interface. This can be achieved either by inheriting from
 * RelationSupport class, or by implementing the interface (fully or delegation to
 * a RelationSupport object member).
 * <P>Specifying such user relation class is to introduce properties and/or
 * methods. Those have to be exposed for remote management. So this means that
 * any user relation class must be a MBean class.
 *
 * @since 1.5
 */
public interface Relation {

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
               RelationServiceNotRegisteredException;

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
               RelationServiceNotRegisteredException;

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
               RoleNotFoundException;

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
        throws RelationServiceNotRegisteredException;

    /**
     * Returns all roles in the relation without checking read mode.
     *
     * @return a RoleList.
     */
    public RoleList retrieveAllRoles();

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
     * <P>- a MBean provided for that role does not exist.
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception RelationTypeNotFoundException  if the relation type has not
     * been declared in the Relation Service.
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
               RelationNotFoundException;

    /**
     * Sets the given roles.
     * <P>Will check the role according to its corresponding role definition
     * provided in relation's relation type
     * <P>Will send one notification (RelationNotification with type
     * RELATION_BASIC_UPDATE or RELATION_MBEAN_UPDATE, depending if the
     * relation is a MBean or not) per updated role.
     *
     * @param roleList  list of roles to be set
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
    public RoleResult setRoles(RoleList roleList)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException,
               RelationTypeNotFoundException,
               RelationNotFoundException;

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
               RelationNotFoundException;

    /**
     * Retrieves MBeans referenced in the various roles of the relation.
     *
     * @return a HashMap mapping:
     * <P> ObjectName {@literal ->} ArrayList of String (role names)
     */
    public Map<ObjectName,List<String>> getReferencedMBeans();

    /**
     * Returns name of associated relation type.
     *
     * @return the name of the relation type.
     */
    public String getRelationTypeName();

    /**
     * Returns ObjectName of the Relation Service handling the relation.
     *
     * @return the ObjectName of the Relation Service.
     */
    public ObjectName getRelationServiceName();

    /**
     * Returns relation identifier (used to uniquely identify the relation
     * inside the Relation Service).
     *
     * @return the relation id.
     */
    public String getRelationId();
}
