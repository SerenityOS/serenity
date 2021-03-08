/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <AK/Vector.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Layout/FormattingContext.h>

namespace Web::Layout {

class BlockFormattingContext : public FormattingContext {
public:
    explicit BlockFormattingContext(Box&, FormattingContext* parent);
    ~BlockFormattingContext();

    virtual void run(Box&, LayoutMode) override;

    bool is_initial() const;

    const Vector<Box*>& left_floating_boxes() const { return m_left_floating_boxes; }
    const Vector<Box*>& right_floating_boxes() const { return m_right_floating_boxes; }

protected:
    void compute_width(Box&);
    void compute_height(Box&);

private:
    virtual bool is_block_formatting_context() const final { return true; }

    void compute_width_for_floating_box(Box&);

    void compute_width_for_block_level_replaced_element_in_normal_flow(ReplacedBox&);

    [[nodiscard]] static float compute_auto_height_for_block_level_element(const Box&);

    void layout_initial_containing_block(LayoutMode);

    void layout_block_level_children(Box&, LayoutMode);
    void layout_inline_children(Box&, LayoutMode);

    void place_block_level_replaced_element_in_normal_flow(Box& child, Box& container);
    void place_block_level_non_replaced_element_in_normal_flow(Box& child, Box& container);

    void layout_floating_child(Box&, Box& containing_block);

    Vector<Box*> m_left_floating_boxes;
    Vector<Box*> m_right_floating_boxes;
};

}
