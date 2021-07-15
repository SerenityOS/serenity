/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/Function.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>

namespace Web::HTML {

class HTMLTokenizer;

class HTMLToken {

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

    struct DoctypeData {
        // NOTE: "Missing" is a distinct state from the empty string.
        String name;
        String public_identifier;
        String system_identifier;
        bool missing_name { true };
        bool missing_public_identifier { true };
        bool missing_system_identifier { true };
        bool force_quirks { false };
    };

    static HTMLToken make_character(u32 code_point)
    {
        HTMLToken token { Type::Character };
        token.set_code_point(code_point);
        return token;
    }

    static HTMLToken make_start_tag(FlyString const& tag_name)
    {
        HTMLToken token { Type::StartTag };
        token.set_tag_name(tag_name);
        return token;
    }

    HTMLToken() = default;

    HTMLToken(Type type)
        : m_type(type)
    {
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

    void set_code_point(u32 code_point)
    {
        VERIFY(is_character());
        StringBuilder builder;
        builder.append_code_point(code_point);
        m_comment_or_character.data = builder.to_string();
    }

    String const& comment() const
    {
        VERIFY(is_comment());
        return m_comment_or_character.data;
    }

    void set_comment(String comment)
    {
        VERIFY(is_comment());
        m_comment_or_character.data = move(comment);
    }

    String const& tag_name() const
    {
        VERIFY(is_start_tag() || is_end_tag());
        return m_tag.tag_name;
    }

    void set_tag_name(String name)
    {
        VERIFY(is_start_tag() || is_end_tag());
        m_tag.tag_name = move(name);
    }

    bool is_self_closing() const
    {
        VERIFY(is_start_tag() || is_end_tag());
        return m_tag.self_closing;
    }

    void set_self_closing(bool self_closing)
    {
        VERIFY(is_start_tag() || is_end_tag());
        m_tag.self_closing = self_closing;
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

    bool has_attributes() const
    {
        VERIFY(is_start_tag() || is_end_tag());
        return !m_tag.attributes.is_empty();
    }

    size_t attribute_count() const
    {
        VERIFY(is_start_tag() || is_end_tag());
        return m_tag.attributes.size();
    }

    void add_attribute(Attribute attribute)
    {
        VERIFY(is_start_tag() || is_end_tag());
        m_tag.attributes.append(move(attribute));
    }

    Attribute const& last_attribute() const
    {
        VERIFY(is_start_tag() || is_end_tag());
        VERIFY(!m_tag.attributes.is_empty());
        return m_tag.attributes.last();
    }

    Attribute& last_attribute()
    {
        VERIFY(is_start_tag() || is_end_tag());
        VERIFY(!m_tag.attributes.is_empty());
        return m_tag.attributes.last();
    }

    void drop_attributes()
    {
        VERIFY(is_start_tag() || is_end_tag());
        m_tag.attributes.clear();
    }

    void for_each_attribute(Function<IterationDecision(Attribute const&)> callback) const
    {
        VERIFY(is_start_tag() || is_end_tag());
        for (auto& attribute : m_tag.attributes) {
            if (callback(attribute) == IterationDecision::Break)
                break;
        }
    }

    void for_each_attribute(Function<IterationDecision(Attribute&)> callback)
    {
        VERIFY(is_start_tag() || is_end_tag());
        for (auto& attribute : m_tag.attributes) {
            if (callback(attribute) == IterationDecision::Break)
                break;
        }
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
        if (old_name == tag_name())
            set_tag_name(new_name);
    }

    void adjust_attribute_name(FlyString const& old_name, FlyString const& new_name)
    {
        VERIFY(is_start_tag() || is_end_tag());
        for_each_attribute([&](Attribute& attribute) {
            if (old_name == attribute.local_name)
                attribute.local_name = new_name;
            return IterationDecision::Continue;
        });
    }

    void adjust_foreign_attribute(FlyString const& old_name, FlyString const& prefix, FlyString const& local_name, FlyString const& namespace_)
    {
        VERIFY(is_start_tag() || is_end_tag());
        for_each_attribute([&](Attribute& attribute) {
            if (old_name == attribute.local_name) {
                attribute.prefix = prefix;
                attribute.local_name = local_name;
                attribute.namespace_ = namespace_;
            }
            return IterationDecision::Continue;
        });
    }

    DoctypeData const& doctype_data() const
    {
        VERIFY(is_doctype());
        return m_doctype;
    }

    DoctypeData& doctype_data()
    {
        VERIFY(is_doctype());
        return m_doctype;
    }

    Type type() const { return m_type; }

    String to_string() const;

    Position const& start_position() const { return m_start_position; }
    Position const& end_position() const { return m_end_position; }

    void set_start_position(Badge<HTMLTokenizer>, Position start_position) { m_start_position = start_position; }
    void set_end_position(Badge<HTMLTokenizer>, Position end_position) { m_end_position = end_position; }

private:
    Type m_type { Type::Invalid };

    // Type::DOCTYPE
    DoctypeData m_doctype;

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
