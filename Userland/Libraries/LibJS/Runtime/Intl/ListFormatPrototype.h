/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Intl {

class ListFormatPrototype final : public Object {
    JS_OBJECT(ListFormatPrototype, Object);

public:
    explicit ListFormatPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~ListFormatPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(format);
};

}
