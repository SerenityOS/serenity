/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Intl {

class NumberFormatPrototype final : public Object {
    JS_OBJECT(NumberFormatPrototype, Object);

public:
    explicit NumberFormatPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~NumberFormatPrototype() override = default;
};

}
