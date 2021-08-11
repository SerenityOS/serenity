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

    outln("Definitions:");
    for (auto& definition : cpp.definitions()) {
        if (definition.value.parameters.is_empty())
            outln("{}: {}", definition.key, definition.value.value);
        else
            outln("{}({}): {}", definition.key, String::join(",", definition.value.parameters), definition.value.value);
    }

    outln("");

    for (auto& token : tokens) {
        dbgln("{}", token.to_string());
    }
}
