/*
 * Copyright (c) 2023, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/MemoryStream.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/ImageFormats/ISOBMFF/Reader.h>

TEST_CASE(parse_animated_avif)
{
    auto file = MUST(Core::MappedFile::map("./test-inputs/loop_forever.avif"sv));
    auto reader = MUST(Gfx::ISOBMFF::Reader::create(MUST(try_make<FixedMemoryStream>(file->bytes()))));
    auto boxes = MUST(reader.read_entire_file());

    for (auto& box : boxes)
        box->dump();

    VERIFY(boxes.size() == 4);
    VERIFY(boxes[0]->box_type() == Gfx::ISOBMFF::BoxType::FileTypeBox);
    auto& file_type_box = static_cast<Gfx::ISOBMFF::FileTypeBox&>(*boxes[0]);
    VERIFY(file_type_box.major_brand == Gfx::ISOBMFF::BrandIdentifier::avis);
    VERIFY(file_type_box.minor_version == 0);
    Vector<Gfx::ISOBMFF::BrandIdentifier, 7> expected_compatible_brands = {
        Gfx::ISOBMFF::BrandIdentifier::avif,
        Gfx::ISOBMFF::BrandIdentifier::avis,
        Gfx::ISOBMFF::BrandIdentifier::msf1,
        Gfx::ISOBMFF::BrandIdentifier::iso8,
        Gfx::ISOBMFF::BrandIdentifier::mif1,
        Gfx::ISOBMFF::BrandIdentifier::miaf,
        Gfx::ISOBMFF::BrandIdentifier::MA1A,
    };
    VERIFY(file_type_box.compatible_brands == expected_compatible_brands);
}
