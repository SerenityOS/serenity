/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Function.h>
#include <AK/Vector.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class Array : public Object {
    JS_OBJECT(Array, Object);

public:
    static ThrowCompletionOr<Array*> create(GlobalObject&, size_t length, Object* prototype = nullptr);
    static Array* create_from(GlobalObject&, Vector<Value> const&);
    // Non-standard but equivalent to CreateArrayFromList.
    template<typename T>
    static Array* create_from(GlobalObject& global_object, Vector<T>& elements, Function<Value(T&)> map_fn)
    {
        auto& vm = global_object.vm();
        auto values = MarkedValueList { global_object.heap() };
        values.ensure_capacity(elements.size());
        for (auto& element : elements) {
            values.append(map_fn(element));
            VERIFY(!vm.exception());
        }
        return Array::create_from(global_object, values);
    }

    explicit Array(Object& prototype);
    virtual ~Array() override;

    virtual ThrowCompletionOr<Optional<PropertyDescriptor>> internal_get_own_property(PropertyKey const&) const override;
    virtual ThrowCompletionOr<bool> internal_define_own_property(PropertyKey const&, PropertyDescriptor const&) override;
    virtual ThrowCompletionOr<bool> internal_delete(PropertyKey const&) override;
    virtual ThrowCompletionOr<MarkedValueList> internal_own_property_keys() const override;

    [[nodiscard]] bool length_is_writable() const { return m_length_writable; };

private:
    ThrowCompletionOr<bool> set_length(PropertyDescriptor const&);

    bool m_length_writable { true };
};

}
