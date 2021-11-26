/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibJS/Heap/Handle.h>
#include <LibJS/Runtime/FunctionObject.h>

namespace Web::HTML {

struct EventHandler {
    EventHandler()
    {
    }

    EventHandler(String s)
        : string(move(s))
    {
    }

    EventHandler(JS::Handle<JS::FunctionObject> c)
        : callback(move(c))
    {
    }

    String string;
    JS::Handle<JS::FunctionObject> callback;
};

}
