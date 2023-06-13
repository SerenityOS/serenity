/*
 * Copyright (c) 2023, Torsten Engelmann <engelTorsten@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ImageMasking.h"
#include <Applications/PixelPaint/LuminosityMaskingGML.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/RangeSlider.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Path.h>

namespace PixelPaint {

ImageMasking::ImageMasking(GUI::Window* parent_window, ImageEditor* editor)
    : GUI::Dialog(parent_window)
{
    set_title("Luminosity Mask");
    set_icon(parent_window->icon());

    auto main_widget = set_main_widget<GUI::Widget>().release_value_but_fixme_should_propagate_errors();
    main_widget->load_from_gml(luminosity_masking_gml).release_value_but_fixme_should_propagate_errors();

    resize(300, 170);
    set_resizable(false);

    m_editor = editor;

    m_full_masking_slider = main_widget->find_descendant_of_type_named<GUI::RangeSlider>("full_masking");
    m_edge_masking_slider = main_widget->find_descendant_of_type_named<GUI::RangeSlider>("edge_masking");
    auto range_illustration_container = main_widget->find_descendant_of_type_named<GUI::Widget>("range_illustration");
    auto mask_visibility = main_widget->find_descendant_of_type_named<GUI::CheckBox>("mask_visibility");
    auto apply_button = main_widget->find_descendant_of_type_named<GUI::Button>("apply_button");
    auto cancel_button = main_widget->find_descendant_of_type_named<GUI::Button>("cancel_button");

    VERIFY(m_full_masking_slider);
    VERIFY(m_edge_masking_slider);
    VERIFY(range_illustration_container);
    VERIFY(mask_visibility);
    VERIFY(apply_button);
    VERIFY(cancel_button);
    VERIFY(m_editor->active_layer());

    m_full_masking_slider->set_gradient_color(Color(0, 0, 0, 255), Color(255, 255, 255, 255));
    m_edge_masking_slider->set_gradient_color(Color(0, 0, 0, 255), Color(255, 255, 255, 255));

    auto illustration_widget = range_illustration_container->try_add<RangeIllustrationWidget>(m_edge_masking_slider, m_full_masking_slider).release_value();
    illustration_widget->set_width(range_illustration_container->width());
    illustration_widget->set_height(range_illustration_container->height());

    // check that edges of full and edge masking are not intersecting, and refine the mask with the updated values
    m_full_masking_slider->on_range_change = [this, illustration_widget](int lower, int upper) {
        if (lower < m_edge_masking_slider->lower_range())
            m_full_masking_slider->set_lower_range(AK::max(lower, m_edge_masking_slider->lower_range()));
        if (upper > m_edge_masking_slider->upper_range())
            m_full_masking_slider->set_upper_range(AK::min(upper, m_edge_masking_slider->upper_range()));

        illustration_widget->update();
        generate_new_mask();
    };
    m_edge_masking_slider->on_range_change = [this, illustration_widget](int lower, int upper) {
        if (lower > m_full_masking_slider->lower_range())
            m_edge_masking_slider->set_lower_range(AK::min(lower, m_full_masking_slider->lower_range()));
        if (upper < m_full_masking_slider->upper_range())
            m_edge_masking_slider->set_upper_range(AK::max(upper, m_full_masking_slider->upper_range()));

        illustration_widget->update();
        generate_new_mask();
    };

    mask_visibility->set_checked(m_editor->active_layer()->mask_visibility());
    mask_visibility->on_checked = [this](auto checked) {
        m_editor->active_layer()->set_mask_visibility(checked);
        m_editor->update();
    };

    apply_button->on_click = [this](auto) {
        if (m_did_change)
            m_editor->did_complete_action("Luminosity Masking"sv);

        cleanup_resources();
        done(ExecResult::OK);
    };

    cancel_button->on_click = [this](auto) {
        done(ExecResult::Cancel);
    };

    generate_new_mask();
}

void ImageMasking::revert_possible_changes()
{
    if (m_did_change && m_reference_mask) {
        MUST(m_editor->active_layer()->set_bitmaps(m_editor->active_layer()->content_bitmap(), m_reference_mask.release_nonnull()));
        m_editor->layers_did_change();
    }
    cleanup_resources();
}

void ImageMasking::generate_new_mask()
{
    ensure_reference_mask().release_value_but_fixme_should_propagate_errors();

    if (m_reference_mask.is_null())
        return;

    int min_luminosity_start = m_edge_masking_slider->lower_range();
    int min_luminosity_full = m_full_masking_slider->lower_range();
    int max_luminosity_full = m_full_masking_slider->upper_range();
    int max_luminosity_end = m_edge_masking_slider->upper_range();
    int current_content_luminosity, approximation_alpha;
    bool has_start_range = min_luminosity_start != min_luminosity_full;
    bool has_end_range = max_luminosity_end != max_luminosity_full;
    Gfx::Color reference_mask_pixel, content_pixel;

    for (int y = 0; y < m_reference_mask->height(); y++) {
        for (int x = 0; x < m_reference_mask->width(); x++) {
            reference_mask_pixel = m_reference_mask->get_pixel(x, y);
            if (!reference_mask_pixel.alpha())
                continue;

            content_pixel = m_editor->active_layer()->content_bitmap().get_pixel(x, y);
            current_content_luminosity = content_pixel.luminosity();

            if (!content_pixel.alpha() || current_content_luminosity < min_luminosity_start || current_content_luminosity > max_luminosity_end) {
                reference_mask_pixel.set_alpha(0);
            } else if (current_content_luminosity >= min_luminosity_start && current_content_luminosity < min_luminosity_full && has_start_range) {
                approximation_alpha = reference_mask_pixel.alpha() * static_cast<float>((current_content_luminosity - min_luminosity_start)) / (min_luminosity_full - min_luminosity_start);
                reference_mask_pixel.set_alpha(approximation_alpha);
            } else if (current_content_luminosity > max_luminosity_full && current_content_luminosity <= max_luminosity_end && has_end_range) {
                approximation_alpha = reference_mask_pixel.alpha() * (1 - static_cast<float>((current_content_luminosity - max_luminosity_full)) / (max_luminosity_end - max_luminosity_full));
                reference_mask_pixel.set_alpha(approximation_alpha);
            }

            m_editor->active_layer()->mask_bitmap()->set_pixel(x, y, reference_mask_pixel);
        }
    }

    m_editor->active_layer()->did_modify_bitmap();
    m_did_change = true;
}

ErrorOr<void> ImageMasking::ensure_reference_mask()
{
    if (m_reference_mask.is_null())
        m_reference_mask = TRY(m_editor->active_layer()->mask_bitmap()->clone());

    return {};
}

void ImageMasking::cleanup_resources()
{
    if (m_reference_mask)
        m_reference_mask = nullptr;
}

void RangeIllustrationWidget::paint_event(GUI::PaintEvent&)
{
    GUI::Painter painter(*this);
    painter.fill_rect(Gfx::IntRect(0, 0, width(), height()), palette().color(background_role()));
    float fraction = width() / 255.0f;

    Gfx::Path illustration;
    illustration.move_to({ fraction * m_edge_mask_values->lower_range(), static_cast<float>(height()) });
    illustration.line_to({ fraction * m_full_mask_values->lower_range(), 0 });
    illustration.line_to({ fraction * m_full_mask_values->upper_range(), 0 });
    illustration.line_to({ fraction * m_edge_mask_values->upper_range(), static_cast<float>(height()) });
    illustration.close();

    painter.fill_path(illustration, Color::MidGray);
}
}
