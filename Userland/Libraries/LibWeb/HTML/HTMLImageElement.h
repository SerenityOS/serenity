/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/OwnPtr.h>
#include <LibGfx/Forward.h>
#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/Loader/ImageLoader.h>

namespace Web::HTML {

class HTMLImageElement final
    : public HTMLElement
    , public FormAssociatedElement {
    WEB_PLATFORM_OBJECT(HTMLImageElement, HTMLElement);
    FORM_ASSOCIATED_ELEMENT(HTMLElement, HTMLImageElement)

public:
    virtual ~HTMLImageElement() override;

    virtual void parse_attribute(FlyString const& name, String const& value) override;

    String alt() const { return attribute(HTML::AttributeNames::alt); }
    String src() const { return attribute(HTML::AttributeNames::src); }

    Gfx::Bitmap const* bitmap() const;

    unsigned width() const;
    void set_width(unsigned);

    unsigned height() const;
    void set_height(unsigned);

    unsigned natural_width() const;
    unsigned natural_height() const;

    // https://html.spec.whatwg.org/multipage/embedded-content.html#dom-img-complete
    bool complete() const;

private:
    HTMLImageElement(DOM::Document&, DOM::QualifiedName);

    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;

    void animate();

    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    ImageLoader m_image_loader;
};

}
