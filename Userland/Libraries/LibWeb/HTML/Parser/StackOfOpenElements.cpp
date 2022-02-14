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

StackOfOpenElements::~StackOfOpenElements()
{
}

bool StackOfOpenElements::has_in_scope_impl(const FlyString& tag_name, const Vector<FlyString>& list) const
{
    for (ssize_t i = m_elements.size() - 1; i >= 0; --i) {
        auto& node = m_elements.at(i);
        if (node.local_name() == tag_name)
            return true;
        if (list.contains_slow(node.local_name()))
            return false;
    }
    VERIFY_NOT_REACHED();
}

bool StackOfOpenElements::has_in_scope(const FlyString& tag_name) const
{
    return has_in_scope_impl(tag_name, s_base_list);
}

bool StackOfOpenElements::has_in_scope_impl(const DOM::Element& target_node, const Vector<FlyString>& list) const
{
    for (ssize_t i = m_elements.size() - 1; i >= 0; --i) {
        auto& node = m_elements.at(i);
        if (&node == &target_node)
            return true;
        if (list.contains_slow(node.local_name()))
            return false;
    }
    VERIFY_NOT_REACHED();
}

bool StackOfOpenElements::has_in_scope(const DOM::Element& target_node) const
{
    return has_in_scope_impl(target_node, s_base_list);
}

bool StackOfOpenElements::has_in_button_scope(const FlyString& tag_name) const
{
    auto list = s_base_list;
    list.append("button");
    return has_in_scope_impl(tag_name, list);
}

bool StackOfOpenElements::has_in_table_scope(const FlyString& tag_name) const
{
    return has_in_scope_impl(tag_name, { "html", "table", "template" });
}

bool StackOfOpenElements::has_in_list_item_scope(const FlyString& tag_name) const
{
    auto list = s_base_list;
    list.append("ol");
    list.append("ul");
    return has_in_scope_impl(tag_name, list);
}

bool StackOfOpenElements::has_in_select_scope(const FlyString& tag_name) const
{
    return has_in_scope_impl(tag_name, { "option", "optgroup" });
}

bool StackOfOpenElements::contains(const DOM::Element& element) const
{
    for (auto& element_on_stack : m_elements) {
        if (&element == &element_on_stack)
            return true;
    }
    return false;
}

bool StackOfOpenElements::contains(const FlyString& tag_name) const
{
    for (auto& element_on_stack : m_elements) {
        if (element_on_stack.local_name() == tag_name)
            return true;
    }
    return false;
}

void StackOfOpenElements::pop_until_an_element_with_tag_name_has_been_popped(const FlyString& tag_name)
{
    while (m_elements.last().local_name() != tag_name)
        (void)pop();
    (void)pop();
}

DOM::Element* StackOfOpenElements::topmost_special_node_below(const DOM::Element& formatting_element)
{
    DOM::Element* found_element = nullptr;
    for (ssize_t i = m_elements.size() - 1; i >= 0; --i) {
        auto& element = m_elements[i];
        if (&element == &formatting_element)
            break;
        if (HTMLParser::is_special_tag(element.local_name(), element.namespace_()))
            found_element = &element;
    }
    return found_element;
}

StackOfOpenElements::LastElementResult StackOfOpenElements::last_element_with_tag_name(const FlyString& tag_name)
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
    for (ssize_t i = m_elements.size() - 1; i >= 0; --i) {
        auto& element = m_elements[i];
        if (&element == &target) {
            found_target = true;
        } else if (found_target)
            return &element;
    }
    return nullptr;
}

}
