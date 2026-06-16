/*
 * Copyright (c) 2026, Eduardo Casadei <educasadei@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ANSIStyle.h>

static bool g_is_color_enabled = true;

namespace AK {

void set_color_enabled(bool value) { g_is_color_enabled = value; }

ErrorOr<void> Formatter<ANSIStyle>::format(FormatBuilder& builder, ANSIStyle style)
{
    bool first = true;
    auto attribute_separator = [&]() -> ErrorOr<void> {
        if (!first)
            TRY(builder.put_literal(";"sv));
        else
            first = false;

        return {};
    };

    if (!style.has_style())
        return {};

    TRY(builder.put_literal("\e["sv));
    if (style.m_flags != ANSIStyle::Flags::NoAttributes) {
        if (has_flag(style.m_flags, ANSIStyle::Flags::Reset)) {
            TRY(attribute_separator());
            TRY(builder.put_literal("0"sv));
        }
        if (has_flag(style.m_flags, ANSIStyle::Flags::Bold)) {
            TRY(attribute_separator());
            TRY(builder.put_literal("1"sv));
        }
        if (has_flag(style.m_flags, ANSIStyle::Flags::Italic)) {
            TRY(attribute_separator());
            TRY(builder.put_literal("3"sv));
        }
        if (has_flag(style.m_flags, ANSIStyle::Flags::Underline)) {
            TRY(attribute_separator());
            TRY(builder.put_literal("4"sv));
        }
        if (has_flag(style.m_flags, ANSIStyle::Flags::Blink)) {
            TRY(attribute_separator());
            TRY(builder.put_literal("5"sv));
        }
        if (has_flag(style.m_flags, ANSIStyle::Flags::Negative)) {
            TRY(attribute_separator());
            TRY(builder.put_literal("7"sv));
        }
        if (has_flag(style.m_flags, ANSIStyle::Flags::Concealed)) {
            TRY(attribute_separator());
            TRY(builder.put_literal("8"sv));
        }
    }
    if (style.m_fg != ANSIStyle::Fg::FgNone) {
        TRY(attribute_separator());
        TRY(builder.put_u64(static_cast<u8>(style.m_fg)));
    }
    if (style.m_bg != ANSIStyle::Bg::BgNone) {
        TRY(attribute_separator());
        TRY(builder.put_u64(static_cast<u8>(style.m_bg)));
    }
    TRY(builder.put_literal("m"sv));

    return {};
}

void ANSIStyle::apply(FILE* file, ANSIStyle style)
{
    if (g_is_color_enabled) {
        out(file, "{}", style);
    }
}
}
