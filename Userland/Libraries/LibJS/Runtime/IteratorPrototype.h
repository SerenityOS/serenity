/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Iterator.h>
#include <LibJS/Runtime/PrototypeObject.h>

namespace JS {

class IteratorPrototype : public PrototypeObject<IteratorPrototype, Iterator> {
    JS_PROTOTYPE_OBJECT(IteratorPrototype, Iterator, Iterator);

public:
    virtual ThrowCompletionOr<void> initialize(Realm&) override;
    virtual ~IteratorPrototype() override = default;

private:
    IteratorPrototype(Realm&);

    JS_DECLARE_NATIVE_FUNCTION(symbol_iterator);
    JS_DECLARE_NATIVE_FUNCTION(map);
    JS_DECLARE_NATIVE_FUNCTION(filter);
};

}
