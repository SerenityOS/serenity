/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Vector.h>

// On Linux distros that use mlibc `basename` is defined as a macro that expands to `__mlibc_gnu_basename` or `__mlibc_gnu_basename_c`, so we undefine it.
#if defined(AK_OS_LINUX) && defined(basename)
#    undef basename
#endif

namespace AK {

class LexicalPath {
public:
    explicit LexicalPath(DeprecatedString);

    bool is_absolute() const { return !m_string.is_empty() && m_string[0] == '/'; }
    DeprecatedString const& string() const { return m_string; }

    StringView dirname() const { return m_dirname; }
    StringView basename() const { return m_basename; }
    StringView title() const { return m_title; }
    StringView extension() const { return m_extension; }

    Vector<StringView> const& parts_view() const { return m_parts; }
    [[nodiscard]] Vector<DeprecatedString> parts() const;

    bool has_extension(StringView) const;

    [[nodiscard]] LexicalPath append(StringView) const;
    [[nodiscard]] LexicalPath prepend(StringView) const;
    [[nodiscard]] LexicalPath parent() const;

    [[nodiscard]] static DeprecatedString canonicalized_path(DeprecatedString);
    [[nodiscard]] static DeprecatedString absolute_path(DeprecatedString dir_path, DeprecatedString target);
    [[nodiscard]] static DeprecatedString relative_path(StringView absolute_path, StringView prefix);

    template<typename... S>
    [[nodiscard]] static LexicalPath join(StringView first, S&&... rest)
    {
        StringBuilder builder;
        builder.append(first);
        ((builder.append('/'), builder.append(forward<S>(rest))), ...);

        return LexicalPath { builder.to_deprecated_string() };
    }

    [[nodiscard]] static DeprecatedString dirname(DeprecatedString path)
    {
        auto lexical_path = LexicalPath(move(path));
        return lexical_path.dirname();
    }

    [[nodiscard]] static DeprecatedString basename(DeprecatedString path)
    {
        auto lexical_path = LexicalPath(move(path));
        return lexical_path.basename();
    }

    [[nodiscard]] static DeprecatedString title(DeprecatedString path)
    {
        auto lexical_path = LexicalPath(move(path));
        return lexical_path.title();
    }

    [[nodiscard]] static DeprecatedString extension(DeprecatedString path)
    {
        auto lexical_path = LexicalPath(move(path));
        return lexical_path.extension();
    }

private:
    Vector<StringView> m_parts;
    DeprecatedString m_string;
    StringView m_dirname;
    StringView m_basename;
    StringView m_title;
    StringView m_extension;
};

template<>
struct Formatter<LexicalPath> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, LexicalPath const& value)
    {
        return Formatter<StringView>::format(builder, value.string());
    }
};

};

#if USING_AK_GLOBALLY
using AK::LexicalPath;
#endif
