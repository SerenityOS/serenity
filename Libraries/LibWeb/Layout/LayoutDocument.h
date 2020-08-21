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

#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/LayoutBlock.h>

namespace Web {

class LayoutDocument final : public LayoutBlock {
public:
    explicit LayoutDocument(DOM::Document&, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~LayoutDocument() override;

    const DOM::Document& node() const { return static_cast<const DOM::Document&>(*LayoutNode::node()); }
    virtual const char* class_name() const override { return "LayoutDocument"; }
    virtual void layout(LayoutMode = LayoutMode::Default) override;

    void paint_all_phases(PaintContext&);
    virtual void paint(PaintContext&, PaintPhase) override;

    virtual HitTestResult hit_test(const Gfx::IntPoint&, HitTestType) const override;

    const LayoutRange& selection() const { return m_selection; }
    void set_selection(const LayoutRange&);
    void set_selection_end(const LayoutPosition&);

    void did_set_viewport_rect(Badge<Frame>, const Gfx::IntRect&);

    virtual bool is_root() const override { return true; }

    void build_stacking_context_tree();

    void recompute_selection_states();

private:
    LayoutRange m_selection;
};

}

AK_BEGIN_TYPE_TRAITS(Web::LayoutDocument)
static bool is_type(const Web::LayoutNode& layout_node) { return layout_node.is_root(); }
AK_END_TYPE_TRAITS()
