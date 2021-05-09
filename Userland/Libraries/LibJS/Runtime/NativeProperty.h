/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class NativeProperty final : public Cell {
public:
    NativeProperty(AK::Function<Value(VM&, GlobalObject&)> getter, AK::Function<void(VM&, GlobalObject&, Value)> setter);
    virtual ~NativeProperty() override;

    Value get(VM&, GlobalObject&) const;
    void set(VM&, GlobalObject&, Value);

private:
    virtual const char* class_name() const override { return "NativeProperty"; }

    AK::Function<Value(VM&, GlobalObject&)> m_getter;
    AK::Function<void(VM&, GlobalObject&, Value)> m_setter;
};

}
