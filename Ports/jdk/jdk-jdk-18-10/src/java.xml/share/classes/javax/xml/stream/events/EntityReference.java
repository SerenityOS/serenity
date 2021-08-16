/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.stream.events;
/**
 * An interface for handling Entity events.
 *
 * This event reports entities that have not been resolved
 * and reports their replacement text unprocessed (if
 * available).  This event will be reported if javax.xml.stream.isReplacingEntityReferences
 * is set to false.  If javax.xml.stream.isReplacingEntityReferences is set to true
 * entity references will be resolved transparently.
 *
 * Entities are handled in two possible ways:
 *
 * (1) If javax.xml.stream.isReplacingEntityReferences is set to true
 * all entity references are resolved and reported as markup transparently.
 * (2) If javax.xml.stream.isReplacingEntityReferences is set to false
 * Entity references are reported as an EntityReference Event.
 *
 * @version 1.0
 * @author Copyright (c) 2009 by Oracle Corporation. All Rights Reserved.
 * @since 1.6
 */
public interface EntityReference extends XMLEvent {

  /**
   * Return the declaration of this entity.
   * @return the declaration
   */
  EntityDeclaration getDeclaration();

  /**
   * The name of the entity
   * @return the entity's name, may not be null
   */
  String getName();
}
