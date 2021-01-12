/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
#include <LibGUI/Layout.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Model.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TreeView.h>

namespace HackStudio {

void DebugInfoWidget::init_toolbar()
{
    m_continue_action = GUI::Action::create("Continue", Gfx::Bitmap::load_from_file("/res/icons/16x16/debug-continue.png"), [](auto&) {
        Debugger::the().set_requested_debugger_action(Debugger::DebuggerAction::Continue);
    });

    m_singlestep_action = GUI::Action::create("Step Over", { Mod_None, Key_F10 }, Gfx::Bitmap::load_from_file("/res/icons/16x16/debug-step-over.png"), [](auto&) {
        Debugger::the().set_requested_debugger_action(Debugger::DebuggerAction::SourceStepOver);
    });

    m_step_in_action = GUI::Action::create("Step In", { Mod_None, Key_F11 }, Gfx::Bitmap::load_from_file("/res/icons/16x16/debug-step-in.png"), [](auto&) {
        Debugger::the().set_requested_debugger_action(Debugger::DebuggerAction::SourceSingleStep);
    });

    m_step_out_action = GUI::Action::create("Step Out", { Mod_Shift, Key_F11 }, Gfx::Bitmap::load_from_file("/res/icons/16x16/debug-step-out.png"), [](auto&) {
        Debugger::the().set_requested_debugger_action(Debugger::DebuggerAction::SourceStepOut);
    });

    m_toolbar->add_action(*m_continue_action);
    m_toolbar->add_action(*m_singlestep_action);
    m_toolbar->add_action(*m_step_in_action);
    m_toolbar->add_action(*m_step_out_action);

    set_debug_actions_enabled(false);
}

DebugInfoWidget::DebugInfoWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    auto& toolbar_container = add<GUI::ToolBarContainer>();
    m_toolbar = toolbar_container.add<GUI::ToolBar>();
    init_toolbar();
    auto& bottom_box = add<GUI::Widget>();
    bottom_box.set_layout<GUI::HorizontalBoxLayout>();

    auto& splitter = bottom_box.add<GUI::HorizontalSplitter>();
    m_backtrace_view = splitter.add<GUI::ListView>();
    auto& variables_tab_widget = splitter.add<GUI::TabWidget>();
    variables_tab_widget.set_tab_position(GUI::TabWidget::TabPosition::Bottom);
    variables_tab_widget.add_widget("Variables", build_variables_tab());
    variables_tab_widget.add_widget("Registers", build_registers_tab());

    m_backtrace_view->on_selection = [this](auto& index) {
        auto& model = static_cast<BacktraceModel&>(*m_backtrace_view->model());

        // Note: The reconstruction of the register set here is obviously incomplete.
        // We currently only reconstruct eip & ebp. Ideally would also reconstruct the other registers somehow.
        // (Other registers may be needed to get the values of variables who are not stored on the stack)
        PtraceRegisters frame_regs {};
        frame_regs.eip = model.frames()[index.row()].instruction_address;
        frame_regs.ebp = model.frames()[index.row()].frame_base;

        m_variables_view->set_model(VariablesModel::create(frame_regs));
    };
}

NonnullRefPtr<GUI::Widget> DebugInfoWidget::build_variables_tab()
{
    auto variables_widget = GUI::Widget::construct();
    variables_widget->set_layout<GUI::HorizontalBoxLayout>();

    m_variables_view = variables_widget->add<GUI::TreeView>();

    auto is_valid_index = [](auto& index) {
        if (!index.is_valid())
            return false;
        auto* variable = static_cast<const Debug::DebugInfo::VariableInfo*>(index.internal_data());
        if (variable->location_type != Debug::DebugInfo::VariableInfo::LocationType::Address)
            return false;
        return variable->is_enum_type() || variable->type_name.is_one_of("int", "bool");
    };

    m_variables_view->on_context_menu_request = [this, is_valid_index](auto& index, auto& event) {
        if (!is_valid_index(index))
            return;
        m_variable_context_menu->popup(event.screen_position());
    };

    m_variables_view->on_activation = [this, is_valid_index](auto& index) {
        if (!is_valid_index(index))
            return;

        String value;
        if (GUI::InputBox::show(value, window(), "Enter new value:", "Set variable value") == GUI::InputBox::ExecOK) {
            auto& model = static_cast<VariablesModel&>(*m_variables_view->model());
            model.set_variable_value(index, value, window());
        }
    };

    auto edit_variable_action = GUI::Action::create("Change value", [this](auto&) {
        m_variables_view->on_activation(m_variables_view->selection().first());
    });

    m_variable_context_menu = GUI::Menu::construct();
    m_variable_context_menu->add_action(edit_variable_action);

    return variables_widget;
}

NonnullRefPtr<GUI::Widget> DebugInfoWidget::build_registers_tab()
{
    auto registers_widget = GUI::Widget::construct();
    registers_widget->set_layout<GUI::HorizontalBoxLayout>();

    m_registers_view = registers_widget->add<GUI::TableView>();

    return registers_widget;
}

void DebugInfoWidget::update_state(const Debug::DebugSession& debug_session, const PtraceRegisters& regs)
{
    m_variables_view->set_model(VariablesModel::create(regs));
    m_backtrace_view->set_model(BacktraceModel::create(debug_session, regs));
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

void DebugInfoWidget::set_debug_actions_enabled(bool enabled)
{
    m_continue_action->set_enabled(enabled);
    m_singlestep_action->set_enabled(enabled);
    m_step_in_action->set_enabled(enabled);
    m_step_out_action->set_enabled(enabled);
}

}
