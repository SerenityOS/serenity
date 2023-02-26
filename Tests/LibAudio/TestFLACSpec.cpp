/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibAudio/FlacLoader.h>
#include <LibCore/Directory.h>
#include <LibTest/TestCase.h>

struct FlacTest : Test::TestCase {
    FlacTest(LexicalPath path)
        : Test::TestCase(
            DeprecatedString::formatted("flac_spec_test_{}", path.basename()), [this]() { run(); }, false)
        , m_path(move(path))
    {
    }

    void run() const
    {
        auto result = Audio::FlacLoaderPlugin::create(m_path.string());
        if (result.is_error()) {
            FAIL(DeprecatedString::formatted("{} (at {})", result.error().description, result.error().index));
            return;
        }

        auto loader = result.release_value();

        while (true) {
            auto maybe_samples = loader->load_chunks(2 * MiB);
            if (maybe_samples.is_error()) {
                FAIL(DeprecatedString::formatted("{} (at {})", maybe_samples.error().description, maybe_samples.error().index));
                return;
            }
            maybe_samples.value().remove_all_matching([](auto& chunk) { return chunk.is_empty(); });
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
        (void)Core::Directory::for_each_entry("./SpecTests"sv, Core::DirIterator::Flags::SkipParentAndBaseDir, [](auto const& entry, auto const& directory) -> ErrorOr<IterationDecision> {
            auto path = LexicalPath::join(directory.path().string(), entry.name);
            if (path.extension() == "flac"sv)
                Test::add_test_case_to_suite(make_ref_counted<FlacTest>(path));
            return IterationDecision::Continue;
        });
    }
};
// Hack taken from TEST_CASE; the above constructor will run as part of global initialization before the tests are actually executed
static struct DiscoverFLACTestsHack hack;
