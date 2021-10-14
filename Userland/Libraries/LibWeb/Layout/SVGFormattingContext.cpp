/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibWeb/Layout/SVGFormattingContext.h>
#include <LibWeb/Layout/SVGPathBox.h>
#include <LibWeb/Layout/SVGSVGBox.h>

namespace Web::Layout {

SVGFormattingContext::SVGFormattingContext(Box& box, FormattingContext* parent)
    : FormattingContext(Type::SVG, box, parent)
{
}

SVGFormattingContext::~SVGFormattingContext()
{
}

void SVGFormattingContext::run(Box& box, LayoutMode)
{
    // FIXME: This formatting context is basically a total hack.
    //        It works by computing the united bounding box of all <path>'s
    //        within an <svg>, and using that as the size of this box.

    Gfx::FloatRect total_bounding_box;

    box.for_each_in_subtree_of_type<SVGBox>([&](auto& descendant) {
        if (is<SVGPathBox>(descendant)) {
            auto& path_box = static_cast<SVGPathBox&>(descendant);
            auto& path = path_box.dom_node().get_path();
            path_box.set_size(path.bounding_box().size());

            total_bounding_box = total_bounding_box.united(path.bounding_box());
        }

        return IterationDecision::Continue;
    });

    box.set_size(total_bounding_box.size());
}

}
