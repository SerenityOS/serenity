/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Filter.h"
#include <AK/StringView.h>
#include <LibGfx/Matrix.h>
#include <LibGfx/Matrix4x4.h>

namespace Gfx {

template<size_t N, typename T>
static constexpr void normalize(Matrix<N, T>& matrix)
{
    auto sum = 0.0f;
    for (size_t i = 0; i < matrix.Size; ++i) {
        for (size_t j = 0; j < matrix.Size; ++j) {
            sum += matrix.elements()[i][j];
        }
    }
    for (size_t i = 0; i < matrix.Size; ++i) {
        for (size_t j = 0; j < matrix.Size; ++j) {
            matrix.elements()[i][j] /= sum;
        }
    }
}

template<size_t N>
class GenericConvolutionFilter : public Filter {
public:
    class Parameters : public Filter::Parameters {
    public:
        Parameters(Gfx::Matrix<N, float> const& kernel, bool should_wrap = true)
            : m_kernel(kernel)
            , m_should_wrap(should_wrap)

        {
        }

        Gfx::Matrix<N, float> const& kernel() const { return m_kernel; }
        Gfx::Matrix<N, float>& kernel() { return m_kernel; }
        bool should_wrap() const { return m_should_wrap; }

    private:
        virtual bool is_generic_convolution_filter() const override { return true; }
        Gfx::Matrix<N, float> m_kernel;
        bool m_should_wrap { false };
    };

    class ApplyCache {
        template<size_t>
        friend class GenericConvolutionFilter;

    private:
        RefPtr<Gfx::Bitmap> m_target;
    };

    GenericConvolutionFilter() = default;
    virtual ~GenericConvolutionFilter() = default;

    virtual StringView class_name() const override { return "GenericConvolutionFilter"sv; }

    virtual void apply(Bitmap& target_bitmap, IntRect const& target_rect, Bitmap const& source_bitmap, IntRect const& source_rect, Filter::Parameters const& parameters) override
    {
        VERIFY(parameters.is_generic_convolution_filter());
        auto& gcf_params = static_cast<GenericConvolutionFilter::Parameters const&>(parameters);

        ApplyCache apply_cache;
        apply_with_cache(target_bitmap, target_rect, source_bitmap, source_rect, gcf_params, apply_cache);
    }

    void apply_with_cache(Bitmap& target, IntRect target_rect, Bitmap const& source, IntRect const& source_rect, GenericConvolutionFilter::Parameters const& parameters, ApplyCache& apply_cache)
    {
        // The target area (where the filter is applied) must be entirely
        // contained by the source area. source_rect should be describing
        // the pixels that can be accessed to apply this filter, while
        // target_rect should describe the area where to apply the filter on.
        VERIFY(source_rect.contains(target_rect));
        VERIFY(source.size().contains(target.size()));
        VERIFY(target.rect().contains(target_rect));
        VERIFY(source.rect().contains(source_rect));

        // If source is different from target, it should still be describing
        // essentially the same bitmap. But it allows us to modify target
        // without a temporary bitmap. This is important if this filter
        // is applied on multiple areas of the same bitmap, at which point
        // we would need to be able to access unmodified pixels if the
        // areas are (almost) adjacent.
        int source_delta_x = target_rect.x() - source_rect.x();
        int source_delta_y = target_rect.y() - source_rect.y();
        if (&target == &source && (!apply_cache.m_target || !apply_cache.m_target->size().contains(source_rect.size()))) {
            // TODO: We probably don't need the entire source_rect, we could inflate
            // the target_rect appropriately
            apply_cache.m_target = Gfx::Bitmap::create(source.format(), source_rect.size()).release_value_but_fixme_should_propagate_errors();
            target_rect.translate_by(-target_rect.location());
        }

        Bitmap* render_target_bitmap = (&target != &source) ? &target : apply_cache.m_target.ptr();

        // FIXME: Help! I am naive!
        constexpr static ssize_t offset = N / 2;
        for (auto i_ = 0; i_ < target_rect.width(); ++i_) {
            ssize_t i = i_ + target_rect.x();
            for (auto j_ = 0; j_ < target_rect.height(); ++j_) {
                ssize_t j = j_ + target_rect.y();
                FloatVector3 value(0, 0, 0);
                for (auto k = 0l; k < (ssize_t)N; ++k) {
                    auto ki = i + k - offset;
                    if (ki < source_rect.x() || ki >= source_rect.right()) {
                        if (parameters.should_wrap())
                            ki = (ki + source.size().width()) % source.size().width(); // TODO: fix up using source_rect
                        else
                            continue;
                    }

                    for (auto l = 0l; l < (ssize_t)N; ++l) {
                        auto lj = j + l - offset;
                        if (lj < source_rect.y() || lj >= source_rect.bottom()) {
                            if (parameters.should_wrap())
                                lj = (lj + source.size().height()) % source.size().height(); // TODO: fix up using source_rect
                            else
                                continue;
                        }

                        auto pixel = source.get_pixel(ki, lj);
                        FloatVector3 pixel_value(pixel.red(), pixel.green(), pixel.blue());

                        value = value + pixel_value * parameters.kernel().elements()[k][l];
                    }
                }

                value.clamp(0, 255);
                render_target_bitmap->set_pixel(i, j, Color(value.x(), value.y(), value.z(), source.get_pixel(i + source_delta_x, j + source_delta_y).alpha()));
            }
        }

        if (render_target_bitmap != &target) {
            // FIXME: Substitute for some sort of faster "blit" method.
            for (auto i_ = 0; i_ < target_rect.width(); ++i_) {
                auto i = i_ + target_rect.x();
                for (auto j_ = 0; j_ < target_rect.height(); ++j_) {
                    auto j = j_ + target_rect.y();
                    target.set_pixel(i, j, render_target_bitmap->get_pixel(i_, j_));
                }
            }
        }
    }
};

}
