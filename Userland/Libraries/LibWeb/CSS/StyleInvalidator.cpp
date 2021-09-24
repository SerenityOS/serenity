/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSStyleRule.h>
#include <LibWeb/CSS/StyleInvalidator.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>

namespace Web::CSS {

StyleInvalidator::StyleInvalidator(DOM::Document& document)
    : m_document(document)
{
    if (!m_document.should_invalidate_styles_on_attribute_changes())
        return;
    auto& style_computer = m_document.style_computer();
    m_document.for_each_in_inclusive_subtree_of_type<DOM::Element>([&](auto& element) {
        m_elements_and_matching_rules_before.set(&element, style_computer.collect_matching_rules(element));
        return IterationDecision::Continue;
    });
}

StyleInvalidator::~StyleInvalidator()
{
    if (!m_document.should_invalidate_styles_on_attribute_changes())
        return;
    auto& style_computer = m_document.style_computer();
    m_document.for_each_in_inclusive_subtree_of_type<DOM::Element>([&](auto& element) {
        auto maybe_matching_rules_before = m_elements_and_matching_rules_before.get(&element);
        if (!maybe_matching_rules_before.has_value()) {
            element.set_needs_style_update(true);
            return IterationDecision::Continue;
        }
        auto& matching_rules_before = maybe_matching_rules_before.value();
        auto matching_rules_after = style_computer.collect_matching_rules(element);
        if (matching_rules_before.size() != matching_rules_after.size()) {
            element.set_needs_style_update(true);
            return IterationDecision::Continue;
        }
        style_computer.sort_matching_rules(matching_rules_before);
        style_computer.sort_matching_rules(matching_rules_after);
        for (size_t i = 0; i < matching_rules_before.size(); ++i) {
            if (matching_rules_before[i].rule != matching_rules_after[i].rule) {
                element.set_needs_style_update(true);
                break;
            }
        }
        return IterationDecision::Continue;
    });
}

}
