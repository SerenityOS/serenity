/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/OwnPtr.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Quad.h>
#include <LibGfx/Rect.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/CanvasRenderingContext2D.h>
#include <LibWeb/HTML/HTMLCanvasElement.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/HTML/ImageData.h>
#include <LibWeb/HTML/Path2D.h>
#include <LibWeb/HTML/TextMetrics.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Platform/FontPlugin.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::HTML {

JS::NonnullGCPtr<CanvasRenderingContext2D> CanvasRenderingContext2D::create(JS::Realm& realm, HTMLCanvasElement& element)
{
    return *realm.heap().allocate<CanvasRenderingContext2D>(realm, realm, element);
}

CanvasRenderingContext2D::CanvasRenderingContext2D(JS::Realm& realm, HTMLCanvasElement& element)
    : PlatformObject(realm)
    , CanvasPath(static_cast<Bindings::PlatformObject&>(*this))
    , m_element(element)
{
    set_prototype(&Bindings::cached_web_prototype(realm, "CanvasRenderingContext2D"));
}

CanvasRenderingContext2D::~CanvasRenderingContext2D() = default;

void CanvasRenderingContext2D::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_element.ptr());
}

HTMLCanvasElement& CanvasRenderingContext2D::canvas_element()
{
    return *m_element;
}

HTMLCanvasElement const& CanvasRenderingContext2D::canvas_element() const
{
    return *m_element;
}

JS::NonnullGCPtr<HTMLCanvasElement> CanvasRenderingContext2D::canvas_for_binding() const
{
    return *m_element;
}

void CanvasRenderingContext2D::fill_rect(float x, float y, float width, float height)
{
    auto painter = this->painter();
    if (!painter)
        return;

    auto& drawing_state = this->drawing_state();

    auto rect = drawing_state.transform.map(Gfx::FloatRect(x, y, width, height));
    painter->fill_rect(enclosing_int_rect(rect), drawing_state.fill_style);
    did_draw(rect);
}

void CanvasRenderingContext2D::clear_rect(float x, float y, float width, float height)
{
    auto painter = this->painter();
    if (!painter)
        return;

    auto rect = drawing_state().transform.map(Gfx::FloatRect(x, y, width, height));
    painter->clear_rect(enclosing_int_rect(rect), Color());
    did_draw(rect);
}

void CanvasRenderingContext2D::stroke_rect(float x, float y, float width, float height)
{
    auto painter = this->painter();
    if (!painter)
        return;

    auto& drawing_state = this->drawing_state();

    auto rect = drawing_state.transform.map(Gfx::FloatRect(x, y, width, height));

    auto top_left = drawing_state.transform.map(Gfx::FloatPoint(x, y)).to_type<int>();
    auto top_right = drawing_state.transform.map(Gfx::FloatPoint(x + width - 1, y)).to_type<int>();
    auto bottom_left = drawing_state.transform.map(Gfx::FloatPoint(x, y + height - 1)).to_type<int>();
    auto bottom_right = drawing_state.transform.map(Gfx::FloatPoint(x + width - 1, y + height - 1)).to_type<int>();

    painter->draw_line(top_left, top_right, drawing_state.stroke_style, drawing_state.line_width);
    painter->draw_line(top_right, bottom_right, drawing_state.stroke_style, drawing_state.line_width);
    painter->draw_line(bottom_right, bottom_left, drawing_state.stroke_style, drawing_state.line_width);
    painter->draw_line(bottom_left, top_left, drawing_state.stroke_style, drawing_state.line_width);

    did_draw(rect);
}

// 4.12.5.1.14 Drawing images, https://html.spec.whatwg.org/multipage/canvas.html#drawing-images
WebIDL::ExceptionOr<void> CanvasRenderingContext2D::draw_image_internal(CanvasImageSource const& image, float source_x, float source_y, float source_width, float source_height, float destination_x, float destination_y, float destination_width, float destination_height)
{
    // 1. If any of the arguments are infinite or NaN, then return.
    if (!isfinite(source_x) || !isfinite(source_y) || !isfinite(source_width) || !isfinite(source_height) || !isfinite(destination_x) || !isfinite(destination_y) || !isfinite(destination_width) || !isfinite(destination_height))
        return {};

    // 2. Let usability be the result of checking the usability of image.
    auto usability = TRY(check_usability_of_image(image));

    // 3. If usability is bad, then return (without drawing anything).
    if (usability == CanvasImageSourceUsability::Bad)
        return {};

    auto const* bitmap = image.visit([](auto const& source) { return source->bitmap(); });
    if (!bitmap)
        return {};

    // 4. Establish the source and destination rectangles as follows:
    //    If not specified, the dw and dh arguments must default to the values of sw and sh, interpreted such that one CSS pixel in the image is treated as one unit in the output bitmap's coordinate space.
    //    If the sx, sy, sw, and sh arguments are omitted, then they must default to 0, 0, the image's intrinsic width in image pixels, and the image's intrinsic height in image pixels, respectively.
    //    If the image has no intrinsic dimensions, then the concrete object size must be used instead, as determined using the CSS "Concrete Object Size Resolution" algorithm, with the specified size having
    //    neither a definite width nor height, nor any additional constraints, the object's intrinsic properties being those of the image argument, and the default object size being the size of the output bitmap.
    //    The source rectangle is the rectangle whose corners are the four points (sx, sy), (sx+sw, sy), (sx+sw, sy+sh), (sx, sy+sh).
    //    The destination rectangle is the rectangle whose corners are the four points (dx, dy), (dx+dw, dy), (dx+dw, dy+dh), (dx, dy+dh).
    // NOTE: Implemented in drawImage() overloads

    //    The source rectangle is the rectangle whose corners are the four points (sx, sy), (sx+sw, sy), (sx+sw, sy+sh), (sx, sy+sh).
    auto source_rect = Gfx::FloatRect { source_x, source_y, source_width, source_height };
    //    The destination rectangle is the rectangle whose corners are the four points (dx, dy), (dx+dw, dy), (dx+dw, dy+dh), (dx, dy+dh).
    auto destination_rect = Gfx::FloatRect { destination_x, destination_y, destination_width, destination_height };
    //    When the source rectangle is outside the source image, the source rectangle must be clipped
    //    to the source image and the destination rectangle must be clipped in the same proportion.
    auto clipped_source = source_rect.intersected(bitmap->rect().to_type<float>());
    auto clipped_destination = destination_rect;
    if (clipped_source != source_rect) {
        clipped_destination.set_width(clipped_destination.width() * (clipped_source.width() / source_rect.width()));
        clipped_destination.set_height(clipped_destination.height() * (clipped_source.height() / source_rect.height()));
    }

    // 5. If one of the sw or sh arguments is zero, then return. Nothing is painted.
    if (source_width == 0 || source_height == 0)
        return {};

    // 6. Paint the region of the image argument specified by the source rectangle on the region of the rendering context's output bitmap specified by the destination rectangle, after applying the current transformation matrix to the destination rectangle.
    auto painter = this->painter();
    if (!painter)
        return {};

    painter->draw_scaled_bitmap_with_transform(destination_rect.to_rounded<int>(), *bitmap, source_rect, drawing_state().transform, 1.0f, Gfx::Painter::ScalingMode::BilinearBlend);

    // 7. If image is not origin-clean, then set the CanvasRenderingContext2D's origin-clean flag to false.
    if (image_is_not_origin_clean(image))
        m_origin_clean = false;

    return {};
}

void CanvasRenderingContext2D::did_draw(Gfx::FloatRect const&)
{
    // FIXME: Make use of the rect to reduce the invalidated area when possible.
    if (!canvas_element().layout_node())
        return;
    canvas_element().layout_node()->set_needs_display();
}

OwnPtr<Gfx::Painter> CanvasRenderingContext2D::painter()
{
    if (!canvas_element().bitmap()) {
        if (!canvas_element().create_bitmap())
            return {};
    }

    return make<Gfx::Painter>(*canvas_element().bitmap());
}

void CanvasRenderingContext2D::fill_text(String const& text, float x, float y, Optional<double> max_width)
{
    if (max_width.has_value() && max_width.value() <= 0)
        return;

    auto painter = this->painter();
    if (!painter)
        return;

    auto& drawing_state = this->drawing_state();

    // FIXME: painter only supports integer rects for text right now, so this effectively chops off any fractional position
    auto text_rect = Gfx::IntRect(x, y, max_width.has_value() ? max_width.value() : painter->font().width(text), painter->font().pixel_size());
    auto transformed_rect = drawing_state.transform.map(text_rect);
    painter->draw_text(transformed_rect, text, Gfx::TextAlignment::TopLeft, drawing_state.fill_style);
    did_draw(transformed_rect.to_type<float>());
}

void CanvasRenderingContext2D::stroke_text(String const& text, float x, float y, Optional<double> max_width)
{
    // FIXME: Stroke the text instead of filling it.
    fill_text(text, x, y, max_width);
}

void CanvasRenderingContext2D::begin_path()
{
    path().clear();
}

void CanvasRenderingContext2D::stroke_internal(Gfx::Path const& path)
{
    auto painter = this->painter();
    if (!painter)
        return;

    auto& drawing_state = this->drawing_state();

    painter->stroke_path(path, drawing_state.stroke_style, drawing_state.line_width);
    did_draw(path.bounding_box());
}

void CanvasRenderingContext2D::stroke()
{
    stroke_internal(path());
}

void CanvasRenderingContext2D::stroke(Path2D const& path)
{
    auto transformed_path = path.path().copy_transformed(drawing_state().transform);
    stroke_internal(transformed_path);
}

void CanvasRenderingContext2D::fill_internal(Gfx::Path& path, String const& fill_rule)
{
    auto painter = this->painter();
    if (!painter)
        return;

    path.close_all_subpaths();

    auto winding = Gfx::Painter::WindingRule::Nonzero;
    if (fill_rule == "evenodd")
        winding = Gfx::Painter::WindingRule::EvenOdd;
    else if (fill_rule == "nonzero")
        winding = Gfx::Painter::WindingRule::Nonzero;
    else
        dbgln("Unrecognized fillRule for CRC2D.fill() - this problem goes away once we pass an enum instead of a string");

    painter->fill_path(path, drawing_state().fill_style, winding);
    did_draw(path.bounding_box());
}

void CanvasRenderingContext2D::fill(String const& fill_rule)
{
    return fill_internal(path(), fill_rule);
}

void CanvasRenderingContext2D::fill(Path2D& path, String const& fill_rule)
{
    auto transformed_path = path.path().copy_transformed(drawing_state().transform);
    return fill_internal(transformed_path, fill_rule);
}

JS::GCPtr<ImageData> CanvasRenderingContext2D::create_image_data(int width, int height) const
{
    return ImageData::create_with_size(realm(), width, height);
}

// https://html.spec.whatwg.org/multipage/canvas.html#dom-context-2d-getimagedata
WebIDL::ExceptionOr<JS::GCPtr<ImageData>> CanvasRenderingContext2D::get_image_data(int x, int y, int width, int height) const
{
    // 1. If either the sw or sh arguments are zero, then throw an "IndexSizeError" DOMException.
    if (width == 0 || height == 0)
        return WebIDL::IndexSizeError::create(realm(), "Width and height must not be zero");

    // 2. If the CanvasRenderingContext2D's origin-clean flag is set to false, then throw a "SecurityError" DOMException.
    if (!m_origin_clean)
        return WebIDL::SecurityError::create(realm(), "CanvasRenderingContext2D is not origin-clean");

    // 3. Let imageData be a new ImageData object.
    // 4. Initialize imageData given sw, sh, settings set to settings, and defaultColorSpace set to this's color space.
    auto image_data = ImageData::create_with_size(realm(), width, height);

    // NOTE: We don't attempt to create the underlying bitmap here; if it doesn't exist, it's like copying only transparent black pixels (which is a no-op).
    if (!canvas_element().bitmap())
        return image_data;
    auto const& bitmap = *canvas_element().bitmap();

    // 5. Let the source rectangle be the rectangle whose corners are the four points (sx, sy), (sx+sw, sy), (sx+sw, sy+sh), (sx, sy+sh).
    auto source_rect = Gfx::Rect { x, y, width, height };
    auto source_rect_intersected = source_rect.intersected(bitmap.rect());

    // 6. Set the pixel values of imageData to be the pixels of this's output bitmap in the area specified by the source rectangle in the bitmap's coordinate space units, converted from this's color space to imageData's colorSpace using 'relative-colorimetric' rendering intent.
    // FIXME: Can't use a Gfx::Painter + blit() here as it doesn't support ImageData bitmap's RGBA8888 format.
    for (int target_y = 0; target_y < source_rect_intersected.height(); ++target_y) {
        for (int target_x = 0; target_x < source_rect_intersected.width(); ++target_x) {
            auto pixel = bitmap.get_pixel(target_x + x, target_y + y);
            image_data->bitmap().set_pixel(target_x, target_y, pixel);
        }
    }

    // 7. Set the pixels values of imageData for areas of the source rectangle that are outside of the output bitmap to transparent black.
    // NOTE: No-op, already done during creation.

    // 8. Return imageData.
    return image_data;
}

void CanvasRenderingContext2D::put_image_data(ImageData const& image_data, float x, float y)
{
    auto painter = this->painter();
    if (!painter)
        return;

    painter->blit(Gfx::IntPoint(x, y), image_data.bitmap(), image_data.bitmap().rect());

    did_draw(Gfx::FloatRect(x, y, image_data.width(), image_data.height()));
}

// https://html.spec.whatwg.org/multipage/canvas.html#reset-the-rendering-context-to-its-default-state
void CanvasRenderingContext2D::reset_to_default_state()
{
    auto painter = this->painter();

    // 1. Clear canvas's bitmap to transparent black.
    if (painter)
        painter->clear_rect(painter->target()->rect(), Color::Transparent);

    // 2. Empty the list of subpaths in context's current default path.
    path().clear();

    // 3. Clear the context's drawing state stack.
    clear_drawing_state_stack();

    // 4. Reset everything that drawing state consists of to their initial values.
    reset_drawing_state();

    if (painter)
        did_draw(painter->target()->rect().to_type<float>());
}

// https://html.spec.whatwg.org/multipage/canvas.html#dom-context-2d-measuretext
JS::NonnullGCPtr<TextMetrics> CanvasRenderingContext2D::measure_text(String const& text)
{
    // The measureText(text) method steps are to run the text preparation
    // algorithm, passing it text and the object implementing the CanvasText
    // interface, and then using the returned inline box must return a new
    // TextMetrics object with members behaving as described in the following
    // list:
    auto prepared_text = prepare_text(text);
    auto metrics = TextMetrics::create(realm());
    // FIXME: Use the font that was used to create the glyphs in prepared_text.
    auto& font = Platform::FontPlugin::the().default_font();

    // width attribute: The width of that inline box, in CSS pixels. (The text's advance width.)
    metrics->set_width(prepared_text.bounding_box.width());
    // actualBoundingBoxLeft attribute: The distance parallel to the baseline from the alignment point given by the textAlign attribute to the left side of the bounding rectangle of the given text, in CSS pixels; positive numbers indicating a distance going left from the given alignment point.
    metrics->set_actual_bounding_box_left(-prepared_text.bounding_box.left());
    // actualBoundingBoxRight attribute: The distance parallel to the baseline from the alignment point given by the textAlign attribute to the right side of the bounding rectangle of the given text, in CSS pixels; positive numbers indicating a distance going right from the given alignment point.
    metrics->set_actual_bounding_box_right(prepared_text.bounding_box.right());
    // fontBoundingBoxAscent attribute: The distance from the horizontal line indicated by the textBaseline attribute to the ascent metric of the first available font, in CSS pixels; positive numbers indicating a distance going up from the given baseline.
    metrics->set_font_bounding_box_ascent(font.baseline());
    // fontBoundingBoxDescent attribute: The distance from the horizontal line indicated by the textBaseline attribute to the descent metric of the first available font, in CSS pixels; positive numbers indicating a distance going down from the given baseline.
    metrics->set_font_bounding_box_descent(prepared_text.bounding_box.height() - font.baseline());
    // actualBoundingBoxAscent attribute: The distance from the horizontal line indicated by the textBaseline attribute to the top of the bounding rectangle of the given text, in CSS pixels; positive numbers indicating a distance going up from the given baseline.
    metrics->set_actual_bounding_box_ascent(font.baseline());
    // actualBoundingBoxDescent attribute: The distance from the horizontal line indicated by the textBaseline attribute to the bottom of the bounding rectangle of the given text, in CSS pixels; positive numbers indicating a distance going down from the given baseline.
    metrics->set_actual_bounding_box_descent(prepared_text.bounding_box.height() - font.baseline());
    // emHeightAscent attribute: The distance from the horizontal line indicated by the textBaseline attribute to the highest top of the em squares in the inline box, in CSS pixels; positive numbers indicating that the given baseline is below the top of that em square (so this value will usually be positive). Zero if the given baseline is the top of that em square; half the font size if the given baseline is the middle of that em square.
    metrics->set_em_height_ascent(font.baseline());
    // emHeightDescent attribute: The distance from the horizontal line indicated by the textBaseline attribute to the lowest bottom of the em squares in the inline box, in CSS pixels; positive numbers indicating that the given baseline is above the bottom of that em square. (Zero if the given baseline is the bottom of that em square.)
    metrics->set_em_height_descent(prepared_text.bounding_box.height() - font.baseline());
    // hangingBaseline attribute: The distance from the horizontal line indicated by the textBaseline attribute to the hanging baseline of the inline box, in CSS pixels; positive numbers indicating that the given baseline is below the hanging baseline. (Zero if the given baseline is the hanging baseline.)
    metrics->set_hanging_baseline(font.baseline());
    // alphabeticBaseline attribute: The distance from the horizontal line indicated by the textBaseline attribute to the alphabetic baseline of the inline box, in CSS pixels; positive numbers indicating that the given baseline is below the alphabetic baseline. (Zero if the given baseline is the alphabetic baseline.)
    metrics->set_font_bounding_box_ascent(0);
    // ideographicBaseline attribute: The distance from the horizontal line indicated by the textBaseline attribute to the ideographic-under baseline of the inline box, in CSS pixels; positive numbers indicating that the given baseline is below the ideographic-under baseline. (Zero if the given baseline is the ideographic-under baseline.)
    metrics->set_font_bounding_box_ascent(0);

    return metrics;
}

// https://html.spec.whatwg.org/multipage/canvas.html#text-preparation-algorithm
CanvasRenderingContext2D::PreparedText CanvasRenderingContext2D::prepare_text(String const& text, float max_width)
{
    // 1. If maxWidth was provided but is less than or equal to zero or equal to NaN, then return an empty array.
    if (max_width <= 0 || max_width != max_width) {
        return {};
    }

    // 2. Replace all ASCII whitespace in text with U+0020 SPACE characters.
    // NOTE: This also replaces vertical tabs with space even though WHATWG
    //       doesn't consider it as whitespace.
    StringBuilder builder { text.length() };
    for (auto c : text) {
        builder.append(is_ascii_space(c) ? ' ' : c);
    }
    String replaced_text = builder.build();

    // 3. Let font be the current font of target, as given by that object's font attribute.
    // FIXME: Once we have CanvasTextDrawingStyles, implement font selection.

    // 4. Apply the appropriate step from the following list to determine the value of direction:
    //   4.1. If the target object's direction attribute has the value "ltr": Let direction be 'ltr'.
    //   4.2. If the target object's direction attribute has the value "rtl": Let direction be 'rtl'.
    //   4.3. If the target object's font style source object is an element: Let direction be the directionality of the target object's font style source object.
    //   4.4. If the target object's font style source object is a Document with a non-null document element: Let direction be the directionality of the target object's font style source object's document element.
    //   4.5. Otherwise: Let direction be 'ltr'.
    // FIXME: Once we have CanvasTextDrawingStyles, implement directionality.

    // 5. Form a hypothetical infinitely-wide CSS line box containing a single inline box containing the text text, with its CSS properties set as follows:
    //   'direction'         -> direction
    //   'font'              -> font
    //   'font-kerning'      -> target's fontKerning
    //   'font-stretch'      -> target's fontStretch
    //   'font-variant-caps' -> target's fontVariantCaps
    //   'letter-spacing'    -> target's letterSpacing
    //   SVG text-rendering  -> target's textRendering
    //   'white-space'       -> 'pre'
    //   'word-spacing'      -> target's wordSpacing
    // ...and with all other properties set to their initial values.
    // FIXME: Actually use a LineBox here instead of, you know, using the default font and measuring its size (which is not the spec at all).
    // FIXME: Once we have CanvasTextDrawingStyles, add the CSS attributes.
    auto& font = Platform::FontPlugin::the().default_font();
    size_t width = 0;
    size_t height = font.pixel_size();
    for (auto c : Utf8View { replaced_text }) {
        width += font.glyph_or_emoji_width(c);
    }

    // 6. If maxWidth was provided and the hypothetical width of the inline box in the hypothetical line box is greater than maxWidth CSS pixels, then change font to have a more condensed font (if one is available or if a reasonably readable one can be synthesized by applying a horizontal scale factor to the font) or a smaller font, and return to the previous step.
    // FIXME: Record the font size used for this piece of text, and actually retry with a smaller size if needed.

    // 7. The anchor point is a point on the inline box, and the physical alignment is one of the values left, right, and center. These variables are determined by the textAlign and textBaseline values as follows:
    // Horizontal position:
    //   7.1. If textAlign is left, if textAlign is start and direction is 'ltr' or if textAlign is end and direction is 'rtl': Let the anchor point's horizontal position be the left edge of the inline box, and let physical alignment be left.
    //   7.2. If textAlign is right, if textAlign is end and direction is 'ltr' or if textAlign is start and direction is 'rtl': Let the anchor point's horizontal position be the right edge of the inline box, and let physical alignment be right.
    //   7.3. If textAlign is center: Let the anchor point's horizontal position be half way between the left and right edges of the inline box, and let physical alignment be center.
    // Vertical position:
    //   7.4. If textBaseline is top: Let the anchor point's vertical position be the top of the em box of the first available font of the inline box.
    //   7.5. If textBaseline is hanging: Let the anchor point's vertical position be the hanging baseline of the first available font of the inline box.
    //   7.6. If textBaseline is middle: Let the anchor point's vertical position be half way between the bottom and the top of the em box of the first available font of the inline box.
    //   7.7. If textBaseline is alphabetic: Let the anchor point's vertical position be the alphabetic baseline of the first available font of the inline box.
    //   7.8. If textBaseline is ideographic: Let the anchor point's vertical position be the ideographic-under baseline of the first available font of the inline box.
    //   7.9. If textBaseline is bottom: Let the anchor point's vertical position be the bottom of the em box of the first available font of the inline box.
    // FIXME: Once we have CanvasTextDrawingStyles, handle the alignment and baseline.
    [[maybe_unused]] Gfx::IntPoint anchor { 0, 0 };
    auto physical_alignment = Gfx::TextAlignment::CenterLeft;

    // 8. Let result be an array constructed by iterating over each glyph in the inline box from left to right (if any), adding to the array, for each glyph, the shape of the glyph as it is in the inline box, positioned on a coordinate space using CSS pixels with its origin is at the anchor point.
    PreparedText prepared_text { {}, physical_alignment, { 0, 0, static_cast<int>(width), static_cast<int>(height) } };
    prepared_text.glyphs.ensure_capacity(replaced_text.length());

    size_t offset = 0;
    for (auto c : Utf8View { replaced_text }) {
        prepared_text.glyphs.append({ c, { static_cast<int>(offset), 0 } });
        offset += font.glyph_or_emoji_width(c);
    }

    // 9. Return result, physical alignment, and the inline box.
    return prepared_text;
}

void CanvasRenderingContext2D::clip()
{
    // FIXME: Implement.
}

// https://html.spec.whatwg.org/multipage/canvas.html#check-the-usability-of-the-image-argument
WebIDL::ExceptionOr<CanvasImageSourceUsability> check_usability_of_image(CanvasImageSource const& image)
{
    // 1. Switch on image:
    auto usability = TRY(image.visit(
        // HTMLOrSVGImageElement
        [](JS::Handle<HTMLImageElement> const& image_element) -> WebIDL::ExceptionOr<Optional<CanvasImageSourceUsability>> {
            // FIXME: If image's current request's state is broken, then throw an "InvalidStateError" DOMException.

            // If image is not fully decodable, then return bad.
            if (!image_element->bitmap())
                return { CanvasImageSourceUsability::Bad };

            // If image has an intrinsic width or intrinsic height (or both) equal to zero, then return bad.
            if (image_element->bitmap()->width() == 0 || image_element->bitmap()->height() == 0)
                return { CanvasImageSourceUsability::Bad };
            return Optional<CanvasImageSourceUsability> {};
        },

        // FIXME: HTMLVideoElement
        // If image's readyState attribute is either HAVE_NOTHING or HAVE_METADATA, then return bad.

        // HTMLCanvasElement
        // FIXME: OffscreenCanvas
        [](JS::Handle<HTMLCanvasElement> const& canvas_element) -> WebIDL::ExceptionOr<Optional<CanvasImageSourceUsability>> {
            // If image has either a horizontal dimension or a vertical dimension equal to zero, then throw an "InvalidStateError" DOMException.
            if (canvas_element->width() == 0 || canvas_element->height() == 0)
                return WebIDL::InvalidStateError::create(canvas_element->realm(), "Canvas width or height is zero");
            return Optional<CanvasImageSourceUsability> {};
        }));
    if (usability.has_value())
        return usability.release_value();

    // 2. Return good.
    return { CanvasImageSourceUsability::Good };
}

// https://html.spec.whatwg.org/multipage/canvas.html#the-image-argument-is-not-origin-clean
bool image_is_not_origin_clean(CanvasImageSource const& image)
{
    // An object image is not origin-clean if, switching on image's type:
    return image.visit(
        // HTMLOrSVGImageElement
        [](JS::Handle<HTMLImageElement> const&) {
            // FIXME: image's current request's image data is CORS-cross-origin.
            return false;
        },

        // FIXME: HTMLVideoElement
        // image's media data is CORS-cross-origin.

        // HTMLCanvasElement
        // FIXME: ImageBitmap
        [](JS::Handle<HTMLCanvasElement> const&) {
            // FIXME: image's bitmap's origin-clean flag is false.
            return false;
        });
}

}
