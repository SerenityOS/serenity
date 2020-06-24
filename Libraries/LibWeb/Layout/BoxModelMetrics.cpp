/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <LibWeb/Layout/BoxModelMetrics.h>

namespace Web {

PixelBox BoxModelMetrics::margin_box(const LayoutNode& layout_node) const
{
    return {
        margin.top.to_px(layout_node) + border.top.to_px(layout_node) + padding.top.to_px(layout_node),
        margin.right.to_px(layout_node) + border.right.to_px(layout_node) + padding.right.to_px(layout_node),
        margin.bottom.to_px(layout_node) + border.bottom.to_px(layout_node) + padding.bottom.to_px(layout_node),
        margin.left.to_px(layout_node) + border.left.to_px(layout_node) + padding.left.to_px(layout_node),
    };
}

PixelBox BoxModelMetrics::padding_box(const LayoutNode& layout_node) const
{
    return {
        padding.top.to_px(layout_node),
        padding.right.to_px(layout_node),
        padding.bottom.to_px(layout_node),
        padding.left.to_px(layout_node),
    };
}

PixelBox BoxModelMetrics::border_box(const LayoutNode& layout_node) const
{
    return {
        border.top.to_px(layout_node) + padding.top.to_px(layout_node),
        border.right.to_px(layout_node) + padding.right.to_px(layout_node),
        border.bottom.to_px(layout_node) + padding.bottom.to_px(layout_node),
        border.left.to_px(layout_node) + padding.left.to_px(layout_node),
    };
}

}
