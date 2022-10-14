/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Utf8View.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/Layout/Node.h>

namespace Web::Layout {

class LineBoxFragment;

class TextNode : public Node {
public:
    TextNode(DOM::Document&, DOM::Text&);
    virtual ~TextNode() override;

    const DOM::Text& dom_node() const { return static_cast<const DOM::Text&>(*Node::dom_node()); }

    String const& text_for_rendering() const { return m_text_for_rendering; }

    struct Chunk {
        Utf8View view;
        size_t start { 0 };
        size_t length { 0 };
        bool has_breaking_newline { false };
        bool is_all_whitespace { false };
    };

    class ChunkIterator {
    public:
        ChunkIterator(StringView text, bool wrap_lines, bool respect_linebreaks, bool is_generated_empty_string);
        Optional<Chunk> next();

    private:
        Optional<Chunk> try_commit_chunk(Utf8View::Iterator const& start, Utf8View::Iterator const& end, bool has_breaking_newline) const;

        bool const m_wrap_lines;
        bool const m_respect_linebreaks;
        bool m_should_emit_one_empty_chunk { false };
        Utf8View m_utf8_view;
        Utf8View::Iterator m_iterator;
    };

    void compute_text_for_rendering(bool collapse);

    virtual RefPtr<Painting::Paintable> create_paintable() const override;

private:
    virtual bool is_text_node() const final { return true; }

    String m_text_for_rendering;
};

template<>
inline bool Node::fast_is<TextNode>() const { return is_text_node(); }

}
