/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BacktraceModel.h"
#include "Debugger.h"
#include <LibDebug/StackFrameUtils.h>

namespace HackStudio {

NonnullRefPtr<BacktraceModel> BacktraceModel::create(const Debug::DebugSession& debug_session, PtraceRegisters const& regs)
{
    return adopt_ref(*new BacktraceModel(create_backtrace(debug_session, regs)));
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

Vector<BacktraceModel::FrameInfo> BacktraceModel::create_backtrace(const Debug::DebugSession& debug_session, PtraceRegisters const& regs)
{
    FlatPtr current_ebp = regs.bp();
    FlatPtr current_instruction = regs.ip();
    Vector<BacktraceModel::FrameInfo> frames;
    do {
        auto lib = debug_session.library_at(regs.ip());
        if (!lib)
            continue;
        String name = lib->debug_info->name_of_containing_function(current_instruction - lib->base_address);
        if (name.is_null()) {
            dbgln("BacktraceModel: couldn't find containing function for address: {:p}", current_instruction);
            name = "<missing>";
        }

        frames.append({ name, current_instruction, current_ebp });
        auto frame_info = Debug::StackFrameUtils::get_info(*Debugger::the().session(), current_ebp);
        VERIFY(frame_info.has_value());
        current_instruction = frame_info.value().return_address;
        current_ebp = frame_info.value().next_ebp;
    } while (current_ebp && current_instruction);
    return frames;
}

}
