/*
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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

#include <LibWeb/CSS/StyleInvalidator.h>
#include <LibWeb/CSS/StyleRule.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>

namespace Web::CSS {

StyleInvalidator::StyleInvalidator(DOM::Document& document)
    : m_document(document)
{
    if (!m_document.should_invalidate_styles_on_attribute_changes())
        return;
    auto& style_resolver = m_document.style_resolver();
    m_document.for_each_in_subtree_of_type<DOM::Element>([&](auto& element) {
        m_elements_and_matching_rules_before.set(&element, style_resolver.collect_matching_rules(element));
        return IterationDecision::Continue;
    });
}

StyleInvalidator::~StyleInvalidator()
{
    if (!m_document.should_invalidate_styles_on_attribute_changes())
        return;
    auto& style_resolver = m_document.style_resolver();
    m_document.for_each_in_subtree_of_type<DOM::Element>([&](auto& element) {
        auto maybe_matching_rules_before = m_elements_and_matching_rules_before.get(&element);
        if (!maybe_matching_rules_before.has_value()) {
            element.set_needs_style_update(true);
            return IterationDecision::Continue;
        }
        auto& matching_rules_before = maybe_matching_rules_before.value();
        auto matching_rules_after = style_resolver.collect_matching_rules(element);
        if (matching_rules_before.size() != matching_rules_after.size()) {
            element.set_needs_style_update(true);
            return IterationDecision::Continue;
        }
        style_resolver.sort_matching_rules(matching_rules_before);
        style_resolver.sort_matching_rules(matching_rules_after);
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
