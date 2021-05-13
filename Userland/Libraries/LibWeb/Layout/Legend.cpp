/*
 * Copyright (c) 2021 Tobias Christiansen <tobi@tobyase.de>
 * 
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/Legend.h>

namespace Web::Layout {

Legend::Legend(DOM::Document& document, HTML::HTMLLegendElement* element, NonnullRefPtr<CSS::StyleProperties> style)
    : BlockBox(document, element, move(style))
{
}

Legend::~Legend()
{
}

}
