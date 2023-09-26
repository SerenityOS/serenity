/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/CSS/StyleValues/DisplayStyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/HTMLTextAreaElement.h>
#include <LibWeb/Namespace.h>

namespace Web::HTML {

HTMLTextAreaElement::HTMLTextAreaElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
    , m_raw_value(DeprecatedString::empty())
{
}

HTMLTextAreaElement::~HTMLTextAreaElement() = default;

JS::GCPtr<Layout::Node> HTMLTextAreaElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    // AD-HOC: We rewrite `display: inline` to `display: inline-block`.
    //         This is required for the internal shadow tree to work correctly in layout.
    if (style->display().is_inline_outside() && style->display().is_flow_inside())
        style->set_property(CSS::PropertyID::Display, CSS::DisplayStyleValue::create(CSS::Display::from_short(CSS::Display::Short::InlineBlock)));

    return Element::create_layout_node_for_display_type(document(), style->display(), style, this);
}

void HTMLTextAreaElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLTextAreaElementPrototype>(realm, "HTMLTextAreaElement"));
}

void HTMLTextAreaElement::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_inner_text_element);
    visitor.visit(m_text_node);
}

void HTMLTextAreaElement::did_receive_focus()
{
    auto* browsing_context = document().browsing_context();
    if (!browsing_context)
        return;
    if (!m_text_node)
        return;
    browsing_context->set_cursor_position(DOM::Position::create(*vm().current_realm(), *m_text_node, 0));
}

void HTMLTextAreaElement::did_lose_focus()
{
    // The change event fires when the value is committed, if that makes sense for the control,
    // or else when the control loses focus
    queue_an_element_task(HTML::Task::Source::UserInteraction, [this] {
        auto change_event = DOM::Event::create(realm(), HTML::EventNames::change);
        change_event->set_bubbles(true);
        dispatch_event(change_event);
    });
}

// https://html.spec.whatwg.org/multipage/interaction.html#dom-tabindex
i32 HTMLTextAreaElement::default_tab_index_value() const
{
    // See the base function for the spec comments.
    return 0;
}

// https://html.spec.whatwg.org/multipage/form-elements.html#the-textarea-element:concept-form-reset-control
void HTMLTextAreaElement::reset_algorithm()
{
    // The reset algorithm for textarea elements is to set the dirty value flag back to false,
    m_dirty = false;
    // and set the raw value of element to its child text content.
    m_raw_value = child_text_content();
}

void HTMLTextAreaElement::form_associated_element_was_inserted()
{
    create_shadow_tree_if_needed();
}

void HTMLTextAreaElement::create_shadow_tree_if_needed()
{
    if (shadow_root_internal())
        return;

    auto shadow_root = heap().allocate<DOM::ShadowRoot>(realm(), document(), *this, Bindings::ShadowRootMode::Closed);
    auto element = MUST(DOM::create_element(document(), HTML::TagNames::div, Namespace::HTML));

    m_inner_text_element = MUST(DOM::create_element(document(), HTML::TagNames::div, Namespace::HTML));

    m_text_node = heap().allocate<DOM::Text>(realm(), document(), String {});
    m_text_node->set_always_editable(true);
    m_text_node->set_editable_text_node_owner(Badge<HTMLTextAreaElement> {}, *this);
    // NOTE: If `children_changed()` was called before now, `m_raw_value` will hold the text content.
    //       Otherwise, it will get filled in whenever that does get called.
    m_text_node->set_text_content(MUST(String::from_deprecated_string(m_raw_value)));

    MUST(m_inner_text_element->append_child(*m_text_node));
    MUST(element->append_child(*m_inner_text_element));
    MUST(shadow_root->append_child(element));
    set_shadow_root(shadow_root);
}

// https://html.spec.whatwg.org/multipage/form-elements.html#the-textarea-element:children-changed-steps
void HTMLTextAreaElement::children_changed()
{
    // The children changed steps for textarea elements must, if the element's dirty value flag is false,
    // set the element's raw value to its child text content.
    if (!m_dirty) {
        m_raw_value = child_text_content();
        if (m_text_node)
            m_text_node->set_text_content(MUST(String::from_deprecated_string(m_raw_value)));
    }
}

void HTMLTextAreaElement::did_edit_text_node(Badge<Web::HTML::BrowsingContext>)
{
    // A textarea element's dirty value flag must be set to true whenever the user interacts with the control in a way that changes the raw value.
    m_dirty = true;
}

}
