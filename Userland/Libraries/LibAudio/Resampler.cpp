/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Resampler.h"
#include "Buffer.h"
#include "Sample.h"

namespace Audio {

ErrorOr<NonnullRefPtr<Buffer>> resample_buffer(ResampleHelper<double>& resampler, Buffer const& to_resample)
{
    Vector<Sample> resampled;
    resampled.ensure_capacity(to_resample.sample_count() * ceil_div(resampler.source(), resampler.target()));
    for (size_t i = 0; i < static_cast<size_t>(to_resample.sample_count()); ++i) {
        auto sample = to_resample.samples()[i];
        resampler.process_sample(sample.left, sample.right);

        while (resampler.read_sample(sample.left, sample.right))
            resampled.append(sample);
    }

    return Buffer::create_with_samples(move(resampled));
}

}
