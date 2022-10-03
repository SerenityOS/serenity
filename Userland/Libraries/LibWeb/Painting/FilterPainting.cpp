/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Filters/BrightnessFilter.h>
#include <LibGfx/Filters/ContrastFilter.h>
#include <LibGfx/Filters/GrayscaleFilter.h>
#include <LibGfx/Filters/HueRotateFilter.h>
#include <LibGfx/Filters/InvertFilter.h>
#include <LibGfx/Filters/OpacityFilter.h>
#include <LibGfx/Filters/SaturateFilter.h>
#include <LibGfx/Filters/SepiaFilter.h>
#include <LibGfx/Filters/StackBlurFilter.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Painting/BorderRadiusCornerClipper.h>
#include <LibWeb/Painting/FilterPainting.h>

namespace Web::Painting {

void apply_filter_list(Gfx::Bitmap& target_bitmap, Layout::Node const& node, Span<CSS::FilterFunction const> filter_list)
{
    auto apply_color_filter = [&](Gfx::ColorFilter const& filter) {
        const_cast<Gfx::ColorFilter&>(filter).apply(target_bitmap, target_bitmap.rect(), target_bitmap, target_bitmap.rect());
    };
    for (auto& filter_function : filter_list) {
        // See: https://drafts.fxtf.org/filter-effects-1/#supported-filter-functions
        filter_function.visit(
            [&](CSS::Filter::Blur const& blur) {
                // Applies a Gaussian blur to the input image.
                // The passed parameter defines the value of the standard deviation to the Gaussian function.
                Gfx::StackBlurFilter filter { target_bitmap };
                filter.process_rgba(blur.resolved_radius(node), Color::Transparent);
            },
            [&](CSS::Filter::Color const& color) {
                auto amount = color.resolved_amount();
                auto amount_clamped = clamp(amount, 0.0f, 1.0f);
                switch (color.operation) {
                case CSS::Filter::Color::Operation::Grayscale: {
                    // Converts the input image to grayscale. The passed parameter defines the proportion of the conversion.
                    // A value of 100% is completely grayscale. A value of 0% leaves the input unchanged.
                    apply_color_filter(Gfx::GrayscaleFilter { amount_clamped });
                    break;
                }
                case CSS::Filter::Color::Operation::Brightness: {
                    // Applies a linear multiplier to input image, making it appear more or less bright.
                    // A value of 0% will create an image that is completely black. A value of 100% leaves the input unchanged.
                    // Values of amount over 100% are allowed, providing brighter results.
                    apply_color_filter(Gfx::BrightnessFilter { amount });
                    break;
                }
                case CSS::Filter::Color::Operation::Contrast: {
                    // Adjusts the contrast of the input. A value of 0% will create an image that is completely gray.
                    // A value of 100% leaves the input unchanged. Values of amount over 100% are allowed, providing results with more contrast.
                    apply_color_filter(Gfx::ContrastFilter { amount });
                    break;
                }
                case CSS::Filter::Color::Operation::Invert: {
                    // Inverts the samples in the input image. The passed parameter defines the proportion of the conversion.
                    // A value of 100% is completely inverted. A value of 0% leaves the input unchanged.
                    apply_color_filter(Gfx::InvertFilter { amount_clamped });
                    break;
                }
                case CSS::Filter::Color::Operation::Opacity: {
                    // Applies transparency to the samples in the input image. The passed parameter defines the proportion of the conversion.
                    // A value of 0% is completely transparent. A value of 100% leaves the input unchanged.
                    apply_color_filter(Gfx::OpacityFilter { amount_clamped });
                    break;
                }
                case CSS::Filter::Color::Operation::Sepia: {
                    // Converts the input image to sepia. The passed parameter defines the proportion of the conversion.
                    // A value of 100% is completely sepia. A value of 0% leaves the input unchanged.
                    apply_color_filter(Gfx::SepiaFilter { amount_clamped });
                    break;
                }
                case CSS::Filter::Color::Operation::Saturate: {
                    // Saturates the input image. The passed parameter defines the proportion of the conversion.
                    // A value of 0% is completely un-saturated. A value of 100% leaves the input unchanged.
                    // Other values are linear multipliers on the effect.
                    // Values of amount over 100% are allowed, providing super-saturated results
                    apply_color_filter(Gfx::SaturateFilter { amount });
                    break;
                }
                default:
                    break;
                }
            },
            [&](CSS::Filter::HueRotate const& hue_rotate) {
                apply_color_filter(Gfx::HueRotateFilter { hue_rotate.angle_degrees() });
            },
            [&](CSS::Filter::DropShadow const&) {
                dbgln("TODO: Implement drop-shadow() filter function!");
            });
    }
}

void apply_backdrop_filter(PaintContext& context, Layout::Node const& node, Gfx::FloatRect const& backdrop_rect, BorderRadiiData const& border_radii_data, CSS::BackdropFilter const& backdrop_filter)
{
    // This performs the backdrop filter operation: https://drafts.fxtf.org/filter-effects-2/#backdrop-filter-operation

    auto backdrop_region = backdrop_rect.to_rounded<int>();

    // Note: The region bitmap can be smaller than the backdrop_region if it's at the edge of canvas.
    Gfx::IntRect actual_region {};

    // FIXME: Go through the steps to find the "Backdrop Root Image"
    // https://drafts.fxtf.org/filter-effects-2/#BackdropRoot

    // 1. Copy the Backdrop Root Image into a temporary buffer, such as a raster image. Call this buffer T’.
    auto maybe_backdrop_bitmap = context.painter().get_region_bitmap(backdrop_region, Gfx::BitmapFormat::BGRA8888, actual_region);
    if (actual_region.is_empty())
        return;
    if (maybe_backdrop_bitmap.is_error()) {
        dbgln("Failed get region bitmap for backdrop-filter");
        return;
    }
    auto backdrop_bitmap = maybe_backdrop_bitmap.release_value();
    // 2. Apply the backdrop-filter’s filter operations to the entire contents of T'.
    apply_filter_list(*backdrop_bitmap, node, backdrop_filter.filters());

    // FIXME: 3. If element B has any transforms (between B and the Backdrop Root), apply the inverse of those transforms to the contents of T’.

    // 4. Apply a clip to the contents of T’, using the border box of element B, including border-radius if specified. Note that the children of B are not considered for the sizing or location of this clip.
    ScopedCornerRadiusClip corner_clipper { context.painter(), backdrop_region, border_radii_data };

    // FIXME: 5. Draw all of element B, including its background, border, and any children elements, into T’.

    // FXIME: 6. If element B has any transforms, effects, or clips, apply those to T’.

    // 7. Composite the contents of T’ into element B’s parent, using source-over compositing.
    context.painter().blit(actual_region.location(), *backdrop_bitmap, backdrop_bitmap->rect());
}

}
