/*
 * Copyright (c) 2020, Jack Karamanian <karamanian.jack@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/BooleanObject.h>

namespace JS {

class BooleanPrototype final : public BooleanObject {
    JS_OBJECT(BooleanPrototype, BooleanObject);

public:
    explicit BooleanPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~BooleanPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(to_string);
    JS_DECLARE_NATIVE_FUNCTION(value_of);
};

}
