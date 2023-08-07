/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/HTML/WindowEventHandlers.h>

namespace Web::HTML {

// NOTE: This element is marked as obsolete, but is still listed as required by the specification.
class HTMLFrameSetElement final
    : public HTMLElement
    , public WindowEventHandlers {
    WEB_PLATFORM_OBJECT(HTMLFrameSetElement, HTMLElement);

public:
    virtual ~HTMLFrameSetElement() override;

private:
    HTMLFrameSetElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
    virtual void attribute_changed(DeprecatedFlyString const&, DeprecatedString const&) override;

    // ^HTML::GlobalEventHandlers
    virtual EventTarget& global_event_handlers_to_event_target(FlyString const& event_name) override;

    // ^HTML::WindowEventHandlers
    virtual EventTarget& window_event_handlers_to_event_target() override;
};

}
