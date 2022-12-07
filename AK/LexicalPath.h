/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>

// On Linux distros that use mlibc `basename` is defined as a macro that expands to `__mlibc_gnu_basename` or `__mlibc_gnu_basename_c`, so we undefine it.
#if defined(AK_OS_LINUX) && defined(basename)
#    undef basename
#endif

namespace AK {

class LexicalPath {
public:
    static ErrorOr<LexicalPath> from_string(StringView);
    static ErrorOr<LexicalPath> from_string(String);

    bool is_absolute() const { return !m_string.is_empty() && *m_string.code_points().begin() == '/'; }
    String const& string() const { return m_string; }

    StringView dirname() const { return m_dirname; }
    StringView basename() const { return m_basename; }
    StringView title() const { return m_title; }
    StringView extension() const { return m_extension; }

    Vector<String> const& parts_view() const { return m_parts; }
    [[nodiscard]] ErrorOr<Vector<String>> parts() const;

    bool has_extension(StringView) const;
    bool is_child_of(LexicalPath const& possible_parent) const;

    [[nodiscard]] ErrorOr<LexicalPath> append(StringView) const;
    [[nodiscard]] ErrorOr<LexicalPath> prepend(StringView) const;
    [[nodiscard]] ErrorOr<LexicalPath> parent() const;

    [[nodiscard]] static ErrorOr<String> canonicalized_path(StringView);
    [[nodiscard]] static ErrorOr<String> absolute_path(StringView dir_path, StringView target);
    [[nodiscard]] static ErrorOr<String> relative_path(StringView absolute_path, StringView prefix);

    template<typename... S>
    [[nodiscard]] static ErrorOr<LexicalPath> join(StringView first, S&&... rest)
    {
        StringBuilder builder;
        builder.append(first);
        ((builder.append('/'), builder.append(forward<S>(rest))), ...);

        return TRY(LexicalPath::from_string(TRY(builder.to_string())));
    }

    [[nodiscard]] static ErrorOr<String> dirname(String path)
    {
        auto lexical_path = TRY(LexicalPath::from_string(move(path)));
        return String::from_utf8(lexical_path.dirname());
    }
    [[nodiscard]] static ErrorOr<String> dirname(StringView path) { return dirname(TRY(String::from_utf8(path))); }

    [[nodiscard]] static ErrorOr<String> basename(String path)
    {
        auto lexical_path = TRY(LexicalPath::from_string(move(path)));
        return String::from_utf8(lexical_path.basename());
    }
    [[nodiscard]] static ErrorOr<String> basename(StringView path) { return basename(TRY(String::from_utf8(path))); }

    [[nodiscard]] static ErrorOr<String> title(String path)
    {
        auto lexical_path = TRY(LexicalPath::from_string(move(path)));
        return String::from_utf8(lexical_path.title());
    }
    [[nodiscard]] static ErrorOr<String> title(StringView path) { return title(TRY(String::from_utf8(path))); }

    [[nodiscard]] static ErrorOr<String> extension(String path)
    {
        auto lexical_path = TRY(LexicalPath::from_string(move(path)));
        return String::from_utf8(lexical_path.extension());
    }
    [[nodiscard]] static ErrorOr<String> extension(StringView path) { return extension(TRY(String::from_utf8(path))); }

private:
    LexicalPath() = default;

    Vector<String> m_parts;
    String m_string;
    String m_dirname;
    String m_basename;
    String m_title;
    String m_extension;
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
