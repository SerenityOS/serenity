/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/QuickSort.h>
#include <AK/RedBlackTree.h>
#include <AK/Stack.h>
#include <LibRegex/Regex.h>
#include <LibRegex/RegexBytecodeStreamOptimizer.h>
#if REGEX_DEBUG
#    include <AK/ScopeGuard.h>
#    include <AK/ScopeLogger.h>
#endif

namespace regex {

using Detail::Block;

template<typename Parser>
void Regex<Parser>::run_optimization_passes()
{
    parser_result.bytecode.flatten();

    // Rewrite fork loops as atomic groups
    // e.g. a*b -> (ATOMIC a*)b
    attempt_rewrite_loops_as_atomic_groups(split_basic_blocks(parser_result.bytecode));

    parser_result.bytecode.flatten();
}

template<typename Parser>
typename Regex<Parser>::BasicBlockList Regex<Parser>::split_basic_blocks(ByteCode const& bytecode)
{
    BasicBlockList block_boundaries;
    size_t end_of_last_block = 0;

    auto bytecode_size = bytecode.size();

    MatchState state;
    state.instruction_position = 0;
    auto check_jump = [&]<typename T>(OpCode const& opcode) {
        auto& op = static_cast<T const&>(opcode);
        ssize_t jump_offset = op.size() + op.offset();
        if (jump_offset >= 0) {
            block_boundaries.append({ end_of_last_block, state.instruction_position });
            end_of_last_block = state.instruction_position + opcode.size();
        } else {
            // This op jumps back, see if that's within this "block".
            if (jump_offset + state.instruction_position > end_of_last_block) {
                // Split the block!
                block_boundaries.append({ end_of_last_block, jump_offset + state.instruction_position });
                block_boundaries.append({ jump_offset + state.instruction_position, state.instruction_position });
                end_of_last_block = state.instruction_position + opcode.size();
            } else {
                // Nope, it's just a jump to another block
                block_boundaries.append({ end_of_last_block, state.instruction_position });
                end_of_last_block = state.instruction_position + opcode.size();
            }
        }
    };
    for (;;) {
        auto& opcode = bytecode.get_opcode(state);

        switch (opcode.opcode_id()) {
        case OpCodeId::Jump:
            check_jump.template operator()<OpCode_Jump>(opcode);
            break;
        case OpCodeId::JumpNonEmpty:
            check_jump.template operator()<OpCode_JumpNonEmpty>(opcode);
            break;
        case OpCodeId::ForkJump:
            check_jump.template operator()<OpCode_ForkJump>(opcode);
            break;
        case OpCodeId::ForkStay:
            check_jump.template operator()<OpCode_ForkStay>(opcode);
            break;
        case OpCodeId::FailForks:
            block_boundaries.append({ end_of_last_block, state.instruction_position });
            end_of_last_block = state.instruction_position + opcode.size();
            break;
        case OpCodeId::Repeat: {
            // Repeat produces two blocks, one containing its repeated expr, and one after that.
            auto repeat_start = state.instruction_position - static_cast<OpCode_Repeat const&>(opcode).offset();
            if (repeat_start > end_of_last_block)
                block_boundaries.append({ end_of_last_block, repeat_start });
            block_boundaries.append({ repeat_start, state.instruction_position });
            end_of_last_block = state.instruction_position + opcode.size();
            break;
        }
        default:
            break;
        }

        auto next_ip = state.instruction_position + opcode.size();
        if (next_ip < bytecode_size)
            state.instruction_position = next_ip;
        else
            break;
    }

    if (end_of_last_block < bytecode_size)
        block_boundaries.append({ end_of_last_block, bytecode_size });

    quick_sort(block_boundaries, [](auto& a, auto& b) { return a.start < b.start; });

    return block_boundaries;
}

enum class AtomicRewritePreconditionResult {
    SatisfiedWithProperHeader,
    SatisfiedWithEmptyHeader,
    NotSatisfied,
};
static AtomicRewritePreconditionResult block_satisfies_atomic_rewrite_precondition(ByteCode const& bytecode, Block const& repeated_block, Block const& following_block)
{
    Vector<Vector<CompareTypeAndValuePair>> repeated_values;
    HashTable<size_t> active_capture_groups;
    MatchState state;
    for (state.instruction_position = repeated_block.start; state.instruction_position < repeated_block.end;) {
        auto& opcode = bytecode.get_opcode(state);
        switch (opcode.opcode_id()) {
        case OpCodeId::Compare: {
            auto compares = static_cast<OpCode_Compare const&>(opcode).flat_compares();
            if (repeated_values.is_empty() && any_of(compares, [](auto& compare) { return compare.type == CharacterCompareType::AnyChar; }))
                return AtomicRewritePreconditionResult::NotSatisfied;
            repeated_values.append(move(compares));
            break;
        }
        case OpCodeId::CheckBegin:
        case OpCodeId::CheckEnd:
            if (repeated_values.is_empty())
                return AtomicRewritePreconditionResult::SatisfiedWithProperHeader;
            break;
        case OpCodeId::CheckBoundary:
            // FIXME: What should we do with these? for now, let's fail.
            return AtomicRewritePreconditionResult::NotSatisfied;
        case OpCodeId::Restore:
        case OpCodeId::GoBack:
            return AtomicRewritePreconditionResult::NotSatisfied;
        case OpCodeId::SaveRightCaptureGroup:
            active_capture_groups.set(static_cast<OpCode_SaveRightCaptureGroup const&>(opcode).id());
            break;
        case OpCodeId::SaveLeftCaptureGroup:
            active_capture_groups.set(static_cast<OpCode_SaveLeftCaptureGroup const&>(opcode).id());
            break;
        default:
            break;
        }

        state.instruction_position += opcode.size();
    }
    dbgln_if(REGEX_DEBUG, "Found {} entries in reference", repeated_values.size());
    dbgln_if(REGEX_DEBUG, "Found {} active capture groups", active_capture_groups.size());

    bool following_block_has_at_least_one_compare = false;
    // Find the first compare in the following block, it must NOT match any of the values in `repeated_values'.
    for (state.instruction_position = following_block.start; state.instruction_position < following_block.end;) {
        auto& opcode = bytecode.get_opcode(state);
        switch (opcode.opcode_id()) {
        // Note: These have to exist since we're effectively repeating the following block as well
        case OpCodeId::SaveRightCaptureGroup:
            active_capture_groups.set(static_cast<OpCode_SaveRightCaptureGroup const&>(opcode).id());
            break;
        case OpCodeId::SaveLeftCaptureGroup:
            active_capture_groups.set(static_cast<OpCode_SaveLeftCaptureGroup const&>(opcode).id());
            break;
        case OpCodeId::Compare: {
            following_block_has_at_least_one_compare = true;
            // We found a compare, let's see what it has.
            auto compares = static_cast<OpCode_Compare const&>(opcode).flat_compares();
            if (compares.is_empty())
                break;

            if (any_of(compares, [&](auto& compare) {
                    return compare.type == CharacterCompareType::AnyChar
                        || (compare.type == CharacterCompareType::Reference && active_capture_groups.contains(compare.value));
                }))
                return AtomicRewritePreconditionResult::NotSatisfied;

            for (auto& repeated_value : repeated_values) {
                // FIXME: This is too naive!
                if (any_of(repeated_value, [](auto& compare) { return compare.type == CharacterCompareType::AnyChar; }))
                    return AtomicRewritePreconditionResult::NotSatisfied;

                for (auto& repeated_compare : repeated_value) {
                    // FIXME: This is too naive! it will miss _tons_ of cases since it doesn't check ranges!
                    if (any_of(compares, [&](auto& compare) { return compare.type == repeated_compare.type && compare.value == repeated_compare.value; }))
                        return AtomicRewritePreconditionResult::NotSatisfied;
                }
            }
            return AtomicRewritePreconditionResult::SatisfiedWithProperHeader;
        }
        case OpCodeId::CheckBegin:
        case OpCodeId::CheckEnd:
            return AtomicRewritePreconditionResult::SatisfiedWithProperHeader; // Nothing can match the end!
        case OpCodeId::CheckBoundary:
            // FIXME: What should we do with these? For now, consider them a failure.
            return AtomicRewritePreconditionResult::NotSatisfied;
        default:
            break;
        }

        state.instruction_position += opcode.size();
    }

    if (following_block_has_at_least_one_compare)
        return AtomicRewritePreconditionResult::SatisfiedWithProperHeader;
    return AtomicRewritePreconditionResult::SatisfiedWithEmptyHeader;
}

template<typename Parser>
void Regex<Parser>::attempt_rewrite_loops_as_atomic_groups(BasicBlockList const& basic_blocks)
{
    auto& bytecode = parser_result.bytecode;
    if constexpr (REGEX_DEBUG) {
        RegexDebug dbg;
        dbg.print_bytecode(*this);
        for (auto const& block : basic_blocks)
            dbgln("block from {} to {}", block.start, block.end);
    }

    // A pattern such as:
    //     bb0       |  RE0
    //               |  ForkX bb0
    //     -------------------------
    //     bb1       |  RE1
    // can be rewritten as:
    //     -------------------------
    //     bb0       | RE0
    //               | ForkReplaceX bb0
    //     -------------------------
    //     bb1       | RE1
    // provided that first(RE1) not-in end(RE0), which is to say
    // that RE1 cannot start with whatever RE0 has matched (ever).
    //
    // Alternatively, a second form of this pattern can also occur:
    //     bb0 | *
    //         | ForkX bb2
    //     ------------------------
    //     bb1 | RE0
    //         | Jump bb0
    //     ------------------------
    //     bb2 | RE1
    // which can be transformed (with the same preconditions) to:
    //     bb0 | *
    //         | ForkReplaceX bb2
    //     ------------------------
    //     bb1 | RE0
    //         | Jump bb0
    //     ------------------------
    //     bb2 | RE1

    enum class AlternateForm {
        DirectLoopWithoutHeader,               // loop without proper header, a block forking to itself. i.e. the first form.
        DirectLoopWithoutHeaderAndEmptyFollow, // loop without proper header, a block forking to itself. i.e. the first form but with RE1 being empty.
        DirectLoopWithHeader,                  // loop with proper header, i.e. the second form.
    };
    struct CandidateBlock {
        Block forking_block;
        Optional<Block> new_target_block;
        AlternateForm form;
    };
    Vector<CandidateBlock> candidate_blocks;

    auto is_an_eligible_jump = [](OpCode const& opcode, size_t ip, size_t block_start, AlternateForm alternate_form) {
        switch (opcode.opcode_id()) {
        case OpCodeId::JumpNonEmpty: {
            auto const& op = static_cast<OpCode_JumpNonEmpty const&>(opcode);
            auto form = op.form();
            if (form != OpCodeId::Jump && alternate_form == AlternateForm::DirectLoopWithHeader)
                return false;
            if (form != OpCodeId::ForkJump && form != OpCodeId::ForkStay && alternate_form == AlternateForm::DirectLoopWithoutHeader)
                return false;
            return op.offset() + ip + opcode.size() == block_start;
        }
        case OpCodeId::ForkJump:
            if (alternate_form == AlternateForm::DirectLoopWithHeader)
                return false;
            return static_cast<OpCode_ForkJump const&>(opcode).offset() + ip + opcode.size() == block_start;
        case OpCodeId::ForkStay:
            if (alternate_form == AlternateForm::DirectLoopWithHeader)
                return false;
            return static_cast<OpCode_ForkStay const&>(opcode).offset() + ip + opcode.size() == block_start;
        case OpCodeId::Jump:
            // Infinite loop does *not* produce forks.
            if (alternate_form == AlternateForm::DirectLoopWithoutHeader)
                return false;
            if (alternate_form == AlternateForm::DirectLoopWithHeader)
                return static_cast<OpCode_Jump const&>(opcode).offset() + ip + opcode.size() == block_start;
            VERIFY_NOT_REACHED();
        default:
            return false;
        }
    };
    for (size_t i = 0; i < basic_blocks.size(); ++i) {
        auto forking_block = basic_blocks[i];
        Optional<Block> fork_fallback_block;
        if (i + 1 < basic_blocks.size())
            fork_fallback_block = basic_blocks[i + 1];
        MatchState state;
        // Check if the last instruction in this block is a jump to the block itself:
        {
            state.instruction_position = forking_block.end;
            auto& opcode = bytecode.get_opcode(state);
            if (is_an_eligible_jump(opcode, state.instruction_position, forking_block.start, AlternateForm::DirectLoopWithoutHeader)) {
                // We've found RE0 (and RE1 is just the following block, if any), let's see if the precondition applies.
                // if RE1 is empty, there's no first(RE1), so this is an automatic pass.
                if (!fork_fallback_block.has_value() || fork_fallback_block->end == fork_fallback_block->start) {
                    candidate_blocks.append({ forking_block, fork_fallback_block, AlternateForm::DirectLoopWithoutHeader });
                    break;
                }

                auto precondition = block_satisfies_atomic_rewrite_precondition(bytecode, forking_block, *fork_fallback_block);
                if (precondition == AtomicRewritePreconditionResult::SatisfiedWithProperHeader) {
                    candidate_blocks.append({ forking_block, fork_fallback_block, AlternateForm::DirectLoopWithoutHeader });
                    break;
                }
                if (precondition == AtomicRewritePreconditionResult::SatisfiedWithEmptyHeader) {
                    candidate_blocks.append({ forking_block, fork_fallback_block, AlternateForm::DirectLoopWithoutHeaderAndEmptyFollow });
                    break;
                }
            }
        }
        // Check if the last instruction in the last block is a direct jump to this block
        if (fork_fallback_block.has_value()) {
            state.instruction_position = fork_fallback_block->end;
            auto& opcode = bytecode.get_opcode(state);
            if (is_an_eligible_jump(opcode, state.instruction_position, forking_block.start, AlternateForm::DirectLoopWithHeader)) {
                // We've found bb1 and bb0, let's just make sure that bb0 forks to bb2.
                state.instruction_position = forking_block.end;
                auto& opcode = bytecode.get_opcode(state);
                if (opcode.opcode_id() == OpCodeId::ForkJump || opcode.opcode_id() == OpCodeId::ForkStay) {
                    Optional<Block> block_following_fork_fallback;
                    if (i + 2 < basic_blocks.size())
                        block_following_fork_fallback = basic_blocks[i + 2];
                    if (!block_following_fork_fallback.has_value()
                        || block_satisfies_atomic_rewrite_precondition(bytecode, *fork_fallback_block, *block_following_fork_fallback) != AtomicRewritePreconditionResult::NotSatisfied) {
                        candidate_blocks.append({ forking_block, {}, AlternateForm::DirectLoopWithHeader });
                        break;
                    }
                }
            }
        }
    }

    dbgln_if(REGEX_DEBUG, "Found {} candidate blocks", candidate_blocks.size());
    if (candidate_blocks.is_empty()) {
        dbgln_if(REGEX_DEBUG, "Failed to find anything for {}", pattern_value);
        return;
    }

    RedBlackTree<size_t, size_t> needed_patches;

    // Reverse the blocks, so we can patch the bytecode without messing with the latter patches.
    quick_sort(candidate_blocks, [](auto& a, auto& b) { return b.forking_block.start > a.forking_block.start; });
    for (auto& candidate : candidate_blocks) {
        // Note that both forms share a ForkReplace patch in forking_block.
        // Patch the ForkX in forking_block to be a ForkReplaceX instead.
        auto& opcode_id = bytecode[candidate.forking_block.end];
        if (opcode_id == (ByteCodeValueType)OpCodeId::ForkStay) {
            opcode_id = (ByteCodeValueType)OpCodeId::ForkReplaceStay;
        } else if (opcode_id == (ByteCodeValueType)OpCodeId::ForkJump) {
            opcode_id = (ByteCodeValueType)OpCodeId::ForkReplaceJump;
        } else if (opcode_id == (ByteCodeValueType)OpCodeId::JumpNonEmpty) {
            auto& jump_opcode_id = bytecode[candidate.forking_block.end + 3];
            if (jump_opcode_id == (ByteCodeValueType)OpCodeId::ForkStay)
                jump_opcode_id = (ByteCodeValueType)OpCodeId::ForkReplaceStay;
            else if (jump_opcode_id == (ByteCodeValueType)OpCodeId::ForkJump)
                jump_opcode_id = (ByteCodeValueType)OpCodeId::ForkReplaceJump;
            else
                VERIFY_NOT_REACHED();
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    if (!needed_patches.is_empty()) {
        MatchState state;
        auto bytecode_size = bytecode.size();
        state.instruction_position = 0;
        struct Patch {
            ssize_t value;
            size_t offset;
            bool should_negate { false };
        };
        for (;;) {
            if (state.instruction_position >= bytecode_size)
                break;

            auto& opcode = bytecode.get_opcode(state);
            Stack<Patch, 2> patch_points;

            switch (opcode.opcode_id()) {
            case OpCodeId::Jump:
                patch_points.push({ static_cast<OpCode_Jump const&>(opcode).offset(), state.instruction_position + 1 });
                break;
            case OpCodeId::JumpNonEmpty:
                patch_points.push({ static_cast<OpCode_JumpNonEmpty const&>(opcode).offset(), state.instruction_position + 1 });
                patch_points.push({ static_cast<OpCode_JumpNonEmpty const&>(opcode).checkpoint(), state.instruction_position + 2 });
                break;
            case OpCodeId::ForkJump:
                patch_points.push({ static_cast<OpCode_ForkJump const&>(opcode).offset(), state.instruction_position + 1 });
                break;
            case OpCodeId::ForkStay:
                patch_points.push({ static_cast<OpCode_ForkStay const&>(opcode).offset(), state.instruction_position + 1 });
                break;
            case OpCodeId::Repeat:
                patch_points.push({ -(ssize_t) static_cast<OpCode_Repeat const&>(opcode).offset(), state.instruction_position + 1, true });
                break;
            default:
                break;
            }

            while (!patch_points.is_empty()) {
                auto& patch_point = patch_points.top();
                auto target_offset = patch_point.value + state.instruction_position + opcode.size();

                constexpr auto do_patch = [](auto& patch_it, auto& patch_point, auto& target_offset, auto& bytecode, auto ip) {
                    if (patch_it.key() == ip)
                        return;

                    if (patch_point.value < 0 && target_offset <= patch_it.key() && ip > patch_it.key())
                        bytecode[patch_point.offset] += (patch_point.should_negate ? 1 : -1) * (*patch_it);
                    else if (patch_point.value > 0 && target_offset >= patch_it.key() && ip < patch_it.key())
                        bytecode[patch_point.offset] += (patch_point.should_negate ? -1 : 1) * (*patch_it);
                };

                if (auto patch_it = needed_patches.find_largest_not_above_iterator(target_offset); !patch_it.is_end())
                    do_patch(patch_it, patch_point, target_offset, bytecode, state.instruction_position);
                else if (auto patch_it = needed_patches.find_largest_not_above_iterator(state.instruction_position); !patch_it.is_end())
                    do_patch(patch_it, patch_point, target_offset, bytecode, state.instruction_position);

                patch_points.pop();
            }

            state.instruction_position += opcode.size();
        }
    }

    if constexpr (REGEX_DEBUG) {
        warnln("Transformed to:");
        RegexDebug dbg;
        dbg.print_bytecode(*this);
    }
}

void Optimizer::append_alternation(ByteCode& target, ByteCode&& left, ByteCode&& right)
{
    Array<ByteCode, 2> alternatives;
    alternatives[0] = move(left);
    alternatives[1] = move(right);

    append_alternation(target, alternatives);
}

void Optimizer::append_alternation(ByteCode& target, Span<ByteCode> alternatives)
{
    if (alternatives.size() == 0)
        return;

    if (alternatives.size() == 1)
        return target.extend(move(alternatives[0]));

    if (all_of(alternatives, [](auto& x) { return x.is_empty(); }))
        return;

    for (auto& entry : alternatives)
        entry.flatten();

#if REGEX_DEBUG
    ScopeLogger<true> log;
    warnln("Alternations:");
    RegexDebug dbg;
    for (auto& entry : alternatives) {
        warnln("----------");
        dbg.print_bytecode(entry);
    }
    ScopeGuard print_at_end {
        [&] {
            warnln("======================");
            RegexDebug dbg;
            dbg.print_bytecode(target);
        }
    };
#endif

    Vector<Vector<Detail::Block>> basic_blocks;
    basic_blocks.ensure_capacity(alternatives.size());

    for (auto& entry : alternatives)
        basic_blocks.append(Regex<PosixBasicParser>::split_basic_blocks(entry));

    size_t left_skip = 0;
    size_t shared_block_count = basic_blocks.first().size();
    for (auto& entry : basic_blocks)
        shared_block_count = min(shared_block_count, entry.size());

    MatchState state;
    for (size_t block_index = 0; block_index < shared_block_count; block_index++) {
        auto& left_block = basic_blocks.first()[block_index];
        auto left_end = block_index + 1 == basic_blocks.first().size() ? left_block.end : basic_blocks.first()[block_index + 1].start;
        auto can_continue = true;
        for (size_t i = 1; i < alternatives.size(); ++i) {
            auto& right_blocks = basic_blocks[i];
            auto& right_block = right_blocks[block_index];
            auto right_end = block_index + 1 == right_blocks.size() ? right_block.end : right_blocks[block_index + 1].start;

            if (left_end - left_block.start != right_end - right_block.start) {
                can_continue = false;
                break;
            }

            if (alternatives[0].spans().slice(left_block.start, left_end - left_block.start) != alternatives[i].spans().slice(right_block.start, right_end - right_block.start)) {
                can_continue = false;
                break;
            }
        }
        if (!can_continue)
            break;

        size_t i = 0;
        for (auto& entry : alternatives) {
            auto& blocks = basic_blocks[i];
            auto& block = blocks[block_index];
            auto end = block_index + 1 == blocks.size() ? block.end : blocks[block_index + 1].start;
            state.instruction_position = block.start;
            size_t skip = 0;
            while (state.instruction_position < end) {
                auto& opcode = entry.get_opcode(state);
                state.instruction_position += opcode.size();
                skip = state.instruction_position;
            }
            left_skip = min(skip, left_skip);
        }
    }

    dbgln_if(REGEX_DEBUG, "Skipping {}/{} bytecode entries from {}", left_skip, 0, alternatives[0].size());

    if (left_skip > 0) {
        target.extend(alternatives[0].release_slice(basic_blocks.first().first().start, left_skip));
        auto first = true;
        for (auto& entry : alternatives) {
            if (first) {
                first = false;
                continue;
            }
            entry = entry.release_slice(left_skip);
        }
    }

    if (all_of(alternatives, [](auto& entry) { return entry.is_empty(); }))
        return;

    size_t patch_start = target.size();
    for (size_t i = 1; i < alternatives.size(); ++i) {
        target.empend(static_cast<ByteCodeValueType>(OpCodeId::ForkJump));
        target.empend(0u); // To be filled later.
    }

    size_t size_to_jump = 0;
    bool seen_one_empty = false;
    for (size_t i = alternatives.size(); i > 0; --i) {
        auto& entry = alternatives[i - 1];
        if (entry.is_empty()) {
            if (seen_one_empty)
                continue;
            seen_one_empty = true;
        }

        auto is_first = i == 1;
        auto instruction_size = entry.size() + (is_first ? 0 : 2); // Jump; -> +2
        size_to_jump += instruction_size;

        if (!is_first)
            target[patch_start + (i - 2) * 2 + 1] = size_to_jump + (alternatives.size() - i) * 2;

        dbgln_if(REGEX_DEBUG, "{} size = {}, cum={}", i - 1, instruction_size, size_to_jump);
    }

    seen_one_empty = false;
    for (size_t i = alternatives.size(); i > 0; --i) {
        auto& chunk = alternatives[i - 1];
        if (chunk.is_empty()) {
            if (seen_one_empty)
                continue;
            seen_one_empty = true;
        }

        ByteCode* previous_chunk = nullptr;
        size_t j = i - 1;
        auto seen_one_empty_before = chunk.is_empty();
        while (j >= 1) {
            --j;
            auto& candidate_chunk = alternatives[j];
            if (candidate_chunk.is_empty()) {
                if (seen_one_empty_before)
                    continue;
            }
            previous_chunk = &candidate_chunk;
            break;
        }

        size_to_jump -= chunk.size() + (previous_chunk ? 2 : 0);

        target.extend(move(chunk));
        target.empend(static_cast<ByteCodeValueType>(OpCodeId::Jump));
        target.empend(size_to_jump); // Jump to the _END label
    }
}

enum class LookupTableInsertionOutcome {
    Successful,
    ReplaceWithAnyChar,
    TemporaryInversionNeeded,
    PermanentInversionNeeded,
    CannotPlaceInTable,
};
static LookupTableInsertionOutcome insert_into_lookup_table(RedBlackTree<ByteCodeValueType, CharRange>& table, CompareTypeAndValuePair pair)
{
    switch (pair.type) {
    case CharacterCompareType::Inverse:
        return LookupTableInsertionOutcome::PermanentInversionNeeded;
    case CharacterCompareType::TemporaryInverse:
        return LookupTableInsertionOutcome::TemporaryInversionNeeded;
    case CharacterCompareType::AnyChar:
        return LookupTableInsertionOutcome::ReplaceWithAnyChar;
    case CharacterCompareType::CharClass:
        return LookupTableInsertionOutcome::CannotPlaceInTable;
    case CharacterCompareType::Char:
        table.insert(pair.value, { (u32)pair.value, (u32)pair.value });
        break;
    case CharacterCompareType::CharRange: {
        CharRange range { pair.value };
        table.insert(range.from, range);
        break;
    }
    case CharacterCompareType::Reference:
    case CharacterCompareType::Property:
    case CharacterCompareType::GeneralCategory:
    case CharacterCompareType::Script:
    case CharacterCompareType::ScriptExtension:
        return LookupTableInsertionOutcome::CannotPlaceInTable;
    case CharacterCompareType::Undefined:
    case CharacterCompareType::RangeExpressionDummy:
    case CharacterCompareType::String:
    case CharacterCompareType::LookupTable:
        VERIFY_NOT_REACHED();
    }

    return LookupTableInsertionOutcome::Successful;
}

void Optimizer::append_character_class(ByteCode& target, Vector<CompareTypeAndValuePair>&& pairs)
{
    ByteCode arguments;
    size_t argument_count = 0;

    if (pairs.size() <= 1) {
        for (auto& pair : pairs) {
            arguments.append(to_underlying(pair.type));
            if (pair.type != CharacterCompareType::AnyChar && pair.type != CharacterCompareType::TemporaryInverse && pair.type != CharacterCompareType::Inverse)
                arguments.append(pair.value);
            ++argument_count;
        }
    } else {
        RedBlackTree<ByteCodeValueType, CharRange> table;
        RedBlackTree<ByteCodeValueType, CharRange> inverted_table;
        auto* current_table = &table;
        auto* current_inverted_table = &inverted_table;
        bool invert_for_next_iteration = false;
        bool is_currently_inverted = false;

        for (auto& value : pairs) {
            auto should_invert_after_this_iteration = invert_for_next_iteration;
            invert_for_next_iteration = false;

            auto insertion_result = insert_into_lookup_table(*current_table, value);
            switch (insertion_result) {
            case LookupTableInsertionOutcome::Successful:
                break;
            case LookupTableInsertionOutcome::ReplaceWithAnyChar: {
                table.clear();
                inverted_table.clear();
                arguments.append(to_underlying(CharacterCompareType::AnyChar));
                ++argument_count;
                break;
            }
            case LookupTableInsertionOutcome::TemporaryInversionNeeded:
                swap(current_table, current_inverted_table);
                invert_for_next_iteration = true;
                is_currently_inverted = !is_currently_inverted;
                break;
            case LookupTableInsertionOutcome::PermanentInversionNeeded:
                swap(current_table, current_inverted_table);
                is_currently_inverted = !is_currently_inverted;
                break;
            case LookupTableInsertionOutcome::CannotPlaceInTable:
                if (is_currently_inverted) {
                    arguments.append(to_underlying(CharacterCompareType::TemporaryInverse));
                    ++argument_count;
                }
                arguments.append(to_underlying(value.type));
                arguments.append(value.value);
                ++argument_count;
                break;
            }

            if (should_invert_after_this_iteration) {
                swap(current_table, current_inverted_table);
                is_currently_inverted = !is_currently_inverted;
            }
        }
        auto append_table = [&](auto& table) {
            ++argument_count;
            arguments.append(to_underlying(CharacterCompareType::LookupTable));
            auto size_index = arguments.size();
            arguments.append(0);
            Optional<CharRange> active_range;
            size_t range_count = 0;
            for (auto& range : table) {
                if (!active_range.has_value()) {
                    active_range = range;
                    continue;
                }

                if (range.from <= active_range->to + 1 && range.to + 1 >= active_range->from) {
                    active_range = CharRange { min(range.from, active_range->from), max(range.to, active_range->to) };
                } else {
                    ++range_count;
                    arguments.append(active_range.release_value());
                    active_range = range;
                }
            }
            if (active_range.has_value()) {
                ++range_count;
                arguments.append(active_range.release_value());
            }
            arguments[size_index] = range_count;
        };

        if (!table.is_empty())
            append_table(table);

        if (!inverted_table.is_empty()) {
            ++argument_count;
            arguments.append(to_underlying(CharacterCompareType::TemporaryInverse));
            append_table(inverted_table);
        }
    }

    target.empend(static_cast<ByteCodeValueType>(OpCodeId::Compare));
    target.empend(argument_count);   // number of arguments
    target.empend(arguments.size()); // size of arguments
    target.extend(move(arguments));
}

template void Regex<PosixBasicParser>::run_optimization_passes();
template void Regex<PosixExtendedParser>::run_optimization_passes();
template void Regex<ECMA262Parser>::run_optimization_passes();
}
