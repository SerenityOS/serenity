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
#include <LibWeb/HTML/Parser/ListOfActiveFormattingElements.h>

namespace Web::HTML {

ListOfActiveFormattingElements::~ListOfActiveFormattingElements()
{
}

void ListOfActiveFormattingElements::add(DOM::Element& element)
{
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

DOM::Element* ListOfActiveFormattingElements::last_element_with_tag_name_before_marker(const FlyString& tag_name)
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

}
