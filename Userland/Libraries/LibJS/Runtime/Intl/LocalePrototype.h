/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS::Intl {

class LocalePrototype final : public Object {
    JS_OBJECT(LocalePrototype, Object);

public:
    explicit LocalePrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~LocalePrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(to_string);
};

}
