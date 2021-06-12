/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/WeakMap.h>

namespace JS {

class WeakMapPrototype final : public Object {
    JS_OBJECT(WeakMapPrototype, Object);

public:
    WeakMapPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~WeakMapPrototype() override;

private:
    static WeakMap* typed_this(VM&, GlobalObject&);

    JS_DECLARE_NATIVE_FUNCTION(delete_);
    JS_DECLARE_NATIVE_FUNCTION(get);
    JS_DECLARE_NATIVE_FUNCTION(has);
    JS_DECLARE_NATIVE_FUNCTION(set);
};

}
