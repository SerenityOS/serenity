/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DeviceTree.h"
#include "FlattenedDeviceTree.h"
#include <AK/Debug.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Types.h>

namespace DeviceTree {

ErrorOr<NonnullOwnPtr<DeviceTree>> DeviceTree::parse(ReadonlyBytes flattened_device_tree)
{
    // Device tree must be 8-byte aligned
    if ((bit_cast<FlatPtr>(flattened_device_tree.data()) & 0b111) != 0)
        return Error::from_errno(EINVAL);

    auto device_tree = TRY(adopt_nonnull_own_or_enomem(new (nothrow) DeviceTree { flattened_device_tree }));
    Node* current_node = device_tree.ptr();

    auto const& header = *reinterpret_cast<FlattenedDeviceTreeHeader const*>(flattened_device_tree.data());

    TRY(walk_device_tree(header, flattened_device_tree,
        {
            .on_node_begin = [&current_node, &device_tree](StringView name) -> ErrorOr<IterationDecision> {
                // Skip the root node, which has an empty name
                if (current_node == device_tree.ptr() && name.is_empty())
                    return IterationDecision::Continue;

                // FIXME: Use something like children.emplace
                TRY(current_node->children().try_set(name, Node { current_node }));
                auto& new_node = current_node->children().get(name).value();
                current_node = &new_node;
                return IterationDecision::Continue;
            },
            .on_node_end = [&current_node](StringView) -> ErrorOr<IterationDecision> {
                current_node = current_node->parent();
                return IterationDecision::Continue;
            },
            .on_property = [&current_node](StringView name, ReadonlyBytes value) -> ErrorOr<IterationDecision> {
                TRY(current_node->properties().try_set(name, Property { value }));
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
    auto fix_parent = [](auto self, Node& node) -> void {
        for (auto& [name, child] : node.children()) {
            child.m_parent = &node;
            self(self, child);
        }
    };

    fix_parent(fix_parent, *device_tree);
    // Note: For the same reason as above, we need to postpone setting the phandles until the tree is fully built
    TRY(device_tree->for_each_node([&device_tree]([[maybe_unused]] StringView name, Node& node) -> ErrorOr<RecursionDecision> {
        if (auto phandle = node.get_property("phandle"sv); phandle.has_value()) {
            auto phandle_value = phandle.value().as<u32>();
            TRY(device_tree->set_phandle(phandle_value, &node));
        }
        return RecursionDecision::Recurse;
    }));

    return device_tree;
}

bool Node::is_compatible_with(StringView wanted_compatible_string) const
{
    auto maybe_compatible = get_property("compatible"sv);
    if (!maybe_compatible.has_value())
        return false;

    bool is_compatible = false;

    maybe_compatible->for_each_string([&is_compatible, wanted_compatible_string](StringView compatible_entry) {
        if (compatible_entry == wanted_compatible_string) {
            is_compatible = true;
            return IterationDecision::Break;
        }

        return IterationDecision::Continue;
    });

    return is_compatible;
}

// 2.4.1 Properties for Interrupt Generating Devices
ErrorOr<Node const*> Node::interrupt_parent(DeviceTree const& device_tree) const
{
    auto maybe_interrupt_parent_prop = get_property("interrupt-parent"sv);
    if (maybe_interrupt_parent_prop.has_value()) {
        auto interrupt_parent_prop = maybe_interrupt_parent_prop.release_value();

        if (interrupt_parent_prop.size() != sizeof(u32))
            return Error::from_errno(EINVAL);

        auto const* interrupt_parent = device_tree.phandle(interrupt_parent_prop.as<u32>());

        if (interrupt_parent == nullptr)
            return Error::from_errno(ENOENT);

        return interrupt_parent;
    }

    if (m_parent == nullptr)
        return Error::from_errno(ENOENT);

    return m_parent;
}

// 2.4 Interrupts and Interrupt Mapping
ErrorOr<Node const*> Node::interrupt_domain_root(DeviceTree const& device_tree) const
{
    auto const* current_node = this;

    for (;;) {
        // Interupt controllers are specified by the presence of the interrupt-controller property.
        // An interrupt nexus can be identified by the interrupt-map property.
        if (current_node->has_property("interrupt-controller"sv) || current_node->has_property("interrupt-map"sv))
            return current_node;

        current_node = TRY(current_node->interrupt_parent(device_tree));
    }
}

ErrorOr<Vector<Interrupt>> Node::interrupts(DeviceTree const& device_tree) const
{
    // 2.4.1 Properties for Interrupt Generating Devices
    // If both interrupts-extended and interrupts are present then interrupts-extended takes precedence.
    auto interrupts_extended_prop = get_property("interrupts-extended"sv);
    if (interrupts_extended_prop.has_value()) {
        Vector<Interrupt> interrupts;

        auto stream = interrupts_extended_prop->as_stream();
        while (!stream.is_eof()) {
            auto interrupt_parent_phandle = TRY(stream.read_cell());
            auto const* interrupt_parent = device_tree.phandle(interrupt_parent_phandle);
            if (interrupt_parent == nullptr)
                return Error::from_errno(ENOENT);

            auto const& domain_root = *TRY(interrupt_parent->interrupt_domain_root(device_tree));
            if (!domain_root.has_property("interrupt-controller"sv))
                return Error::from_errno(ENOTSUP); // TODO: Handle interrupt nexuses.

            auto interrupt_cells_prop = domain_root.get_property("#interrupt-cells"sv);
            if (!interrupt_cells_prop.has_value())
                return Error::from_errno(EINVAL);

            if (interrupt_cells_prop->size() != sizeof(u32))
                return Error::from_errno(EINVAL);

            auto interrupt_cells = interrupt_cells_prop->as<u32>();

            auto interrupt_identifier = TRY(stream.read_in_place<u8 const>(interrupt_cells * sizeof(u32)));

            TRY(interrupts.try_append(Interrupt {
                .domain_root = &domain_root,
                .interrupt_identifier = interrupt_identifier,
            }));
        }

        return interrupts;
    }

    auto interrupts_prop = get_property("interrupts"sv);

    if (!interrupts_prop.has_value())
        return Error::from_errno(EINVAL);

    auto const& domain_root = *TRY(interrupt_domain_root(device_tree));
    if (!domain_root.has_property("interrupt-controller"sv))
        return Error::from_errno(ENOTSUP); // TODO: Handle interrupt nexuses.

    auto interrupt_cells_prop = domain_root.get_property("#interrupt-cells"sv);
    if (!interrupt_cells_prop.has_value())
        return Error::from_errno(EINVAL);

    if (interrupt_cells_prop->size() != sizeof(u32))
        return Error::from_errno(EINVAL);

    auto interrupt_cells = interrupt_cells_prop->as<u32>();

    auto interrupts_raw = interrupts_prop->raw_data;

    auto interrupt_count = interrupts_prop->size() / (interrupt_cells * sizeof(u32));

    Vector<Interrupt> interrupts;
    TRY(interrupts.try_resize(interrupt_count));

    for (size_t i = 0; i < interrupt_count; i++) {
        interrupts[i] = Interrupt {
            .domain_root = &domain_root,
            .interrupt_identifier = interrupts_raw.slice(i * interrupt_cells * sizeof(u32), interrupt_cells * sizeof(u32)),
        };
    }

    return interrupts;
}

ErrorOr<Reg> Node::reg() const
{
    if (parent() == nullptr)
        return Error::from_errno(EINVAL);

    auto reg_prop = get_property("reg"sv);
    if (!reg_prop.has_value())
        return Error::from_errno(ENOENT);

    return Reg { reg_prop->raw_data, *this };
}

ErrorOr<Ranges> Node::ranges() const
{
    if (parent() == nullptr)
        return Error::from_errno(EINVAL);

    auto ranges_prop = get_property("ranges"sv);
    if (!ranges_prop.has_value())
        return Error::from_errno(ENOENT);

    return Ranges { ranges_prop->raw_data, *this };
}

ErrorOr<Address> RegEntry::resolve_root_address() const
{
    VERIFY(m_node.parent() != nullptr);
    return m_node.parent()->translate_child_bus_address_to_root_address(bus_address());
}

ErrorOr<RegEntry> Reg::entry(size_t index) const
{
    if (index >= entry_count())
        return Error::from_errno(EINVAL);

    VERIFY(m_node.parent() != nullptr);

    auto parent_address_cells = m_node.parent()->address_cells();
    auto parent_size_cells = m_node.parent()->size_cells();

    size_t const start_index = index * (parent_address_cells + parent_size_cells) * sizeof(u32);

    auto address = Address { m_raw.slice(start_index, parent_address_cells * sizeof(u32)) };
    auto length = Size { m_raw.slice(start_index + (parent_address_cells * sizeof(u32)), parent_size_cells * sizeof(u32)) };

    return RegEntry {
        move(address),
        move(length),
        m_node,
    };
}

size_t Reg::entry_count() const
{
    VERIFY(m_node.parent() != nullptr);

    auto parent_address_cells = m_node.parent()->address_cells();
    auto parent_size_cells = m_node.parent()->size_cells();

    // #address-cells should never be 0, but still avoid dividing by 0.
    if (parent_address_cells + parent_size_cells == 0)
        return 0;

    return m_raw.size() / ((parent_address_cells + parent_size_cells) * sizeof(u32));
}

ErrorOr<Address> RangesEntry::translate_child_bus_address_to_parent_bus_address(Address const& address) const
{
    auto maybe_device_type = m_node.get_property("device_type"sv);

    if (maybe_device_type.has_value() && maybe_device_type->as_string() == "pci"sv) {
        // TODO
        return Error::from_errno(ENOTSUP);
    }

    auto address_as_flatptr = TRY(address.as_flatptr());
    auto child_bus_address_as_flatptr = TRY(m_child_bus_address.as_flatptr());
    auto parent_bus_address_as_flatptr = TRY(m_parent_bus_address.as_flatptr());
    auto length_as_size_t = TRY(m_length.as_size_t());

    if (address_as_flatptr >= child_bus_address_as_flatptr && address_as_flatptr < (child_bus_address_as_flatptr + length_as_size_t))
        return Address::from_flatptr(address_as_flatptr - child_bus_address_as_flatptr + parent_bus_address_as_flatptr);

    return Error::from_errno(EFAULT);
}

ErrorOr<RangesEntry> Ranges::entry(size_t index) const
{
    if (index >= entry_count())
        return Error::from_errno(EINVAL);

    VERIFY(m_node.parent() != nullptr);

    auto address_cells = m_node.address_cells();
    auto parent_address_cells = m_node.parent()->address_cells();
    auto size_cells = m_node.size_cells();

    size_t const start_index = index * (address_cells + parent_address_cells + size_cells) * sizeof(u32);

    auto child_bus_address = Address { m_raw.slice(start_index, address_cells * sizeof(u32)) };
    auto parent_bus_addres = Address { m_raw.slice(start_index + (address_cells * sizeof(u32)), parent_address_cells * sizeof(u32)) };
    auto size = Size { m_raw.slice(start_index + ((address_cells + parent_address_cells) * sizeof(u32)), size_cells * sizeof(u32)) };

    return RangesEntry {
        move(child_bus_address),
        move(parent_bus_addres),
        move(size),
        m_node,
    };
}

size_t Ranges::entry_count() const
{
    VERIFY(m_node.parent() != nullptr);

    auto address_cells = m_node.address_cells();
    auto parent_address_cells = m_node.parent()->address_cells();
    auto size_cells = m_node.size_cells();

    // #address-cells should never be 0, but still avoid dividing by 0.
    if (address_cells + parent_address_cells + size_cells == 0)
        return 0;

    return m_raw.size() / ((address_cells + parent_address_cells + size_cells) * sizeof(u32));
}

ErrorOr<Address> Ranges::translate_child_bus_address_to_parent_bus_address(Address const& addr) const
{
    // 2.3.8 ranges
    // If the property is defined with an <empty> value, it specifies that the parent and child address space is identical,
    // and no address translation is required.
    if (entry_count() == 0)
        return addr;

    for (size_t i = 0; i < entry_count(); i++) {
        if (auto translation_or_error = MUST(entry(i)).translate_child_bus_address_to_parent_bus_address(addr); !translation_or_error.is_error())
            return translation_or_error.release_value();
    }

    return Error::from_errno(EFAULT);
}

ErrorOr<Address> Node::translate_child_bus_address_to_root_address(Address const& addr) const
{
    dbgln_if(DEVICETREE_DEBUG, "DeviceTree: Translating bus address {:hex-dump}", addr.raw());

    auto const* current_node = this;
    auto current_address = addr;

    while (!current_node->is_root()) {
        // 2.3.8 ranges
        // If the property is not present in a bus node, it is assumed that no mapping exists between children of the node
        // and the parent address space.
        auto ranges_or_error = current_node->ranges();
        if (ranges_or_error.is_error()) {
            VERIFY(ranges_or_error.release_error().code() == ENOENT);
            return Error::from_errno(EFAULT);
        }

        current_address = TRY(ranges_or_error.release_value().translate_child_bus_address_to_parent_bus_address(current_address));

        current_node = current_node->parent();

        dbgln_if(DEVICETREE_DEBUG, "DeviceTree: -> {} address: {:hex-dump}", current_node->is_root() ? "root" : "parent bus", current_address.raw());
    }

    return current_address;
}

}
