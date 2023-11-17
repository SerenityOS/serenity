/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/Vector.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Process.h>
#include <LibDesktop/AppFile.h>
#include <LibFileSystem/FileSystem.h>

namespace Desktop {

DeprecatedString AppFile::app_file_path_for_app(StringView app_name)
{
    return DeprecatedString::formatted("{}/{}.af", APP_FILES_DIRECTORY, app_name);
}

bool AppFile::exists_for_app(StringView app_name)
{
    return FileSystem::exists(app_file_path_for_app(app_name));
}

NonnullRefPtr<AppFile> AppFile::get_for_app(StringView app_name)
{
    return open(app_file_path_for_app(app_name));
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
        if (!name.ends_with(".af"sv))
            continue;
        auto path = DeprecatedString::formatted("{}/{}", directory, name);
        auto af = AppFile::open(path);
        if (!af->is_valid())
            continue;
        callback(af);
    }
}

AppFile::AppFile(StringView path)
    : m_config(Core::ConfigFile::open(path).release_value_but_fixme_should_propagate_errors())
    , m_valid(validate())
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

DeprecatedString AppFile::name() const
{
    auto name = m_config->read_entry("App", "Name").trim_whitespace();
    VERIFY(!name.is_empty());
    return name;
}

DeprecatedString AppFile::executable() const
{
    auto executable = m_config->read_entry("App", "Executable").trim_whitespace();
    VERIFY(!executable.is_empty());
    return executable;
}

DeprecatedString AppFile::description() const
{
    return m_config->read_entry("App", "Description").trim_whitespace();
}

DeprecatedString AppFile::category() const
{
    return m_config->read_entry("App", "Category").trim_whitespace();
}

DeprecatedString AppFile::working_directory() const
{
    return m_config->read_entry("App", "WorkingDirectory").trim_whitespace();
}

DeprecatedString AppFile::icon_path() const
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

bool AppFile::requires_root() const
{
    return m_config->read_bool_entry("App", "RequiresRoot", false);
}

bool AppFile::exclude_from_system_menu() const
{
    return m_config->read_bool_entry("App", "ExcludeFromSystemMenu", false);
}

Vector<DeprecatedString> AppFile::launcher_mime_types() const
{
    Vector<DeprecatedString> mime_types;
    for (auto& entry : m_config->read_entry("Launcher", "MimeTypes").split(',')) {
        entry = entry.trim_whitespace();
        if (!entry.is_empty())
            mime_types.append(entry);
    }
    return mime_types;
}

Vector<DeprecatedString> AppFile::launcher_file_types() const
{
    Vector<DeprecatedString> file_types;
    for (auto& entry : m_config->read_entry("Launcher", "FileTypes").split(',')) {
        entry = entry.trim_whitespace();
        if (!entry.is_empty())
            file_types.append(entry);
    }
    return file_types;
}

Vector<DeprecatedString> AppFile::launcher_protocols() const
{
    Vector<DeprecatedString> protocols;
    for (auto& entry : m_config->read_entry("Launcher", "Protocols").split(',')) {
        entry = entry.trim_whitespace();
        if (!entry.is_empty())
            protocols.append(entry);
    }
    return protocols;
}

bool AppFile::spawn(ReadonlySpan<StringView> arguments) const
{
    if (!is_valid())
        return false;

    auto pid = Core::Process::spawn(executable(), arguments, working_directory());
    if (pid.is_error())
        return false;

    return true;
}

}
