/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class MapIteratorPrototype final : public Object {
    JS_OBJECT(MapIteratorPrototype, Object)

public:
    MapIteratorPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~MapIteratorPrototype() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(next);
};

}
