/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/Forward.h>

namespace Web::Bindings {

class NavigatorPrototype final : public JS::Object {
    JS_OBJECT(NavigatorPrototype, JS::Object);

public:
    explicit NavigatorPrototype(JS::GlobalObject& global_object)
        : JS::Object(*global_object.object_prototype())
    {
    }
};

}
