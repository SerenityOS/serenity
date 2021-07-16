/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class RegExpStringIteratorPrototype final : public Object {
    JS_OBJECT(RegExpStringIteratorPrototype, Object)

public:
    explicit RegExpStringIteratorPrototype(GlobalObject&);
    virtual ~RegExpStringIteratorPrototype() override = default;

    virtual void initialize(GlobalObject&) override;

private:
    JS_DECLARE_NATIVE_FUNCTION(next);
};

}
