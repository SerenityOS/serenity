/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Utf16View.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

PrimitiveString::PrimitiveString(String string)
    : m_utf8_string(move(string))
    , m_has_utf8_string(true)
{
}

PrimitiveString::PrimitiveString(Vector<u16> string)
    : m_utf16_string(move(string))
    , m_has_utf16_string(true)
{
}

PrimitiveString::~PrimitiveString()
{
}

String const& PrimitiveString::string() const
{
    if (!m_has_utf8_string) {
        m_utf8_string = utf16_string_view().to_utf8(Utf16View::AllowInvalidCodeUnits::Yes);
        m_has_utf8_string = true;
    }
    return m_utf8_string;
}

Vector<u16> const& PrimitiveString::utf16_string() const
{
    if (!m_has_utf16_string) {
        m_utf16_string = AK::utf8_to_utf16(m_utf8_string);
        m_has_utf16_string = true;
    }
    return m_utf16_string;
}

Utf16View PrimitiveString::utf16_string_view() const
{
    return Utf16View { utf16_string() };
}

PrimitiveString* js_string(Heap& heap, Utf16View const& view)
{
    if (view.is_empty())
        return &heap.vm().empty_string();

    if (view.length_in_code_units() == 1) {
        u16 code_unit = view.code_unit_at(0);
        if (is_ascii(code_unit))
            return &heap.vm().single_ascii_character_string(static_cast<u8>(code_unit));
    }

    Vector<u16> string;
    string.ensure_capacity(view.length_in_code_units());
    string.append(view.data(), view.length_in_code_units());
    return js_string(heap, move(string));
}

PrimitiveString* js_string(VM& vm, Utf16View const& view)
{
    return js_string(vm.heap(), view);
}

PrimitiveString* js_string(Heap& heap, Vector<u16> string)
{
    if (string.is_empty())
        return &heap.vm().empty_string();

    if (string.size() == 1) {
        u16 code_unit = string.at(0);
        if (is_ascii(code_unit))
            return &heap.vm().single_ascii_character_string(static_cast<u8>(code_unit));
    }

    return heap.allocate_without_global_object<PrimitiveString>(move(string));
}

PrimitiveString* js_string(VM& vm, Vector<u16> string)
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

    return heap.allocate_without_global_object<PrimitiveString>(move(string));
}

PrimitiveString* js_string(VM& vm, String string)
{
    return js_string(vm.heap(), move(string));
}

}
