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

namespace Run {

class RunWindow final : public GUI::Window {
    C_OBJECT(RunWindow)
public:
    virtual ~RunWindow() override = default;

    virtual void event(Core::Event&) override;

private:
    RunWindow();

    void do_run();
    bool run_as_command(ByteString const& run_input);
    bool run_via_launch(ByteString const& run_input);

    ByteString history_file_path();
    ErrorOr<void> load_history();
    ErrorOr<void> save_history();
    void prepend_history(ByteString const& input);

    Vector<ByteString> m_path_history;
    NonnullRefPtr<GUI::ItemListModel<ByteString>> m_path_history_model;

    RefPtr<GUI::ImageWidget> m_icon_image_widget;
    RefPtr<GUI::Button> m_ok_button;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::Button> m_browse_button;
    RefPtr<GUI::ComboBox> m_path_combo_box;
};

}
