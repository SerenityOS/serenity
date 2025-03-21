/*
 * Copyright (c) 2024, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Endian.h>
#include <AK/FixedArray.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/IterationDecision.h>
#include <AK/MemoryStream.h>
#include <AK/Optional.h>
#include <AK/RecursionDecision.h>
#include <AK/Span.h>

namespace DeviceTree {

// Devicetree Specification 0.4 (DTSpec): https://github.com/devicetree-org/devicetree-specification/releases/download/v0.4/devicetree-specification-v0.4.pdf

class DeviceTree;
class Node;

class Address {
public:
    Address() = default;
    Address(ReadonlyBytes data)
        : m_raw(static_cast<decltype(m_raw)>(data))
    {
    }

    ReadonlyBytes raw() const { return m_raw; }

    static Address from_flatptr(FlatPtr flatptr)
    {
        BigEndian<FlatPtr> big_endian_flatptr { flatptr };

        Address address;
        address.m_raw.resize(sizeof(FlatPtr));
        __builtin_memcpy(address.m_raw.data(), &big_endian_flatptr, sizeof(big_endian_flatptr));
        return address;
    }

    ErrorOr<FlatPtr> as_flatptr() const
    {
        if (m_raw.size() == sizeof(u32))
            return *reinterpret_cast<BigEndian<u32> const*>(m_raw.data());
        if (m_raw.size() == 2 * sizeof(u32))
            return *reinterpret_cast<BigEndian<u64> const*>(m_raw.data());
        return ERANGE;
    }

private:
    Vector<u8, 4 * sizeof(u32)> m_raw;
};

class Size {
public:
    Size() = default;
    Size(ReadonlyBytes data)
        : m_raw(static_cast<decltype(m_raw)>(data))
    {
    }

    ReadonlyBytes raw() const { return m_raw; }

    ErrorOr<size_t> as_size_t() const
    {
        if (m_raw.size() == sizeof(u32))
            return *reinterpret_cast<BigEndian<u32> const*>(m_raw.data());
        if (m_raw.size() == 2 * sizeof(u32))
            return *reinterpret_cast<BigEndian<u64> const*>(m_raw.data());
        return ERANGE;
    }

private:
    Vector<u8, 2 * sizeof(u32)> m_raw;
};

struct Interrupt {
    Node const* domain_root;
    ReadonlyBytes interrupt_identifier;
};

struct Property {
    class ValueStream : public FixedMemoryStream {
    public:
        using AK::FixedMemoryStream::FixedMemoryStream;

        ErrorOr<u32> read_cell()
        {
            return read_value<BigEndian<u32>>();
        }

        ErrorOr<FlatPtr> read_cells(u32 cell_size)
        {
            // FIXME: There are rare cases of 3 cell size big values, even in addresses, especially in addresses
            VERIFY(cell_size <= 2);
            if (cell_size == 1)
                return read_value<BigEndian<u32>>();
            return read_value<BigEndian<u64>>();
        }
    };

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
            auto sub_property = Property { raw_data.slice(offset, sizeof(T)) };
            auto result = callback(sub_property.as<T>());
            if (result.is_error())
                return result;
            if (result.value() == IterationDecision::Break)
                break;
        }
        return {};
    }

    ValueStream as_stream() const { return ValueStream { raw_data }; }
};

// 2.3.6 reg
class RegEntry {
public:
    RegEntry(Address const& address, Size const& size, Node const& node)
        : m_address(address)
        , m_length(size)
        , m_node(node)
    {
    }

    RegEntry(Address&& address, Size&& size, Node const& node)
        : m_address(move(address))
        , m_length(move(size))
        , m_node(node)
    {
    }

    Address bus_address() const { return m_address; }
    Size length() const { return m_length; }

    ErrorOr<Address> resolve_root_address() const;

private:
    Address m_address;
    Size m_length;
    Node const& m_node;
};

class Reg {
public:
    Reg(ReadonlyBytes data, Node const& node)
        : m_raw(data)
        , m_node(node)
    {
    }

    ErrorOr<RegEntry> entry(size_t index) const;
    size_t entry_count() const;

private:
    ReadonlyBytes m_raw;
    Node const& m_node;
};

// 2.3.8 ranges
class RangesEntry {
public:
    RangesEntry(Address const& child_bus_address, Address const& parent_bus_address, Size const& length, Node const& node)
        : m_child_bus_address(child_bus_address)
        , m_parent_bus_address(parent_bus_address)
        , m_length(length)
        , m_node(node)
    {
    }

    RangesEntry(Address&& child_bus_address, Address&& parent_bus_address, Size&& length, Node const& node)
        : m_child_bus_address(move(child_bus_address))
        , m_parent_bus_address(move(parent_bus_address))
        , m_length(move(length))
        , m_node(node)
    {
    }

    Address child_bus_address() const { return m_child_bus_address; }
    Address parent_bus_address() const { return m_parent_bus_address; }
    Size length() const { return m_length; }

    ErrorOr<Address> translate_child_bus_address_to_parent_bus_address(Address const&) const;

private:
    Address m_child_bus_address;
    Address m_parent_bus_address;
    Size m_length;
    Node const& m_node;
};

class Ranges {
public:
    Ranges(ReadonlyBytes data, Node const& node)
        : m_raw(data)
        , m_node(node)
    {
    }

    ErrorOr<RangesEntry> entry(size_t index) const;
    size_t entry_count() const;

    ErrorOr<Address> translate_child_bus_address_to_parent_bus_address(Address const&) const;

private:
    ReadonlyBytes m_raw;
    Node const& m_node;
};

class Node {
    AK_MAKE_NONCOPYABLE(Node);
    AK_MAKE_DEFAULT_MOVABLE(Node);

public:
    bool has_property(StringView prop) const { return m_properties.contains(prop); }
    bool has_child(StringView child) const { return m_children.contains(child); }

    Optional<Property> get_property(StringView prop) const { return m_properties.get(prop).copy(); }

    // FIXME: The spec says that @address parts of the name should be ignored when looking up nodes
    //        when they do not appear in the queried name, and all nodes with the same name should be returned
    Optional<Node const&> get_child(StringView child) const { return m_children.get(child); }

    HashMap<StringView, Node> const& children() const { return m_children; }
    HashMap<StringView, Property> const& properties() const { return m_properties; }

    bool is_root() const { return m_parent == nullptr; }

    Node const* parent() const { return m_parent; }

    // NOTE: When checking for multiple drivers, prefer iterating over the string array instead,
    //       as the compatible strings are sorted by preference, which this function cannot account for.
    bool is_compatible_with(StringView) const;

    // 2.3.5 #address-cells and #size-cells
    u32 address_cells() const
    {
        if (auto prop = get_property("#address-cells"sv); prop.has_value())
            return prop.release_value().as<u32>();

        // If missing, a client program should assume a default value of 2 for #address-cells, and a value of 1 for #size-cells.
        return 2;
    }

    // 2.3.5 #address-cells and #size-cells
    u32 size_cells() const
    {
        if (auto prop = get_property("#size-cells"sv); prop.has_value())
            return prop.release_value().as<u32>();

        // If missing, a client program should assume a default value of 2 for #address-cells, and a value of 1 for #size-cells.
        return 1;
    }

    ErrorOr<Reg> reg() const;
    ErrorOr<Ranges> ranges() const;

    ErrorOr<Address> translate_child_bus_address_to_root_address(Address const&) const;

    // ErrorOr can't hold a reference, so these have to be pointers.
    // The return value of these functions is always non-null.
    ErrorOr<Node const*> interrupt_parent(DeviceTree const& device_tree) const;
    ErrorOr<Node const*> interrupt_domain_root(DeviceTree const& device_tree) const;

    // Handles both the "interrupts" and "interrupts-extended" properties.
    // The returned Interrupt::domain_root is always non-null.
    ErrorOr<Vector<Interrupt>> interrupts(DeviceTree const& device_tree) const;

    // FIXME: Stringify?
    // FIXME: Flatten?
    // Note: That we dont have a oder of children and properties in this view
protected:
    friend class DeviceTree;
    Node(Node* parent)
        : m_parent(parent)
    {
    }
    HashMap<StringView, Node>& children() { return m_children; }
    HashMap<StringView, Property>& properties() { return m_properties; }
    Node* parent() { return m_parent; }

private:
    Node* m_parent;
    HashMap<StringView, Node> m_children;
    HashMap<StringView, Property> m_properties;
};

class DeviceTree : public Node {
public:
    static ErrorOr<NonnullOwnPtr<DeviceTree>> parse(ReadonlyBytes);

    Node const* resolve_node(StringView path) const
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

        Node const* node = this;
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

    Optional<Property> resolve_property(StringView path) const
    {
        auto property_name = path.find_last_split_view('/');
        auto node_path = path.substring_view(0, path.length() - property_name.length() - 1);
        auto const* node = resolve_node(node_path);
        if (!node)
            return {};
        return node->get_property(property_name);
    }

    auto for_each_node(CallableAs<ErrorOr<RecursionDecision>, StringView, Node const&> auto callback) const
    {
        auto iterate = [&](auto self, StringView name, Node const& node) -> ErrorOr<RecursionDecision> {
            auto result = TRY(callback(name, node));

            if (result == RecursionDecision::Recurse) {
                for (auto const& [name, child] : node.children()) {
                    auto child_result = TRY(self(self, name, child));

                    if (child_result == RecursionDecision::Break)
                        return RecursionDecision::Break;
                }

                return RecursionDecision::Continue;
            }

            return result;
        };

        return iterate(iterate, "/"sv, *this);
    }

    Node const* phandle(u32 phandle) const
    {
        if (phandle >= m_phandles.size())
            return nullptr;
        return m_phandles[phandle];
    }

    ReadonlyBytes flattened_device_tree() const { return m_flattened_device_tree; }

private:
    DeviceTree(ReadonlyBytes flattened_device_tree)
        : Node(nullptr)
        , m_flattened_device_tree(flattened_device_tree)
    {
    }

    auto for_each_node(CallableAs<ErrorOr<RecursionDecision>, StringView, Node&> auto callback)
    {
        auto iterate = [&](auto self, StringView name, Node& node) -> ErrorOr<RecursionDecision> {
            auto result = TRY(callback(name, node));

            if (result == RecursionDecision::Recurse) {
                for (auto& [name, child] : node.children()) {
                    auto child_result = TRY(self(self, name, child));

                    if (child_result == RecursionDecision::Break)
                        return RecursionDecision::Break;
                }

                return RecursionDecision::Continue;
            }

            return result;
        };

        return iterate(iterate, "/"sv, *this);
    }

    ErrorOr<void> set_phandle(u32 phandle, Node* node)
    {
        if (m_phandles.size() > phandle && m_phandles[phandle] != nullptr)
            return Error::from_string_view_or_print_error_and_return_errno("Duplicate phandle entry in DeviceTree"sv, EINVAL);
        if (m_phandles.size() <= phandle)
            TRY(m_phandles.try_resize(phandle + 1));
        m_phandles[phandle] = node;
        return {};
    }

    ReadonlyBytes m_flattened_device_tree;
    Vector<Node*> m_phandles;
};

}
