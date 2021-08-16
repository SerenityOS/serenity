/*
 * Copyright (c) 2000, 2005, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @since 1.5
 */
public interface RelationSupportMBean
    extends Relation {

    /**
     * Returns an internal flag specifying if the object is still handled by
     * the Relation Service.
     *
     * @return a Boolean equal to {@link Boolean#TRUE} if the object
     * is still handled by the Relation Service and {@link
     * Boolean#FALSE} otherwise.
     */
    public Boolean isInRelationService();

    /**
     * <p>Specifies whether this relation is handled by the Relation
     * Service.</p>
     * <P>BEWARE, this method has to be exposed as the Relation Service will
     * access the relation through its management interface. It is RECOMMENDED
     * NOT to use this method. Using it does not affect the registration of the
     * relation object in the Relation Service, but will provide wrong
     * information about it!
     *
     * @param flag whether the relation is handled by the Relation Service.
     *
     * @exception IllegalArgumentException  if null parameter
     */
    public void setRelationServiceManagementFlag(Boolean flag)
        throws IllegalArgumentException;
}
