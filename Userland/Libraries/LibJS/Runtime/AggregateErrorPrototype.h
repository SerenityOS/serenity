/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class AggregateErrorPrototype final : public Object {
    JS_OBJECT(AggregateErrorPrototype, Object);

public:
    explicit AggregateErrorPrototype(Realm&);
    virtual void initialize(GlobalObject&) override;
    virtual ~AggregateErrorPrototype() override = default;
};

}
