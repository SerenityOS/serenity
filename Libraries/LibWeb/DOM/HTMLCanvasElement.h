/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/ByteBuffer.h>
#include <LibGfx/Forward.h>
#include <LibWeb/DOM/HTMLElement.h>

namespace Web {

class LayoutDocument;

class HTMLCanvasElement : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLCanvasElementWrapper;

    HTMLCanvasElement(Document&, const FlyString& tag_name);
    virtual ~HTMLCanvasElement() override;

    const Gfx::Bitmap* bitmap() const { return m_bitmap; }
    Gfx::Bitmap* bitmap() { return m_bitmap; }
    bool create_bitmap();

    CanvasRenderingContext2D* get_context(String type);

    int requested_width() const;
    int requested_height() const;

private:
    virtual RefPtr<LayoutNode> create_layout_node(const StyleProperties* parent_style) const override;

    RefPtr<Gfx::Bitmap> m_bitmap;
    RefPtr<CanvasRenderingContext2D> m_context;
};

template<>
inline bool is<HTMLCanvasElement>(const Node& node)
{
    return is<Element>(node) && to<Element>(node).tag_name().equals_ignoring_case("canvas");
}

}
