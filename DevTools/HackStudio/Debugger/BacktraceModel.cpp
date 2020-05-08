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

NonnullRefPtr<BacktraceModel> BacktraceModel::create(const DebugSession& debug_session, const PtraceRegisters& regs)
{
    return adopt(*new BacktraceModel(create_backtrace(debug_session, regs)));
}

GUI::Variant BacktraceModel::data(const GUI::ModelIndex& index, Role role) const
{
    if (role == Role::Display) {
        auto& frame = m_frames.at(index.row());
        return frame.function_name;
    }
    return {};
}

Vector<BacktraceModel::FrameInfo> BacktraceModel::create_backtrace(const DebugSession& debug_session, const PtraceRegisters& regs)
{
    u32 current_ebp = regs.ebp;
    u32 current_instruction = regs.eip;
    Vector<BacktraceModel::FrameInfo> frames;
    do {
        String name = debug_session.debug_info().name_of_containing_function(current_instruction);
        if (name.is_null()) {
            dbg() << "BacktraceModel: couldn't find containing function for address: " << (void*)current_instruction;
            break;
        }

        frames.append({ name, current_instruction, current_ebp });
        current_instruction = Debugger::the().session()->peek(reinterpret_cast<u32*>(current_ebp + 4)).value();
        current_ebp = Debugger::the().session()->peek(reinterpret_cast<u32*>(current_ebp)).value();
    } while (current_ebp);
    return frames;
}
