/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/PrototypeObject.h>
#include <LibJS/Runtime/RegExpStringIterator.h>

namespace JS {

class RegExpStringIteratorPrototype final : public PrototypeObject<RegExpStringIteratorPrototype, RegExpStringIterator> {
    JS_PROTOTYPE_OBJECT(RegExpStringIteratorPrototype, RegExpStringIterator, RegExpStringIterator);

public:
    explicit RegExpStringIteratorPrototype(GlobalObject&);
    virtual ~RegExpStringIteratorPrototype() override = default;

    virtual void initialize(GlobalObject&) override;

private:
    JS_DECLARE_NATIVE_FUNCTION(next);
};

}
