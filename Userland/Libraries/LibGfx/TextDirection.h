/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Utf32View.h>
#include <AK/Vector.h>

namespace Gfx {

enum class BidirectionalClass {
    STRONG_LTR,
    STRONG_RTL,
    WEAK_NUMBERS,
    WEAK_SEPARATORS,
    NEUTRAL,
};

extern Array<BidirectionalClass, 0x1F000> const char_bidi_class_lookup_table;

constexpr BidirectionalClass get_char_bidi_class(u32 ch)
{
    if (ch >= char_bidi_class_lookup_table.size())
        return BidirectionalClass::STRONG_LTR;
    return char_bidi_class_lookup_table[ch];
}

// FIXME: These should be parsed from the official BidiMirroring.txt that specifies the mirroring character for each character (this function doesn't take into account a large amount of characters)
constexpr u32 get_mirror_char(u32 ch)
{
    if (ch == 0x28)
        return 0x29;
    if (ch == 0x29)
        return 0x28;
    if (ch == 0x3C)
        return 0x3E;
    if (ch == 0x3E)
        return 0x3C;
    if (ch == 0x5B)
        return 0x5D;
    if (ch == 0x7B)
        return 0x7D;
    if (ch == 0x7D)
        return 0x7B;
    if (ch == 0xAB)
        return 0xBB;
    if (ch == 0xBB)
        return 0xAB;
    if (ch == 0x2039)
        return 0x203A;
    if (ch == 0x203A)
        return 0x2039;
    return ch;
}

enum class TextDirection {
    LTR,
    RTL,
};

constexpr TextDirection bidi_class_to_direction(BidirectionalClass class_)
{
    VERIFY(class_ != BidirectionalClass::NEUTRAL);
    if (class_ == BidirectionalClass::STRONG_LTR || class_ == BidirectionalClass::WEAK_NUMBERS || class_ == BidirectionalClass::WEAK_SEPARATORS)
        return TextDirection::LTR;
    return TextDirection::RTL;
}

// Assumes the text has a homogeneous direction
template<typename TextType>
constexpr TextDirection get_text_direction(TextType text)
{
    for (u32 code_point : text) {
        auto char_direction = get_char_bidi_class(code_point);
        if (char_direction != BidirectionalClass::NEUTRAL)
            return bidi_class_to_direction(char_direction);
    }
    return TextDirection::LTR;
}

class DirectionalRun {
public:
    DirectionalRun(Vector<u32> code_points, u8 embedding_level)
        : m_code_points(move(code_points))
        , m_embedding_level(embedding_level)
    {
    }

    [[nodiscard]] Utf32View text() const { return { m_code_points.data(), m_code_points.size() }; }
    [[nodiscard]] u8 embedding_level() const { return m_embedding_level; }
    [[nodiscard]] TextDirection direction() const { return (m_embedding_level % 2) == 0 ? TextDirection::LTR : TextDirection::RTL; }

    Vector<u32>& code_points() { return m_code_points; }

private:
    Vector<u32> m_code_points;
    u8 m_embedding_level;
};

}
