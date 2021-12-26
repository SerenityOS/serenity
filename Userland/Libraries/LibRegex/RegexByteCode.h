/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "RegexBytecodeStreamOptimizer.h"
#include "RegexMatch.h"
#include "RegexOptions.h"

#include <AK/DisjointChunks.h>
#include <AK/Format.h>
#include <AK/Forward.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <AK/Traits.h>
#include <AK/TypeCasts.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibUnicode/Forward.h>

namespace regex {

using ByteCodeValueType = u64;

#define ENUMERATE_OPCODES                          \
    __ENUMERATE_OPCODE(Compare)                    \
    __ENUMERATE_OPCODE(Jump)                       \
    __ENUMERATE_OPCODE(JumpNonEmpty)               \
    __ENUMERATE_OPCODE(ForkJump)                   \
    __ENUMERATE_OPCODE(ForkStay)                   \
    __ENUMERATE_OPCODE(ForkReplaceJump)            \
    __ENUMERATE_OPCODE(ForkReplaceStay)            \
    __ENUMERATE_OPCODE(FailForks)                  \
    __ENUMERATE_OPCODE(SaveLeftCaptureGroup)       \
    __ENUMERATE_OPCODE(SaveRightCaptureGroup)      \
    __ENUMERATE_OPCODE(SaveRightNamedCaptureGroup) \
    __ENUMERATE_OPCODE(CheckBegin)                 \
    __ENUMERATE_OPCODE(CheckEnd)                   \
    __ENUMERATE_OPCODE(CheckBoundary)              \
    __ENUMERATE_OPCODE(Save)                       \
    __ENUMERATE_OPCODE(Restore)                    \
    __ENUMERATE_OPCODE(GoBack)                     \
    __ENUMERATE_OPCODE(ClearCaptureGroup)          \
    __ENUMERATE_OPCODE(Repeat)                     \
    __ENUMERATE_OPCODE(ResetRepeat)                \
    __ENUMERATE_OPCODE(Checkpoint)                 \
    __ENUMERATE_OPCODE(Exit)

// clang-format off
enum class OpCodeId : ByteCodeValueType {
#define __ENUMERATE_OPCODE(x) x,
    ENUMERATE_OPCODES
#undef __ENUMERATE_OPCODE

    First = Compare,
    Last = Exit,
};
// clang-format on

#define ENUMERATE_CHARACTER_COMPARE_TYPES                    \
    __ENUMERATE_CHARACTER_COMPARE_TYPE(Undefined)            \
    __ENUMERATE_CHARACTER_COMPARE_TYPE(Inverse)              \
    __ENUMERATE_CHARACTER_COMPARE_TYPE(TemporaryInverse)     \
    __ENUMERATE_CHARACTER_COMPARE_TYPE(AnyChar)              \
    __ENUMERATE_CHARACTER_COMPARE_TYPE(Char)                 \
    __ENUMERATE_CHARACTER_COMPARE_TYPE(String)               \
    __ENUMERATE_CHARACTER_COMPARE_TYPE(CharClass)            \
    __ENUMERATE_CHARACTER_COMPARE_TYPE(CharRange)            \
    __ENUMERATE_CHARACTER_COMPARE_TYPE(Reference)            \
    __ENUMERATE_CHARACTER_COMPARE_TYPE(Property)             \
    __ENUMERATE_CHARACTER_COMPARE_TYPE(GeneralCategory)      \
    __ENUMERATE_CHARACTER_COMPARE_TYPE(Script)               \
    __ENUMERATE_CHARACTER_COMPARE_TYPE(ScriptExtension)      \
    __ENUMERATE_CHARACTER_COMPARE_TYPE(RangeExpressionDummy) \
    __ENUMERATE_CHARACTER_COMPARE_TYPE(LookupTable)

enum class CharacterCompareType : ByteCodeValueType {
#define __ENUMERATE_CHARACTER_COMPARE_TYPE(x) x,
    ENUMERATE_CHARACTER_COMPARE_TYPES
#undef __ENUMERATE_CHARACTER_COMPARE_TYPE
};

#define ENUMERATE_CHARACTER_CLASSES    \
    __ENUMERATE_CHARACTER_CLASS(Alnum) \
    __ENUMERATE_CHARACTER_CLASS(Cntrl) \
    __ENUMERATE_CHARACTER_CLASS(Lower) \
    __ENUMERATE_CHARACTER_CLASS(Space) \
    __ENUMERATE_CHARACTER_CLASS(Alpha) \
    __ENUMERATE_CHARACTER_CLASS(Digit) \
    __ENUMERATE_CHARACTER_CLASS(Print) \
    __ENUMERATE_CHARACTER_CLASS(Upper) \
    __ENUMERATE_CHARACTER_CLASS(Blank) \
    __ENUMERATE_CHARACTER_CLASS(Graph) \
    __ENUMERATE_CHARACTER_CLASS(Punct) \
    __ENUMERATE_CHARACTER_CLASS(Word)  \
    __ENUMERATE_CHARACTER_CLASS(Xdigit)

enum class CharClass : ByteCodeValueType {
#define __ENUMERATE_CHARACTER_CLASS(x) x,
    ENUMERATE_CHARACTER_CLASSES
#undef __ENUMERATE_CHARACTER_CLASS
};

#define ENUMERATE_BOUNDARY_CHECK_TYPES    \
    __ENUMERATE_BOUNDARY_CHECK_TYPE(Word) \
    __ENUMERATE_BOUNDARY_CHECK_TYPE(NonWord)

enum class BoundaryCheckType : ByteCodeValueType {
#define __ENUMERATE_BOUNDARY_CHECK_TYPE(x) x,
    ENUMERATE_BOUNDARY_CHECK_TYPES
#undef __ENUMERATE_BOUNDARY_CHECK_TYPE
};

struct CharRange {
    u32 const from;
    u32 const to;

    CharRange(u64 value)
        : from(value >> 32)
        , to(value & 0xffffffff)
    {
    }

    CharRange(u32 from, u32 to)
        : from(from)
        , to(to)
    {
    }

    operator ByteCodeValueType() const { return ((u64)from << 32) | to; }
};

struct CompareTypeAndValuePair {
    CharacterCompareType type;
    ByteCodeValueType value;
};

class OpCode;

class ByteCode : public DisjointChunks<ByteCodeValueType> {
    using Base = DisjointChunks<ByteCodeValueType>;

public:
    ByteCode()
    {
        ensure_opcodes_initialized();
    }

    ByteCode(ByteCode const&) = default;
    virtual ~ByteCode() = default;

    ByteCode& operator=(ByteCode&&) = default;
    ByteCode& operator=(Base&& value)
    {
        static_cast<Base&>(*this) = move(value);
        return *this;
    }

    template<typename... Args>
    void empend(Args&&... args)
    {
        if (is_empty())
            Base::append({});
        Base::last_chunk().empend(forward<Args>(args)...);
    }
    template<typename T>
    void append(T&& value)
    {
        if (is_empty())
            Base::append({});
        Base::last_chunk().append(forward<T>(value));
    }
    template<typename T>
    void prepend(T&& value)
    {
        if (is_empty())
            return append(forward<T>(value));
        Base::first_chunk().prepend(forward<T>(value));
    }

    void last_chunk() const = delete;
    void first_chunk() const = delete;

    void insert_bytecode_compare_values(Vector<CompareTypeAndValuePair>&& pairs)
    {
        Optimizer::append_character_class(*this, move(pairs));
    }

    void insert_bytecode_check_boundary(BoundaryCheckType type)
    {
        ByteCode bytecode;
        bytecode.empend((ByteCodeValueType)OpCodeId::CheckBoundary);
        bytecode.empend((ByteCodeValueType)type);

        extend(move(bytecode));
    }

    void insert_bytecode_clear_capture_group(size_t index)
    {
        empend(static_cast<ByteCodeValueType>(OpCodeId::ClearCaptureGroup));
        empend(index);
    }

    void insert_bytecode_compare_string(StringView view)
    {
        ByteCode bytecode;

        bytecode.empend(static_cast<ByteCodeValueType>(OpCodeId::Compare));
        bytecode.empend(static_cast<u64>(1)); // number of arguments

        ByteCode arguments;

        arguments.empend(static_cast<ByteCodeValueType>(CharacterCompareType::String));
        arguments.insert_string(view);

        bytecode.empend(arguments.size()); // size of arguments
        bytecode.extend(move(arguments));

        extend(move(bytecode));
    }

    void insert_bytecode_group_capture_left(size_t capture_groups_count)
    {
        empend(static_cast<ByteCodeValueType>(OpCodeId::SaveLeftCaptureGroup));
        empend(capture_groups_count);
    }

    void insert_bytecode_group_capture_right(size_t capture_groups_count)
    {
        empend(static_cast<ByteCodeValueType>(OpCodeId::SaveRightCaptureGroup));
        empend(capture_groups_count);
    }

    void insert_bytecode_group_capture_right(size_t capture_groups_count, StringView name)
    {
        empend(static_cast<ByteCodeValueType>(OpCodeId::SaveRightNamedCaptureGroup));
        empend(reinterpret_cast<ByteCodeValueType>(name.characters_without_null_termination()));
        empend(name.length());
        empend(capture_groups_count);
    }

    enum class LookAroundType {
        LookAhead,
        LookBehind,
        NegatedLookAhead,
        NegatedLookBehind,
    };
    void insert_bytecode_lookaround(ByteCode&& lookaround_body, LookAroundType type, size_t match_length = 0)
    {
        // FIXME: The save stack will grow infinitely with repeated failures
        //        as we do not discard that on failure (we don't necessarily know how many to pop with the current architecture).
        switch (type) {
        case LookAroundType::LookAhead: {
            // SAVE
            // REGEXP BODY
            // RESTORE
            empend((ByteCodeValueType)OpCodeId::Save);
            extend(move(lookaround_body));
            empend((ByteCodeValueType)OpCodeId::Restore);
            return;
        }
        case LookAroundType::NegatedLookAhead: {
            // JUMP _A
            // LABEL _L
            // REGEXP BODY
            // FAIL
            // LABEL _A
            // SAVE
            // FORKJUMP _L
            // RESTORE
            auto body_length = lookaround_body.size();
            empend((ByteCodeValueType)OpCodeId::Jump);
            empend((ByteCodeValueType)body_length + 1); // JUMP to label _A
            extend(move(lookaround_body));
            empend((ByteCodeValueType)OpCodeId::FailForks);
            empend((ByteCodeValueType)OpCodeId::Save);
            empend((ByteCodeValueType)OpCodeId::ForkJump);
            empend((ByteCodeValueType) - (body_length + 4)); // JUMP to label _L
            empend((ByteCodeValueType)OpCodeId::Restore);
            return;
        }
        case LookAroundType::LookBehind:
            // SAVE
            // GOBACK match_length(BODY)
            // REGEXP BODY
            // RESTORE
            empend((ByteCodeValueType)OpCodeId::Save);
            empend((ByteCodeValueType)OpCodeId::GoBack);
            empend((ByteCodeValueType)match_length);
            extend(move(lookaround_body));
            empend((ByteCodeValueType)OpCodeId::Restore);
            return;
        case LookAroundType::NegatedLookBehind: {
            // JUMP _A
            // LABEL _L
            // GOBACK match_length(BODY)
            // REGEXP BODY
            // FAIL
            // LABEL _A
            // SAVE
            // FORKJUMP _L
            // RESTORE
            auto body_length = lookaround_body.size();
            empend((ByteCodeValueType)OpCodeId::Jump);
            empend((ByteCodeValueType)body_length + 3); // JUMP to label _A
            empend((ByteCodeValueType)OpCodeId::GoBack);
            empend((ByteCodeValueType)match_length);
            extend(move(lookaround_body));
            empend((ByteCodeValueType)OpCodeId::FailForks);
            empend((ByteCodeValueType)OpCodeId::Save);
            empend((ByteCodeValueType)OpCodeId::ForkJump);
            empend((ByteCodeValueType) - (body_length + 6)); // JUMP to label _L
            empend((ByteCodeValueType)OpCodeId::Restore);
            return;
        }
        }

        VERIFY_NOT_REACHED();
    }

    void insert_bytecode_alternation(ByteCode&& left, ByteCode&& right)
    {

        // FORKJUMP _ALT
        // REGEXP ALT2
        // JUMP  _END
        // LABEL _ALT
        // REGEXP ALT1
        // LABEL _END

        // Optimisation: Eliminate extra work by unifying common pre-and-postfix exprs.
        Optimizer::append_alternation(*this, move(left), move(right));
    }

    template<typename T>
    static void transform_bytecode_repetition_min_max(ByteCode& bytecode_to_repeat, T minimum, Optional<T> maximum, size_t min_repetition_mark_id, size_t max_repetition_mark_id, bool greedy = true) requires(IsIntegral<T>)
    {
        if (!maximum.has_value()) {
            if (minimum == 0)
                return transform_bytecode_repetition_any(bytecode_to_repeat, greedy);
            if (minimum == 1)
                return transform_bytecode_repetition_min_one(bytecode_to_repeat, greedy);
        }

        ByteCode new_bytecode;
        new_bytecode.insert_bytecode_repetition_n(bytecode_to_repeat, minimum, min_repetition_mark_id);

        if (maximum.has_value()) {
            // (REPEAT REGEXP MIN)
            // LABEL _MAX_LOOP            |
            // FORK END                   |
            // REGEXP                     |
            // REPEAT _MAX_LOOP MAX-MIN   | if max > min
            // FORK END                   |
            // REGEXP                     |
            // LABEL END                  |
            // RESET _MAX_LOOP            |
            auto jump_kind = static_cast<ByteCodeValueType>(greedy ? OpCodeId::ForkStay : OpCodeId::ForkJump);
            if (maximum.value() > minimum) {
                new_bytecode.empend(jump_kind);
                new_bytecode.empend((ByteCodeValueType)0); // Placeholder for the jump target.
                auto pre_loop_fork_jump_index = new_bytecode.size();
                new_bytecode.extend(bytecode_to_repeat);
                auto repetitions = maximum.value() - minimum;
                auto fork_jump_address = new_bytecode.size();
                if (repetitions > 1) {
                    new_bytecode.empend((ByteCodeValueType)OpCodeId::Repeat);
                    new_bytecode.empend(bytecode_to_repeat.size() + 2);
                    new_bytecode.empend(static_cast<ByteCodeValueType>(repetitions - 1));
                    new_bytecode.empend(max_repetition_mark_id);
                    new_bytecode.empend(jump_kind);
                    new_bytecode.empend((ByteCodeValueType)0); // Placeholder for the jump target.
                    auto post_loop_fork_jump_index = new_bytecode.size();
                    new_bytecode.extend(bytecode_to_repeat);
                    fork_jump_address = new_bytecode.size();

                    new_bytecode[post_loop_fork_jump_index - 1] = (ByteCodeValueType)(fork_jump_address - post_loop_fork_jump_index);

                    new_bytecode.empend((ByteCodeValueType)OpCodeId::ResetRepeat);
                    new_bytecode.empend((ByteCodeValueType)max_repetition_mark_id);
                }
                new_bytecode[pre_loop_fork_jump_index - 1] = (ByteCodeValueType)(fork_jump_address - pre_loop_fork_jump_index);
            }
        } else {
            // no maximum value set, repeat finding if possible:
            // (REPEAT REGEXP MIN)
            // LABEL _START
            // CHECKPOINT _C
            // REGEXP
            // JUMP_NONEMPTY _C _START FORK

            // Note: This is only safe because REPEAT will leave one iteration outside (see repetition_n)
            new_bytecode.insert(new_bytecode.size() - bytecode_to_repeat.size(), (ByteCodeValueType)OpCodeId::Checkpoint);

            auto jump_kind = static_cast<ByteCodeValueType>(greedy ? OpCodeId::ForkJump : OpCodeId::ForkStay);
            new_bytecode.empend((ByteCodeValueType)OpCodeId::JumpNonEmpty);
            new_bytecode.empend(-bytecode_to_repeat.size() - 4 - 1); // Jump to the last iteration
            new_bytecode.empend(-bytecode_to_repeat.size() - 4 - 1); // if _C is not empty.
            new_bytecode.empend(jump_kind);
        }

        bytecode_to_repeat = move(new_bytecode);
    }

    template<typename T>
    void insert_bytecode_repetition_n(ByteCode& bytecode_to_repeat, T n, size_t repetition_mark_id) requires(IsIntegral<T>)
    {
        // LABEL _LOOP
        // REGEXP
        // REPEAT _LOOP N-1
        // REGEXP
        if (n == 0)
            return;

        // Note: this bytecode layout allows callers to repeat the last REGEXP instruction without the
        // REPEAT instruction forcing another loop.
        extend(bytecode_to_repeat);

        if (n > 1) {
            empend(static_cast<ByteCodeValueType>(OpCodeId::Repeat));
            empend(bytecode_to_repeat.size());
            empend(static_cast<ByteCodeValueType>(n - 1));
            empend(repetition_mark_id);

            extend(bytecode_to_repeat);
        }
    }

    static void transform_bytecode_repetition_min_one(ByteCode& bytecode_to_repeat, bool greedy)
    {
        // LABEL _START = -bytecode_to_repeat.size()
        // CHECKPOINT _C
        // REGEXP
        // JUMP_NONEMPTY _C _START FORKSTAY (FORKJUMP -> Greedy)

        bytecode_to_repeat.prepend((ByteCodeValueType)OpCodeId::Checkpoint);

        bytecode_to_repeat.empend((ByteCodeValueType)OpCodeId::JumpNonEmpty);
        bytecode_to_repeat.empend(-bytecode_to_repeat.size() - 3); // Jump to the _START label...
        bytecode_to_repeat.empend(-bytecode_to_repeat.size() - 2); // ...if _C is not empty

        if (greedy)
            bytecode_to_repeat.empend(static_cast<ByteCodeValueType>(OpCodeId::ForkJump));
        else
            bytecode_to_repeat.empend(static_cast<ByteCodeValueType>(OpCodeId::ForkStay));
    }

    static void transform_bytecode_repetition_any(ByteCode& bytecode_to_repeat, bool greedy)
    {
        // LABEL _START
        // FORKJUMP _END  (FORKSTAY -> Greedy)
        // CHECKPOINT _C
        // REGEXP
        // JUMP_NONEMPTY _C _START JUMP
        // LABEL _END

        // LABEL _START = m_bytes.size();
        ByteCode bytecode;

        if (greedy)
            bytecode.empend(static_cast<ByteCodeValueType>(OpCodeId::ForkStay));
        else
            bytecode.empend(static_cast<ByteCodeValueType>(OpCodeId::ForkJump));

        bytecode.empend(bytecode_to_repeat.size() + 1 + 4); // Jump to the _END label

        auto c_label = bytecode.size();
        bytecode.empend(static_cast<ByteCodeValueType>(OpCodeId::Checkpoint));

        bytecode.extend(bytecode_to_repeat);

        bytecode.empend(static_cast<ByteCodeValueType>(OpCodeId::JumpNonEmpty));
        bytecode.empend(-bytecode.size() - 3);          // Jump(...) to the _START label...
        bytecode.empend(c_label - bytecode.size() - 2); // ...only if _C passes.
        bytecode.empend((ByteCodeValueType)OpCodeId::Jump);
        // LABEL _END = bytecode.size()

        bytecode_to_repeat = move(bytecode);
    }

    static void transform_bytecode_repetition_zero_or_one(ByteCode& bytecode_to_repeat, bool greedy)
    {
        // FORKJUMP _END (FORKSTAY -> Greedy)
        // REGEXP
        // LABEL _END
        ByteCode bytecode;

        if (greedy)
            bytecode.empend(static_cast<ByteCodeValueType>(OpCodeId::ForkStay));
        else
            bytecode.empend(static_cast<ByteCodeValueType>(OpCodeId::ForkJump));

        bytecode.empend(bytecode_to_repeat.size()); // Jump to the _END label

        bytecode.extend(move(bytecode_to_repeat));
        // LABEL _END = bytecode.size()

        bytecode_to_repeat = move(bytecode);
    }

    OpCode& get_opcode(MatchState& state) const;

private:
    void insert_string(StringView view)
    {
        empend((ByteCodeValueType)view.length());
        for (size_t i = 0; i < view.length(); ++i)
            empend((ByteCodeValueType)view[i]);
    }

    void ensure_opcodes_initialized();
    ALWAYS_INLINE OpCode& get_opcode_by_id(OpCodeId id) const;
    static OwnPtr<OpCode> s_opcodes[(size_t)OpCodeId::Last + 1];
    static bool s_opcodes_initialized;
};

#define ENUMERATE_EXECUTION_RESULTS                          \
    __ENUMERATE_EXECUTION_RESULT(Continue)                   \
    __ENUMERATE_EXECUTION_RESULT(Fork_PrioHigh)              \
    __ENUMERATE_EXECUTION_RESULT(Fork_PrioLow)               \
    __ENUMERATE_EXECUTION_RESULT(Failed)                     \
    __ENUMERATE_EXECUTION_RESULT(Failed_ExecuteLowPrioForks) \
    __ENUMERATE_EXECUTION_RESULT(Succeeded)

enum class ExecutionResult : u8 {
#define __ENUMERATE_EXECUTION_RESULT(x) x,
    ENUMERATE_EXECUTION_RESULTS
#undef __ENUMERATE_EXECUTION_RESULT
};

char const* execution_result_name(ExecutionResult result);
char const* opcode_id_name(OpCodeId opcode_id);
char const* boundary_check_type_name(BoundaryCheckType);
char const* character_compare_type_name(CharacterCompareType result);

class OpCode {
public:
    OpCode() = default;
    virtual ~OpCode() = default;

    virtual OpCodeId opcode_id() const = 0;
    virtual size_t size() const = 0;
    virtual ExecutionResult execute(MatchInput const& input, MatchState& state) const = 0;

    ALWAYS_INLINE ByteCodeValueType argument(size_t offset) const
    {
        VERIFY(state().instruction_position + offset <= m_bytecode->size());
        return m_bytecode->at(state().instruction_position + 1 + offset);
    }

    ALWAYS_INLINE char const* name() const;
    static char const* name(OpCodeId);

    ALWAYS_INLINE void set_state(MatchState& state) { m_state = &state; }

    ALWAYS_INLINE void set_bytecode(ByteCode& bytecode) { m_bytecode = &bytecode; }

    ALWAYS_INLINE MatchState const& state() const
    {
        VERIFY(m_state);
        return *m_state;
    }

    String to_string() const
    {
        return String::formatted("[{:#02X}] {}", (int)opcode_id(), name(opcode_id()));
    }

    virtual String arguments_string() const = 0;

    ALWAYS_INLINE ByteCode const& bytecode() const { return *m_bytecode; }

protected:
    ByteCode* m_bytecode { nullptr };
    MatchState* m_state { nullptr };
};

class OpCode_Exit final : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::Exit; }
    ALWAYS_INLINE size_t size() const override { return 1; }
    String arguments_string() const override { return String::empty(); }
};

class OpCode_FailForks final : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::FailForks; }
    ALWAYS_INLINE size_t size() const override { return 1; }
    String arguments_string() const override { return String::empty(); }
};

class OpCode_Save final : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::Save; }
    ALWAYS_INLINE size_t size() const override { return 1; }
    String arguments_string() const override { return String::empty(); }
};

class OpCode_Restore final : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::Restore; }
    ALWAYS_INLINE size_t size() const override { return 1; }
    String arguments_string() const override { return String::empty(); }
};

class OpCode_GoBack final : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::GoBack; }
    ALWAYS_INLINE size_t size() const override { return 2; }
    ALWAYS_INLINE size_t count() const { return argument(0); }
    String arguments_string() const override { return String::formatted("count={}", count()); }
};

class OpCode_Jump final : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::Jump; }
    ALWAYS_INLINE size_t size() const override { return 2; }
    ALWAYS_INLINE ssize_t offset() const { return argument(0); }
    String arguments_string() const override
    {
        return String::formatted("offset={} [&{}]", offset(), state().instruction_position + size() + offset());
    }
};

class OpCode_ForkJump : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::ForkJump; }
    ALWAYS_INLINE size_t size() const override { return 2; }
    ALWAYS_INLINE ssize_t offset() const { return argument(0); }
    String arguments_string() const override
    {
        return String::formatted("offset={} [&{}], sp: {}", offset(), state().instruction_position + size() + offset(), state().string_position);
    }
};

class OpCode_ForkReplaceJump final : public OpCode_ForkJump {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::ForkReplaceJump; }
};

class OpCode_ForkStay : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::ForkStay; }
    ALWAYS_INLINE size_t size() const override { return 2; }
    ALWAYS_INLINE ssize_t offset() const { return argument(0); }
    String arguments_string() const override
    {
        return String::formatted("offset={} [&{}], sp: {}", offset(), state().instruction_position + size() + offset(), state().string_position);
    }
};

class OpCode_ForkReplaceStay final : public OpCode_ForkStay {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::ForkReplaceStay; }
};

class OpCode_CheckBegin final : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::CheckBegin; }
    ALWAYS_INLINE size_t size() const override { return 1; }
    String arguments_string() const override { return String::empty(); }
};

class OpCode_CheckEnd final : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::CheckEnd; }
    ALWAYS_INLINE size_t size() const override { return 1; }
    String arguments_string() const override { return String::empty(); }
};

class OpCode_CheckBoundary final : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::CheckBoundary; }
    ALWAYS_INLINE size_t size() const override { return 2; }
    ALWAYS_INLINE size_t arguments_count() const { return 1; }
    ALWAYS_INLINE BoundaryCheckType type() const { return static_cast<BoundaryCheckType>(argument(0)); }
    String arguments_string() const override { return String::formatted("kind={} ({})", (long unsigned int)argument(0), boundary_check_type_name(type())); }
};

class OpCode_ClearCaptureGroup final : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::ClearCaptureGroup; }
    ALWAYS_INLINE size_t size() const override { return 2; }
    ALWAYS_INLINE size_t id() const { return argument(0); }
    String arguments_string() const override { return String::formatted("id={}", id()); }
};

class OpCode_SaveLeftCaptureGroup final : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::SaveLeftCaptureGroup; }
    ALWAYS_INLINE size_t size() const override { return 2; }
    ALWAYS_INLINE size_t id() const { return argument(0); }
    String arguments_string() const override { return String::formatted("id={}", id()); }
};

class OpCode_SaveRightCaptureGroup final : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::SaveRightCaptureGroup; }
    ALWAYS_INLINE size_t size() const override { return 2; }
    ALWAYS_INLINE size_t id() const { return argument(0); }
    String arguments_string() const override { return String::formatted("id={}", id()); }
};

class OpCode_SaveRightNamedCaptureGroup final : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::SaveRightNamedCaptureGroup; }
    ALWAYS_INLINE size_t size() const override { return 4; }
    ALWAYS_INLINE StringView name() const { return { reinterpret_cast<char*>(argument(0)), length() }; }
    ALWAYS_INLINE size_t length() const { return argument(1); }
    ALWAYS_INLINE size_t id() const { return argument(2); }
    String arguments_string() const override
    {
        return String::formatted("name={}, length={}", name(), length());
    }
};

class OpCode_Compare final : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::Compare; }
    ALWAYS_INLINE size_t size() const override { return arguments_size() + 3; }
    ALWAYS_INLINE size_t arguments_count() const { return argument(0); }
    ALWAYS_INLINE size_t arguments_size() const { return argument(1); }
    String arguments_string() const override;
    Vector<String> variable_arguments_to_string(Optional<MatchInput> input = {}) const;
    Vector<CompareTypeAndValuePair> flat_compares() const;

private:
    ALWAYS_INLINE static void compare_char(MatchInput const& input, MatchState& state, u32 ch1, bool inverse, bool& inverse_matched);
    ALWAYS_INLINE static bool compare_string(MatchInput const& input, MatchState& state, RegexStringView str, bool& had_zero_length_match);
    ALWAYS_INLINE static void compare_character_class(MatchInput const& input, MatchState& state, CharClass character_class, u32 ch, bool inverse, bool& inverse_matched);
    ALWAYS_INLINE static void compare_character_range(MatchInput const& input, MatchState& state, u32 from, u32 to, u32 ch, bool inverse, bool& inverse_matched);
    ALWAYS_INLINE static void compare_property(MatchInput const& input, MatchState& state, Unicode::Property property, bool inverse, bool& inverse_matched);
    ALWAYS_INLINE static void compare_general_category(MatchInput const& input, MatchState& state, Unicode::GeneralCategory general_category, bool inverse, bool& inverse_matched);
    ALWAYS_INLINE static void compare_script(MatchInput const& input, MatchState& state, Unicode::Script script, bool inverse, bool& inverse_matched);
    ALWAYS_INLINE static void compare_script_extension(MatchInput const& input, MatchState& state, Unicode::Script script, bool inverse, bool& inverse_matched);
};

class OpCode_Repeat : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::Repeat; }
    ALWAYS_INLINE size_t size() const override { return 4; }
    ALWAYS_INLINE size_t offset() const { return argument(0); }
    ALWAYS_INLINE u64 count() const { return argument(1); }
    ALWAYS_INLINE size_t id() const { return argument(2); }
    String arguments_string() const override
    {
        auto reps = id() < state().repetition_marks.size() ? state().repetition_marks.at(id()) : 0;
        return String::formatted("offset={} count={} id={} rep={}, sp: {}", offset(), count() + 1, id(), reps + 1, state().string_position);
    }
};

class OpCode_ResetRepeat : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::ResetRepeat; }
    ALWAYS_INLINE size_t size() const override { return 2; }
    ALWAYS_INLINE size_t id() const { return argument(0); }
    String arguments_string() const override
    {
        auto reps = id() < state().repetition_marks.size() ? state().repetition_marks.at(id()) : 0;
        return String::formatted("id={} rep={}", id(), reps + 1);
    }
};

class OpCode_Checkpoint final : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::Checkpoint; }
    ALWAYS_INLINE size_t size() const override { return 1; }
    String arguments_string() const override { return String::empty(); }
};

class OpCode_JumpNonEmpty final : public OpCode {
public:
    ExecutionResult execute(MatchInput const& input, MatchState& state) const override;
    ALWAYS_INLINE OpCodeId opcode_id() const override { return OpCodeId::JumpNonEmpty; }
    ALWAYS_INLINE size_t size() const override { return 4; }
    ALWAYS_INLINE ssize_t offset() const { return argument(0); }
    ALWAYS_INLINE ssize_t checkpoint() const { return argument(1); }
    ALWAYS_INLINE OpCodeId form() const { return (OpCodeId)argument(2); }
    String arguments_string() const override
    {
        return String::formatted("{} offset={} [&{}], cp={} [&{}]",
            opcode_id_name(form()),
            offset(), state().instruction_position + size() + offset(),
            checkpoint(), state().instruction_position + size() + checkpoint());
    }
};

template<typename T>
bool is(OpCode const&);

template<typename T>
ALWAYS_INLINE bool is(OpCode const&)
{
    return false;
}

template<typename T>
ALWAYS_INLINE bool is(OpCode const* opcode)
{
    return is<T>(*opcode);
}

template<>
ALWAYS_INLINE bool is<OpCode_ForkStay>(OpCode const& opcode)
{
    return opcode.opcode_id() == OpCodeId::ForkStay;
}

template<>
ALWAYS_INLINE bool is<OpCode_Exit>(OpCode const& opcode)
{
    return opcode.opcode_id() == OpCodeId::Exit;
}

template<>
ALWAYS_INLINE bool is<OpCode_Compare>(OpCode const& opcode)
{
    return opcode.opcode_id() == OpCodeId::Compare;
}

template<typename T>
ALWAYS_INLINE T const& to(OpCode const& opcode)
{
    return verify_cast<T>(opcode);
}

template<typename T>
ALWAYS_INLINE T* to(OpCode* opcode)
{
    return verify_cast<T>(opcode);
}

template<typename T>
ALWAYS_INLINE T const* to(OpCode const* opcode)
{
    return verify_cast<T>(opcode);
}

template<typename T>
ALWAYS_INLINE T& to(OpCode& opcode)
{
    return verify_cast<T>(opcode);
}

}
