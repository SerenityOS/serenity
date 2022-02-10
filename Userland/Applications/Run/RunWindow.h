/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Window.h>

class RunWindow final : public GUI::Window {
    C_OBJECT(RunWindow)
public:
    virtual ~RunWindow() override = default;

    virtual void event(Core::Event&) override;

private:
    RunWindow();

    void do_run();
    bool run_as_command(const String& run_input);
    bool run_via_launch(const String& run_input);

    String history_file_path();
    void load_history();
    void save_history();

    Vector<String> m_path_history;
    NonnullRefPtr<GUI::ItemListModel<String>> m_path_history_model;

    RefPtr<GUI::ImageWidget> m_icon_image_widget;
    RefPtr<GUI::Button> m_ok_button;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::Button> m_browse_button;
    RefPtr<GUI::ComboBox> m_path_combo_box;
};
