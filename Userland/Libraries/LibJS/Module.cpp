/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Module.h>

namespace JS {

Module::Module(Realm& realm)
    : m_vm(realm.vm())
    , m_realm(make_handle(&realm))
{
}

Module::~Module()
{
}

}
