/*
 * Copyright (c) 2022, Torsten Engelmann <engelTorsten@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LevelsDialog.h"
#include <Applications/PixelPaint/LevelsDialogGML.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/ValueSlider.h>

namespace PixelPaint {

LevelsDialog::LevelsDialog(GUI::Window* parent_window, ImageEditor* editor)
    : GUI::Dialog(parent_window)
{
    set_title("Levels");
    set_icon(parent_window->icon());

    auto& main_widget = set_main_widget<GUI::Widget>();
    if (!main_widget.load_from_gml(levels_dialog_gml))
        VERIFY_NOT_REACHED();

    resize(305, 202);
    set_resizable(false);

    m_editor = editor;

    m_brightness_slider = main_widget.find_descendant_of_type_named<GUI::ValueSlider>("brightness_slider");
    m_contrast_slider = main_widget.find_descendant_of_type_named<GUI::ValueSlider>("contrast_slider");
    m_gamma_slider = main_widget.find_descendant_of_type_named<GUI::ValueSlider>("gamma_slider");
    auto context_label = main_widget.find_descendant_of_type_named<GUI::Label>("context_label");
    auto apply_button = main_widget.find_descendant_of_type_named<GUI::Button>("apply_button");
    auto cancel_button = main_widget.find_descendant_of_type_named<GUI::Button>("cancel_button");

    VERIFY(m_brightness_slider);
    VERIFY(m_contrast_slider);
    VERIFY(m_gamma_slider);
    VERIFY(context_label);
    VERIFY(apply_button);
    VERIFY(cancel_button);
    VERIFY(m_editor->active_layer());

    context_label->set_text(String::formatted("Working on layer: {}", m_editor->active_layer()->name()));
    m_gamma_slider->set_value(100);

    m_brightness_slider->on_change = [this](auto) {
        generate_new_image();
    };

    m_contrast_slider->on_change = [this](auto) {
        generate_new_image();
    };

    m_gamma_slider->on_change = [this](auto) {
        generate_new_image();
    };

    apply_button->on_click = [this](auto) {
        if (m_did_change) {
            m_editor->on_modified_change(true);
            m_editor->did_complete_action("Levels"sv);
        }

        cleanup_resources();
        done(ExecResult::OK);
    };

    cancel_button->on_click = [this](auto) {
        done(ExecResult::Cancel);
    };
}

void LevelsDialog::revert_possible_changes()
{
    // FIXME: Find a faster way to revert all the changes that we have done.
    if (m_did_change && m_reference_bitmap) {
        for (int x = 0; x < m_reference_bitmap->width(); x++) {
            for (int y = 0; y < m_reference_bitmap->height(); y++) {
                m_editor->active_layer()->content_bitmap().set_pixel(x, y, m_reference_bitmap->get_pixel(x, y));
            }
        }
        m_editor->layers_did_change();
    }

    cleanup_resources();
}

void LevelsDialog::generate_new_image()
{
    (void)ensure_reference_bitmap();
    if (m_reference_bitmap.is_null())
        return;

    generate_precomputed_color_correction();
    Color current_pixel_color;
    Color new_pixel_color;
    Gfx::StorageFormat storage_format = Gfx::determine_storage_format(m_editor->active_layer()->content_bitmap().format());

    for (int x = 0; x < m_reference_bitmap->width(); x++) {
        for (int y = 0; y < m_reference_bitmap->height(); y++) {
            current_pixel_color = m_reference_bitmap->get_pixel(x, y);

            new_pixel_color.set_alpha(current_pixel_color.alpha());
            new_pixel_color.set_red(m_precomputed_color_correction[current_pixel_color.red()]);
            new_pixel_color.set_green(m_precomputed_color_correction[current_pixel_color.green()]);
            new_pixel_color.set_blue(m_precomputed_color_correction[current_pixel_color.blue()]);

            switch (storage_format) {
            case Gfx::StorageFormat::BGRx8888:
            case Gfx::StorageFormat::BGRA8888:
                m_editor->active_layer()->content_bitmap().scanline(y)[x] = new_pixel_color.value();
                break;
            default:
                m_editor->active_layer()->content_bitmap().set_pixel(x, y, new_pixel_color);
            }
        }
    }

    m_editor->active_layer()->did_modify_bitmap();
    m_did_change = true;
}

ErrorOr<void> LevelsDialog::ensure_reference_bitmap()
{
    if (m_reference_bitmap.is_null())
        m_reference_bitmap = TRY(m_editor->active_layer()->content_bitmap().clone());

    return {};
}

void LevelsDialog::cleanup_resources()
{
    if (m_reference_bitmap)
        m_reference_bitmap = nullptr;
}

void LevelsDialog::generate_precomputed_color_correction()
{
    int delta_brightness = m_brightness_slider->value();
    float contrast_correction_factor = static_cast<float>(259 * (m_contrast_slider->value() + 255) / static_cast<float>(255 * (259 - m_contrast_slider->value())));
    float gamma_correction = 1 / (m_gamma_slider->value() / 100.0);

    for (int color_val = 0; color_val < 256; color_val++) {
        m_precomputed_color_correction[color_val] = color_val + delta_brightness;
        m_precomputed_color_correction[color_val] = m_precomputed_color_correction[color_val] < 0 ? 0 : m_precomputed_color_correction[color_val];
        m_precomputed_color_correction[color_val] = m_precomputed_color_correction[color_val] > 255 ? 255 : m_precomputed_color_correction[color_val];

        m_precomputed_color_correction[color_val] = 255 * AK::pow<float>((m_precomputed_color_correction[color_val] / 255.0), gamma_correction);

        m_precomputed_color_correction[color_val] = contrast_correction_factor * (m_precomputed_color_correction[color_val] - 128) + 128;
        m_precomputed_color_correction[color_val] = m_precomputed_color_correction[color_val] < 0 ? 0 : m_precomputed_color_correction[color_val];
        m_precomputed_color_correction[color_val] = m_precomputed_color_correction[color_val] > 255 ? 255 : m_precomputed_color_correction[color_val];
    }
}

}
