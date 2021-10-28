/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/PrototypeObject.h>
#include <LibJS/Runtime/Set.h>

namespace JS {

class SetPrototype final : public PrototypeObject<SetPrototype, Set> {
    JS_PROTOTYPE_OBJECT(SetPrototype, Set, Set);

public:
    SetPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~SetPrototype() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(add);
    JS_DECLARE_NATIVE_FUNCTION(clear);
    JS_DECLARE_NATIVE_FUNCTION(delete_);
    JS_DECLARE_NATIVE_FUNCTION(entries);
    JS_DECLARE_NATIVE_FUNCTION(for_each);
    JS_DECLARE_NATIVE_FUNCTION(has);
    JS_DECLARE_NATIVE_FUNCTION(values);

    JS_DECLARE_NATIVE_FUNCTION(size_getter);
};

}
