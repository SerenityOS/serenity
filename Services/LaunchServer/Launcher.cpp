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
#include <LibCore/ConfigFile.h>
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

    return { m_protocol_handlers.get(url.protocol()).value_or(m_protocol_handlers.get("*").value_or({})) };
}

bool Launcher::open_url(const URL& url)
{
    if (url.protocol() == "file")
        return open_file_url(url);

    return open_with_handlers(m_protocol_handlers, url.protocol(), url.to_string(), "/bin/Browser");
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

bool Launcher::open_with_handlers(const HashMap<String, String>& handlers, const String key, const String argument, const String default_program)
{
    auto program_path = handlers.get(key);
    if (program_path.has_value())
        return spawn(program_path.value(), argument);

    // There wasn't a handler for this, so try the fallback instead
    program_path = handlers.get("*");
    if (program_path.has_value())
        return spawn(program_path.value(), argument);

    // Absolute worst case, try the provided default

    return spawn(default_program, argument);
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
    return { m_file_handlers.get(extension).value_or(m_file_handlers.get("*").value_or({})) };
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
    return open_with_handlers(m_file_handlers, extension, url.path(), "/bin/TextEdit");
}

}
