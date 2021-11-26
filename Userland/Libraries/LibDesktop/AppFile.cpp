/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/Vector.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Process.h>
#include <LibDesktop/AppFile.h>

namespace Desktop {

NonnullRefPtr<AppFile> AppFile::get_for_app(StringView app_name)
{
    auto path = String::formatted("{}/{}.af", APP_FILES_DIRECTORY, app_name);
    return open(path);
}

NonnullRefPtr<AppFile> AppFile::open(StringView path)
{
    return adopt_ref(*new AppFile(path));
}

void AppFile::for_each(Function<void(NonnullRefPtr<AppFile>)> callback, StringView directory)
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

AppFile::AppFile(StringView path)
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

String AppFile::description() const
{
    return m_config->read_entry("App", "Description").trim_whitespace();
}

String AppFile::category() const
{
    return m_config->read_entry("App", "Category").trim_whitespace();
}

String AppFile::icon_path() const
{
    return m_config->read_entry("App", "IconPath").trim_whitespace();
}

GUI::Icon AppFile::icon() const
{
    auto override_icon = icon_path();
    // FIXME: support pointing to actual .ico files
    if (!override_icon.is_empty())
        return GUI::FileIconProvider::icon_for_path(override_icon);

    return GUI::FileIconProvider::icon_for_path(executable());
}

bool AppFile::run_in_terminal() const
{
    return m_config->read_bool_entry("App", "RunInTerminal", false);
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

bool AppFile::spawn() const
{
    if (!is_valid())
        return false;

    auto pid = Core::Process::spawn(executable());
    if (pid < 0)
        return false;

    return true;
}

}
