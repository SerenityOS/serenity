/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLBodyElement final : public HTMLElement {
public:
    using WrapperType = Bindings::HTMLBodyElementWrapper;

    HTMLBodyElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLBodyElement() override;

    virtual void parse_attribute(const FlyString&, const String&) override;
    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;

private:
    // ^HTML::GlobalEventHandlers
    virtual EventTarget& global_event_handlers_to_event_target() override;

    RefPtr<CSS::ImageStyleValue> m_background_style_value;
};

}
