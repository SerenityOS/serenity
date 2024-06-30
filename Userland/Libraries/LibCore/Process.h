/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2024, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Forward.h>
#include <AK/Span.h>
#include <LibCore/File.h>
#include <LibCore/Socket.h>

namespace Core {

namespace FileAction {

struct OpenFile {
    ByteString path;
    File::OpenMode mode = File::OpenMode::NotOpen;
    int fd = -1;
    mode_t permissions = 0600;
};

struct CloseFile {
    int fd { -1 };
};

// FIXME: Implement other file actions

}

struct ProcessSpawnOptions {
    StringView name {};
    ByteString executable {};
    bool search_for_executable_in_path { false };
    Vector<ByteString> const& arguments {};
    Optional<ByteString> working_directory {};

    using FileActionType = Variant<FileAction::OpenFile, FileAction::CloseFile>;
    Vector<FileActionType> file_actions {};
};

class IPCProcess;

class Process {
    AK_MAKE_NONCOPYABLE(Process);

public:
    enum class KeepAsChild {
        Yes,
        No
    };

    Process(Process&& other)
        : m_pid(exchange(other.m_pid, 0))
        , m_should_disown(exchange(other.m_should_disown, false))
    {
    }

    Process& operator=(Process&& other)
    {
        m_pid = exchange(other.m_pid, 0);
        m_should_disown = exchange(other.m_should_disown, false);
        return *this;
    }

    ~Process()
    {
        (void)disown();
    }

    static ErrorOr<Process> spawn(ProcessSpawnOptions const& options);

    // FIXME: Make the following 2 functions return Process instance or delete them.
    static ErrorOr<pid_t> spawn(StringView path, ReadonlySpan<ByteString> arguments, ByteString working_directory = {}, KeepAsChild keep_as_child = KeepAsChild::No);
    static ErrorOr<pid_t> spawn(StringView path, ReadonlySpan<StringView> arguments, ByteString working_directory = {}, KeepAsChild keep_as_child = KeepAsChild::No);

    // FIXME: Remove this. char const* should not exist on this level of abstraction.
    static ErrorOr<pid_t> spawn(StringView path, ReadonlySpan<char const*> arguments = {}, ByteString working_directory = {}, KeepAsChild keep_as_child = KeepAsChild::No);

    static ErrorOr<String> get_name();
    enum class SetThreadName {
        No,
        Yes,
    };
    static ErrorOr<void> set_name(StringView, SetThreadName = SetThreadName::No);

    static void wait_for_debugger_and_break();
    static ErrorOr<bool> is_being_debugged();

    pid_t pid() const { return m_pid; }

    ErrorOr<void> disown();

    // FIXME: Make it return an exit code.
    ErrorOr<bool> wait_for_termination();

private:
    friend IPCProcess;

    Process(pid_t pid)
        : m_pid(pid)
        , m_should_disown(true)
    {
    }

    pid_t m_pid;
    bool m_should_disown;
};

class IPCProcess {
public:
    template<typename ClientType>
    struct ProcessAndIPCClient {
        Process process;
        NonnullRefPtr<ClientType> client;
    };

    template<typename ClientType, typename... ClientArguments>
    static ErrorOr<ProcessAndIPCClient<ClientType>> spawn(ProcessSpawnOptions const& options, ClientArguments&&... client_arguments)
    {
        auto [process, socket] = TRY(spawn_and_connect_to_process(options));
        auto client = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ClientType { move(socket), forward<ClientArguments>(client_arguments)... }));

        return ProcessAndIPCClient<ClientType> { move(process), move(client) };
    }

    template<typename ClientType, typename... ClientArguments>
    static ErrorOr<ProcessAndIPCClient<ClientType>> spawn_singleton(ProcessSpawnOptions const& options, ClientArguments&&... client_arguments)
    {
        auto [process, socket] = TRY(spawn_singleton_and_connect_to_process(options));
        auto client = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) ClientType { move(socket), forward<ClientArguments>(client_arguments)... }));

        return ProcessAndIPCClient<ClientType> { move(process), move(client) };
    }

    struct ProcessPaths {
        ByteString socket_path;
        ByteString pid_path;
    };
    static ErrorOr<ProcessPaths> paths_for_process(StringView process_name);
    static ErrorOr<Optional<pid_t>> get_process_pid(StringView process_name, StringView pid_path);
    static ErrorOr<int> create_ipc_socket(ByteString const& socket_path);

    pid_t pid() const { return m_process.pid(); }

private:
    struct ProcessAndIPCSocket {
        Process process;
        NonnullOwnPtr<Core::LocalSocket> m_ipc_socket;
    };
    static ErrorOr<ProcessAndIPCSocket> spawn_and_connect_to_process(ProcessSpawnOptions const& options);
    static ErrorOr<ProcessAndIPCSocket> spawn_singleton_and_connect_to_process(ProcessSpawnOptions const& options);

    Process m_process;
};

}
