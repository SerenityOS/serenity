/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Element.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/GlobalEventHandlers.h>

namespace Web::HTML {

class HTMLElement
    : public DOM::Element
    , public HTML::GlobalEventHandlers {
public:
    using WrapperType = Bindings::HTMLElementWrapper;

    HTMLElement(DOM::Document&, QualifiedName);
    virtual ~HTMLElement() override;

    String title() const { return attribute(HTML::AttributeNames::title); }

    virtual bool is_editable() const final;
    String content_editable() const;
    DOM::ExceptionOr<void> set_content_editable(String const&);

    String inner_text();
    void set_inner_text(StringView);

    unsigned offset_top() const;
    unsigned offset_left() const;

    bool cannot_navigate() const;

protected:
    virtual void parse_attribute(FlyString const& name, String const& value) override;

private:
    // ^HTML::GlobalEventHandlers
    virtual EventTarget& global_event_handlers_to_event_target() override { return *this; }

    enum class ContentEditableState {
        True,
        False,
        Inherit,
    };
    ContentEditableState content_editable_state() const;
};

}
