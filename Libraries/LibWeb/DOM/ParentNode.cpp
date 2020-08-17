/*
 * Copyright (c) 2020, Luke Wilde <luke.wilde@live.co.uk>
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

#include <LibWeb/CSS/Parser/CSSParser.h>
#include <LibWeb/CSS/SelectorEngine.h>
#include <LibWeb/DOM/ParentNode.h>
#include <LibWeb/Dump.h>

namespace Web::DOM {

RefPtr<Element> ParentNode::query_selector(const StringView& selector_text)
{
    auto selector = parse_selector(CSS::ParsingContext(*this), selector_text);
    if (!selector.has_value())
        return {};

    dump_selector(selector.value());

    RefPtr<Element> result;
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        if (SelectorEngine::matches(selector.value(), element)) {
            result = element;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    return result;
}

NonnullRefPtrVector<Element> ParentNode::query_selector_all(const StringView& selector_text)
{
    auto selector = parse_selector(CSS::ParsingContext(*this), selector_text);
    if (!selector.has_value())
        return {};

    dump_selector(selector.value());

    NonnullRefPtrVector<Element> elements;
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        if (SelectorEngine::matches(selector.value(), element)) {
            elements.append(element);
        }
        return IterationDecision::Continue;
    });

    return elements;
}

}
