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
    WEB_PLATFORM_OBJECT(HTMLAnchorElement, HTMLElement);

public:
    virtual ~HTMLAnchorElement() override;

    String target() const { return attribute(HTML::AttributeNames::target); }
    String download() const { return attribute(HTML::AttributeNames::download); }

    // ^EventTarget
    // https://html.spec.whatwg.org/multipage/interaction.html#the-tabindex-attribute:the-a-element
    virtual bool is_focusable() const override { return has_attribute(HTML::AttributeNames::href); }

    virtual bool is_html_anchor_element() const override { return true; }

private:
    HTMLAnchorElement(DOM::Document&, DOM::QualifiedName);

    void run_activation_behavior(Web::DOM::Event const&);

    // ^DOM::Element
    virtual void parse_attribute(FlyString const& name, String const& value) override;

    // ^HTML::HTMLHyperlinkElementUtils
    virtual DOM::Document& hyperlink_element_utils_document() override { return document(); }
    virtual String hyperlink_element_utils_href() const override;
    virtual void set_hyperlink_element_utils_href(String) override;
    virtual bool hyperlink_element_utils_is_html_anchor_element() const final { return true; }
    virtual bool hyperlink_element_utils_is_connected() const final { return is_connected(); }
    virtual String hyperlink_element_utils_target() const final { return target(); }
    virtual void hyperlink_element_utils_queue_an_element_task(HTML::Task::Source source, Function<void()> steps) override
    {
        queue_an_element_task(source, move(steps));
    }
};

}

namespace Web::DOM {
template<>
inline bool Node::fast_is<HTML::HTMLAnchorElement>() const { return is_html_anchor_element(); }
}
