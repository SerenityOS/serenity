/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "EditorWrapper.h"
#include "LanguageClients/ConnectionsToServer.h"
#include "Project.h"
#include <AK/ByteString.h>
#include <LibGUI/TextEditor.h>

namespace HackStudio {

GUI::TextEditor& current_editor();
void open_file(ByteString const&);
RefPtr<EditorWrapper> current_editor_wrapper();
void open_file(ByteString const&, size_t line, size_t column);
Project& project();
ByteString currently_open_file();
void set_current_editor_wrapper(RefPtr<EditorWrapper>);
void update_editor_window_title();
void for_each_open_file(Function<void(ProjectFile const&)>);
bool semantic_syntax_highlighting_is_enabled();

class Locator;
Locator& locator();

}
