/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/HTML/HTMLHyperlinkElementUtils.h>

namespace Web::HTML {

class HTMLAreaElement final
    : public HTMLElement
    , public HTMLHyperlinkElementUtils {
public:
    using WrapperType = Bindings::HTMLAreaElementWrapper;

    HTMLAreaElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLAreaElement() override;

private:
    // ^DOM::Element
    virtual void parse_attribute(FlyString const& name, String const& value) override;

    // ^HTML::HTMLHyperlinkElementUtils
    virtual DOM::Document const& hyperlink_element_utils_document() const override { return document(); }
    virtual String hyperlink_element_utils_href() const override;
    virtual void set_hyperlink_element_utils_href(String) override;
};

}
