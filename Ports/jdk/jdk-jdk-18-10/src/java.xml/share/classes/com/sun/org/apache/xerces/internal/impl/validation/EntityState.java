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

package com.sun.org.apache.xerces.internal.impl.validation;


/**
 * The entity state interface defines methods that must be implemented
 * by components that store information about entity declarations, as well as by
 * entity validator that will need to validate attributes of type entity.
 *
 * @xerces.internal
 *
 * @author Elena Litani, IBM
 */
public interface EntityState {
    /**
     * Query method to check if entity with this name was declared.
     *
     * @param name
     * @return true if name is a declared entity
     */
    public boolean isEntityDeclared (String name);

    /**
     * Query method to check if entity is unparsed.
     *
     * @param name
     * @return true if name is an unparsed entity
     */
    public boolean isEntityUnparsed (String name);
}
