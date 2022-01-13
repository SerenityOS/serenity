/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/GlyphMapWidget.h>
#include <LibGUI/Statusbar.h>

class CharacterMapWidget final : public GUI::Widget {
    C_OBJECT(CharacterMapWidget);

public:
    virtual ~CharacterMapWidget() override;

    void initialize_menubar(GUI::Window& window);

private:
    CharacterMapWidget();

    virtual void did_change_font() override;
    void update_statusbar();

    RefPtr<GUI::Toolbar> m_toolbar;
    RefPtr<GUI::Label> m_font_name_label;
    RefPtr<GUI::GlyphMapWidget> m_glyph_map;
    RefPtr<GUI::TextBox> m_output_box;
    RefPtr<GUI::Button> m_copy_output_button;
    RefPtr<GUI::Statusbar> m_statusbar;

    RefPtr<GUI::Action> m_choose_font_action;
    RefPtr<GUI::Action> m_copy_selection_action;
};
