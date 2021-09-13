/*
 * Copyright (c) 2020, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/SelectorEngine.h>
#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/DOM/ParentNode.h>
#include <LibWeb/Dump.h>

namespace Web::DOM {

ExceptionOr<RefPtr<Element>> ParentNode::query_selector(StringView selector_text)
{
    auto maybe_selectors = parse_selector(CSS::ParsingContext(*this), selector_text);
    if (!maybe_selectors.has_value())
        return DOM::SyntaxError::create("Failed to parse selector");

    auto selectors = maybe_selectors.value();

    RefPtr<Element> result;
    for_each_in_inclusive_subtree_of_type<Element>([&](auto& element) {
        for (auto& selector : selectors) {
            if (SelectorEngine::matches(selector, element)) {
                result = element;
                return IterationDecision::Break;
            }
        }
        return IterationDecision::Continue;
    });

    return result;
}

ExceptionOr<NonnullRefPtrVector<Element>> ParentNode::query_selector_all(StringView selector_text)
{
    auto maybe_selectors = parse_selector(CSS::ParsingContext(*this), selector_text);
    if (!maybe_selectors.has_value())
        return DOM::SyntaxError::create("Failed to parse selector");

    auto selectors = maybe_selectors.value();

    NonnullRefPtrVector<Element> elements;
    for_each_in_inclusive_subtree_of_type<Element>([&](auto& element) {
        for (auto& selector : selectors) {
            if (SelectorEngine::matches(selector, element)) {
                elements.append(element);
            }
        }
        return IterationDecision::Continue;
    });

    return elements;
}

RefPtr<Element> ParentNode::first_element_child()
{
    return first_child_of_type<Element>();
}

RefPtr<Element> ParentNode::last_element_child()
{
    return last_child_of_type<Element>();
}

// https://dom.spec.whatwg.org/#dom-parentnode-childelementcount
u32 ParentNode::child_element_count() const
{
    u32 count = 0;
    for (auto* child = first_child(); child; child = child->next_sibling()) {
        if (is<Element>(child))
            ++count;
    }
    return count;
}

// https://dom.spec.whatwg.org/#dom-parentnode-children
NonnullRefPtr<HTMLCollection> ParentNode::children()
{
    // The children getter steps are to return an HTMLCollection collection rooted at this matching only element children.
    // FIXME: This should return the same HTMLCollection object every time,
    //        but that would cause a reference cycle since HTMLCollection refs the root.
    return HTMLCollection::create(*this, [this](Element const& element) {
        return is_parent_of(element);
    });
}

}
