/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Endian.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/ICC/BinaryWriter.h>
#include <LibGfx/ICC/Profile.h>
#include <LibGfx/ICC/WellKnownProfiles.h>
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

TEST_CASE(built_in_sRGB)
{
    auto sRGB = MUST(Gfx::ICC::sRGB());
    auto serialized_bytes = MUST(Gfx::ICC::encode(sRGB));

    // We currently exactly match the curve in GIMP's built-in sRGB profile. It's a type 3 'para' curve with 5 parameters.
    u32 para[] = { 0x70617261, 0x00000000, 0x00030000, 0x00026666, 0x0000F2A7, 0x00000D59, 0x000013D0, 0x00000A5B };
    for (u32& i : para)
        i = AK::convert_between_host_and_big_endian(i);
    EXPECT(memmem(serialized_bytes.data(), serialized_bytes.size(), para, sizeof(para)) != nullptr);

    // We currently exactly match the chromatic adaptation matrix in GIMP's (and other's) built-in sRGB profile.
    u32 sf32[] = { 0x73663332, 0x00000000, 0x00010C42, 0x000005DE, 0xFFFFF325, 0x00000793, 0x0000FD90, 0xFFFFFBA1, 0xFFFFFDA2, 0x000003DC, 0x0000C06E };
    for (u32& i : sf32)
        i = AK::convert_between_host_and_big_endian(i);
    EXPECT(memmem(serialized_bytes.data(), serialized_bytes.size(), sf32, sizeof(sf32)) != nullptr);
}
