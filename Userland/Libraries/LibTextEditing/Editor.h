/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <LibTextEditing/Forward.h>

namespace TextEditing {

// The editor is represented by some sort of interface.
// It is closely coupled with an editing engine that actually performs editing, but that engine might be exchanged at any moment.
class Editor : public RefCounted<Editor> {
public:
    void set_engine(NonnullOwnPtr<Engine> engine);

    void link_with_interface(RefPtr<Interface> interface);
    RefPtr<Interface const> interface() const;

    NonnullRefPtr<Engine> engine(Badge<Interface>);

private:
    friend class Interface;

    NonnullOwnPtr<Engine> m_engine;
    RefPtr<Interface> m_interface;
};

}
