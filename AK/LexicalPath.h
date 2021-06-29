/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>

namespace AK {

class LexicalPath {
public:
    LexicalPath() = default;
    explicit LexicalPath(String);

    bool is_absolute() const { return m_is_absolute; }
    String const& string() const { return m_string; }

    String const& dirname() const { return m_dirname; }
    String const& basename() const { return m_basename; }
    String const& title() const { return m_title; }
    String const& extension() const { return m_extension; }

    Vector<String> const& parts() const { return m_parts; }

    bool has_extension(StringView const&) const;

    void append(String const& component);

    static String canonicalized_path(String);
    static String relative_path(String absolute_path, String const& prefix);

    template<typename... S>
    static LexicalPath join(String const& first, S&&... rest)
    {
        StringBuilder builder;
        builder.append(first);
        ((builder.append('/'), builder.append(forward<S>(rest))), ...);

        return LexicalPath { builder.to_string() };
    }

    static String dirname(String path)
    {
        auto lexical_path = LexicalPath(move(path));
        return lexical_path.dirname();
    }

    static String basename(String path)
    {
        auto lexical_path = LexicalPath(move(path));
        return lexical_path.basename();
    }

    static String title(String path)
    {
        auto lexical_path = LexicalPath(move(path));
        return lexical_path.title();
    }

    static String extension(String path)
    {
        auto lexical_path = LexicalPath(move(path));
        return lexical_path.extension();
    }

private:
    void canonicalize();

    Vector<String> m_parts;
    String m_string;
    String m_dirname;
    String m_basename;
    String m_title;
    String m_extension;
    bool m_is_absolute { false };
};

template<>
struct Formatter<LexicalPath> : Formatter<StringView> {
    void format(FormatBuilder& builder, LexicalPath const& value)
    {
        Formatter<StringView>::format(builder, value.string());
    }
};

};

using AK::LexicalPath;
