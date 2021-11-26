/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>
#include <LibJS/Runtime/PrototypeObject.h>
#include <LibJS/Runtime/StringIterator.h>

namespace JS {

class StringIteratorPrototype final : public PrototypeObject<StringIteratorPrototype, StringIterator> {
    JS_PROTOTYPE_OBJECT(StringIteratorPrototype, StringIterator, StringIterator);

public:
    StringIteratorPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~StringIteratorPrototype() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(next);
};

}
