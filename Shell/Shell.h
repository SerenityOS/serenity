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

struct ExitCodeOrContinuationRequest {
    enum ContinuationRequest {
        Nothing,
        Pipe,
        DoubleQuotedString,
        SingleQuotedString,
    };

    ExitCodeOrContinuationRequest(ContinuationRequest continuation)
        : continuation(continuation)
    {
    }

    ExitCodeOrContinuationRequest(int exit)
        : exit_code(exit)
    {
    }

    bool has_value() const { return exit_code.has_value(); }
    int value() const
    {
        ASSERT(has_value());
        return exit_code.value();
    }

    Optional<int> exit_code;
    ContinuationRequest continuation { Nothing };
};

using ContinuationRequest = ExitCodeOrContinuationRequest::ContinuationRequest;

#define ENUMERATE_SHELL_BUILTINS()     \
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
    __ENUMERATE_SHELL_BUILTIN(time)    \
    __ENUMERATE_SHELL_BUILTIN(jobs)    \
    __ENUMERATE_SHELL_BUILTIN(fg)      \
    __ENUMERATE_SHELL_BUILTIN(bg)

class Shell;

class Shell : public Core::Object {
    C_OBJECT(Shell);

public:
    ExitCodeOrContinuationRequest run_command(const StringView&);
    bool run_builtin(int argc, const char** argv, int& retval);
    String prompt() const;

    static String expand_tilde(const String&);
    static Vector<String> expand_globs(const StringView& path, const StringView& base);
    Vector<String> expand_parameters(const StringView&) const;

    static String escape_token(const String& token);
    static String unescape_token(const String& token);

    static bool is_glob(const StringView&);
    static Vector<StringView> split_path(const StringView&);

    Vector<String> process_arguments(const Vector<Token>&);

    static ContinuationRequest is_complete(const Vector<Command>&);

    void highlight(Line::Editor&) const;
    Vector<Line::CompletionSuggestion> complete_first(const String&);
    Vector<Line::CompletionSuggestion> complete_other(const String&);

    String get_history_path();
    void load_history();
    void save_history();
    void print_path(const String& path);

    bool should_read_more() const { return m_should_continue != ContinuationRequest::Nothing; }
    void finish_command() { m_should_break_current_command = true; }

    void read_single_line();

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
    HashMap<u64, OwnPtr<Job>> jobs;
    Vector<String, 256> cached_path;

    enum ShellEventType {
        ReadLine,
        ChildExited,
    };

private:
    Shell();
    virtual ~Shell() override;

    struct SpawnedProcess {
        String name;
        pid_t pid;
    };

    void cache_path();

    IterationDecision wait_for_pid(const SpawnedProcess&, bool is_first_command_in_chain, int& return_value);

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

    ExitCodeOrContinuationRequest::ContinuationRequest m_should_continue { ExitCodeOrContinuationRequest::Nothing };
    StringBuilder m_complete_line_builder;
    bool m_should_break_current_command { false };
};

static constexpr bool is_word_character(char c)
{
    return c == '_' || (c <= 'Z' && c >= 'A') || (c <= 'z' && c >= 'a');
}
