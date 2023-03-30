/*
 * Copyright (c) 2023, Luiz Gustavo de França Chaves
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConvolutionFilter.h"
#include <Applications/PixelPaint/BaseConvolutionParamsWidget.h>
#include <Applications/PixelPaint/FilterParams.h>

namespace PixelPaint::Filters {
ErrorOr<RefPtr<GUI::Widget>> ConvolutionFilter::get_settings_widget()
{
    if (!m_settings_widget) {
        auto base_params_widget = BaseConvolutionParamsWidget::construct();
        base_params_widget->on_wrap_around_checked = [&](bool checked) {
            m_filter_options.should_wrap = checked;
            update_preview();
        };
        base_params_widget->set_name_label(filter_name());
        base_params_widget->set_should_wrap(m_filter_options.should_wrap);

        m_settings_widget = GUI::Widget::construct();
        m_settings_widget->set_layout<GUI::VerticalBoxLayout>();
        m_settings_widget->add_child(base_params_widget);

        update_preview();
    }

    return m_settings_widget;
}
}
