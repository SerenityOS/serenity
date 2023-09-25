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

ParsingContext::ParsingContext(JS::Realm& realm, Mode mode)
    : m_realm(realm)
    , m_mode(mode)
{
}

ParsingContext::ParsingContext(DOM::Document const& document, AK::URL url, Mode mode)
    : m_realm(const_cast<JS::Realm&>(document.realm()))
    , m_document(&document)
    , m_url(move(url))
    , m_mode(mode)
{
}

ParsingContext::ParsingContext(DOM::Document const& document, Mode mode)
    : m_realm(const_cast<JS::Realm&>(document.realm()))
    , m_document(&document)
    , m_url(document.url())
    , m_mode(mode)
{
}

ParsingContext::ParsingContext(DOM::ParentNode& parent_node, Mode mode)
    : m_realm(parent_node.realm())
    , m_document(&parent_node.document())
    , m_url(parent_node.document().url())
    , m_mode(mode)
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
