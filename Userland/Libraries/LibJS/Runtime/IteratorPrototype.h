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
    virtual void initialize(Realm&) override;
    virtual ~IteratorPrototype() override = default;

private:
    IteratorPrototype(Realm&);

    JS_DECLARE_NATIVE_FUNCTION(symbol_iterator);
};

}
