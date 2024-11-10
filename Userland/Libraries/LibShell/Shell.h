/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Job.h"
#include "Parser.h"
#include <AK/Array.h>
#include <AK/ByteString.h>
#include <AK/CircularQueue.h>
#include <AK/HashMap.h>
#include <AK/IntrusiveList.h>
#include <AK/StackInfo.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/EventReceiver.h>
#include <LibCore/Notifier.h>
#include <LibLine/Editor.h>
#include <LibMain/Main.h>
#include <termios.h>

#define ENUMERATE_SHELL_BUILTINS()                           \
    __ENUMERATE_SHELL_BUILTIN(alias, InAllModes)             \
    __ENUMERATE_SHELL_BUILTIN(where, InAllModes)             \
    __ENUMERATE_SHELL_BUILTIN(cd, InAllModes)                \
    __ENUMERATE_SHELL_BUILTIN(cdh, InAllModes)               \
    __ENUMERATE_SHELL_BUILTIN(command, InAllModes)           \
    __ENUMERATE_SHELL_BUILTIN(pwd, InAllModes)               \
    __ENUMERATE_SHELL_BUILTIN(type, InAllModes)              \
    __ENUMERATE_SHELL_BUILTIN(exec, InAllModes)              \
    __ENUMERATE_SHELL_BUILTIN(eval, OnlyInPOSIXMode)         \
    __ENUMERATE_SHELL_BUILTIN(exit, InAllModes)              \
    __ENUMERATE_SHELL_BUILTIN(export, InAllModes)            \
    __ENUMERATE_SHELL_BUILTIN(glob, InAllModes)              \
    __ENUMERATE_SHELL_BUILTIN(unalias, InAllModes)           \
    __ENUMERATE_SHELL_BUILTIN(unset, InAllModes)             \
    __ENUMERATE_SHELL_BUILTIN(set, InAllModes)               \
    __ENUMERATE_SHELL_BUILTIN(history, InAllModes)           \
    __ENUMERATE_SHELL_BUILTIN(umask, InAllModes)             \
    __ENUMERATE_SHELL_BUILTIN(not, InAllModes)               \
    __ENUMERATE_SHELL_BUILTIN(dirs, InAllModes)              \
    __ENUMERATE_SHELL_BUILTIN(pushd, InAllModes)             \
    __ENUMERATE_SHELL_BUILTIN(popd, InAllModes)              \
    __ENUMERATE_SHELL_BUILTIN(setopt, InAllModes)            \
    __ENUMERATE_SHELL_BUILTIN(shift, InAllModes)             \
    __ENUMERATE_SHELL_BUILTIN(source, InAllModes)            \
    __ENUMERATE_SHELL_BUILTIN(time, InAllModes)              \
    __ENUMERATE_SHELL_BUILTIN(jobs, InAllModes)              \
    __ENUMERATE_SHELL_BUILTIN(disown, InAllModes)            \
    __ENUMERATE_SHELL_BUILTIN(fg, InAllModes)                \
    __ENUMERATE_SHELL_BUILTIN(bg, InAllModes)                \
    __ENUMERATE_SHELL_BUILTIN(wait, InAllModes)              \
    __ENUMERATE_SHELL_BUILTIN(dump, InAllModes)              \
    __ENUMERATE_SHELL_BUILTIN(kill, InAllModes)              \
    __ENUMERATE_SHELL_BUILTIN(reset, InAllModes)             \
    __ENUMERATE_SHELL_BUILTIN(noop, InAllModes)              \
    __ENUMERATE_SHELL_BUILTIN(break, OnlyInPOSIXMode)        \
    __ENUMERATE_SHELL_BUILTIN(continue, OnlyInPOSIXMode)     \
    __ENUMERATE_SHELL_BUILTIN(return, InAllModes)            \
    __ENUMERATE_SHELL_BUILTIN(read, OnlyInPOSIXMode)         \
    __ENUMERATE_SHELL_BUILTIN(run_with_env, OnlyInPOSIXMode) \
    __ENUMERATE_SHELL_BUILTIN(argsparser_parse, InAllModes)  \
    __ENUMERATE_SHELL_BUILTIN(in_parallel, InAllModes)       \
    __ENUMERATE_SHELL_BUILTIN(shell_set_active_prompt, InAllModes)

#define ENUMERATE_SHELL_OPTIONS()                                                                                    \
    __ENUMERATE_SHELL_OPTION(inline_exec_keep_empty_segments, false, "Keep empty segments in inline execute $(...)") \
    __ENUMERATE_SHELL_OPTION(verbose, false, "Announce every command that is about to be executed")                  \
    __ENUMERATE_SHELL_OPTION(invoke_program_for_autocomplete, false, "Attempt to use the program being completed itself for autocompletion via --complete")

#define ENUMERATE_SHELL_IMMEDIATE_FUNCTIONS()                          \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(concat_lists)                 \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(length)                       \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(length_across)                \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(remove_suffix)                \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(remove_prefix)                \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(regex_replace)                \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(filter_glob)                  \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(split)                        \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(join)                         \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(value_or_default)             \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(assign_default)               \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(error_if_empty)               \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(null_or_alternative)          \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(defined_value_or_default)     \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(assign_defined_default)       \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(error_if_unset)               \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(null_if_unset_or_alternative) \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(length_of_variable)           \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(reexpand)                     \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(math)

namespace Shell {

class Shell;

enum class POSIXModeRequirement {
    OnlyInPOSIXMode,
    InAllModes,
};

class Shell : public Core::EventReceiver {
    C_OBJECT(Shell);

public:
    constexpr static auto local_init_file_path = "~/.shellrc";
    constexpr static auto global_init_file_path = "/etc/shellrc";
    constexpr static auto local_posix_init_file_path = "~/.posixshrc";
    constexpr static auto global_posix_init_file_path = "/etc/posixshrc";

    bool should_format_live() const { return m_should_format_live; }
    void set_live_formatting(bool value) { m_should_format_live = value; }

    void setup_signals();
    void setup_keybinds();

    struct SourcePosition {
        Optional<ByteString> source_file;
        ByteString literal_source_text;
        Optional<AST::Position> position;
    };

    struct RunnablePath {
        enum class Kind {
            Builtin,
            Function,
            Alias,
            Executable,
        };

        Kind kind;
        ByteString path;

        bool operator<(RunnablePath const& other) const
        {
            return path < other.path;
        }

        bool operator==(RunnablePath const&) const = default;
    };

    struct RunnablePathComparator {
        int operator()(RunnablePath const& lhs, RunnablePath const& rhs)
        {
            if (lhs.path > rhs.path)
                return 1;

            if (lhs.path < rhs.path)
                return -1;

            return 0;
        }

        int operator()(StringView lhs, RunnablePath const& rhs)
        {
            if (lhs > rhs.path)
                return 1;

            if (lhs < rhs.path)
                return -1;

            return 0;
        }
    };

    int run_command(StringView, Optional<SourcePosition> = {});
    Optional<RunnablePath> runnable_path_for(StringView);
    Optional<ByteString> help_path_for(Vector<RunnablePath> visited, RunnablePath const& runnable_path);
    ErrorOr<RefPtr<Job>> run_command(const AST::Command&);
    Vector<NonnullRefPtr<Job>> run_commands(Vector<AST::Command>&);
    bool run_file(ByteString const&, bool explicitly_invoked = true);
    ErrorOr<bool> run_builtin(const AST::Command&, Vector<NonnullRefPtr<AST::Rewiring>> const&, int& retval);
    bool has_builtin(StringView) const;
    ErrorOr<RefPtr<AST::Node>> run_immediate_function(StringView name, AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const&);
    static bool has_immediate_function(StringView);
    void block_on_job(RefPtr<Job>);
    void block_on_pipeline(RefPtr<AST::Pipeline>);
    ByteString prompt() const;

    static ByteString expand_tilde(StringView expression);
    static ErrorOr<Vector<ByteString>> expand_globs(StringView path, StringView base);
    static Vector<ByteString> expand_globs(Vector<StringView> path_segments, StringView base);
    ErrorOr<Vector<AST::Command>> expand_aliases(Vector<AST::Command>);
    ByteString resolve_path(ByteString) const;
    Optional<ByteString> resolve_alias(StringView) const;

    static bool has_history_event(StringView);

    ErrorOr<RefPtr<AST::Value const>> get_argument(size_t) const;
    ErrorOr<RefPtr<AST::Value const>> look_up_local_variable(StringView) const;
    ErrorOr<ByteString> local_variable_or(StringView, ByteString const&) const;
    void set_local_variable(ByteString const&, RefPtr<AST::Value>, bool only_in_current_frame = false);
    void unset_local_variable(StringView, bool only_in_current_frame = false);

    void define_function(ByteString name, Vector<ByteString> argnames, RefPtr<AST::Node> body);
    bool has_function(StringView);
    bool invoke_function(const AST::Command&, int& retval);

    ByteString format(StringView, ssize_t& cursor) const;

    RefPtr<Line::Editor> editor() const { return m_editor; }

    enum class LocalFrameKind {
        FunctionOrGlobal,
        Block,
    };
    struct LocalFrame {
        LocalFrame(ByteString name, HashMap<ByteString, RefPtr<AST::Value>> variables, LocalFrameKind kind = LocalFrameKind::Block)
            : name(move(name))
            , local_variables(move(variables))
            , is_function_frame(kind == LocalFrameKind::FunctionOrGlobal)
        {
        }

        ByteString name;
        HashMap<ByteString, RefPtr<AST::Value>> local_variables;
        bool is_function_frame;
    };

    struct Frame {
        Frame(Vector<NonnullOwnPtr<LocalFrame>>& frames, LocalFrame const& frame)
            : frames(frames)
            , frame(frame)
        {
        }
        ~Frame();

        void leak_frame() { should_destroy_frame = false; }

    private:
        Vector<NonnullOwnPtr<LocalFrame>>& frames;
        LocalFrame const& frame;
        bool should_destroy_frame { true };
    };

    [[nodiscard]] Frame push_frame(ByteString name, LocalFrameKind = LocalFrameKind::Block);
    void pop_frame();

    struct Promise {
        struct Data {
            struct Unveil {
                ByteString path;
                ByteString access;
            };
            ByteString exec_promises;
            Vector<Unveil> unveils;
        } data;

        IntrusiveListNode<Promise> node;
        using List = IntrusiveList<&Promise::node>;
    };

    struct ScopedPromise {
        ScopedPromise(Promise::List& promises, Promise&& promise)
            : promises(promises)
            , promise(move(promise))
        {
            promises.append(this->promise);
        }

        ~ScopedPromise()
        {
            promises.remove(promise);
        }

        Promise::List& promises;
        Promise promise;
    };
    [[nodiscard]] ScopedPromise promise(Promise::Data data)
    {
        return { m_active_promises, { move(data), {} } };
    }

    enum class EscapeMode {
        Bareword,
        SingleQuotedString,
        DoubleQuotedString,
    };
    static ByteString escape_token_for_double_quotes(StringView token);
    static ByteString escape_token_for_single_quotes(StringView token);
    static ByteString escape_token(StringView token, EscapeMode = EscapeMode::Bareword);
    static ByteString escape_token(Utf32View token, EscapeMode = EscapeMode::Bareword);
    static ByteString unescape_token(StringView token);
    enum class SpecialCharacterEscapeMode {
        Untouched,
        Escaped,
        QuotedAsEscape,
        QuotedAsHex,
    };
    static SpecialCharacterEscapeMode special_character_escape_mode(u32 c, EscapeMode);

    static bool is_glob(StringView);

    enum class ExecutableOnly {
        Yes,
        No
    };

    ErrorOr<void> highlight(Line::Editor&) const;
    Vector<Line::CompletionSuggestion> complete();
    Vector<Line::CompletionSuggestion> complete(StringView);
    Vector<Line::CompletionSuggestion> complete_program_name(StringView, size_t offset, EscapeMode = EscapeMode::Bareword);
    Vector<Line::CompletionSuggestion> complete_variable(StringView, size_t offset);
    Vector<Line::CompletionSuggestion> complete_user(StringView, size_t offset);
    Vector<Line::CompletionSuggestion> complete_immediate_function_name(StringView, size_t offset);

    Vector<Line::CompletionSuggestion> complete_path(StringView base, StringView, size_t offset, ExecutableOnly executable_only, AST::Node const* command_node, AST::Node const*, EscapeMode = EscapeMode::Bareword);
    Vector<Line::CompletionSuggestion> complete_option(StringView, StringView, size_t offset, AST::Node const* command_node, AST::Node const*);
    ErrorOr<Vector<Line::CompletionSuggestion>> complete_via_program_itself(size_t offset, AST::Node const* command_node, AST::Node const*, EscapeMode escape_mode, StringView known_program_name);

    void restore_ios();

    u64 find_last_job_id() const;
    Job* find_job(u64 id, bool is_pid = false);
    Job* current_job() const { return m_current_job; }
    void kill_job(Job const*, int sig);

    ByteString get_history_path();
    void print_path(StringView path);
    void cache_path();

    bool read_single_line();

    void notify_child_event();

    bool posix_mode() const { return m_in_posix_mode; }

    struct termios termios;
    struct termios default_termios;
    bool was_interrupted { false };
    bool was_resized { false };

    ByteString cwd;
    ByteString username;
    ByteString home;

    constexpr static auto TTYNameSize = 32;
    constexpr static auto HostNameSize = 64;

    char ttyname[TTYNameSize];
    char hostname[HostNameSize];

    uid_t uid;
    Optional<int> last_return_code;
    Vector<ByteString> directory_stack;
    CircularQueue<ByteString, 8> cd_history; // FIXME: have a configurable cd history length
    HashMap<u64, NonnullRefPtr<Job>> jobs;
    Vector<RunnablePath, 256> cached_path;

    ByteString current_script;

    enum ShellEventType {
        ReadLine,
    };

    enum class ShellError {
        None,
        InternalControlFlowBreak,
        InternalControlFlowContinue,
        InternalControlFlowReturn,
        InternalControlFlowInterrupted,
        InternalControlFlowKilled,
        EvaluatedSyntaxError,
        NonExhaustiveMatchRules,
        InvalidGlobError,
        InvalidSliceContentsError,
        OpenFailure,
        OutOfMemory,
        LaunchError,
        PipeFailure,
        WriteFailure,
    };

    void raise_error(ShellError kind, ByteString description, Optional<AST::Position> position = {})
    {
        m_error = kind;
        m_error_description = move(description);
        if (m_source_position.has_value() && position.has_value())
            m_source_position.value().position = position.release_value();
    }
    bool has_error(ShellError err) const { return m_error == err; }
    bool has_any_error() const { return !has_error(ShellError::None); }
    ByteString const& error_description() const { return m_error_description; }
    ShellError take_error()
    {
        auto err = m_error;
        m_error = ShellError::None;
        m_error_description = {};
        return err;
    }
    void possibly_print_error() const;
    static bool is_control_flow(ShellError error)
    {
        switch (error) {
        case ShellError::InternalControlFlowBreak:
        case ShellError::InternalControlFlowContinue:
        case ShellError::InternalControlFlowReturn:
        case ShellError::InternalControlFlowInterrupted:
        case ShellError::InternalControlFlowKilled:
            return true;
        default:
            return false;
        }
    }

#define __ENUMERATE_SHELL_OPTION(name, default_, description) \
    bool name { default_ };

    struct Options {
        ENUMERATE_SHELL_OPTIONS();
    } options;

#undef __ENUMERATE_SHELL_OPTION

private:
    Shell(Line::Editor&, bool attempt_interactive, bool posix_mode = false);
    Shell();
    virtual ~Shell() override;

    void destroy();
    void initialize(bool attempt_interactive);

    RefPtr<AST::Node> parse(StringView, bool interactive = false, bool as_command = true) const;

    void timer_event(Core::TimerEvent&) override;

    void set_user_prompt();

    bool is_allowed_to_modify_termios(const AST::Command&) const;

    void bring_cursor_to_beginning_of_a_line() const;

    Optional<int> resolve_job_spec(StringView);
    void add_entry_to_cache(RunnablePath const&);
    void remove_entry_from_cache(StringView);
    void stop_all_jobs();
    Job* m_current_job { nullptr };
    LocalFrame* find_frame_containing_local_variable(StringView name);
    LocalFrame const* find_frame_containing_local_variable(StringView name) const
    {
        return const_cast<Shell*>(this)->find_frame_containing_local_variable(name);
    }

    void run_tail(RefPtr<Job>);
    void run_tail(const AST::Command&, const AST::NodeWithAction&, int head_exit_code);

    [[noreturn]] void execute_process(Vector<char const*>&& argv);
    ErrorOr<void> execute_process(Span<StringView> argv);

    virtual void custom_event(Core::CustomEvent&) override;

#define __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(name) \
    ErrorOr<RefPtr<AST::Node>> immediate_##name(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const&);

    ENUMERATE_SHELL_IMMEDIATE_FUNCTIONS();

#undef __ENUMERATE_SHELL_IMMEDIATE_FUNCTION

    ErrorOr<RefPtr<AST::Node>> immediate_length_impl(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const&, bool across);

#define __ENUMERATE_SHELL_BUILTIN(builtin, _mode) \
    ErrorOr<int> builtin_##builtin(Main::Arguments);

    ENUMERATE_SHELL_BUILTINS();

#undef __ENUMERATE_SHELL_BUILTIN

    static constexpr Array builtin_names = {
#define __ENUMERATE_SHELL_BUILTIN(builtin, _mode) #builtin##sv,

        ENUMERATE_SHELL_BUILTINS()

#undef __ENUMERATE_SHELL_BUILTIN

            "."sv, // Needs to be aliased to "source" in POSIX mode.
        // clang-format off
        // Clang-format does not properly indent this, it gives it 4 spaces too few.
            ":"sv, // POSIX-y name for "noop".
        // clang-format on
    };

    struct ShellFunction {
        ByteString name;
        Vector<ByteString> arguments;
        RefPtr<AST::Node> body;
    };

    ErrorOr<String> serialize_function_definition(ShellFunction const&) const;

    bool m_should_ignore_jobs_on_next_exit { false };
    pid_t m_pid { 0 };

    HashMap<ByteString, ShellFunction> m_functions;
    Vector<NonnullOwnPtr<LocalFrame>> m_local_frames;
    Promise::List m_active_promises;
    Vector<NonnullRefPtr<AST::Redirection>> m_global_redirections;

    HashMap<ByteString, ByteString> m_aliases;
    bool m_is_interactive { true };
    bool m_is_subshell { false };
    bool m_should_reinstall_signal_handlers { true };
    bool m_in_posix_mode { false };

    ShellError m_error { ShellError::None };
    ByteString m_error_description;
    Optional<SourcePosition> m_source_position;

    bool m_should_format_live { false };

    RefPtr<Line::Editor> m_editor;

    bool m_default_constructed { false };

    mutable bool m_last_continuation_state { false }; // false == not needed.

    Optional<size_t> m_history_autosave_time;

    StackInfo m_completion_stack_info;

    RefPtr<AST::Node> m_prompt_command_node;
    mutable Optional<ByteString> m_next_scheduled_prompt_text;
};

[[maybe_unused]] static constexpr bool is_word_character(char c)
{
    return c == '_' || (c <= 'Z' && c >= 'A') || (c <= 'z' && c >= 'a') || (c <= '9' && c >= '0');
}

inline size_t find_offset_into_node(StringView unescaped_text, size_t escaped_offset, Shell::EscapeMode escape_mode)
{
    size_t unescaped_offset = 0;
    size_t offset = 0;
    auto do_find_offset = [&](auto& unescaped_text) {
        for (auto c : unescaped_text) {
            if (offset == escaped_offset)
                return unescaped_offset;

            switch (Shell::special_character_escape_mode(c, escape_mode)) {
            case Shell::SpecialCharacterEscapeMode::Untouched:
                break;
            case Shell::SpecialCharacterEscapeMode::Escaped:
                ++offset; // X -> \X
                break;
            case Shell::SpecialCharacterEscapeMode::QuotedAsEscape:
                switch (escape_mode) {
                case Shell::EscapeMode::Bareword:
                    offset += 3; // X -> "\Y"
                    break;
                case Shell::EscapeMode::SingleQuotedString:
                    offset += 5; // X -> '"\Y"'
                    break;
                case Shell::EscapeMode::DoubleQuotedString:
                    offset += 1; // X -> \Y
                    break;
                }
                break;
            case Shell::SpecialCharacterEscapeMode::QuotedAsHex:
                switch (escape_mode) {
                case Shell::EscapeMode::Bareword:
                    offset += 2; // X -> "\..."
                    break;
                case Shell::EscapeMode::SingleQuotedString:
                    offset += 4; // X -> '"\..."'
                    break;
                case Shell::EscapeMode::DoubleQuotedString:
                    // X -> \...
                    break;
                }
                if (c > NumericLimits<u8>::max())
                    offset += 8; // X -> "\uhhhhhhhh"
                else
                    offset += 3; // X -> "\xhh"
                break;
            }
            ++offset;
            ++unescaped_offset;
        }
        return unescaped_offset;
    };

    Utf8View view { unescaped_text };
    if (view.validate())
        return do_find_offset(view);
    return do_find_offset(unescaped_text);
}

}

namespace AK {

template<>
struct Traits<Shell::Shell::RunnablePath> : public DefaultTraits<Shell::Shell::RunnablePath> {
    static constexpr bool is_trivial() { return false; }

    static bool equals(Shell::Shell::RunnablePath const& self, Shell::Shell::RunnablePath const& other)
    {
        return self == other;
    }

    static bool equals(Shell::Shell::RunnablePath const& self, StringView other)
    {
        return self.path == other;
    }

    static bool equals(Shell::Shell::RunnablePath const& self, ByteString const& other)
    {
        return self.path == other;
    }
};

}
