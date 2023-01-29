/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../AutoCompleteResponse.h"
#include "FileDB.h"
#include <AK/HashMap.h>
#include <AK/LexicalPath.h>
#include <LibCodeComprehension/CodeComprehensionEngine.h>
#include <LibIPC/ConnectionFromClient.h>

#include <Userland/DevTools/HackStudio/LanguageServers/LanguageClientEndpoint.h>
#include <Userland/DevTools/HackStudio/LanguageServers/LanguageServerEndpoint.h>

namespace LanguageServers {

class ConnectionFromClient : public IPC::ConnectionFromClient<LanguageClientEndpoint, LanguageServerEndpoint> {
public:
    explicit ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket>);
    ~ConnectionFromClient() override = default;

    virtual void die() override;

protected:
    virtual void greet(DeprecatedString const&) override;
    virtual void file_opened(DeprecatedString const&, IPC::File const&) override;
    virtual void file_edit_insert_text(DeprecatedString const&, DeprecatedString const&, i32, i32) override;
    virtual void file_edit_remove_text(DeprecatedString const&, i32, i32, i32, i32) override;
    virtual void set_file_content(DeprecatedString const&, DeprecatedString const&) override;
    virtual void auto_complete_suggestions(CodeComprehension::ProjectLocation const&) override;
    virtual void find_declaration(CodeComprehension::ProjectLocation const&) override;
    virtual void get_parameters_hint(CodeComprehension::ProjectLocation const&) override;
    virtual void get_tokens_info(DeprecatedString const&) override;

    FileDB m_filedb;
    OwnPtr<CodeComprehension::CodeComprehensionEngine> m_autocomplete_engine;
};

}
