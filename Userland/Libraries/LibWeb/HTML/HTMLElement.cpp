/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibWeb/DOM/DOMException.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/BrowsingContextContainer.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/BreakNode.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/UIEvents/EventNames.h>
#include <LibWeb/UIEvents/FocusEvent.h>

namespace Web::HTML {

HTMLElement::HTMLElement(DOM::Document& document, QualifiedName qualified_name)
    : Element(document, move(qualified_name))
    , m_dataset(DOMStringMap::create(*this))
{
}

HTMLElement::~HTMLElement()
{
}

HTMLElement::ContentEditableState HTMLElement::content_editable_state() const
{
    auto contenteditable = attribute(HTML::AttributeNames::contenteditable);
    // "true", an empty string or a missing value map to the "true" state.
    if ((!contenteditable.is_null() && contenteditable.is_empty()) || contenteditable.equals_ignoring_case("true"))
        return ContentEditableState::True;
    // "false" maps to the "false" state.
    if (contenteditable.equals_ignoring_case("false"))
        return ContentEditableState::False;
    // Having no such attribute or an invalid value maps to the "inherit" state.
    return ContentEditableState::Inherit;
}

bool HTMLElement::is_editable() const
{
    switch (content_editable_state()) {
    case ContentEditableState::True:
        return true;
    case ContentEditableState::False:
        return false;
    case ContentEditableState::Inherit:
        return parent() && parent()->is_editable();
    default:
        VERIFY_NOT_REACHED();
    }
}

String HTMLElement::content_editable() const
{
    switch (content_editable_state()) {
    case ContentEditableState::True:
        return "true";
    case ContentEditableState::False:
        return "false";
    case ContentEditableState::Inherit:
        return "inherit";
    default:
        VERIFY_NOT_REACHED();
    }
}

// https://html.spec.whatwg.org/multipage/interaction.html#contenteditable
DOM::ExceptionOr<void> HTMLElement::set_content_editable(const String& content_editable)
{
    if (content_editable.equals_ignoring_case("inherit")) {
        remove_attribute(HTML::AttributeNames::contenteditable);
        return {};
    }
    if (content_editable.equals_ignoring_case("true")) {
        set_attribute(HTML::AttributeNames::contenteditable, "true");
        return {};
    }
    if (content_editable.equals_ignoring_case("false")) {
        set_attribute(HTML::AttributeNames::contenteditable, "false");
        return {};
    }
    return DOM::SyntaxError::create("Invalid contentEditable value, must be 'true', 'false', or 'inherit'");
}

void HTMLElement::set_inner_text(StringView text)
{
    remove_all_children();
    append_child(document().create_text_node(text));

    set_needs_style_update(true);
}

String HTMLElement::inner_text()
{
    StringBuilder builder;

    // innerText for element being rendered takes visibility into account, so force a layout and then walk the layout tree.
    document().update_layout();
    if (!layout_node())
        return text_content();

    Function<void(const Layout::Node&)> recurse = [&](auto& node) {
        for (auto* child = node.first_child(); child; child = child->next_sibling()) {
            if (is<Layout::TextNode>(child))
                builder.append(verify_cast<Layout::TextNode>(*child).text_for_rendering());
            if (is<Layout::BreakNode>(child))
                builder.append('\n');
            recurse(*child);
        }
    };
    recurse(*layout_node());

    return builder.to_string();
}

// // https://drafts.csswg.org/cssom-view/#dom-htmlelement-offsettop
int HTMLElement::offset_top() const
{
    if (is<HTML::HTMLBodyElement>(this) || !layout_node() || !parent_element() || !parent_element()->layout_node())
        return 0;
    auto position = layout_node()->box_type_agnostic_position();
    auto parent_position = parent_element()->layout_node()->box_type_agnostic_position();
    return position.y() - parent_position.y();
}

// https://drafts.csswg.org/cssom-view/#dom-htmlelement-offsetleft
int HTMLElement::offset_left() const
{
    if (is<HTML::HTMLBodyElement>(this) || !layout_node() || !parent_element() || !parent_element()->layout_node())
        return 0;
    auto position = layout_node()->box_type_agnostic_position();
    auto parent_position = parent_element()->layout_node()->box_type_agnostic_position();
    return position.x() - parent_position.x();
}

// https://drafts.csswg.org/cssom-view/#dom-htmlelement-offsetwidth
int HTMLElement::offset_width() const
{
    if (!layout_node() || !layout_node()->is_box())
        return 0;
    return static_cast<Layout::Box const&>(*layout_node()).border_box_width();
}

// https://drafts.csswg.org/cssom-view/#dom-htmlelement-offsetheight
int HTMLElement::offset_height() const
{
    if (!layout_node() || !layout_node()->is_box())
        return 0;
    return static_cast<Layout::Box const&>(*layout_node()).border_box_height();
}

bool HTMLElement::cannot_navigate() const
{
    // FIXME: Return true if element's node document is not fully active
    return !is<HTML::HTMLAnchorElement>(this) && !is_connected();
}

void HTMLElement::parse_attribute(const FlyString& name, const String& value)
{
    Element::parse_attribute(name, value);

#undef __ENUMERATE
#define __ENUMERATE(attribute_name, event_name)                          \
    if (name == HTML::AttributeNames::attribute_name) {                  \
        set_event_handler_attribute(event_name, EventHandler { value }); \
    }
    ENUMERATE_GLOBAL_EVENT_HANDLERS(__ENUMERATE)
#undef __ENUMERATE
}

// https://html.spec.whatwg.org/multipage/interaction.html#focus-update-steps
static void run_focus_update_steps(NonnullRefPtrVector<DOM::Node> old_chain, NonnullRefPtrVector<DOM::Node> new_chain, DOM::Node& new_focus_target)
{
    // 1. If the last entry in old chain and the last entry in new chain are the same,
    //    pop the last entry from old chain and the last entry from new chain and redo this step.
    while (!old_chain.is_empty()
        && !new_chain.is_empty()
        && &old_chain.last() == &new_chain.last()) {
        (void)old_chain.take_last();
        (void)new_chain.take_last();
    }

    // 2. For each entry entry in old chain, in order, run these substeps:
    for (auto& entry : old_chain) {
        // FIXME: 1. If entry is an input element, and the change event applies to the element,
        //           and the element does not have a defined activation behavior,
        //           and the user has changed the element's value or its list of selected files
        //           while the control was focused without committing that change
        //           (such that it is different to what it was when the control was first focused),
        //           then fire an event named change at the element,
        //           with the bubbles attribute initialized to true.

        RefPtr<DOM::EventTarget> blur_event_target;
        if (is<DOM::Element>(entry)) {
            // 2. If entry is an element, let blur event target be entry.
            blur_event_target = entry;
        } else if (is<DOM::Document>(entry)) {
            // If entry is a Document object, let blur event target be that Document object's relevant global object.
            blur_event_target = static_cast<DOM::Document&>(entry).window();
        }

        // 3. If entry is the last entry in old chain, and entry is an Element,
        //    and the last entry in new chain is also an Element,
        //    then let related blur target be the last entry in new chain.
        //    Otherwise, let related blur target be null.
        RefPtr<DOM::EventTarget> related_blur_target;
        if (!old_chain.is_empty()
            && &entry == &old_chain.last()
            && is<DOM::Element>(entry)
            && !new_chain.is_empty()
            && is<DOM::Element>(new_chain.last())) {
            related_blur_target = new_chain.last();
        }

        // 4. If blur event target is not null, fire a focus event named blur at blur event target,
        //    with related blur target as the related target.
        if (blur_event_target) {
            // FIXME: Implement the "fire a focus event" spec operation.
            auto blur_event = UIEvents::FocusEvent::create(HTML::EventNames::blur);
            blur_event->set_related_target(related_blur_target);
            blur_event_target->dispatch_event(move(blur_event));
        }
    }

    // FIXME: 3. Apply any relevant platform-specific conventions for focusing new focus target.
    //           (For example, some platforms select the contents of a text control when that control is focused.)
    (void)new_focus_target;

    // 4. For each entry entry in new chain, in reverse order, run these substeps:
    for (ssize_t i = new_chain.size() - 1; i >= 0; --i) {
        auto& entry = new_chain[i];

        // 1. If entry is a focusable area: designate entry as the focused area of the document.
        // FIXME: This isn't entirely right.
        if (is<DOM::Element>(entry))
            entry.document().set_focused_element(&static_cast<DOM::Element&>(entry));

        RefPtr<DOM::EventTarget> focus_event_target;
        if (is<DOM::Element>(entry)) {
            // 2. If entry is an element, let focus event target be entry.
            focus_event_target = entry;
        } else if (is<DOM::Document>(entry)) {
            // If entry is a Document object, let focus event target be that Document object's relevant global object.
            focus_event_target = static_cast<DOM::Document&>(entry).window();
        }

        // 3. If entry is the last entry in new chain, and entry is an Element,
        //    and the last entry in old chain is also an Element,
        //    then let related focus target be the last entry in old chain.
        //    Otherwise, let related focus target be null.
        RefPtr<DOM::EventTarget> related_focus_target;
        if (!new_chain.is_empty()
            && &entry == &new_chain.last()
            && is<DOM::Element>(entry)
            && !old_chain.is_empty()
            && is<DOM::Element>(old_chain.last())) {
            related_focus_target = old_chain.last();
        }

        // 4. If focus event target is not null, fire a focus event named focus at focus event target,
        //    with related focus target as the related target.
        if (focus_event_target) {
            // FIXME: Implement the "fire a focus event" spec operation.
            auto focus_event = UIEvents::FocusEvent::create(HTML::EventNames::focus);
            focus_event->set_related_target(related_focus_target);
            focus_event_target->dispatch_event(move(focus_event));
        }
    }
}
// https://html.spec.whatwg.org/multipage/interaction.html#focus-chain
static NonnullRefPtrVector<DOM::Node> focus_chain(DOM::Node* subject)
{
    // FIXME: Move this somewhere more spec-friendly.
    if (!subject)
        return {};

    // 1. Let output be an empty list.
    NonnullRefPtrVector<DOM::Node> output;

    // 2. Let currentObject be subject.
    auto* current_object = subject;

    // 3. While true:
    while (true) {
        // 1. Append currentObject to output.
        output.append(*current_object);

        // FIXME: 2. If currentObject is an area element's shape, then append that area element to output.

        // FIXME:    Otherwise, if currentObject's DOM anchor is an element that is not currentObject itself, then append currentObject's DOM anchor to output.

        // FIXME: Everything below needs work. The conditions are not entirely right.
        if (!is<DOM::Document>(*current_object)) {
            // 3. If currentObject is a focusable area, then set currentObject to currentObject's DOM anchor's node document.
            current_object = &current_object->document();
        } else if (is<DOM::Document>(*current_object)
            && static_cast<DOM::Document&>(*current_object).browsing_context()
            && !static_cast<DOM::Document&>(*current_object).browsing_context()->is_top_level()) {
            // Otherwise, if currentObject is a Document whose browsing context is a child browsing context,
            // then set currentObject to currentObject's browsing context's container.
            current_object = static_cast<DOM::Document&>(*current_object).browsing_context()->container();
        } else {
            break;
        }
    }

    // 4. Return output.
    return output;
}

// https://html.spec.whatwg.org/multipage/interaction.html#focusing-steps
// FIXME: This should accept more types.
static void run_focusing_steps(DOM::Node* new_focus_target, DOM::Node* fallback_target = nullptr, [[maybe_unused]] Optional<String> focus_trigger = {})
{
    // FIXME: 1. If new focus target is not a focusable area, then set new focus target
    //           to the result of getting the focusable area for new focus target,
    //           given focus trigger if it was passed.

    // 2. If new focus target is null, then:
    if (!new_focus_target) {
        // 1. If no fallback target was specified, then return.
        if (!fallback_target)
            return;

        // 2. Otherwise, set new focus target to the fallback target.
        new_focus_target = fallback_target;
    }

    // 3. If new focus target is a browsing context container with non-null nested browsing context,
    //    then set new focus target to the nested browsing context's active document.
    if (is<BrowsingContextContainer>(*new_focus_target)) {
        auto& browsing_context_container = static_cast<BrowsingContextContainer&>(*new_focus_target);
        if (auto* nested_browsing_context = browsing_context_container.nested_browsing_context())
            new_focus_target = nested_browsing_context->active_document();
    }

    // FIXME: 4. If new focus target is a focusable area and its DOM anchor is inert, then return.

    // 5. If new focus target is the currently focused area of a top-level browsing context, then return.
    if (!new_focus_target->document().browsing_context())
        return;
    auto& top_level_browsing_context = new_focus_target->document().browsing_context()->top_level_browsing_context();
    if (new_focus_target == top_level_browsing_context.currently_focused_area())
        return;

    // 6. Let old chain be the current focus chain of the top-level browsing context in which
    //    new focus target finds itself.
    auto old_chain = focus_chain(top_level_browsing_context.currently_focused_area());

    // 7. Let new chain be the focus chain of new focus target.
    auto new_chain = focus_chain(new_focus_target);

    run_focus_update_steps(old_chain, new_chain, *new_focus_target);
}

// https://html.spec.whatwg.org/multipage/interaction.html#dom-focus
void HTMLElement::focus()
{
    // 1. If the element is marked as locked for focus, then return.
    if (m_locked_for_focus)
        return;

    // 2. Mark the element as locked for focus.
    m_locked_for_focus = true;

    // 3. Run the focusing steps for the element.
    run_focusing_steps(this);

    // FIXME: 4. If the value of the preventScroll dictionary member of options is false,
    //           then scroll the element into view with scroll behavior "auto",
    //           block flow direction position set to an implementation-defined value,
    //           and inline base direction position set to an implementation-defined value.

    // 5. Unmark the element as locked for focus.
    m_locked_for_focus = false;
}
}
