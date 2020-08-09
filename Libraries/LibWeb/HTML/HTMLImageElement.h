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
#include <AK/OwnPtr.h>
#include <LibGfx/Forward.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/Loader/ImageLoader.h>

namespace Web::HTML {

class HTMLImageElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLImageElementWrapper;

    HTMLImageElement(DOM::Document&, const FlyString& local_name);
    virtual ~HTMLImageElement() override;

    virtual void parse_attribute(const FlyString& name, const String& value) override;

    String alt() const { return attribute(HTML::AttributeNames::alt); }
    String src() const { return attribute(HTML::AttributeNames::src); }

    const Gfx::Bitmap* bitmap() const;

private:
    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;

    void animate();

    virtual RefPtr<LayoutNode> create_layout_node(const CSS::StyleProperties* parent_style) override;

    ImageLoader m_image_loader;
};

}

AK_BEGIN_TYPE_TRAITS(Web::HTML::HTMLImageElement)
static bool is_type(const Web::DOM::Node& node) { return node.is_element() && downcast<Web::DOM::Element>(node).local_name() == Web::HTML::TagNames::img; }
AK_END_TYPE_TRAITS()
