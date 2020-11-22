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

#include <LibWeb/Forward.h>
#include <LibWeb/Layout/FormattingContext.h>

namespace Web::Layout {

class BlockFormattingContext : public FormattingContext {
public:
    explicit BlockFormattingContext(Box& containing_block);
    ~BlockFormattingContext();

    virtual void run(LayoutMode) override;

    bool is_initial() const;

protected:
    void compute_width(Box&);
    void compute_height(Box&);

private:
    void compute_width_for_absolutely_positioned_block(Box&);

    void layout_initial_containing_block(LayoutMode);
    void layout_block_level_children(LayoutMode);
    void layout_inline_children(LayoutMode);
    void layout_absolutely_positioned_descendants();

    void place_block_level_replaced_element_in_normal_flow(Box&);
    void place_block_level_non_replaced_element_in_normal_flow(Box&);

    void layout_absolutely_positioned_descendant(Box&);
};

}
