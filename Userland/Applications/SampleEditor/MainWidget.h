/*
 * Copyright (c) 2025, Lee Hanken
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "SampleWidget.h"
#include <AK/ByteString.h>
#include <AK/RefPtr.h>
#include <LibGUI/Action.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Frame.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/Window.h>

class MainWidget : public GUI::Frame
    , public GUI::Clipboard::ClipboardClient {
    C_OBJECT(MainWidget)

public:
    ErrorOr<void> initialize_menu_and_toolbar(NonnullRefPtr<GUI::Window> window);
    ErrorOr<void> open(StringView path);
    ErrorOr<void> save(StringView path);
    void update_action_states();

    // ^GUI::Clipboard::ClipboardClient
    virtual void clipboard_content_did_change(ByteString const& mime_type) override;

private:
    MainWidget();
    virtual ~MainWidget() override = default;

    ByteString m_sample_name;
    ByteString m_sample_path;
    RefPtr<GUI::ToolbarContainer> m_toolbar_container;
    RefPtr<GUI::Toolbar> m_toolbar;
    RefPtr<GUI::Action> m_new_action;
    RefPtr<GUI::Action> m_open_action;
    RefPtr<GUI::Action> m_save_action;
    RefPtr<GUI::Action> m_save_as_action;
    RefPtr<GUI::Action> m_save_all_action;
    RefPtr<GUI::Action> m_copy_action;
    RefPtr<GUI::Action> m_cut_action;
    RefPtr<GUI::Action> m_paste_action;
    RefPtr<GUI::Action> m_zoom_in_action;
    RefPtr<GUI::Action> m_zoom_out_action;
    RefPtr<GUI::Action> m_clear_selection_action;
    RefPtr<GUI::Action> m_select_all_action;
    RefPtr<GUI::Action> m_play_action;
    RefPtr<GUI::Action> m_stop_action;
    RefPtr<SampleWidget> m_sample_widget;
};
