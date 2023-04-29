/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DeviceEventLoop.h"
#include <AK/Debug.h>
#include <LibCore/DirIterator.h>
#include <LibCore/System.h>
#include <LibIPC/MultiServer.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

namespace DeviceMapper {

DeviceEventLoop::DeviceEventLoop(NonnullOwnPtr<Core::File> devctl_file)
    : m_devctl_file(move(devctl_file))
{
}

using MinorNumberAllocationType = DeviceEventLoop::MinorNumberAllocationType;

static constexpr DeviceEventLoop::DeviceNodeMatch s_matchers[] = {
    { "audio"sv, "audio"sv, "audio/%digit"sv, DeviceNodeFamily::Type::CharacterDevice, 116, MinorNumberAllocationType::SequentialUnlimited, 0, 0, 0220 },
    { {}, "render"sv, "gpu/render%digit"sv, DeviceNodeFamily::Type::CharacterDevice, 28, MinorNumberAllocationType::SequentialUnlimited, 0, 0, 0666 },
    { "window"sv, "gpu-connector"sv, "gpu/connector%digit"sv, DeviceNodeFamily::Type::CharacterDevice, 226, MinorNumberAllocationType::SequentialUnlimited, 0, 0, 0660 },
    { {}, "virtio-console"sv, "hvc0p%digit"sv, DeviceNodeFamily::Type::CharacterDevice, 229, MinorNumberAllocationType::SequentialUnlimited, 0, 0, 0666 },
    { "phys"sv, "hid-mouse"sv, "input/mouse/%digit"sv, DeviceNodeFamily::Type::CharacterDevice, 10, MinorNumberAllocationType::SequentialUnlimited, 0, 0, 0666 },
    { "phys"sv, "hid-keyboard"sv, "input/keyboard/%digit"sv, DeviceNodeFamily::Type::CharacterDevice, 85, MinorNumberAllocationType::SequentialUnlimited, 0, 0, 0666 },
    { {}, "storage"sv, "hd%letter"sv, DeviceNodeFamily::Type::BlockDevice, 3, MinorNumberAllocationType::SequentialUnlimited, 0, 0, 0600 },
    { "tty"sv, "console"sv, "tty%digit"sv, DeviceNodeFamily::Type::CharacterDevice, 4, MinorNumberAllocationType::SequentialLimited, 0, 63, 0620 },
    { "tty"sv, "console"sv, "ttyS%digit"sv, DeviceNodeFamily::Type::CharacterDevice, 4, MinorNumberAllocationType::SequentialLimited, 64, 127, 0620 },
};

static bool is_in_minor_number_range(DeviceEventLoop::DeviceNodeMatch const& matcher, MinorNumber minor_number)
{
    if (matcher.minor_number_allocation_type == MinorNumberAllocationType::SequentialUnlimited)
        return true;

    return matcher.minor_number_start <= minor_number && static_cast<MinorNumber>(matcher.minor_number_start.value() + matcher.minor_number_range_size) >= minor_number;
}

static Optional<DeviceEventLoop::DeviceNodeMatch const&> device_node_family_to_match_type(DeviceNodeFamily::Type unix_device_type, MajorNumber major_number, MinorNumber minor_number)
{
    for (auto& matcher : s_matchers) {
        if (matcher.major_number == major_number
            && unix_device_type == matcher.unix_device_type
            && is_in_minor_number_range(matcher, minor_number))
            return matcher;
    }
    return {};
}

static bool is_in_family_minor_number_range(DeviceNodeFamily const& family, MinorNumber minor_number)
{
    return family.base_minor_number() <= minor_number && static_cast<MinorNumber>(family.base_minor_number().value() + family.devices_symbol_suffix_allocation_map().size()) >= minor_number;
}

Optional<DeviceNodeFamily&> DeviceEventLoop::find_device_node_family(DeviceNodeFamily::Type unix_device_type, MajorNumber major_number, MinorNumber minor_number) const
{
    for (auto const& family : m_device_node_families) {
        if (family->major_number() == major_number && family->type() == unix_device_type && is_in_family_minor_number_range(*family, minor_number))
            return *family.ptr();
    }
    return {};
}

ErrorOr<NonnullRefPtr<DeviceNodeFamily>> DeviceEventLoop::find_or_register_new_device_node_family(DeviceNodeMatch const& match, DeviceNodeFamily::Type unix_device_type, MajorNumber major_number, MinorNumber minor_number)
{
    if (auto possible_family = find_device_node_family(unix_device_type, major_number, minor_number); possible_family.has_value())
        return possible_family.release_value();
    unsigned allocation_map_size = 1024;
    if (match.minor_number_allocation_type == MinorNumberAllocationType::SequentialLimited)
        allocation_map_size = match.minor_number_range_size;
    auto bitmap = TRY(Bitmap::create(allocation_map_size, false));
    auto node = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) DeviceNodeFamily(move(bitmap),
        match.family_type_literal,
        unix_device_type,
        major_number,
        minor_number)));
    TRY(m_device_node_families.try_append(node));

    return node;
}

static ErrorOr<String> build_suffix_with_letters(size_t allocation_index)
{
    auto base_string = TRY(String::from_utf8(""sv));
    while (true) {
        base_string = TRY(String::formatted("{:c}{}", 'a' + (allocation_index % 26), base_string));
        allocation_index = (allocation_index / 26);
        if (allocation_index == 0)
            break;
        allocation_index = allocation_index - 1;
    }
    return base_string;
}

static ErrorOr<String> build_suffix_with_numbers(size_t allocation_index)
{
    return String::number(allocation_index);
}

static ErrorOr<void> prepare_permissions_after_populating_devtmpfs(StringView path, DeviceEventLoop::DeviceNodeMatch const& match)
{
    if (match.permission_group.is_null())
        return {};
    auto group = TRY(Core::System::getgrnam(match.permission_group));
    VERIFY(group.has_value());
    TRY(Core::System::endgrent());
    TRY(Core::System::chown(path, 0, group.value().gr_gid));
    return {};
}

ErrorOr<void> DeviceEventLoop::register_new_device(DeviceNodeFamily::Type unix_device_type, MajorNumber major_number, MinorNumber minor_number)
{
    auto possible_match = device_node_family_to_match_type(unix_device_type, major_number, minor_number);
    if (!possible_match.has_value())
        return {};
    auto const& match = possible_match.release_value();
    auto device_node_family = TRY(find_or_register_new_device_node_family(match, unix_device_type, major_number, minor_number));
    static constexpr StringView devtmpfs_base_path = "/dev/"sv;
    auto path_pattern = TRY(String::from_utf8(match.path_pattern));
    auto& allocation_map = device_node_family->devices_symbol_suffix_allocation_map();
    auto possible_allocated_suffix_index = allocation_map.find_first_unset();
    if (!possible_allocated_suffix_index.has_value()) {
        // FIXME: Make the allocation map bigger?
        return Error::from_errno(ERANGE);
    }
    auto allocated_suffix_index = possible_allocated_suffix_index.release_value();

    auto path = TRY(String::from_utf8(path_pattern));
    if (match.path_pattern.contains("%digit"sv)) {
        auto replacement = TRY(build_suffix_with_numbers(allocated_suffix_index));
        path = TRY(path.replace("%digit"sv, replacement, ReplaceMode::All));
    }
    if (match.path_pattern.contains("%letter"sv)) {
        auto replacement = TRY(build_suffix_with_letters(allocated_suffix_index));
        path = TRY(path.replace("%letter"sv, replacement, ReplaceMode::All));
    }
    VERIFY(!path.is_empty());
    path = TRY(String::formatted("{}{}", devtmpfs_base_path, path));
    mode_t old_mask = umask(0);
    if (unix_device_type == DeviceNodeFamily::Type::BlockDevice)
        TRY(Core::System::create_block_device(path.bytes_as_string_view(), match.create_mode, major_number.value(), minor_number.value()));
    else
        TRY(Core::System::create_char_device(path.bytes_as_string_view(), match.create_mode, major_number.value(), minor_number.value()));
    umask(old_mask);
    TRY(prepare_permissions_after_populating_devtmpfs(path.bytes_as_string_view(), match));

    auto result = TRY(device_node_family->registered_nodes().try_set(RegisteredDeviceNode { move(path), minor_number }, AK::HashSetExistingEntryBehavior::Keep));
    VERIFY(result != HashSetResult::ReplacedExistingEntry);
    if (result == HashSetResult::KeptExistingEntry) {
        // FIXME: Handle this case properly.
        return Error::from_errno(EEXIST);
    }
    allocation_map.set(allocated_suffix_index, true);
    return {};
}

ErrorOr<void> DeviceEventLoop::unregister_device(DeviceNodeFamily::Type unix_device_type, MajorNumber major_number, MinorNumber minor_number)
{
    if (!device_node_family_to_match_type(unix_device_type, major_number, minor_number).has_value())
        return {};
    auto possible_family = find_device_node_family(unix_device_type, major_number, minor_number);
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
    auto removed_anything = family.registered_nodes().remove_all_matching([minor_number](auto& device) { return device.minor_number() == minor_number; });
    if (!removed_anything) {
        // FIXME: Handle cases where we can't remove a device node.
        // This could happen when the DeviceMapper program was restarted
        // so the previous state was not preserved and a device was removed.
        return Error::from_errno(ENODEV);
    }
    return {};
}

static ErrorOr<void> create_kcov_device_node()
{
    mode_t old_mask = umask(0);
    ScopeGuard umask_guard([old_mask] { umask(old_mask); });
    TRY(Core::System::create_char_device("/dev/kcov"sv, 0666, 30, 0));
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
            // NOTE: We have a special function for the KCOV device, because we don't
            // want to create a new MinorNumberAllocationType (e.g. SingleInstance).
            // Instead, just blindly create that device node and assume we will never
            // have to worry about it, so we don't need to register that!
            if (event.major_number == 30 && event.minor_number == 0 && !event.is_block_device) {
                TRY(create_kcov_device_node());
                continue;
            }
            VERIFY(event.is_block_device == 1 || event.is_block_device == 0);
            TRY(register_new_device(event.is_block_device ? DeviceNodeFamily::Type::BlockDevice : DeviceNodeFamily::Type::CharacterDevice, event.major_number, event.minor_number));
        } else if (event.state == DeviceEvent::State::Removed) {
            if (event.major_number == 30 && event.minor_number == 0 && !event.is_block_device) {
                dbgln("DeviceMapper: unregistering device failed: kcov tried to de-register itself!?");
                continue;
            }
            if (auto error_or_void = unregister_device(event.is_block_device ? DeviceNodeFamily::Type::BlockDevice : DeviceNodeFamily::Type::CharacterDevice, event.major_number, event.minor_number); error_or_void.is_error())
                dbgln("DeviceMapper: unregistering device failed: {}", error_or_void.error());
        } else {
            dbgln("DeviceMapper: Unhandled device event ({:x})!", event.state);
        }
    }
    VERIFY_NOT_REACHED();
}

}
