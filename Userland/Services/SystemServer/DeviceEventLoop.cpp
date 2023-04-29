/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DeviceEventLoop.h"
#include "Utils.h"
#include <AK/Debug.h>
#include <Kernel/API/DeviceEvent.h>
#include <LibCore/DirIterator.h>
#include <LibCore/System.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>

namespace SystemServer {

DeviceEventLoop::DeviceEventLoop(int devctl_fd)
{
    VERIFY(devctl_fd >= 0);
    m_devctl_fd = devctl_fd;
}

using MinorNumberAllocationType = DeviceEventLoop::MinorNumberAllocationType;

static constexpr DeviceEventLoop::device_node_match s_matchers[] = {
    { "audio"sv, "audio"sv, "audio/%d"sv, false, 116, MinorNumberAllocationType::SequentialUnlimited, 0, 0, 0220 },
    { {}, "render"sv, "gpu/render%d"sv, false, 28, MinorNumberAllocationType::SequentialUnlimited, 0, 0, 0666 },
    { "window"sv, "gpu-connector"sv, "gpu/connector%d"sv, false, 226, MinorNumberAllocationType::SequentialUnlimited, 0, 0, 0660 },
    { {}, "virtio-console"sv, "hvc0p%d"sv, false, 229, MinorNumberAllocationType::SequentialUnlimited, 0, 0, 0666 },
    { "phys"sv, "hid-mouse"sv, "input/mouse/%d"sv, false, 10, MinorNumberAllocationType::SequentialUnlimited, 0, 0, 0666 },
    { "phys"sv, "hid-keyboard"sv, "input/keyboard/%d"sv, false, 85, MinorNumberAllocationType::SequentialUnlimited, 0, 0, 0666 },
    { {}, "storage"sv, "hd%c"sv, true, 3, MinorNumberAllocationType::SequentialUnlimited, 0, 0, 0600 },
    { "tty"sv, "console"sv, "tty%d"sv, false, 4, MinorNumberAllocationType::SequentialLimited, 0, 63, 0620 },
    { "tty"sv, "console"sv, "ttyS%d"sv, false, 4, MinorNumberAllocationType::SequentialLimited, 64, 127, 0620 },
};

static bool is_in_minor_number_range(DeviceEventLoop::device_node_match const& matcher, MinorNumber minor_number)
{
    if (matcher.minor_number_allocation_type == MinorNumberAllocationType::SequentialUnlimited)
        return true;

    return matcher.minor_number_start <= minor_number && static_cast<MinorNumber>(matcher.minor_number_start.value() + matcher.minor_number_range_size) >= minor_number;
}

static DeviceEventLoop::device_node_match const* device_node_family_to_match_type(bool block_device, MajorNumber major_number, MinorNumber minor_number)
{
    for (auto& matcher : s_matchers) {
        if (matcher.major_number == major_number
            && block_device == matcher.block_device
            && is_in_minor_number_range(matcher, minor_number))
            return &matcher;
    }
    return nullptr;
}

static bool is_in_family_minor_number_range(DeviceNodeFamily const& family, MinorNumber minor_number)
{
    return family.base_minor_number() <= minor_number && static_cast<MinorNumber>(family.base_minor_number().value() + family.devices_symbol_suffix_allocation_map().size()) >= minor_number;
}

DeviceNodeFamily* DeviceEventLoop::find_device_node_family(bool block_device, MajorNumber major_number, MinorNumber minor_number) const
{
    DeviceNodeFamily::Type type = block_device ? DeviceNodeFamily::Type::CharacterDevice : DeviceNodeFamily::Type::CharacterDevice;
    for (auto& family : m_device_node_families) {
        if (family->major_number() == major_number && type == family->type() && is_in_family_minor_number_range(*family, minor_number))
            return family.ptr();
    }
    return nullptr;
}

ErrorOr<DeviceNodeFamily*> DeviceEventLoop::find_or_register_new_device_node_family(device_node_match const& match, bool block_device, MajorNumber major_number, MinorNumber minor_number)
{
    if (auto* family = find_device_node_family(block_device, major_number, minor_number); !!family)
        return family;
    unsigned allocation_map_size = 1024;
    if (match.minor_number_allocation_type == MinorNumberAllocationType::SequentialLimited)
        allocation_map_size = match.minor_number_range_size;
    auto bitmap = TRY(Bitmap::create(allocation_map_size, false));
    auto node = TRY(adopt_nonnull_own_or_enomem(new (nothrow) DeviceNodeFamily(move(bitmap),
        match.family_type_literal,
        block_device ? DeviceNodeFamily::Type::BlockDevice : DeviceNodeFamily::Type::CharacterDevice,
        major_number,
        minor_number)));
    TRY(m_device_node_families.try_append(move(node)));

    return m_device_node_families.last().ptr();
}

inline char offset_character_with_number(char base_char, size_t offset)
{
    char offsetted_char = base_char;
    VERIFY(static_cast<size_t>(offsetted_char) + static_cast<size_t>(offset) <= static_cast<size_t>('z'));
    offsetted_char += offset;
    return offsetted_char;
}

static ErrorOr<String> build_suffix_with_letters(size_t allocation_index)
{
    auto base_string = TRY(String::from_utf8(""sv));
    do {
        auto current_char = offset_character_with_number('a', allocation_index % 26);
        base_string = TRY(String::formatted("{:c}{}", current_char, base_string));
        allocation_index = allocation_index / 26;
    } while (allocation_index > 0);
    return base_string;
}

static ErrorOr<String> build_suffix_with_numbers(size_t allocation_index)
{
    return String::number(allocation_index);
}

static ErrorOr<void> prepare_permissions_after_populating_devtmpfs(StringView path, DeviceEventLoop::device_node_match const& match)
{
    if (match.permission_group.is_null())
        return {};
    auto group = TRY(Core::System::getgrnam(match.permission_group));
    VERIFY(group.has_value());
    TRY(Core::System::chown(path, 0, group.value().gr_gid));
    TRY(Core::System::endgrent());
    return {};
}

ErrorOr<void> DeviceEventLoop::register_new_device(bool block_device, MajorNumber major_number, MinorNumber minor_number)
{
    auto* possible_match = device_node_family_to_match_type(block_device, major_number, minor_number);
    if (!possible_match)
        return {};
    auto& match = *possible_match;
    auto& device_node_family = *TRY(find_or_register_new_device_node_family(match, block_device, major_number, minor_number));

    static constexpr StringView devtmpfs_base_path = "/dev/"sv;
    auto path_pattern = TRY(String::from_utf8(match.path_pattern));
    auto& allocation_map = device_node_family.devices_symbol_suffix_allocation_map();
    auto possible_allocated_suffix_index = allocation_map.find_first_unset();
    if (!possible_allocated_suffix_index.has_value()) {
        // FIXME: Make the allocation map bigger?
        return Error::from_errno(ERANGE);
    }
    auto allocated_suffix_index = possible_allocated_suffix_index.release_value();

    auto path = TRY(String::from_utf8(path_pattern));
    if (match.path_pattern.contains("%d"sv)) {
        auto replacement = TRY(build_suffix_with_numbers(allocated_suffix_index));
        path = TRY(path.replace("%d"sv, replacement, ReplaceMode::All));
    }
    if (match.path_pattern.contains("%c"sv)) {
        auto replacement = TRY(build_suffix_with_letters(allocated_suffix_index));
        path = TRY(path.replace("%c"sv, replacement, ReplaceMode::All));
    }
    VERIFY(!path.is_empty());
    path = TRY(String::formatted("{}{}", devtmpfs_base_path, path));
    mode_t old_mask = umask(0);
    if (block_device)
        TRY(create_devtmpfs_block_device(path.bytes_as_string_view(), match.create_mode, major_number, minor_number));
    else
        TRY(create_devtmpfs_char_device(path.bytes_as_string_view(), match.create_mode, major_number, minor_number));
    umask(old_mask);
    TRY(prepare_permissions_after_populating_devtmpfs(path.bytes_as_string_view(), match));

    auto result = TRY(device_node_family.registered_nodes().try_set(RegisteredDeviceNode { move(path), minor_number }, AK::HashSetExistingEntryBehavior::Keep));
    VERIFY(result != HashSetResult::ReplacedExistingEntry);
    if (result == HashSetResult::KeptExistingEntry) {
        // FIXME: Is this an actual bug?
        return Error::from_errno(EEXIST);
    }
    allocation_map.set(allocated_suffix_index, true);
    return {};
}

ErrorOr<void> DeviceEventLoop::unregister_device(bool block_device, MajorNumber major_number, MinorNumber minor_number)
{
    if (!device_node_family_to_match_type(block_device, major_number, minor_number))
        return {};
    auto* family = find_device_node_family(block_device, major_number, minor_number);
    if (!family) {
        // FIXME: Is this an actual bug?
        return Error::from_errno(ENODEV);
    }
    for (auto& node : family->registered_nodes()) {
        if (node.minor_number() == minor_number)
            TRY(Core::System::unlink(node.device_path()));
    }
    auto removed_anything = family->registered_nodes().remove_all_matching([minor_number](auto& device) { return device.minor_number() == minor_number; });
    if (!removed_anything) {
        // FIXME: Is this an actual bug?
        return Error::from_errno(ENODEV);
    }
    return {};
}

static ErrorOr<void> create_kcov_device_node()
{
    mode_t old_mask = umask(0);
    TRY(create_devtmpfs_char_device("/dev/kcov"sv, 0666, 30, 0));
    umask(old_mask);
    return {};
}

void DeviceEventLoop::drain_events_from_devctl()
{
    for (;;) {
        ::DeviceEvent event;
        ssize_t nread = read(m_devctl_fd, (u8*)&event, sizeof(::DeviceEvent));
        if (nread == 0) {
            sleep(1);
            continue;
        }
        VERIFY(nread == sizeof(::DeviceEvent));
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
                MUST(create_kcov_device_node());
                continue;
            }

            VERIFY(event.is_block_device == 1 || event.is_block_device == 0);
            MUST(register_new_device(event.is_block_device, event.major_number, event.minor_number));
            continue;
        } else if (event.state == DeviceEvent::State::Removed) {
            MUST(unregister_device(event.is_block_device, event.major_number, event.minor_number));
            continue;
        }
        dbgln("SystemServer: Unhandled device event!");
    }
}

}
