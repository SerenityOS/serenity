/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class Now final : public Object {
    JS_OBJECT(Now, Object);

public:
    explicit Now(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~Now() override = default;
};

}
