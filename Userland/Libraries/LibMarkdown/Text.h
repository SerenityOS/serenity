/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Noncopyable.h>
#include <AK/OwnPtr.h>
#include <AK/RecursionDecision.h>
#include <AK/Vector.h>
#include <LibMarkdown/Forward.h>

namespace Markdown {

class Text final {
public:
    class Node {
    public:
        virtual void render_to_html(StringBuilder& builder) const = 0;
        virtual void render_for_terminal(StringBuilder& builder) const = 0;
        virtual void render_for_raw_print(StringBuilder& builder) const = 0;
        virtual size_t terminal_length() const = 0;
        virtual RecursionDecision walk(Visitor&) const = 0;

        virtual ~Node() = default;
    };

    class EmphasisNode : public Node {
    public:
        bool strong;
        NonnullOwnPtr<Node> child;

        EmphasisNode(bool strong, NonnullOwnPtr<Node> child)
            : strong(strong)
            , child(move(child))
        {
        }

        virtual void render_to_html(StringBuilder& builder) const override;
        virtual void render_for_terminal(StringBuilder& builder) const override;
        virtual void render_for_raw_print(StringBuilder& builder) const override;
        virtual size_t terminal_length() const override;
        virtual RecursionDecision walk(Visitor&) const override;
    };

    class CodeNode : public Node {
    public:
        NonnullOwnPtr<Node> code;

        CodeNode(NonnullOwnPtr<Node> code)
            : code(move(code))
        {
        }

        virtual void render_to_html(StringBuilder& builder) const override;
        virtual void render_for_terminal(StringBuilder& builder) const override;
        virtual void render_for_raw_print(StringBuilder& builder) const override;
        virtual size_t terminal_length() const override;
        virtual RecursionDecision walk(Visitor&) const override;
    };

    class BreakNode : public Node {
    public:
        virtual void render_to_html(StringBuilder& builder) const override;
        virtual void render_for_terminal(StringBuilder& builder) const override;
        virtual void render_for_raw_print(StringBuilder& builder) const override;
        virtual size_t terminal_length() const override;
        virtual RecursionDecision walk(Visitor&) const override;
    };

    class TextNode : public Node {
    public:
        ByteString text;
        bool collapsible;

        TextNode(StringView text)
            : text(text)
            , collapsible(true)
        {
        }

        TextNode(StringView text, bool collapsible)
            : text(text)
            , collapsible(collapsible)
        {
        }

        virtual void render_to_html(StringBuilder& builder) const override;
        virtual void render_for_terminal(StringBuilder& builder) const override;
        virtual void render_for_raw_print(StringBuilder& builder) const override;
        virtual size_t terminal_length() const override;
        virtual RecursionDecision walk(Visitor&) const override;
    };

    class LinkNode : public Node {
    public:
        bool is_image;
        NonnullOwnPtr<Node> text;
        ByteString href;
        Optional<int> image_width;
        Optional<int> image_height;

        LinkNode(bool is_image, NonnullOwnPtr<Node> text, ByteString href, Optional<int> image_width, Optional<int> image_height)
            : is_image(is_image)
            , text(move(text))
            , href(move(href))
            , image_width(image_width)
            , image_height(image_height)
        {
        }

        bool has_image_dimensions() const
        {
            return image_width.has_value() || image_height.has_value();
        }
        virtual void render_to_html(StringBuilder& builder) const override;
        virtual void render_for_terminal(StringBuilder& builder) const override;
        virtual void render_for_raw_print(StringBuilder& builder) const override;
        virtual size_t terminal_length() const override;
        virtual RecursionDecision walk(Visitor&) const override;
    };

    class MultiNode : public Node {
    public:
        Vector<NonnullOwnPtr<Node>> children;

        virtual void render_to_html(StringBuilder& builder) const override;
        virtual void render_for_terminal(StringBuilder& builder) const override;
        virtual void render_for_raw_print(StringBuilder& builder) const override;
        virtual size_t terminal_length() const override;
        virtual RecursionDecision walk(Visitor&) const override;
    };

    class StrikeThroughNode : public Node {
    public:
        NonnullOwnPtr<Node> striked_text;

        StrikeThroughNode(NonnullOwnPtr<Node> striked_text)
            : striked_text(move(striked_text))
        {
        }

        virtual void render_to_html(StringBuilder& builder) const override;
        virtual void render_for_terminal(StringBuilder& builder) const override;
        virtual void render_for_raw_print(StringBuilder& builder) const override;
        virtual size_t terminal_length() const override;
        virtual RecursionDecision walk(Visitor&) const override;
    };

    size_t terminal_length() const;

    ByteString render_to_html() const;
    ByteString render_for_terminal() const;
    ByteString render_for_raw_print() const;
    RecursionDecision walk(Visitor&) const;

    static Text parse(StringView);

private:
    struct Token {
        ByteString data;
        // Flanking basically means that a delimiter run has a non-whitespace,
        // non-punctuation character on the corresponding side. For a more exact
        // definition, see the CommonMark spec.
        bool left_flanking;
        bool right_flanking;
        bool punct_before;
        bool punct_after;
        // is_run indicates that this token is a 'delimiter run'. A delimiter
        // run occurs when several of the same syntactical character ('`', '_',
        // or '*') occur in a row.
        bool is_run;

        char run_char() const
        {
            VERIFY(is_run);
            return data[0];
        }
        char run_length() const
        {
            VERIFY(is_run);
            return data.length();
        }
        bool is_space() const
        {
            return data[0] == ' ';
        }
        bool operator==(StringView str) const { return str == data; }
    };

    static Vector<Token> tokenize(StringView);

    static bool can_open(Token const& opening);
    static bool can_close_for(Token const& opening, Token const& closing);

    static NonnullOwnPtr<MultiNode> parse_sequence(Vector<Token>::ConstIterator& tokens, bool in_link);
    static NonnullOwnPtr<Node> parse_break(Vector<Token>::ConstIterator& tokens);
    static NonnullOwnPtr<Node> parse_newline(Vector<Token>::ConstIterator& tokens);
    static NonnullOwnPtr<Node> parse_emph(Vector<Token>::ConstIterator& tokens, bool in_link);
    static NonnullOwnPtr<Node> parse_code(Vector<Token>::ConstIterator& tokens);
    static NonnullOwnPtr<Node> parse_link(Vector<Token>::ConstIterator& tokens);
    static NonnullOwnPtr<Node> parse_strike_through(Vector<Token>::ConstIterator& tokens);

    OwnPtr<Node> m_node;
};

}
