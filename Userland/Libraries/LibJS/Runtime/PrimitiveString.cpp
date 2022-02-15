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

PrimitiveString::PrimitiveString(String string)
    : m_utf8_string(move(string))
    , m_has_utf8_string(true)
{
}

PrimitiveString::PrimitiveString(Utf16String string)
    : m_utf16_string(move(string))
    , m_has_utf16_string(true)
{
}

PrimitiveString::~PrimitiveString()
{
    vm().string_cache().remove(m_utf8_string);
}

String const& PrimitiveString::string() const
{
    if (!m_has_utf8_string) {
        m_utf8_string = m_utf16_string.to_utf8();
        m_has_utf8_string = true;
    }
    return m_utf8_string;
}

Utf16String const& PrimitiveString::utf16_string() const
{
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

Optional<Value> PrimitiveString::get(GlobalObject& global_object, PropertyKey const& property_key) const
{
    if (property_key.is_symbol())
        return {};
    if (property_key.is_string()) {
        if (property_key.as_string() == global_object.vm().names.length.as_string()) {
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
    return js_string(vm(), str.substring_view(index.as_index(), 1));
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

    return heap.allocate_without_global_object<PrimitiveString>(move(string));
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
        auto* new_string = heap.allocate_without_global_object<PrimitiveString>(string);
        string_cache.set(move(string), new_string);
        return new_string;
    }
    return it->value;
}

PrimitiveString* js_string(VM& vm, String string)
{
    return js_string(vm.heap(), move(string));
}

}
