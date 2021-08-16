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

package com.sun.org.apache.xerces.internal.dom.events;

import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventTarget;

/**
 * EventImpl is an implementation of the basic "generic" DOM Level 2 Event
 * object. It may be subclassed by more specialized event sets.
 * Note that in our implementation, events are re-dispatchable (dispatch
 * clears the stopPropagation and preventDefault flags before it starts);
 * I believe that is the DOM's intent but I don't see an explicit statement
 * to this effect.
 *
 * @xerces.internal
 *
 */
public class EventImpl implements Event
{
    public String type=null;
    public EventTarget target;
    public EventTarget currentTarget;
    public short eventPhase;
    public boolean initialized=false, bubbles=true, cancelable=false;
    public boolean stopPropagation=false, preventDefault=false;

    protected long timeStamp = System.currentTimeMillis();

    /** The DOM doesn't deal with constructors, so instead we have an
        initializer call to set most of the read-only fields. The
        others are set, and reset, by the event subsystem during dispatch.
        <p>
        Note that init() -- and the subclass-specific initWhatever() calls --
        may be reinvoked. At least one initialization is required; repeated
        initializations overwrite the event with new values of their
        parameters.
    */
    public void initEvent(String eventTypeArg, boolean canBubbleArg,
                        boolean cancelableArg)
    {
            type=eventTypeArg;
            bubbles=canBubbleArg;
            cancelable=cancelableArg;

            initialized=true;
    }

    /** @return true iff this Event is of a class and type which supports
        bubbling. In the generic case, this is True.
        */
    public boolean getBubbles()
    {
        return bubbles;
    }

    /** @return true iff this Event is of a class and type which (a) has a
        Default Behavior in this DOM, and (b)allows cancellation (blocking)
        of that behavior. In the generic case, this is False.
        */
    public boolean getCancelable()
    {
        return cancelable;
    }

    /** @return the Node (EventTarget) whose EventListeners are currently
        being processed. During capture and bubble phases, this may not be
        the target node. */
    public EventTarget getCurrentTarget()
    {
        return currentTarget;
    }

    /** @return the current processing phase for this event --
        CAPTURING_PHASE, AT_TARGET, BUBBLING_PHASE. (There may be
        an internal DEFAULT_PHASE as well, but the users won't see it.) */
    public short getEventPhase()
    {
        return eventPhase;
    }

    /** @return the EventTarget (Node) to which the event was originally
        dispatched.
        */
    public EventTarget getTarget()
    {
        return target;
    }

    /** @return event name as a string
    */
    public String getType()
    {
        return type;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    /** Causes exit from in-progress event dispatch before the next
        currentTarget is selected. Replaces the preventBubble() and
        preventCapture() methods which were present in early drafts;
        they may be reintroduced in future levels of the DOM. */
    public void stopPropagation()
    {
        stopPropagation=true;
    }

    /** Prevents any default processing built into the target node from
        occurring.
      */
    public void preventDefault()
    {
        preventDefault=true;
    }

}
