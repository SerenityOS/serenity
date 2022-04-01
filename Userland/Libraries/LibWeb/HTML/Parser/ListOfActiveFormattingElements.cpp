/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Element.h>
#include <LibWeb/HTML/Parser/ListOfActiveFormattingElements.h>

namespace Web::HTML {

ListOfActiveFormattingElements::~ListOfActiveFormattingElements() = default;

void ListOfActiveFormattingElements::add(DOM::Element& element)
{
    // FIXME: Implement the Noah's Ark clause https://html.spec.whatwg.org/multipage/parsing.html#push-onto-the-list-of-active-formatting-elements
    m_entries.append({ element });
}

void ListOfActiveFormattingElements::add_marker()
{
    m_entries.append({ nullptr });
}

bool ListOfActiveFormattingElements::contains(const DOM::Element& element) const
{
    for (auto& entry : m_entries) {
        if (entry.element == &element)
            return true;
    }
    return false;
}

DOM::Element* ListOfActiveFormattingElements::last_element_with_tag_name_before_marker(FlyString const& tag_name)
{
    for (ssize_t i = m_entries.size() - 1; i >= 0; --i) {
        auto& entry = m_entries[i];
        if (entry.is_marker())
            return nullptr;
        if (entry.element->local_name() == tag_name)
            return entry.element;
    }
    return nullptr;
}

void ListOfActiveFormattingElements::remove(DOM::Element& element)
{
    m_entries.remove_first_matching([&](auto& entry) {
        return entry.element == &element;
    });
}

void ListOfActiveFormattingElements::clear_up_to_the_last_marker()
{
    while (!m_entries.is_empty()) {
        auto entry = m_entries.take_last();
        if (entry.is_marker())
            break;
    }
}

Optional<size_t> ListOfActiveFormattingElements::find_index(DOM::Element const& element) const
{
    for (size_t i = 0; i < m_entries.size(); i++) {
        if (m_entries[i].element == element)
            return i;
    }
    return {};
}

void ListOfActiveFormattingElements::replace(DOM::Element& to_remove, DOM::Element& to_add)
{
    for (size_t i = 0; i < m_entries.size(); i++) {
        if (m_entries[i].element == to_remove)
            m_entries[i].element = to_add;
    }
}

void ListOfActiveFormattingElements::insert_at(size_t index, DOM::Element& element)
{
    m_entries.insert(index, { element });
}

}
