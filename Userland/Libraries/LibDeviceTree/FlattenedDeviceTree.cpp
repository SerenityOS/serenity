/*
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <AK/IterationDecision.h>
#include <AK/MemoryStream.h>
#include <AK/StringView.h>
#include <LibDeviceTree/FlattenedDeviceTree.h>

namespace DeviceTree {

static ErrorOr<StringView> read_string_view(ReadonlyBytes bytes, StringView error_string)
{
    auto len = strnlen(reinterpret_cast<char const*>(bytes.data()), bytes.size());
    if (len == bytes.size()) {
        return Error::from_string_view_or_print_error_and_return_errno(error_string, EINVAL);
    }
    return StringView { bytes.slice(0, len) };
}

ErrorOr<void> walk_device_tree(FlattenedDeviceTreeHeader const& header, ReadonlyBytes raw_device_tree, DeviceTreeCallbacks callbacks)
{
    ReadonlyBytes struct_bytes { raw_device_tree.data() + header.off_dt_struct, header.size_dt_struct };
    FixedMemoryStream stream(struct_bytes);
    char const* begin_strings_block = reinterpret_cast<char const*>(raw_device_tree.data() + header.off_dt_strings);

    FlattenedDeviceTreeTokenType prev_token = EndNode;
    StringView current_node_name;

    while (!stream.is_eof()) {
        auto current_token = TRY(stream.read_value<BigEndian<u32>>());

        switch (current_token) {
        case BeginNode: {
            current_node_name = TRY(read_string_view(struct_bytes.slice(stream.offset()), "Non-null terminated name for FDT_BEGIN_NODE token!"sv));
            size_t const consume_len = round_up_to_power_of_two(current_node_name.length() + 1, 4);
            TRY(stream.discard(consume_len));
            if (callbacks.on_node_begin) {
                if (IterationDecision::Break == TRY(callbacks.on_node_begin(current_node_name)))
                    return {};
            }
            break;
        }
        case EndNode:
            if (callbacks.on_node_end) {
                if (IterationDecision::Break == TRY(callbacks.on_node_end(current_node_name)))
                    return {};
            }
            break;
        case Property: {
            if (prev_token == EndNode) {
                return Error::from_string_view_or_print_error_and_return_errno("Invalid node sequence, FDT_PROP after FDT_END_NODE"sv, EINVAL);
            }
            auto len = TRY(stream.read_value<BigEndian<u32>>());
            auto nameoff = TRY(stream.read_value<BigEndian<u32>>());
            if (nameoff >= header.size_dt_strings) {
                return Error::from_string_view_or_print_error_and_return_errno("Invalid name offset in FDT_PROP"sv, EINVAL);
            }
            size_t const prop_name_max_len = header.size_dt_strings - nameoff;
            size_t const prop_name_len = strnlen(begin_strings_block + nameoff, prop_name_max_len);
            if (prop_name_len == prop_name_max_len) {
                return Error::from_string_view_or_print_error_and_return_errno("Non-null terminated name for FDT_PROP token!"sv, EINVAL);
            }
            StringView prop_name(begin_strings_block + nameoff, prop_name_len);
            if (len >= stream.remaining()) {
                return Error::from_string_view_or_print_error_and_return_errno("Property value length too large"sv, EINVAL);
            }
            ReadonlyBytes prop_value;
            if (len != 0) {
                prop_value = { struct_bytes.slice(stream.offset()).data(), len };
                size_t const consume_len = round_up_to_power_of_two(static_cast<u32>(len), 4);
                TRY(stream.discard(consume_len));
            }
            if (callbacks.on_property) {
                if (IterationDecision::Break == TRY(callbacks.on_property(prop_name, prop_value)))
                    return {};
            }
            break;
        }
        case NoOp:
            if (callbacks.on_noop) {
                if (IterationDecision::Break == TRY(callbacks.on_noop()))
                    return {};
            }
            break;
        case End: {
            if (prev_token == BeginNode || prev_token == Property) {
                return Error::from_string_view_or_print_error_and_return_errno("Invalid node sequence, FDT_END after BEGIN_NODE or PROP"sv, EINVAL);
            }
            if (!stream.is_eof()) {
                return Error::from_string_view_or_print_error_and_return_errno("Expected EOF at FTD_END but more data remains"sv, EINVAL);
            }

            if (callbacks.on_end) {
                return callbacks.on_end();
            }
            return {};
        }
        default:
            return Error::from_string_view_or_print_error_and_return_errno("Invalid token"sv, EINVAL);
        }
        prev_token = static_cast<FlattenedDeviceTreeTokenType>(static_cast<u32>(current_token));
    }
    return Error::from_string_view_or_print_error_and_return_errno("Unexpected end of stream"sv, EINVAL);
}

} // namespace DeviceTree
