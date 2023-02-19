/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Debugger.h"
#include <AK/NonnullOwnPtr.h>
#include <LibGUI/Action.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Model.h>
#include <LibGUI/TableView.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Widget.h>
#include <sys/arch/regs.h>

namespace HackStudio {

class DebugInfoWidget final : public GUI::Widget {
    C_OBJECT(DebugInfoWidget)
public:
    static ErrorOr<NonnullRefPtr<DebugInfoWidget>> create();
    virtual ~DebugInfoWidget() override { }

    void update_state(Debug::ProcessInspector&, PtraceRegisters const&);
    void program_stopped();

    enum class DebugActionsState {
        DebuggeeRunning,
        DebuggeeStopped,
    };
    void set_debug_actions_enabled(bool enabled, Optional<DebugActionsState>);

    Function<void(Debug::DebugInfo::SourcePosition const&)> on_backtrace_frame_selection;

private:
    explicit DebugInfoWidget();
    ErrorOr<void> init_toolbar();

    NonnullRefPtr<GUI::Widget> build_variables_tab();
    NonnullRefPtr<GUI::Widget> build_registers_tab();
    bool does_variable_support_writing(Debug::DebugInfo::VariableInfo const*);
    RefPtr<GUI::Menu> get_context_menu_for_variable(const GUI::ModelIndex&);

    RefPtr<GUI::TreeView> m_variables_view;
    RefPtr<GUI::TableView> m_registers_view;
    RefPtr<GUI::ListView> m_backtrace_view;
    RefPtr<GUI::Menu> m_variable_context_menu;
    RefPtr<GUI::Toolbar> m_toolbar;
    RefPtr<GUI::Action> m_continue_action;
    RefPtr<GUI::Action> m_singlestep_action;
    RefPtr<GUI::Action> m_step_in_action;
    RefPtr<GUI::Action> m_step_out_action;
    RefPtr<GUI::Action> m_pause_action;
};

}
