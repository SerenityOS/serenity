/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/WeakRef.h>

namespace JS {

class WeakRefPrototype final : public Object {
    JS_OBJECT(WeakRefPrototype, Object);

public:
    WeakRefPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~WeakRefPrototype() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(deref);
};

}
