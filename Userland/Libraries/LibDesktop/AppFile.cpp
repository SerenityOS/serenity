/*
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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

#include <AK/Function.h>
#include <AK/Vector.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibDesktop/AppFile.h>

namespace Desktop {

NonnullRefPtr<AppFile> AppFile::get_for_app(const StringView& app_name)
{
    auto path = String::formatted("{}/{}.af", APP_FILES_DIRECTORY, app_name);
    return open(path);
}

NonnullRefPtr<AppFile> AppFile::open(const StringView& path)
{
    return adopt(*new AppFile(path));
}

void AppFile::for_each(Function<void(NonnullRefPtr<AppFile>)> callback, const StringView& directory)
{
    Core::DirIterator di(directory, Core::DirIterator::SkipDots);
    if (di.has_error())
        return;
    while (di.has_next()) {
        auto name = di.next_path();
        if (!name.ends_with(".af"))
            continue;
        auto path = String::formatted("{}/{}", directory, name);
        auto af = AppFile::open(path);
        if (!af->is_valid())
            continue;
        callback(af);
    }
}

AppFile::AppFile(const StringView& path)
    : m_config(Core::ConfigFile::open(path))
    , m_valid(validate())
{
}

AppFile::~AppFile()
{
}

bool AppFile::validate() const
{
    if (m_config->read_entry("App", "Name").trim_whitespace().is_empty())
        return false;
    if (m_config->read_entry("App", "Executable").trim_whitespace().is_empty())
        return false;
    return true;
}

String AppFile::name() const
{
    auto name = m_config->read_entry("App", "Name").trim_whitespace();
    VERIFY(!name.is_empty());
    return name;
}

String AppFile::executable() const
{
    auto executable = m_config->read_entry("App", "Executable").trim_whitespace();
    VERIFY(!executable.is_empty());
    return executable;
}

String AppFile::category() const
{
    return m_config->read_entry("App", "Category").trim_whitespace();
}

Vector<String> AppFile::launcher_file_types() const
{
    Vector<String> file_types;
    for (auto& entry : m_config->read_entry("Launcher", "FileTypes").split(',')) {
        entry = entry.trim_whitespace();
        if (!entry.is_empty())
            file_types.append(entry);
    }
    return file_types;
}

Vector<String> AppFile::launcher_protocols() const
{
    Vector<String> protocols;
    for (auto& entry : m_config->read_entry("Launcher", "Protocols").split(',')) {
        entry = entry.trim_whitespace();
        if (!entry.is_empty())
            protocols.append(entry);
    }
    return protocols;
}

}
