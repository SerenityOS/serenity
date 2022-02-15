/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/WeakPtr.h>
#include <LibCore/Event.h>
#include <LibCore/Object.h>

namespace Core {

ChildEvent::ChildEvent(Type type, Object& child, Object* insertion_before_child)
    : Core::Event(type)
    , m_child(child.make_weak_ptr())
    , m_insertion_before_child(AK::make_weak_ptr_if_nonnull(insertion_before_child))
{
}

ChildEvent::~ChildEvent()
{
}

Object* ChildEvent::child()
{
    if (auto ref = m_child.strong_ref())
        return ref.ptr();
    return nullptr;
}

const Object* ChildEvent::child() const
{
    if (auto ref = m_child.strong_ref())
        return ref.ptr();
    return nullptr;
}

Object* ChildEvent::insertion_before_child()
{
    if (auto ref = m_insertion_before_child.strong_ref())
        return ref.ptr();
    return nullptr;
}

const Object* ChildEvent::insertion_before_child() const
{
    if (auto ref = m_insertion_before_child.strong_ref())
        return ref.ptr();
    return nullptr;
}

}
