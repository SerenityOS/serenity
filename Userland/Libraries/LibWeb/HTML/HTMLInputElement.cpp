/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGfx/FontDatabase.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/InProcessWebView.h>
#include <LibWeb/Layout/BlockBox.h>
#include <LibWeb/Layout/ButtonBox.h>
#include <LibWeb/Layout/CheckBox.h>
#include <LibWeb/Layout/RadioButton.h>
#include <LibWeb/Page/Frame.h>

namespace Web::HTML {

HTMLInputElement::HTMLInputElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLInputElement::~HTMLInputElement()
{
}

void HTMLInputElement::did_click_button(Badge<Layout::ButtonBox>)
{
    // FIXME: This should be a PointerEvent.
    dispatch_event(DOM::Event::create(EventNames::click));

    if (type().equals_ignoring_case("submit")) {
        if (auto* form = first_ancestor_of_type<HTMLFormElement>()) {
            form->submit_form(this);
        }
        return;
    }
}

RefPtr<Layout::Node> HTMLInputElement::create_layout_node()
{
    if (type() == "hidden")
        return nullptr;

    auto style = document().style_resolver().resolve_style(*this);
    if (style->display() == CSS::Display::None)
        return nullptr;

    if (type().equals_ignoring_case("submit") || type().equals_ignoring_case("button"))
        return adopt(*new Layout::ButtonBox(document(), *this, move(style)));

    if (type() == "checkbox")
        return adopt(*new Layout::CheckBox(document(), *this, move(style)));

    if (type() == "radio")
        return adopt(*new Layout::RadioButton(document(), *this, move(style)));

    create_shadow_tree_if_needed();
    auto layout_node = adopt(*new Layout::BlockBox(document(), this, move(style)));
    layout_node->set_inline(true);
    return layout_node;
}

void HTMLInputElement::set_checked(bool checked)
{
    if (m_checked == checked)
        return;
    m_checked = checked;
    if (layout_node())
        layout_node()->set_needs_display();

    dispatch_event(DOM::Event::create(EventNames::change));
}

bool HTMLInputElement::enabled() const
{
    return !has_attribute(HTML::AttributeNames::disabled);
}

String HTMLInputElement::value() const
{
    if (m_text_node)
        return m_text_node->data();
    return default_value();
}

void HTMLInputElement::set_value(String value)
{
    if (m_text_node) {
        m_text_node->set_data(move(value));
        return;
    }
    set_attribute(HTML::AttributeNames::value, move(value));
}

void HTMLInputElement::create_shadow_tree_if_needed()
{
    if (shadow_root())
        return;

    // FIXME: This assumes that we want a text box. Is that always true?
    auto shadow_root = adopt(*new DOM::ShadowRoot(document(), *this));
    auto initial_value = attribute(HTML::AttributeNames::value);
    if (initial_value.is_null())
        initial_value = String::empty();
    auto element = document().create_element(HTML::TagNames::div);
    element->set_attribute(HTML::AttributeNames::style, "white-space: pre");
    m_text_node = adopt(*new DOM::Text(document(), initial_value));
    m_text_node->set_always_editable(true);
    element->append_child(*m_text_node);
    shadow_root->append_child(move(element));
    set_shadow_root(move(shadow_root));
}

}
