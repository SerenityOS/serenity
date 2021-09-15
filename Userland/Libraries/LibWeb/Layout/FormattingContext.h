/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Forward.h>

namespace Web::Layout {

class FormattingContext {
public:
    virtual void run(Box&, LayoutMode) = 0;

    Box& context_box() { return *m_context_box; }
    const Box& context_box() const { return *m_context_box; }

    FormattingContext* parent() { return m_parent; }
    const FormattingContext* parent() const { return m_parent; }

    virtual bool is_block_formatting_context() const { return false; }
    virtual bool inhibits_floating() const { return false; }

    static bool creates_block_formatting_context(const Box&);

    static float compute_width_for_replaced_element(const ReplacedBox&);
    static float compute_height_for_replaced_element(const ReplacedBox&);

protected:
    FormattingContext(Box&, FormattingContext* parent = nullptr);
    virtual ~FormattingContext();

    void layout_inside(Box&, LayoutMode);

    struct ShrinkToFitResult {
        float preferred_width { 0 };
        float preferred_minimum_width { 0 };
    };

    static float tentative_width_for_replaced_element(const ReplacedBox&, const CSS::Length& width);
    static float tentative_height_for_replaced_element(const ReplacedBox&, const CSS::Length& width);

    ShrinkToFitResult calculate_shrink_to_fit_widths(Box&);

    void layout_absolutely_positioned_element(Box&);
    void compute_width_for_absolutely_positioned_element(Box&);
    void compute_width_for_absolutely_positioned_non_replaced_element(Box&);
    void compute_width_for_absolutely_positioned_replaced_element(ReplacedBox&);
    void compute_height_for_absolutely_positioned_element(Box&);
    void compute_height_for_absolutely_positioned_non_replaced_element(Box&);
    void compute_height_for_absolutely_positioned_replaced_element(ReplacedBox&);

    FormattingContext* m_parent { nullptr };
    Box* m_context_box { nullptr };
};

}
