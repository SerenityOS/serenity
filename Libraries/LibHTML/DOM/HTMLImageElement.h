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

#include <AK/ByteBuffer.h>
#include <LibGfx/Forward.h>
#include <LibHTML/DOM/HTMLElement.h>

class LayoutDocument;

class HTMLImageElement : public HTMLElement {
public:
    HTMLImageElement(Document&, const String& tag_name);
    virtual ~HTMLImageElement() override;

    virtual void parse_attribute(const String& name, const String& value) override;

    String alt() const { return attribute("alt"); }
    String src() const { return attribute("src"); }
    int preferred_width() const;
    int preferred_height() const;

    const Gfx::Bitmap* bitmap() const;
    const Gfx::ImageDecoder* image_decoder() const { return m_image_decoder; }

    void set_volatile(Badge<LayoutDocument>, bool);

private:
    void load_image(const String& src);

    virtual RefPtr<LayoutNode> create_layout_node(const StyleProperties* parent_style) const override;

    RefPtr<Gfx::ImageDecoder> m_image_decoder;
    ByteBuffer m_encoded_data;
};
