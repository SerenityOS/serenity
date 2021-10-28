/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/PrototypeObject.h>
#include <LibJS/Runtime/WeakMap.h>

namespace JS {

class WeakMapPrototype final : public PrototypeObject<WeakMapPrototype, WeakMap> {
    JS_PROTOTYPE_OBJECT(WeakMapPrototype, WeakMap, WeakMap);

public:
    WeakMapPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~WeakMapPrototype() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(delete_);
    JS_DECLARE_NATIVE_FUNCTION(get);
    JS_DECLARE_NATIVE_FUNCTION(has);
    JS_DECLARE_NATIVE_FUNCTION(set);
};

}
