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

#include <LibGfx/FloatRect.h>
#include <LibHTML/Layout/LayoutNode.h>

class LayoutBox : public LayoutNodeWithStyleAndBoxModelMetrics {
public:
    const FloatRect& rect() const { return m_rect; }
    FloatRect& rect() { return m_rect; }
    void set_rect(const FloatRect& rect) { m_rect = rect; }

    float x() const { return rect().x(); }
    float y() const { return rect().y(); }
    float width() const { return rect().width(); }
    float height() const { return rect().height(); }
    FloatSize size() const { return rect().size(); }
    FloatPoint position() const { return rect().location(); }

    virtual HitTestResult hit_test(const Gfx::Point& position) const override;
    virtual void set_needs_display() override;

    bool is_body() const;

protected:
    LayoutBox(const Node* node, NonnullRefPtr<StyleProperties> style)
        : LayoutNodeWithStyleAndBoxModelMetrics(node, move(style))
    {
    }

    virtual void render(RenderingContext&) override;

private:
    virtual bool is_box() const override { return true; }

    enum class Edge {
        Top,
        Right,
        Bottom,
        Left,
    };
    void paint_border(RenderingContext&, Edge, const FloatRect&, CSS::PropertyID style_property_id, CSS::PropertyID color_property_id, CSS::PropertyID width_property_id);

    FloatRect m_rect;
};

template<>
inline bool is<LayoutBox>(const LayoutNode& node)
{
    return node.is_box();
}
