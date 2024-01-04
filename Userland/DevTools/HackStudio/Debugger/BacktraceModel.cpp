/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BacktraceModel.h"
#include "Debugger.h"
#include <LibDebug/StackFrameUtils.h>

namespace HackStudio {

NonnullRefPtr<BacktraceModel> BacktraceModel::create(Debug::ProcessInspector const& inspector, PtraceRegisters const& regs)
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
    FlatPtr current_frame_pointer = regs.bp();
    FlatPtr current_program_counter = regs.ip();
    Vector<BacktraceModel::FrameInfo> frames;
    size_t frame_index = 0;
    do {
        auto lib = inspector.library_at(current_program_counter);

        if (lib) {
            // After the first frame, current_instruction holds the return address from the function call.
            // We need to go back to the 'call' instruction to get accurate source position information.
            if (frame_index > 0)
                --current_program_counter;
            ByteString name = lib->debug_info->elf().symbolicate(current_program_counter - lib->base_address);
            if (name.is_empty()) {
                dbgln("BacktraceModel: couldn't find containing function for address: {:p} (library={})", current_program_counter, lib->name);
                name = "<missing>";
            }

            auto source_position = lib->debug_info->get_source_position(current_program_counter - lib->base_address);

            frames.append({ name, current_program_counter, current_frame_pointer, source_position });
        } else {
            dbgln("BacktraceModel: couldn't find containing library for address: {:p}", current_program_counter);
            frames.append({ "<missing>", current_program_counter, current_frame_pointer, {} });
        }

        auto frame_info = Debug::StackFrameUtils::get_info(inspector, current_frame_pointer);
        VERIFY(frame_info.has_value());
        current_program_counter = frame_info.value().return_address;
        current_frame_pointer = frame_info.value().next_ebp;
        ++frame_index;
    } while (current_frame_pointer && current_program_counter);
    return frames;
}

}
