/*
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/UnicodeUtils.h>

namespace AK::UnicodeUtils {

Optional<StringView> get_unicode_control_code_point_alias(u32 code_point)
{
    static constexpr Array<StringView, 32> ascii_controls_lookup_table = {
        "NUL", "SOH", "STX", "ETX", "EOT", "ENQ", "ACK", "BEL",
        "BS", "HT", "LF", "VT", "FF", "CR", "SO", "SI",
        "DLE", "DC1", "DC2", "DC3", "DC4", "NAK", "SYN", "ETB",
        "CAN", "EM", "SUB", "ESC", "FS", "GS", "RS", "US"
    };

    static constexpr Array<StringView, 32> c1_controls_lookup_table = {
        "XXX", "XXX", "BPH", "NBH", "IND", "NEL", "SSA", "ESA",
        "HTS", "HTJ", "VTS", "PLD", "PLU", "RI", "SS2", "SS3",
        "DCS", "PU1", "PU2", "STS", "CCH", "MW", "SPA", "EPA",
        "SOS", "XXX", "SCI", "CSI", "ST", "OSC", "PM", "APC"
    };

    if (code_point < 0x20)
        return ascii_controls_lookup_table[code_point];
    if (code_point >= 0x80 && code_point < 0xa0)
        return c1_controls_lookup_table[code_point - 0x80];
    return {};
}

}
