/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/Selector.h>
#include <LibWeb/DOM/Element.h>

namespace Web::SelectorEngine {

bool matches(CSS::Selector const&, Optional<CSS::CSSStyleSheet const&> style_sheet_for_rule, DOM::Element const&, Optional<CSS::Selector::PseudoElement> = {}, JS::GCPtr<DOM::ParentNode const> scope = {});

}
