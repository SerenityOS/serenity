/*
 * Copyright (c) 2020, Peter Elliott <pelliott@ualberta.ca>
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

#include <LibCore/StandardPaths.h>
#include <LibDesktop/App.h>
#include <spawn.h>

#ifdef __serenity__
#    include <serenity.h>
#endif

namespace Desktop {

App::App(const StringView& path)
    : m_af_path(path)
    , m_config_file(Core::ConfigFile::open(path))
{
}

String App::name() const
{
    return m_config_file->read_entry("App", "Name");
}

String App::executable() const
{
    return m_config_file->read_entry("App", "Executable");
}

String App::category() const
{
    return m_config_file->read_entry("App", "Category");
}

String App::icon16x16() const
{
    return m_config_file->read_entry("Icons", "16x16");
}

String App::icon32x32() const
{
    return m_config_file->read_entry("Icons", "32x32");
}

#ifdef __serenity__
int App::launch(bool chdir) const
{
    pid_t child_pid;
    const char* argv[] = { executable().characters(), nullptr };

    auto af_key = String::formatted("AF_PATH={}", af_path().characters());
    const char* env[] = { af_key.characters(), nullptr };

    String home_directory;
    posix_spawn_file_actions_t actions;
    posix_spawn_file_actions_init(&actions);
    if (chdir) {
        home_directory = Core::StandardPaths::home_directory();
        posix_spawn_file_actions_addchdir(&actions, home_directory.characters());
    }

    int rc = posix_spawn(&child_pid, executable().characters(), &actions, nullptr, const_cast<char**>(argv), const_cast<char**>(env));
    posix_spawn_file_actions_destroy(&actions);

    if (rc)
        return rc;

    if (disown(child_pid) < 0)
        return errno;

    return 0;
}
#else
int App::launch(bool chdir) const
{
    (void)chdir;
    ASSERT_NOT_REACHED();
}
#endif

bool App::is_well_formed() const
{
    return m_config_file->has_key("App", "Name") && m_config_file->has_key("App", "Executable");
}

}
