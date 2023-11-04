/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/CommonNames.h>
#include <LibPDF/Document.h>
#include <LibPDF/Function.h>
#include <LibPDF/ObjectDerivatives.h>

// PDF 1.7 spec, 3.9 Functions

namespace PDF {

struct Bound {
    float lower;
    float upper;
};

class SampledFunction final : public Function {
public:
    virtual PDFErrorOr<ReadonlySpan<float>> evaluate(ReadonlySpan<float>) const override;
};

PDFErrorOr<ReadonlySpan<float>> SampledFunction::evaluate(ReadonlySpan<float>) const
{
    return Error(Error::Type::RenderingUnsupported, "SampledFunction not yet implemented"_string);
}

class ExponentialInterpolationFunction final : public Function {
public:
    virtual PDFErrorOr<ReadonlySpan<float>> evaluate(ReadonlySpan<float>) const override;
};

PDFErrorOr<ReadonlySpan<float>> ExponentialInterpolationFunction::evaluate(ReadonlySpan<float>) const
{
    return Error(Error::Type::RenderingUnsupported, "ExponentialInterpolationFunction not yet implemented"_string);
}

class StitchingFunction final : public Function {
public:
    virtual PDFErrorOr<ReadonlySpan<float>> evaluate(ReadonlySpan<float>) const override;
};

PDFErrorOr<ReadonlySpan<float>> StitchingFunction::evaluate(ReadonlySpan<float>) const
{
    return Error(Error::Type::RenderingUnsupported, "StitchingFunction not yet implemented"_string);
}

class PostScriptCalculatorFunction final : public Function {
public:
    virtual PDFErrorOr<ReadonlySpan<float>> evaluate(ReadonlySpan<float>) const override;
};

PDFErrorOr<ReadonlySpan<float>> PostScriptCalculatorFunction::evaluate(ReadonlySpan<float>) const
{
    return Error(Error::Type::RenderingUnsupported, "PostScriptCalculatorFunction not yet implemented"_string);
}

PDFErrorOr<NonnullRefPtr<Function>> Function::create(Document* document, NonnullRefPtr<Object> object)
{
    if (!object->is<DictObject>() && !object->is<StreamObject>())
        return Error { Error::Type::MalformedPDF, "Function object must be dict or stream" };

    auto function_dict = object->is<DictObject>() ? object->cast<DictObject>() : object->cast<StreamObject>()->dict();

    // "TABLE 3.35 Entries common to all function dictionaries"

    if (!function_dict->contains(CommonNames::FunctionType))
        return Error { Error::Type::MalformedPDF, "Function requires /FunctionType" };
    auto function_type = TRY(document->resolve_to<int>(function_dict->get_value(CommonNames::FunctionType)));

    if (!function_dict->contains(CommonNames::Domain))
        return Error { Error::Type::MalformedPDF, "Function requires /Domain" };
    auto domain_array = TRY(function_dict->get_array(document, CommonNames::Domain));
    if (domain_array->size() % 2 != 0)
        return Error { Error::Type::MalformedPDF, "Function /Domain size not multiple of 2" };

    Vector<Bound> domain;
    for (size_t i = 0; i < domain_array->size(); i += 2) {
        domain.append({ domain_array->at(i).to_float(), domain_array->at(i + 1).to_float() });
        if (domain.last().lower > domain.last().upper)
            return Error { Error::Type::MalformedPDF, "Function /Domain lower bound > upper bound" };
    }

    // Can't use PDFErrorOr with Optional::map()
    Optional<Vector<Bound>> optional_range;
    if (function_dict->contains(CommonNames::Range)) {
        auto range_array = TRY(function_dict->get_array(document, CommonNames::Range));
        if (range_array->size() % 2 != 0)
            return Error { Error::Type::MalformedPDF, "Function /Range size not multiple of 2" };

        Vector<Bound> range;
        for (size_t i = 0; i < range_array->size(); i += 2) {
            range.append({ range_array->at(i).to_float(), range_array->at(i + 1).to_float() });
            if (range.last().lower > range.last().upper)
                return Error { Error::Type::MalformedPDF, "Function /Range lower bound > upper bound" };
        }
        optional_range = move(range);
    }

    switch (function_type) {
    case 0:
        return adopt_ref(*new SampledFunction());
    // The spec has no entry for `1`.
    case 2:
        return adopt_ref(*new ExponentialInterpolationFunction());
    case 3:
        return adopt_ref(*new StitchingFunction());
    case 4:
        return adopt_ref(*new PostScriptCalculatorFunction());
    default:
        dbgln("invalid function type {}", function_type);
        return Error(Error::Type::MalformedPDF, "Function has unkonwn type"_string);
    }
}

}
