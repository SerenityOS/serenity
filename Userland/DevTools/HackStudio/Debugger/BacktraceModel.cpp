/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BacktraceModel.h"
#include "Debugger.h"
#include <AK/StackUnwinder.h>

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
    Vector<BacktraceModel::FrameInfo> frames;

    auto add_frame = [&frames, &inspector](FlatPtr address, FlatPtr frame_pointer) {
        auto const* lib = inspector.library_at(address);

        if (lib) {
            ByteString name = lib->debug_info->elf().symbolicate(address - lib->base_address);
            if (name.is_empty()) {
                dbgln("BacktraceModel: couldn't find containing function for address: {:p} (library={})", address, lib->name);
                name = "<missing>";
            }

            auto source_position = lib->debug_info->get_source_position(address - lib->base_address);

            frames.append({ name, address, frame_pointer, source_position });
        } else {
            dbgln("BacktraceModel: couldn't find containing library for address: {:p}", address);
            frames.append({ "<missing>", address, frame_pointer, {} });
        }
    };

    add_frame(regs.ip(), regs.bp());

    MUST(AK::unwind_stack_from_frame_pointer(
        regs.bp(),
        [&](FlatPtr address) -> ErrorOr<FlatPtr> {
            auto maybe_value = inspector.peek(address);
            if (!maybe_value.has_value())
                return EFAULT;

            return maybe_value.value();
        },
        [&add_frame](AK::StackFrame stack_frame) -> ErrorOr<IterationDecision> {
            // Subtract one from return_address to go back to the calling instruction to get accurate source position information.
            auto address = stack_frame.return_address - 1;

            add_frame(address, stack_frame.previous_frame_pointer);

            return IterationDecision::Continue;
        }));

    return frames;
}

}
