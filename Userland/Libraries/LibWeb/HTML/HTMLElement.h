/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Element.h>
#include <LibWeb/HTML/DOMStringMap.h>
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
    DOM::ExceptionOr<void> set_content_editable(const String&);

    String inner_text();
    void set_inner_text(StringView);

    int offset_top() const;
    int offset_left() const;
    int offset_width() const;
    int offset_height() const;

    bool cannot_navigate() const;

    NonnullRefPtr<DOMStringMap> dataset() const { return m_dataset; }

    void focus();

    void click();

protected:
    virtual void parse_attribute(const FlyString& name, const String& value) override;

private:
    // ^HTML::GlobalEventHandlers
    virtual EventTarget& global_event_handlers_to_event_target() override { return *this; }

    enum class ContentEditableState {
        True,
        False,
        Inherit,
    };
    ContentEditableState content_editable_state() const;

    NonnullRefPtr<DOMStringMap> m_dataset;

    // https://html.spec.whatwg.org/multipage/interaction.html#locked-for-focus
    bool m_locked_for_focus { false };

    // https://html.spec.whatwg.org/multipage/interaction.html#click-in-progress-flag
    bool m_click_in_progress { false };
};

}
