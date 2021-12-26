/*
 * Copyright (c) 2020, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/DeprecatedCSSParser.h>
#include <LibWeb/CSS/SelectorEngine.h>
#include <LibWeb/DOM/ParentNode.h>
#include <LibWeb/Dump.h>

namespace Web::DOM {

RefPtr<Element> ParentNode::query_selector(const StringView& selector_text)
{
    auto selector = parse_selector(CSS::DeprecatedParsingContext(*this), selector_text);
    if (!selector)
        return {};

    dump_selector(*selector);

    RefPtr<Element> result;
    for_each_in_inclusive_subtree_of_type<Element>([&](auto& element) {
        if (SelectorEngine::matches(*selector, element)) {
            result = element;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    return result;
}

NonnullRefPtrVector<Element> ParentNode::query_selector_all(const StringView& selector_text)
{
    auto selector = parse_selector(CSS::DeprecatedParsingContext(*this), selector_text);
    if (!selector)
        return {};

    dump_selector(*selector);

    NonnullRefPtrVector<Element> elements;
    for_each_in_inclusive_subtree_of_type<Element>([&](auto& element) {
        if (SelectorEngine::matches(*selector, element)) {
            elements.append(element);
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

}
