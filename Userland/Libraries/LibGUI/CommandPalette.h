/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Dialog.h>
#include <LibGUI/FilteringProxyModel.h>

namespace GUI {

class CommandPalette final : public GUI::Dialog {
    C_OBJECT(CommandPalette);

public:
    GUI::Action* selected_action() { return m_selected_action; }
    GUI::Action const* selected_action() const { return m_selected_action; }

private:
    explicit CommandPalette(GUI::Window& parent_window, ScreenPosition = ScreenPosition::CenterWithinParent);
    virtual ~CommandPalette() override = default;

    void collect_actions(GUI::Window& parent_window);
    void finish_with_index(GUI::ModelIndex const&);

    RefPtr<GUI::Action> m_selected_action;
    Vector<NonnullRefPtr<GUI::Action>> m_actions;

    RefPtr<GUI::TextBox> m_text_box;
    RefPtr<GUI::TableView> m_table_view;
    RefPtr<GUI::Model> m_model;
    RefPtr<GUI::FilteringProxyModel> m_filter_model;
};

}
