/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ModeBilevelDialog.h"
#include <Applications/PixelPaint/ModeBilevelDialogGML.h>
#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>

namespace PixelPaint {

ModeBilevelDialog::ModeBilevelDialog(GUI::Window* parent_window)
    : Dialog(parent_window)
{
    set_title("Convert to Bilevel");
    set_icon(parent_window->icon());

    auto main_widget = set_main_widget<GUI::Widget>();
    main_widget->load_from_gml(mode_bilevel_dialog_gml).release_value_but_fixme_should_propagate_errors();

    auto method_combobox = main_widget->find_descendant_of_type_named<GUI::ComboBox>("method_combobox");
    VERIFY(method_combobox);

    enum class DitheringMethodIndex {
        None = 0,
        FloydSteinberg,
    };

    static constexpr AK::Array dithering_strings = {
        "Global Threshold"sv,
        "Floyd-Steinberg"sv
    };

    auto selected_dithering_method = [&] {
        switch (m_dithering_algorithm) {
        case Gfx::DitheringAlgorithm::None:
            return DitheringMethodIndex::None;
        case Gfx::DitheringAlgorithm::FloydSteinberg:
            return DitheringMethodIndex::FloydSteinberg;
        }
        VERIFY_NOT_REACHED();
    }();

    method_combobox->set_only_allow_values_from_model(true);
    method_combobox->set_model(*GUI::ItemListModel<StringView, decltype(dithering_strings)>::create(dithering_strings));

    m_dithering_algorithm = static_cast<Gfx::DitheringAlgorithm>(method_combobox->selected_index());
    method_combobox->on_change = [this](auto const&, auto& index) {
        auto dithering_method_index = static_cast<DitheringMethodIndex>(index.row());
        m_dithering_algorithm = [&]() -> Gfx::DitheringAlgorithm {
            switch (dithering_method_index) {
            case DitheringMethodIndex::None:
                return Gfx::DitheringAlgorithm::None;
            case DitheringMethodIndex::FloydSteinberg:
                return Gfx::DitheringAlgorithm::FloydSteinberg;
            }
            VERIFY_NOT_REACHED();
        }();
        m_dithering_algorithm = static_cast<Gfx::DitheringAlgorithm>(index.row());
    };
    method_combobox->set_selected_index(to_underlying(selected_dithering_method));

    auto ok_button = main_widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    auto cancel_button = main_widget->find_descendant_of_type_named<GUI::Button>("cancel_button");

    VERIFY(ok_button);
    VERIFY(cancel_button);

    ok_button->on_click = [this](auto) {
        done(ExecResult::OK);
    };
    ok_button->set_default(true);

    cancel_button->on_click = [this](auto) {
        done(ExecResult::Cancel);
    };
}

}
