/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
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
static size_t get_function_length(FunctionType& function)
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

struct Type : public RefCounted<Type> {
    Type() = default;

    Type(String name, bool nullable)
        : name(move(name))
        , nullable(nullable)
    {
    }

    virtual ~Type() = default;

    String name;
    bool nullable { false };
    bool is_string() const { return name.is_one_of("ByteString", "CSSOMString", "DOMString", "USVString"); }

    // https://webidl.spec.whatwg.org/#dfn-integer-type
    bool is_integer() const { return name.is_one_of("byte", "octet", "short", "unsigned short", "long", "unsigned long", "long long", "unsigned long long"); }

    // https://webidl.spec.whatwg.org/#dfn-numeric-type
    bool is_numeric() const { return is_integer() || name.is_one_of("float", "unrestricted float", "double", "unrestricted double"); }

    // https://webidl.spec.whatwg.org/#dfn-primitive-type
    bool is_primitive() const { return is_numeric() || name.is_one_of("bigint", "boolean"); }
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

    size_t length() const { return get_function_length(*this); }
};

struct Constructor {
    String name;
    Vector<Parameter> parameters;

    size_t length() const { return get_function_length(*this); }
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

struct Interface;

struct ParameterizedType : public Type {
    ParameterizedType() = default;

    ParameterizedType(String name, bool nullable, NonnullRefPtrVector<Type> parameters)
        : Type(move(name), nullable)
        , parameters(move(parameters))
    {
    }

    virtual ~ParameterizedType() override = default;

    NonnullRefPtrVector<Type> parameters;

    void generate_sequence_from_iterable(SourceGenerator& generator, String const& cpp_name, String const& iterable_cpp_name, String const& iterator_method_cpp_name, IDL::Interface const&, size_t recursion_depth) const;
};

static inline size_t get_shortest_function_length(Vector<Function&> const& overload_set)
{
    size_t longest_length = SIZE_MAX;
    for (auto const& function : overload_set)
        longest_length = min(function.length(), longest_length);
    return longest_length;
}

struct Interface : public RefCounted<Interface> {
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
    HashMap<String, NonnullRefPtr<Interface>> mixins;
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
    NonnullRefPtrVector<Interface> imported_modules;

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

CppType idl_type_name_to_cpp_type(Type const& type, IDL::Interface const& interface);

struct UnionType : public Type {
    UnionType() = default;

    UnionType(String name, bool nullable, NonnullRefPtrVector<Type> member_types)
        : Type(move(name), nullable)
        , member_types(move(member_types))
    {
    }

    virtual ~UnionType() override = default;

    NonnullRefPtrVector<Type> member_types;

    // https://webidl.spec.whatwg.org/#dfn-flattened-union-member-types
    NonnullRefPtrVector<Type> flattened_member_types() const
    {
        // 1. Let T be the union type.

        // 2. Initialize S to ∅.
        NonnullRefPtrVector<Type> types;

        // 3. For each member type U of T:
        for (auto& type : member_types) {
            // FIXME: 1. If U is an annotated type, then set U to be the inner type of U.

            // 2. If U is a nullable type, then set U to be the inner type of U. (NOTE: Not necessary as nullable is stored with Type and not as a separate struct)

            // 3. If U is a union type, then add to S the flattened member types of U.
            if (is<UnionType>(type)) {
                auto& union_member_type = verify_cast<UnionType>(type);
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
        for (auto& type : member_types) {
            // 1. If U is a nullable type, then:
            if (type.nullable) {
                // 1. Set n to n + 1.
                ++num_nullable_member_types;

                // 2. Set U to be the inner type of U. (NOTE: Not necessary as nullable is stored with Type and not as a separate struct)
            }

            // 2. If U is a union type, then:
            if (is<UnionType>(type)) {
                auto& union_member_type = verify_cast<UnionType>(type);

                // 1. Let m be the number of nullable member types of U.
                // 2. Set n to n + m.
                num_nullable_member_types += union_member_type.number_of_nullable_member_types();
            }
        }

        // 4. Return n.
        return num_nullable_member_types;
    }

    // https://webidl.spec.whatwg.org/#dfn-includes-a-nullable-type
    bool includes_nullable_type() const
    {
        // -> the type is a union type and its number of nullable member types is 1.
        return number_of_nullable_member_types() == 1;
    }

    // -> https://webidl.spec.whatwg.org/#dfn-includes-undefined
    bool includes_undefined() const
    {
        // -> the type is a union type and one of its member types includes undefined.
        for (auto& type : member_types) {
            if (is<UnionType>(type)) {
                auto& union_type = verify_cast<UnionType>(type);
                if (union_type.includes_undefined())
                    return true;
            }

            if (type.name == "undefined"sv)
                return true;
        }
        return false;
    }

    String to_variant(IDL::Interface const& interface) const
    {
        StringBuilder builder;
        builder.append("Variant<");

        auto flattened_types = flattened_member_types();
        for (size_t type_index = 0; type_index < flattened_types.size(); ++type_index) {
            auto& type = flattened_types.at(type_index);

            if (type_index > 0)
                builder.append(", ");

            auto cpp_type = idl_type_name_to_cpp_type(type, interface);
            builder.append(cpp_type.name);
        }

        if (includes_undefined())
            builder.append(", Empty");

        builder.append('>');
        return builder.to_string();
    }
};

}
