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
#include <AK/FileSystemPath.h>
#include <AK/Function.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <stdio.h>
#include <sys/stat.h>

namespace LaunchServer {

static Launcher* s_the;
static bool spawn(String executable, String argument);

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
        m_handlers.set(app_executable, { app_name, app_executable, file_types, protocols });
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
    if (url.protocol() == "file")
        return handlers_for_path(url.path());

    return handlers_for(url.protocol(), m_protocol_handlers, [](auto& handler, auto& key) {
        return handler.protocols.contains(key);
    });
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
    pid_t child_pid = fork();
    if (child_pid < 0) {
        perror("fork");
        return false;
    }
    if (child_pid == 0) {
        if (execl(executable.characters(), executable.characters(), argument.characters(), nullptr) < 0) {
            perror("execl");
            return false;
        }
        ASSERT_NOT_REACHED();
    }
    return true;
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

Vector<String> Launcher::handlers_for(const String& key, HashMap<String, String>& user_preference, Function<bool(Handler&, const String&)> handler_matches)
{
    Vector<String> handlers;

    auto user_preferred = user_preference.get(key);
    if (user_preferred.has_value())
        handlers.append(user_preferred.value());

    for (auto& handler : m_handlers) {
        // Skip over the existing item in the list
        if (user_preferred.has_value() && user_preferred.value() == handler.value.executable)
            continue;
        if (handler_matches(handler.value, key))
            handlers.append(handler.value.executable);
    }

    auto user_default = user_preference.get("*");
    if (handlers.size() == 0 && user_default.has_value())
        handlers.append(user_default.value());

    return handlers;
}

Vector<String> Launcher::handlers_for_path(const String& path)
{
    struct stat st;
    if (stat(path.characters(), &st) < 0) {
        perror("stat");
        return {};
    }

    // TODO: Make directory opening configurable
    if (S_ISDIR(st.st_mode))
        return { "/bin/FileManager" };

    auto extension = FileSystemPath(path).extension().to_lowercase();

    return handlers_for(extension, m_file_handlers, [](auto& handler, auto& key) {
        return handler.file_types.contains(key);
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

    if (st.st_mode & (S_IXUSR | S_IXGRP | S_IXOTH))
        return spawn(url.path(), {});

    auto extension_parts = url.path().to_lowercase().split('.');
    String extension = {};
    if (extension_parts.size() > 1)
        extension = extension_parts.last();
    return open_with_user_preferences(m_file_handlers, extension, url.path(), "/bin/TextEdit");
}
}
