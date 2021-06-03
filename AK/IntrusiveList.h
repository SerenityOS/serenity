/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/BitCast.h>
#include <AK/Forward.h>
#include <AK/StdLibExtras.h>

namespace AK {

namespace Detail {
template<typename T, typename Container = RawPtr<T>>
class IntrusiveListNode;

template<typename T, typename Container>
struct SubstituteIntrusiveListNodeContainerType {
    using Type = Container;
};

template<typename T>
struct SubstituteIntrusiveListNodeContainerType<T, NonnullRefPtr<T>> {
    using Type = RefPtr<T>;
};
}

template<typename T, typename Container = RawPtr<T>>
using IntrusiveListNode = Detail::IntrusiveListNode<T, typename Detail::SubstituteIntrusiveListNodeContainerType<T, Container>::Type>;

template<typename T, typename Container>
class IntrusiveListStorage {
private:
    friend class Detail::IntrusiveListNode<T, Container>;

    template<class T_, typename Container_, IntrusiveListNode<T_, Container_> T_::*member>
    friend class IntrusiveList;

    IntrusiveListNode<T, Container>* m_first { nullptr };
    IntrusiveListNode<T, Container>* m_last { nullptr };
};

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
class IntrusiveList {
public:
    IntrusiveList() = default;
    ~IntrusiveList();

    void clear();
    [[nodiscard]] bool is_empty() const;
    void append(T& n);
    void prepend(T& n);
    void remove(T& n);
    [[nodiscard]] bool contains(const T&) const;
    [[nodiscard]] Container first() const;
    [[nodiscard]] Container last() const;

    [[nodiscard]] Container take_first();
    [[nodiscard]] Container take_last();

    class Iterator {
    public:
        Iterator() = default;
        Iterator(T* value)
            : m_value(move(value))
        {
        }

        const T& operator*() const { return *m_value; }
        auto operator->() const { return m_value; }
        T& operator*() { return *m_value; }
        auto operator->() { return m_value; }
        bool operator==(const Iterator& other) const { return other.m_value == m_value; }
        bool operator!=(const Iterator& other) const { return !(*this == other); }
        Iterator& operator++()
        {
            m_value = IntrusiveList<T, Container, member>::next(m_value);
            return *this;
        }
        Iterator& erase();

    private:
        T* m_value { nullptr };
    };

    Iterator begin();
    Iterator end() { return Iterator {}; }

    class ReverseIterator {
    public:
        ReverseIterator() = default;
        ReverseIterator(T* value)
            : m_value(move(value))
        {
        }

        const T& operator*() const { return *m_value; }
        auto operator->() const { return m_value; }
        T& operator*() { return *m_value; }
        auto operator->() { return m_value; }
        bool operator==(const ReverseIterator& other) const { return other.m_value == m_value; }
        bool operator!=(const ReverseIterator& other) const { return !(*this == other); }
        ReverseIterator& operator++()
        {
            m_value = IntrusiveList<T, Container, member>::prev(m_value);
            return *this;
        }
        ReverseIterator& erase();

    private:
        T* m_value { nullptr };
    };

    ReverseIterator rbegin();
    ReverseIterator rend() { return ReverseIterator {}; }

    class ConstIterator {
    public:
        ConstIterator() = default;
        ConstIterator(const T* value)
            : m_value(value)
        {
        }

        const T& operator*() const { return *m_value; }
        auto operator->() const { return m_value; }
        bool operator==(const ConstIterator& other) const { return other.m_value == m_value; }
        bool operator!=(const ConstIterator& other) const { return !(*this == other); }
        ConstIterator& operator++()
        {
            m_value = IntrusiveList<T, Container, member>::next(m_value);
            return *this;
        }

    private:
        const T* m_value { nullptr };
    };

    ConstIterator begin() const;
    ConstIterator end() const { return ConstIterator {}; }

private:
    static T* next(T* current);
    static T* prev(T* current);
    static const T* next(const T* current);
    static const T* prev(const T* current);
    static T* node_to_value(IntrusiveListNode<T, Container>& node);
    IntrusiveListStorage<T, Container> m_storage;
};

template<typename Contained, bool _IsRaw>
struct SelfReferenceIfNeeded {
    Contained reference = nullptr;
};
template<typename Contained>
struct SelfReferenceIfNeeded<Contained, true> {
};

namespace Detail {

template<typename T, typename Container>
class IntrusiveListNode {
public:
    ~IntrusiveListNode();
    void remove();
    bool is_in_list() const;

    static constexpr bool IsRaw = IsPointer<Container>;

    // Note: For some reason, clang does not consider `member` as declared here, and as declared above (`IntrusiveListNode<T, Container> T::*`)
    //       to be of equal types. so for now, just make the members public on clang.
#ifndef __clang__
private:
    template<class T_, typename Container_, IntrusiveListNode<T_, Container_> T_::*member>
    friend class ::AK::IntrusiveList;
#endif

    IntrusiveListStorage<T, Container>* m_storage = nullptr;
    IntrusiveListNode<T, Container>* m_next = nullptr;
    IntrusiveListNode<T, Container>* m_prev = nullptr;
    SelfReferenceIfNeeded<Container, IsRaw> m_self;
};

}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline typename IntrusiveList<T, Container, member>::Iterator& IntrusiveList<T, Container, member>::Iterator::erase()
{
    auto old = m_value;
    m_value = IntrusiveList<T, Container, member>::next(m_value);
    (old->*member).remove();
    return *this;
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline IntrusiveList<T, Container, member>::~IntrusiveList()
{
    clear();
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline void IntrusiveList<T, Container, member>::clear()
{
    while (m_storage.m_first)
        m_storage.m_first->remove();
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline bool IntrusiveList<T, Container, member>::is_empty() const
{
    return m_storage.m_first == nullptr;
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline void IntrusiveList<T, Container, member>::append(T& n)
{
    remove(n);

    auto& nnode = n.*member;
    nnode.m_storage = &m_storage;
    nnode.m_prev = m_storage.m_last;
    nnode.m_next = nullptr;
    if constexpr (!RemoveReference<decltype(nnode)>::IsRaw)
        nnode.m_self.reference = &n; // Note: Self-reference ensures that the object will keep a ref to itself when the Container is a smart pointer.

    if (m_storage.m_last)
        m_storage.m_last->m_next = &nnode;
    m_storage.m_last = &nnode;
    if (!m_storage.m_first)
        m_storage.m_first = &nnode;
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline void IntrusiveList<T, Container, member>::prepend(T& n)
{
    auto& nnode = n.*member;
    if (nnode.m_storage)
        nnode.remove();

    nnode.m_storage = &m_storage;
    nnode.m_prev = nullptr;
    nnode.m_next = m_storage.m_first;
    if constexpr (!RemoveReference<decltype(nnode)>::IsRaw)
        nnode.m_self.reference = &n;

    if (m_storage.m_first)
        m_storage.m_first->m_prev = &nnode;
    m_storage.m_first = &nnode;
    if (!m_storage.m_last)
        m_storage.m_last = &nnode;
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline void IntrusiveList<T, Container, member>::remove(T& n)
{
    auto& nnode = n.*member;
    if (nnode.m_storage)
        nnode.remove();
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline bool IntrusiveList<T, Container, member>::contains(const T& n) const
{
    auto& nnode = n.*member;
    return nnode.m_storage == &m_storage;
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline Container IntrusiveList<T, Container, member>::first() const
{
    return m_storage.m_first ? node_to_value(*m_storage.m_first) : nullptr;
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline Container IntrusiveList<T, Container, member>::take_first()
{
    if (Container ptr = first()) {
        remove(*ptr);
        return ptr;
    }
    return nullptr;
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline Container IntrusiveList<T, Container, member>::take_last()
{
    if (Container ptr = last()) {
        remove(*ptr);
        return ptr;
    }
    return nullptr;
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline Container IntrusiveList<T, Container, member>::last() const
{
    return m_storage.m_last ? node_to_value(*m_storage.m_last) : nullptr;
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline const T* IntrusiveList<T, Container, member>::next(const T* current)
{
    auto& nextnode = (current->*member).m_next;
    const T* nextstruct = nextnode ? node_to_value(*nextnode) : nullptr;
    return nextstruct;
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline const T* IntrusiveList<T, Container, member>::prev(const T* current)
{
    auto& prevnode = (current->*member).m_prev;
    const T* prevstruct = prevnode ? node_to_value(*prevnode) : nullptr;
    return prevstruct;
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline T* IntrusiveList<T, Container, member>::next(T* current)
{
    auto& nextnode = (current->*member).m_next;
    T* nextstruct = nextnode ? node_to_value(*nextnode) : nullptr;
    return nextstruct;
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline T* IntrusiveList<T, Container, member>::prev(T* current)
{
    auto& prevnode = (current->*member).m_prev;
    T* prevstruct = prevnode ? node_to_value(*prevnode) : nullptr;
    return prevstruct;
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline typename IntrusiveList<T, Container, member>::Iterator IntrusiveList<T, Container, member>::begin()
{
    return m_storage.m_first ? Iterator(node_to_value(*m_storage.m_first)) : Iterator();
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline typename IntrusiveList<T, Container, member>::ReverseIterator IntrusiveList<T, Container, member>::rbegin()
{
    return m_storage.m_last ? ReverseIterator(node_to_value(*m_storage.m_last)) : ReverseIterator();
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline typename IntrusiveList<T, Container, member>::ConstIterator IntrusiveList<T, Container, member>::begin() const
{
    return m_storage.m_first ? ConstIterator(node_to_value(*m_storage.m_first)) : ConstIterator();
}

template<class T, typename Container, IntrusiveListNode<T, Container> T::*member>
inline T* IntrusiveList<T, Container, member>::node_to_value(IntrusiveListNode<T, Container>& node)
{
    // Note: Since this might seem odd, here's an explanation on what this function actually does:
    //       `node` is a reference that resides in some part of the actual value (of type T), the
    //       placement (i.e. offset) of which is described by the pointer-to-data-member parameter
    //       named `member`.
    //       This function effectively takes in the address of the data member, and returns the address
    //       of the value (of type T) holding that member.
    return bit_cast<T*>(bit_cast<unsigned char*>(&node) - bit_cast<unsigned char*>(member));
}

namespace Detail {

template<typename T, typename Container>
inline IntrusiveListNode<T, Container>::~IntrusiveListNode()
{
    VERIFY(!is_in_list());
}

template<typename T, typename Container>
inline void IntrusiveListNode<T, Container>::remove()
{
    VERIFY(m_storage);
    if (m_storage->m_first == this)
        m_storage->m_first = m_next;
    if (m_storage->m_last == this)
        m_storage->m_last = m_prev;
    if (m_prev)
        m_prev->m_next = m_next;
    if (m_next)
        m_next->m_prev = m_prev;
    m_prev = nullptr;
    m_next = nullptr;
    m_storage = nullptr;
    if constexpr (!IsRaw)
        m_self.reference = nullptr;
}

template<typename T, typename Container>
inline bool IntrusiveListNode<T, Container>::is_in_list() const
{
    return m_storage != nullptr;
}

}

// Specialise IntrusiveList for NonnullRefPtr
// By default, intrusive lists cannot contain null entries anyway, so switch to RefPtr
// and just make the user-facing functions deref the pointers.

template<class T, IntrusiveListNode<T, NonnullRefPtr<T>> T::*member>
class IntrusiveList<T, NonnullRefPtr<T>, member> : public IntrusiveList<T, RefPtr<T>, member> {
public:
    [[nodiscard]] NonnullRefPtr<T> first() const { return *IntrusiveList<T, RefPtr<T>, member>::first(); }
    [[nodiscard]] NonnullRefPtr<T> last() const { return *IntrusiveList<T, RefPtr<T>, member>::last(); }

    [[nodiscard]] NonnullRefPtr<T> take_first() { return *IntrusiveList<T, RefPtr<T>, member>::take_first(); }
    [[nodiscard]] NonnullRefPtr<T> take_last() { return *IntrusiveList<T, RefPtr<T>, member>::take_last(); }
};

}

using AK::IntrusiveList;
using AK::IntrusiveListNode;
