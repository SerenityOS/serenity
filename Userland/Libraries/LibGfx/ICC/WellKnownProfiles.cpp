/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/ICC/Profile.h>
#include <LibGfx/ICC/Tags.h>
#include <LibGfx/ICC/WellKnownProfiles.h>
#include <time.h>

namespace Gfx::ICC {

static ProfileHeader rgb_header()
{
    ProfileHeader header;
    header.version = Version(4, 0x40);
    header.device_class = DeviceClass::DisplayDevice;
    header.data_color_space = ColorSpace::RGB;
    header.connection_space = ColorSpace::PCSXYZ;
    header.creation_timestamp = time(NULL);
    header.rendering_intent = RenderingIntent::Perceptual;
    header.pcs_illuminant = XYZ { 0.9642, 1.0, 0.8249 };
    return header;
}

static ErrorOr<NonnullRefPtr<MultiLocalizedUnicodeTagData>> en_US(StringView text)
{
    Vector<MultiLocalizedUnicodeTagData::Record> records;
    TRY(records.try_append({ ('e' << 8) | 'n', ('U' << 8) | 'S', TRY(String::from_utf8(text)) }));
    return try_make_ref_counted<MultiLocalizedUnicodeTagData>(0, 0, records);
}

static ErrorOr<NonnullRefPtr<XYZTagData>> XYZ_data(XYZ xyz)
{
    Vector<XYZ> xyzs;
    TRY(xyzs.try_append(xyz));
    return try_make_ref_counted<XYZTagData>(0, 0, move(xyzs));
}

ErrorOr<NonnullRefPtr<Profile>> sRGB()
{
    // Returns an sRGB profile.
    // https://en.wikipedia.org/wiki/SRGB

    // FIXME: There's a surprising amount of variety in sRGB ICC profiles in the wild.
    //        Explain why, and why this picks the numbers it does.

    auto header = rgb_header();

    OrderedHashMap<TagSignature, NonnullRefPtr<TagData>> tag_table;

    TRY(tag_table.try_set(profileDescriptionTag, TRY(en_US("SerenityOS sRGB"sv))));
    TRY(tag_table.try_set(copyrightTag, TRY(en_US("Public Domain"sv))));

    // Transfer function.
    // Numbers from https://en.wikipedia.org/wiki/SRGB#From_sRGB_to_CIE_XYZ
    Array<S15Fixed16, 7> curve_parameters = { 2.4, 1 / 1.055, 0.055 / 1.055, 1 / 12.92, 0.04045 };
    auto curve = TRY(try_make_ref_counted<ParametricCurveTagData>(0, 0, ParametricCurveTagData::FunctionType::sRGB, curve_parameters));
    TRY(tag_table.try_set(redTRCTag, curve));
    TRY(tag_table.try_set(greenTRCTag, curve));
    TRY(tag_table.try_set(blueTRCTag, curve));

    // Chromatic adaptation matrix, chromacities and whitepoint.
    // FIXME: Actual values for chromatic adaptation matrix and chromacities.
    TRY(tag_table.try_set(mediaWhitePointTag, TRY(XYZ_data(XYZ { 0, 0, 0 }))));
    TRY(tag_table.try_set(redMatrixColumnTag, TRY(XYZ_data(XYZ { 0, 0, 0 }))));
    TRY(tag_table.try_set(greenMatrixColumnTag, TRY(XYZ_data(XYZ { 0, 0, 0 }))));
    TRY(tag_table.try_set(blueMatrixColumnTag, TRY(XYZ_data(XYZ { 0, 0, 0 }))));

    Vector<S15Fixed16, 9> chromatic_adaptation_matrix = { 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    TRY(tag_table.try_set(chromaticAdaptationTag, TRY(try_make_ref_counted<S15Fixed16ArrayTagData>(0, 0, move(chromatic_adaptation_matrix)))));

    return Profile::create(header, move(tag_table));
}

}
