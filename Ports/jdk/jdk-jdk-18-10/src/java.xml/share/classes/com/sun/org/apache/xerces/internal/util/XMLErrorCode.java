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

package com.sun.org.apache.xerces.internal.util;

/**
 * <p>A structure that represents an error code, characterized by
 * a domain and a message key.</p>
 *
 * @author Naela Nissar, IBM
 *
 */
final class XMLErrorCode {

    //
    // Data
    //

    /** error domain **/
    private String fDomain;

    /** message key **/
    private String fKey;

    /**
     * <p>Constructs an XMLErrorCode with the given domain and key.</p>
     *
     * @param domain The error domain.
     * @param key The key of the error message.
     */
    public XMLErrorCode(String domain, String key) {
        fDomain = domain;
        fKey = key;
    }

    /**
     * <p>Convenience method to set the values of an XMLErrorCode.</p>
     *
     * @param domain The error domain.
     * @param key The key of the error message.
     */
    public void setValues(String domain, String key) {
        fDomain = domain;
        fKey = key;
    }

    /**
     * <p>Indicates whether some other object is equal to this XMLErrorCode.</p>
     *
     * @param obj the object with which to compare.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof XMLErrorCode))
            return false;
        XMLErrorCode err = (XMLErrorCode) obj;
        return (fDomain.equals(err.fDomain) && fKey.equals(err.fKey));
    }

    /**
     * <p>Returns a hash code value for this XMLErrorCode.</p>
     *
     * @return a hash code value for this XMLErrorCode.
     */
    public int hashCode() {
        return fDomain.hashCode() + fKey.hashCode();
    }
}
