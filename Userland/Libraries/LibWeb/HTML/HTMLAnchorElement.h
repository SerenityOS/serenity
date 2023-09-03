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

    DeprecatedString rel() const { return deprecated_attribute(HTML::AttributeNames::rel); }
    DeprecatedString target() const { return deprecated_attribute(HTML::AttributeNames::target); }
    DeprecatedString download() const { return deprecated_attribute(HTML::AttributeNames::download); }

    DeprecatedString text() const;
    void set_text(DeprecatedString const&);

    DeprecatedString referrer_policy() const;
    WebIDL::ExceptionOr<void> set_referrer_policy(DeprecatedString const&);

    // ^EventTarget
    // https://html.spec.whatwg.org/multipage/interaction.html#the-tabindex-attribute:the-a-element
    virtual bool is_focusable() const override { return has_attribute(HTML::AttributeNames::href); }

    virtual bool is_html_anchor_element() const override { return true; }

private:
    HTMLAnchorElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;

    void run_activation_behavior(Web::DOM::Event const&);

    // ^DOM::Element
    virtual void attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value) override;
    virtual i32 default_tab_index_value() const override;

    // ^HTML::HTMLHyperlinkElementUtils
    virtual DOM::Document& hyperlink_element_utils_document() override { return document(); }
    virtual DeprecatedString hyperlink_element_utils_href() const override;
    virtual WebIDL::ExceptionOr<void> set_hyperlink_element_utils_href(DeprecatedString) override;
    virtual bool hyperlink_element_utils_is_html_anchor_element() const final { return true; }
    virtual bool hyperlink_element_utils_is_connected() const final { return is_connected(); }
    virtual void hyperlink_element_utils_queue_an_element_task(HTML::Task::Source source, Function<void()> steps) override
    {
        queue_an_element_task(source, move(steps));
    }
    virtual DeprecatedString hyperlink_element_utils_get_an_elements_target() const override
    {
        return get_an_elements_target();
    }
    virtual TokenizedFeature::NoOpener hyperlink_element_utils_get_an_elements_noopener(StringView target) const override
    {
        return get_an_elements_noopener(target);
    }

    virtual Optional<ARIA::Role> default_role() const override;
};

}

namespace Web::DOM {
template<>
inline bool Node::fast_is<HTML::HTMLAnchorElement>() const { return is_html_anchor_element(); }
}
