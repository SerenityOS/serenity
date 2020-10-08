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

#include "BacktraceModel.h"
#include "Debugger.h"
#include <LibDebug/StackFrameUtils.h>

namespace HackStudio {

NonnullRefPtr<BacktraceModel> BacktraceModel::create(const Debug::DebugSession& debug_session, const PtraceRegisters& regs)
{
    return adopt(*new BacktraceModel(create_backtrace(debug_session, regs)));
}

GUI::Variant BacktraceModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    if (role == GUI::ModelRole::Display) {
        auto& frame = m_frames.at(index.row());
        return frame.function_name;
    }
    return {};
}

GUI::ModelIndex BacktraceModel::index(int row, int column, const GUI::ModelIndex&) const
{
    if (row < 0 || row >= static_cast<int>(m_frames.size()))
        return {};
    return create_index(row, column, &m_frames.at(row));
}

Vector<BacktraceModel::FrameInfo> BacktraceModel::create_backtrace(const Debug::DebugSession& debug_session, const PtraceRegisters& regs)
{
    u32 current_ebp = regs.ebp;
    u32 current_instruction = regs.eip;
    Vector<BacktraceModel::FrameInfo> frames;
    do {
        String name = debug_session.debug_info().name_of_containing_function(current_instruction);
        if (name.is_null()) {
            dbgln("BacktraceModel: couldn't find containing function for address: {:p}", current_instruction);
            name = "<missing>";
        }

        frames.append({ name, current_instruction, current_ebp });
        auto frame_info = Debug::StackFrameUtils::get_info(*Debugger::the().session(), current_ebp);
        ASSERT(frame_info.has_value());
        current_instruction = frame_info.value().return_address;
        current_ebp = frame_info.value().next_ebp;
    } while (current_ebp && current_instruction);
    return frames;
}

}
