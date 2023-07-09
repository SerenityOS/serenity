/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/DOM/Document.h>

namespace Web::CSS::Parser {

ParsingContext::ParsingContext(JS::Realm& realm)
    : m_realm(realm)
{
}

ParsingContext::ParsingContext(DOM::Document const& document, AK::URL url)
    : m_realm(const_cast<JS::Realm&>(document.realm()))
    , m_document(&document)
    , m_url(move(url))
{
}

ParsingContext::ParsingContext(DOM::Document const& document)
    : m_realm(const_cast<JS::Realm&>(document.realm()))
    , m_document(&document)
    , m_url(document.url())
{
}

ParsingContext::ParsingContext(DOM::ParentNode& parent_node)
    : m_realm(parent_node.realm())
    , m_document(&parent_node.document())
    , m_url(parent_node.document().url())
{
}

bool ParsingContext::in_quirks_mode() const
{
    return m_document ? m_document->in_quirks_mode() : false;
}

// https://www.w3.org/TR/css-values-4/#relative-urls
AK::URL ParsingContext::complete_url(StringView relative_url) const
{
    return m_url.complete_url(relative_url);
}

HTML::Window const* ParsingContext::window() const
{
    return m_document ? &m_document->window() : nullptr;
}

}
