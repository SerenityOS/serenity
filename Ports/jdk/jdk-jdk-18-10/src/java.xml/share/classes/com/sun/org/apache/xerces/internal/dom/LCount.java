/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.org.apache.xerces.internal.dom;


/** Internal class LCount is used to track the number of
    listeners registered for a given event name, as an entry
    in a global Map. This should allow us to avoid generating,
    or discarding, events for which no listeners are registered.

    ***** There should undoubtedly be methods here to manipulate
    this table. At the moment that code's residing in NodeImpl.
    Move it when we have a chance to do so. Sorry; we were
    rushed.
*/
/**
 * @xerces.internal
 *
 */

class LCount
{
    static final java.util.Map<String, LCount> lCounts=new java.util.concurrent.ConcurrentHashMap<>();
    public int captures=0,bubbles=0,defaults, total=0;

    static LCount lookup(String evtName)
    {
        return lCounts.computeIfAbsent(evtName, (key) -> new LCount());
    }
} // class LCount
