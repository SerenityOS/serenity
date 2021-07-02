/*
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Providers.h"
#include "FuzzyMatch.h"
#include <AK/URL.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/StandardPaths.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/FileIconProvider.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Lexer.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <unistd.h>

namespace Assistant {

void AppResult::activate() const
{
    if (chdir(Core::StandardPaths::home_directory().characters()) < 0) {
        perror("chdir");
        exit(1);
    }

    m_app_file->spawn();
}

void CalculatorResult::activate() const
{
    GUI::Clipboard::the().set_plain_text(title());
}

void FileResult::activate() const
{
    Desktop::Launcher::open(URL::create_with_file_protocol(title()));
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
        results.append(adopt_ref(*new AppResult(icon.bitmap_for_size(16), app_file->name(), {}, app_file, match_result.score)));
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

void FileProvider::query(const String& query, Function<void(Vector<NonnullRefPtr<Result>>)> on_complete)
{
    build_filesystem_cache();

    if (m_fuzzy_match_work)
        m_fuzzy_match_work->cancel();

    m_fuzzy_match_work = Threading::BackgroundAction<Vector<NonnullRefPtr<Result>>>::create([this, query](auto& task) {
        Vector<NonnullRefPtr<Result>> results;

        for (auto& path : m_full_path_cache) {
            if (task.is_cancelled())
                return results;

            auto match_result = fuzzy_match(query, path);
            if (!match_result.matched)
                continue;
            if (match_result.score < 0)
                continue;

            results.append(adopt_ref(*new FileResult(path, match_result.score)));
        }
        return results; }, [on_complete = move(on_complete)](auto results) { on_complete(results); });
}

void FileProvider::build_filesystem_cache()
{
    if (m_full_path_cache.size() > 0 || m_building_cache)
        return;

    m_building_cache = true;
    m_work_queue.enqueue("/");

    Threading::BackgroundAction<int>::create([this](auto&) {
        while (!m_work_queue.is_empty()) {
            auto start = m_work_queue.dequeue();
            Core::DirIterator di(start, Core::DirIterator::SkipDots);

            while (di.has_next()) {
                auto path = di.next_full_path();
                struct stat st = {};
                if (lstat(path.characters(), &st) < 0) {
                    perror("stat");
                    continue;
                }

                if (S_ISLNK(st.st_mode))
                    continue;

                m_full_path_cache.append(path);

                if (S_ISDIR(st.st_mode)) {
                    m_work_queue.enqueue(path);
                }
            }
        }

        return 0; }, [this](auto) { m_building_cache = false; });
}

}
