/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibWeb/Layout/SVGFormattingContext.h>
#include <LibWeb/Layout/SVGSVGBox.h>

namespace Web::Layout {

SVGFormattingContext::SVGFormattingContext(Box& box, FormattingContext* parent)
    : FormattingContext(box, parent)
{
}

SVGFormattingContext::~SVGFormattingContext()
{
}

void SVGFormattingContext::run(Box&, LayoutMode)
{
}

}
