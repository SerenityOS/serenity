/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Forward.h>
#include <LibGfx/Forward.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/Loader/ImageLoader.h>

namespace Web::HTML {

class HTMLObjectElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLObjectElementWrapper;

    HTMLObjectElement(DOM::Document&, QualifiedName);
    virtual ~HTMLObjectElement() override;

    virtual void parse_attribute(const FlyString& name, const String& value) override;

    String data() const { return attribute(HTML::AttributeNames::data); }
    String type() const { return attribute(HTML::AttributeNames::type); }

private:
    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    ImageLoader m_image_loader;
    bool m_should_show_fallback_content { false };
};

}
