/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/String.h>
#include <YAK/Vector.h>

namespace YAK {

class LexicalPath {
public:
    explicit LexicalPath(String);

    bool is_absolute() const { return !m_string.is_empty() && m_string[0] == '/'; }
    String const& string() const { return m_string; }

    StringView const& dirname() const { return m_dirname; }
    StringView const& basename() const { return m_basename; }
    StringView const& title() const { return m_title; }
    StringView const& extension() const { return m_extension; }

    Vector<StringView> const& parts_view() const { return m_parts; }
    [[nodiscard]] Vector<String> parts() const;

    bool has_extension(StringView const&) const;

    [[nodiscard]] LexicalPath append(StringView const&) const;
    [[nodiscard]] LexicalPath parent() const;

    [[nodiscard]] static String canonicalized_path(String);
    [[nodiscard]] static String relative_path(StringView const& absolute_path, StringView const& prefix);

    template<typename... S>
    [[nodiscard]] static LexicalPath join(String const& first, S&&... rest)
    {
        StringBuilder builder;
        builder.append(first);
        ((builder.append('/'), builder.append(forward<S>(rest))), ...);

        return LexicalPath { builder.to_string() };
    }

    [[nodiscard]] static String dirname(String path)
    {
        auto lexical_path = LexicalPath(move(path));
        return lexical_path.dirname();
    }

    [[nodiscard]] static String basename(String path)
    {
        auto lexical_path = LexicalPath(move(path));
        return lexical_path.basename();
    }

    [[nodiscard]] static String title(String path)
    {
        auto lexical_path = LexicalPath(move(path));
        return lexical_path.title();
    }

    [[nodiscard]] static String extension(String path)
    {
        auto lexical_path = LexicalPath(move(path));
        return lexical_path.extension();
    }

private:
    Vector<StringView> m_parts;
    String m_string;
    StringView m_dirname;
    StringView m_basename;
    StringView m_title;
    StringView m_extension;
};

template<>
struct Formatter<LexicalPath> : Formatter<StringView> {
    void format(FormatBuilder& builder, LexicalPath const& value)
    {
        Formatter<StringView>::format(builder, value.string());
    }
};

};

using YAK::LexicalPath;
