/*
 * Copyright (c) 2026, Eduardo Casadei <educasadei@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ANSIStyle.h>
#include <AK/StringBuilder.h>

static bool is_color_enabled = true;

namespace AK {

void set_color_enabled(bool value) { is_color_enabled = value; }

ErrorOr<void> Formatter<ANSIStyle>::format(FormatBuilder& builder, ANSIStyle value)
{
    bool first = true;
    auto attribute_separator = [&]() -> ErrorOr<void> {
        if (!first)
            TRY(builder.put_literal(";"sv));
        else
            first = false;

        return {};
    };

    if (!value.has_style())
        return {};

    TRY(builder.put_literal("\e["sv));
    if (value.m_style != ANSIStyle::Style::None) {
        if (has_flag(value.m_style, ANSIStyle::Style::Reset)) {
            TRY(attribute_separator());
            TRY(builder.put_literal("0"sv));
        }
        if (has_flag(value.m_style, ANSIStyle::Style::Bold)) {
            TRY(attribute_separator());
            TRY(builder.put_literal("1"sv));
        }
        if (has_flag(value.m_style, ANSIStyle::Style::Italic)) {
            TRY(attribute_separator());
            TRY(builder.put_literal("3"sv));
        }
        if (has_flag(value.m_style, ANSIStyle::Style::Underline)) {
            TRY(attribute_separator());
            TRY(builder.put_literal("4"sv));
        }
        if (has_flag(value.m_style, ANSIStyle::Style::Blink)) {
            TRY(attribute_separator());
            TRY(builder.put_literal("5"sv));
        }
        if (has_flag(value.m_style, ANSIStyle::Style::Negative)) {
            TRY(attribute_separator());
            TRY(builder.put_literal("7"sv));
        }
        if (has_flag(value.m_style, ANSIStyle::Style::Concealed)) {
            TRY(attribute_separator());
            TRY(builder.put_literal("8"sv));
        }
    }
    if (value.m_foreground.has_value()) {
        TRY(attribute_separator());
        TRY(builder.put_u64(value.m_foreground.value()));
    }
    if (value.m_background.has_value()) {
        TRY(attribute_separator());
        TRY(builder.put_u64(value.m_background.value() + 10));
    }
    TRY(builder.put_literal("m"sv));

    return {};
}

void ANSIStyle::apply(StringBuilder& builder) const
{
    if (is_color_enabled) {
        builder.appendff("{}", *this);
    }
}

void ANSIStyle::apply(FILE* file) const
{
    if (is_color_enabled) {
        out(file, "{}", *this);
    }
}

void vout(FILE* file, ANSIStyle ansi_style, StringView fmtstr, TypeErasedFormatParams& params, bool newline)
{
    StringBuilder builder;
    ansi_style.apply(builder);
    MUST(vformat(builder, fmtstr, params));
    style(ANSIStyle::Reset).apply(builder);

    if (newline)
        builder.append('\n');

    auto const string = builder.string_view();
    auto const retval = ::fwrite(string.characters_without_null_termination(), 1, string.length(), file);
    if (static_cast<size_t>(retval) != string.length()) {
        auto error = ferror(file);
        dbgln("vout() failed ({} written out of {}), error was {} ({})", retval, string.length(), error, strerror(error));
    }
}
}
