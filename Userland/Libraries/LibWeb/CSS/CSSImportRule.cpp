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

}
