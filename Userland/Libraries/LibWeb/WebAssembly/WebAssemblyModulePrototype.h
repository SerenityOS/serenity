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
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/Window.h>

namespace Web::Bindings {

class WebAssemblyModulePrototype final : public JS::Object {
    JS_OBJECT(WebAssemblyModulePrototype, JS::Object);

public:
    explicit WebAssemblyModulePrototype(JS::Realm& realm)
        : JS::Object(*realm.intrinsics().object_prototype())
    {
    }
};

}
