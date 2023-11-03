/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibPDF/Function.h>

// PDF 1.7 spec, 3.9 Functions

namespace PDF {

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

PDFErrorOr<NonnullRefPtr<Function>> Function::create(Document*, NonnullRefPtr<Object>)
{
    return Error(Error::Type::RenderingUnsupported, "Function creation not yet implemented"_string);
}

}
