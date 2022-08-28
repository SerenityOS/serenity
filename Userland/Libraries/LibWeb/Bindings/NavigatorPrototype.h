/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/Window.h>

namespace Web::Bindings {

class NavigatorPrototype final : public JS::Object {
    JS_OBJECT(NavigatorPrototype, JS::Object);

public:
    explicit NavigatorPrototype(JS::Realm& realm)
        : JS::Object(*realm.intrinsics().object_prototype())
    {
    }
};

}
