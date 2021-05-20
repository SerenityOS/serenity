/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
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

    static HTMLToken make_character(u32 code_point)
    {
        HTMLToken token;
        token.m_type = Type::Character;
        token.m_comment_or_character.data.append(code_point);
        return token;
    }

    static HTMLToken make_start_tag(const FlyString& tag_name)
    {
        HTMLToken token;
        token.m_type = Type::StartTag;
        token.m_tag.tag_name.append(tag_name);
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
        Utf8View view(m_comment_or_character.data.string_view());
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
        return m_tag.tag_name.to_string();
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

    StringView attribute(const FlyString& attribute_name)
    {
        VERIFY(is_start_tag() || is_end_tag());
        for (auto& attribute : m_tag.attributes) {
            if (attribute_name == attribute.local_name_builder.string_view())
                return attribute.value_builder.string_view();
        }
        return {};
    }

    bool has_attribute(const FlyString& attribute_name)
    {
        return !attribute(attribute_name).is_null();
    }

    void adjust_tag_name(const FlyString& old_name, const FlyString& new_name)
    {
        VERIFY(is_start_tag() || is_end_tag());
        if (old_name == m_tag.tag_name.string_view()) {
            m_tag.tag_name.clear();
            m_tag.tag_name.append(new_name);
        }
    }

    void adjust_attribute_name(const FlyString& old_name, const FlyString& new_name)
    {
        VERIFY(is_start_tag() || is_end_tag());
        for (auto& attribute : m_tag.attributes) {
            if (old_name == attribute.local_name_builder.string_view()) {
                attribute.local_name_builder.clear();
                attribute.local_name_builder.append(new_name);
            }
        }
    }

    void adjust_foreign_attribute(const FlyString& old_name, const FlyString& prefix, const FlyString& local_name, const FlyString& namespace_)
    {
        VERIFY(is_start_tag() || is_end_tag());
        for (auto& attribute : m_tag.attributes) {
            if (old_name == attribute.local_name_builder.string_view()) {
                attribute.prefix_builder.clear();
                attribute.prefix_builder.append(prefix);

                attribute.local_name_builder.clear();
                attribute.local_name_builder.append(local_name);

                attribute.namespace_builder.clear();
                attribute.namespace_builder.append(namespace_);
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

    const auto& start_position() const { return m_start_position; }
    const auto& end_position() const { return m_end_position; }

    const auto& attributes() const
    {
        VERIFY(is_start_tag() || is_end_tag());
        return m_tag.attributes;
    }

private:
    struct Position {
        size_t line { 0 };
        size_t column { 0 };
    };

    struct AttributeBuilder {
        StringBuilder prefix_builder;
        StringBuilder local_name_builder;
        StringBuilder namespace_builder;
        StringBuilder value_builder;
        Position name_start_position;
        Position value_start_position;
        Position name_end_position;
        Position value_end_position;
    };

    Type m_type { Type::Invalid };

    // Type::DOCTYPE
    struct {
        // NOTE: "Missing" is a distinct state from the empty string.

        StringBuilder name;
        bool missing_name { true };
        StringBuilder public_identifier;
        bool missing_public_identifier { true };
        StringBuilder system_identifier;
        bool missing_system_identifier { true };
        bool force_quirks { false };
    } m_doctype;

    // Type::StartTag
    // Type::EndTag
    struct {
        StringBuilder tag_name;
        bool self_closing { false };
        bool self_closing_acknowledged { false };
        Vector<AttributeBuilder> attributes;
    } m_tag;

    // Type::Comment
    // Type::Character
    struct {
        StringBuilder data;
    } m_comment_or_character;

    Position m_start_position;
    Position m_end_position;
};

}
