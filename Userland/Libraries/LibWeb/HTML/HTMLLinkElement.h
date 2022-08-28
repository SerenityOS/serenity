/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/DocumentLoadEventDelayer.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLLinkElement final
    : public HTMLElement
    , public ResourceClient {
    WEB_PLATFORM_OBJECT(HTMLLinkElement, HTMLElement);

public:
    virtual ~HTMLLinkElement() override;

    virtual void inserted() override;

    String rel() const { return attribute(HTML::AttributeNames::rel); }
    String type() const { return attribute(HTML::AttributeNames::type); }
    String href() const { return attribute(HTML::AttributeNames::href); }

    bool has_loaded_icon() const;
    bool load_favicon_and_use_if_window_is_active();

private:
    HTMLLinkElement(DOM::Document&, DOM::QualifiedName);

    void parse_attribute(FlyString const&, String const&) override;

    // ^ResourceClient
    virtual void resource_did_fail() override;
    virtual void resource_did_load() override;

    void resource_did_load_stylesheet();
    void resource_did_load_favicon();

    struct Relationship {
        enum {
            Alternate = 1 << 0,
            Stylesheet = 1 << 1,
            Preload = 1 << 2,
            DNSPrefetch = 1 << 3,
            Preconnect = 1 << 4,
            Icon = 1 << 5,
        };
    };

    RefPtr<Resource> m_preload_resource;

    Optional<DOM::DocumentLoadEventDelayer> m_document_load_event_delayer;
    unsigned m_relationship { 0 };
};

}

WRAPPER_HACK(HTMLLinkElement, Web::HTML)
