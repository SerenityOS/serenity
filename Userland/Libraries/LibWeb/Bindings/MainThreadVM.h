/*
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/JobCallback.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/DOM/MutationObserver.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/Window.h>

namespace Web::Bindings {

struct WebEngineCustomData final : public JS::VM::CustomData {
    virtual ~WebEngineCustomData() override = default;

    virtual void spin_event_loop_until(Function<bool()> goal_condition) override;

    HTML::EventLoop event_loop;

    // FIXME: These should only be on similar-origin window agents, but we don't currently differentiate agent types.

    // https://dom.spec.whatwg.org/#mutation-observer-compound-microtask-queued-flag
    bool mutation_observer_microtask_queued { false };

    // https://dom.spec.whatwg.org/#mutation-observer-list
    // FIXME: This should be a set.
    Vector<JS::Handle<DOM::MutationObserver>> mutation_observers;

    OwnPtr<JS::ExecutionContext> root_execution_context;

    // This object is used as the global object for GC-allocated objects that don't
    // belong to a web-facing global object.
    JS::Handle<HTML::Window> internal_window_object;
};

struct WebEngineCustomJobCallbackData final : public JS::JobCallback::CustomData {
    WebEngineCustomJobCallbackData(HTML::EnvironmentSettingsObject& incumbent_settings, OwnPtr<JS::ExecutionContext> active_script_context)
        : incumbent_settings(incumbent_settings)
        , active_script_context(move(active_script_context))
    {
    }

    virtual ~WebEngineCustomJobCallbackData() override = default;

    HTML::EnvironmentSettingsObject& incumbent_settings;
    OwnPtr<JS::ExecutionContext> active_script_context;
};

HTML::ClassicScript* active_script();
JS::VM& main_thread_vm();
HTML::Window& main_thread_internal_window_object();
void queue_mutation_observer_microtask(DOM::Document&);
NonnullOwnPtr<JS::ExecutionContext> create_a_new_javascript_realm(JS::VM&, Function<JS::Object*(JS::Realm&)> create_global_object, Function<JS::Object*(JS::Realm&)> create_global_this_value);

}
