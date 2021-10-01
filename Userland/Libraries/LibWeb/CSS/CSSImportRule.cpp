/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/URL.h>
#include <LibWeb/CSS/CSSImportRule.h>
#include <LibWeb/CSS/CSSStyleSheet.h>

namespace Web::CSS {

CSSImportRule::CSSImportRule(AK::URL url)
    : m_url(move(url))
{
}

CSSImportRule::~CSSImportRule()
{
}

// https://drafts.csswg.org/cssom/#serialize-a-css-rule
String CSSImportRule::serialized() const
{
    StringBuilder builder;
    // The result of concatenating the following:

    // 1. The string "@import" followed by a single SPACE (U+0020).
    builder.append("@import "sv);

    // 2. The result of performing serialize a URL on the rule’s location.
    // FIXME: Look into the correctness of this serialization
    builder.append("url("sv);
    builder.append(m_url.to_string());
    builder.append(')');

    // FIXME: 3. If the rule’s associated media list is not empty, a single SPACE (U+0020) followed by the result of performing serialize a media query list on the media list.

    // 4. The string ";", i.e., SEMICOLON (U+003B).
    builder.append(';');

    return builder.to_string();
}

}
