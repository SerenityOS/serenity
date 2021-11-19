/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BacktraceModel.h"
#include "Debugger.h"
#include <LibDebug/StackFrameUtils.h>

namespace HackStudio {

NonnullRefPtr<BacktraceModel> BacktraceModel::create(Debug::ProcessInspector const& inspector, const PtraceRegisters& regs)
{
    return adopt_ref(*new BacktraceModel(create_backtrace(inspector, regs)));
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

Vector<BacktraceModel::FrameInfo> BacktraceModel::create_backtrace(Debug::ProcessInspector const& inspector, PtraceRegisters const& regs)
{
    FlatPtr current_ebp = regs.bp();
    FlatPtr current_instruction = regs.ip();
    Vector<BacktraceModel::FrameInfo> frames;
    size_t frame_index = 0;
    do {
        auto lib = inspector.library_at(current_instruction);
        if (!lib)
            continue;

        // After the first frame, current_instruction holds the return address from the function call.
        // We need to go back to the 'call' instruction to get accurate source position information.
        if (frame_index > 0)
            --current_instruction;
        String name = lib->debug_info->elf().symbolicate(current_instruction - lib->base_address);
        if (name.is_null()) {
            dbgln("BacktraceModel: couldn't find containing function for address: {:p} (library={})", current_instruction, lib->name);
            name = "<missing>";
        }

        auto source_position = lib->debug_info->get_source_position(current_instruction - lib->base_address);

        frames.append({ name, current_instruction, current_ebp, source_position });
        auto frame_info = Debug::StackFrameUtils::get_info(inspector, current_ebp);
        VERIFY(frame_info.has_value());
        current_instruction = frame_info.value().return_address;
        current_ebp = frame_info.value().next_ebp;
        ++frame_index;
    } while (current_ebp && current_instruction);
    return frames;
}

}
