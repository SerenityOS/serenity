/*
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Providers.h"
#include "FuzzyMatch.h"
#include <LibGUI/Clipboard.h>
#include <LibGUI/FileIconProvider.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Lexer.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace Assistant {

void AppResult::activate() const
{
    m_app_file->spawn();
}

void CalculatorResult::activate() const
{
    GUI::Clipboard::the().set_plain_text(title());
}

void AppProvider::query(String const& query, Function<void(Vector<NonnullRefPtr<Result>>)> on_complete)
{
    if (query.starts_with("="))
        return;

    Vector<NonnullRefPtr<Result>> results;

    Desktop::AppFile::for_each([&](NonnullRefPtr<Desktop::AppFile> app_file) {
        auto match_result = fuzzy_match(query, app_file->name());
        if (!match_result.matched)
            return;

        auto icon = GUI::FileIconProvider::icon_for_executable(app_file->executable());
        results.append(adopt_ref(*new AppResult(icon.bitmap_for_size(16), app_file->name(), app_file, match_result.score)));
    });

    on_complete(results);
}

void CalculatorProvider::query(String const& query, Function<void(Vector<NonnullRefPtr<Result>>)> on_complete)
{
    if (!query.starts_with("="))
        return;

    auto vm = JS::VM::create();
    auto interpreter = JS::Interpreter::create<JS::GlobalObject>(*vm);

    auto source_code = query.substring(1);
    auto parser = JS::Parser(JS::Lexer(source_code));
    auto program = parser.parse_program();
    if (parser.has_errors())
        return;

    interpreter->run(interpreter->global_object(), *program);
    if (interpreter->exception())
        return;

    auto result = interpreter->vm().last_value();
    String calculation;
    if (!result.is_number()) {
        calculation = "0";
    } else {
        calculation = result.to_string_without_side_effects();
    }

    Vector<NonnullRefPtr<Result>> results;
    results.append(adopt_ref(*new CalculatorResult(calculation)));
    on_complete(results);
}

}
