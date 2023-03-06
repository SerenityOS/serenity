/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Bytecode/PassManager.h>

namespace JS::Bytecode::Passes {

struct UnwindFrame {
    BasicBlock const* handler;
    BasicBlock const* finalizer;
};

static HashTable<BasicBlock const*> seen_blocks;
static Vector<UnwindFrame*> unwind_frames;

static void generate_cfg_for_block(BasicBlock const& current_block, PassPipelineExecutable executable)
{
    seen_blocks.set(&current_block);

    auto enter_label = [&](Label const& label, auto& entering_block) {
        executable.cfg->ensure(&entering_block).set(&label.block());
        executable.inverted_cfg->ensure(&label.block()).set(&entering_block);

        // The finalizer of an unwind context is handled separately
        if (!seen_blocks.contains(&label.block()) && unwind_frames.last()->finalizer != &current_block)
            generate_cfg_for_block(label.block(), executable);
    };

    if (auto const* handler = unwind_frames.last()->handler)
        enter_label(Label { *handler }, current_block);

    for (InstructionStreamIterator it { current_block.instruction_stream() }; !it.at_end(); ++it) {
        auto const& instruction = *it;

        if (instruction.type() == Instruction::Type::LeaveUnwindContext) {
            if (unwind_frames.last()->finalizer && unwind_frames.last()->finalizer != &current_block)
                dbgln("FIXME: Popping finalizer from the unwind context from outside the finalizer");
            unwind_frames.take_last();
        }

        if (!instruction.is_terminator())
            continue;

        using enum Instruction::Type;
        switch (instruction.type()) {
        case Jump: {
            auto true_target = *static_cast<Op::Jump const&>(instruction).true_target();
            enter_label(true_target, current_block);
            return;
        }
        case JumpConditional:
        case JumpNullish:
        case JumpUndefined: {
            // FIXME: Can we avoid this copy
            // Note: We might partially unwind here, so we need to make a copy of
            //       the current context to assure that the falsy code path has the same one
            auto saved_context = unwind_frames;

            auto true_target = *static_cast<Op::Jump const&>(instruction).true_target();
            enter_label(true_target, current_block);

            unwind_frames = move(saved_context);

            auto false_target = *static_cast<Op::Jump const&>(instruction).false_target();
            enter_label(false_target, current_block);
            return;
        }
        case Yield: {
            auto continuation = static_cast<Op::Yield const&>(instruction).continuation();
            if (continuation.has_value()) {
                executable.exported_blocks->set(&continuation->block());
                enter_label(*continuation, current_block);
            } else if (auto const* finalizer = unwind_frames.is_empty() ? nullptr : unwind_frames.last()->finalizer) {
                enter_label(Label { *finalizer }, current_block);
            }
            return;
        }
        case EnterUnwindContext: {
            auto entry_point = static_cast<Op::EnterUnwindContext const&>(instruction).entry_point();
            auto handler_target = static_cast<Op::EnterUnwindContext const&>(instruction).handler_target();
            auto finalizer_target = static_cast<Op::EnterUnwindContext const&>(instruction).finalizer_target();

            // We keep the frame alive here on the stack, to save some allocation size
            UnwindFrame frame { nullptr, finalizer_target.has_value() ? &finalizer_target->block() : nullptr };
            unwind_frames.append(&frame);

            if (handler_target.has_value()) {
                // We might pop from this context while in the handler, so we
                // need to save it and return it to its rightful place
                // FIXME: We can relax this when we are stricter about entering finally statements
                auto saved_context = unwind_frames;
                enter_label(*handler_target, current_block);
                unwind_frames = move(saved_context);

                frame.handler = &handler_target->block();
            }
            {
                // We might pop from this context while in the handler, so we
                // need to save it and return it to its rightful place
                // FIXME: We can relax this when we are stricter about entering finally statements
                auto saved_context = unwind_frames;
                enter_label(entry_point, current_block);
                unwind_frames = move(saved_context);
            }
            frame.handler = nullptr;

            if (finalizer_target.has_value()) {
                auto saved_context = unwind_frames;

                enter_label(*finalizer_target, current_block);
            }

            return;
        }
        case ContinuePendingUnwind: {
            auto resume_target = static_cast<Op::ContinuePendingUnwind const&>(instruction).resume_target();
            enter_label(resume_target, current_block);
            return;
        }
        case Throw:
            // Note: We technically register that we enter the handler in the prelude,
            //       but lets be correct and mark it again,
            //       this will be useful once we have more info on which instruction can
            //       actually fail
            if (auto const* handler = unwind_frames.last()->finalizer)
                enter_label(Label { *handler }, current_block);
            else if (auto const* finalizer = unwind_frames.last()->finalizer)
                enter_label(Label { *finalizer }, current_block);
            return;
        case Return:
            if (auto const* finalizer = unwind_frames.last()->finalizer)
                enter_label(Label { *finalizer }, current_block);
            return;
        case ScheduleJump:
            enter_label(Label { *unwind_frames.last()->finalizer }, current_block);
            enter_label(static_cast<Op::ScheduleJump const&>(instruction).target(), *unwind_frames.last()->finalizer);
            return;
        default:
            dbgln("Unhandled terminator instruction: `{}`", instruction.to_deprecated_string(executable.executable));
            VERIFY_NOT_REACHED();
        };
    }

    // We have left the block, but not through a designated terminator,
    // so before we return, we need to check if we still need to go through a finalizer
    if (auto const* finalizer = unwind_frames.last()->finalizer)
        enter_label(Label { *finalizer }, current_block);
}

void GenerateCFG::perform(PassPipelineExecutable& executable)
{
    started();

    executable.cfg = HashMap<BasicBlock const*, HashTable<BasicBlock const*>> {};
    executable.inverted_cfg = HashMap<BasicBlock const*, HashTable<BasicBlock const*>> {};
    executable.exported_blocks = HashTable<BasicBlock const*> {};

    seen_blocks.clear();
    unwind_frames.clear();
    UnwindFrame top_level_frame = {};

    unwind_frames.append(&top_level_frame);

    generate_cfg_for_block(*executable.executable.basic_blocks.first(), executable);

    finished();
}

}
