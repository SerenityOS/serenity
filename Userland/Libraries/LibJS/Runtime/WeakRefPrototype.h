/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/PrototypeObject.h>
#include <LibJS/Runtime/WeakRef.h>

namespace JS {

class WeakRefPrototype final : public PrototypeObject<WeakRefPrototype, WeakRef> {
    JS_PROTOTYPE_OBJECT(WeakRefPrototype, WeakRef, WeakRef);

public:
    WeakRefPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~WeakRefPrototype() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(deref);
};

}
