/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Julius Heijmen <julius.heijmen@gmail.com>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, Karol Kosek <krkk@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

class MainWidget final : public GUI::Widget {
    C_OBJECT_ABSTRACT(MainWidget)

public:
    static ErrorOr<NonnullRefPtr<MainWidget>> try_create(GUI::Icon const&);
    ErrorOr<void> initialize_menubar(GUI::Window&);
    GUI::Window::CloseRequestDecision request_close();

    void load_file(FileSystemAccessClient::File);
    void update_title();

    GUI::TextEditor& editor() const { return *m_editor; }

private:
    MainWidget();
    virtual void drag_enter_event(GUI::DragEvent&) override;
    virtual void drop_event(GUI::DropEvent&) override;

    RefPtr<GUI::Action> m_save_action;
    RefPtr<GUI::Action> m_save_as_action;
    RefPtr<GUI::TextEditor> m_editor;
    RefPtr<GUI::Toolbar> m_toolbar;
    RefPtr<GUI::Splitter> m_splitter;
    RefPtr<GUI::Statusbar> m_statusbar;

    RefPtr<GUI::Frame> m_preview_frame_widget;
    RefPtr<GUI::Window> m_preview_window;
    RefPtr<GUI::Widget> m_preview_window_widget;
    GUI::Widget* m_preview;

    GUI::ActionGroup m_views_group;
    RefPtr<GUI::Action> m_view_frame_action;
    RefPtr<GUI::Action> m_view_window_action;

    GUI::Icon m_icon;
    ByteString m_file_path;
};
