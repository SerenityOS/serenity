/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/HTML/HTMLHyperlinkElementUtils.h>

namespace Web::HTML {

class HTMLAnchorElement final
    : public HTMLElement
    , public HTMLHyperlinkElementUtils {
public:
    using WrapperType = Bindings::HTMLAnchorElementWrapper;

    HTMLAnchorElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLAnchorElement() override;

    String target() const { return attribute(HTML::AttributeNames::target); }

    virtual bool is_focusable() const override { return has_attribute(HTML::AttributeNames::href); }

private:
    // ^DOM::Element
    virtual void parse_attribute(FlyString const& name, String const& value) override;

    // ^HTML::HTMLHyperlinkElementUtils
    virtual DOM::Document const& hyperlink_element_utils_document() const override { return document(); }
    virtual String hyperlink_element_utils_href() const override;
    virtual void set_hyperlink_element_utils_href(String) override;
};

}
