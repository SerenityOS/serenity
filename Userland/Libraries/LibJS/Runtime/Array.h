/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class Array : public Object {
    JS_OBJECT(Array, Object);

public:
    static Array* create(GlobalObject&, size_t length, Object* prototype = nullptr);
    static Array* create_from(GlobalObject&, Vector<Value> const&);

    explicit Array(Object& prototype);
    virtual void initialize(GlobalObject&) override;
    virtual ~Array() override;

    static Array* typed_this(VM&, GlobalObject&);

private:
    JS_DECLARE_NATIVE_GETTER(length_getter);
    JS_DECLARE_NATIVE_SETTER(length_setter);
};

}
