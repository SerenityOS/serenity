/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/HTML/WindowEventHandlers.h>

namespace Web::HTML {

class HTMLBodyElement final
    : public HTMLElement
    , public WindowEventHandlers {
    WEB_PLATFORM_OBJECT(HTMLBodyElement, HTMLElement);

public:
    virtual ~HTMLBodyElement() override;

    virtual void parse_attribute(FlyString const&, String const&) override;
    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;

private:
    HTMLBodyElement(DOM::Document&, DOM::QualifiedName);

    // ^HTML::GlobalEventHandlers
    virtual EventTarget& global_event_handlers_to_event_target(FlyString const& event_name) override;

    // ^HTML::WindowEventHandlers
    virtual EventTarget& window_event_handlers_to_event_target() override;

    RefPtr<CSS::ImageStyleValue> m_background_style_value;
};

}

WRAPPER_HACK(HTMLBodyElement, Web::HTML)
