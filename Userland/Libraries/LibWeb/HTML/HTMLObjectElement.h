/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Forward.h>
#include <LibGfx/Forward.h>
#include <LibWeb/Fetch/ImageLoader.h>
#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLObjectElement final : public FormAssociatedElement {
public:
    using WrapperType = Bindings::HTMLObjectElementWrapper;

    HTMLObjectElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLObjectElement() override;

    virtual void parse_attribute(const FlyString& name, const String& value) override;

    String data() const { return attribute(HTML::AttributeNames::data); }
    String type() const { return attribute(HTML::AttributeNames::type); }

    // ^FormAssociatedElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-listed
    virtual bool is_listed() const override { return true; }

private:
    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    Fetch::ImageLoader m_image_loader;
    bool m_should_show_fallback_content { false };
};

}
