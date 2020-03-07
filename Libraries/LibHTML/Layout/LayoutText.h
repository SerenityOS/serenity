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

#include <LibHTML/DOM/Text.h>
#include <LibHTML/Layout/LayoutNode.h>

namespace Web {

class LineBoxFragment;

class LayoutText : public LayoutNode {
public:
    explicit LayoutText(const Text&);
    virtual ~LayoutText() override;

    const Text& node() const { return static_cast<const Text&>(*LayoutNode::node()); }

    const String& text_for_style(const StyleProperties&) const;
    const String& text_for_rendering() const { return m_text_for_rendering; }

    virtual const char* class_name() const override { return "LayoutText"; }
    virtual bool is_text() const final { return true; }

    void render_fragment(RenderingContext&, const LineBoxFragment&) const;

    virtual void split_into_lines(LayoutBlock& container) override;

    const StyleProperties& style() const { return parent()->style(); }

private:
    void split_preformatted_into_lines(LayoutBlock& container);

    template<typename Callback>
    void for_each_word(Callback) const;

    String m_text_for_rendering;
};

template<>
inline bool is<LayoutText>(const LayoutNode& node)
{
    return node.is_text();
}

}
