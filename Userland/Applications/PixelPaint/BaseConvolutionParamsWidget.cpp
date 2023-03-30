/*
 * Copyright (c) 2023, Luiz Gustavo de França Chaves
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BaseConvolutionParamsWidget.h"
#include "LibGUI/Label.h"
#include <AK/Format.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/CheckBox.h>

REGISTER_WIDGET(PixelPaint, BaseConvolutionParamsWidget);

namespace PixelPaint {

void BaseConvolutionParamsWidget::set_name_label(StringView name)
{
    if (!name.is_empty()) {
        m_name_label->set_text(name.to_deprecated_string());
        m_name_label->set_visible(true);
    } else {
        m_name_label->set_visible(false);
    }
}

BaseConvolutionParamsWidget::BaseConvolutionParamsWidget()
{
    set_layout<GUI::VerticalBoxLayout>(0, 0);
    set_frame_thickness(0);

    m_name_label = &add<GUI::Label>();
    m_name_label->set_visible(false);
    m_name_label->set_font_weight(Gfx::FontWeight::Bold);
    m_name_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
    m_name_label->set_fixed_height(20);

    m_should_wrap_checkbox = &add<GUI::CheckBox>(String::from_utf8("Wrap Around"sv).release_value_but_fixme_should_propagate_errors());
    m_should_wrap_checkbox->set_checked(m_should_wrap);

    m_should_wrap_checkbox->on_checked = [&](bool checked) {
        if (on_wrap_around_checked) {
            on_wrap_around_checked(checked);
        }
    };
}

void BaseConvolutionParamsWidget::set_should_wrap(bool should_wrap)
{
    m_should_wrap_checkbox->set_checked(should_wrap);
}

}
