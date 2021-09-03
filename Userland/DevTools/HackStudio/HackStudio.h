/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "EditorWrapper.h"
#include "LanguageClients/ServerConnections.h"
#include "Project.h"
#include <YAK/String.h>
#include <LibGUI/TextEditor.h>

namespace HackStudio {

GUI::TextEditor& current_editor();
void open_file(const String&);
RefPtr<EditorWrapper> current_editor_wrapper();
void open_file(const String&, size_t line, size_t column);
Project& project();
String currently_open_file();
void set_current_editor_wrapper(RefPtr<EditorWrapper>);

class Locator;
Locator& locator();

}
