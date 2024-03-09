/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/TypeCasts.h>

#include "DiagnosticEngine.h"
#include "Function.h"
#include "Runtime/ObjectType.h"

namespace JSSpecCompiler::Runtime {

struct Slot {
    bool operator==(Slot const&) const = default;

    FlyString key;
};

struct StringPropertyKey {
    bool operator==(StringPropertyKey const&) const = default;

    FlyString key;
};

#define ENUMERATE_WELL_KNOWN_SYMBOLS(F) \
    F(InstanceType, _instanceType)      \
    F(ToStringTag, toStringTag)

enum class WellKnownSymbol {
#define ID(enum_name, spec_name) enum_name,
    ENUMERATE_WELL_KNOWN_SYMBOLS(ID)
#undef ID
};

class PropertyKey : public Variant<Slot, StringPropertyKey, WellKnownSymbol> {
public:
    using Variant::Variant;
};

struct DataProperty {
    template<typename T>
    bool is() const
    {
        return ::is<Runtime::Object>(value);
    }

    template<typename T>
    T* as() const
    {
        return verify_cast<T>(value);
    }

    template<typename T>
    Optional<T*> get_or_diagnose(Realm* realm, QualifiedName name, Location location)
    {
        if (!is<T>()) {
            realm->diag().error(location,
                "{} must be a {}", name.to_string(), T::TYPE_NAME);
            realm->diag().note(this->location,
                "set to {} here", value->type_name());
            return {};
        }
        return verify_cast<T>(value);
    }

    Cell* value;
    Location location;

    bool is_writable = true;
    bool is_enumerable = false;
    bool is_configurable = true;
};

struct AccessorProperty {
    Optional<FunctionDeclarationRef> getter;
    Optional<FunctionDeclarationRef> setter;
    Location location;

    bool is_enumerable = false;
    bool is_configurable = true;
};

class Property : public Variant<DataProperty, AccessorProperty> {
public:
    using Variant::Variant;

    Location location() const
    {
        return visit([&](auto const& value) { return value.location; });
    }

    Optional<DataProperty&> get_data_property_or_diagnose(Realm* realm, QualifiedName name, Location location);
};

class Object : public Runtime::Cell {
public:
    static constexpr StringView TYPE_NAME = "object"sv;

    static Object* create(Realm* realm)
    {
        return realm->adopt_cell(new Object {});
    }

    StringView type_name() const override { return TYPE_NAME; }

    auto& type() { return m_type; }
    auto& properties() { return m_properties; }

    bool has(PropertyKey const& key) const
    {
        return m_properties.contains(key);
    }

    Property& get(PropertyKey const& key)
    {
        return m_properties.get(key).value();
    }

    void set(PropertyKey const& key, Property&& property)
    {
        auto insertion_result = m_properties.set(key, move(property));
        VERIFY(insertion_result == HashSetResult::InsertedNewEntry);
    }

protected:
    void do_dump(Printer& printer) const override;

private:
    Object() = default;

    Optional<ObjectType*> m_type;
    HashMap<PropertyKey, Property> m_properties;
};

}

template<>
struct AK::Traits<JSSpecCompiler::Runtime::PropertyKey> : public DefaultTraits<JSSpecCompiler::Runtime::PropertyKey> {
    static unsigned hash(JSSpecCompiler::Runtime::PropertyKey const& key)
    {
        using namespace JSSpecCompiler::Runtime;
        return key.visit(
            [](Slot const& slot) { return pair_int_hash(1, slot.key.hash()); },
            [](StringPropertyKey const& string_key) { return pair_int_hash(2, string_key.key.hash()); },
            [](WellKnownSymbol const& symbol) { return pair_int_hash(3, to_underlying(symbol)); });
    }
};
