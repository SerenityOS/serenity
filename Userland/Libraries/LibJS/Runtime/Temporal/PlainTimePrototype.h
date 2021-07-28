/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class PlainTimePrototype final : public Object {
    JS_OBJECT(PlainTimePrototype, Object);

public:
    explicit PlainTimePrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~PlainTimePrototype() override = default;
};

}
