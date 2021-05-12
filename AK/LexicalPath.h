/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

    bool is_valid() const { return m_is_valid; }
    bool is_absolute() const { return m_is_absolute; }
    const String& string() const { return m_string; }

    const String& dirname() const { return m_dirname; }
    const String& basename() const { return m_basename; }
    const String& title() const { return m_title; }
    const String& extension() const { return m_extension; }

    const Vector<String>& parts() const { return m_parts; }

    bool has_extension(const StringView&) const;

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

private:
    void canonicalize();

    Vector<String> m_parts;
    String m_string;
    String m_dirname;
    String m_basename;
    String m_title;
    String m_extension;
    bool m_is_valid { false };
    bool m_is_absolute { false };
};

template<>
struct Formatter<LexicalPath> : Formatter<StringView> {
    void format(FormatBuilder& builder, const LexicalPath& value)
    {
        Formatter<StringView>::format(builder, value.string());
    }
};

};

using AK::LexicalPath;
