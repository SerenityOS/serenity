/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSMediaRule.h>

namespace Web::CSS {

CSSMediaRule::CSSMediaRule(NonnullRefPtr<MediaList>&& media, NonnullRefPtrVector<CSSRule>&& rules)
    : CSSConditionRule(move(rules))
    , m_media(move(media))
{
}

CSSMediaRule::~CSSMediaRule()
{
}

String CSSMediaRule::condition_text() const
{
    return m_media->media_text();
}

void CSSMediaRule::set_condition_text(String text)
{
    m_media->set_media_text(text);
}

}
