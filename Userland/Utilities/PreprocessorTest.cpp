/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/File.h>
#include <LibCpp/Preprocessor.h>

int main(int, char**)
{
    auto file = Core::File::construct("/home/anon/Source/little/other.h");
    if (!file->open(Core::OpenMode::ReadOnly)) {
        perror("open");
        exit(1);
    }
    auto content = file->read_all();
    Cpp::Preprocessor cpp("other.h", StringView { content });
    auto tokens = cpp.process_and_lex();
    for (auto& token : tokens) {
        dbgln("{}", token.to_string());
    }
}
