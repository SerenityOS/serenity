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
public:
    using WrapperType = Bindings::HTMLFrameSetElementWrapper;

    HTMLFrameSetElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLFrameSetElement() override;

    virtual void parse_attribute(FlyString const&, String const&) override;

private:
    // ^HTML::GlobalEventHandlers
    virtual EventTarget& global_event_handlers_to_event_target(FlyString const& event_name) override;

    // ^HTML::WindowEventHandlers
    virtual EventTarget& window_event_handlers_to_event_target() override;
};

}
