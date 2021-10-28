/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/ArrayIterator.h>
#include <LibJS/Runtime/PrototypeObject.h>

namespace JS {

class ArrayIteratorPrototype final : public PrototypeObject<ArrayIteratorPrototype, ArrayIterator> {
    JS_PROTOTYPE_OBJECT(ArrayIteratorPrototype, ArrayIterator, ArrayIterator);

public:
    ArrayIteratorPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~ArrayIteratorPrototype() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(next);
};

}
