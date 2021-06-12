/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Map.h>

namespace JS {

class MapPrototype final : public Object {
    JS_OBJECT(MapPrototype, Object);

public:
    MapPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~MapPrototype() override;

private:
    static Map* typed_this(VM&, GlobalObject&);

    JS_DECLARE_NATIVE_GETTER(size_getter);
};

}
