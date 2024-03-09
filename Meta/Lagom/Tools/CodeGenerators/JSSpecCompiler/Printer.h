/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringBuilder.h>
#include <AK/TemporaryChange.h>

namespace JSSpecCompiler {

class Printer {
public:
    template<typename Func>
    void block(Func&& func, StringView start = "{"sv, StringView end = "}"sv)
    {
        formatln("{}", start);
        ++indent_level;
        func();
        --indent_level;
        format("{}", end);
    }

    template<typename... Parameters>
    void format(AK::CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
    {
        if (builder.string_view().ends_with('\n'))
            builder.append_repeated(' ', indent_level * 4);
        builder.appendff(move(fmtstr), forward<Parameters const&>(parameters)...);
    }

    template<typename... Parameters>
    void formatln(AK::CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
    {
        format(move(fmtstr), forward<Parameters const&>(parameters)...);
        builder.append("\n"sv);
    }

    StringView view() const { return builder.string_view(); }

private:
    StringBuilder builder;
    size_t indent_level = 0;
};

}
