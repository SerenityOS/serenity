/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/ConfigFile.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>

namespace Desktop {

class AppFile : public RefCounted<AppFile> {
public:
    static constexpr auto APP_FILES_DIRECTORY = "/res/apps"sv;

    static bool exists_for_app(StringView app_name);
    static ByteString file_for_app(StringView app_name);
    static ByteString app_file_path_for_app(StringView app_name);

    static NonnullRefPtr<AppFile> get_for_app(StringView app_name);
    static NonnullRefPtr<AppFile> open(StringView path);
    static void for_each(Function<void(NonnullRefPtr<AppFile>)>, StringView directory = APP_FILES_DIRECTORY);
    ~AppFile() = default;

    bool is_valid() const { return m_valid; }
    ByteString filename() const { return m_config->filename(); }

    ByteString name() const;
    ByteString menu_name() const;
    ByteString executable() const;
    Vector<ByteString> arguments() const;
    ByteString category() const;
    ByteString description() const;
    ByteString working_directory() const;
    ByteString icon_path() const;
    GUI::Icon icon() const;
    bool run_in_terminal() const;
    bool requires_root() const;
    bool exclude_from_system_menu() const;
    Vector<ByteString> launcher_mime_types() const;
    Vector<ByteString> launcher_file_types() const;
    Vector<ByteString> launcher_protocols() const;
    ErrorOr<void> spawn(ReadonlySpan<StringView> arguments = {}) const;
    ErrorOr<void> spawn_with_escalation(ReadonlySpan<StringView> arguments = {}) const;
    void spawn_with_escalation_or_show_error(GUI::Window&, ReadonlySpan<StringView> arguments = {}) const;

private:
    explicit AppFile(StringView path);

    bool validate() const;

    RefPtr<Core::ConfigFile> m_config;
    bool m_valid { false };
};

}
