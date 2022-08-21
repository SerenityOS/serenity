/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Utf16View.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/PropertyKey.h>
#include <LibJS/Runtime/VM.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

PrimitiveString::PrimitiveString(PrimitiveString& lhs, PrimitiveString& rhs)
    : m_is_rope(true)
    , m_lhs(&lhs)
    , m_rhs(&rhs)
{
}

PrimitiveString::PrimitiveString(String string)
    : m_has_utf8_string(true)
    , m_utf8_string(move(string))
{
}

PrimitiveString::PrimitiveString(Utf16String string)
    : m_has_utf16_string(true)
    , m_utf16_string(move(string))
{
}

PrimitiveString::~PrimitiveString()
{
    vm().string_cache().remove(m_utf8_string);
}

void PrimitiveString::visit_edges(Cell::Visitor& visitor)
{
    Cell::visit_edges(visitor);
    if (m_is_rope) {
        visitor.visit(m_lhs);
        visitor.visit(m_rhs);
    }
}

bool PrimitiveString::is_empty() const
{
    if (m_is_rope) {
        // NOTE: We never make an empty rope string.
        return false;
    }

    if (m_has_utf16_string)
        return m_utf16_string.is_empty();
    if (m_has_utf8_string)
        return m_utf8_string.is_empty();
    VERIFY_NOT_REACHED();
}

String const& PrimitiveString::string() const
{
    resolve_rope_if_needed();
    if (!m_has_utf8_string) {
        m_utf8_string = m_utf16_string.to_utf8();
        m_has_utf8_string = true;
    }
    return m_utf8_string;
}

Utf16String const& PrimitiveString::utf16_string() const
{
    resolve_rope_if_needed();
    if (!m_has_utf16_string) {
        m_utf16_string = Utf16String(m_utf8_string);
        m_has_utf16_string = true;
    }
    return m_utf16_string;
}

Utf16View PrimitiveString::utf16_string_view() const
{
    return utf16_string().view();
}

Optional<Value> PrimitiveString::get(VM& vm, PropertyKey const& property_key) const
{
    if (property_key.is_symbol())
        return {};
    if (property_key.is_string()) {
        if (property_key.as_string() == vm.names.length.as_string()) {
            auto length = utf16_string().length_in_code_units();
            return Value(static_cast<double>(length));
        }
    }
    auto index = canonical_numeric_index_string(property_key, CanonicalIndexMode::IgnoreNumericRoundtrip);
    if (!index.is_index())
        return {};
    auto str = utf16_string_view();
    auto length = str.length_in_code_units();
    if (length <= index.as_index())
        return {};
    return js_string(vm, str.substring_view(index.as_index(), 1));
}

PrimitiveString* js_string(Heap& heap, Utf16View const& view)
{
    return js_string(heap, Utf16String(view));
}

PrimitiveString* js_string(VM& vm, Utf16View const& view)
{
    return js_string(vm.heap(), view);
}

PrimitiveString* js_string(Heap& heap, Utf16String string)
{
    if (string.is_empty())
        return &heap.vm().empty_string();

    if (string.length_in_code_units() == 1) {
        u16 code_unit = string.code_unit_at(0);
        if (is_ascii(code_unit))
            return &heap.vm().single_ascii_character_string(static_cast<u8>(code_unit));
    }

    return heap.allocate_without_realm<PrimitiveString>(move(string));
}

PrimitiveString* js_string(VM& vm, Utf16String string)
{
    return js_string(vm.heap(), move(string));
}

PrimitiveString* js_string(Heap& heap, String string)
{
    if (string.is_empty())
        return &heap.vm().empty_string();

    if (string.length() == 1) {
        auto ch = static_cast<u8>(string.characters()[0]);
        if (is_ascii(ch))
            return &heap.vm().single_ascii_character_string(ch);
    }

    auto& string_cache = heap.vm().string_cache();
    auto it = string_cache.find(string);
    if (it == string_cache.end()) {
        auto* new_string = heap.allocate_without_realm<PrimitiveString>(string);
        string_cache.set(move(string), new_string);
        return new_string;
    }
    return it->value;
}

PrimitiveString* js_string(VM& vm, String string)
{
    return js_string(vm.heap(), move(string));
}

PrimitiveString* js_rope_string(VM& vm, PrimitiveString& lhs, PrimitiveString& rhs)
{
    // We're here to concatenate two strings into a new rope string.
    // However, if any of them are empty, no rope is required.

    bool lhs_empty = lhs.is_empty();
    bool rhs_empty = rhs.is_empty();

    if (lhs_empty && rhs_empty)
        return &vm.empty_string();

    if (lhs_empty)
        return &rhs;

    if (rhs_empty)
        return &lhs;

    return vm.heap().allocate_without_realm<PrimitiveString>(lhs, rhs);
}

void PrimitiveString::resolve_rope_if_needed() const
{
    if (!m_is_rope)
        return;

    // NOTE: Special case for two concatenated UTF-16 strings.
    //       This is here as an optimization, although I'm unsure how valuable it is.
    if (m_lhs->has_utf16_string() && m_rhs->has_utf16_string()) {
        auto const& lhs_string = m_lhs->utf16_string();
        auto const& rhs_string = m_rhs->utf16_string();

        Vector<u16, 1> combined;
        combined.ensure_capacity(lhs_string.length_in_code_units() + rhs_string.length_in_code_units());
        combined.extend(lhs_string.string());
        combined.extend(rhs_string.string());

        m_utf16_string = Utf16String(move(combined));
        m_has_utf16_string = true;
        m_is_rope = false;
        m_lhs = nullptr;
        m_rhs = nullptr;
        return;
    }

    // This vector will hold all the pieces of the rope that need to be assembled
    // into the resolved string.
    Vector<PrimitiveString const*> pieces;

    // NOTE: We traverse the rope tree without using recursion, since we'd run out of
    //       stack space quickly when handling a long sequence of unresolved concatenations.
    Vector<PrimitiveString const*> stack;
    stack.append(m_rhs);
    stack.append(m_lhs);
    while (!stack.is_empty()) {
        auto* current = stack.take_last();
        if (current->m_is_rope) {
            stack.append(current->m_rhs);
            stack.append(current->m_lhs);
            continue;
        }
        pieces.append(current);
    }

    // Now that we have all the pieces, we can concatenate them using a StringBuilder.
    StringBuilder builder;

    // We keep track of the previous piece in order to handle surrogate pairs spread across two pieces.
    PrimitiveString const* previous = nullptr;
    for (auto const* current : pieces) {
        if (!previous) {
            // This is the very first piece, just append it and continue.
            builder.append(current->string());
            previous = current;
            continue;
        }

        // Get the UTF-8 representations for both strings.
        auto const& previous_string_as_utf8 = previous->string();
        auto const& current_string_as_utf8 = current->string();

        // NOTE: Now we need to look at the end of the previous string and the start
        //       of the current string, to see if they should be combined into a surrogate.

        // Surrogates encoded as UTF-8 are 3 bytes.
        if ((previous_string_as_utf8.length() < 3) || (current_string_as_utf8.length() < 3)) {
            builder.append(current->string());
            previous = current;
            continue;
        }

        // Might the previous string end with a UTF-8 encoded surrogate?
        if ((static_cast<u8>(previous_string_as_utf8[previous_string_as_utf8.length() - 3]) & 0xf0) != 0xe0) {
            // If not, just append the current string and continue.
            builder.append(current->string());
            previous = current;
            continue;
        }

        // Might the current string begin with a UTF-8 encoded surrogate?
        if ((static_cast<u8>(current_string_as_utf8[0]) & 0xf0) != 0xe0) {
            // If not, just append the current string and continue.
            builder.append(current->string());
            previous = current;
            continue;
        }

        auto high_surrogate = *Utf8View(previous_string_as_utf8.substring_view(previous_string_as_utf8.length() - 3)).begin();
        auto low_surrogate = *Utf8View(current_string_as_utf8).begin();

        if (!Utf16View::is_high_surrogate(high_surrogate) || !Utf16View::is_low_surrogate(low_surrogate)) {
            builder.append(current->string());
            previous = current;
            continue;
        }

        // Remove 3 bytes from the builder and replace them with the UTF-8 encoded code point.
        builder.trim(3);
        builder.append_code_point(Utf16View::decode_surrogate_pair(high_surrogate, low_surrogate));

        // Append the remaining part of the current string.
        builder.append(current_string_as_utf8.substring_view(3));
        previous = current;
    }

    m_utf8_string = builder.to_string();
    m_has_utf8_string = true;
    m_is_rope = false;
    m_lhs = nullptr;
    m_rhs = nullptr;
}

}
