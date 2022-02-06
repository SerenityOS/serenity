/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibCore/Timer.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HighResolutionTime/Performance.h>

namespace Web::HTML {

EventLoop::EventLoop()
    : m_task_queue(*this)
    , m_microtask_queue(*this)
{
}

EventLoop::~EventLoop()
{
}

void EventLoop::schedule()
{
    if (!m_system_event_loop_timer) {
        m_system_event_loop_timer = Core::Timer::create_single_shot(0, [this] {
            process();
        });
    }

    if (!m_system_event_loop_timer->is_active())
        m_system_event_loop_timer->restart();
}

void EventLoop::set_vm(JS::VM& vm)
{
    VERIFY(!m_vm);
    m_vm = &vm;
}

EventLoop& main_thread_event_loop()
{
    return static_cast<Bindings::WebEngineCustomData*>(Bindings::main_thread_vm().custom_data())->event_loop;
}

// https://html.spec.whatwg.org/multipage/webappapis.html#spin-the-event-loop
void EventLoop::spin_until(Function<bool()> goal_condition)
{
    // FIXME: 1. Let task be the event loop's currently running task.

    // FIXME: 2. Let task source be task's source.

    // 3. Let old stack be a copy of the JavaScript execution context stack.
    // 4. Empty the JavaScript execution context stack.
    auto& vm = Bindings::main_thread_vm();
    vm.save_execution_context_stack();

    // 5. Perform a microtask checkpoint.
    perform_a_microtask_checkpoint();

    // 6. In parallel:
    // NOTE: We do these in reverse order here, but it shouldn't matter.

    //    2. Queue a task on task source to:
    //       1. Replace the JavaScript execution context stack with old stack.
    vm.restore_execution_context_stack();
    //       2. Perform any steps that appear after this spin the event loop instance in the original algorithm.
    //       NOTE: This is achieved by returning from the function.

    //    1. Wait until the condition goal is met.
    Core::EventLoop loop;
    loop.spin_until([&]() -> bool {
        if (goal_condition())
            return true;

        return goal_condition();
    });

    // 7. Stop task, allowing whatever algorithm that invoked it to resume.
    // NOTE: This is achieved by returning from the function.
}

// https://html.spec.whatwg.org/multipage/webappapis.html#event-loop-processing-model
void EventLoop::process()
{
    // An event loop must continually run through the following steps for as long as it exists:

    // 1. Let taskQueue be one of the event loop's task queues, chosen in an implementation-defined manner, with the constraint that the chosen task queue must contain at least one runnable task. If there is no such task queue, then jump to the microtasks step below.
    auto& task_queue = m_task_queue;

    if (auto oldest_task = task_queue.take_first_runnable()) {
        // 2. Let oldestTask be the first runnable task in taskQueue, and remove it from taskQueue.

        // 3. Set the event loop's currently running task to oldestTask.
        m_currently_running_task = oldest_task.ptr();

        // FIXME: 4. Let taskStartTime be the current high resolution time.

        // 5. Perform oldestTask's steps.
        oldest_task->execute();

        // 6. Set the event loop's currently running task back to null.
        m_currently_running_task = nullptr;
    }

    // 7. Microtasks: Perform a microtask checkpoint.
    perform_a_microtask_checkpoint();

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
    // FIXME: NOTE: The sort order specified above is missing here!
    NonnullRefPtrVector<DOM::Document> docs = documents_in_this_event_loop();

    auto for_each_fully_active_document_in_docs = [&](auto&& callback) {
        for (auto& document : docs) {
            if (document.is_fully_active())
                callback(document);
        }
    };

    // 2. Rendering opportunities: Remove from docs all Document objects whose browsing context do not have a rendering opportunity.
    docs.remove_all_matching([&](auto& document) {
        return document->browsing_context() && !document->browsing_context()->has_a_rendering_opportunity();
    });

    // 3. If docs is not empty, then set hasARenderingOpportunity to true.
    if (!docs.is_empty())
        has_a_rendering_opportunity = true;

    // FIXME:     4. Unnecessary rendering: Remove from docs all Document objects which meet both of the following conditions:
    //               - The user agent believes that updating the rendering of the Document's browsing context would have no visible effect, and
    //               - The Document's map of animation frame callbacks is empty.

    // FIXME:     5. Remove from docs all Document objects for which the user agent believes that it's preferrable to skip updating the rendering for other reasons.

    // FIXME:     6. For each fully active Document in docs, flush autofocus candidates for that Document if its browsing context is a top-level browsing context.

    // 7. For each fully active Document in docs, run the resize steps for that Document, passing in now as the timestamp. [CSSOMVIEW]
    for_each_fully_active_document_in_docs([&](DOM::Document& document) {
        document.run_the_resize_steps();
    });

    // FIXME:     8. For each fully active Document in docs, run the scroll steps for that Document, passing in now as the timestamp. [CSSOMVIEW]

    // 9. For each fully active Document in docs, evaluate media queries and report changes for that Document, passing in now as the timestamp. [CSSOMVIEW]
    for_each_fully_active_document_in_docs([&](DOM::Document& document) {
        document.evaluate_media_queries_and_report_changes();
    });

    // FIXME:     10. For each fully active Document in docs, update animations and send events for that Document, passing in now as the timestamp. [WEBANIMATIONS]

    // FIXME:     11. For each fully active Document in docs, run the fullscreen steps for that Document, passing in now as the timestamp. [FULLSCREEN]

    // FIXME:     12. For each fully active Document in docs, if the user agent detects that the backing storage associated with a CanvasRenderingContext2D or an OffscreenCanvasRenderingContext2D, context, has been lost, then it must run the context lost steps for each such context:

    // FIXME:     13. For each fully active Document in docs, run the animation frame callbacks for that Document, passing in now as the timestamp.
    for_each_fully_active_document_in_docs([&](DOM::Document& document) {
        run_animation_frame_callbacks(document, document.window().performance().now());
    });

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

    // If there are tasks in the queue, schedule a new round of processing. :^)
    if (m_task_queue.has_runnable_tasks() || !m_microtask_queue.is_empty())
        schedule();
}

// FIXME: This is here to paper over an issue in the HTML parser where it'll create new interpreters (and thus ESOs) on temporary documents created for innerHTML if it uses Document::realm() to get the global object.
//        Use queue_global_task instead.
void old_queue_global_task_with_document(HTML::Task::Source source, DOM::Document& document, Function<void()> steps)
{
    main_thread_event_loop().task_queue().add(HTML::Task::create(source, &document, move(steps)));
}

// https://html.spec.whatwg.org/multipage/webappapis.html#queue-a-global-task
void queue_global_task(HTML::Task::Source source, JS::GlobalObject& global_object, Function<void()> steps)
{
    // 1. Let event loop be global's relevant agent's event loop.
    auto& global_custom_data = verify_cast<Bindings::WebEngineCustomData>(*global_object.vm().custom_data());
    auto& event_loop = global_custom_data.event_loop;

    // 2. Let document be global's associated Document, if global is a Window object; otherwise null.
    DOM::Document* document { nullptr };
    if (is<Bindings::WindowObject>(global_object)) {
        auto& window_object = verify_cast<Bindings::WindowObject>(global_object);
        document = &window_object.impl().associated_document();
    }

    // 3. Queue a task given source, event loop, document, and steps.
    event_loop.task_queue().add(HTML::Task::create(source, document, move(steps)));
}

// https://html.spec.whatwg.org/#queue-a-microtask
void queue_a_microtask(DOM::Document* document, Function<void()> steps)
{
    // 1. If event loop was not given, set event loop to the implied event loop.
    auto& event_loop = HTML::main_thread_event_loop();

    // FIXME: 2. If document was not given, set document to the implied document.

    // 3. Let microtask be a new task.
    // 4. Set microtask's steps to steps.
    // 5. Set microtask's source to the microtask task source.
    // 6. Set microtask's document to document.
    auto microtask = HTML::Task::create(HTML::Task::Source::Microtask, document, move(steps));

    // FIXME: 7. Set microtask's script evaluation environment settings object set to an empty set.

    // 8. Enqueue microtask on event loop's microtask queue.
    event_loop.microtask_queue().enqueue(move(microtask));
}

// https://html.spec.whatwg.org/#perform-a-microtask-checkpoint
void EventLoop::perform_a_microtask_checkpoint()
{
    // 1. If the event loop's performing a microtask checkpoint is true, then return.
    if (m_performing_a_microtask_checkpoint)
        return;

    // 2. Set the event loop's performing a microtask checkpoint to true.
    m_performing_a_microtask_checkpoint = true;

    // 3. While the event loop's microtask queue is not empty:
    while (!m_microtask_queue.is_empty()) {
        // 1. Let oldestMicrotask be the result of dequeuing from the event loop's microtask queue.
        auto oldest_microtask = m_microtask_queue.dequeue();

        // 2. Set the event loop's currently running task to oldestMicrotask.
        m_currently_running_task = oldest_microtask;

        // 3. Run oldestMicrotask.
        oldest_microtask->execute();

        // 4. Set the event loop's currently running task back to null.
        m_currently_running_task = nullptr;
    }

    // 4. For each environment settings object whose responsible event loop is this event loop, notify about rejected promises on that environment settings object.
    for (auto& environment_settings_object : m_related_environment_settings_objects)
        environment_settings_object.notify_about_rejected_promises({});

    // FIXME: 5. Cleanup Indexed Database transactions.

    // 6. Perform ClearKeptObjects().
    vm().finish_execution_generation();

    // 7. Set the event loop's performing a microtask checkpoint to false.
    m_performing_a_microtask_checkpoint = false;
}

NonnullRefPtrVector<DOM::Document> EventLoop::documents_in_this_event_loop() const
{
    NonnullRefPtrVector<DOM::Document> documents;
    for (auto& document : m_documents) {
        VERIFY(document);
        documents.append(*document);
    }
    return documents;
}

void EventLoop::register_document(Badge<DOM::Document>, DOM::Document& document)
{
    m_documents.append(&document);
}

void EventLoop::unregister_document(Badge<DOM::Document>, DOM::Document& document)
{
    bool did_remove = m_documents.remove_first_matching([&](auto& entry) { return entry.ptr() == &document; });
    VERIFY(did_remove);
}

void EventLoop::push_onto_backup_incumbent_settings_object_stack(Badge<EnvironmentSettingsObject>, EnvironmentSettingsObject& environment_settings_object)
{
    m_backup_incumbent_settings_object_stack.append(environment_settings_object);
}

void EventLoop::pop_backup_incumbent_settings_object_stack(Badge<EnvironmentSettingsObject>)
{
    m_backup_incumbent_settings_object_stack.take_last();
}

EnvironmentSettingsObject& EventLoop::top_of_backup_incumbent_settings_object_stack()
{
    return m_backup_incumbent_settings_object_stack.last();
}

void EventLoop::register_environment_settings_object(Badge<EnvironmentSettingsObject>, EnvironmentSettingsObject& environment_settings_object)
{
    m_related_environment_settings_objects.append(environment_settings_object);
}

void EventLoop::unregister_environment_settings_object(Badge<EnvironmentSettingsObject>, EnvironmentSettingsObject& environment_settings_object)
{
    bool did_remove = m_related_environment_settings_objects.remove_first_matching([&](auto& entry) { return &entry == &environment_settings_object; });
    VERIFY(did_remove);
}

}
