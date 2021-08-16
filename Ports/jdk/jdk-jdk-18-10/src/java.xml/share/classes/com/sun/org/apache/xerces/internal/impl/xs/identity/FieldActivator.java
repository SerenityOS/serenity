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

package com.sun.org.apache.xerces.internal.impl.xs.identity;


/**
 * Interface for a field activator. The field activator is responsible
 * for activating fields within a specific scope; the caller merely
 * requests the fields to be activated.
 *
 * @xerces.internal
 *
 * @author Andy Clark, IBM
 *
 */
public interface FieldActivator {

    //
    // FieldActivator methods
    //

    /**
     * Start the value scope for the specified identity constraint. This
     * method is called when the selector matches in order to initialize
     * the value store.
     *
     * @param identityConstraint The identity constraint.
     * @param initialDepth  the depth at which the selector began matching
     */
    public void startValueScopeFor(IdentityConstraint identityConstraint,
            int initialDepth);

    /**
     * Request to activate the specified field. This method returns the
     * matcher for the field.
     *
     * @param field The field to activate.
     * @param initialDepth the 0-indexed depth in the instance document at which the Selector began to match.
     */
    public XPathMatcher activateField(Field field, int initialDepth);

    /**
     * Ends the value scope for the specified identity constraint.
     *
     * @param identityConstraint The identity constraint.
     * @param initialDepth  the 0-indexed depth where the Selector began to match.
     */
    public void endValueScopeFor(IdentityConstraint identityConstraint, int initialDepth);

} // interface FieldActivator
