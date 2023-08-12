/*
 * Copyright (c) 2021, Spencer Dixon <spencercdixon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Providers.h"
#include <AK/BinaryHeap.h>
#include <AK/FuzzyMatch.h>
#include <AK/LexicalPath.h>
#include <AK/URL.h>
#include <LibCore/Directory.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/Process.h>
#include <LibCore/StandardPaths.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/FileIconProvider.h>
#include <LibJS/Bytecode/Interpreter.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Script.h>
#include <errno.h>
#include <fcntl.h>
#include <serenity.h>
#include <spawn.h>
#include <sys/stat.h>
#include <unistd.h>

namespace Assistant {

void AppResult::activate() const
{
    if (chdir(Core::StandardPaths::home_directory().characters()) < 0) {
        perror("chdir");
        exit(1);
    }

    auto arguments_list = m_arguments.split_view(' ');
    m_app_file->spawn(arguments_list.span());
}

void CalculatorResult::activate() const
{
    GUI::Clipboard::the().set_plain_text(title());
}

void FileResult::activate() const
{
    Desktop::Launcher::open(URL::create_with_file_scheme(title()));
}

void TerminalResult::activate() const
{
    // FIXME: This should be a GUI::Process::spawn_or_show_error(), however this is a
    // Assistant::Result object, which does not have access to the application's GUI::Window* pointer
    // (which spawn_or_show_error() needs in case it has to open a error message box).
    (void)Core::Process::spawn("/bin/Terminal"sv, Array { "-k", "-e", title().characters() });
}

void URLResult::activate() const
{
    Desktop::Launcher::open(URL::create_with_url_or_path(title()));
}

AppProvider::AppProvider()
{
    Desktop::AppFile::for_each([this](NonnullRefPtr<Desktop::AppFile> app_file) {
        m_app_file_cache.append(move(app_file));
    });
}

void AppProvider::query(DeprecatedString const& query, Function<void(Vector<NonnullRefPtr<Result>>)> on_complete)
{
    if (query.starts_with('=') || query.starts_with('$'))
        return;

    Vector<NonnullRefPtr<Result>> results;

    for (auto const& app_file : m_app_file_cache) {
        auto query_and_arguments = query.split_limit(' ', 2);
        auto app_name = query_and_arguments.is_empty() ? query : query_and_arguments[0];
        auto arguments = query_and_arguments.size() < 2 ? DeprecatedString::empty() : query_and_arguments[1];
        auto match_result = fuzzy_match(app_name, app_file->name());
        if (!match_result.matched)
            continue;

        auto icon = GUI::FileIconProvider::icon_for_executable(app_file->executable());
        results.append(make_ref_counted<AppResult>(icon.bitmap_for_size(16), app_file->name(), DeprecatedString::empty(), app_file, arguments, match_result.score));
    };

    on_complete(move(results));
}

void CalculatorProvider::query(DeprecatedString const& query, Function<void(Vector<NonnullRefPtr<Result>>)> on_complete)
{
    if (!query.starts_with('='))
        return;

    auto vm = JS::VM::create().release_value_but_fixme_should_propagate_errors();
    auto root_execution_context = JS::create_simple_execution_context<JS::GlobalObject>(*vm);

    auto source_code = query.substring(1);
    auto parse_result = JS::Script::parse(source_code, *root_execution_context->realm);
    if (parse_result.is_error())
        return;

    auto completion = vm->bytecode_interpreter().run(parse_result.value());
    if (completion.is_error())
        return;

    auto result = completion.release_value();
    DeprecatedString calculation;
    if (!result.is_number()) {
        calculation = "0";
    } else {
        calculation = result.to_string_without_side_effects().to_deprecated_string();
    }

    Vector<NonnullRefPtr<Result>> results;
    results.append(make_ref_counted<CalculatorResult>(calculation));
    on_complete(move(results));
}

Gfx::Bitmap const* FileResult::bitmap() const
{
    return GUI::FileIconProvider::icon_for_path(title()).bitmap_for_size(16);
}

FileProvider::FileProvider()
{
    build_filesystem_cache();
}

void FileProvider::query(DeprecatedString const& query, Function<void(Vector<NonnullRefPtr<Result>>)> on_complete)
{
    build_filesystem_cache();

    if (m_fuzzy_match_work)
        m_fuzzy_match_work->cancel();

    m_fuzzy_match_work = Threading::BackgroundAction<Optional<Vector<NonnullRefPtr<Result>>>>::construct(
        [this, query](auto& task) -> Optional<Vector<NonnullRefPtr<Result>>> {
            BinaryHeap<int, DeprecatedString, MAX_SEARCH_RESULTS> sorted_results;

            for (auto& path : m_full_path_cache) {
                if (task.is_canceled())
                    return {};

                auto match_result = fuzzy_match(query, path);
                if (!match_result.matched)
                    continue;
                if (match_result.score < 0)
                    continue;

                if (sorted_results.size() < MAX_SEARCH_RESULTS || match_result.score > sorted_results.peek_min_key()) {
                    if (sorted_results.size() == MAX_SEARCH_RESULTS)
                        sorted_results.pop_min();

                    sorted_results.insert(match_result.score, path);
                }
            }

            Vector<NonnullRefPtr<Result>> results;
            results.ensure_capacity(sorted_results.size());
            while (!sorted_results.is_empty()) {
                auto score = sorted_results.peek_min_key();
                auto path = sorted_results.pop_min();
                results.append(make_ref_counted<FileResult>(path, score));
            }
            return results;
        },
        [on_complete = move(on_complete)](auto results) -> ErrorOr<void> {
            if (results.has_value())
                on_complete(move(results.value()));

            return {};
        },
        [](auto) {
            // Ignore cancellation errors.
        });
}

void FileProvider::build_filesystem_cache()
{
    if (m_full_path_cache.size() > 0 || m_building_cache)
        return;

    m_building_cache = true;
    m_work_queue.enqueue("/");

    (void)Threading::BackgroundAction<int>::construct(
        [this, strong_ref = NonnullRefPtr(*this)](auto&) {
            DeprecatedString slash = "/";
            auto timer = Core::ElapsedTimer::start_new();
            while (!m_work_queue.is_empty()) {
                auto base_directory = m_work_queue.dequeue();

                if (base_directory.template is_one_of("/dev"sv, "/proc"sv, "/sys"sv))
                    continue;

                // FIXME: Propagate errors.
                (void)Core::Directory::for_each_entry(base_directory, Core::DirIterator::SkipDots, [&](auto const& entry, auto const& directory) -> ErrorOr<IterationDecision> {
                    struct stat st = {};
                    if (fstatat(directory.fd(), entry.name.characters(), &st, AT_SYMLINK_NOFOLLOW) < 0) {
                        perror("fstatat");
                        return IterationDecision::Continue;
                    }

                    if (S_ISLNK(st.st_mode))
                        return IterationDecision::Continue;

                    auto full_path = LexicalPath::join(directory.path().string(), entry.name).string();
                    m_full_path_cache.append(full_path);

                    if (S_ISDIR(st.st_mode)) {
                        m_work_queue.enqueue(full_path);
                    }
                    return IterationDecision::Continue;
                });
            }
            dbgln("Built cache in {} ms", timer.elapsed());
            return 0;
        },
        [this](auto) -> ErrorOr<void> {
            m_building_cache = false;
            return {};
        });
}

void TerminalProvider::query(DeprecatedString const& query, Function<void(Vector<NonnullRefPtr<Result>>)> on_complete)
{
    if (!query.starts_with('$'))
        return;

    auto command = query.substring(1).trim_whitespace();

    Vector<NonnullRefPtr<Result>> results;
    results.append(make_ref_counted<TerminalResult>(move(command)));
    on_complete(move(results));
}

void URLProvider::query(DeprecatedString const& query, Function<void(Vector<NonnullRefPtr<Result>>)> on_complete)
{
    if (query.is_empty() || query.starts_with('=') || query.starts_with('$'))
        return;

    URL url = URL(query);

    if (url.scheme().is_empty())
        url.set_scheme("http"_string);
    if (url.host().has<Empty>() || url.host() == String {})
        url.set_host(String::from_deprecated_string(query).release_value_but_fixme_should_propagate_errors());
    if (url.path_segment_count() == 0)
        url.set_paths({ "" });

    if (!url.is_valid())
        return;

    Vector<NonnullRefPtr<Result>> results;
    results.append(make_ref_counted<URLResult>(url));
    on_complete(results);
}

}
