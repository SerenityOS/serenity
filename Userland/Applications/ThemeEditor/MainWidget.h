/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "PreviewWidget.h"
#include <AK/Time.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/SystemTheme.h>

namespace ThemeEditor {

class MainWidget final : public GUI::Widget {
    C_OBJECT(MainWidget);

public:
    virtual ~MainWidget() override = default;

    ErrorOr<void> initialize_menubar(GUI::Window&);
    GUI::Window::CloseRequestDecision request_close();
    void update_title();

private:
    explicit MainWidget(Optional<String> path, Gfx::Palette startup_preview_palette);

    void save_to_file(Core::File&);
    void set_path(String);

    RefPtr<PreviewWidget> m_preview_widget;
    RefPtr<GUI::Action> m_save_action;

    Optional<String> m_path;
    Time m_last_modified_time { Time::now_monotonic() };

    Vector<Gfx::AlignmentRole> m_alignment_roles;
    Vector<Gfx::ColorRole> m_color_roles;
    Vector<Gfx::FlagRole> m_flag_roles;
    Vector<Gfx::MetricRole> m_metric_roles;
    Vector<Gfx::PathRole> m_path_roles;

    OwnPtr<GUI::ActionGroup> m_preview_type_action_group;
};

}
