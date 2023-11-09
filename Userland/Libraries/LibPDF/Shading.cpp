/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/CommonNames.h>
#include <LibPDF/Document.h>
#include <LibPDF/Shading.h>

namespace PDF {

PDFErrorOr<NonnullRefPtr<Shading>> Shading::create(Document* document, NonnullRefPtr<Object> shading_dict_or_stream)
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

    // TABLE 4.28 Entries common to all shading dictionaries
    int shading_type = TRY(document->resolve(shading_dict->get_value(CommonNames::ShadingType))).to_int();

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
