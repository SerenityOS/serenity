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

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

class StackOfOpenElements {
public:
    StackOfOpenElements() { }
    ~StackOfOpenElements();

    DOM::Element& first() { return m_elements.first(); }
    DOM::Element& last() { return m_elements.last(); }

    bool is_empty() const { return m_elements.is_empty(); }
    void push(NonnullRefPtr<DOM::Element> element) { m_elements.append(move(element)); }
    NonnullRefPtr<DOM::Element> pop() { return m_elements.take_last(); }

    const DOM::Element& current_node() const { return m_elements.last(); }
    DOM::Element& current_node() { return m_elements.last(); }

    bool has_in_scope(const FlyString& tag_name) const;
    bool has_in_button_scope(const FlyString& tag_name) const;
    bool has_in_table_scope(const FlyString& tag_name) const;
    bool has_in_list_item_scope(const FlyString& tag_name) const;
    bool has_in_select_scope(const FlyString& tag_name) const;

    bool has_in_scope(const DOM::Element&) const;

    bool contains(const DOM::Element&) const;
    bool contains(const FlyString& tag_name) const;

    const NonnullRefPtrVector<DOM::Element>& elements() const { return m_elements; }
    NonnullRefPtrVector<DOM::Element>& elements() { return m_elements; }

    void pop_until_an_element_with_tag_name_has_been_popped(const FlyString&);

    DOM::Element* topmost_special_node_below(const DOM::Element&);

    struct LastElementResult {
        DOM::Element* element;
        ssize_t index;
    };
    LastElementResult last_element_with_tag_name(const FlyString&);
    DOM::Element* element_before(const DOM::Element&);

private:
    bool has_in_scope_impl(const FlyString& tag_name, const Vector<FlyString>&) const;
    bool has_in_scope_impl(const DOM::Element& target_node, const Vector<FlyString>&) const;

    NonnullRefPtrVector<DOM::Element> m_elements;
};

}
