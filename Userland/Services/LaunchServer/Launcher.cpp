/*
 * Copyright (c) 2020, Nicholas Hollett <niax@niax.co.uk>
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Launcher.h"
#include <AK/JsonObjectSerializer.h>
#include <AK/LexicalPath.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <errno.h>
#include <serenity.h>
#include <spawn.h>
#include <sys/stat.h>

namespace LaunchServer {

static Launcher* s_the;

static ALWAYS_INLINE bool is_executable(mode_t mode)
{
    return (mode & (S_IXUSR | S_IXGRP | S_IXOTH));
}

static bool spawn(const String& executable, const Vector<String>& arguments)
{
    Vector<const char*> argv { executable.characters() };
    for (auto& arg : arguments)
        argv.append(arg.characters());
    argv.append(nullptr);

    pid_t child_pid;
    if ((errno = posix_spawn(&child_pid, executable.characters(), nullptr, nullptr, const_cast<char**>(argv.data()), environ))) {
        perror("posix_spawn");
        return false;
    }
    if (disown(child_pid) < 0) {
        perror("disown");
        return false;
    }

    return true;
}

static constexpr const char* TEXT_EDITOR_NAME = "Text Editor";
static constexpr const char* TEXT_EDITOR_EXECUTABLE = "/bin/TextEditor";
static constexpr const char* FILE_MANAGER_NAME = "File Manager";
static constexpr const char* FILE_MANAGER_EXECUTABLE = "/bin/FileManager";
static constexpr const char* TERMINAL_NAME = "Terminal";
static constexpr const char* TERMINAL_EXECUTABLE = "/bin/Terminal";

String Handler::to_details_str() const
{
    return to_details_str(handler_type, name, executable, run_in_terminal);
}

String Handler::to_details_str(Type handler_type, const String& name, const String& executable, bool run_in_terminal)
{
    StringBuilder builder;
    JsonObjectSerializer obj { builder };
    switch (handler_type) {
    case Type::Default:
        obj.add("type", "default");
        break;
    case Type::Application:
        obj.add("type", "application");
        break;
    case Type::UserDefault:
        obj.add("type", "userdefault");
        break;
    case Type::UserPreferred:
        obj.add("type", "userpreferred");
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    obj.add("name", name);
    obj.add("executable", executable);
    obj.add("run_in_terminal", run_in_terminal);
    obj.finish();
    return builder.build();
}

Launcher::Launcher(const String& app_file_dir)
    : m_app_file_dir { app_file_dir }
{
    VERIFY(s_the == nullptr);
    s_the = this;

    load_default_handlers();
    load_config();

    bool failed { false };
    auto watcher_or_error = Core::FileWatcher::create();
    if (watcher_or_error.is_error()) {
        dbgln("Core::FileWatcher::create(): {}", watcher_or_error.error());
        failed = true;
        return;
    }
    m_config_file_watcher = watcher_or_error.release_value();
    m_config_file_watcher->on_change = [this](auto&) {
        dbgln("Reloading config file");
        m_file_handlers.clear();
        m_protocol_handlers.clear();
        load_config();
    };
    auto add_watch_result = m_config_file_watcher->add_watch(LexicalPath::join(Core::StandardPaths::config_directory(), "LaunchServer.ini").string(), Core::FileWatcherEvent::Type::ContentModified | Core::FileWatcherEvent::Type::Deleted);
    if (add_watch_result.is_error()) {
        dbgln("Core::FileWatcher::add_watch(): {}", add_watch_result.error());
        failed = true;
    } else if (!add_watch_result.value()) {
        dbgln("Core::FileWatcher::add_watch(): false");
        failed = true;
    }
    if (failed)
        dbgln("Could not create a file watcher. Changes to config file will not be notified");
}

Launcher& Launcher::the()
{
    VERIFY(s_the);
    return *s_the;
}

void Launcher::load_default_handlers()
{
    Desktop::AppFile::for_each([this](auto app_file) {
        auto name = app_file->name();
        auto executable = app_file->executable();
        struct stat st;
        if (stat(executable.characters(), &st) < 0 || !is_executable(st.st_mode))
            return;
        auto default_handler = create<Handler>(Handler::Type::Default, name, executable, app_file->run_in_terminal());
        for (const auto& file_type : app_file->launcher_file_types())
            m_default_file_handlers.set(file_type.to_lowercase(), default_handler);
        for (const auto& protocol : app_file->launcher_protocols())
            m_default_protocol_handlers.set(protocol.to_lowercase(), default_handler);
    },
        m_app_file_dir);
}

void Launcher::load_config()
{
    auto config_file = Core::ConfigFile::get_for_app("LaunchServer");
    auto set_handlers = [this, &config_file](String entry, auto& handlers) {
        for (const auto& key : config_file->keys(entry)) {
            auto executable = config_file->read_entry(entry, key).trim_whitespace();
            if (executable.is_empty())
                continue;
            struct stat st;
            if (stat(executable.characters(), &st) < 0 || !is_executable(st.st_mode))
                continue;
            auto handler = create<Handler>(key == "*" ? Handler::Type::UserDefault : Handler::Type::UserPreferred, get_name_for_executable(executable), executable, run_in_terminal(executable));
            handlers.set(key.to_lowercase(), handler);
        }
    };

    set_handlers("FileType", m_file_handlers);
    set_handlers("Protocol", m_protocol_handlers);
}

bool Launcher::open_url(const URL& url, const String& handler_name)
{
    if (!handler_name.is_null())
        return open_with_handler_name(url, handler_name);
    if (url.protocol() == "file")
        return open_file_url(url);
    auto handlers = get_handlers(url);
    if (handlers.is_empty())
        return false;
    return spawn(handlers.first(), { url.to_string() });
}

Vector<String> Launcher::handlers_for_url(const URL& url)
{
    return get_handlers(url);
}

Vector<String> Launcher::handlers_with_details_for_url(const URL& url)
{
    return get_handlers(url, true);
}

bool Launcher::open_file_url(const URL& url)
{
    auto path = url.path();

    struct stat st;
    if (stat(path.characters(), &st) < 0) {
        perror("stat");
        return false;
    }

    if (S_ISDIR(st.st_mode)) {
        Vector<String> file_manager_arguments;
        if (url.fragment().is_empty()) {
            file_manager_arguments.append(path);
        } else {
            file_manager_arguments.append("-s");
            file_manager_arguments.append("-r");
            file_manager_arguments.append(LexicalPath::join(path, url.fragment()).string());
        }
        return spawn(FILE_MANAGER_EXECUTABLE, file_manager_arguments);
    }

    const auto& handler = get_handlers(url).take_first();

    if (is_executable(st.st_mode)) {
        if (run_in_terminal(handler))
            return spawn(TERMINAL_EXECUTABLE, { "-k", "-e", String::formatted("'{}'", path) });
        return spawn(path, {});
    }

    // Additional parameters parsing, specific for the file protocol and txt file handlers
    for (const auto& parameter : url.query().split('&')) {
        auto pair = parameter.split('=');
        if (pair.size() == 2 && pair[0] == "line_number") {
            auto line_number = pair[1].to_uint();
            if (line_number.has_value()) { // TextEditor uses file:line:col to open a file at a specific line number
                path = String::formatted("{}:{}", path, line_number.value());
                break;
            }
        }
    }

    if (run_in_terminal(handler))
        return spawn(TERMINAL_EXECUTABLE, { "-k", "-e", String::formatted("'{}' '{}'", handler, path) });
    return spawn(handler, { path });
}

bool Launcher::open_with_handler_name(const URL& url, const String& executable)
{
    struct stat st;
    if (stat(executable.characters(), &st) < 0 || !is_executable(st.st_mode))
        return false;
    String argument;
    if (url.protocol() == "file")
        argument = url.path();
    else
        argument = url.to_string();
    return spawn(executable, { argument });
}

Vector<String> Launcher::get_handlers(const URL& url, bool with_details) const
{
    bool is_file_url = (url.protocol() == "file");
    const auto& key = is_file_url ? LexicalPath::extension(Core::File::real_path_for(url.path())) : url.protocol();
    const auto& all_handlers = is_file_url ? m_file_handlers : m_protocol_handlers;
    const auto& all_default_handlers = is_file_url ? m_default_file_handlers : m_default_protocol_handlers;
    Vector<String> handlers;
    Vector<String> existing_handlers;
    handlers.ensure_capacity(4);
    existing_handlers.ensure_capacity(3);

    auto append_handler = [&handlers, &existing_handlers, with_details](const auto& handler_optional) {
        if (!handler_optional.has_value())
            return;
        auto handler = handler_optional.value();
        if (!existing_handlers.contains_slow(handler->executable))
            handlers.append(with_details ? handler->to_details_str() : handler->executable);
        existing_handlers.append(handler->executable);
    };

    if (is_file_url) {
        const auto& file_path = url.path();
        struct stat st;
        if (stat(file_path.characters(), &st) < 0) {
            perror("stat");
            return {};
        }
        if (S_ISDIR(st.st_mode)) {
            handlers.append(with_details ? Handler::to_details_str(Handler::Type::Default, FILE_MANAGER_NAME, FILE_MANAGER_EXECUTABLE) : FILE_MANAGER_EXECUTABLE);
            return handlers;
        }
        if (is_executable(st.st_mode)) {
            handlers.append(with_details ? Handler::to_details_str(Handler::Type::Application, get_name_for_executable(file_path), file_path, run_in_terminal(file_path)) : file_path);
        }
    }

    if (!key.is_empty())
        append_handler(all_handlers.get(key));
    append_handler(all_handlers.get("*"));
    if (!key.is_empty())
        append_handler(all_default_handlers.get(key));
    if (is_file_url && handlers.is_empty())
        handlers.append(with_details ? Handler::to_details_str(Handler::Type::Default, TEXT_EDITOR_NAME, TEXT_EDITOR_EXECUTABLE) : TEXT_EDITOR_EXECUTABLE);
    return handlers;
}

bool Launcher::run_in_terminal(const String& path) const
{
    auto basename = LexicalPath::basename(Core::File::real_path_for(path));
    return !(Core::File::exists(String::formatted("{}/{}.af", m_app_file_dir, basename)) && !Desktop::AppFile::get_for_app(basename)->run_in_terminal());
}

String Launcher::get_name_for_executable(const String& executable) const
{
    auto executable_basename = LexicalPath::basename(Core::File::real_path_for(executable));
    auto app_file = Desktop::AppFile::get_for_app(executable_basename);
    if (!Core::File::exists(String::formatted("{}/{}.af", m_app_file_dir, executable_basename)) || !app_file->is_valid())
        return executable_basename;
    return app_file->name();
}

}
