/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Job.h"
#include "Parser.h"
#include <AK/Array.h>
#include <AK/CircularQueue.h>
#include <AK/DeprecatedString.h>
#include <AK/HashMap.h>
#include <AK/StackInfo.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/Notifier.h>
#include <LibCore/Object.h>
#include <LibLine/Editor.h>
#include <LibMain/Main.h>
#include <termios.h>

#define ENUMERATE_SHELL_BUILTINS()     \
    __ENUMERATE_SHELL_BUILTIN(alias)   \
    __ENUMERATE_SHELL_BUILTIN(where)   \
    __ENUMERATE_SHELL_BUILTIN(cd)      \
    __ENUMERATE_SHELL_BUILTIN(cdh)     \
    __ENUMERATE_SHELL_BUILTIN(pwd)     \
    __ENUMERATE_SHELL_BUILTIN(type)    \
    __ENUMERATE_SHELL_BUILTIN(exec)    \
    __ENUMERATE_SHELL_BUILTIN(exit)    \
    __ENUMERATE_SHELL_BUILTIN(export)  \
    __ENUMERATE_SHELL_BUILTIN(glob)    \
    __ENUMERATE_SHELL_BUILTIN(unalias) \
    __ENUMERATE_SHELL_BUILTIN(unset)   \
    __ENUMERATE_SHELL_BUILTIN(history) \
    __ENUMERATE_SHELL_BUILTIN(umask)   \
    __ENUMERATE_SHELL_BUILTIN(not )    \
    __ENUMERATE_SHELL_BUILTIN(dirs)    \
    __ENUMERATE_SHELL_BUILTIN(pushd)   \
    __ENUMERATE_SHELL_BUILTIN(popd)    \
    __ENUMERATE_SHELL_BUILTIN(setopt)  \
    __ENUMERATE_SHELL_BUILTIN(shift)   \
    __ENUMERATE_SHELL_BUILTIN(source)  \
    __ENUMERATE_SHELL_BUILTIN(time)    \
    __ENUMERATE_SHELL_BUILTIN(jobs)    \
    __ENUMERATE_SHELL_BUILTIN(disown)  \
    __ENUMERATE_SHELL_BUILTIN(fg)      \
    __ENUMERATE_SHELL_BUILTIN(bg)      \
    __ENUMERATE_SHELL_BUILTIN(wait)    \
    __ENUMERATE_SHELL_BUILTIN(dump)    \
    __ENUMERATE_SHELL_BUILTIN(kill)    \
    __ENUMERATE_SHELL_BUILTIN(noop)    \
    __ENUMERATE_SHELL_BUILTIN(argsparser_parse)

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

class Shell : public Core::Object {
    C_OBJECT(Shell);

public:
    constexpr static auto local_init_file_path = "~/.shellrc";
    constexpr static auto global_init_file_path = "/etc/shellrc";
    constexpr static auto local_posix_init_file_path = "~/.posixshrc";
    constexpr static auto global_posix_init_file_path = "/etc/posixshrc";

    bool should_format_live() const { return m_should_format_live; }
    void set_live_formatting(bool value) { m_should_format_live = value; }

    void setup_signals();

    struct SourcePosition {
        DeprecatedString source_file;
        DeprecatedString literal_source_text;
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
        DeprecatedString path;

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
    Optional<DeprecatedString> help_path_for(Vector<RunnablePath> visited, RunnablePath const& runnable_path);
    ErrorOr<RefPtr<Job>> run_command(const AST::Command&);
    Vector<NonnullRefPtr<Job>> run_commands(Vector<AST::Command>&);
    bool run_file(DeprecatedString const&, bool explicitly_invoked = true);
    ErrorOr<bool> run_builtin(const AST::Command&, Vector<NonnullRefPtr<AST::Rewiring>> const&, int& retval);
    bool has_builtin(StringView) const;
    ErrorOr<RefPtr<AST::Node>> run_immediate_function(StringView name, AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const&);
    static bool has_immediate_function(StringView);
    void block_on_job(RefPtr<Job>);
    void block_on_pipeline(RefPtr<AST::Pipeline>);
    DeprecatedString prompt() const;

    static DeprecatedString expand_tilde(StringView expression);
    static Vector<DeprecatedString> expand_globs(StringView path, StringView base);
    static Vector<DeprecatedString> expand_globs(Vector<StringView> path_segments, StringView base);
    ErrorOr<Vector<AST::Command>> expand_aliases(Vector<AST::Command>);
    DeprecatedString resolve_path(DeprecatedString) const;
    DeprecatedString resolve_alias(StringView) const;

    static bool has_history_event(StringView);

    ErrorOr<RefPtr<AST::Value const>> get_argument(size_t) const;
    ErrorOr<RefPtr<AST::Value const>> lookup_local_variable(StringView) const;
    ErrorOr<DeprecatedString> local_variable_or(StringView, DeprecatedString const&) const;
    void set_local_variable(DeprecatedString const&, RefPtr<AST::Value>, bool only_in_current_frame = false);
    void unset_local_variable(StringView, bool only_in_current_frame = false);

    void define_function(DeprecatedString name, Vector<DeprecatedString> argnames, RefPtr<AST::Node> body);
    bool has_function(StringView);
    bool invoke_function(const AST::Command&, int& retval);

    DeprecatedString format(StringView, ssize_t& cursor) const;

    RefPtr<Line::Editor> editor() const { return m_editor; }

    struct LocalFrame {
        LocalFrame(DeprecatedString name, HashMap<DeprecatedString, RefPtr<AST::Value>> variables)
            : name(move(name))
            , local_variables(move(variables))
        {
        }

        DeprecatedString name;
        HashMap<DeprecatedString, RefPtr<AST::Value>> local_variables;
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

    [[nodiscard]] Frame push_frame(DeprecatedString name);
    void pop_frame();

    struct Promise {
        struct Data {
            struct Unveil {
                DeprecatedString path;
                DeprecatedString access;
            };
            DeprecatedString exec_promises;
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
    static DeprecatedString escape_token_for_double_quotes(StringView token);
    static DeprecatedString escape_token_for_single_quotes(StringView token);
    static DeprecatedString escape_token(StringView token, EscapeMode = EscapeMode::Bareword);
    static DeprecatedString escape_token(Utf32View token, EscapeMode = EscapeMode::Bareword);
    static DeprecatedString unescape_token(StringView token);
    enum class SpecialCharacterEscapeMode {
        Untouched,
        Escaped,
        QuotedAsEscape,
        QuotedAsHex,
    };
    static SpecialCharacterEscapeMode special_character_escape_mode(u32 c, EscapeMode);

    static bool is_glob(StringView);
    static Vector<StringView> split_path(StringView);

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

    DeprecatedString get_history_path();
    void print_path(StringView path);
    void cache_path();

    bool read_single_line();

    void notify_child_event();

    struct termios termios;
    struct termios default_termios;
    bool was_interrupted { false };
    bool was_resized { false };

    DeprecatedString cwd;
    DeprecatedString username;
    DeprecatedString home;

    constexpr static auto TTYNameSize = 32;
    constexpr static auto HostNameSize = 64;

    char ttyname[TTYNameSize];
    char hostname[HostNameSize];

    uid_t uid;
    Optional<int> last_return_code;
    Vector<DeprecatedString> directory_stack;
    CircularQueue<DeprecatedString, 8> cd_history; // FIXME: have a configurable cd history length
    HashMap<u64, NonnullRefPtr<Job>> jobs;
    Vector<RunnablePath, 256> cached_path;

    DeprecatedString current_script;

    enum ShellEventType {
        ReadLine,
    };

    enum class ShellError {
        None,
        InternalControlFlowBreak,
        InternalControlFlowContinue,
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

    void raise_error(ShellError kind, DeprecatedString description, Optional<AST::Position> position = {})
    {
        m_error = kind;
        m_error_description = move(description);
        if (m_source_position.has_value() && position.has_value())
            m_source_position.value().position = position.release_value();
    }
    bool has_error(ShellError err) const { return m_error == err; }
    bool has_any_error() const { return !has_error(ShellError::None); }
    DeprecatedString const& error_description() const { return m_error_description; }
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

    RefPtr<AST::Node> parse(StringView, bool interactive = false, bool as_command = true) const;

    void timer_event(Core::TimerEvent&) override;

    bool is_allowed_to_modify_termios(const AST::Command&) const;

    // FIXME: Port to Core::Property
    ErrorOr<void> save_to(JsonObject&);
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

#define __ENUMERATE_SHELL_BUILTIN(builtin) \
    ErrorOr<int> builtin_##builtin(Main::Arguments);

    ENUMERATE_SHELL_BUILTINS();

#undef __ENUMERATE_SHELL_BUILTIN

    static constexpr Array builtin_names = {
#define __ENUMERATE_SHELL_BUILTIN(builtin) #builtin##sv,

        ENUMERATE_SHELL_BUILTINS()

#undef __ENUMERATE_SHELL_BUILTIN

            ":"sv, // POSIX-y name for "noop".
    };

    bool m_should_ignore_jobs_on_next_exit { false };
    pid_t m_pid { 0 };

    struct ShellFunction {
        DeprecatedString name;
        Vector<DeprecatedString> arguments;
        RefPtr<AST::Node> body;
    };

    HashMap<DeprecatedString, ShellFunction> m_functions;
    Vector<NonnullOwnPtr<LocalFrame>> m_local_frames;
    Promise::List m_active_promises;
    Vector<NonnullRefPtr<AST::Redirection>> m_global_redirections;

    HashMap<DeprecatedString, DeprecatedString> m_aliases;
    bool m_is_interactive { true };
    bool m_is_subshell { false };
    bool m_should_reinstall_signal_handlers { true };
    bool m_in_posix_mode { false };

    ShellError m_error { ShellError::None };
    DeprecatedString m_error_description;
    Optional<SourcePosition> m_source_position;

    bool m_should_format_live { false };

    RefPtr<Line::Editor> m_editor;

    bool m_default_constructed { false };

    mutable bool m_last_continuation_state { false }; // false == not needed.

    Optional<size_t> m_history_autosave_time;

    StackInfo m_completion_stack_info;
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
struct Traits<Shell::Shell::RunnablePath> : public GenericTraits<Shell::Shell::RunnablePath> {
    static constexpr bool is_trivial() { return false; }

    static bool equals(Shell::Shell::RunnablePath const& self, Shell::Shell::RunnablePath const& other)
    {
        return self == other;
    }

    static bool equals(Shell::Shell::RunnablePath const& self, StringView other)
    {
        return self.path == other;
    }

    static bool equals(Shell::Shell::RunnablePath const& self, DeprecatedString const& other)
    {
        return self.path == other;
    }
};

}
