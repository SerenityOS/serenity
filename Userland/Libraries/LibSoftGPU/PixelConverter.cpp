/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/Error.h>
#include <AK/FloatingPoint.h>
#include <LibSoftGPU/PixelConverter.h>

namespace SoftGPU {

template<typename T>
static constexpr T reverse_component_bytes_if_needed(T value, GPU::ImageDataLayout const& image_data_layout)
requires(sizeof(T) == 2 || sizeof(T) == 4)
{
    if (image_data_layout.packing.component_bytes_order == GPU::ComponentBytesOrder::Normal)
        return value;
    VERIFY(image_data_layout.pixel_type.bits == GPU::PixelComponentBits::AllBits);

    auto* u8_ptr = reinterpret_cast<u8*>(&value);
    if constexpr (sizeof(T) == 2) {
        swap(u8_ptr[0], u8_ptr[1]);
    } else if constexpr (sizeof(T) == 4) {
        swap(u8_ptr[0], u8_ptr[3]);
        swap(u8_ptr[1], u8_ptr[2]);
    }
    return value;
}

static constexpr FloatVector4 decode_component_order_for_format(FloatVector4 const& components, GPU::PixelFormat format)
{
    switch (format) {
    case GPU::PixelFormat::Alpha:
        return { 0.f, 0.f, 0.f, components[0] };
    case GPU::PixelFormat::BGR:
        return { components[2], components[1], components[0], 1.f };
    case GPU::PixelFormat::BGRA:
        return { components[2], components[1], components[0], components[3] };
    case GPU::PixelFormat::Blue:
        return { 0.f, 0.f, components[0], 1.f };
    case GPU::PixelFormat::ColorIndex:
    case GPU::PixelFormat::DepthComponent:
    case GPU::PixelFormat::StencilIndex:
        return { components[0], 0.f, 0.f, 0.f };
    case GPU::PixelFormat::Green:
        return { 0.f, components[0], 0.f, 1.f };
    case GPU::PixelFormat::Intensity:
        return { components[0], components[0], components[0], components[0] };
    case GPU::PixelFormat::Luminance:
        return { components[0], components[0], components[0], 1.f };
    case GPU::PixelFormat::LuminanceAlpha:
        return { components[0], components[0], components[0], components[1] };
    case GPU::PixelFormat::Red:
        return { components[0], 0.f, 0.f, 1.f };
    case GPU::PixelFormat::RGB:
        return { components[0], components[1], components[2], 1.f };
    case GPU::PixelFormat::RGBA:
        return components;
    }
    VERIFY_NOT_REACHED();
}

static constexpr FloatVector4 encode_component_order_for_format(FloatVector4 const& components, GPU::PixelFormat format)
{
    switch (format) {
    case GPU::PixelFormat::Alpha:
        return { components[3], 0.f, 0.f, 0.f };
    case GPU::PixelFormat::BGR:
        return { components[2], components[1], components[0], 0.f };
    case GPU::PixelFormat::BGRA:
        return { components[2], components[1], components[0], components[3] };
    case GPU::PixelFormat::Blue:
        return { components[2], 0.f, 0.f, 0.f };
    case GPU::PixelFormat::ColorIndex:
    case GPU::PixelFormat::DepthComponent:
    case GPU::PixelFormat::Intensity:
    case GPU::PixelFormat::Luminance:
    case GPU::PixelFormat::Red:
    case GPU::PixelFormat::RGB:
    case GPU::PixelFormat::RGBA:
    case GPU::PixelFormat::StencilIndex:
        return components;
    case GPU::PixelFormat::Green:
        return { components[1], 0.f, 0.f, 0.f };
    case GPU::PixelFormat::LuminanceAlpha:
        return { components[0], components[3], 0.f, 0.f };
    }
    VERIFY_NOT_REACHED();
}

template<typename S, typename O>
static int read_pixel_values(u8 const* input_data, Array<O, 4>& output_values, GPU::ImageDataLayout const& layout)
{
    auto const& pixel_type = layout.pixel_type;
    auto const number_of_data_reads = GPU::number_of_components(pixel_type.format) / GPU::number_of_components(pixel_type.bits);

    for (int i = 0; i < number_of_data_reads; ++i) {
        auto storage_value = reinterpret_cast<S const*>(input_data)[i];
        if (layout.pixel_type.bits == GPU::PixelComponentBits::AllBits) {
            if constexpr (sizeof(S) == 2 || sizeof(S) == 4)
                storage_value = reverse_component_bytes_if_needed(storage_value, layout);
        }
        O value = storage_value;

        // Special case: convert HalfFloat to regular float
        if constexpr (IsSame<O, float>) {
            if (pixel_type.data_type == GPU::PixelDataType::HalfFloat)
                value = convert_to_native_float(FloatingPointBits<1, 5, 10>(storage_value));
        }

        output_values[i] = value;
    }
    return number_of_data_reads;
}

template<typename T>
constexpr FloatVector4 extract_component_values(Span<T> data_values, GPU::PixelType const& pixel_type)
{
    // FIXME: implement fixed point conversion for ::StencilIndex
    // FIXME: stencil components should account for GL_MAP_STENCIL
    // FIXME: stencil components should get GL_INDEX_SHIFT and GL_INDEX_OFFSET applied
    // FIXME: depth components should get GL_DEPTH_SCALE and GL_DEPTH_BIAS applied
    // FIXME: color components should get GL_C_SCALE and GL_C_BIAS applied

    auto const number_of_values = data_values.size();
    auto const bits_number_of_components = number_of_components(pixel_type.bits);
    VERIFY(bits_number_of_components == 1 || bits_number_of_components == number_of_components(pixel_type.format));

    // Maps a signed value to -1.0f..1.0f
    auto signed_to_float = [](T value) -> float {
        auto constexpr number_of_bits = sizeof(T) * 8 - 1;
        return max(static_cast<float>(value / static_cast<float>(1 << number_of_bits)), -1.f);
    };

    // Maps an unsigned value to 0.0f..1.0f
    auto unsigned_to_float = [](T value, u8 const number_of_bits) -> float {
        return static_cast<float>(value / static_cast<double>((1ull << number_of_bits) - 1));
    };

    // Handle full data values (1 or more)
    if (pixel_type.bits == GPU::PixelComponentBits::AllBits) {
        FloatVector4 components;
        for (size_t i = 0; i < number_of_values; ++i) {
            if constexpr (IsSigned<T>)
                components[i] = signed_to_float(data_values[i]);
            else
                components[i] = unsigned_to_float(data_values[i], sizeof(T) * 8);
        }
        return components;
    }

    VERIFY(number_of_values == 1);
    T const value = data_values[0];
    auto bitfields = pixel_component_bitfield_lengths(pixel_type.bits);

    // Map arbitrary bitfields to floats
    u8 remaining_width = 0;
    for (auto bitwidth : bitfields)
        remaining_width += bitwidth;

    // "By default the components are laid out from msb (most-significant bit) to lsb (least-significant bit)"
    FloatVector4 components;
    for (auto i = 0; i < 4; ++i) {
        auto bitwidth = bitfields[i];
        if (bitwidth == 0)
            break;
        remaining_width -= bitwidth;
        components[i] = unsigned_to_float((value >> remaining_width) & ((1 << bitwidth) - 1), bitwidth);
    }
    return components;
}

template<>
constexpr FloatVector4 extract_component_values(Span<float> data_values, GPU::PixelType const&)
{
    FloatVector4 components;
    for (size_t i = 0; i < data_values.size(); ++i)
        components[i] = data_values[i];
    return components;
}

template<typename T>
static FloatVector4 pixel_values_to_components(Span<T> values, GPU::PixelType const& pixel_type)
{
    // Deconstruct read value(s) into separate components
    auto components = extract_component_values(values, pixel_type);
    if (pixel_type.components_order == GPU::ComponentsOrder::Reversed)
        components = { components[3], components[2], components[1], components[0] };

    // Reconstruct component values in order
    auto component_values = decode_component_order_for_format(components, pixel_type.format);
    component_values.clamp(0.f, 1.f);
    return component_values;
}

FloatVector4 PixelConverter::read_pixel(u8 const** input_data)
{
    auto read_components = [&]<typename S, typename O>() {
        Array<O, 4> values;
        auto number_of_values = read_pixel_values<S, O>(*input_data, values, m_input_specification);
        *input_data += number_of_values * sizeof(O);
        return pixel_values_to_components(values.span().trim(number_of_values), m_input_specification.pixel_type);
    };
    switch (m_input_specification.pixel_type.data_type) {
    case GPU::PixelDataType::Bitmap:
        VERIFY_NOT_REACHED();
    case GPU::PixelDataType::Byte:
        return read_components.template operator()<i8, i8>();
    case GPU::PixelDataType::Float:
        return read_components.template operator()<float, float>();
    case GPU::PixelDataType::HalfFloat:
        return read_components.template operator()<u16, float>();
    case GPU::PixelDataType::Int:
        return read_components.template operator()<i32, i32>();
    case GPU::PixelDataType::Short:
        return read_components.template operator()<i16, i16>();
    case GPU::PixelDataType::UnsignedByte:
        return read_components.template operator()<u8, u8>();
    case GPU::PixelDataType::UnsignedInt:
        return read_components.template operator()<u32, u32>();
    case GPU::PixelDataType::UnsignedShort:
        return read_components.template operator()<u16, u16>();
    }
    VERIFY_NOT_REACHED();
}

static constexpr void write_pixel_as_type(u8** output_data, float value, GPU::ImageDataLayout layout)
{
    auto write_value = [&output_data, &layout]<typename T>(T value) -> void {
        if constexpr (sizeof(T) == 2 || sizeof(T) == 4)
            value = reverse_component_bytes_if_needed(value, layout);
        **reinterpret_cast<T**>(output_data) = value;
        (*output_data) += sizeof(T);
    };
    auto constexpr float_to_signed = []<typename T>(float value) -> T {
        auto const signed_max = 1ull << (sizeof(T) * 8 - 1);
        auto const unsigned_max = 2 * signed_max - 1;
        return round_to<T>((static_cast<double>(value) + 1.) / 2. * unsigned_max - signed_max);
    };
    auto constexpr float_to_unsigned = []<typename T>(float value) -> T {
        auto const unsigned_max = (1ull << (sizeof(T) * 8)) - 1;
        return round_to<T>(static_cast<double>(value) * unsigned_max);
    };
    switch (layout.pixel_type.data_type) {
    case GPU::PixelDataType::Bitmap:
        VERIFY_NOT_REACHED();
    case GPU::PixelDataType::Byte:
        write_value(float_to_signed.operator()<i8>(value));
        break;
    case GPU::PixelDataType::Float:
        write_value(value);
        break;
    case GPU::PixelDataType::HalfFloat:
        write_value(static_cast<u16>(convert_from_native_float<FloatingPointBits<1, 5, 10>>(value).bits()));
        break;
    case GPU::PixelDataType::Int:
        write_value(float_to_signed.operator()<i32>(value));
        break;
    case GPU::PixelDataType::Short:
        write_value(float_to_signed.operator()<i16>(value));
        break;
    case GPU::PixelDataType::UnsignedByte:
        write_value(float_to_unsigned.operator()<u8>(value));
        break;
    case GPU::PixelDataType::UnsignedInt:
        write_value(float_to_unsigned.operator()<u32>(value));
        break;
    case GPU::PixelDataType::UnsignedShort:
        write_value(float_to_unsigned.operator()<u16>(value));
        break;
    }
}

void constexpr write_pixel_as_bitfield(u8** output_data, FloatVector4 const& components, GPU::PixelType const& pixel_type)
{
    auto constexpr float_to_unsigned = [](float value, u8 bits) {
        auto unsigned_max = (1ull << bits) - 1;
        return round_to<u64>(value * unsigned_max);
    };

    // Construct value with concatenated bitfields - first component has most significant bits
    auto bitfields = pixel_component_bitfield_lengths(pixel_type.bits);
    u64 value = 0;
    u8 bitsize = 0;
    for (auto i = 0; i < 4; ++i) {
        value <<= bitsize;
        bitsize = bitfields[i];
        if (bitsize == 0)
            break;
        value |= float_to_unsigned(components[i], bitsize);
    }

    // Write out the value in the requested data type
    auto write_value = [&output_data]<typename T>(T value) -> void {
        **reinterpret_cast<T**>(output_data) = value;
        (*output_data) += sizeof(T);
    };
    switch (pixel_type.data_type) {
    case GPU::PixelDataType::UnsignedByte:
        write_value.operator()<u8>(value);
        break;
    case GPU::PixelDataType::UnsignedInt:
        write_value.operator()<u32>(value);
        break;
    case GPU::PixelDataType::UnsignedShort:
        write_value.operator()<u16>(value);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void PixelConverter::write_pixel(u8** output_data, FloatVector4 const& components)
{
    // NOTE: `components` is already clamped to 0.f..1.f

    // Reorder float components to data order
    auto const& pixel_type = m_output_specification.pixel_type;
    auto output_components = encode_component_order_for_format(components, pixel_type.format);
    if (pixel_type.components_order == GPU::ComponentsOrder::Reversed)
        output_components = { output_components[3], output_components[2], output_components[1], output_components[0] };

    // Write components as full data types
    auto const number_of_components_in_pixel = number_of_components(pixel_type.format);
    if (pixel_type.bits == GPU::PixelComponentBits::AllBits) {
        for (u8 i = 0; i < number_of_components_in_pixel; ++i)
            write_pixel_as_type(output_data, output_components[i], m_output_specification);
        return;
    }

    // Write components as a concatenated bitfield value
    VERIFY(number_of_components_in_pixel == number_of_components(pixel_type.bits));
    write_pixel_as_bitfield(output_data, output_components, pixel_type);
}

static constexpr GPU::ImageSelection restrain_selection_within_dimensions(GPU::ImageSelection selection, GPU::DimensionSpecification const& dimensions)
{
    if (selection.offset_x < 0) {
        selection.width += selection.offset_x;
        selection.offset_x = 0;
    }
    if (selection.offset_y < 0) {
        selection.height += selection.offset_y;
        selection.offset_y = 0;
    }
    if (selection.offset_z < 0) {
        selection.depth += selection.offset_z;
        selection.offset_z = 0;
    }

    if (selection.offset_x + selection.width > dimensions.width)
        selection.width = dimensions.width - selection.offset_x;
    if (selection.offset_y + selection.height > dimensions.height)
        selection.height = dimensions.height - selection.offset_y;
    if (selection.offset_z + selection.depth > dimensions.depth)
        selection.depth = dimensions.depth - selection.offset_z;

    return selection;
}

ErrorOr<void> PixelConverter::convert(void const* input_data, void* output_data, Function<void(FloatVector4&)> transform)
{
    // Verify pixel data specifications
    auto validate_image_data_layout = [](GPU::ImageDataLayout const& specification) -> ErrorOr<void> {
        if (specification.packing.row_stride > 0
            && specification.dimensions.width > specification.packing.row_stride)
            return Error::from_string_view("Width exceeds the row stride"sv);

        if (specification.packing.depth_stride > 0
            && specification.dimensions.height > specification.packing.depth_stride)
            return Error::from_string_view("Height exceeds the depth stride"sv);

        // NOTE: GL_BITMAP is removed from current OpenGL specs. Since it is largely unsupported and it
        //       requires extra logic (i.e. 8 vs. 1 pixel packing/unpacking), we also do not support it.
        if (specification.pixel_type.data_type == GPU::PixelDataType::Bitmap)
            return Error::from_string_view("Bitmap is unsupported"sv);

        return {};
    };
    TRY(validate_image_data_layout(m_input_specification));
    TRY(validate_image_data_layout(m_output_specification));

    // Restrain input and output selection:
    // - selection dimensions should be equal
    // - selection offsets cannot be negative
    // - selection bounds cannot exceed the image dimensions
    auto const& input_dimensions = m_input_specification.dimensions;
    auto const& output_dimensions = m_output_specification.dimensions;
    auto input_selection = restrain_selection_within_dimensions(m_input_specification.selection, input_dimensions);
    auto const& output_selection = restrain_selection_within_dimensions(m_output_specification.selection, output_dimensions);

    input_selection.width = min(input_selection.width, output_selection.width);
    input_selection.height = min(input_selection.height, output_selection.height);
    input_selection.depth = min(input_selection.depth, output_selection.depth);

    // Set up copy parameters
    auto const& input_packing = m_input_specification.packing;
    auto const input_pixels_per_row = input_packing.row_stride > 0 ? input_packing.row_stride : input_dimensions.width;
    auto const input_pixel_size_in_bytes = pixel_size_in_bytes(m_input_specification.pixel_type);
    auto const input_row_width_bytes = input_pixels_per_row * input_pixel_size_in_bytes;
    auto const input_byte_alignment = input_packing.byte_alignment;
    auto const input_row_stride = input_row_width_bytes + (input_byte_alignment - input_row_width_bytes % input_byte_alignment) % input_byte_alignment;
    auto const input_rows_per_image = input_packing.depth_stride > 0 ? input_packing.depth_stride : input_dimensions.height;
    auto const input_depth_stride = input_rows_per_image * input_row_stride;

    auto const& output_packing = m_output_specification.packing;
    auto const output_pixels_per_row = output_packing.row_stride > 0 ? output_packing.row_stride : output_dimensions.width;
    auto const output_pixel_size_in_bytes = pixel_size_in_bytes(m_output_specification.pixel_type);
    auto const output_row_width_bytes = output_pixels_per_row * output_pixel_size_in_bytes;
    auto const output_byte_alignment = output_packing.byte_alignment;
    auto const output_row_stride = output_row_width_bytes + (output_byte_alignment - output_row_width_bytes % output_byte_alignment) % output_byte_alignment;
    auto const output_rows_per_image = output_packing.depth_stride > 0 ? output_packing.depth_stride : output_dimensions.height;
    auto const output_depth_stride = output_rows_per_image * output_row_stride;

    // Copy all pixels from input to output
    auto input_bytes = reinterpret_cast<u8 const*>(input_data);
    auto output_bytes = reinterpret_cast<u8*>(output_data);
    auto output_z = output_selection.offset_z;
    for (u32 input_z = input_selection.offset_z; input_z < input_selection.offset_z + input_selection.depth; ++input_z) {
        auto output_y = output_selection.offset_y;
        for (u32 input_y = input_selection.offset_y; input_y < input_selection.offset_y + input_selection.height; ++input_y) {
            auto const* input_scanline = &input_bytes[input_z * input_depth_stride
                + input_y * input_row_stride
                + input_selection.offset_x * input_pixel_size_in_bytes];
            auto* output_scanline = &output_bytes[output_z * output_depth_stride
                + output_y * output_row_stride
                + output_selection.offset_x * output_pixel_size_in_bytes];
            for (u32 input_x = input_selection.offset_x; input_x < input_selection.offset_x + input_selection.width; ++input_x) {
                auto pixel_components = read_pixel(&input_scanline);
                if (transform)
                    transform(pixel_components);
                write_pixel(&output_scanline, pixel_components);
            }
            ++output_y;
        }
        ++output_z;
    }
    return {};
}

}
