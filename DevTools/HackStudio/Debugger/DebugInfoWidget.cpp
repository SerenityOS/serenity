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
#include "VariablesModel.h"
#include <AK/StringBuilder.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Model.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/TreeView.h>

DebugInfoWidget::DebugInfoWidget()
{
    set_layout<GUI::HorizontalBoxLayout>();
    auto& splitter = add<GUI::HorizontalSplitter>();
    m_backtrace_view = splitter.add<GUI::ListView>();
    m_variables_view = splitter.add<GUI::TreeView>();
}

void DebugInfoWidget::update_state(const DebugSession& debug_session, const PtraceRegisters& regs)
{
    m_variables_view->set_model(VariablesModel::create(regs));
    m_backtrace_view->set_model(BacktraceModel::create(debug_session, regs));
    m_backtrace_view->selection().set(m_backtrace_view->model()->index(0));

    m_backtrace_view->on_selection
        = [this](auto& index) {
              auto& model = static_cast<BacktraceModel&>(*m_backtrace_view->model());

              // Note: The recontruction of the register set here is obviously incomplete.
              // We currently only reconstruct eip & ebp. Ideally would also reconstruct the other registers somehow.
              // (Other registers may be needed to get the values of variables who are not stored on the stack)
              PtraceRegisters frame_regs {};
              frame_regs.eip = model.frames()[index.row()].instruction_address;
              frame_regs.ebp = model.frames()[index.row()].frame_base;

              m_variables_view->set_model(VariablesModel::create(frame_regs));
          };
}

void DebugInfoWidget::program_stopped()
{
    m_variables_view->set_model({});
}
