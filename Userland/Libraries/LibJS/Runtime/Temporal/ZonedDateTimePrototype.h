/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class ZonedDateTimePrototype final : public Object {
    JS_OBJECT(ZonedDateTimePrototype, Object);

public:
    explicit ZonedDateTimePrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~ZonedDateTimePrototype() override = default;
};

}
