/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>

namespace Web::HTML {

class HTMLToken {
    friend class HTMLDocumentParser;
    friend class HTMLTokenizer;

public:
    enum class Type {
        Invalid,
        DOCTYPE,
        StartTag,
        EndTag,
        Comment,
        Character,
        EndOfFile,
    };

    struct Position {
        size_t line { 0 };
        size_t column { 0 };
    };

    struct Attribute {
        String prefix;
        String local_name;
        String namespace_;
        String value;
        Position name_start_position;
        Position value_start_position;
        Position name_end_position;
        Position value_end_position;
    };

    static HTMLToken make_character(u32 code_point)
    {
        HTMLToken token;
        token.m_type = Type::Character;
        StringBuilder builder;
        // FIXME: This narrows code_point to char, should this be append_code_point() instead?
        builder.append(code_point);
        token.m_comment_or_character.data = builder.to_string();
        return token;
    }

    static HTMLToken make_start_tag(FlyString const& tag_name)
    {
        HTMLToken token;
        token.m_type = Type::StartTag;
        token.m_tag.tag_name = tag_name;
        return token;
    }

    bool is_doctype() const { return m_type == Type::DOCTYPE; }
    bool is_start_tag() const { return m_type == Type::StartTag; }
    bool is_end_tag() const { return m_type == Type::EndTag; }
    bool is_comment() const { return m_type == Type::Comment; }
    bool is_character() const { return m_type == Type::Character; }
    bool is_end_of_file() const { return m_type == Type::EndOfFile; }

    u32 code_point() const
    {
        VERIFY(is_character());
        Utf8View view(m_comment_or_character.data);
        VERIFY(view.length() == 1);
        return *view.begin();
    }

    bool is_parser_whitespace() const
    {
        // NOTE: The parser considers '\r' to be whitespace, while the tokenizer does not.
        if (!is_character())
            return false;
        switch (code_point()) {
        case '\t':
        case '\n':
        case '\f':
        case '\r':
        case ' ':
            return true;
        default:
            return false;
        }
    }

    String tag_name() const
    {
        VERIFY(is_start_tag() || is_end_tag());
        return m_tag.tag_name;
    }

    bool is_self_closing() const
    {
        VERIFY(is_start_tag() || is_end_tag());
        return m_tag.self_closing;
    }

    bool has_acknowledged_self_closing_flag() const
    {
        VERIFY(is_self_closing());
        return m_tag.self_closing_acknowledged;
    }

    void acknowledge_self_closing_flag_if_set()
    {
        if (is_self_closing())
            m_tag.self_closing_acknowledged = true;
    }

    StringView attribute(FlyString const& attribute_name)
    {
        VERIFY(is_start_tag() || is_end_tag());
        for (auto& attribute : m_tag.attributes) {
            if (attribute_name == attribute.local_name)
                return attribute.value;
        }
        return {};
    }

    bool has_attribute(FlyString const& attribute_name)
    {
        return !attribute(attribute_name).is_null();
    }

    void adjust_tag_name(FlyString const& old_name, FlyString const& new_name)
    {
        VERIFY(is_start_tag() || is_end_tag());
        if (old_name == m_tag.tag_name)
            m_tag.tag_name = new_name;
    }

    void adjust_attribute_name(FlyString const& old_name, FlyString const& new_name)
    {
        VERIFY(is_start_tag() || is_end_tag());
        for (auto& attribute : m_tag.attributes) {
            if (old_name == attribute.local_name) {
                attribute.local_name = new_name;
            }
        }
    }

    void adjust_foreign_attribute(FlyString const& old_name, FlyString const& prefix, FlyString const& local_name, FlyString const& namespace_)
    {
        VERIFY(is_start_tag() || is_end_tag());
        for (auto& attribute : m_tag.attributes) {
            if (old_name == attribute.local_name) {
                attribute.prefix = prefix;
                attribute.local_name = local_name;
                attribute.namespace_ = namespace_;
            }
        }
    }

    void drop_attributes()
    {
        VERIFY(is_start_tag() || is_end_tag());
        m_tag.attributes.clear();
    }

    Type type() const { return m_type; }

    String to_string() const;

    Position const& start_position() const { return m_start_position; }
    Position const& end_position() const { return m_end_position; }

    Vector<Attribute> const& attributes() const
    {
        VERIFY(is_start_tag() || is_end_tag());
        return m_tag.attributes;
    }

private:
    Type m_type { Type::Invalid };

    // Type::DOCTYPE
    struct {
        // NOTE: "Missing" is a distinct state from the empty string.

        String name;
        bool missing_name { true };
        String public_identifier;
        bool missing_public_identifier { true };
        String system_identifier;
        bool missing_system_identifier { true };
        bool force_quirks { false };
    } m_doctype;

    // Type::StartTag
    // Type::EndTag
    struct {
        String tag_name;
        bool self_closing { false };
        bool self_closing_acknowledged { false };
        Vector<Attribute> attributes;
    } m_tag;

    // Type::Comment
    // Type::Character
    struct {
        String data;
    } m_comment_or_character;

    Position m_start_position;
    Position m_end_position;
};

}
