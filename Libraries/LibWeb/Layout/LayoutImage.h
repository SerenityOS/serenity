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

#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/Layout/LayoutReplaced.h>

namespace Web {

class LayoutImage : public LayoutReplaced {
public:
    LayoutImage(DOM::Document&, DOM::Element&, NonnullRefPtr<CSS::StyleProperties>, const ImageLoader&);
    virtual ~LayoutImage() override;

    virtual void layout(LayoutMode = LayoutMode::Default) override;
    virtual void paint(PaintContext&, PaintPhase) override;

    const DOM::Element& node() const { return static_cast<const DOM::Element&>(LayoutReplaced::node()); }

    bool renders_as_alt_text() const;

    void set_visible_in_viewport(Badge<LayoutDocument>, bool);

private:
    virtual const char* class_name() const override { return "LayoutImage"; }
    virtual bool is_image() const override { return true; }

    int preferred_width() const;
    int preferred_height() const;

    const ImageLoader& m_image_loader;
};

}

AK_BEGIN_TYPE_TRAITS(Web::LayoutImage)
static bool is_type(const Web::LayoutNode& layout_node) { return layout_node.is_image(); }
AK_END_TYPE_TRAITS()
