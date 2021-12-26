/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibJS/Runtime/EnvironmentRecord.h>
#include <LibJS/Runtime/PropertyName.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class Reference {
public:
    enum class BaseType : u8 {
        Unresolvable,
        Value,
        EnvironmentRecord,
    };

    Reference() { }
    Reference(BaseType type, PropertyName const& name, bool strict)
        : m_base_type(type)
        , m_name(name)
        , m_strict(strict)
    {
    }

    Reference(Value base, PropertyName const& name, Value this_value, bool strict = false)
        : m_base_type(BaseType::Value)
        , m_base_value(base)
        , m_name(name)
        , m_this_value(this_value)
        , m_strict(strict)
    {
        if (base.is_nullish()) {
            m_base_type = BaseType::Unresolvable;
            m_base_value = {};
            m_this_value = {};
            m_name = {};
        }
    }

    Reference(EnvironmentRecord& base, FlyString const& referenced_name, bool strict = false)
        : m_base_type(BaseType::EnvironmentRecord)
        , m_base_environment_record(&base)
        , m_name(referenced_name)
        , m_strict(strict)
    {
    }

    Value base() const
    {
        VERIFY(m_base_type == BaseType::Value);
        return m_base_value;
    }

    EnvironmentRecord& base_environment() const
    {
        VERIFY(m_base_type == BaseType::EnvironmentRecord);
        return *m_base_environment_record;
    }

    PropertyName const& name() const { return m_name; }
    bool is_strict() const { return m_strict; }

    // 6.2.4.2 IsUnresolvableReference ( V ), https://tc39.es/ecma262/#sec-isunresolvablereference
    bool is_unresolvable() const { return m_base_type == BaseType::Unresolvable; }

    // 6.2.4.1 IsPropertyReference ( V ), https://tc39.es/ecma262/#sec-ispropertyreference
    bool is_property_reference() const
    {
        if (is_unresolvable())
            return false;
        if (m_base_type == BaseType::EnvironmentRecord)
            return false;
        if (m_base_value.is_boolean() || m_base_value.is_string() || m_base_value.is_symbol() || m_base_value.is_bigint() || m_base_value.is_number() || m_base_value.is_object())
            return true;
        return false;
    }

    // 6.2.4.7 GetThisValue ( V ), https://tc39.es/ecma262/#sec-getthisvalue
    Value get_this_value() const
    {
        VERIFY(is_property_reference());
        if (is_super_reference())
            return m_this_value;
        return m_base_value;
    }

    // 6.2.4.3 IsSuperReference ( V ), https://tc39.es/ecma262/#sec-issuperreference
    bool is_super_reference() const
    {
        return !m_this_value.is_empty();
    }

    void put(GlobalObject&, Value);
    Value get(GlobalObject&, bool throw_if_undefined = true);
    bool delete_(GlobalObject&);

    String to_string() const;

private:
    void throw_reference_error(GlobalObject&);

    BaseType m_base_type { BaseType::Unresolvable };
    union {
        Value m_base_value;
        EnvironmentRecord* m_base_environment_record;
    };
    PropertyName m_name;
    Value m_this_value;
    bool m_strict { false };
};

}
