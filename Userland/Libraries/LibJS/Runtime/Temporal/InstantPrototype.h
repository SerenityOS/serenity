/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class InstantPrototype final : public Object {
    JS_OBJECT(InstantPrototype, Object);

public:
    explicit InstantPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~InstantPrototype() override = default;
};

}
