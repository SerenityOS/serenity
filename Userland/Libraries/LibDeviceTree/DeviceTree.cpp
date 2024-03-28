/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DeviceTree.h"
#include "FlattenedDeviceTree.h"
#include <AK/NonnullOwnPtr.h>
#include <AK/StringBuilder.h>
#include <AK/Types.h>

namespace DeviceTree {

FlatPtr DeviceTreeNodeView::to_root_address(FlatPtr value) const
{
    // FIXME: Handle address sizes other than 32 and 64
    // FIXME: `dma_ranges` looks very similar to this,
    //        so maybe we can make this use this code as well
    // ranges: [<own-addr(#address-cells)> <parent-addr(::#address-cells)> <length(#size-cells)>]

    // The root node:
    if (m_parent == nullptr)
        return value;

    auto own_ranges = get_property("ranges"sv);
    // Note: If you hit this, you should probably call this on the parent node of your current node
    //       This is because the most prominent user `reg` uses the parents address space
    VERIFY(own_ranges.has_value());

    if (own_ranges->size() == 0)
        return m_parent->to_root_address(value);

    auto own_address_size = get_property("#address-cells"sv);
    VERIFY(own_address_size.has_value());
    auto own_length_size = get_property("#size-cells"sv);
    VERIFY(own_length_size.has_value());

    auto parent_address_size = m_parent->get_property("#address-cells"sv);
    VERIFY(parent_address_size.has_value());

    if (own_ranges->size() % (own_address_size->as<u32>() + parent_address_size->as<u32>() + own_length_size->as<u32>()) != 0) {
        dbgln("DeviceTree: Ranges property has invalid length {}, expected a multiple of {}",
            own_ranges->size(), own_address_size->as<u32>() + parent_address_size->as<u32>() + own_length_size->as<u32>());
        VERIFY_NOT_REACHED();
    }

    auto range_stream = own_ranges->as_stream();
    while (!range_stream.is_eof()) {
        FlatPtr own_address = MUST(range_stream.read_cells(own_address_size->as<u32>()));
        FlatPtr parent_address = MUST(range_stream.read_cells(parent_address_size->as<u32>()));
        FlatPtr length = MUST(range_stream.read_cells(own_length_size->as<u32>()));

        if (value >= own_address && value < own_address + length)
            return m_parent->to_root_address(parent_address + (value - own_address));
    }

    // FIXME: Getting this error half way through translation is not very helpful
    //        It'd be nice to get the translation chain leading to this error
    dbgln("DeviceTree: Address {} not found in ranges property", value);

    StringBuilder builder;
    range_stream = own_ranges->as_stream();
    while (!range_stream.is_eof()) {
        FlatPtr own_address = MUST(range_stream.read_cells(own_address_size->as<u32>()));
        FlatPtr parent_address = MUST(range_stream.read_cells(parent_address_size->as<u32>()));
        FlatPtr length = MUST(range_stream.read_cells(own_length_size->as<u32>()));

        builder.appendff("(own:{:#08x} parent:{:#08x} size:{:#x}),", own_address, parent_address, length);
    }

    dbgln("DeviceTree: ranges: [{}]", builder.string_view());
    VERIFY_NOT_REACHED();
}

ErrorOr<NonnullOwnPtr<DeviceTree>> DeviceTree::parse(ReadonlyBytes flattened_device_tree)
{
    // Device tree must be 8-byte aligned
    if ((bit_cast<FlatPtr>(flattened_device_tree.data()) & 0b111) != 0)
        return Error::from_errno(EINVAL);

    auto device_tree = TRY(adopt_nonnull_own_or_enomem(new (nothrow) DeviceTree { flattened_device_tree }));
    DeviceTreeNodeView* current_node = device_tree.ptr();

    auto const& header = *reinterpret_cast<FlattenedDeviceTreeHeader const*>(flattened_device_tree.data());

    TRY(walk_device_tree(header, flattened_device_tree,
        {
            .on_node_begin = [&current_node, &device_tree](StringView name) -> ErrorOr<IterationDecision> {
                // Skip the root node, which has an empty name
                if (current_node == device_tree.ptr() && name.is_empty())
                    return IterationDecision::Continue;

                // FIXME: Use something like children.emplace
                TRY(current_node->children().try_set(name, DeviceTreeNodeView { current_node }));
                auto& new_node = current_node->children().get(name).value();
                current_node = &new_node;
                return IterationDecision::Continue;
            },
            .on_node_end = [&current_node](StringView) -> ErrorOr<IterationDecision> {
                current_node = current_node->parent();
                return IterationDecision::Continue;
            },
            .on_property = [&device_tree, &current_node](StringView name, ReadonlyBytes value) -> ErrorOr<IterationDecision> {
                DeviceTreeProperty property { value };

                if (name == "phandle"sv) {
                    auto phandle = property.as<u32>();
                    TRY(device_tree->set_phandle(phandle, current_node));
                }

                TRY(current_node->properties().try_set(name, DeviceTreeProperty { value }));
                return IterationDecision::Continue;
            },
            .on_noop = []() -> ErrorOr<IterationDecision> {
                return IterationDecision::Continue;
            },
            .on_end = [&]() -> ErrorOr<void> {
                return {};
            },
        }));

    // FIXME: While growing the a nodes children map, we might have reallocated it's storage
    //        breaking the parent pointers of the children, so we need to fix them here
    auto fix_parent = [](auto self, DeviceTreeNodeView& node) -> void {
        for (auto& [name, child] : node.children()) {
            child.m_parent = &node;
            self(self, child);
        }
    };

    fix_parent(fix_parent, *device_tree);

    return device_tree;
}

}
