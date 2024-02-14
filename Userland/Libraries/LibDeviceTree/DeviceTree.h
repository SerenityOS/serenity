/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Endian.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/IterationDecision.h>
#include <AK/MemoryStream.h>
#include <AK/Optional.h>
#include <AK/Span.h>

namespace DeviceTree {

struct DeviceTreeProperty {
    ReadonlyBytes raw_data;

    size_t size() const { return raw_data.size(); }

    StringView as_string() const { return StringView(raw_data.data(), raw_data.size() - 1); }
    Vector<StringView> as_strings() const { return as_string().split_view('\0'); }
    template<typename T>
    auto for_each_string(T callback) const { return as_string().for_each_split_view('\0', SplitBehavior::Nothing, callback); }

    // Note: as<T> does not convert endianness, so all structures passed in
    //       should use BigEndian<T>s for their members and keep ordering in mind
    // Note: The Integral variant does convert endianness, so no need to pass in BigEndian<T>s
    template<typename T>
    T as() const
    {
        VERIFY(raw_data.size() == sizeof(T));
        T value;
        __builtin_memcpy(&value, raw_data.data(), sizeof(T));
        return value;
    }

    template<typename T>
    requires(alignof(T) <= 4 && !IsIntegral<T>)
    T const& as() const
    {
        return *reinterpret_cast<T const*>(raw_data.data());
    }

    template<Integral I>
    I as() const
    {
        VERIFY(raw_data.size() == sizeof(I));
        BigEndian<I> value;
        __builtin_memcpy(&value, raw_data.data(), sizeof(I));
        return value;
    }

    template<typename T>
    ErrorOr<void> for_each_in_array_of(CallableAs<ErrorOr<IterationDecision>, T const&> auto callback) const
    {
        VERIFY(raw_data.size() % sizeof(T) == 0);
        size_t count = raw_data.size() / sizeof(T);
        size_t offset = 0;
        for (size_t i = 0; i < count; ++i, offset += sizeof(T)) {
            auto sub_property = DeviceTreeProperty { raw_data.slice(offset, sizeof(T)) };
            auto result = callback(sub_property.as<T>());
            if (result.is_error())
                return result;
            if (result.value() == IterationDecision::Break)
                break;
        }
        return {};
    }

    FixedMemoryStream as_stream() const { return FixedMemoryStream { raw_data }; }
};

class DeviceTreeNodeView {
public:
    bool has_property(StringView prop) const { return m_properties.contains(prop); }
    bool has_child(StringView child) const { return m_children.contains(child); }
    bool child(StringView name) const { return has_property(name) || has_child(name); }

    Optional<DeviceTreeProperty> get_property(StringView prop) const { return m_properties.get(prop); }

    // FIXME: The spec says that @address parts of the name should be ignored when looking up nodes
    //        when they do not appear in the queried name, and all nodes with the same name should be returned
    Optional<DeviceTreeNodeView const&> get_child(StringView child) const { return m_children.get(child); }

    HashMap<StringView, DeviceTreeNodeView> const& children() const { return m_children; }
    HashMap<StringView, DeviceTreeProperty> const& properties() const { return m_properties; }

    DeviceTreeNodeView const* parent() const { return m_parent; }

    // FIXME: Add convenience functions for common properties like "reg" and "compatible"
    // Note: The "reg" property is a list of address and size pairs, but the address is not always a u32 or u64
    //       In pci devices the #address-size is 3 cells: (phys.lo phys.mid phys.hi)
    //       with the following format:
    //       phys.lo, phys.mid: 64-bit Address - BigEndian
    //       phys.hi: relocatable(1), prefetchable(1), aliased(1), 000(3), space type(2), bus number(8), device number(5), function number(3), register number(8) - BigEndian

    // FIXME: Stringify?
    // FIXME: Flatten?
    // Note: That we dont have a oder of children and properties in this view
protected:
    friend class DeviceTree;
    DeviceTreeNodeView(DeviceTreeNodeView* parent)
        : m_parent(parent)
    {
    }
    HashMap<StringView, DeviceTreeNodeView>& children() { return m_children; }
    HashMap<StringView, DeviceTreeProperty>& properties() { return m_properties; }
    DeviceTreeNodeView* parent() { return m_parent; }

private:
    DeviceTreeNodeView* m_parent;
    HashMap<StringView, DeviceTreeNodeView> m_children;
    HashMap<StringView, DeviceTreeProperty> m_properties;
};

class DeviceTree : public DeviceTreeNodeView {
public:
    static ErrorOr<NonnullOwnPtr<DeviceTree>> parse(ReadonlyBytes);

    DeviceTreeNodeView const* resolve_node(StringView path) const
    {
        // FIXME: May children of aliases be referenced?
        // Note: Aliases may not contain a '/' in their name
        //       And as all paths other than aliases should start with '/', we can just check for the first '/'
        if (!path.starts_with('/')) {
            if (auto alias_list = get_child("aliases"sv); alias_list.has_value()) {
                if (auto alias = alias_list->get_property(path); alias.has_value()) {
                    path = alias.value().as_string();
                } else {
                    dbgln("DeviceTree: '{}' not found in /aliases, treating as absolute path", path);
                }
            } else {
                dbgln("DeviceTree: No /aliases node found, treating '{}' as absolute path", path);
            }
        }

        DeviceTreeNodeView const* node = this;
        path.for_each_split_view('/', SplitBehavior::Nothing, [&](auto const& part) {
            if (auto child = node->get_child(part); child.has_value()) {
                node = &child.value();
            } else {
                node = nullptr;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });

        return node;
    }

    Optional<DeviceTreeProperty> resolve_property(StringView path) const
    {
        auto property_name = path.find_last_split_view('/');
        auto node_path = path.substring_view(0, path.length() - property_name.length() - 1);
        auto const* node = resolve_node(node_path);
        if (!node)
            return {};
        return node->get_property(property_name);
    }

    // FIXME: Add a helper to iterate over each descendant fulfilling some properties
    //        Like each node with a "compatible" property containing "pci" or "usb",
    //        bonus points if it could automatically recurse in the tree under some conditions,
    //        like "simple-bus" or "pci-bridge" nodes

    DeviceTreeNodeView const* phandle(u32 phandle) const
    {
        if (phandle >= m_phandles.size())
            return nullptr;
        return m_phandles[phandle];
    }

    ReadonlyBytes flattened_device_tree() const { return m_flattened_device_tree; }

private:
    DeviceTree(ReadonlyBytes flattened_device_tree)
        : DeviceTreeNodeView(nullptr)
        , m_flattened_device_tree(flattened_device_tree)
    {
    }

    ErrorOr<void> set_phandle(u32 phandle, DeviceTreeNodeView* node)
    {
        if (m_phandles.size() > phandle && m_phandles[phandle] != nullptr)
            return Error::from_string_view_or_print_error_and_return_errno("Duplicate phandle entry in DeviceTree"sv, EINVAL);
        if (m_phandles.size() <= phandle)
            TRY(m_phandles.try_resize(phandle + 1));
        m_phandles[phandle] = node;
        return {};
    }

    ReadonlyBytes m_flattened_device_tree;
    Vector<DeviceTreeNodeView*> m_phandles;
};

}
