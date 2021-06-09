/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/WeakSet.h>

namespace JS {

class WeakSetPrototype final : public Object {
    JS_OBJECT(WeakSetPrototype, Object);

public:
    WeakSetPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~WeakSetPrototype() override;

private:
    static WeakSet* typed_this(VM&, GlobalObject&);

    JS_DECLARE_NATIVE_FUNCTION(add);
    JS_DECLARE_NATIVE_FUNCTION(delete_);
    JS_DECLARE_NATIVE_FUNCTION(has);
};

}
