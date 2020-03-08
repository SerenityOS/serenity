/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibJS/Cell.h>
#include <LibJS/Forward.h>

namespace JS {

class Heap {
    AK_MAKE_NONCOPYABLE(Heap);
    AK_MAKE_NONMOVABLE(Heap);

public:
    explicit Heap(Interpreter&);
    ~Heap();

    template<typename T, typename... Args>
    T* allocate(Args&&... args)
    {
        auto* memory = allocate_cell(sizeof(T));
        new (memory) T(forward<Args>(args)...);
        return static_cast<T*>(memory);
    }

    void collect_garbage();

private:
    Cell* allocate_cell(size_t);

    void collect_roots(HashTable<Cell*>&);
    void visit_live_cells(const HashTable<Cell*>& roots, HashTable<Cell*>& live_cells);
    void mark_live_cells(const HashTable<Cell*>& live_cells);
    void sweep_dead_cells();

    Interpreter& m_interpreter;
    Vector<NonnullOwnPtr<HeapBlock>> m_blocks;
};

}
