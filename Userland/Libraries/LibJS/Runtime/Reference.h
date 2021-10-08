/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/EnvironmentCoordinate.h>
#include <LibJS/Runtime/ExecutionContext.h>
#include <LibJS/Runtime/PropertyName.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class Reference {
public:
    enum class BaseType : u8 {
        Unresolvable,
        Value,
        Environment,
    };

    Reference() { }
    Reference(BaseType type, PropertyName name, bool strict)
        : m_base_type(type)
        , m_name(move(name))
        , m_strict(strict)
    {
    }

    Reference(Value base, PropertyName name, Value this_value, bool strict = false)
        : m_base_type(BaseType::Value)
        , m_base_value(base)
        , m_name(move(name))
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

    Reference(Environment& base, FlyString referenced_name, bool strict = false, Optional<EnvironmentCoordinate> environment_coordinate = {})
        : m_base_type(BaseType::Environment)
        , m_base_environment(&base)
        , m_name(move(referenced_name))
        , m_strict(strict)
        , m_environment_coordinate(move(environment_coordinate))
    {
    }

    Value base() const
    {
        VERIFY(m_base_type == BaseType::Value);
        return m_base_value;
    }

    Environment& base_environment() const
    {
        VERIFY(m_base_type == BaseType::Environment);
        return *m_base_environment;
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
        if (m_base_type == BaseType::Environment)
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

    // Note: Non-standard helper.
    bool is_environment_reference() const
    {
        return m_base_type == BaseType::Environment;
    }

    void initialize_referenced_binding(GlobalObject& global_object, Value value) const
    {
        VERIFY(!is_unresolvable());
        VERIFY(m_base_type == BaseType::Environment);
        m_base_environment->initialize_binding(global_object, m_name.as_string(), value);
    }

    void put_value(GlobalObject&, Value);
    Value get_value(GlobalObject&) const;
    bool delete_(GlobalObject&);

    String to_string() const;

    bool is_valid_reference() const { return m_name.is_valid(); }

    Optional<EnvironmentCoordinate> environment_coordinate() const { return m_environment_coordinate; }

private:
    void throw_reference_error(GlobalObject&) const;

    BaseType m_base_type { BaseType::Unresolvable };
    union {
        Value m_base_value {};
        mutable Environment* m_base_environment;
    };
    PropertyName m_name;
    Value m_this_value;
    bool m_strict { false };
    Optional<EnvironmentCoordinate> m_environment_coordinate;
};

}
