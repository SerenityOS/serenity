/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/NonnullRefPtrVector.h>
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
