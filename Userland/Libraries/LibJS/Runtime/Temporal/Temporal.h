/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Temporal {

class Temporal final : public Object {
    JS_OBJECT(Temporal, Object);

public:
    explicit Temporal(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~Temporal() override = default;
};

}
