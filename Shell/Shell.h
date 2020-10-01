/*
 * Copyright (c) 2020, The SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "Job.h"
#include "Parser.h"
#include <AK/CircularQueue.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/Notifier.h>
#include <LibCore/Object.h>
#include <LibLine/Editor.h>
#include <termios.h>

#define ENUMERATE_SHELL_BUILTINS()     \
    __ENUMERATE_SHELL_BUILTIN(alias)   \
    __ENUMERATE_SHELL_BUILTIN(cd)      \
    __ENUMERATE_SHELL_BUILTIN(cdh)     \
    __ENUMERATE_SHELL_BUILTIN(pwd)     \
    __ENUMERATE_SHELL_BUILTIN(exit)    \
    __ENUMERATE_SHELL_BUILTIN(export)  \
    __ENUMERATE_SHELL_BUILTIN(unset)   \
    __ENUMERATE_SHELL_BUILTIN(history) \
    __ENUMERATE_SHELL_BUILTIN(umask)   \
    __ENUMERATE_SHELL_BUILTIN(dirs)    \
    __ENUMERATE_SHELL_BUILTIN(pushd)   \
    __ENUMERATE_SHELL_BUILTIN(popd)    \
    __ENUMERATE_SHELL_BUILTIN(setopt)  \
    __ENUMERATE_SHELL_BUILTIN(shift)   \
    __ENUMERATE_SHELL_BUILTIN(time)    \
    __ENUMERATE_SHELL_BUILTIN(jobs)    \
    __ENUMERATE_SHELL_BUILTIN(disown)  \
    __ENUMERATE_SHELL_BUILTIN(fg)      \
    __ENUMERATE_SHELL_BUILTIN(bg)

#define ENUMERATE_SHELL_OPTIONS()                                                                                    \
    __ENUMERATE_SHELL_OPTION(inline_exec_keep_empty_segments, false, "Keep empty segments in inline execute $(...)") \
    __ENUMERATE_SHELL_OPTION(verbose, false, "Announce every command that is about to be executed")

namespace Shell {

class Shell;

class Shell : public Core::Object {
    C_OBJECT(Shell);

public:
    constexpr static auto local_init_file_path = "~/.shellrc";
    constexpr static auto global_init_file_path = "/etc/shellrc";

    bool should_format_live() const { return m_should_format_live; }
    void set_live_formatting(bool value) { m_should_format_live = value; }

    void setup_signals();

    int run_command(const StringView&);
    bool is_runnable(const StringView&);
    RefPtr<Job> run_command(const AST::Command&);
    NonnullRefPtrVector<Job> run_commands(Vector<AST::Command>&);
    bool run_file(const String&, bool explicitly_invoked = true);
    bool run_builtin(const AST::Command&, const NonnullRefPtrVector<AST::Rewiring>&, int& retval);
    bool has_builtin(const StringView&) const;
    void block_on_job(RefPtr<Job>);
    String prompt() const;

    static String expand_tilde(const String&);
    static Vector<String> expand_globs(const StringView& path, StringView base);
    static Vector<String> expand_globs(Vector<StringView> path_segments, const StringView& base);
    Vector<AST::Command> expand_aliases(Vector<AST::Command>);
    String resolve_path(String) const;
    String resolve_alias(const String&) const;

    RefPtr<AST::Value> get_argument(size_t);
    RefPtr<AST::Value> lookup_local_variable(const String&);
    String local_variable_or(const String&, const String&);
    void set_local_variable(const String&, RefPtr<AST::Value>);
    void unset_local_variable(const String&);

    void define_function(String name, Vector<String> argnames, RefPtr<AST::Node> body);
    bool has_function(const String&);
    bool invoke_function(const AST::Command&, int& retval);

    String format(const StringView&, ssize_t& cursor) const;

    struct LocalFrame {
        HashMap<String, RefPtr<AST::Value>> local_variables;
    };

    struct Frame {
        Frame(Vector<LocalFrame>& frames, const LocalFrame& frame)
            : frames(frames)
            , frame(frame)
        {
        }
        ~Frame();

        void leak_frame() { should_destroy_frame = false; }

    private:
        Vector<LocalFrame>& frames;
        const LocalFrame& frame;
        bool should_destroy_frame { true };
    };

    [[nodiscard]] Frame push_frame();
    void pop_frame();

    static String escape_token(const String& token);
    static String unescape_token(const String& token);

    static bool is_glob(const StringView&);
    static Vector<StringView> split_path(const StringView&);

    void highlight(Line::Editor&) const;
    Vector<Line::CompletionSuggestion> complete();
    Vector<Line::CompletionSuggestion> complete_path(const String& base, const String&, size_t offset);
    Vector<Line::CompletionSuggestion> complete_program_name(const String&, size_t offset);
    Vector<Line::CompletionSuggestion> complete_variable(const String&, size_t offset);
    Vector<Line::CompletionSuggestion> complete_user(const String&, size_t offset);
    Vector<Line::CompletionSuggestion> complete_option(const String&, const String&, size_t offset);

    void restore_ios();

    u64 find_last_job_id() const;
    const Job* find_job(u64 id);
    const Job* current_job() const { return m_current_job; }
    void kill_job(const Job*, int sig);

    String get_history_path();
    void load_history();
    void save_history();
    void print_path(const String& path);

    bool read_single_line();

    void notify_child_event();

    struct termios termios;
    struct termios default_termios;
    bool was_interrupted { false };
    bool was_resized { false };

    String cwd;
    String username;
    String home;

    constexpr static auto TTYNameSize = 32;
    constexpr static auto HostNameSize = 64;

    char ttyname[TTYNameSize];
    char hostname[HostNameSize];

    uid_t uid;
    int last_return_code { 0 };
    Vector<String> directory_stack;
    CircularQueue<String, 8> cd_history; // FIXME: have a configurable cd history length
    HashMap<u64, NonnullRefPtr<Job>> jobs;
    Vector<String, 256> cached_path;

    String current_script;

    enum ShellEventType {
        ReadLine,
    };

#define __ENUMERATE_SHELL_OPTION(name, default_, description) \
    bool name { default_ };

    struct Options {
        ENUMERATE_SHELL_OPTIONS();
    } options;

#undef __ENUMERATE_SHELL_OPTION

private:
    Shell(Line::Editor&);
    Shell();
    virtual ~Shell() override;

    // FIXME: Port to Core::Property
    void save_to(JsonObject&);
    void bring_cursor_to_beginning_of_a_line() const;

    void cache_path();
    void add_entry_to_cache(const String&);
    void stop_all_jobs();
    const Job* m_current_job { nullptr };
    LocalFrame* find_frame_containing_local_variable(const String& name);

    void run_tail(RefPtr<Job>);
    void run_tail(const AST::NodeWithAction&, int head_exit_code);

    virtual void custom_event(Core::CustomEvent&) override;

#define __ENUMERATE_SHELL_BUILTIN(builtin) \
    int builtin_##builtin(int argc, const char** argv);

    ENUMERATE_SHELL_BUILTINS();

#undef __ENUMERATE_SHELL_BUILTIN

    constexpr static const char* builtin_names[] = {
#define __ENUMERATE_SHELL_BUILTIN(builtin) #builtin,

        ENUMERATE_SHELL_BUILTINS()

#undef __ENUMERATE_SHELL_BUILTIN
    };

    StringBuilder m_complete_line_builder;
    bool m_should_ignore_jobs_on_next_exit { false };
    pid_t m_pid { 0 };

    struct ShellFunction {
        String name;
        Vector<String> arguments;
        RefPtr<AST::Node> body;
    };

    HashMap<String, ShellFunction> m_functions;
    Vector<LocalFrame> m_local_frames;
    NonnullRefPtrVector<AST::Redirection> m_global_redirections;

    HashMap<String, String> m_aliases;
    bool m_is_interactive { true };
    bool m_is_subshell { false };

    bool m_should_format_live { false };

    RefPtr<Line::Editor> m_editor;

    bool m_default_constructed { false };
};

static constexpr bool is_word_character(char c)
{
    return c == '_' || (c <= 'Z' && c >= 'A') || (c <= 'z' && c >= 'a');
}

}
