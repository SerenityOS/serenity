/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Tuple.h>
#include <AK/TypeCasts.h>

namespace IDL {

template<typename FunctionType>
static size_t get_function_shortest_length(FunctionType& function)
{
    size_t length = 0;
    for (auto& parameter : function.parameters) {
        if (!parameter.optional && !parameter.variadic)
            length++;
    }
    return length;
}

enum class SequenceStorageType {
    Vector,       // Used to safely store non-JS values
    MarkedVector, // Used to safely store JS::Value and anything that inherits JS::Cell, e.g. JS::Object
};

struct CppType {
    String name;
    SequenceStorageType sequence_storage_type;
};

class ParameterizedType;
class UnionType;

class Type : public RefCounted<Type> {
public:
    enum class Kind {
        Plain, // AKA, Type.
        Parameterized,
        Union,
    };

    Type(String name, bool nullable)
        : m_kind(Kind::Plain)
        , m_name(move(name))
        , m_nullable(nullable)
    {
    }

    Type(Kind kind, String name, bool nullable)
        : m_kind(kind)
        , m_name(move(name))
        , m_nullable(nullable)
    {
    }

    virtual ~Type() = default;

    Kind kind() const { return m_kind; }

    bool is_plain() const { return m_kind == Kind::Plain; }

    bool is_parameterized() const { return m_kind == Kind::Parameterized; }
    ParameterizedType const& as_parameterized() const;
    ParameterizedType& as_parameterized();

    bool is_union() const { return m_kind == Kind::Union; }
    UnionType const& as_union() const;
    UnionType& as_union();

    String const& name() const { return m_name; }

    bool is_nullable() const { return m_nullable; }
    void set_nullable(bool value) { m_nullable = value; }

    // https://webidl.spec.whatwg.org/#dfn-includes-a-nullable-type
    bool includes_nullable_type() const;

    // -> https://webidl.spec.whatwg.org/#dfn-includes-undefined
    bool includes_undefined() const;

    Type const& innermost_type() const
    {
        // From step 4 of https://webidl.spec.whatwg.org/#dfn-distinguishable
        // "Consider the two "innermost" types derived by taking each type’s inner type if it is an annotated type, and then taking its inner type inner type if the result is a nullable type."
        // FIXME: Annotated types.
        VERIFY(!is_union());
        return *this;
    }

    // https://webidl.spec.whatwg.org/#idl-any
    bool is_any() const { return is_plain() && m_name == "any"; }

    // https://webidl.spec.whatwg.org/#idl-undefined
    bool is_undefined() const { return is_plain() && m_name == "undefined"; }

    // https://webidl.spec.whatwg.org/#idl-boolean
    bool is_boolean() const { return is_plain() && m_name == "boolean"; }

    // https://webidl.spec.whatwg.org/#idl-bigint
    bool is_bigint() const { return is_plain() && m_name == "bigint"; }

    // https://webidl.spec.whatwg.org/#idl-object
    bool is_object() const { return is_plain() && m_name == "object"; }

    // https://webidl.spec.whatwg.org/#idl-symbol
    bool is_symbol() const { return is_plain() && m_name == "symbol"; }

    bool is_string() const { return is_plain() && m_name.is_one_of("ByteString", "CSSOMString", "DOMString", "USVString"); }

    // https://webidl.spec.whatwg.org/#dfn-integer-type
    bool is_integer() const { return is_plain() && m_name.is_one_of("byte", "octet", "short", "unsigned short", "long", "unsigned long", "long long", "unsigned long long"); }

    // https://webidl.spec.whatwg.org/#dfn-numeric-type
    bool is_numeric() const { return is_plain() && (is_integer() || m_name.is_one_of("float", "unrestricted float", "double", "unrestricted double")); }

    // https://webidl.spec.whatwg.org/#dfn-primitive-type
    bool is_primitive() const { return is_plain() && (is_numeric() || is_boolean() || m_name == "bigint"); }

    // https://webidl.spec.whatwg.org/#idl-sequence
    bool is_sequence() const { return is_parameterized() && m_name == "sequence"; }

    // https://webidl.spec.whatwg.org/#dfn-distinguishable
    bool is_distinguishable_from(Type const& other) const;

private:
    Kind m_kind;
    String m_name;
    bool m_nullable { false };
};

struct Parameter {
    NonnullRefPtr<Type> type;
    String name;
    bool optional { false };
    Optional<String> optional_default_value;
    HashMap<String, String> extended_attributes;
    bool variadic { false };
};

struct Function {
    NonnullRefPtr<Type> return_type;
    String name;
    Vector<Parameter> parameters;
    HashMap<String, String> extended_attributes;
    size_t overload_index { 0 };
    bool is_overloaded { false };

    size_t shortest_length() const { return get_function_shortest_length(*this); }
};

struct Constructor {
    String name;
    Vector<Parameter> parameters;

    size_t shortest_length() const { return get_function_shortest_length(*this); }
};

struct Constant {
    NonnullRefPtr<Type> type;
    String name;
    String value;
};

struct Attribute {
    bool readonly { false };
    NonnullRefPtr<Type> type;
    String name;
    HashMap<String, String> extended_attributes;

    // Added for convenience after parsing
    String getter_callback_name;
    String setter_callback_name;
};

struct DictionaryMember {
    bool required { false };
    NonnullRefPtr<Type> type;
    String name;
    HashMap<String, String> extended_attributes;
    Optional<String> default_value;
};

struct Dictionary {
    String parent_name;
    Vector<DictionaryMember> members;
};

struct Typedef {
    HashMap<String, String> extended_attributes;
    NonnullRefPtr<Type> type;
};

struct Enumeration {
    HashTable<String> values;
    HashMap<String, String> translated_cpp_names;
    String first_member;
    bool is_original_definition { true };
};

struct CallbackFunction {
    NonnullRefPtr<Type> return_type;
    Vector<Parameter> parameters;
    bool is_legacy_treat_non_object_as_null { false };
};

class Interface;

class ParameterizedType : public Type {
public:
    ParameterizedType(String name, bool nullable, NonnullRefPtrVector<Type> parameters)
        : Type(Kind::Parameterized, move(name), nullable)
        , m_parameters(move(parameters))
    {
    }

    virtual ~ParameterizedType() override = default;

    void generate_sequence_from_iterable(SourceGenerator& generator, String const& cpp_name, String const& iterable_cpp_name, String const& iterator_method_cpp_name, IDL::Interface const&, size_t recursion_depth) const;

    NonnullRefPtrVector<Type> const& parameters() const { return m_parameters; }
    NonnullRefPtrVector<Type>& parameters() { return m_parameters; }

private:
    NonnullRefPtrVector<Type> m_parameters;
};

static inline size_t get_shortest_function_length(Vector<Function&> const& overload_set)
{
    size_t shortest_length = SIZE_MAX;
    for (auto const& function : overload_set)
        shortest_length = min(function.shortest_length(), shortest_length);
    return shortest_length;
}

class Interface {
    AK_MAKE_NONCOPYABLE(Interface);
    AK_MAKE_NONMOVABLE(Interface);

public:
    explicit Interface() = default;

    String name;
    String parent_name;

    bool is_mixin { false };

    HashMap<String, String> extended_attributes;

    Vector<Attribute> attributes;
    Vector<Constant> constants;
    Vector<Constructor> constructors;
    Vector<Function> functions;
    Vector<Function> static_functions;
    bool has_stringifier { false };
    Optional<String> stringifier_attribute;
    bool has_unscopable_member { false };

    Optional<NonnullRefPtr<Type>> value_iterator_type;
    Optional<Tuple<NonnullRefPtr<Type>, NonnullRefPtr<Type>>> pair_iterator_types;

    Optional<Function> named_property_getter;
    Optional<Function> named_property_setter;

    Optional<Function> indexed_property_getter;
    Optional<Function> indexed_property_setter;

    Optional<Function> named_property_deleter;

    HashMap<String, Dictionary> dictionaries;
    HashMap<String, Enumeration> enumerations;
    HashMap<String, Typedef> typedefs;
    HashMap<String, Interface*> mixins;
    HashMap<String, CallbackFunction> callback_functions;

    // Added for convenience after parsing
    String wrapper_class;
    String wrapper_base_class;
    String fully_qualified_name;
    String constructor_class;
    String prototype_class;
    String prototype_base_class;
    HashMap<String, HashTable<String>> included_mixins;

    String module_own_path;
    HashTable<String> required_imported_paths;
    Vector<Interface&> imported_modules;

    HashMap<String, Vector<Function&>> overload_sets;
    HashMap<String, Vector<Function&>> static_overload_sets;

    // https://webidl.spec.whatwg.org/#dfn-support-indexed-properties
    bool supports_indexed_properties() const { return indexed_property_getter.has_value(); }

    // https://webidl.spec.whatwg.org/#dfn-support-named-properties
    bool supports_named_properties() const { return named_property_getter.has_value(); }

    // https://webidl.spec.whatwg.org/#dfn-legacy-platform-object
    bool is_legacy_platform_object() const { return !extended_attributes.contains("Global") && (supports_indexed_properties() || supports_named_properties()); }

    bool will_generate_code() const
    {
        return !name.is_empty() || any_of(enumerations, [](auto& entry) { return entry.value.is_original_definition; });
    }
};

class UnionType : public Type {
public:
    UnionType(String name, bool nullable, NonnullRefPtrVector<Type> member_types)
        : Type(Kind::Union, move(name), nullable)
        , m_member_types(move(member_types))
    {
    }

    virtual ~UnionType() override = default;

    NonnullRefPtrVector<Type> const& member_types() const { return m_member_types; }
    NonnullRefPtrVector<Type>& member_types() { return m_member_types; }

    // https://webidl.spec.whatwg.org/#dfn-flattened-union-member-types
    NonnullRefPtrVector<Type> flattened_member_types() const
    {
        // 1. Let T be the union type.

        // 2. Initialize S to ∅.
        NonnullRefPtrVector<Type> types;

        // 3. For each member type U of T:
        for (auto& type : m_member_types) {
            // FIXME: 1. If U is an annotated type, then set U to be the inner type of U.

            // 2. If U is a nullable type, then set U to be the inner type of U. (NOTE: Not necessary as nullable is stored with Type and not as a separate struct)

            // 3. If U is a union type, then add to S the flattened member types of U.
            if (type.is_union()) {
                auto& union_member_type = type.as_union();
                types.extend(union_member_type.flattened_member_types());
            } else {
                // 4. Otherwise, U is not a union type. Add U to S.
                types.append(type);
            }
        }

        // 4. Return S.
        return types;
    }

    // https://webidl.spec.whatwg.org/#dfn-number-of-nullable-member-types
    size_t number_of_nullable_member_types() const
    {
        // 1. Let T be the union type.

        // 2. Initialize n to 0.
        size_t num_nullable_member_types = 0;

        // 3. For each member type U of T:
        for (auto& type : m_member_types) {
            // 1. If U is a nullable type, then:
            if (type.is_nullable()) {
                // 1. Set n to n + 1.
                ++num_nullable_member_types;

                // 2. Set U to be the inner type of U. (NOTE: Not necessary as nullable is stored with Type and not as a separate struct)
            }

            // 2. If U is a union type, then:
            if (type.is_union()) {
                auto& union_member_type = type.as_union();

                // 1. Let m be the number of nullable member types of U.
                // 2. Set n to n + m.
                num_nullable_member_types += union_member_type.number_of_nullable_member_types();
            }
        }

        // 4. Return n.
        return num_nullable_member_types;
    }

private:
    NonnullRefPtrVector<Type> m_member_types;
};

}
