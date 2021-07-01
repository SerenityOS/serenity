/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "WebAssemblyModuleConstructor.h"
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/Forward.h>

namespace Web::Bindings {

class WebAssemblyModulePrototype final : public JS::Object {
    JS_OBJECT(WebAssemblyModulePrototype, JS::Object);

public:
    explicit WebAssemblyModulePrototype(JS::GlobalObject& global_object)
        : JS::Object(global_object)
    {
    }
};

}
