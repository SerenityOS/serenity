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
#include <LibCore/DirIterator.h>
#include <serenity.h>
#include <spawn.h>
#include <stdio.h>
#include <sys/stat.h>

namespace LaunchServer {

static Launcher* s_the;
static bool spawn(String executable, String argument);

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
    if (!icons.is_empty()) {
        JsonObject icons_obj;
        for (auto& icon : icons)
            icons_obj.set(icon.key, icon.value);
        obj.add("icons", move(icons_obj));
    }
    obj.finish();
    return builder.build();
}

Launcher::Launcher()
{
    ASSERT(s_the == nullptr);
    s_the = this;
}

Launcher& Launcher::the()
{
    ASSERT(s_the);
    return *s_the;
}

void Launcher::load_handlers(const String& af_dir)
{
    auto load_hashtable = [](auto& af, auto& key) {
        HashTable<String> table;

        auto config_value = af->read_entry("Launcher", key, {});
        for (auto& key : config_value.split(','))
            table.set(key.to_lowercase());

        return table;
    };
    auto load_hashmap = [](auto& af, auto& group) {
        HashMap<String, String> map;
        auto keys = af->keys(group);
        for (auto& key : keys)
            map.set(key, af->read_entry(group, key));

        return map;
    };

    Core::DirIterator dt(af_dir, Core::DirIterator::SkipDots);
    while (dt.has_next()) {
        auto af_name = dt.next_path();
        auto af_path = String::format("%s/%s", af_dir.characters(), af_name.characters());
        auto af = Core::ConfigFile::open(af_path);
        if (!af->has_key("App", "Name") || !af->has_key("App", "Executable"))
            continue;
        auto app_name = af->read_entry("App", "Name");
        auto app_executable = af->read_entry("App", "Executable");
        auto file_types = load_hashtable(af, "FileTypes");
        auto protocols = load_hashtable(af, "Protocols");
        auto icons = load_hashmap(af, "Icons");
        m_handlers.set(app_executable, { Handler::Type::Default, move(app_name), app_executable, move(file_types), move(protocols), move(icons) });
    }
}

void Launcher::load_config(const Core::ConfigFile& cfg)
{
    for (auto key : cfg.keys("FileType")) {
        m_file_handlers.set(key.to_lowercase(), cfg.read_entry("FileType", key));
    }

    for (auto key : cfg.keys("Protocol")) {
        m_protocol_handlers.set(key.to_lowercase(), cfg.read_entry("Protocol", key));
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

    return open_with_user_preferences(m_protocol_handlers, url.protocol(), url.to_string(), "/bin/Browser");
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
    return spawn(handler.executable, argument);
}

bool spawn(String executable, String argument)
{
    pid_t child_pid;
    const char* argv[] = { executable.characters(), argument.characters(), nullptr };
    if ((errno = posix_spawn(&child_pid, executable.characters(), nullptr, nullptr, const_cast<char**>(argv), environ))) {
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

bool Launcher::open_with_user_preferences(const HashMap<String, String>& user_preferences, const String key, const String argument, const String default_program)
{
    auto program_path = user_preferences.get(key);
    if (program_path.has_value())
        return spawn(program_path.value(), argument);

    // There wasn't a handler for this, so try the fallback instead
    program_path = user_preferences.get("*");
    if (program_path.has_value())
        return spawn(program_path.value(), argument);

    // Absolute worst case, try the provided default

    return spawn(default_program, argument);
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
    if (S_ISDIR(st.st_mode))
        return spawn("/bin/FileManager", url.path());

    if ((st.st_mode & S_IFMT) == S_IFREG && st.st_mode & (S_IXUSR | S_IXGRP | S_IXOTH))
        return spawn(url.path(), {});

    auto extension_parts = url.path().to_lowercase().split('.');
    String extension = {};
    if (extension_parts.size() > 1)
        extension = extension_parts.last();
    return open_with_user_preferences(m_file_handlers, extension, url.path(), "/bin/TextEdit");
}
}
