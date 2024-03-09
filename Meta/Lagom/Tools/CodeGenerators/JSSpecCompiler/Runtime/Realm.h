/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/Vector.h>

#include "Runtime/Cell.h"

namespace JSSpecCompiler::Runtime {

class Realm {
public:
    Realm(DiagnosticEngine& diag);

    Runtime::Object* global_object() { return m_global_object; }

    template<typename T>
    T* adopt_cell(T* cell)
    {
        m_cells.append(NonnullOwnPtr<T> { NonnullOwnPtr<T>::AdoptTag::Adopt, *cell });
        return cell;
    }

    DiagnosticEngine& diag() { return m_diag; }

private:
    DiagnosticEngine& m_diag;
    Vector<NonnullOwnPtr<Runtime::Cell>> m_cells;

    Runtime::Object* m_global_object;
};

}
