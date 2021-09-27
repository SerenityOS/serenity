/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/Loader/CSSLoader.h>

namespace Web::HTML {

class HTMLLinkElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLLinkElementWrapper;

    HTMLLinkElement(DOM::Document&, QualifiedName);
    virtual ~HTMLLinkElement() override;

    virtual void inserted() override;

    String rel() const { return attribute(HTML::AttributeNames::rel); }
    String type() const { return attribute(HTML::AttributeNames::type); }
    String href() const { return attribute(HTML::AttributeNames::href); }

private:
    void parse_attribute(const FlyString&, const String&) override;

    struct Relationship {
        enum {
            Alternate = 1 << 0,
            Stylesheet = 1 << 1,
            Preload = 1 << 2,
            DNSPrefetch = 1 << 3,
            Preconnect = 1 << 4,
        };
    };

    RefPtr<Resource> m_preload_resource;

    CSSLoader m_css_loader;
    unsigned m_relationship { 0 };
};

}
