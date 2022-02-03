/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Types.h>

namespace AK {

namespace Detail {

template<typename TypeA, typename Default>
struct SubstituteIfVoid {
    using Type = TypeA;
};

template<typename Default>
struct SubstituteIfVoid<void, Default> {
    using Type = Default;
};

template<typename DeclaredBaseType, typename DefaultBaseType, typename ValueType, typename MetadataT, typename ValueTraits>
class Trie {
    using BaseType = typename SubstituteIfVoid<DeclaredBaseType, DefaultBaseType>::Type;

    class ConstIterator {

    public:
        static ConstIterator end() { return {}; }

        bool operator==(const ConstIterator& other) const { return m_current_node == other.m_current_node; }

        const BaseType& operator*() const { return static_cast<const BaseType&>(*m_current_node); }
        const BaseType* operator->() const { return static_cast<const BaseType*>(m_current_node); }
        void operator++() { skip_to_next(); }

        explicit ConstIterator(const Trie& node)
        {
            m_current_node = &node;
            // FIXME: Figure out how to OOM harden this iterator.
            MUST(m_state.try_empend(false, node.m_children.begin(), node.m_children.end()));
        }

    private:
        void skip_to_next()
        {
            auto& current_state = m_state.last();
            if (current_state.did_generate_root)
                ++current_state.it;
            else
                current_state.did_generate_root = true;
            if (current_state.it == current_state.end)
                return pop_and_get_next();

            m_current_node = &*(*current_state.it).value;

            // FIXME: Figure out how to OOM harden this iterator.
            MUST(m_state.try_empend(false, m_current_node->m_children.begin(), m_current_node->m_children.end()));
        }
        void pop_and_get_next()
        {
            m_state.take_last();
            if (m_state.is_empty()) {
                m_current_node = nullptr;
                return;
            }

            skip_to_next();
        }

        ConstIterator() = default;

        struct State {
            bool did_generate_root { false };
            typename HashMap<ValueType, NonnullOwnPtr<Trie>, ValueTraits>::ConstIteratorType it;
            typename HashMap<ValueType, NonnullOwnPtr<Trie>, ValueTraits>::ConstIteratorType end;
        };
        Vector<State> m_state;
        const Trie* m_current_node { nullptr };
    };

public:
    using MetadataType = MetadataT;

    Trie(ValueType value, Optional<MetadataType> metadata)
        : m_value(move(value))
        , m_metadata(move(metadata))
    {
    }

    template<typename It>
    BaseType& traverse_until_last_accessible_node(It& it, const It& end)
    {
        Trie* node = this;
        for (; it < end; ++it) {
            auto next_it = node->m_children.find(*it);
            if (next_it == node->m_children.end())
                return static_cast<BaseType&>(*node);
            node = &*(*next_it).value;
        }
        return static_cast<BaseType&>(*node);
    }

    template<typename It>
    const BaseType& traverse_until_last_accessible_node(It& it, const It& end) const { return const_cast<Trie*>(this)->traverse_until_last_accessible_node(it, end); }

    template<typename It>
    BaseType& traverse_until_last_accessible_node(const It& begin, const It& end)
    {
        auto it = begin;
        return const_cast<Trie*>(this)->traverse_until_last_accessible_node(it, end);
    }

    template<typename It>
    const BaseType& traverse_until_last_accessible_node(const It& begin, const It& end) const
    {
        auto it = begin;
        return const_cast<Trie*>(this)->traverse_until_last_accessible_node(it, end);
    }

    Optional<MetadataType> metadata() const requires(!IsNullPointer<MetadataType>) { return m_metadata; }
    void set_metadata(MetadataType metadata) requires(!IsNullPointer<MetadataType>) { m_metadata = move(metadata); }
    const MetadataType& metadata_value() const requires(!IsNullPointer<MetadataType>) { return m_metadata.value(); }

    const ValueType& value() const { return m_value; }
    ValueType& value() { return m_value; }

    Trie& ensure_child(ValueType value, Optional<MetadataType> metadata = {})
    {
        auto it = m_children.find(value);
        if (it == m_children.end()) {
            auto node = adopt_nonnull_own_or_enomem(new (nothrow) Trie(value, move(metadata))).release_value_but_fixme_should_propagate_errors();
            auto& node_ref = *node;
            m_children.set(move(value), move(node));
            return static_cast<BaseType&>(node_ref);
        }

        auto& node_ref = *it->value;
        if (metadata.has_value())
            node_ref.m_metadata = move(metadata);
        return static_cast<BaseType&>(node_ref);
    }

    template<typename It, typename ProvideMetadataFunction>
    BaseType& insert(
        It& it, const It& end, MetadataType metadata, ProvideMetadataFunction provide_missing_metadata) requires(!IsNullPointer<MetadataType>)
    {
        Trie* last_root_node = &traverse_until_last_accessible_node(it, end);
        for (; it != end; ++it)
            last_root_node = static_cast<Trie*>(&last_root_node->ensure_child(*it, provide_missing_metadata(static_cast<BaseType&>(*last_root_node), it)));
        last_root_node->set_metadata(move(metadata));
        return static_cast<BaseType&>(*last_root_node);
    }

    template<typename It>
    BaseType& insert(It& it, const It& end) requires(IsNullPointer<MetadataType>)
    {
        Trie* last_root_node = &traverse_until_last_accessible_node(it, end);
        for (; it != end; ++it)
            last_root_node = static_cast<Trie*>(&last_root_node->ensure_child(*it, {}));
        return static_cast<BaseType&>(*last_root_node);
    }

    template<typename It, typename ProvideMetadataFunction>
    BaseType& insert(
        const It& begin, const It& end, MetadataType metadata, ProvideMetadataFunction provide_missing_metadata) requires(!IsNullPointer<MetadataType>)
    {
        auto it = begin;
        return insert(it, end, move(metadata), move(provide_missing_metadata));
    }

    template<typename It>
    BaseType& insert(const It& begin, const It& end) requires(IsNullPointer<MetadataType>)
    {
        auto it = begin;
        return insert(it, end);
    }

    HashMap<ValueType, NonnullOwnPtr<Trie>, ValueTraits>& children() { return m_children; }
    HashMap<ValueType, NonnullOwnPtr<Trie>, ValueTraits> const& children() const { return m_children; }

    ConstIterator begin() const { return ConstIterator(*this); }
    ConstIterator end() const { return ConstIterator::end(); }

    [[nodiscard]] bool is_empty() const { return m_children.is_empty(); }
    void clear() { m_children.clear(); }

    BaseType deep_copy()
    {
        Trie root(m_value, m_metadata);
        for (auto& it : m_children)
            root.m_children.set(it.key, adopt_nonnull_own_or_enomem(new (nothrow) Trie(it.value->deep_copy())).release_value_but_fixme_should_propagate_errors());
        return static_cast<BaseType&&>(move(root));
    }

private:
    ValueType m_value;
    Optional<MetadataType> m_metadata;
    HashMap<ValueType, NonnullOwnPtr<Trie>, ValueTraits> m_children;
};

template<typename BaseType, typename DefaultBaseType, typename ValueType, typename ValueTraits>
class Trie<BaseType, DefaultBaseType, ValueType, void, ValueTraits> : public Trie<BaseType, DefaultBaseType, ValueType, decltype(nullptr), ValueTraits> {
    using Trie<BaseType, DefaultBaseType, ValueType, decltype(nullptr), ValueTraits>::Trie;
};

}

template<typename ValueType, typename MetadataT = void, typename ValueTraits = Traits<ValueType>, typename BaseT = void>
class Trie : public Detail::Trie<BaseT, Trie<ValueType, MetadataT, ValueTraits>, ValueType, MetadataT, ValueTraits> {
public:
    using DetailTrie = Detail::Trie<BaseT, Trie<ValueType, MetadataT, ValueTraits>, ValueType, MetadataT, ValueTraits>;
    using MetadataType = typename DetailTrie::MetadataType;

    Trie(ValueType value, MetadataType metadata) requires(!IsVoid<MetadataType> && !IsNullPointer<MetadataType>)
        : DetailTrie(move(value), move(metadata))
    {
    }

    explicit Trie(ValueType value)
        : DetailTrie(move(value), Optional<MetadataType> {})
    {
    }
};

}

using AK::Trie;
