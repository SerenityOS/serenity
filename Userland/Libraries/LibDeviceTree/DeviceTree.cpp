/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DeviceTree.h"
#include "FlattenedDeviceTree.h"
#include <AK/NonnullOwnPtr.h>
#include <AK/Types.h>

namespace DeviceTree {

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
            .on_property = [&current_node](StringView name, ReadonlyBytes value) -> ErrorOr<IterationDecision> {
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
    // Note: For the same reason as above, we need to postpone setting the phandles until the tree is fully built
    TRY(device_tree->for_each_node([&device_tree]([[maybe_unused]] StringView name, DeviceTreeNodeView& node) -> ErrorOr<RecursionDecision> {
        if (auto phandle = node.get_property("phandle"sv); phandle.has_value()) {
            auto phandle_value = phandle.value().as<u32>();
            TRY(device_tree->set_phandle(phandle_value, &node));
        }
        return RecursionDecision::Recurse;
    }));

    return device_tree;
}

}
