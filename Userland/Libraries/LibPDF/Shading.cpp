/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/ColorSpace.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Document.h>
#include <LibPDF/Shading.h>

namespace PDF {

namespace {

// TABLE 4.28 Entries common to all shading dictionaries
struct CommonEntries {
    // "(Required) The color space in which color values are expressed. This may be
    //  any device, CIE-based, or special color space except a Pattern space."
    AK::NonnullRefPtr<ColorSpace> color_space;

    // "(Optional) An array of color components appropriate to the color space,
    //  specifying a single background color value. If present, this color is used, be-
    //  fore any painting operation involving the shading, to fill those portions of the
    //  area to be painted that lie outside the bounds of the shading object
    //  Note: The background color is applied only when the shading is used as part of
    //  a shading pattern, not when it is painted directly with the sh operator."
    // We currently don't support shading patterns yet, so we don't use this yet.
    Optional<Vector<float>> background {};

    // "(Optional) An array of four numbers giving the left, bottom, right, and top
    //  coordinates, respectively, of the shading’s bounding box. The coordinates are
    //  interpreted in the shading’s target coordinate space. If present, this bounding
    //  box is applied as a temporary clipping boundary when the shading is painted,
    //  in addition to the current clipping path and any other clipping boundaries in
    //  effect at that time."
    Optional<Gfx::FloatRect> b_box {};

    // "(Optional) A flag indicating whether to filter the shading function to prevent
    //  aliasing artifacts. [...] Anti-aliasing
    //  may not be implemented on some output devices, in which case this flag is
    //  ignored. Default value: false."
    // We currently ignore this.
    bool anti_alias { false };
};

PDFErrorOr<CommonEntries> read_common_entries(Document* document, DictObject const& shading_dict, Renderer& renderer)
{
    auto color_space_object = TRY(shading_dict.get_object(document, CommonNames::ColorSpace));
    auto color_space = TRY(ColorSpace::create(document, move(color_space_object), renderer));

    CommonEntries common_entries { .color_space = color_space };

    if (shading_dict.contains(CommonNames::Background)) {
        auto background_array = TRY(shading_dict.get_array(document, CommonNames::Background));
        Vector<float> background;
        for (auto const& value : background_array->elements())
            background.append(value.to_float());
        common_entries.background = move(background);
    }

    if (shading_dict.contains(CommonNames::BBox)) {
        auto bbox_array = TRY(shading_dict.get_array(document, CommonNames::BBox));
        if (bbox_array->size() != 4)
            return Error::malformed_error("BBox must have 4 elements");
        Gfx::FloatRect bbox {
            bbox_array->at(0).to_float(),
            bbox_array->at(1).to_float(),
            bbox_array->at(2).to_float(),
            bbox_array->at(3).to_float(),
        };
        common_entries.b_box = bbox;
    }

    if (shading_dict.contains(CommonNames::AntiAlias))
        common_entries.anti_alias = TRY(document->resolve(shading_dict.get_value(CommonNames::AntiAlias))).get<bool>();

    return common_entries;
}

}

PDFErrorOr<NonnullRefPtr<Shading>> Shading::create(Document* document, NonnullRefPtr<Object> shading_dict_or_stream, Renderer& renderer)
{
    // "Shading types 4 to 7 are defined by a stream containing descriptive data charac-
    //  terizing the shading’s gradient fill. In these cases, the shading dictionary is also a
    //  stream dictionary and can contain any of the standard entries common to all
    //  streams"
    auto shading_dict = TRY([&]() -> PDFErrorOr<NonnullRefPtr<DictObject>> {
        if (shading_dict_or_stream->is<DictObject>())
            return shading_dict_or_stream->cast<DictObject>();
        if (shading_dict_or_stream->is<StreamObject>())
            return shading_dict_or_stream->cast<StreamObject>()->dict();
        return Error::malformed_error("Shading must be a dictionary or stream");
    }());

    int shading_type = TRY(document->resolve(shading_dict->get_value(CommonNames::ShadingType))).to_int();
    auto common_entries = TRY(read_common_entries(document, *shading_dict, renderer));

    switch (shading_type) {
    case 1:
        return Error::rendering_unsupported_error("Function-based shading not yet implemented");
    case 2:
        return Error::rendering_unsupported_error("Axial shading not yet implemented");
    case 3:
        return Error::rendering_unsupported_error("Radial shading not yet implemented");
    case 4:
        return Error::rendering_unsupported_error("Free-form Gouraud-shaded triangle mesh not yet implemented");
    case 5:
        return Error::rendering_unsupported_error("Lattice-form Gouraud-shaded triangle mesh not yet implemented");
    case 6:
        return Error::rendering_unsupported_error("Coons patch mesh not yet implemented");
    case 7:
        return Error::rendering_unsupported_error("Tensor-product patch mesh not yet implemented");
    }
    dbgln("Shading type {}", shading_type);
    return Error::malformed_error("Invalid shading type");
}

}
