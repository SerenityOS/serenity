/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibWeb/DOM/Text.h>
#include <LibWeb/Layout/LayoutNode.h>

namespace Web {

class LineBoxFragment;

class LayoutText : public LayoutNode {
public:
    LayoutText(DOM::Document&, DOM::Text&);
    virtual ~LayoutText() override;

    const DOM::Text& node() const { return static_cast<const DOM::Text&>(*LayoutNode::node()); }

    const String& text_for_style(const CSS::StyleProperties&) const;
    const String& text_for_rendering() const { return m_text_for_rendering; }

    virtual const char* class_name() const override { return "LayoutText"; }
    virtual bool is_text() const final { return true; }

    void paint_fragment(PaintContext&, const LineBoxFragment&) const;

    virtual void split_into_lines(LayoutBlock& container, LayoutMode) override;

    const CSS::StyleProperties& specified_style() const { return parent()->specified_style(); }

private:
    void split_into_lines_by_rules(LayoutBlock& container, LayoutMode, bool do_collapse, bool do_wrap_lines, bool do_wrap_breaks);
    void paint_cursor_if_needed(PaintContext&, const LineBoxFragment&) const;

    template<typename Callback>
    void for_each_chunk(Callback, LayoutMode, bool do_wrap_lines, bool do_wrap_breaks) const;

    String m_text_for_rendering;
};

}

AK_BEGIN_TYPE_TRAITS(Web::LayoutText)
static bool is_type(const Web::LayoutNode& layout_node) { return layout_node.is_text(); }
AK_END_TYPE_TRAITS()
