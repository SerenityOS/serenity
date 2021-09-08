/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FilterTool.h"
#include "../FilterParams.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <AK/String.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Filters/BoxBlurFilter.h>

namespace PixelPaint {

GUI::Widget* FilterTool::get_properties_widget()
{
    if (!m_properties_widget) {
        m_properties_widget = GUI::Widget::construct();
        m_properties_widget->set_layout<GUI::VerticalBoxLayout>();

        for (int i = 0; i < (int)FilterTool::FilterType::__Count; i++) {
            switch ((FilterTool::FilterType)i) {
            case FilterType::Unselected:
                m_filter_names.append("Select Filter...");
                m_filters.append(nullptr);
                m_filter_parameters.append(nullptr);
                break;
            case FilterType::LaplacianCardinal:
                m_filter_names.append("Laplacian (Cardinal)");
                m_filters.append(make<Gfx::LaplacianFilter>());
                m_filter_parameters.append(PixelPaint::FilterParameters<Gfx::LaplacianFilter>::get(false));
                break;
            case FilterType::LaplacianDiagonal:
                m_filter_names.append("Laplacian (Diagonal)");
                m_filters.append(make<Gfx::LaplacianFilter>());
                m_filter_parameters.append(PixelPaint::FilterParameters<Gfx::LaplacianFilter>::get(true));
                break;
            case FilterType::Gauss3:
                m_filter_names.append("Gaussian (3x3)");
                m_filters.append(make<Gfx::SpatialGaussianBlurFilter<3>>());
                m_filter_parameters.append(PixelPaint::FilterParameters<Gfx::SpatialGaussianBlurFilter<3>>::get());
                break;
            case FilterType::Gauss5:
                m_filter_names.append("Gaussian (5x5)");
                m_filters.append(make<Gfx::SpatialGaussianBlurFilter<5>>());
                m_filter_parameters.append(PixelPaint::FilterParameters<Gfx::SpatialGaussianBlurFilter<5>>::get());
                break;
            case FilterType::BoxBlur3:
                m_filter_names.append("Box Blur (3)");
                m_filters.append(make<Gfx::BoxBlurFilter<3>>());
                m_filter_parameters.append(PixelPaint::FilterParameters<Gfx::BoxBlurFilter<3>>::get());
                break;
            case FilterType::BoxBlur5:
                m_filter_names.append("Box Blur (5)");
                m_filters.append(make<Gfx::BoxBlurFilter<5>>());
                m_filter_parameters.append(PixelPaint::FilterParameters<Gfx::BoxBlurFilter<5>>::get());
                break;
            case FilterType::Sharpen:
                m_filter_names.append("Sharpen");
                m_filters.append(make<Gfx::SharpenFilter>());
                m_filter_parameters.append(PixelPaint::FilterParameters<Gfx::SharpenFilter>::get());
                break;
            case FilterType::Grayscale:
                m_filter_names.append("Grayscale");
                m_filters.append(make<Gfx::GrayscaleFilter>());
                m_filter_parameters.append(nullptr);
                break;
            case FilterType::Invert:
                m_filter_names.append("Invert");
                m_filters.append(make<Gfx::InvertFilter>());
                m_filter_parameters.append(nullptr);
                break;
            default:
                VERIFY_NOT_REACHED();
            }
        }

        auto& filter = m_properties_widget->add<GUI::ComboBox>();
        filter.set_only_allow_values_from_model(true);
        filter.set_model(*GUI::ItemListModel<String>::create(m_filter_names));
        filter.set_selected_index((int)m_selected_filter);
        filter.on_change = [this](auto&&, GUI::ModelIndex const& index) {
            VERIFY(index.row() >= 0);
            VERIFY(index.row() < (int)FilterTool::FilterType::__Count);

            m_selected_filter = (FilterTool::FilterType)index.row();
        };

        auto& apply_button = m_properties_widget->add<GUI::Button>("Apply filter");
        apply_button.on_click = [&](auto) {
            if ((FilterTool::FilterType)m_selected_filter == FilterType::Unselected)
                return;
            auto layer = editor()->active_layer();
            auto& filter = m_filters[(size_t)m_selected_filter];
            auto& parameters = m_filter_parameters[(size_t)m_selected_filter];

            if (!filter)
                return;

            if (parameters)
                filter->apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect(), *parameters);
            else
                filter->apply(layer->bitmap(), layer->rect(), layer->bitmap(), layer->rect());

            editor()->layers_did_change();
        };
    }
    return m_properties_widget.ptr();
}
}
