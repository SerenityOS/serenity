/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Intl {

class DisplayNamesPrototype final : public Object {
    JS_OBJECT(DisplayNamesPrototype, Object);

public:
    explicit DisplayNamesPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~DisplayNamesPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(of);
};

}
