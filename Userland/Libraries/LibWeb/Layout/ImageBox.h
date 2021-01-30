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
#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/Page/Frame.h>

namespace Web::Layout {

class ImageBox
    : public ReplacedBox
    , public Frame::ViewportClient {
public:
    ImageBox(DOM::Document&, DOM::Element&, NonnullRefPtr<CSS::StyleProperties>, const ImageLoader&);
    virtual ~ImageBox() override;

    virtual void prepare_for_replaced_layout() override;
    virtual void paint(PaintContext&, PaintPhase) override;

    const DOM::Element& dom_node() const { return static_cast<const DOM::Element&>(ReplacedBox::dom_node()); }

    bool renders_as_alt_text() const;

private:
    virtual void frame_did_set_viewport_rect(const Gfx::IntRect&) final;

    int preferred_width() const;
    int preferred_height() const;

    const ImageLoader& m_image_loader;
};

}
