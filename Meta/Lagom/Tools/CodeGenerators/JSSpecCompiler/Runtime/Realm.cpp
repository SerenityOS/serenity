/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Runtime/Realm.h"
#include "Runtime/Object.h"

namespace JSSpecCompiler::Runtime {

Realm::Realm(DiagnosticEngine& diag)
    : m_diag(diag)
    , m_global_object(Object::create(this))
{
}

}
