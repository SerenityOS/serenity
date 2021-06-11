/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class AggregateError : public Object {
    JS_OBJECT(Error, Object);

public:
    static AggregateError* create(GlobalObject&, String const& message, Vector<Value> const& errors);

    explicit AggregateError(Object& prototype);
    virtual ~AggregateError() override = default;
};

}
