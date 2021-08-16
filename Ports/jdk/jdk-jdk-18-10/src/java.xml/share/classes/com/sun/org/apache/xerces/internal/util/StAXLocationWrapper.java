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


import javax.xml.stream.Location;

import com.sun.org.apache.xerces.internal.xni.XMLLocator;

/**
 * <p>A light wrapper around a StAX location. This is useful
 * when bridging between StAX and XNI components.</p>
 *
 * @author Michael Glavassevich, IBM
 *
 */
public final class StAXLocationWrapper implements XMLLocator {

    private Location fLocation = null;

    public StAXLocationWrapper() {}

    public void setLocation(Location location) {
        fLocation = location;
    }

    public Location getLocation() {
        return fLocation;
    }

    /*
     * XMLLocator methods
     */

    public String getPublicId() {
        if (fLocation != null) {
            return fLocation.getPublicId();
        }
        return null;
    }

    public String getLiteralSystemId() {
        if (fLocation != null) {
            return fLocation.getSystemId();
        }
        return null;
    }

    public String getBaseSystemId() {
        return null;
    }

    public String getExpandedSystemId() {
        return getLiteralSystemId();
    }

    public int getLineNumber() {
        if (fLocation != null) {
            return fLocation.getLineNumber();
        }
        return -1;
    }

    public int getColumnNumber() {
        if (fLocation != null) {
            return fLocation.getColumnNumber();
        }
        return -1;
    }

    public int getCharacterOffset() {
        if (fLocation != null) {
            return fLocation.getCharacterOffset();
        }
        return -1;
    }

    public String getEncoding() {
        return null;
    }

    public String getXMLVersion() {
        return null;
    }

} // StAXLocationWrapper
