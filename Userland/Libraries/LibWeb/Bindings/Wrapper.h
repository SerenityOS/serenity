/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/Weakable.h>
#include <LibJS/Runtime/Object.h>
#include <LibWeb/Forward.h>

namespace Web::Bindings {

class Wrapper
    : public JS::Object
    , public Weakable<Wrapper> {
    JS_OBJECT(Wrapper, JS::Object);

public:
protected:
    explicit Wrapper(Object& prototype)
        : Object(prototype)
    {
    }
};

}
