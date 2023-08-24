/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Forward.h>
#include <LibJS/Heap/Cell.h>
#include <LibWeb/HTML/HistoryHandlingBehavior.h>
#include <LibWeb/HTML/PolicyContainers.h>
#include <LibWeb/HTML/TokenizedFeatures.h>
#include <LibWeb/HTML/WindowProxy.h>

namespace Web::HTML {

class AbstractBrowsingContext : public JS::Cell {
    JS_CELL(AbstractBrowsingContext, Cell);

public:
    virtual HTML::WindowProxy* window_proxy() = 0;
    virtual HTML::WindowProxy const* window_proxy() const = 0;

    String const& name() const { return m_name; }
    void set_name(String const& name) { m_name = name; }

    JS::GCPtr<BrowsingContext> opener_browsing_context() const { return m_opener_browsing_context; }
    void set_opener_browsing_context(JS::GCPtr<BrowsingContext> browsing_context) { m_opener_browsing_context = browsing_context; }

    void set_is_popup(TokenizedFeature::Popup is_popup) { m_is_popup = is_popup; }

    virtual String const& window_handle() const = 0;
    virtual void set_window_handle(String handle) = 0;

protected:
    String m_name;

    // https://html.spec.whatwg.org/multipage/browsers.html#is-popup
    TokenizedFeature::Popup m_is_popup { TokenizedFeature::Popup::No };

    // https://html.spec.whatwg.org/multipage/browsers.html#opener-browsing-context
    JS::GCPtr<BrowsingContext> m_opener_browsing_context;
};

}
