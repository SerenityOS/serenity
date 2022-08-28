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
    WEB_PLATFORM_OBJECT(HTMLElement, DOM::Element);

public:
    virtual ~HTMLElement() override;

    String title() const { return attribute(HTML::AttributeNames::title); }

    virtual bool is_editable() const final;
    String content_editable() const;
    DOM::ExceptionOr<void> set_content_editable(String const&);

    String inner_text();
    void set_inner_text(StringView);

    int offset_top() const;
    int offset_left() const;
    int offset_width() const;
    int offset_height() const;

    bool cannot_navigate() const;

    DOMStringMap* dataset() { return m_dataset.ptr(); }
    DOMStringMap const* dataset() const { return m_dataset.ptr(); }

    void focus();

    void click();

    bool fire_a_synthetic_pointer_event(FlyString const& type, DOM::Element& target, bool not_trusted);

    // https://html.spec.whatwg.org/multipage/forms.html#category-label
    virtual bool is_labelable() const { return false; }

protected:
    HTMLElement(DOM::Document&, DOM::QualifiedName);

    virtual void parse_attribute(FlyString const& name, String const& value) override;

    virtual void visit_edges(Cell::Visitor&) override;

private:
    virtual bool is_html_element() const final { return true; }

    // ^HTML::GlobalEventHandlers
    virtual DOM::EventTarget& global_event_handlers_to_event_target(FlyString const&) override { return *this; }

    enum class ContentEditableState {
        True,
        False,
        Inherit,
    };
    ContentEditableState content_editable_state() const;

    JS::NonnullGCPtr<DOMStringMap> m_dataset;

    // https://html.spec.whatwg.org/multipage/interaction.html#locked-for-focus
    bool m_locked_for_focus { false };

    // https://html.spec.whatwg.org/multipage/interaction.html#click-in-progress-flag
    bool m_click_in_progress { false };
};

}

namespace Web::DOM {
template<>
inline bool Node::fast_is<HTML::HTMLElement>() const { return is_html_element(); }
}

WRAPPER_HACK(HTMLElement, Web::HTML)
