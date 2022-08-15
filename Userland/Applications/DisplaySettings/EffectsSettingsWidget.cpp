/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EffectsSettingsWidget.h"
#include <Applications/DisplaySettings/EffectsSettingsGML.h>
#include <LibCore/ConfigFile.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/ItemListModel.h>

namespace GUI {

namespace DisplaySettings {

EffectsSettingsWidget::EffectsSettingsWidget()
{
    load_from_gml(effects_settings_gml);

    m_geometry_combobox = find_descendant_of_type_named<ComboBox>("geometry_combobox");
    m_geometry_combobox->set_only_allow_values_from_model(true);
    m_geometry_combobox->on_change = [this](auto&, auto&) {
        m_system_effects.set_geometry(static_cast<ShowGeometry>(m_geometry_combobox->selected_index()));
        set_modified(true);
    };

    if (auto result = load_settings(); result.is_error()) {
        warnln("Failed to load [Effects] from WindowServer.ini");
        return;
    }

    auto& animate_menus = *find_descendant_of_type_named<GUI::CheckBox>("animate_menus_checkbox");
    animate_menus.set_checked(m_system_effects.animate_menus());
    animate_menus.on_checked = [this](bool checked) {
        m_system_effects.effects().at(Effects::AnimateMenus) = checked;
        set_modified(true);
    };
    auto& flash_menus = *find_descendant_of_type_named<GUI::CheckBox>("flash_menus_checkbox");
    flash_menus.set_checked(m_system_effects.flash_menus());
    flash_menus.on_checked = [this](bool checked) {
        m_system_effects.effects().at(Effects::FlashMenus) = checked;
        set_modified(true);
    };
    auto& animate_windows = *find_descendant_of_type_named<GUI::CheckBox>("animate_windows_checkbox");
    animate_windows.set_checked(m_system_effects.animate_windows());
    animate_windows.on_checked = [this](bool checked) {
        m_system_effects.effects().at(Effects::AnimateWindows) = checked;
        set_modified(true);
    };
    auto& smooth_scrolling = *find_descendant_of_type_named<GUI::CheckBox>("smooth_scrolling_checkbox");
    smooth_scrolling.set_checked(m_system_effects.smooth_scrolling());
    smooth_scrolling.on_checked = [this](bool checked) {
        m_system_effects.effects().at(Effects::SmoothScrolling) = checked;
        set_modified(true);
    };
    auto& tab_accents = *find_descendant_of_type_named<GUI::CheckBox>("tab_accents_checkbox");
    tab_accents.set_checked(m_system_effects.tab_accents());
    tab_accents.on_checked = [this](bool checked) {
        m_system_effects.effects().at(Effects::TabAccents) = checked;
        set_modified(true);
    };
    auto& splitter_knurls = *find_descendant_of_type_named<GUI::CheckBox>("splitter_knurls_checkbox");
    splitter_knurls.set_checked(m_system_effects.splitter_knurls());
    splitter_knurls.on_checked = [this](bool checked) {
        m_system_effects.effects().at(Effects::SplitterKnurls) = checked;
        set_modified(true);
    };
    auto& tooltips = *find_descendant_of_type_named<GUI::CheckBox>("tooltips_checkbox");
    tooltips.set_checked(m_system_effects.tooltips());
    tooltips.on_checked = [this](bool checked) {
        m_system_effects.effects().at(Effects::Tooltips) = checked;
        set_modified(true);
    };
    auto& menu_shadow = *find_descendant_of_type_named<GUI::CheckBox>("menu_shadow_checkbox");
    menu_shadow.set_checked(m_system_effects.menu_shadow());
    menu_shadow.on_checked = [this](bool checked) {
        m_system_effects.effects().at(Effects::MenuShadow) = checked;
        set_modified(true);
    };
    auto& window_shadow = *find_descendant_of_type_named<GUI::CheckBox>("window_shadow_checkbox");
    window_shadow.set_checked(m_system_effects.window_shadow());
    window_shadow.on_checked = [this](bool checked) {
        m_system_effects.effects().at(Effects::WindowShadow) = checked;
        set_modified(true);
    };
    auto& tooltip_shadow = *find_descendant_of_type_named<GUI::CheckBox>("tooltip_shadow_checkbox");
    tooltip_shadow.set_checked(m_system_effects.tooltip_shadow());
    tooltip_shadow.on_checked = [this](bool checked) {
        m_system_effects.effects().at(Effects::TooltipShadow) = checked;
        set_modified(true);
    };
}

ErrorOr<void> EffectsSettingsWidget::load_settings()
{
    auto ws_config = TRY(Core::ConfigFile::open("/etc/WindowServer.ini"));
    Vector<bool> effects = {
        ws_config->read_bool_entry("Effects", "AnimateMenus", true),
        ws_config->read_bool_entry("Effects", "FlashMenus", true),
        ws_config->read_bool_entry("Effects", "AnimateWindows", true),
        ws_config->read_bool_entry("Effects", "SmoothScrolling", true),
        ws_config->read_bool_entry("Effects", "TabAccents", true),
        ws_config->read_bool_entry("Effects", "SplitterKnurls", true),
        ws_config->read_bool_entry("Effects", "Tooltips", true),
        ws_config->read_bool_entry("Effects", "MenuShadow", true),
        ws_config->read_bool_entry("Effects", "WindowShadow", true),
        ws_config->read_bool_entry("Effects", "TooltipShadow", true),
    };
    auto geometry = WindowServer::ShowGeometryTools::string_to_enum(ws_config->read_entry("Effects", "ShowGeometry", "OnMoveAndResize"));
    m_system_effects = { effects, geometry };

    static constexpr Array list = {
        "On Move and Resize"sv,
        "On Move only"sv,
        "On Resize only"sv,
        "Never"sv
    };
    for (size_t i = 0; i < list.size(); ++i)
        TRY(m_geometry_list.try_append(list[i]));
    m_geometry_combobox->set_model(ItemListModel<String>::create(m_geometry_list));
    m_geometry_combobox->set_selected_index(m_system_effects.geometry());

    return {};
}

void EffectsSettingsWidget::apply_settings()
{
    ConnectionToWindowServer::the().async_set_system_effects(m_system_effects.effects(), m_system_effects.geometry());
}

}

}
