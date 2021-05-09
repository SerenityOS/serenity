/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>

namespace JS {

class Uint8ClampedArray final : public Object {
    JS_OBJECT(Uint8ClampedArray, Object);

public:
    static Uint8ClampedArray* create(GlobalObject&, u32 length);

    Uint8ClampedArray(u32 length, Object& prototype);
    virtual ~Uint8ClampedArray() override;

    i32 length() const { return m_length; }

    virtual bool put_by_index(u32 property_index, Value value) override;
    virtual Value get_by_index(u32 property_index) const override;

    u8* data() { return m_data; }
    const u8* data() const { return m_data; }

private:
    JS_DECLARE_NATIVE_GETTER(length_getter);

    u8* m_data { nullptr };
    u32 m_length { 0 };
};

}
