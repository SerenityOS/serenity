/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibIDL/Types.h>

namespace IDL {

ParameterizedType const& Type::as_parameterized() const
{
    return verify_cast<ParameterizedType const>(*this);
}

ParameterizedType& Type::as_parameterized()
{
    return verify_cast<ParameterizedType>(*this);
}

UnionType const& Type::as_union() const
{
    return verify_cast<UnionType const>(*this);
}

UnionType& Type::as_union()
{
    return verify_cast<UnionType>(*this);
}

// https://webidl.spec.whatwg.org/#dfn-includes-a-nullable-type
bool Type::includes_nullable_type() const
{
    // A type includes a nullable type if:
    // - the type is a nullable type, or
    if (is_nullable())
        return true;

    // FIXME: - the type is an annotated type and its inner type is a nullable type, or

    // - the type is a union type and its number of nullable member types is 1.
    if (is_union() && as_union().number_of_nullable_member_types() == 1)
        return true;

    return false;
}

// https://webidl.spec.whatwg.org/#dfn-includes-undefined
bool Type::includes_undefined() const
{
    // A type includes undefined if:
    // - the type is undefined, or
    if (is_undefined())
        return true;

    // - the type is a nullable type and its inner type includes undefined, or
    //   NOTE: We don't treat nullable as its own type, so this is handled by the other cases.

    // FIXME: - the type is an annotated type and its inner type includes undefined, or

    // - the type is a union type and one of its member types includes undefined.
    if (is_union()) {
        for (auto& type : as_union().member_types()) {
            if (type.includes_undefined())
                return true;
        }
    }

    return false;
}

// https://webidl.spec.whatwg.org/#dfn-distinguishable
bool Type::is_distinguishable_from(IDL::Type const& other) const
{
    // 1. If one type includes a nullable type and the other type either includes a nullable type,
    //    is a union type with flattened member types including a dictionary type, or is a dictionary type,
    //    return false.
    // FIXME: "is a union type with flattened member types including a dictionary type, or is a dictionary type,"
    if (includes_nullable_type() && other.includes_nullable_type())
        return false;

    // 2. If both types are either a union type or nullable union type, return true if each member type
    //    of the one is distinguishable with each member type of the other, or false otherwise.
    if (is_union() && other.is_union()) {
        auto const& this_union = as_union();
        auto const& other_union = other.as_union();

        for (auto& this_member_type : this_union.member_types()) {
            for (auto& other_member_type : other_union.member_types()) {
                if (!this_member_type.is_distinguishable_from(other_member_type))
                    return false;
            }
        }
        return true;
    }

    // 3. If one type is a union type or nullable union type, return true if each member type of the union
    //    type is distinguishable with the non-union type, or false otherwise.
    if (is_union() || other.is_union()) {
        auto const& the_union = is_union() ? as_union() : other.as_union();
        auto const& non_union = is_union() ? other : *this;

        for (auto& member_type : the_union.member_types()) {
            if (!non_union.is_distinguishable_from(member_type))
                return false;
        }
        return true;
    }

    // 4. Consider the two "innermost" types derived by taking each type’s inner type if it is an annotated type,
    //    and then taking its inner type inner type if the result is a nullable type. If these two innermost types
    //    appear or are in categories appearing in the following table and there is a “●” mark in the corresponding
    //    entry or there is a letter in the corresponding entry and the designated additional requirement below the
    //    table is satisfied, then return true. Otherwise return false.
    auto const& this_innermost_type = innermost_type();
    auto const& other_innermost_type = other.innermost_type();

    enum class DistinguishabilityCategory {
        Undefined,
        Boolean,
        Numeric,
        BigInt,
        String,
        Object,
        Symbol,
        InterfaceLike,
        CallbackFunction,
        DictionaryLike,
        SequenceLike,
        __Count
    };

    // See https://webidl.spec.whatwg.org/#distinguishable-table
    // clang-format off
    static constexpr bool table[to_underlying(DistinguishabilityCategory::__Count)][to_underlying(DistinguishabilityCategory::__Count)] {
        {false,  true,  true,  true,  true,  true,  true,  true,  true, false,  true},
        { true, false,  true,  true,  true,  true,  true,  true,  true,  true,  true},
        { true,  true, false,  true,  true,  true,  true,  true,  true,  true,  true},
        { true,  true,  true, false,  true,  true,  true,  true,  true,  true,  true},
        { true,  true,  true,  true, false,  true,  true,  true,  true,  true,  true},
        { true,  true,  true,  true,  true, false,  true, false, false, false, false},
        { true,  true,  true,  true,  true,  true, false,  true,  true,  true,  true},
        { true,  true,  true,  true,  true, false,  true, false,  true,  true,  true},
        { true,  true,  true,  true,  true, false,  true,  true, false, false,  true},
        {false,  true,  true,  true,  true, false,  true,  true, false, false,  true},
        { true,  true,  true,  true,  true, false,  true,  true,  true,  true, false},
    };
    // clang-format on

    auto determine_category = [](Type const& type) -> DistinguishabilityCategory {
        if (type.is_undefined())
            return DistinguishabilityCategory::Undefined;
        if (type.is_boolean())
            return DistinguishabilityCategory::Boolean;
        if (type.is_numeric())
            return DistinguishabilityCategory::Numeric;
        if (type.is_bigint())
            return DistinguishabilityCategory::BigInt;
        if (type.is_string())
            return DistinguishabilityCategory::String;
        if (type.is_object())
            return DistinguishabilityCategory::Object;
        if (type.is_symbol())
            return DistinguishabilityCategory::Symbol;
        // FIXME: InterfaceLike - see below
        // FIXME: CallbackFunction
        // FIXME: DictionaryLike
        // FIXME: Frozen array types are included in "sequence-like"
        if (type.is_sequence())
            return DistinguishabilityCategory::SequenceLike;

        // FIXME: For lack of a better way of determining if something is an interface type, this just assumes anything we don't recognise is one.
        dbgln("Unable to determine category for type named '{}', assuming it's an interface type.", type.name());
        return DistinguishabilityCategory::InterfaceLike;
    };

    auto this_distinguishability = determine_category(this_innermost_type);
    auto other_distinguishability = determine_category(other_innermost_type);

    if (this_distinguishability == DistinguishabilityCategory::InterfaceLike && other_distinguishability == DistinguishabilityCategory::InterfaceLike) {
        // Two interface-likes are distinguishable if:
        // "The two identified interface-like types are not the same, and no single platform object
        // implements both interface-like types."
        // FIXME: Implement this.
        return false;
    }

    return table[to_underlying(this_distinguishability)][to_underlying(other_distinguishability)];
}

// https://webidl.spec.whatwg.org/#dfn-distinguishing-argument-index
int EffectiveOverloadSet::distinguishing_argument_index()
{
    for (auto argument_index = 0u; argument_index < m_argument_count; ++argument_index) {
        bool found_indistinguishable = false;

        for (auto first_item_index = 0u; first_item_index < m_items.size(); ++first_item_index) {
            for (auto second_item_index = first_item_index + 1; second_item_index < m_items.size(); ++second_item_index) {
                if (!m_items[first_item_index].types[argument_index].is_distinguishable_from(m_items[second_item_index].types[argument_index])) {
                    found_indistinguishable = true;
                    break;
                }
            }
            if (found_indistinguishable)
                break;
        }

        if (!found_indistinguishable)
            return argument_index;
    }

    VERIFY_NOT_REACHED();
}

void EffectiveOverloadSet::remove_all_other_entries()
{
    m_items.remove_all_matching([this](auto const& item) {
        return &item != m_last_matching_item;
    });
}

}
