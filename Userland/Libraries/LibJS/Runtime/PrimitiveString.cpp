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
    : m_string(move(string))
{
}

PrimitiveString::~PrimitiveString()
{
}

Vector<u16> const& PrimitiveString::utf16_string() const
{
    if (m_utf16_string.is_empty() && !m_string.is_empty())
        m_utf16_string = AK::utf8_to_utf16(m_string);
    return m_utf16_string;
}

Utf16View PrimitiveString::utf16_string_view() const
{
    return Utf16View { utf16_string() };
}

PrimitiveString* js_string(Heap& heap, Utf16View const& string)
{
    if (string.is_empty())
        return &heap.vm().empty_string();

    if (string.length_in_code_units() == 1) {
        u16 code_unit = string.code_unit_at(0);
        if (is_ascii(code_unit))
            return &heap.vm().single_ascii_character_string(static_cast<u8>(code_unit));
    }

    auto utf8_string = string.to_utf8(Utf16View::AllowInvalidCodeUnits::Yes);
    return heap.allocate_without_global_object<PrimitiveString>(move(utf8_string));
}

PrimitiveString* js_string(VM& vm, Utf16View const& string)
{
    return js_string(vm.heap(), string);
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

    // UTF-8 strings must first be transcoded to UTF-16, even though they are stored as String objects
    // internally, to parse encoded surrogate pairs. As an optimization to reduce string copying, only
    // perform that transcoding if there are non-ASCII codepoints in the string.
    for (auto it : string) {
        auto ch = static_cast<u8>(it);
        if (!is_ascii(ch)) {
            auto utf16_string = AK::utf8_to_utf16(string);
            return js_string(heap, Utf16View { utf16_string });
        }
    }

    return heap.allocate_without_global_object<PrimitiveString>(move(string));
}

PrimitiveString* js_string(VM& vm, String string)
{
    return js_string(vm.heap(), move(string));
}

}
