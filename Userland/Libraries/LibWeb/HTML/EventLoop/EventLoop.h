/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/WeakPtr.h>
#include <LibCore/Forward.h>
#include <LibJS/Forward.h>
#include <LibWeb/HTML/EventLoop/TaskQueue.h>

namespace Web::HTML {

class EventLoop {
public:
    enum class Type {
        // https://html.spec.whatwg.org/multipage/webappapis.html#window-event-loop
        Window,
        // https://html.spec.whatwg.org/multipage/webappapis.html#worker-event-loop
        Worker,
        // https://html.spec.whatwg.org/multipage/webappapis.html#worklet-event-loop
        Worklet,
    };

    EventLoop();
    ~EventLoop();

    Type type() const { return m_type; }

    TaskQueue& task_queue() { return m_task_queue; }
    TaskQueue const& task_queue() const { return m_task_queue; }

    TaskQueue& microtask_queue() { return m_microtask_queue; }
    TaskQueue const& microtask_queue() const { return m_microtask_queue; }

    void spin_until(Function<bool()> goal_condition);
    void process();

    Task const* currently_running_task() const { return m_currently_running_task; }

    JS::VM& vm() { return *m_vm; }
    JS::VM const& vm() const { return *m_vm; }

    void set_vm(JS::VM&);

    void schedule();

    void perform_a_microtask_checkpoint();

    void register_document(Badge<DOM::Document>, DOM::Document&);
    void unregister_document(Badge<DOM::Document>, DOM::Document&);

    NonnullRefPtrVector<DOM::Document> documents_in_this_event_loop() const;

    void push_onto_backup_incumbent_settings_object_stack(Badge<EnvironmentSettingsObject>, EnvironmentSettingsObject& environment_settings_object);
    void pop_backup_incumbent_settings_object_stack(Badge<EnvironmentSettingsObject>);
    EnvironmentSettingsObject& top_of_backup_incumbent_settings_object_stack();
    bool is_backup_incumbent_settings_object_stack_empty() const { return m_backup_incumbent_settings_object_stack.is_empty(); }

    void register_environment_settings_object(Badge<EnvironmentSettingsObject>, EnvironmentSettingsObject&);
    void unregister_environment_settings_object(Badge<EnvironmentSettingsObject>, EnvironmentSettingsObject&);

private:
    Type m_type { Type::Window };

    TaskQueue m_task_queue;
    TaskQueue m_microtask_queue;

    // https://html.spec.whatwg.org/multipage/webappapis.html#currently-running-task
    Task* m_currently_running_task { nullptr };

    JS::VM* m_vm { nullptr };

    RefPtr<Core::Timer> m_system_event_loop_timer;

    // https://html.spec.whatwg.org/#performing-a-microtask-checkpoint
    bool m_performing_a_microtask_checkpoint { false };

    Vector<WeakPtr<DOM::Document>> m_documents;

    // Used to implement step 4 of "perform a microtask checkpoint".
    Vector<EnvironmentSettingsObject&> m_related_environment_settings_objects;

    // https://html.spec.whatwg.org/multipage/webappapis.html#backup-incumbent-settings-object-stack
    Vector<EnvironmentSettingsObject&> m_backup_incumbent_settings_object_stack;
};

EventLoop& main_thread_event_loop();
void old_queue_global_task_with_document(HTML::Task::Source, DOM::Document&, Function<void()> steps);
void queue_global_task(HTML::Task::Source, JS::GlobalObject&, Function<void()> steps);
void queue_a_microtask(DOM::Document*, Function<void()> steps);
void perform_a_microtask_checkpoint();

}
