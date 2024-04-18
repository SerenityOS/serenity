/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DebugInfoWidget.h"
#include "BacktraceModel.h"
#include "Debugger.h"
#include "RegistersModel.h"
#include "VariablesModel.h"
#include <AK/StringBuilder.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Model.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TreeView.h>

namespace HackStudio {

ErrorOr<void> DebugInfoWidget::init_toolbar()
{
    m_continue_action = GUI::Action::create("Continue", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/debug-continue.png"sv)), [](auto&) {
        Debugger::the().set_requested_debugger_action(Debugger::DebuggerAction::Continue);
    });

    m_singlestep_action = GUI::Action::create("Step Over", { Mod_None, Key_F10 }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/debug-step-over.png"sv)), [](auto&) {
        Debugger::the().set_requested_debugger_action(Debugger::DebuggerAction::SourceStepOver);
    });

    m_step_in_action = GUI::Action::create("Step In", { Mod_None, Key_F11 }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/debug-step-in.png"sv)), [](auto&) {
        Debugger::the().set_requested_debugger_action(Debugger::DebuggerAction::SourceSingleStep);
    });

    m_step_out_action = GUI::Action::create("Step Out", { Mod_Shift, Key_F11 }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/debug-step-out.png"sv)), [](auto&) {
        Debugger::the().set_requested_debugger_action(Debugger::DebuggerAction::SourceStepOut);
    });

    m_pause_action = GUI::Action::create("Pause", {}, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/debug-pause.png"sv)), [](auto&) {
        Debugger::the().stop_debuggee();
    });

    m_toolbar->add_action(*m_continue_action);
    m_toolbar->add_action(*m_singlestep_action);
    m_toolbar->add_action(*m_step_in_action);
    m_toolbar->add_action(*m_step_out_action);
    m_toolbar->add_action(*m_pause_action);

    set_debug_actions_enabled(false, {});

    return {};
}

ErrorOr<NonnullRefPtr<DebugInfoWidget>> DebugInfoWidget::create()
{
    auto debuginfo_widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DebugInfoWidget));

    auto& toolbar_container = debuginfo_widget->add<GUI::ToolbarContainer>();
    debuginfo_widget->m_toolbar = toolbar_container.add<GUI::Toolbar>();

    TRY(debuginfo_widget->init_toolbar());

    return debuginfo_widget;
}

DebugInfoWidget::DebugInfoWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    auto& bottom_box = add<GUI::Widget>();
    bottom_box.set_layout<GUI::HorizontalBoxLayout>();

    auto& splitter = bottom_box.add<GUI::HorizontalSplitter>();
    m_backtrace_view = splitter.add<GUI::ListView>();
    auto& variables_tab_widget = splitter.add<GUI::TabWidget>();
    variables_tab_widget.set_tab_position(TabPosition::Bottom);
    variables_tab_widget.add_widget(build_variables_tab());
    variables_tab_widget.add_widget(build_registers_tab());

    m_backtrace_view->on_selection_change = [this] {
        auto const& index = m_backtrace_view->selection().first();

        if (!index.is_valid()) {
            return;
        }

        auto& model = static_cast<BacktraceModel&>(*m_backtrace_view->model());
        // Note: The reconstruction of the register set here is obviously incomplete.
        // We currently only reconstruct eip & ebp. Ideally would also reconstruct the other registers somehow.
        // (Other registers may be needed to get the values of variables who are not stored on the stack)
        PtraceRegisters frame_regs {};
        auto backtrace_frame = model.frames()[index.row()];
        frame_regs.set_ip(backtrace_frame.instruction_address);
        frame_regs.set_bp(backtrace_frame.frame_base);

        m_variables_view->set_model(VariablesModel::create(static_cast<VariablesModel&>(*m_variables_view->model()).inspector(), frame_regs));
        if (on_backtrace_frame_selection && backtrace_frame.m_source_position.has_value()) {
            on_backtrace_frame_selection(*backtrace_frame.m_source_position);
        } else {
            dbgln("no source position info");
        }
    };
}

bool DebugInfoWidget::does_variable_support_writing(Debug::DebugInfo::VariableInfo const* variable)
{
    if (variable->location_type != Debug::DebugInfo::VariableInfo::LocationType::Address)
        return false;
    return variable->is_enum_type() || variable->type_name.is_one_of("int", "bool");
}

RefPtr<GUI::Menu> DebugInfoWidget::get_context_menu_for_variable(const GUI::ModelIndex& index)
{
    if (!index.is_valid())
        return nullptr;
    auto context_menu = GUI::Menu::construct();

    auto* variable = static_cast<Debug::DebugInfo::VariableInfo const*>(index.internal_data());
    if (does_variable_support_writing(variable)) {
        context_menu->add_action(GUI::Action::create("Change value", [&](auto&) {
            String value;
            if (GUI::InputBox::show(window(), value, "Enter new value:"sv, "Set variable value"sv) == GUI::InputBox::ExecResult::OK) {
                auto& model = static_cast<VariablesModel&>(*m_variables_view->model());
                model.set_variable_value(index, value, window());
            }
        }));
    }

    auto variable_address = variable->location_data.address;
    if (Debugger::the().session()->watchpoint_exists(variable_address)) {
        context_menu->add_action(GUI::Action::create("Remove watchpoint", [variable_address](auto&) {
            Debugger::the().session()->remove_watchpoint(variable_address);
        }));
    } else {
        auto& backtrace_model = static_cast<BacktraceModel&>(*m_backtrace_view->model());
        auto current_frame = m_backtrace_view->selection().first().row();
        auto ebp = backtrace_model.frames()[current_frame].frame_base;
        context_menu->add_action(GUI::Action::create("Add watchpoint", [variable_address, ebp](auto&) {
            Debugger::the().session()->insert_watchpoint(variable_address, ebp);
        }));
    }
    return context_menu;
}

NonnullRefPtr<GUI::Widget> DebugInfoWidget::build_variables_tab()
{
    auto variables_widget = GUI::Widget::construct();
    variables_widget->set_title("Variables"_string);
    variables_widget->set_layout<GUI::HorizontalBoxLayout>();

    m_variables_view = variables_widget->add<GUI::TreeView>();

    m_variables_view->on_context_menu_request = [this](auto& index, auto& event) {
        m_variable_context_menu = get_context_menu_for_variable(index);
        if (m_variable_context_menu)
            m_variable_context_menu->popup(event.screen_position());
    };

    return variables_widget;
}

NonnullRefPtr<GUI::Widget> DebugInfoWidget::build_registers_tab()
{
    auto registers_widget = GUI::Widget::construct();
    registers_widget->set_title("Registers"_string);
    registers_widget->set_layout<GUI::HorizontalBoxLayout>();

    m_registers_view = registers_widget->add<GUI::TableView>();

    return registers_widget;
}

void DebugInfoWidget::update_state(Debug::ProcessInspector& inspector, PtraceRegisters const& regs)
{
    m_variables_view->set_model(VariablesModel::create(inspector, regs));
    m_backtrace_view->set_model(BacktraceModel::create(inspector, regs));
    if (m_registers_view->model()) {
        auto& previous_registers = static_cast<RegistersModel*>(m_registers_view->model())->raw_registers();
        m_registers_view->set_model(RegistersModel::create(regs, previous_registers));
    } else {
        m_registers_view->set_model(RegistersModel::create(regs));
    }
    auto selected_index = m_backtrace_view->model()->index(0);
    if (!selected_index.is_valid()) {
        dbgln("Warning: DebugInfoWidget: backtrace selected index is invalid");
        return;
    }
    m_backtrace_view->selection().set(selected_index);
}

void DebugInfoWidget::program_stopped()
{
    m_variables_view->set_model({});
    m_backtrace_view->set_model({});
    m_registers_view->set_model({});
}

void DebugInfoWidget::set_debug_actions_enabled(bool enabled, Optional<DebugActionsState> state)
{
    if (!enabled) {
        m_continue_action->set_enabled(false);
        m_singlestep_action->set_enabled(false);
        m_step_in_action->set_enabled(false);
        m_step_out_action->set_enabled(false);
        m_pause_action->set_enabled(false);
        return;
    }

    m_continue_action->set_enabled(state == DebugActionsState::DebuggeeStopped);
    m_singlestep_action->set_enabled(state == DebugActionsState::DebuggeeStopped);
    m_step_in_action->set_enabled(state == DebugActionsState::DebuggeeStopped);
    m_step_out_action->set_enabled(state == DebugActionsState::DebuggeeStopped);
    m_pause_action->set_enabled(state == DebugActionsState::DebuggeeRunning);
}

}
