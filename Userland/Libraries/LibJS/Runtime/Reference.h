/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibJS/Runtime/PropertyName.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class Reference {
public:
    Reference() { }
    Reference(Value base, const PropertyName& name, bool strict = false)
        : m_base(base)
        , m_name(name)
        , m_strict(strict)
    {
    }

    enum LocalVariableTag { LocalVariable };
    Reference(LocalVariableTag, const FlyString& name, bool strict = false)
        : m_base(js_null())
        , m_name(name)
        , m_strict(strict)
        , m_local_variable(true)
    {
    }

    enum GlobalVariableTag { GlobalVariable };
    Reference(GlobalVariableTag, const FlyString& name, bool strict = false)
        : m_base(js_null())
        , m_name(name)
        , m_strict(strict)
        , m_global_variable(true)
    {
    }

    Value base() const { return m_base; }
    const PropertyName& name() const { return m_name; }
    bool is_strict() const { return m_strict; }

    bool is_unresolvable() const { return m_base.is_empty(); }
    bool is_property() const
    {
        return m_base.is_object() || has_primitive_base();
    }

    bool has_primitive_base() const
    {
        return m_base.is_boolean() || m_base.is_string() || m_base.is_number();
    }

    bool is_local_variable() const
    {
        return m_local_variable;
    }

    bool is_global_variable() const
    {
        return m_global_variable;
    }

    void put(GlobalObject&, Value);
    Value get(GlobalObject&);

private:
    void throw_reference_error(GlobalObject&);

    Value m_base;
    PropertyName m_name;
    bool m_strict { false };
    bool m_local_variable { false };
    bool m_global_variable { false };
};

}
