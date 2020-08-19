/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibWeb/DOM/Element.h>
#include <LibWeb/HTML/Parser/HTMLDocumentParser.h>
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
    ASSERT_NOT_REACHED();
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
    ASSERT_NOT_REACHED();
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
        pop();
    pop();
}

DOM::Element* StackOfOpenElements::topmost_special_node_below(const DOM::Element& formatting_element)
{
    DOM::Element* found_element = nullptr;
    for (ssize_t i = m_elements.size() - 1; i >= 0; --i) {
        auto& element = m_elements[i];
        if (&element == &formatting_element)
            break;
        if (HTMLDocumentParser::is_special_tag(element.local_name()))
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

DOM::Element* StackOfOpenElements::element_before(const DOM::Element& target)
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
