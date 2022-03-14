/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/EnvironmentCoordinate.h>
#include <LibJS/Runtime/PropertyKey.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

Reference make_private_reference(VM&, Value base_value, FlyString const& private_identifier);

class Reference {
public:
    enum class BaseType : u8 {
        Unresolvable,
        Value,
        Environment,
    };

    Reference() = default;
    Reference(BaseType type, PropertyKey name, bool strict)
        : m_base_type(type)
        , m_name(move(name))
        , m_strict(strict)
    {
    }

    Reference(Value base, PropertyKey name, Value this_value, bool strict = false)
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

    Reference(Value base, PrivateName name)
        : m_base_type(BaseType::Value)
        , m_base_value(base)
        , m_this_value(Value {})
        , m_strict(true)
        , m_is_private(true)
        , m_private_name(move(name))
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

    PropertyKey const& name() const { return m_name; }
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
        return true;
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

    // 6.2.4.4 IsPrivateReference ( V ), https://tc39.es/ecma262/#sec-isprivatereference
    bool is_private_reference() const
    {
        return m_is_private;
    }

    // Note: Non-standard helper.
    bool is_environment_reference() const
    {
        return m_base_type == BaseType::Environment;
    }

    // 6.2.4.8 InitializeReferencedBinding ( V, W ), https://tc39.es/ecma262/#sec-object.prototype.hasownproperty
    ThrowCompletionOr<void> initialize_referenced_binding(GlobalObject& global_object, Value value) const
    {
        VERIFY(!is_unresolvable());
        VERIFY(m_base_type == BaseType::Environment);
        return m_base_environment->initialize_binding(global_object, m_name.as_string(), value);
    }

    ThrowCompletionOr<void> put_value(GlobalObject&, Value);
    ThrowCompletionOr<Value> get_value(GlobalObject&) const;
    ThrowCompletionOr<bool> delete_(GlobalObject&);

    String to_string() const;

    bool is_valid_reference() const { return m_name.is_valid() || m_is_private; }

    Optional<EnvironmentCoordinate> environment_coordinate() const { return m_environment_coordinate; }

private:
    Completion throw_reference_error(GlobalObject&) const;

    BaseType m_base_type { BaseType::Unresolvable };
    union {
        Value m_base_value {};
        mutable Environment* m_base_environment;
    };
    PropertyKey m_name;
    Value m_this_value;
    bool m_strict { false };

    bool m_is_private { false };
    // FIXME: This can (probably) be an union with m_name.
    PrivateName m_private_name;

    Optional<EnvironmentCoordinate> m_environment_coordinate;
};

}
