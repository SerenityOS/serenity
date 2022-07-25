/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/Types.h>
#include <LibAudio/FlacLoader.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Stream.h>
#include <LibTest/TestCase.h>

struct FlacTest : Test::TestCase {
    FlacTest(LexicalPath path)
        : Test::TestCase(
            String::formatted("flac_spec_test_{}", path.basename()), [this]() { run(); }, false)
        , m_path(std::move(path))
    {
    }

    void run() const
    {
        auto loader = Audio::FlacLoaderPlugin { m_path.string() };
        if (auto result = loader.initialize(); result.is_error()) {
            FAIL(String::formatted("{} (at {})", result.error().description, result.error().index));
            return;
        }

        while (true) {
            auto maybe_samples = loader.get_more_samples(2 * MiB);
            if (maybe_samples.is_error()) {
                FAIL(String::formatted("{} (at {})", maybe_samples.error().description, maybe_samples.error().index));
                return;
            }
            if (maybe_samples.value().is_empty())
                return;
        }
    }

    LexicalPath m_path;
};

struct DiscoverFLACTestsHack {
    DiscoverFLACTestsHack()
    {
        // FIXME: Also run (our own) tests in this directory.
        auto test_iterator = Core::DirIterator { "./SpecTests", Core::DirIterator::Flags::SkipParentAndBaseDir };

        while (test_iterator.has_next()) {
            auto file = LexicalPath { test_iterator.next_full_path() };
            if (file.extension() == "flac"sv) {
                Test::add_test_case_to_suite(make_ref_counted<FlacTest>(move(file)));
            }
        }
    }
};
// Hack taken from TEST_CASE; the above constructor will run as part of global initialization before the tests are actually executed
static struct DiscoverFLACTestsHack hack;
