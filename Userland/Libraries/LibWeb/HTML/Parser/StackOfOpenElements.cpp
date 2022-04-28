/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Element.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/HTML/Parser/StackOfOpenElements.h>

namespace Web::HTML {

static Vector<FlyString> s_base_list { "applet", "caption", "html", "table", "td", "th", "marquee", "object", "template" };

StackOfOpenElements::~StackOfOpenElements() = default;

bool StackOfOpenElements::has_in_scope_impl(FlyString const& tag_name, Vector<FlyString> const& list) const
{
    for (auto const& element : m_elements.in_reverse()) {
        if (element.local_name() == tag_name)
            return true;
        if (list.contains_slow(element.local_name()))
            return false;
    }
    VERIFY_NOT_REACHED();
}

bool StackOfOpenElements::has_in_scope(FlyString const& tag_name) const
{
    return has_in_scope_impl(tag_name, s_base_list);
}

bool StackOfOpenElements::has_in_scope_impl(const DOM::Element& target_node, Vector<FlyString> const& list) const
{
    for (auto& element : m_elements.in_reverse()) {
        if (&element == &target_node)
            return true;
        if (list.contains_slow(element.local_name()))
            return false;
    }
    VERIFY_NOT_REACHED();
}

bool StackOfOpenElements::has_in_scope(const DOM::Element& target_node) const
{
    return has_in_scope_impl(target_node, s_base_list);
}

bool StackOfOpenElements::has_in_button_scope(FlyString const& tag_name) const
{
    auto list = s_base_list;
    list.append("button");
    return has_in_scope_impl(tag_name, list);
}

bool StackOfOpenElements::has_in_table_scope(FlyString const& tag_name) const
{
    return has_in_scope_impl(tag_name, { "html", "table", "template" });
}

bool StackOfOpenElements::has_in_list_item_scope(FlyString const& tag_name) const
{
    auto list = s_base_list;
    list.append("ol");
    list.append("ul");
    return has_in_scope_impl(tag_name, list);
}

// https://html.spec.whatwg.org/multipage/parsing.html#has-an-element-in-select-scope
// The stack of open elements is said to have a particular element in select scope
// when it has that element in the specific scope consisting of all element types except the following:
// - optgroup in the HTML namespace
// - option in the HTML namespace
// NOTE: In this case it's "all element types _except_"
bool StackOfOpenElements::has_in_select_scope(FlyString const& tag_name) const
{
    // https://html.spec.whatwg.org/multipage/parsing.html#has-an-element-in-the-specific-scope
    // 1. Initialize node to be the current node (the bottommost node of the stack).
    for (auto& node : m_elements.in_reverse()) {
        // 2. If node is the target node, terminate in a match state.
        if (node.local_name() == tag_name)
            return true;
        // 3. Otherwise, if node is one of the element types in list, terminate in a failure state.
        // NOTE: Here "list" refers to all elements except option and optgroup
        if (node.local_name() != HTML::TagNames::option && node.local_name() != HTML::TagNames::optgroup)
            return false;
        // 4. Otherwise, set node to the previous entry in the stack of open elements and return to step 2.
    }
    // [4.] (This will never fail, since the loop will always terminate in the previous step if the top of the stack
    // — an html element — is reached.)
    VERIFY_NOT_REACHED();
}

bool StackOfOpenElements::contains(const DOM::Element& element) const
{
    for (auto& element_on_stack : m_elements) {
        if (&element == &element_on_stack)
            return true;
    }
    return false;
}

bool StackOfOpenElements::contains(FlyString const& tag_name) const
{
    for (auto& element_on_stack : m_elements) {
        if (element_on_stack.local_name() == tag_name)
            return true;
    }
    return false;
}

void StackOfOpenElements::pop_until_an_element_with_tag_name_has_been_popped(FlyString const& tag_name)
{
    while (m_elements.last().local_name() != tag_name)
        (void)pop();
    (void)pop();
}

DOM::Element* StackOfOpenElements::topmost_special_node_below(const DOM::Element& formatting_element)
{
    DOM::Element* found_element = nullptr;
    for (auto& element : m_elements.in_reverse()) {
        if (&element == &formatting_element)
            break;
        if (HTMLParser::is_special_tag(element.local_name(), element.namespace_()))
            found_element = &element;
    }
    return found_element;
}

StackOfOpenElements::LastElementResult StackOfOpenElements::last_element_with_tag_name(FlyString const& tag_name)
{
    for (ssize_t i = m_elements.size() - 1; i >= 0; --i) {
        auto& element = m_elements[i];
        if (element.local_name() == tag_name)
            return { &element, i };
    }
    return { nullptr, -1 };
}

DOM::Element* StackOfOpenElements::element_immediately_above(DOM::Element const& target)
{
    bool found_target = false;
    for (auto& element : m_elements.in_reverse()) {
        if (&element == &target) {
            found_target = true;
        } else if (found_target)
            return &element;
    }
    return nullptr;
}

void StackOfOpenElements::remove(const DOM::Element& element)
{
    m_elements.remove_first_matching([&element](DOM::Element const& other) {
        return &other == &element;
    });
}

void StackOfOpenElements::replace(const DOM::Element& to_remove, NonnullRefPtr<DOM::Element> to_add)
{
    for (size_t i = 0; i < m_elements.size(); i++) {
        if (&m_elements[i] == &to_remove) {
            m_elements.remove(i);
            m_elements.insert(i, move(to_add));
            break;
        }
    }
}

void StackOfOpenElements::insert_immediately_below(NonnullRefPtr<DOM::Element> element_to_add, DOM::Element const& target)
{
    for (size_t i = 0; i < m_elements.size(); i++) {
        if (&m_elements[i] == &target) {
            m_elements.insert(i + 1, move(element_to_add));
            break;
        }
    }
}

}
