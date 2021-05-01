/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class StringIteratorPrototype final : public Object {
    JS_OBJECT(StringIteratorPrototype, Object)

public:
    StringIteratorPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~StringIteratorPrototype() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(next);
};

}
