/*
 * Copyright (c) 2020, Nicholas Hollett <niax@niax.co.uk>, Andreas Kling <kling@serenityos.org>
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

#include "Launcher.h"
#include <AK/Function.h>
#include <AK/JsonObject.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/ConfigFile.h>
#include <LibDesktop/AppFile.h>
#include <serenity.h>
#include <spawn.h>
#include <stdio.h>
#include <sys/stat.h>

namespace LaunchServer {

static Launcher* s_the;
static bool spawn(String executable, const Vector<String>& arguments);

String Handler::name_from_executable(const StringView& executable)
{
    auto separator = executable.find_last_of('/');
    if (separator.has_value()) {
        auto start = separator.value() + 1;
        return executable.substring_view(start, executable.length() - start);
    }
    return executable;
}

void Handler::from_executable(Type handler_type, const String& executable)
{
    this->handler_type = handler_type;
    this->name = name_from_executable(executable);
    this->executable = executable;
}

String Handler::to_details_str() const
{
    StringBuilder builder;
    JsonObjectSerializer obj { builder };
    obj.add("executable", executable);
    obj.add("name", name);
    switch (handler_type) {
    case Type::Application:
        obj.add("type", "app");
        break;
    case Type::UserDefault:
        obj.add("type", "userdefault");
        break;
    case Type::UserPreferred:
        obj.add("type", "userpreferred");
        break;
    default:
        break;
    }
    obj.finish();
    return builder.build();
}

Launcher::Launcher()
{
    VERIFY(s_the == nullptr);
    s_the = this;
}

Launcher& Launcher::the()
{
    VERIFY(s_the);
    return *s_the;
}

void Launcher::load_handlers(const String& af_dir)
{
    Desktop::AppFile::for_each([&](auto af) {
        auto app_name = af->name();
        auto app_executable = af->executable();
        HashTable<String> file_types;
        for (auto& file_type : af->launcher_file_types())
            file_types.set(file_type);
        HashTable<String> protocols;
        for (auto& protocol : af->launcher_protocols())
            protocols.set(protocol);
        m_handlers.set(app_executable, { Handler::Type::Default, app_name, app_executable, file_types, protocols });
    },
        af_dir);
}

void Launcher::load_config(const Core::ConfigFile& cfg)
{
    for (auto key : cfg.keys("FileType")) {
        auto handler = cfg.read_entry("FileType", key).trim_whitespace();
        if (handler.is_empty())
            continue;
        m_file_handlers.set(key.to_lowercase(), handler);
    }

    for (auto key : cfg.keys("Protocol")) {
        auto handler = cfg.read_entry("Protocol", key).trim_whitespace();
        if (handler.is_empty())
            continue;
        m_protocol_handlers.set(key.to_lowercase(), handler);
    }
}

Vector<String> Launcher::handlers_for_url(const URL& url)
{
    Vector<String> handlers;
    if (url.protocol() == "file") {
        for_each_handler_for_path(url.path(), [&](auto& handler) -> bool {
            handlers.append(handler.executable);
            return true;
        });
    } else {
        for_each_handler(url.protocol(), m_protocol_handlers, [&](const auto& handler) -> bool {
            if (handler.handler_type != Handler::Type::Default || handler.protocols.contains(url.protocol())) {
                handlers.append(handler.executable);
                return true;
            }
            return false;
        });
    }
    return handlers;
}

Vector<String> Launcher::handlers_with_details_for_url(const URL& url)
{
    Vector<String> handlers;
    if (url.protocol() == "file") {
        for_each_handler_for_path(url.path(), [&](auto& handler) -> bool {
            handlers.append(handler.to_details_str());
            return true;
        });
    } else {
        for_each_handler(url.protocol(), m_protocol_handlers, [&](const auto& handler) -> bool {
            if (handler.handler_type != Handler::Type::Default || handler.protocols.contains(url.protocol())) {
                handlers.append(handler.to_details_str());
                return true;
            }
            return false;
        });
    }
    return handlers;
}

bool Launcher::open_url(const URL& url, const String& handler_name)
{
    if (!handler_name.is_null())
        return open_with_handler_name(url, handler_name);

    if (url.protocol() == "file")
        return open_file_url(url);

    return open_with_user_preferences(m_protocol_handlers, url.protocol(), { url.to_string() });
}

bool Launcher::open_with_handler_name(const URL& url, const String& handler_name)
{
    auto handler_optional = m_handlers.get(handler_name);
    if (!handler_optional.has_value())
        return false;

    auto& handler = handler_optional.value();
    String argument;
    if (url.protocol() == "file")
        argument = url.path();
    else
        argument = url.to_string();
    return spawn(handler.executable, { argument });
}

bool spawn(String executable, const Vector<String>& arguments)
{
    Vector<const char*> argv { executable.characters() };
    for (auto& arg : arguments)
        argv.append(arg.characters());
    argv.append(nullptr);

    pid_t child_pid;
    if ((errno = posix_spawn(&child_pid, executable.characters(), nullptr, nullptr, const_cast<char**>(argv.data()), environ))) {
        perror("posix_spawn");
        return false;
    } else {
        if (disown(child_pid) < 0)
            perror("disown");
    }
    return true;
}

Handler Launcher::get_handler_for_executable(Handler::Type handler_type, const String& executable) const
{
    Handler handler;
    auto existing_handler = m_handlers.get(executable);
    if (existing_handler.has_value()) {
        handler = existing_handler.value();
        handler.handler_type = handler_type;
    } else {
        handler.from_executable(handler_type, executable);
    }
    return handler;
}

bool Launcher::open_with_user_preferences(const HashMap<String, String>& user_preferences, const String key, const AK::Vector<String> arguments, const String default_program)
{
    auto program_path = user_preferences.get(key);
    if (program_path.has_value())
        return spawn(program_path.value(), arguments);

    // There wasn't a handler for this, so try the fallback instead
    program_path = user_preferences.get("*");
    if (program_path.has_value())
        return spawn(program_path.value(), arguments);

    // Absolute worst case, try the provided default program, if any
    if (!default_program.is_empty())
        return spawn(default_program, arguments);

    return false;
}

void Launcher::for_each_handler(const String& key, HashMap<String, String>& user_preference, Function<bool(const Handler&)> f)
{
    auto user_preferred = user_preference.get(key);
    if (user_preferred.has_value())
        f(get_handler_for_executable(Handler::Type::UserPreferred, user_preferred.value()));

    size_t counted = 0;
    for (auto& handler : m_handlers) {
        // Skip over the existing item in the list
        if (user_preferred.has_value() && user_preferred.value() == handler.value.executable)
            continue;
        if (f(handler.value))
            counted++;
    }

    auto user_default = user_preference.get("*");
    if (counted == 0 && user_default.has_value())
        f(get_handler_for_executable(Handler::Type::UserDefault, user_default.value()));
}

void Launcher::for_each_handler_for_path(const String& path, Function<bool(const Handler&)> f)
{
    struct stat st;
    if (stat(path.characters(), &st) < 0) {
        perror("stat");
        return;
    }

    // TODO: Make directory opening configurable
    if (S_ISDIR(st.st_mode)) {
        f(get_handler_for_executable(Handler::Type::Default, "/bin/FileManager"));
        return;
    }

    if ((st.st_mode & S_IFMT) == S_IFREG && (st.st_mode & (S_IXUSR | S_IXGRP | S_IXOTH)))
        f(get_handler_for_executable(Handler::Type::Application, path));

    auto extension = LexicalPath(path).extension().to_lowercase();

    for_each_handler(extension, m_file_handlers, [&](const auto& handler) -> bool {
        if (handler.handler_type != Handler::Type::Default || handler.file_types.contains(extension))
            return f(handler);
        return false;
    });
}

bool Launcher::open_file_url(const URL& url)
{
    struct stat st;
    if (stat(url.path().characters(), &st) < 0) {
        perror("stat");
        return false;
    }

    // TODO: Make directory opening configurable
    if (S_ISDIR(st.st_mode)) {
        Vector<String> fm_arguments;
        if (url.fragment().is_empty()) {
            fm_arguments.append(url.path());
        } else {
            fm_arguments.append(String::formatted("{}/{}", url.path(), url.fragment()));
            fm_arguments.append("-s");
            fm_arguments.append("-r");
        }
        return spawn("/bin/FileManager", fm_arguments);
    }

    if ((st.st_mode & S_IFMT) == S_IFREG && st.st_mode & (S_IXUSR | S_IXGRP | S_IXOTH))
        return spawn(url.path(), {});

    auto extension_parts = url.path().to_lowercase().split('.');
    String extension = {};
    if (extension_parts.size() > 1)
        extension = extension_parts.last();

    // Additionnal parameters parsing, specific for the file protocol
    Vector<String> additional_parameters;
    additional_parameters.append(url.path());
    auto parameters = url.query().split('&');
    for (auto parameter = parameters.begin(); parameter != parameters.end(); ++parameter) {
        auto pair = parameter->split('=');
        if (pair[0] == "line_number") {
            auto line = pair[1].to_int();
            if (line.has_value())
                additional_parameters.prepend(String::format("-l%i", *line));
        }
    }
    return open_with_user_preferences(m_file_handlers, extension, additional_parameters, "/bin/TextEditor");
}
}
