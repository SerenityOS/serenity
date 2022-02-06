/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibJS/Runtime/JobCallback.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>

namespace Web::Bindings {

struct WebEngineCustomData final : public JS::VM::CustomData {
    virtual ~WebEngineCustomData() override { }

    HTML::EventLoop event_loop;
};

struct WebEngineCustomJobCallbackData final : public JS::JobCallback::CustomData {
    WebEngineCustomJobCallbackData(HTML::EnvironmentSettingsObject& incumbent_settings, OwnPtr<JS::ExecutionContext> active_script_context)
        : incumbent_settings(incumbent_settings)
        , active_script_context(move(active_script_context))
    {
    }

    virtual ~WebEngineCustomJobCallbackData() override { }

    HTML::EnvironmentSettingsObject& incumbent_settings;
    OwnPtr<JS::ExecutionContext> active_script_context;
};

HTML::ClassicScript* active_script();
JS::VM& main_thread_vm();

}
