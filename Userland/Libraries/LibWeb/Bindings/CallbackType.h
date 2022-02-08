/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Heap/Handle.h>
#include <LibWeb/Forward.h>

namespace Web::Bindings {

// https://heycam.github.io/webidl/#idl-callback-interface
struct CallbackType {
    CallbackType(JS::Handle<JS::Object> callback, HTML::EnvironmentSettingsObject& callback_context)
        : callback(move(callback))
        , callback_context(callback_context)
    {
    }

    JS::Handle<JS::Object> callback;

    // https://heycam.github.io/webidl/#dfn-callback-context
    HTML::EnvironmentSettingsObject& callback_context;
};

}
