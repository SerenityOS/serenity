/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <LibGfx/TextDirection.h>

namespace Gfx {

// FIXME: These should be parsed from the official UnicodeData.txt that specifies the class for each character (this function doesn't take into account a large amount of characters)
static consteval Array<BidirectionalClass, 0x1F000> generate_char_bidi_class_lookup_table()
{
    Array<BidirectionalClass, 0x1F000> lookup_table {};
    for (u32 ch = 0; ch < 0x1F000; ch++) {
        auto char_class = BidirectionalClass::STRONG_LTR;
        if ((ch >= 0x600 && ch <= 0x7BF) || (ch >= 0x8A0 && ch <= 0x8FF) || (ch >= 0xFB50 && ch <= 0xFDCF) || (ch >= 0xFDF0 && ch <= 0xFDFF) || (ch >= 0xFE70 && ch <= 0xFEFF) || (ch >= 0x1EE00 && ch <= 0x1EEFF))
            char_class = BidirectionalClass::STRONG_RTL; // Arabic RTL
        if ((ch >= 0x590 && ch <= 0x5FF) || (ch >= 0x7C0 && ch <= 0x89F) || (ch == 0x200F) || (ch >= 0xFB1D && ch <= 0xFB4F) || (ch >= 0x10800 && ch <= 0x10FFF) || (ch >= 0x1E800 && ch <= 0x1EDFF) || (ch >= 0x1EF00 && ch <= 0x1EFFF))
            char_class = BidirectionalClass::STRONG_RTL; // Hebrew RTL
        if ((ch >= 0x30 && ch <= 0x39) || (ch >= 0x660 && ch <= 0x669) || (ch >= 0x10D30 && ch <= 0x10E7E))
            char_class = BidirectionalClass::WEAK_NUMBERS; // Numerals
        if ((ch >= 0x23 && ch <= 0x25) || (ch >= 0x2B && ch <= 0x2F) || (ch == 0x3A))
            char_class = BidirectionalClass::WEAK_SEPARATORS; // Separators
        if ((ch >= 0x9 && ch <= 0xD) || (ch >= 0x1C && ch <= 0x22) || (ch >= 0x26 && ch <= 0x2A) || (ch >= 0x3B && ch <= 0x40) || (ch >= 0x5B && ch <= 0x60) || (ch >= 0x7B && ch <= 0x7E))
            char_class = BidirectionalClass::NEUTRAL;
        lookup_table[ch] = char_class;
    }
    return lookup_table;
}
constexpr Array<BidirectionalClass, 0x1F000> char_bidi_class_lookup_table = generate_char_bidi_class_lookup_table();

}
