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

#pragma once

#include <LibCore/ConfigFile.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Icon.h>

namespace Desktop {

class AppFile : public RefCounted<AppFile> {
public:
    static constexpr const char* APP_FILES_DIRECTORY = "/res/apps";
    static NonnullRefPtr<AppFile> get_for_app(const StringView& app_name);
    static NonnullRefPtr<AppFile> open(const StringView& path);
    static void for_each(Function<void(NonnullRefPtr<AppFile>)>, const StringView& directory = APP_FILES_DIRECTORY);
    ~AppFile();

    bool is_valid() const { return m_valid; }
    String file_name() const { return m_config->file_name(); }

    String name() const;
    String executable() const;
    String category() const;
    Vector<String> launcher_file_types() const;
    Vector<String> launcher_protocols() const;

    GUI::Icon icon() const { return GUI::FileIconProvider::icon_for_path(executable()); };

private:
    explicit AppFile(const StringView& path);

    bool validate() const;

    RefPtr<Core::ConfigFile> m_config;
    bool m_valid { false };
};

}
