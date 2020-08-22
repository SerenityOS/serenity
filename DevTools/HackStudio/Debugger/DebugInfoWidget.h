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

#pragma once

#include "Debugger.h"
#include <AK/NonnullOwnPtr.h>
#include <LibGUI/Action.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Model.h>
#include <LibGUI/ToolBar.h>
#include <LibGUI/ToolBarContainer.h>
#include <LibGUI/Widget.h>
#include <sys/arch/i386/regs.h>

namespace HackStudio {

class DebugInfoWidget final : public GUI::Widget {
    C_OBJECT(DebugInfoWidget)
public:
    virtual ~DebugInfoWidget() override {}

    void update_state(const DebugSession&, const PtraceRegisters&);
    void program_stopped();

    GUI::Action& continue_action();
    GUI::Action& singlestep_action();

private:
    explicit DebugInfoWidget();
    void init_toolbar();

    RefPtr<GUI::TreeView> m_variables_view;
    RefPtr<GUI::ListView> m_backtrace_view;
    RefPtr<GUI::Menu> m_variable_context_menu;
    RefPtr<GUI::ToolBar> m_toolbar;
    RefPtr<GUI::Action> m_continue_action;
    RefPtr<GUI::Action> m_singlestep_action;
};

}
