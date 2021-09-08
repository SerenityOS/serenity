/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>

namespace Web::HTML {

EventLoop::EventLoop()
{
}

EventLoop::~EventLoop()
{
}

EventLoop& main_thread_event_loop()
{
    return static_cast<Bindings::WebEngineCustomData*>(Bindings::main_thread_vm().custom_data())->event_loop;
}

// https://html.spec.whatwg.org/multipage/webappapis.html#spin-the-event-loop
void EventLoop::spin_until([[maybe_unused]] Function<bool()> goal_condition)
{
    // FIXME: 1. Let task be the event loop's currently running task.

    // FIXME: 2. Let task source be task's source.

    // FIXME: 3. Let old stack be a copy of the JavaScript execution context stack.

    // FIXME: 4. Empty the JavaScript execution context stack.

    // FIXME: 5. Perform a microtask checkpoint.

    // FIXME: 6. In parallel:

    // FIXME:    1. Wait until the condition goal is met.

    // FIXME:    2. Queue a task on task source to:

    // FIXME:       1. Replace the JavaScript execution context stack with old stack.

    // FIXME:       2. Perform any steps that appear after this spin the event loop instance in the original algorithm.

    // FIXME: 7. Stop task, allowing whatever algorithm that invoked it to resume.
}

// https://html.spec.whatwg.org/multipage/webappapis.html#event-loop-processing-model
void EventLoop::process()
{
    // An event loop must continually run through the following steps for as long as it exists:

    // 1. Let taskQueue be one of the event loop's task queues, chosen in an implementation-defined manner, with the constraint that the chosen task queue must contain at least one runnable task. If there is no such task queue, then jump to the microtasks step below.
    auto& task_queue = m_task_queue;

    // 2. Let oldestTask be the first runnable task in taskQueue, and remove it from taskQueue.
    auto oldest_task = task_queue.take_first_runnable();

    // 3. Set the event loop's currently running task to oldestTask.
    m_currently_running_task = oldest_task.ptr();

    // FIXME: 4. Let taskStartTime be the current high resolution time.

    // 5. Perform oldestTask's steps.
    oldest_task->execute();

    // 6. Set the event loop's currently running task back to null.
    m_currently_running_task = nullptr;

    // FIXME: 7. Microtasks: Perform a microtask checkpoint.

    // 8. Let hasARenderingOpportunity be false.
    [[maybe_unused]] bool has_a_rendering_opportunity = false;

    // FIXME: 9. Let now be the current high resolution time. [HRT]

    // FIXME: 10. Report the task's duration by performing the following steps:

    // FIXME:     1. Let top-level browsing contexts be an empty set.

    // FIXME:     2. For each environment settings object settings of oldestTask's script evaluation environment settings object set, append setting's top-level browsing context to top-level browsing contexts.

    // FIXME:     3. Report long tasks, passing in taskStartTime, now (the end time of the task), top-level browsing contexts, and oldestTask.

    // FIXME: 11. Update the rendering: if this is a window event loop, then:

    // FIXME:     1. Let docs be all Document objects whose relevant agent's event loop is this event loop, sorted arbitrarily except that the following conditions must be met:
    //               - Any Document B whose browsing context's container document is A must be listed after A in the list.
    //               - If there are two documents A and B whose browsing contexts are both child browsing contexts whose container documents are another Document C, then the order of A and B in the list must match the shadow-including tree order of their respective browsing context containers in C's node tree.

    // FIXME:     2. Rendering opportunities: Remove from docs all Document objects whose browsing context do not have a rendering opportunity.

    // FIXME:     3. If docs is not empty, then set hasARenderingOpportunity to true.

    // FIXME:     4. Unnecessary rendering: Remove from docs all Document objects which meet both of the following conditions:
    //               - The user agent believes that updating the rendering of the Document's browsing context would have no visible effect, and
    //               - The Document's map of animation frame callbacks is empty.

    // FIXME:     5. Remove from docs all Document objects for which the user agent believes that it's preferrable to skip updating the rendering for other reasons.

    // FIXME:     6. For each fully active Document in docs, flush autofocus candidates for that Document if its browsing context is a top-level browsing context.

    // FIXME:     7. For each fully active Document in docs, run the resize steps for that Document, passing in now as the timestamp. [CSSOMVIEW]

    // FIXME:     8. For each fully active Document in docs, run the scroll steps for that Document, passing in now as the timestamp. [CSSOMVIEW]

    // FIXME:     9. For each fully active Document in docs, evaluate media queries and report changes for that Document, passing in now as the timestamp. [CSSOMVIEW]

    // FIXME:     10. For each fully active Document in docs, update animations and send events for that Document, passing in now as the timestamp. [WEBANIMATIONS]

    // FIXME:     11. For each fully active Document in docs, run the fullscreen steps for that Document, passing in now as the timestamp. [FULLSCREEN]

    // FIXME:     12. For each fully active Document in docs, if the user agent detects that the backing storage associated with a CanvasRenderingContext2D or an OffscreenCanvasRenderingContext2D, context, has been lost, then it must run the context lost steps for each such context:

    // FIXME:     13. For each fully active Document in docs, run the animation frame callbacks for that Document, passing in now as the timestamp.

    // FIXME:     14. For each fully active Document in docs, run the update intersection observations steps for that Document, passing in now as the timestamp. [INTERSECTIONOBSERVER]

    // FIXME:     15. Invoke the mark paint timing algorithm for each Document object in docs.

    // FIXME:     16. For each fully active Document in docs, update the rendering or user interface of that Document and its browsing context to reflect the current state.

    // FIXME: 12. If all of the following are true
    //            - this is a window event loop
    //            - there is no task in this event loop's task queues whose document is fully active
    //            - this event loop's microtask queue is empty
    //            - hasARenderingOpportunity is false
    // FIXME:         then for each Window object whose relevant agent's event loop is this event loop, run the start an idle period algorithm, passing the Window. [REQUESTIDLECALLBACK]

    // FIXME: 13. If this is a worker event loop, then:

    // FIXME:     1. If this event loop's agent's single realm's global object is a supported DedicatedWorkerGlobalScope and the user agent believes that it would benefit from having its rendering updated at this time, then:
    // FIXME:        1. Let now be the current high resolution time. [HRT]
    // FIXME:        2. Run the animation frame callbacks for that DedicatedWorkerGlobalScope, passing in now as the timestamp.
    // FIXME:        3. Update the rendering of that dedicated worker to reflect the current state.

    // FIXME:     2. If there are no tasks in the event loop's task queues and the WorkerGlobalScope object's closing flag is true, then destroy the event loop, aborting these steps, resuming the run a worker steps described in the Web workers section below.
}

}
