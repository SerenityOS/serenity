/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TemporaryChange.h>
#include <LibJS/Bytecode/PassManager.h>

namespace JS::Bytecode::Passes {

struct UnwindFrame {
    BasicBlock const* handler;
    BasicBlock const* finalizer;
    Vector<BasicBlock const*> finalizer_targets;
};

static HashTable<BasicBlock const*> seen_blocks;
static Vector<UnwindFrame*> unwind_frames;

static BasicBlock const* next_handler_or_finalizer()
{
    return unwind_frames.last()->handler ?: unwind_frames.last()->finalizer;
}

static void generate_cfg_for_block(BasicBlock const& current_block, PassPipelineExecutable& executable)
{
    seen_blocks.set(&current_block);

    auto enter_label = [&](Label const& label, BasicBlock const& entering_block) {
        executable.cfg->ensure(&entering_block).set(&label.block());
        executable.inverted_cfg->ensure(&label.block()).set(&entering_block);

        // The finalizers and handlers of an unwind context are handled separately
        if (!seen_blocks.contains(&label.block())
            && &label.block() != unwind_frames.last()->handler
            && &label.block() != unwind_frames.last()->finalizer)
            generate_cfg_for_block(label.block(), executable);
    };

    if (auto const* block = next_handler_or_finalizer())
        enter_label(Label { *block }, current_block);

    for (InstructionStreamIterator it { current_block.instruction_stream() }; !it.at_end(); ++it) {
        auto const& instruction = *it;

        if (instruction.type() == Instruction::Type::LeaveUnwindContext) {
            if (unwind_frames.last()->finalizer && unwind_frames.last()->finalizer != &current_block)
                dbgln("FIXME: Popping finalizer from the unwind context from outside the finalizer");
            unwind_frames.take_last();

            if (auto const* block = next_handler_or_finalizer())
                enter_label(Label { *block }, current_block);
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
            // FIXME: It would be nice if we could avoid this copy, if we know that the unwind context stays the same in both paths
            //        Or with a COW capable Vector alternative
            // Note: We might partially unwind here, so we need to make a copy of
            //       the current context to assure that the falsy code path has the same one
            {
                TemporaryChange saved_context { unwind_frames, unwind_frames };

                auto true_target = *static_cast<Op::Jump const&>(instruction).true_target();
                enter_label(true_target, current_block);
            }

            auto false_target = *static_cast<Op::Jump const&>(instruction).false_target();
            enter_label(false_target, current_block);
            return;
        }
        case Yield: {
            auto continuation = static_cast<Op::Yield const&>(instruction).continuation();
            if (continuation.has_value()) {
                executable.exported_blocks->set(&continuation->block());
                enter_label(*continuation, current_block);
            } else if (auto const* finalizer = unwind_frames.last()->finalizer) {
                enter_label(Label { *finalizer }, current_block);
                unwind_frames.last()->finalizer_targets.append(nullptr);
            }
            return;
        }
        case Await: {
            auto const& continuation = static_cast<Op::Await const&>(instruction).continuation();
            executable.exported_blocks->set(&continuation.block());
            enter_label(continuation, current_block);
            return;
        }
        case EnterUnwindContext: {
            auto entry_point = static_cast<Op::EnterUnwindContext const&>(instruction).entry_point();
            auto handler_target = static_cast<Op::EnterUnwindContext const&>(instruction).handler_target();
            auto finalizer_target = static_cast<Op::EnterUnwindContext const&>(instruction).finalizer_target();

            // We keep the frame alive here on the stack, to save some allocation size
            UnwindFrame frame {
                .handler = handler_target.has_value() ? &handler_target->block() : nullptr,
                .finalizer = finalizer_target.has_value() ? &finalizer_target->block() : nullptr,
                .finalizer_targets = {}
            };

            unwind_frames.append(&frame);

            {
                // This will enter the handler and finalizer when needed.
                TemporaryChange saved_context { unwind_frames, unwind_frames };
                enter_label(entry_point, current_block);
            }
            frame.handler = nullptr;
            if (handler_target.has_value()) {
                // We manually generate the CFG, because we previously skiped it
                TemporaryChange saved_context { unwind_frames, unwind_frames };
                generate_cfg_for_block(handler_target->block(), executable);
            }

            if (finalizer_target.has_value()) {
                // We manually generate the CFG, because we previously halted before entering it
                generate_cfg_for_block(finalizer_target->block(), executable);
                VERIFY(unwind_frames.last() != &frame);

                // We previously halted execution when we would enter the finalizer,
                // So we now have to visit all possible targets
                // This mainly affects the ScheduleJump instruction
                for (auto const* block : frame.finalizer_targets) {
                    if (block == nullptr) {
                        // This signals a `return`, which we do not handle specially, so we skip
                        continue;
                    }
                    if (!seen_blocks.contains(block))
                        generate_cfg_for_block(*block, executable);
                }
            } else {
                VERIFY(unwind_frames.last() == &frame);
                unwind_frames.take_last();
                VERIFY(frame.finalizer_targets.is_empty());
            }

            return;
        }
        case ContinuePendingUnwind: {
            auto resume_target = static_cast<Op::ContinuePendingUnwind const&>(instruction).resume_target();
            enter_label(resume_target, current_block);
            // Note: We already mark these possible control flow changes further up, but when we get
            //       get better error awareness, being explicit here will be required
            if (auto const* handler = unwind_frames.last()->handler)
                enter_label(Label { *handler }, current_block);
            else if (auto const* finalizer = unwind_frames.last()->finalizer)
                enter_label(Label { *finalizer }, current_block);

            return;
        }
        case Throw:
            // Note: We technically register that we enter the handler in the prelude,
            //       but lets be correct and mark it again,
            //       this will be useful once we have more info on which instruction can
            //       actually fail
            if (auto const* handler = unwind_frames.last()->handler) {
                enter_label(Label { *handler }, current_block);
            } else if (auto const* finalizer = unwind_frames.last()->finalizer) {
                enter_label(Label { *finalizer }, current_block);
                // Note: This error might bubble through the finalizer to the next handler/finalizer,
                //       This is currently marked in the general path
            }
            return;
        case Return:
            if (auto const* finalizer = unwind_frames.last()->finalizer) {
                enter_label(Label { *finalizer }, current_block);
                unwind_frames.last()->finalizer_targets.append(nullptr);
            }
            return;
        case ScheduleJump: {
            enter_label(Label { *unwind_frames.last()->finalizer }, current_block);

            unwind_frames.last()->finalizer_targets.append(
                &static_cast<Op::ScheduleJump const&>(instruction).target().block());
            return;
        }
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
