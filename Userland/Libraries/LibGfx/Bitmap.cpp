/*
 * Copyright (c) 2018-2024, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Bitmap.h>
#include <AK/ByteString.h>
#include <AK/Checked.h>
#include <AK/LexicalPath.h>
#include <AK/Memory.h>
#include <AK/MemoryStream.h>
#include <AK/Optional.h>
#include <AK/Queue.h>
#include <AK/ScopeGuard.h>
#include <AK/Try.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibCore/MimeData.h>
#include <LibCore/System.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>
#include <LibGfx/ShareableBitmap.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>
#include <LibIPC/File.h>
#include <errno.h>
#include <stdio.h>

namespace Gfx {

struct BackingStore {
    void* data { nullptr };
    size_t pitch { 0 };
    size_t size_in_bytes { 0 };
};

size_t Bitmap::minimum_pitch(size_t physical_width, BitmapFormat format)
{
    size_t element_size;
    switch (determine_storage_format(format)) {
    case StorageFormat::BGRx8888:
    case StorageFormat::BGRA8888:
    case StorageFormat::RGBA8888:
        element_size = 4;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    return physical_width * element_size;
}

static bool size_would_overflow(BitmapFormat format, IntSize size, int scale_factor)
{
    if (size.width() < 0 || size.height() < 0)
        return true;
    // This check is a bit arbitrary, but should protect us from most shenanigans:
    if (size.width() >= INT16_MAX || size.height() >= INT16_MAX || scale_factor < 1 || scale_factor > 4)
        return true;
    // In contrast, this check is absolutely necessary:
    size_t pitch = Bitmap::minimum_pitch(size.width() * scale_factor, format);
    return Checked<size_t>::multiplication_would_overflow(pitch, size.height() * scale_factor);
}

ErrorOr<NonnullRefPtr<Bitmap>> Bitmap::create(BitmapFormat format, IntSize size, int scale_factor, Optional<size_t> pitch)
{
    auto backing_store = TRY(Bitmap::allocate_backing_store(format, size, scale_factor, pitch));
    return AK::adopt_nonnull_ref_or_enomem(new (nothrow) Bitmap(format, size, scale_factor, backing_store));
}

ErrorOr<NonnullRefPtr<Bitmap>> Bitmap::create_shareable(BitmapFormat format, IntSize size, int scale_factor)
{
    if (size_would_overflow(format, size, scale_factor))
        return Error::from_string_literal("Gfx::Bitmap::create_shareable size overflow");

    auto const pitch = minimum_pitch(size.width() * scale_factor, format);
    auto const data_size = size_in_bytes(pitch, size.height() * scale_factor);

    auto buffer = TRY(Core::AnonymousBuffer::create_with_size(round_up_to_power_of_two(data_size, PAGE_SIZE)));
    auto bitmap = TRY(Bitmap::create_with_anonymous_buffer(format, buffer, size, scale_factor));
    return bitmap;
}

Bitmap::Bitmap(BitmapFormat format, IntSize size, int scale_factor, BackingStore const& backing_store)
    : m_size(size)
    , m_scale(scale_factor)
    , m_data(backing_store.data)
    , m_pitch(backing_store.pitch)
    , m_format(format)
{
    VERIFY(!m_size.is_empty());
    VERIFY(!size_would_overflow(format, size, scale_factor));
    VERIFY(m_data);
    VERIFY(backing_store.size_in_bytes == size_in_bytes());
    m_destruction_callback = [data = m_data, size_in_bytes = this->size_in_bytes()] {
        kfree_sized(data, size_in_bytes);
    };
}

ErrorOr<NonnullRefPtr<Bitmap>> Bitmap::create_wrapper(BitmapFormat format, IntSize size, int scale_factor, size_t pitch, void* data, Function<void()>&& destruction_callback)
{
    if (size_would_overflow(format, size, scale_factor))
        return Error::from_string_literal("Gfx::Bitmap::create_wrapper size overflow");
    return adopt_ref(*new Bitmap(format, size, scale_factor, pitch, data, move(destruction_callback)));
}

ErrorOr<NonnullRefPtr<Bitmap>> Bitmap::load_from_file(StringView path, int scale_factor, Optional<IntSize> ideal_size)
{
    if (scale_factor > 1 && path.starts_with("/res/"sv)) {
        auto load_scaled_bitmap = [](StringView path, int scale_factor, Optional<IntSize> ideal_size) -> ErrorOr<NonnullRefPtr<Bitmap>> {
            LexicalPath lexical_path { path };
            StringBuilder highdpi_icon_path;
            TRY(highdpi_icon_path.try_appendff("{}/{}-{}x.{}", lexical_path.dirname(), lexical_path.title(), scale_factor, lexical_path.extension()));

            auto highdpi_icon_string = highdpi_icon_path.string_view();
            auto file = TRY(Core::File::open(highdpi_icon_string, Core::File::OpenMode::Read));

            auto bitmap = TRY(load_from_file(move(file), highdpi_icon_string, ideal_size));
            if (bitmap->width() % scale_factor != 0 || bitmap->height() % scale_factor != 0)
                return Error::from_string_literal("Bitmap::load_from_file: HighDPI image size should be divisible by scale factor");
            bitmap->m_size.set_width(bitmap->width() / scale_factor);
            bitmap->m_size.set_height(bitmap->height() / scale_factor);
            bitmap->m_scale = scale_factor;
            return bitmap;
        };

        auto scaled_bitmap_or_error = load_scaled_bitmap(path, scale_factor, ideal_size);
        if (!scaled_bitmap_or_error.is_error())
            return scaled_bitmap_or_error.release_value();

        auto error = scaled_bitmap_or_error.release_error();
        if (!(error.is_syscall() && error.code() == ENOENT)) {
            dbgln("Couldn't load scaled bitmap: {}", error);
            dbgln("Trying base scale instead.");
        }
    }

    auto file = TRY(Core::File::open(path, Core::File::OpenMode::Read));
    return load_from_file(move(file), path, ideal_size);
}

ErrorOr<NonnullRefPtr<Bitmap>> Bitmap::load_from_file(NonnullOwnPtr<Core::File> file, StringView path, Optional<IntSize> ideal_size)
{
    auto mapped_file = TRY(Core::MappedFile::map_from_file(move(file), path));
    auto mime_type = Core::guess_mime_type_based_on_filename(path);
    return load_from_bytes(mapped_file->bytes(), ideal_size, mime_type);
}

ErrorOr<NonnullRefPtr<Bitmap>> Bitmap::load_from_bytes(ReadonlyBytes bytes, Optional<IntSize> ideal_size, Optional<ByteString> mine_type)
{
    if (auto decoder = TRY(ImageDecoder::try_create_for_raw_bytes(bytes, mine_type))) {
        auto frame = TRY(decoder->frame(0, ideal_size));
        if (auto& bitmap = frame.image)
            return bitmap.release_nonnull();
    }

    return Error::from_string_literal("Gfx::Bitmap unable to load from file");
}

Bitmap::Bitmap(BitmapFormat format, IntSize size, int scale_factor, size_t pitch, void* data, Function<void()>&& destruction_callback)
    : m_size(size)
    , m_scale(scale_factor)
    , m_data(data)
    , m_pitch(pitch)
    , m_format(format)
    , m_destruction_callback(move(destruction_callback))
{
    VERIFY(pitch >= minimum_pitch(size.width() * scale_factor, format));
    VERIFY(!size_would_overflow(format, size, scale_factor));
    // FIXME: assert that `data` is actually long enough!
}

static bool check_size(IntSize size, int scale_factor, BitmapFormat format, unsigned actual_size)
{
    // FIXME: Code duplication of size_in_bytes() and m_pitch
    unsigned expected_size_min = Bitmap::minimum_pitch(size.width() * scale_factor, format) * size.height() * scale_factor;
    unsigned expected_size_max = round_up_to_power_of_two(expected_size_min, PAGE_SIZE);
    if (expected_size_min > actual_size || actual_size > expected_size_max) {
        // Getting here is most likely an error.
        dbgln("Constructing a shared bitmap for format {} and size {} @ {}x, which demands {} bytes, which rounds up to at most {}.",
            static_cast<int>(format),
            size,
            scale_factor,
            expected_size_min,
            expected_size_max);

        dbgln("However, we were given {} bytes, which is outside this range?! Refusing cowardly.", actual_size);
        return false;
    }
    return true;
}

ErrorOr<NonnullRefPtr<Bitmap>> Bitmap::create_with_anonymous_buffer(BitmapFormat format, Core::AnonymousBuffer buffer, IntSize size, int scale_factor)
{
    if (size_would_overflow(format, size, scale_factor))
        return Error::from_string_literal("Gfx::Bitmap::create_with_anonymous_buffer size overflow");

    return adopt_nonnull_ref_or_enomem(new (nothrow) Bitmap(format, move(buffer), size, scale_factor));
}

ErrorOr<NonnullRefPtr<Bitmap>> Bitmap::create_from_serialized_byte_buffer(ByteBuffer&& buffer)
{
    return create_from_serialized_bytes(buffer.bytes());
}

/// Read a bitmap as described by:
/// - actual size
/// - width
/// - height
/// - scale_factor
/// - format
/// - image data (= actual size * u8)
ErrorOr<NonnullRefPtr<Bitmap>> Bitmap::create_from_serialized_bytes(ReadonlyBytes bytes)
{
    FixedMemoryStream stream { bytes };

    auto actual_size = TRY(stream.read_value<size_t>());
    auto width = TRY(stream.read_value<unsigned>());
    auto height = TRY(stream.read_value<unsigned>());
    auto scale_factor = TRY(stream.read_value<unsigned>());
    auto format = TRY(stream.read_value<BitmapFormat>());

    if (format > BitmapFormat::LastValid || format < BitmapFormat::FirstValid)
        return Error::from_string_literal("Gfx::Bitmap::create_from_serialized_byte_buffer: decode failed");

    if (!check_size({ width, height }, scale_factor, format, actual_size))
        return Error::from_string_literal("Gfx::Bitmap::create_from_serialized_byte_buffer: decode failed");

    if (TRY(stream.size()) - TRY(stream.tell()) < actual_size)
        return Error::from_string_literal("Gfx::Bitmap::create_from_serialized_byte_buffer: decode failed");

    auto data = bytes.slice(TRY(stream.tell()), actual_size);

    auto bitmap = TRY(Bitmap::create(format, { width, height }, scale_factor));

    data.copy_to({ bitmap->scanline(0), bitmap->size_in_bytes() });
    return bitmap;
}

ErrorOr<ByteBuffer> Bitmap::serialize_to_byte_buffer() const
{
    auto buffer = TRY(ByteBuffer::create_uninitialized(sizeof(size_t) + 3 * sizeof(unsigned) + sizeof(BitmapFormat) + size_in_bytes()));
    FixedMemoryStream stream { buffer.span() };

    TRY(stream.write_value(size_in_bytes()));
    TRY(stream.write_value<unsigned>(size().width()));
    TRY(stream.write_value<unsigned>(size().height()));
    TRY(stream.write_value<unsigned>(scale()));
    TRY(stream.write_value(m_format));

    auto size = size_in_bytes();
    TRY(stream.write_until_depleted({ scanline(0), size }));

    VERIFY(TRY(stream.tell()) == TRY(stream.size()));

    return buffer;
}

Bitmap::Bitmap(BitmapFormat format, Core::AnonymousBuffer buffer, IntSize size, int scale_factor)
    : m_size(size)
    , m_scale(scale_factor)
    , m_data(buffer.data<void>())
    , m_pitch(minimum_pitch(size.width() * scale_factor, format))
    , m_format(format)
    , m_buffer(move(buffer))
{
    VERIFY(!size_would_overflow(format, size, scale_factor));
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> Bitmap::clone() const
{
    auto new_bitmap = TRY(Bitmap::create(format(), size(), scale()));

    VERIFY(size_in_bytes() == new_bitmap->size_in_bytes());
    memcpy(new_bitmap->scanline(0), scanline(0), size_in_bytes());

    return new_bitmap;
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> Bitmap::rotated(Gfx::RotationDirection rotation_direction) const
{
    if (rotation_direction == Gfx::RotationDirection::Flip) {
        auto new_bitmap = TRY(Gfx::Bitmap::create(format(), { width(), height() }, scale()));

        auto w = this->physical_width();
        auto h = this->physical_height();
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++)
                new_bitmap->set_pixel(w - i - 1, h - j - 1, this->get_pixel(i, j));
        }

        return new_bitmap;
    }

    auto new_bitmap = TRY(Gfx::Bitmap::create(this->format(), { height(), width() }, scale()));

    auto w = this->physical_width();
    auto h = this->physical_height();
    for (int i = 0; i < w; i++) {
        for (int j = 0; j < h; j++) {
            Color color;
            if (rotation_direction == Gfx::RotationDirection::CounterClockwise)
                color = this->get_pixel(w - i - 1, j);
            else
                color = this->get_pixel(i, h - j - 1);

            new_bitmap->set_pixel(j, i, color);
        }
    }

    return new_bitmap;
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> Bitmap::flipped(Gfx::Orientation orientation) const
{
    auto new_bitmap = TRY(Gfx::Bitmap::create(this->format(), { width(), height() }, scale()));

    auto w = this->physical_width();
    auto h = this->physical_height();
    for (int i = 0; i < w; i++) {
        for (int j = 0; j < h; j++) {
            Color color = this->get_pixel(i, j);
            if (orientation == Orientation::Vertical)
                new_bitmap->set_pixel(i, h - j - 1, color);
            else
                new_bitmap->set_pixel(w - i - 1, j, color);
        }
    }

    return new_bitmap;
}

void Bitmap::apply_mask(Gfx::Bitmap const& mask, MaskKind mask_kind)
{
    VERIFY(size() == mask.size());

    for (int y = 0; y < height(); y++) {
        for (int x = 0; x < width(); x++) {
            auto color = get_pixel(x, y);
            auto mask_color = mask.get_pixel(x, y);
            if (mask_kind == MaskKind::Luminance) {
                color = color.with_alpha(color.alpha() * mask_color.alpha() * mask_color.luminosity() / (255 * 255));
            } else {
                VERIFY(mask_kind == MaskKind::Alpha);
                color = color.with_alpha(color.alpha() * mask_color.alpha() / 255);
            }
            set_pixel(x, y, color);
        }
    }
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> Bitmap::scaled(int sx, int sy) const
{
    VERIFY(sx >= 0 && sy >= 0);
    if (sx == 1 && sy == 1)
        return clone();

    auto new_bitmap = TRY(Gfx::Bitmap::create(format(), { width() * sx, height() * sy }, scale()));

    auto old_width = physical_width();
    auto old_height = physical_height();

    for (int y = 0; y < old_height; y++) {
        for (int x = 0; x < old_width; x++) {
            auto color = get_pixel(x, y);

            auto base_x = x * sx;
            auto base_y = y * sy;
            for (int new_y = base_y; new_y < base_y + sy; new_y++) {
                for (int new_x = base_x; new_x < base_x + sx; new_x++) {
                    new_bitmap->set_pixel(new_x, new_y, color);
                }
            }
        }
    }

    return new_bitmap;
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> Bitmap::scaled(float sx, float sy) const
{
    VERIFY(sx >= 0.0f && sy >= 0.0f);
    if (floorf(sx) == sx && floorf(sy) == sy)
        return scaled(static_cast<int>(sx), static_cast<int>(sy));

    int scaled_width = (int)ceilf(sx * (float)width());
    int scaled_height = (int)ceilf(sy * (float)height());
    return scaled_to_size({ scaled_width, scaled_height });
}

// http://fourier.eng.hmc.edu/e161/lectures/resize/node3.html
ErrorOr<NonnullRefPtr<Gfx::Bitmap>> Bitmap::scaled_to_size(Gfx::IntSize size) const
{
    auto new_bitmap = TRY(Gfx::Bitmap::create(format(), size, scale()));

    auto old_width = physical_width();
    auto old_height = physical_height();
    auto new_width = new_bitmap->physical_width();
    auto new_height = new_bitmap->physical_height();

    if (old_width == 1 && old_height == 1) {
        new_bitmap->fill(get_pixel(0, 0));
        return new_bitmap;
    }

    if (old_width > 1 && old_height > 1) {
        // The interpolation goes out of bounds on the bottom- and right-most edges.
        // We handle those in two specialized loops not only to make them faster, but
        // also to avoid four branch checks for every pixel.
        for (int y = 0; y < new_height - 1; y++) {
            for (int x = 0; x < new_width - 1; x++) {
                auto p = static_cast<float>(x) * static_cast<float>(old_width - 1) / static_cast<float>(new_width - 1);
                auto q = static_cast<float>(y) * static_cast<float>(old_height - 1) / static_cast<float>(new_height - 1);

                int i = floorf(p);
                int j = floorf(q);
                float u = p - static_cast<float>(i);
                float v = q - static_cast<float>(j);

                auto a = get_pixel(i, j);
                auto b = get_pixel(i + 1, j);
                auto c = get_pixel(i, j + 1);
                auto d = get_pixel(i + 1, j + 1);

                auto e = a.mixed_with(b, u);
                auto f = c.mixed_with(d, u);
                auto color = e.mixed_with(f, v);
                new_bitmap->set_pixel(x, y, color);
            }
        }

        // Bottom strip (excluding last pixel)
        auto old_bottom_y = old_height - 1;
        auto new_bottom_y = new_height - 1;
        for (int x = 0; x < new_width - 1; x++) {
            auto p = static_cast<float>(x) * static_cast<float>(old_width - 1) / static_cast<float>(new_width - 1);

            int i = floorf(p);
            float u = p - static_cast<float>(i);

            auto a = get_pixel(i, old_bottom_y);
            auto b = get_pixel(i + 1, old_bottom_y);
            auto color = a.mixed_with(b, u);
            new_bitmap->set_pixel(x, new_bottom_y, color);
        }

        // Right strip (excluding last pixel)
        auto old_right_x = old_width - 1;
        auto new_right_x = new_width - 1;
        for (int y = 0; y < new_height - 1; y++) {
            auto q = static_cast<float>(y) * static_cast<float>(old_height - 1) / static_cast<float>(new_height - 1);

            int j = floorf(q);
            float v = q - static_cast<float>(j);

            auto c = get_pixel(old_right_x, j);
            auto d = get_pixel(old_right_x, j + 1);

            auto color = c.mixed_with(d, v);
            new_bitmap->set_pixel(new_right_x, y, color);
        }

        // Bottom-right pixel
        new_bitmap->set_pixel(new_width - 1, new_height - 1, get_pixel(physical_width() - 1, physical_height() - 1));
        return new_bitmap;
    } else if (old_height == 1) {
        // Copy horizontal strip multiple times (excluding last pixel to out of bounds).
        auto old_bottom_y = old_height - 1;
        for (int x = 0; x < new_width - 1; x++) {
            auto p = static_cast<float>(x) * static_cast<float>(old_width - 1) / static_cast<float>(new_width - 1);
            int i = floorf(p);
            float u = p - static_cast<float>(i);

            auto a = get_pixel(i, old_bottom_y);
            auto b = get_pixel(i + 1, old_bottom_y);
            auto color = a.mixed_with(b, u);
            for (int new_bottom_y = 0; new_bottom_y < new_height; new_bottom_y++) {
                // Interpolate color only once and then copy into all columns.
                new_bitmap->set_pixel(x, new_bottom_y, color);
            }
        }
        for (int new_bottom_y = 0; new_bottom_y < new_height; new_bottom_y++) {
            // Copy last pixel of horizontal strip
            new_bitmap->set_pixel(new_width - 1, new_bottom_y, get_pixel(physical_width() - 1, old_bottom_y));
        }
        return new_bitmap;
    } else if (old_width == 1) {
        // Copy vertical strip multiple times (excluding last pixel to avoid out of bounds).
        auto old_right_x = old_width - 1;
        for (int y = 0; y < new_height - 1; y++) {
            auto q = static_cast<float>(y) * static_cast<float>(old_height - 1) / static_cast<float>(new_height - 1);
            int j = floorf(q);
            float v = q - static_cast<float>(j);

            auto c = get_pixel(old_right_x, j);
            auto d = get_pixel(old_right_x, j + 1);

            auto color = c.mixed_with(d, v);
            for (int new_right_x = 0; new_right_x < new_width; new_right_x++) {
                // Interpolate color only once and copy into all rows.
                new_bitmap->set_pixel(new_right_x, y, color);
            }
        }
        for (int new_right_x = 0; new_right_x < new_width; new_right_x++) {
            // Copy last pixel of vertical strip
            new_bitmap->set_pixel(new_right_x, new_height - 1, get_pixel(old_right_x, physical_height() - 1));
        }
    }
    return new_bitmap;
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> Bitmap::cropped(Gfx::IntRect crop, Optional<BitmapFormat> new_bitmap_format) const
{
    auto new_bitmap = TRY(Gfx::Bitmap::create(new_bitmap_format.value_or(format()), { crop.width(), crop.height() }, scale()));
    auto scaled_crop = crop * scale();

    for (int y = 0; y < scaled_crop.height(); ++y) {
        for (int x = 0; x < scaled_crop.width(); ++x) {
            int global_x = x + scaled_crop.left();
            int global_y = y + scaled_crop.top();
            if (global_x >= physical_width() || global_y >= physical_height() || global_x < 0 || global_y < 0) {
                new_bitmap->set_pixel(x, y, Gfx::Color::Black);
            } else {
                new_bitmap->set_pixel(x, y, get_pixel(global_x, global_y));
            }
        }
    }
    return new_bitmap;
}

ErrorOr<NonnullRefPtr<Bitmap>> Bitmap::to_bitmap_backed_by_anonymous_buffer() const
{
    if (m_buffer.is_valid()) {
        // FIXME: The const_cast here is awkward.
        return NonnullRefPtr { const_cast<Bitmap&>(*this) };
    }
    auto buffer = TRY(Core::AnonymousBuffer::create_with_size(round_up_to_power_of_two(size_in_bytes(), PAGE_SIZE)));
    auto bitmap = TRY(Bitmap::create_with_anonymous_buffer(m_format, move(buffer), size(), scale()));
    memcpy(bitmap->scanline(0), scanline(0), size_in_bytes());
    return bitmap;
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> Bitmap::inverted() const
{
    auto inverted_bitmap = TRY(clone());
    for (auto y = 0; y < height(); y++) {
        for (auto x = 0; x < width(); x++)
            inverted_bitmap->set_pixel(x, y, get_pixel(x, y).inverted());
    }
    return inverted_bitmap;
}

Bitmap::~Bitmap()
{
    if (m_destruction_callback)
        m_destruction_callback();
    m_data = nullptr;
}

void Bitmap::strip_alpha_channel()
{
    VERIFY(m_format == BitmapFormat::BGRA8888 || m_format == BitmapFormat::BGRx8888);
    for (ARGB32& pixel : *this)
        pixel = 0xff000000 | (pixel & 0xffffff);
    m_format = BitmapFormat::BGRx8888;
}

void Bitmap::fill(Color color)
{
    for (int y = 0; y < physical_height(); ++y) {
        auto* scanline = this->scanline(y);
        fast_u32_fill(scanline, color.value(), physical_width());
    }
}

Gfx::ShareableBitmap Bitmap::to_shareable_bitmap() const
{
    auto bitmap_or_error = to_bitmap_backed_by_anonymous_buffer();
    if (bitmap_or_error.is_error())
        return {};
    return Gfx::ShareableBitmap { bitmap_or_error.release_value_but_fixme_should_propagate_errors(), Gfx::ShareableBitmap::ConstructWithKnownGoodBitmap };
}

ErrorOr<BackingStore> Bitmap::allocate_backing_store(BitmapFormat format, IntSize size, int scale_factor, Optional<size_t> pitch)
{
    if (size.is_empty())
        return Error::from_string_literal("Gfx::Bitmap backing store size is empty");

    if (size_would_overflow(format, size, scale_factor))
        return Error::from_string_literal("Gfx::Bitmap backing store size overflow");

    if (!pitch.has_value())
        pitch = minimum_pitch(size.width() * scale_factor, format);
    auto const data_size_in_bytes = size_in_bytes(pitch.value(), size.height() * scale_factor);

    void* data = kcalloc(1, data_size_in_bytes);
    if (data == nullptr)
        return Error::from_errno(errno);
    return BackingStore { data, pitch.value(), data_size_in_bytes };
}

bool Bitmap::visually_equals(Bitmap const& other) const
{
    auto own_width = width();
    auto own_height = height();
    if (other.width() != own_width || other.height() != own_height)
        return false;

    for (auto y = 0; y < own_height; ++y) {
        for (auto x = 0; x < own_width; ++x) {
            if (get_pixel(x, y) != other.get_pixel(x, y))
                return false;
        }
    }

    return true;
}

Optional<Color> Bitmap::solid_color(u8 alpha_threshold) const
{
    Optional<Color> color;
    for (auto y = 0; y < height(); ++y) {
        for (auto x = 0; x < width(); ++x) {
            auto const& pixel = get_pixel(x, y);
            if (has_alpha_channel() && pixel.alpha() <= alpha_threshold)
                continue;
            if (!color.has_value())
                color = pixel;
            else if (pixel != color)
                return {};
        }
    }
    return color;
}

void Bitmap::flood_visit_from_point(Gfx::IntPoint start_point, int threshold,
    Function<void(Gfx::IntPoint location)> pixel_reached)
{

    VERIFY(rect().contains(start_point));

    auto target_color = get_pixel(start_point.x(), start_point.y());

    float threshold_normalized_squared = (threshold / 100.0f) * (threshold / 100.0f);

    Queue<Gfx::IntPoint> points_to_visit = Queue<Gfx::IntPoint>();

    points_to_visit.enqueue(start_point);
    pixel_reached(start_point);
    auto flood_mask = AK::Bitmap::create(width() * height(), false).release_value_but_fixme_should_propagate_errors();

    flood_mask.set(width() * start_point.y() + start_point.x(), true);

    // This implements a non-recursive flood fill. This is a breadth-first search of paintable neighbors
    // As we find neighbors that are reachable we call the location_reached callback, add them to the queue, and mark them in the mask
    while (!points_to_visit.is_empty()) {
        auto current_point = points_to_visit.dequeue();
        auto candidate_points = Array {
            current_point.moved_left(1),
            current_point.moved_right(1),
            current_point.moved_up(1),
            current_point.moved_down(1)
        };
        for (auto candidate_point : candidate_points) {
            auto flood_mask_index = width() * candidate_point.y() + candidate_point.x();
            if (!rect().contains(candidate_point))
                continue;

            auto pixel_color = get_pixel<Gfx::StorageFormat::BGRA8888>(candidate_point.x(), candidate_point.y());
            auto can_paint = pixel_color.distance_squared_to(target_color) <= threshold_normalized_squared;

            if (flood_mask.get(flood_mask_index) == false && can_paint) {
                points_to_visit.enqueue(candidate_point);
                pixel_reached(candidate_point);
            }

            flood_mask.set(flood_mask_index, true);
        }
    }
}

}

namespace IPC {

template<>
ErrorOr<void> encode(Encoder& encoder, AK::NonnullRefPtr<Gfx::Bitmap> const& bitmap)
{
    Core::AnonymousBuffer buffer;
    if (bitmap->anonymous_buffer().is_valid()) {
        buffer = bitmap->anonymous_buffer();
    } else {
        buffer = MUST(Core::AnonymousBuffer::create_with_size(bitmap->size_in_bytes()));
        memcpy(buffer.data<void>(), bitmap->scanline(0), bitmap->size_in_bytes());
    }
    TRY(encoder.encode(TRY(IPC::File::clone_fd(buffer.fd()))));
    TRY(encoder.encode(static_cast<u32>(bitmap->format())));
    TRY(encoder.encode(bitmap->size_in_bytes()));
    TRY(encoder.encode(bitmap->pitch()));
    TRY(encoder.encode(bitmap->size()));
    TRY(encoder.encode(bitmap->scale()));
    return {};
}

template<>
ErrorOr<AK::NonnullRefPtr<Gfx::Bitmap>> decode(Decoder& decoder)
{
    auto anon_file = TRY(decoder.decode<IPC::File>());
    auto raw_bitmap_format = TRY(decoder.decode<u32>());
    if (!Gfx::is_valid_bitmap_format(raw_bitmap_format))
        return Error::from_string_literal("IPC: Invalid Gfx::ShareableBitmap format");
    auto bitmap_format = static_cast<Gfx::BitmapFormat>(raw_bitmap_format);
    auto size_in_bytes = TRY(decoder.decode<size_t>());
    auto pitch = TRY(decoder.decode<size_t>());
    auto size = TRY(decoder.decode<Gfx::IntSize>());
    auto scale = TRY(decoder.decode<int>());
    auto* data = TRY(Core::System::mmap(nullptr, round_up_to_power_of_two(size_in_bytes, PAGE_SIZE), PROT_READ | PROT_WRITE, MAP_SHARED, anon_file.fd(), 0));
    return Gfx::Bitmap::create_wrapper(bitmap_format, size, scale, pitch, data, [data, size_in_bytes] {
        MUST(Core::System::munmap(data, size_in_bytes));
    });
}

}
