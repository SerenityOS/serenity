/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/NavigableContainer.h>

namespace Web::HTML {

class HTMLIFrameElement final : public NavigableContainer {
    WEB_PLATFORM_OBJECT(HTMLIFrameElement, NavigableContainer);

public:
    virtual ~HTMLIFrameElement() override;

    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    // https://html.spec.whatwg.org/multipage/urls-and-fetching.html#will-lazy-load-element-steps
    bool will_lazy_load_element() const;

    void set_current_navigation_was_lazy_loaded(bool value) { m_current_navigation_was_lazy_loaded = value; }

    Optional<HighResolutionTime::DOMHighResTimeStamp> const& pending_resource_start_time() const { return m_pending_resource_start_time; }
    void set_pending_resource_start_time(Optional<HighResolutionTime::DOMHighResTimeStamp> time) { m_pending_resource_start_time = time; }

    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;

private:
    HTMLIFrameElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;

    // ^DOM::Element
    virtual void inserted() override;
    virtual void removed_from(Node*) override;
    virtual void attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value) override;
    virtual i32 default_tab_index_value() const override;

    // https://html.spec.whatwg.org/multipage/iframe-embed-object.html#process-the-iframe-attributes
    void process_the_iframe_attributes(bool initial_insertion = false);

    // https://html.spec.whatwg.org/multipage/iframe-embed-object.html#current-navigation-was-lazy-loaded
    bool m_current_navigation_was_lazy_loaded { false };

    // https://html.spec.whatwg.org/multipage/iframe-embed-object.html#iframe-pending-resource-timing-start-time
    Optional<HighResolutionTime::DOMHighResTimeStamp> m_pending_resource_start_time = {};
};

void run_iframe_load_event_steps(HTML::HTMLIFrameElement&);

}
