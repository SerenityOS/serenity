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
    virtual ~Array() override;

    virtual Optional<PropertyDescriptor> internal_get_own_property(PropertyName const&) const override;
    virtual bool internal_define_own_property(PropertyName const&, PropertyDescriptor const&) override;
    virtual bool internal_delete(PropertyName const&) override;
    virtual MarkedValueList internal_own_property_keys() const override;

    [[nodiscard]] bool length_is_writable() const { return m_length_writable; };

private:
    bool set_length(PropertyDescriptor const&);

    bool m_length_writable { true };
};

}
