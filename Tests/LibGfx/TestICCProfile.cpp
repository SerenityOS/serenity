/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/MappedFile.h>
#include <LibGfx/ICC/BinaryWriter.h>
#include <LibGfx/ICC/Profile.h>
#include <LibGfx/JPEGLoader.h>
#include <LibGfx/PNGLoader.h>
#include <LibGfx/WebPLoader.h>
#include <LibTest/TestCase.h>

#ifdef AK_OS_SERENITY
#    define TEST_INPUT(x) ("/usr/Tests/LibGfx/test-inputs/" x)
#else
#    define TEST_INPUT(x) ("test-inputs/" x)
#endif

TEST_CASE(png)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("icc-v2.png"sv)));
    auto png = MUST(Gfx::PNGImageDecoderPlugin::create(file->bytes()));
    EXPECT(png->initialize());
    auto icc_bytes = MUST(png->icc_data());
    EXPECT(icc_bytes.has_value());

    auto icc_profile = MUST(Gfx::ICC::Profile::try_load_from_externally_owned_memory(icc_bytes.value()));
    EXPECT(icc_profile->is_v2());
}

TEST_CASE(jpg)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("icc-v4.jpg"sv)));
    auto jpg = MUST(Gfx::JPEGImageDecoderPlugin::create(file->bytes()));
    EXPECT(jpg->initialize());
    auto icc_bytes = MUST(jpg->icc_data());
    EXPECT(icc_bytes.has_value());

    auto icc_profile = MUST(Gfx::ICC::Profile::try_load_from_externally_owned_memory(icc_bytes.value()));
    EXPECT(icc_profile->is_v4());
}

TEST_CASE(webp_extended_lossless)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("extended-lossless.webp"sv)));
    auto webp = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));
    EXPECT(webp->initialize());
    auto icc_bytes = MUST(webp->icc_data());
    EXPECT(icc_bytes.has_value());

    auto icc_profile = MUST(Gfx::ICC::Profile::try_load_from_externally_owned_memory(icc_bytes.value()));
    EXPECT(icc_profile->is_v2());
}

TEST_CASE(webp_extended_lossy)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("extended-lossy.webp"sv)));
    auto webp = MUST(Gfx::WebPImageDecoderPlugin::create(file->bytes()));
    EXPECT(webp->initialize());
    auto icc_bytes = MUST(webp->icc_data());
    EXPECT(icc_bytes.has_value());

    auto icc_profile = MUST(Gfx::ICC::Profile::try_load_from_externally_owned_memory(icc_bytes.value()));
    EXPECT(icc_profile->is_v2());
}

TEST_CASE(serialize_icc)
{
    auto file = MUST(Core::MappedFile::map(TEST_INPUT("p3-v4.icc"sv)));
    auto icc_profile = MUST(Gfx::ICC::Profile::try_load_from_externally_owned_memory(file->bytes()));
    EXPECT(icc_profile->is_v4());

    auto serialized_bytes = MUST(Gfx::ICC::encode(*icc_profile));
    EXPECT_EQ(serialized_bytes, file->bytes());
}
