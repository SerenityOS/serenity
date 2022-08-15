/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class IteratorPrototype : public Object {
    JS_OBJECT(IteratorPrototype, Object)

public:
    IteratorPrototype(Realm&);
    virtual void initialize(GlobalObject&) override;
    virtual ~IteratorPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(symbol_iterator);
};

}
