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

namespace Desktop {

class AppFile : public RefCounted<AppFile> {
public:
    static constexpr auto APP_FILES_DIRECTORY = "/res/apps"sv;

    static NonnullRefPtr<AppFile> get_for_app(StringView app_name);
    static NonnullRefPtr<AppFile> open(StringView path);
    static void for_each(Function<void(NonnullRefPtr<AppFile>)>, StringView directory = APP_FILES_DIRECTORY);
    ~AppFile() = default;

    bool is_valid() const { return m_valid; }
    DeprecatedString filename() const { return m_config->filename(); }

    DeprecatedString name() const;
    DeprecatedString executable() const;
    DeprecatedString category() const;
    DeprecatedString description() const;
    DeprecatedString working_directory() const;
    DeprecatedString icon_path() const;
    GUI::Icon icon() const;
    bool run_in_terminal() const;
    bool requires_root() const;
    Vector<DeprecatedString> launcher_mime_types() const;
    Vector<DeprecatedString> launcher_file_types() const;
    Vector<DeprecatedString> launcher_protocols() const;
    bool spawn() const;

private:
    explicit AppFile(StringView path);

    bool validate() const;

    RefPtr<Core::ConfigFile> m_config;
    bool m_valid { false };
};

}
