/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class PlainDateTimePrototype final : public Object {
    JS_OBJECT(PlainDateTimePrototype, Object);

public:
    explicit PlainDateTimePrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~PlainDateTimePrototype() override = default;
};

}
