/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package nsk.share.jdi;

import java.io.PrintStream;
import java.lang.reflect.*;
import java.util.*;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import nsk.share.TestBug;

/*
 *  Class is used as base debugger in tests for following events and event requests:
 *  - MonitorContendedEnterRequest / MonitorContendedEnterEvent
 *  - MonitorContendedEnteredRequest / MonitorContendedEnteredEvent
 *  - MonitorWaitRequest / MonitorWaitEvent
 *  - MonitorWaitedRequest / MonitorWaitedEvent
 *
 * In all these tests similar scenario is used:
 *  - debugger VM forces debuggee VM to create number of objects which should generate events during test
 *  - if any event filters are used each generating event object is checked is this object accepted by all filters,
 *    if object was accepted it should save information about all generated events and this information is available for debugger
 * - debuggee performs event generation and stop at breakpoint
 * - debugger reads data saved by event generators and checks is only expected events was generated
 */
public class JDIEventsDebugger extends TestDebuggerType2 {
    // types of tested events
    static public enum EventType {
        MONITOR_CONTENTED_ENTER,
        MONITOR_CONTENTED_ENTERED,
        MONITOR_WAIT,
        MONITOR_WAITED
    }

    /*
     * Class contains information required for event testing
     */
    static class TestedEventData {
        // event class
        public Class<?> eventClass;

        // class representing event data on debuggee's side
        public Class<?> eventDataMirrorClass;

        // class representing event data on debugger's side
        public Class<?> eventDataClass;

        public TestedEventData(Class<?> eventClass, Class<?> eventDataMirrorClass, Class<?> eventDataClass) {
            this.eventClass = eventClass;
            this.eventDataMirrorClass = eventDataMirrorClass;
            this.eventDataClass = eventDataClass;
        }
    }

    static public Map<EventType, TestedEventData> testedEventData = new HashMap<EventType, TestedEventData>();

    static {
        testedEventData.put(EventType.MONITOR_CONTENTED_ENTER, new TestedEventData(MonitorContendedEnterEvent.class,
                DebuggeeEventData.DebugMonitorEnterEventData.class, DebuggerEventData.DebugMonitorEnterEventData.class));

        testedEventData.put(EventType.MONITOR_CONTENTED_ENTERED, new TestedEventData(MonitorContendedEnteredEvent.class,
                DebuggeeEventData.DebugMonitorEnteredEventData.class, DebuggerEventData.DebugMonitorEnteredEventData.class));

        testedEventData.put(EventType.MONITOR_WAIT, new TestedEventData(MonitorWaitEvent.class, DebuggeeEventData.DebugMonitorWaitEventData.class,
                DebuggerEventData.DebugMonitorWaitEventData.class));

        testedEventData.put(EventType.MONITOR_WAITED, new TestedEventData(MonitorWaitedEvent.class,
                DebuggeeEventData.DebugMonitorWaitedEventData.class, DebuggerEventData.DebugMonitorWaitedEventData.class));
    }

    static public TestedEventData[] eventDataByEventTypes(EventType[] eventTypes) {
        TestedEventData[] result = new TestedEventData[eventTypes.length];

        int i = 0;
        for (EventType eventType : eventTypes) {
            TestedEventData eventData = testedEventData.get(eventType);

            if (eventData == null)
                throw new TestBug("Unsupported event type: " + eventType);

            result[i++] = eventData;
        }

        return result;
    }

    /*
     * Dummy event listener, just accepts all events
     */
    public class DummyEventListener extends EventHandler.EventListener {
        private volatile boolean breakpointEventReceived;

        public boolean eventReceived(Event event) {
            if (event instanceof BreakpointEvent) {
                breakpointEventReceived = true;
                vm.resume();
            }

            return true;
        }

        public void waitBreakpoint() {
            while (!breakpointEventReceived)
                Thread.yield();
        }
    }

    /*
     * Parse common for event tests parameters
     */
    protected String[] doInit(String[] args, PrintStream out) {
        args = super.doInit(args, out);

        ArrayList<String> standardArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-allowExtraEvents")) {
                extraEventClasses = createEventClassArray(args[i + 1]);

                i++;
            } else if (args[i].equals("-allowMissedEvents")) {
                missedEventClasses = createEventClassArray(args[i + 1]);

                i++;
            } else
                standardArgs.add(args[i]);
        }

        return standardArgs.toArray(new String[standardArgs.size()]);
    }

    // can't control some kinds of events (events from system libraries) and
    // not all events should be saved for analysis
    // (should be implemented in subclasses)
    protected boolean shouldSaveEvent(Event event) {
        return true;
    }

    public Class<?> findEventDataClass(TestedEventData[] testedEventData, Event event) {
        for (TestedEventData eventData : testedEventData) {
            if (eventData.eventClass.isAssignableFrom(event.getClass()))
                return eventData.eventClass;
        }

        return null;
    }

    /*
     * This event listener stores received monitor events until BreakpointEvent
     * is not received, after getting of BreakpointEvent checks only expected
     * events were received
     */
    public class EventListener extends EventHandler.EventListener {

        private TestedEventData[] testedEventData;

        public EventListener(TestedEventData[] testedEventData) {
            this.testedEventData = testedEventData;
        }

        private boolean shouldHandleEvent(Event event) {
            return findEventDataClass(testedEventData, event) == null ? false : true;
        }

        volatile boolean breakpointWasReceived;

        // execution was interrupted because of timeout
        volatile boolean executionWasInterrupted;

        public boolean eventReceived(Event event) {
            if (shouldHandleEvent(event)) {
                if (shouldSaveEvent(event)) {

                    Class<?> eventClass;

                    eventClass = findEventDataClass(testedEventData, event);
                    List<Event> events = allReceivedEvents.get(eventClass);

                    if (events == null) {
                        events = new LinkedList<Event>();
                        allReceivedEvents.put(eventClass, events);
                    }

                    events.add(event);
                }

                return true;
            }
            // debuggee should stop at the end of test
            else if (event instanceof BreakpointEvent) {
                breakpointWasReceived = true;

                try {
                    // if execution was interrupted because of timeout don't check received
                    // events because it can consume too much time
                    if (!executionWasInterrupted) {
                        // get data from debuggee about all generated events
                        initExpectedEvents(testedEventData);

                        checkEvents();
                    } else
                        log.complain("WARNING: execution was interrupted because of timeout, test doesn't check received events");
                } catch (Throwable t) {
                    unexpectedException(t);
                }

                vm.resume();

                return true;
            }

            return false;
        }
    }

    protected Class<?> extraEventClasses[];

    protected Class<?> missedEventClasses[];

    /*
     * If test can't strictly control event generation it may allow generation
     * of extra events and unexpected events aren't treated as error
     * (subclasses should specify what kinds of extra events are allowed)
     */
    private Class<?>[] allowedExtraEvents() {
        return extraEventClasses;
    }

    /*
     * If test can't strictly control event generation case when debugger doesn't
     * receive expected event may be not treated as failure
     * (subclasses should specify what kinds of expected events can be not received)
     */
    private Class<?>[] allowedMissedEvents() {
        return missedEventClasses;
    }

    private boolean isExtraEventAllowed(Class<?> eventClass) {
        return checkEvent(eventClass, allowedExtraEvents());
    }

    private boolean isMissedEventAllowed(Class<?> eventClass) {
        return checkEvent(eventClass, allowedMissedEvents());
    }

    private boolean checkEvent(Class<?> eventClass, Class<?> classes[]) {
        if (classes == null)
            return false;

        for (Class<?> klass : classes) {
            if (klass.isAssignableFrom(eventClass))
                return true;
        }

        return false;
    }

    // flag is modified from the event listener thread
    private volatile boolean eventsNotGenerated;

    /*
     * Method returns true if test expects event generation, but events weren't
     * generated. If test can't strictly control event generation such case isn't
     * necessarily treated as test failure (sublasses of JDIEventsDebugger can
     * for example try to rerun test several times).
     */
    protected boolean eventsNotGenerated() {
        return eventsNotGenerated;
    }

    /*
     * Print debug information about expected and received events(this data
     * should be stored in lists 'allExpectedEvents' and 'allReceivedEvents')
     * and check that only expected events were received
     */
    private void checkEvents() {
        if (getAllExpectedEvents().size() > 0 && getAllReceivedEvents().size() == 0 && allowedMissedEvents() != null) {
            log.display("WARNING: didn't receive any event");
            eventsNotGenerated = true;
        }

        log.display("ALL RECEIVED EVENTS: ");
        for (Event event : getAllReceivedEvents())
            log.display("received event: " + eventToString(event));

        log.display("ALL EXPECTED EVENTS: ");
        for (DebuggerEventData.DebugEventData eventData : getAllExpectedEvents())
            log.display("expected event: " + eventData);

        // try to find received event in the list of expected events, if this event
        // was found remove data about events from both lists
        for (Class<?> eventClass : allReceivedEvents.keySet()) {
            List<Event> receivedEvents = allReceivedEvents.get(eventClass);
            List<DebuggerEventData.DebugEventData> expectedEvents = allExpectedEvents.get(eventClass);

            for (Iterator<Event> allReceivedEventsIterator = receivedEvents.iterator();
                allReceivedEventsIterator.hasNext();) {

                Event event = allReceivedEventsIterator.next();

                for (Iterator<DebuggerEventData.DebugEventData> allExpectedEventsIterator = expectedEvents.iterator();
                    allExpectedEventsIterator.hasNext();) {

                    DebuggerEventData.DebugEventData debugEventData = allExpectedEventsIterator.next();

                    if (debugEventData.shouldCheckEvent(event)) {
                        if (debugEventData.checkEvent(event)) {
                            allExpectedEventsIterator.remove();
                            allReceivedEventsIterator.remove();
                            break;
                        }
                    }
                }
            }
        }

        List<Event> receivedEventsLeft = getAllReceivedEvents();

        // check is all received events were found in expected
        if (receivedEventsLeft.size() > 0) {
            // if allowExtraEvents = true extra events are not treated as error
            for (Event event : receivedEventsLeft) {
                if (!isExtraEventAllowed(event.getClass())) {
                    setSuccess(false);
                    log.complain("Unexpected event " + eventToString(event));
                }
            }
        }

        List<DebuggerEventData.DebugEventData> expectedEventsLeft = getAllExpectedEvents();

        // check is all expected events were received
        if (expectedEventsLeft.size() > 0) {
            for (DebuggerEventData.DebugEventData eventData : expectedEventsLeft) {
                if (!isMissedEventAllowed(eventData.eventClass)) {
                    setSuccess(false);
                    log.complain("Expected event was not generated: " + eventData);
                }
            }
        }
    }

    private String eventToString(Event event) {
        try {
            if (event instanceof MonitorContendedEnterEvent)
                return event + ". Details(MonitorContendedEnterEvent):" + " Monitor: " + ((MonitorContendedEnterEvent) event).monitor() + " Thread: "
                        + ((MonitorContendedEnterEvent) event).thread();
            else if (event instanceof MonitorContendedEnteredEvent)
                return event + ". Details(MonitorContendedEnteredEvent):" + " Monitor: " + ((MonitorContendedEnteredEvent) event).monitor()
                        + " Thread: " + ((MonitorContendedEnteredEvent) event).thread();
            else if (event instanceof MonitorWaitEvent)
                return event + ". Details(MonitorWaitEvent):" + " Monitor: " + ((MonitorWaitEvent) event).monitor() + " Thread: "
                        + ((MonitorWaitEvent) event).thread() + " Timeout: " + ((MonitorWaitEvent) event).timeout();
            else if (event instanceof MonitorWaitedEvent)
                return event + ". Details(MonitorWaitedEvent):" + " Monitor: " + ((MonitorWaitedEvent) event).monitor() + " Thread: "
                        + ((MonitorWaitedEvent) event).thread() + " Timedout: " + ((MonitorWaitedEvent) event).timedout();

            return event.toString();
        }
        // this exception can occur when unexpected event was received
        catch (ObjectCollectedException e) {
            // allowExtraEvents=true extra events are not treated as error
            if (!isExtraEventAllowed(event.getClass())) {
                setSuccess(false);
                e.printStackTrace(log.getOutStream());
                log.complain("Unexpected ObjectCollectedException was caught, possible unexpected event was received");
            }

            return event.getClass().getName() + " [ Can't get full description, ObjectCollectedException was thrown ]";
        }
    }

    // events received during test execution are stored here
    private Map<Class<?>, List<Event>> allReceivedEvents = new HashMap<Class<?>, List<Event>>();

    private List<Event> getAllReceivedEvents() {
        List<Event> result = new LinkedList<Event>();

        for (Class<?> eventClass : allReceivedEvents.keySet()) {
            result.addAll(allReceivedEvents.get(eventClass));
        }

        return result;
    }

    protected Map<Class<?>, List<DebuggerEventData.DebugEventData>> allExpectedEvents = new HashMap<Class<?>, List<DebuggerEventData.DebugEventData>>();

    private List<DebuggerEventData.DebugEventData> getAllExpectedEvents() {
        List<DebuggerEventData.DebugEventData> result = new LinkedList<DebuggerEventData.DebugEventData>();

        for (Class<?> eventClass : allExpectedEvents.keySet()) {
            result.addAll(allExpectedEvents.get(eventClass));
        }

        return result;
    }

    // find in debuggee VM and add to the list 'allExpectedEvents' instances
    // of classes representing generated events
    protected void initExpectedEvents(TestedEventData testedEventData[]) {
        List<DebuggerEventData.DebugEventData> events;

        ReferenceType referenceType = debuggee.classByName(debuggeeClassNameWithoutArgs());

        ArrayReference generatedEvents = (ArrayReference) referenceType.getValue(referenceType.fieldByName("generatedEvents"));

        for (TestedEventData eventData : testedEventData) {
            events = new LinkedList<DebuggerEventData.DebugEventData>();
            allExpectedEvents.put(eventData.eventClass, events);
        }

        for (int i = 0; i < generatedEvents.length(); i++) {
            ObjectReference debuggeeMirror = (ObjectReference) generatedEvents.getValue(i);

            for (TestedEventData eventData : testedEventData) {

                if (debuggeeMirror.referenceType().name().equals(eventData.eventDataMirrorClass.getName())) {
                    events = allExpectedEvents.get(eventData.eventClass);

                    /*
                     * Use reflection to create object representing generated
                     * event Event data class should has constructor with single
                     * parameter of type com.sun.jdi.ObjectReference
                     */
                    Constructor<?> constructor;

                    try {
                        constructor = eventData.eventDataClass.getConstructor(new Class[] { ObjectReference.class });
                    } catch (NoSuchMethodException e) {
                        TestBug testBug = new TestBug(
                                "Class representing debug event data should implement constructor with single parameter of type com.sun.jdi.ObjectReference");
                        testBug.initCause(e);
                        throw testBug;
                    }

                    DebuggerEventData.DebugEventData expectedEvent;

                    try {
                        expectedEvent = (DebuggerEventData.DebugEventData) constructor.newInstance(new Object[] { debuggeeMirror });
                    } catch (Exception e) {
                        TestBug testBug = new TestBug("Error when create debug event data: " + e);
                        testBug.initCause(e);
                        throw testBug;
                    }
                    events.add(expectedEvent);
                }
            }
        }
    }

    private void printFiltersInfo() {
        if (eventFilters.size() > 0) {
            log.display("Use following filters: ");

            for (EventFilters.DebugEventFilter filter : eventFilters)
                log.display("" + filter);
        } else {
            log.display("Don't use event filters");
        }
    }

    // filters used in test
    protected List<EventFilters.DebugEventFilter> eventFilters = new LinkedList<EventFilters.DebugEventFilter>();

    // Check is object generating events matches all filters,
    // if object was accepted by all filters set this object's field
    // 'saveEventData' to 'true', otherwise to 'false',
    private void checkEventGenerator(ThreadReference eventThread, ObjectReference executor) {
        boolean acceptedByFilters = true;

        for (EventFilters.DebugEventFilter eventFilter : eventFilters) {
            if (!eventFilter.isObjectMatch(executor, eventThread)) {
                acceptedByFilters = false;
                break;
            }
        }

        try {
            executor.setValue(executor.referenceType().fieldByName("saveEventData"), vm.mirrorOf(acceptedByFilters));
        } catch (Exception e) {
            throw new TestBug("Unexpected exception when change object field in debugee VM: " + e, e);
        }
    }

    /*
     * Find all event generating threads in debuggee VM (instances of
     * nsk.share.jdi.MonitorEventsDebuggee$MonitorActionsThread) all these
     * threads have special field - 'executor', this is object which generates
     * events. If event generating thread and event generating object was
     * accepted by all filters generating object should store information about
     * all generated events and this information will be available for debugger
     */
    protected void initializeEventGenerators() {
        printFiltersInfo();

        List<ThreadReference> eventThreads = getEventThreads();

        for (ThreadReference eventThread : eventThreads) {
            ObjectReference executor = (ObjectReference) eventThread.getValue(eventThread.referenceType().fieldByName("executor"));
            checkEventGenerator(eventThread, executor);
        }

        // debuggee's main thread also can generate events, need to filter it in
        // the same way as other threads
        checkEventGenerator(debuggee.threadByName(JDIEventsDebuggee.MAIN_THREAD_NAME), findSingleObjectReference(debuggeeClassNameWithoutArgs()));
    }

    // find instances of nsk.share.jdi.MonitorEventsDebuggee$MonitorActionsThread
    protected List<ThreadReference> getEventThreads() {
        ReferenceType referenceType = debuggee.classByName(JDIEventsDebuggee.EventActionsThread.class.getName());
        List<ObjectReference> debuggeeEventThreads = referenceType.instances(0);

        List<ThreadReference> result = new LinkedList<ThreadReference>();
        for (ObjectReference threadReference : debuggeeEventThreads)
            result.add((ThreadReference) threadReference);

        return result;
    }

    // find instances of nsk.share.jdi.MonitorEventsDebuggee$MonitorActionsThread,
    // and get value of this object's field with name 'executor'
    protected List<ObjectReference> getEventObjects() {
        List<ObjectReference> eventObjects = new LinkedList<ObjectReference>();

        List<ThreadReference> eventThreads = getEventThreads();

        for (ThreadReference eventThread : eventThreads) {
            eventObjects.add((ObjectReference) eventThread.getValue(eventThread.referenceType().fieldByName("executor")));
        }

        return eventObjects;
    }

    // remove all filters, received and expected events
    private void clearTestData() {
        allExpectedEvents.clear();
        allReceivedEvents.clear();
        eventFilters.clear();
        eventsNotGenerated = false;
    }

    private boolean isEventSupported(EventType eventType) {
        switch (eventType) {
        case MONITOR_CONTENTED_ENTER:
        case MONITOR_CONTENTED_ENTERED:
        case MONITOR_WAIT:
        case MONITOR_WAITED:
            return vm.canRequestMonitorEvents();

        default:
            throw new TestBug("Invalid tested event type: " + eventType);
        }
    }

    // create instance of EventRequest depending on given eventType
    private EventRequest createTestRequest(EventType eventType) {
        switch (eventType) {
        case MONITOR_CONTENTED_ENTER:
            return debuggee.getEventRequestManager().createMonitorContendedEnterRequest();
        case MONITOR_CONTENTED_ENTERED:
            return debuggee.getEventRequestManager().createMonitorContendedEnteredRequest();
        case MONITOR_WAIT:
            return debuggee.getEventRequestManager().createMonitorWaitRequest();
        case MONITOR_WAITED:
            return debuggee.getEventRequestManager().createMonitorWaitedRequest();

        default:
            throw new TestBug("Invalid tested event type: " + eventType);
        }
    }

    // create command depending on given eventType
    private String createCommand(EventType eventTypes[], int eventsNumber) {
        String command = JDIEventsDebuggee.COMMAND_CREATE_ACTIONS_EXECUTORS + ":" + eventsNumber + ":";

        for (EventType eventType : eventTypes) {
            switch (eventType) {
            case MONITOR_CONTENTED_ENTER:
            case MONITOR_CONTENTED_ENTERED:
            case MONITOR_WAIT:
            case MONITOR_WAITED:
                command += " " + eventType.name();
                break;

            default:
                throw new TestBug("Invalid tested event type: " + eventType);
            }
        }

        return command;
    }

    // get list of event requests from EventRequestManager depending on the given eventType
    private List<?> getEventRequestsFromManager(EventType eventType) {
        switch (eventType) {
        case MONITOR_CONTENTED_ENTER:
            return debuggee.getEventRequestManager().monitorContendedEnterRequests();
        case MONITOR_CONTENTED_ENTERED:
            return debuggee.getEventRequestManager().monitorContendedEnteredRequests();
        case MONITOR_WAIT:
            return debuggee.getEventRequestManager().monitorWaitRequests();
        case MONITOR_WAITED:
            return debuggee.getEventRequestManager().monitorWaitedRequests();

        default:
            throw new TestBug("Invalid tested event type: " + eventType);
        }
    }

    protected EventHandler eventHandler;

    private EventListener eventListener;

    // perform event generation before test begins to load all using classes
    // and avoid unexpected events related to classloading
    protected void prepareDebuggee(EventType[] eventTypes) {
        initDefaultBreakpoint();

        eventHandler = new EventHandler(debuggee, log);
        eventHandler.startListening();

        // use event listener which just skip all received events
        DummyEventListener dummyEventListener = new DummyEventListener();
        eventHandler.addListener(dummyEventListener);

        EventRequest eventRequests[] = new EventRequest[eventTypes.length];

        for (int i = 0; i < eventRequests.length; i++) {
            eventRequests[i] = createTestRequest(eventTypes[i]);
            eventRequests[i].setSuspendPolicy(EventRequest.SUSPEND_NONE);
            eventRequests[i].enable();
        }

        // debuggee should create event generators
        pipe.println(createCommand(eventTypes, 1));

        if (!isDebuggeeReady())
            return;

        // start event generation
        pipe.println(JDIEventsDebuggee.COMMAND_START_EXECUTION);

        if (!isDebuggeeReady())
            return;

        for (int i = 0; i < eventRequests.length; i++)
            eventRequests[i].disable();

        dummyEventListener.waitBreakpoint();

        eventHandler.removeListener(dummyEventListener);

        pipe.println(JDIEventsDebuggee.COMMAND_WAIT_EXECUTION_COMPLETION);

        if (!isDebuggeeReady())
            return;

        eventListener = new EventListener(eventDataByEventTypes(eventTypes));
        eventHandler.addListener(eventListener);
    }

    /*
     * Method for stress testing, allows specify requests for several event
     * types, number of events which should be generated during test and number
     * of threads which simultaneously generate events
     */
    protected void stressTestTemplate(EventType[] eventTypes, int eventsNumber, int threadsNumber) {
        for (EventType eventType : eventTypes) {
            if (!isEventSupported(eventType)) {
                log.complain("Can't test event because of it isn't supported: " + eventType);
                return;
            }
        }

        // Used framework is intended for testing event filters and debuggee
        // creates 3 threads performing event generation and there is possibility
        // to filter events from some threads
        for (int i = 0; i < threadsNumber; i++) {
            pipe.println(createCommand(eventTypes, eventsNumber));

            if (!isDebuggeeReady())
                return;
        }

        // clear data(if this method is executed several times)
        clearTestData();

        initializeEventGenerators();

        EventRequest eventRequests[] = new EventRequest[eventTypes.length];

        // create event requests
        for (int i = 0; i < eventTypes.length; i++) {
            eventRequests[i] = createTestRequest(eventTypes[i]);
            eventRequests[i].setSuspendPolicy(EventRequest.SUSPEND_NONE);
            eventRequests[i].enable();

            log.display("Use following event request: " + eventRequests[i]);
        }

        // stressTestTemplate can control only execution time, so ignore iteration count
        stresser.start(0);
        try {
            // start event generation
            pipe.println(JDIEventsDebuggee.COMMAND_START_EXECUTION);

            if (!isDebuggeeReady())
                return;

            // check is stressTime exceeded
            while (stresser.continueExecution()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    unexpectedException(e);
                }

                // periodically check is test completed
                if (eventListener.breakpointWasReceived)
                    break;
            }
        } finally {
            stresser.finish();
        }

        // debugger should interrupt test because of timeout
        if (!eventListener.breakpointWasReceived) {

            eventListener.executionWasInterrupted = true;

            log.complain("WARNING: time is exceeded, interrupt test");

            pipe.println(JDIEventsDebuggee.COMMAND_STOP_EXECUTION);

            if (!isDebuggeeReady())
                return;
        }

        pipe.println(JDIEventsDebuggee.COMMAND_WAIT_EXECUTION_COMPLETION);

        if (!isDebuggeeReady())
            return;

        for (int i = 0; i < eventRequests.length; i++)
            eventRequests[i].disable();
    }

    /*
     * Hook method for subclasses implementing tests against event filters (it is called from eventFilterTestTemplate)
     */
    protected EventFilters.DebugEventFilter[] createTestFilters(int testedFilterIndex) {
        throw new TestBug("Not implemented");
    }

    /*
     * Test event request with filter
     *
     * Also this method check following:
     * - InvalidRequestStateException is thrown if add filter for deleted or enabled request
     * - EventRequestManager.xxxRequests() returns created event request
     */
    protected void eventFilterTestTemplate(EventType eventType, int testedFilterIndex) {
        if (!isEventSupported(eventType)) {
            log.complain("Can't test event because of it isn't supported: " + eventType);
            return;
        }

        // debuggee create event generators
        pipe.println(createCommand(new EventType[] { eventType }, 1));

        if (!isDebuggeeReady())
            return;

        clearTestData();

        EventFilters.DebugEventFilter[] filters = createTestFilters(testedFilterIndex);

        for (EventFilters.DebugEventFilter filter : filters) {
            if (filter.isSupported(vm))
                eventFilters.add(filter);
            else {
                log.complain("Can't test filter because of it isn't supported: " + filter);
                return;
            }
        }

        initializeEventGenerators();

        // create event request
        EventRequest request = createTestRequest(eventType);
        request.setSuspendPolicy(EventRequest.SUSPEND_NONE);

        // try add filter to enabled request, expect
        // 'InvalidRequestStateException'
        request.enable();
        try {
            for (EventFilters.DebugEventFilter filter : filters)
                filter.addFilter(request);

            setSuccess(false);
            log.complain("Expected 'InvalidRequestStateException' was not thrown");
        } catch (InvalidRequestStateException e) {
            // expected exception
        } catch (Throwable e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());
        }

        // add event filter
        request.disable();

        for (EventFilters.DebugEventFilter filter : filters)
            addFilter(filter, request);

        request.enable();

        log.display("Use following event request: " + request);

        // start event generation
        pipe.println(JDIEventsDebuggee.COMMAND_START_EXECUTION);

        if (!isDebuggeeReady())
            return;

        // wait execution completion
        pipe.println(JDIEventsDebuggee.COMMAND_WAIT_EXECUTION_COMPLETION);

        if (!isDebuggeeReady())
            return;

        // check that method EventRequestManager.xxxRequests() return created
        // request
        if (!getEventRequestsFromManager(eventType).contains(request)) {
            setSuccess(false);
            log.complain("EventRequestManager doesn't return request: " + request);
        }

        // delete event request
        debuggee.getEventRequestManager().deleteEventRequest(request);

        // try add filter to removed request, expect
        // 'InvalidRequestStateException'
        try {
            for (EventFilters.DebugEventFilter filter : filters)
                filter.addFilter(request);
            setSuccess(false);
            log.complain("Expected 'InvalidRequestStateException' was not thrown");
        } catch (InvalidRequestStateException e) {
            // expected exception
        } catch (Throwable t) {
            unexpectedException(t);
        }
    }

    private void addFilter(EventFilters.DebugEventFilter filter, EventRequest request) {
        try {
            filter.addFilter(request);
        } catch (Throwable t) {
            unexpectedException(t);
        }
    }

    // used to parse parameters -allowExtraEvents and -allowMissedEvents
    private Class<?>[] createEventClassArray(String string) {
        String eventTypesNames[] = string.split(":");
        EventType eventTypes[] = new EventType[eventTypesNames.length];
        try {
            for (int i = 0; i < eventTypesNames.length; i++) {
                eventTypes[i] = EventType.valueOf(eventTypesNames[i]);
            }
        } catch (IllegalArgumentException e) {
            throw new TestBug("Invalid event type", e);
        }

        if (eventTypesNames.length == 0)
            throw new TestBug("Event types weren't specified");

        Class<?>[] result = new Class[eventTypesNames.length];

        for (int i = 0; i < result.length; i++)
            result[i] = testedEventData.get(eventTypes[i]).eventClass;

        return result;
    }

}
