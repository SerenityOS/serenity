/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/parsing.html#stack-of-open-elements
class StackOfOpenElements {
public:
    // Initially, the stack of open elements is empty.
    // The stack grows downwards; the topmost node on the stack is the first one added to the stack,
    // and the bottommost node of the stack is the most recently added node in the stack
    // (notwithstanding when the stack is manipulated in a random access fashion as part of the handling for misnested tags).

    StackOfOpenElements() = default;
    ~StackOfOpenElements();

    DOM::Element& first() { return m_elements.first(); }
    DOM::Element& last() { return m_elements.last(); }

    bool is_empty() const { return m_elements.is_empty(); }
    void push(NonnullRefPtr<DOM::Element> element) { m_elements.append(move(element)); }
    NonnullRefPtr<DOM::Element> pop() { return m_elements.take_last(); }
    void remove(DOM::Element const& element);
    void replace(DOM::Element const& to_remove, NonnullRefPtr<DOM::Element> to_add);
    void insert_immediately_below(NonnullRefPtr<DOM::Element> element_to_add, DOM::Element const& target);

    const DOM::Element& current_node() const { return m_elements.last(); }
    DOM::Element& current_node() { return m_elements.last(); }

    bool has_in_scope(FlyString const& tag_name) const;
    bool has_in_button_scope(FlyString const& tag_name) const;
    bool has_in_table_scope(FlyString const& tag_name) const;
    bool has_in_list_item_scope(FlyString const& tag_name) const;
    bool has_in_select_scope(FlyString const& tag_name) const;

    bool has_in_scope(const DOM::Element&) const;

    bool contains(const DOM::Element&) const;
    bool contains(FlyString const& tag_name) const;

    NonnullRefPtrVector<DOM::Element> const& elements() const { return m_elements; }
    NonnullRefPtrVector<DOM::Element>& elements() { return m_elements; }

    void pop_until_an_element_with_tag_name_has_been_popped(FlyString const&);

    DOM::Element* topmost_special_node_below(const DOM::Element&);

    struct LastElementResult {
        DOM::Element* element;
        ssize_t index;
    };
    LastElementResult last_element_with_tag_name(FlyString const&);
    DOM::Element* element_immediately_above(DOM::Element const&);

private:
    bool has_in_scope_impl(FlyString const& tag_name, Vector<FlyString> const&) const;
    bool has_in_scope_impl(const DOM::Element& target_node, Vector<FlyString> const&) const;

    NonnullRefPtrVector<DOM::Element> m_elements;
};

}
