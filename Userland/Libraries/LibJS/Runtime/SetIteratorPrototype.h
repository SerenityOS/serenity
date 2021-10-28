/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/PrototypeObject.h>
#include <LibJS/Runtime/SetIterator.h>

namespace JS {

class SetIteratorPrototype final : public PrototypeObject<SetIteratorPrototype, SetIterator> {
    JS_PROTOTYPE_OBJECT(SetIteratorPrototype, SetIterator, SetIterator);

public:
    SetIteratorPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~SetIteratorPrototype() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(next);
};

}
