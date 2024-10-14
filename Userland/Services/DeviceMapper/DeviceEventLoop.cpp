/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DeviceEventLoop.h"
#include <AK/Debug.h>
#include <AK/LexicalPath.h>
#include <LibCore/DirIterator.h>
#include <LibCore/System.h>
#include <LibIPC/MultiServer.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

namespace DeviceMapper {

DeviceEventLoop::DeviceEventLoop(Vector<DeviceNodeMatch> matches, NonnullOwnPtr<Core::File> devctl_file)
    : m_devctl_file(move(devctl_file))
    , m_matches(move(matches))
{
}

static constexpr StringView digit_pattern = "%d"sv;
static constexpr StringView letter_char_pattern = "%c"sv;

Optional<DeviceEventLoop::DeviceNodeMatch const&> DeviceEventLoop::device_node_family_to_match_type(DeviceNodeType device_node_type, MajorNumber major_number, MinorNumber minor_number)
{
    Optional<DeviceEventLoop::DeviceNodeMatch const&> match {};
    for (auto& matcher : m_matches) {
        auto basic_match = matcher.major_number == major_number && device_node_type == matcher.device_node_type;
        if (basic_match) {
            if (matcher.specific_minor_number.has_value()) {
                if (minor_number == matcher.specific_minor_number.value())
                    return matcher;
                continue;
            }
            match = matcher;
        }
    }
    return match;
}

Optional<DeviceNodeFamily&> DeviceEventLoop::find_device_node_family(DeviceNodeType device_node_type, MajorNumber major_number) const
{
    for (auto const& family : m_device_node_families) {
        if (family->major_number() == major_number && family->device_node_type() == device_node_type)
            return *family.ptr();
    }
    return {};
}

ErrorOr<NonnullRefPtr<DeviceNodeFamily>> DeviceEventLoop::find_or_register_new_device_node_family(DeviceNodeMatch const& match, DeviceNodeType device_node_type, MajorNumber major_number)
{
    VERIFY(device_node_type == DeviceNodeType::Block
        || device_node_type == DeviceNodeType::Character);

    if (auto possible_family = find_device_node_family(device_node_type, major_number); possible_family.has_value())
        return possible_family.release_value();

    // FIXME: Is 1024 enough nodes for allocated device nodes? or should
    // we exapnd it?
    unsigned allocation_map_size = 1024;
    auto bitmap = TRY(Bitmap::create(allocation_map_size, false));
    auto node = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DeviceNodeFamily(move(bitmap),
        match.family_type_literal,
        device_node_type,
        major_number)));
    TRY(m_device_node_families.try_append(node));

    return node;
}

static ErrorOr<String> build_suffix_with_letters(size_t allocation_index)
{
    String base_string {};
    while (true) {
        base_string = TRY(String::formatted("{:c}{}", 'a' + (allocation_index % 26), base_string));
        allocation_index = (allocation_index / 26);
        if (allocation_index == 0)
            break;
        allocation_index = allocation_index - 1;
    }
    return base_string;
}

static String build_suffix_with_numbers(size_t allocation_index)
{
    return String::number(allocation_index);
}

static ErrorOr<void> prepare_permissions_after_populating_devtmpfs(StringView path, DeviceEventLoop::DeviceNodeMatch const& match)
{
    auto group = TRY(Core::System::getgrnam(match.permission_group));
    VERIFY(group.has_value());
    TRY(Core::System::endgrent());
    TRY(Core::System::chown(path, 0, group.value().gr_gid));
    return {};
}

ErrorOr<void> DeviceEventLoop::register_new_device(DeviceNodeType device_node_type, MajorNumber major_number, MinorNumber minor_number)
{
    VERIFY(device_node_type == DeviceNodeType::Block
        || device_node_type == DeviceNodeType::Character);

    auto possible_match = device_node_family_to_match_type(device_node_type, major_number, minor_number);
    if (!possible_match.has_value())
        return {};
    auto const& match = possible_match.release_value();

    auto path_pattern = match.path_pattern;
    if (path_pattern.is_empty())
        return Error::from_string_literal("Device node family path pattern is empty");

    static constexpr StringView devtmpfs_base_path = "/dev/"sv;

    // NOTE: If the match has specific minor number, then it's for a specific
    // device node (for example, /dev/beep). In such case, just create a device node
    // and don't attempt to create (or find) a device node family, as there's no actual
    // family (i.e. that matches its path pattern or permissions) for such device.
    if (match.specific_minor_number.has_value()) {
        auto path = match.path_pattern;
        if (path.contains(digit_pattern) || path.contains(letter_char_pattern))
            return Error::from_string_literal("Path pattern for specific device contains replacement specifiers");
        path = TRY(String::formatted("{}{}", devtmpfs_base_path, path));
        mode_t old_mask = umask(0);
        if (device_node_type == DeviceNodeType::Block)
            TRY(Core::System::create_block_device(path.bytes_as_string_view(), match.create_mode, major_number.value(), minor_number.value()));
        else
            TRY(Core::System::create_char_device(path.bytes_as_string_view(), match.create_mode, major_number.value(), minor_number.value()));
        umask(old_mask);
        TRY(prepare_permissions_after_populating_devtmpfs(path.bytes_as_string_view(), match));
        return {};
    }

    auto device_node_family = TRY(find_or_register_new_device_node_family(match, device_node_type, major_number));

    auto& allocation_map = device_node_family->devices_symbol_suffix_allocation_map();
    auto possible_allocated_suffix_index = allocation_map.find_first_unset();
    if (!possible_allocated_suffix_index.has_value()) {
        // FIXME: Make the allocation map bigger?
        return Error::from_errno(ERANGE);
    }
    auto allocated_suffix_index = possible_allocated_suffix_index.release_value();

    auto path = match.path_pattern;
    if (path.contains(digit_pattern)) {
        auto replacement = build_suffix_with_numbers(allocated_suffix_index);
        path = TRY(path.replace(digit_pattern, replacement, ReplaceMode::All));
    }
    if (path.contains(letter_char_pattern)) {
        auto replacement = TRY(build_suffix_with_letters(allocated_suffix_index));
        path = TRY(path.replace(letter_char_pattern, replacement, ReplaceMode::All));
    }
    VERIFY(!path.is_empty());
    path = TRY(String::formatted("{}{}", devtmpfs_base_path, path));
    mode_t old_mask = umask(0);
    if (device_node_type == DeviceNodeType::Block)
        TRY(Core::System::create_block_device(path.bytes_as_string_view(), match.create_mode, major_number.value(), minor_number.value()));
    else
        TRY(Core::System::create_char_device(path.bytes_as_string_view(), match.create_mode, major_number.value(), minor_number.value()));
    umask(old_mask);
    TRY(prepare_permissions_after_populating_devtmpfs(path.bytes_as_string_view(), match));

    auto symlink_path = LexicalPath("/tmp/system/devicemap/nodes/")
                            .append(device_node_type == DeviceNodeType::Block ? "block"sv : "char"sv)
                            .append(String::number(major_number.value()))
                            .append(String::number(minor_number.value()));

    TRY(Core::System::symlink(path.bytes_as_string_view(), symlink_path.string()));

    auto result = TRY(device_node_family->registered_nodes().try_set(RegisteredDeviceNode { move(path), minor_number }, AK::HashSetExistingEntryBehavior::Keep));
    VERIFY(result != HashSetResult::ReplacedExistingEntry);
    if (result == HashSetResult::KeptExistingEntry) {
        // FIXME: Handle this case properly.
        return Error::from_errno(EEXIST);
    }
    allocation_map.set(allocated_suffix_index, true);
    return {};
}

ErrorOr<void> DeviceEventLoop::unregister_device(DeviceNodeType device_node_type, MajorNumber major_number, MinorNumber minor_number)
{
    VERIFY(device_node_type == DeviceNodeType::Block
        || device_node_type == DeviceNodeType::Character);

    if (!device_node_family_to_match_type(device_node_type, major_number, minor_number).has_value())
        return {};
    auto possible_family = find_device_node_family(device_node_type, major_number);
    if (!possible_family.has_value()) {
        // FIXME: Handle cases where we can't remove a device node.
        // This could happen when the DeviceMapper program was restarted
        // so the previous state was not preserved and a device was removed.
        return Error::from_errno(ENODEV);
    }
    auto& family = possible_family.release_value();
    for (auto& node : family.registered_nodes()) {
        if (node.minor_number() == minor_number)
            TRY(Core::System::unlink(node.device_path()));
    }

    auto symlink_path = LexicalPath("/tmp/system/devicemap/nodes/")
                            .append(device_node_type == DeviceNodeType::Block ? "block"sv : "char"sv)
                            .append(String::number(major_number.value()))
                            .append(String::number(minor_number.value()));

    TRY(Core::System::unlink(symlink_path.string()));

    auto removed_anything = family.registered_nodes().remove_all_matching([minor_number](auto& device) { return device.minor_number() == minor_number; });
    if (!removed_anything) {
        // FIXME: Handle cases where we can't remove a device node.
        // This could happen when the DeviceMapper program was restarted
        // so the previous state was not preserved and a device was removed.
        return Error::from_errno(ENODEV);
    }
    return {};
}

ErrorOr<void> DeviceEventLoop::read_one_or_eof(DeviceEvent& event)
{
    if (m_devctl_file->read_until_filled({ bit_cast<u8*>(&event), sizeof(DeviceEvent) }).is_error()) {
        // Bad! Kernel and SystemServer apparently disagree on the record size,
        // which means that previous data is likely to be invalid.
        return Error::from_string_view("File ended after incomplete record? /dev/devctl seems broken!"sv);
    }
    return {};
}

ErrorOr<void> DeviceEventLoop::drain_events_from_devctl()
{
    for (;;) {
        DeviceEvent event;
        TRY(read_one_or_eof(event));
        // NOTE: Ignore any event related to /dev/devctl device node - normally
        // it should never disappear from the system and we already use it in this
        // code.
        if (event.major_number == 2 && event.minor_number == 10 && !event.is_block_device)
            continue;

        if (event.state == DeviceEvent::State::Inserted) {
            VERIFY(event.is_block_device == 1 || event.is_block_device == 0);
            TRY(register_new_device(event.is_block_device ? DeviceNodeType::Block : DeviceNodeType::Character, event.major_number, event.minor_number));
        } else if (event.state == DeviceEvent::State::Removed) {
            if (auto error_or_void = unregister_device(event.is_block_device ? DeviceNodeType::Block : DeviceNodeType::Character, event.major_number, event.minor_number); error_or_void.is_error())
                dbgln("DeviceMapper: unregistering device failed: {}", error_or_void.error());
        } else {
            dbgln("DeviceMapper: Unhandled device event ({:x})!", event.state);
        }
    }
    VERIFY_NOT_REACHED();
}

}
