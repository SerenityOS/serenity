/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibAudio/FlacLoader.h>
#include <LibCore/Directory.h>
#include <LibTest/TestCase.h>

struct DiscoverFLACTestsHack {
    DiscoverFLACTestsHack()
    {
        // FIXME: Also run (our own) tests in this directory.
        (void)Core::Directory::for_each_entry("./FLAC/SpecTests"sv, Core::DirIterator::Flags::SkipParentAndBaseDir, [](auto const& entry, auto const& directory) -> ErrorOr<IterationDecision> {
            auto path = LexicalPath::join(directory.path().string(), entry.name);
            if (path.extension() == "flac"sv) {
                Test::add_test_case_to_suite(adopt_ref(*new ::Test::TestCase(
                    DeprecatedString::formatted("flac_spec_test_{}", path.basename()),
                    [path = move(path)]() {
                        auto result = Audio::FlacLoaderPlugin::create(path.string());
                        if (result.is_error()) {
                            FAIL(DeprecatedString::formatted("{}", result.error()));
                            return;
                        }

                        auto loader = result.release_value();

                        while (true) {
                            auto maybe_samples = loader->load_chunks(2 * MiB);
                            if (maybe_samples.is_error()) {
                                FAIL(DeprecatedString::formatted("{}", maybe_samples.error()));
                                return;
                            }
                            maybe_samples.value().remove_all_matching([](auto& chunk) { return chunk.is_empty(); });
                            if (maybe_samples.value().is_empty())
                                return;
                        }
                    },
                    false)));
            }
            return IterationDecision::Continue;
        });
    }
};
// Hack taken from TEST_CASE; the above constructor will run as part of global initialization before the tests are actually executed
static struct DiscoverFLACTestsHack hack;
