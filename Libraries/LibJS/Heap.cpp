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

#include <AK/HashTable.h>
#include <LibJS/Heap.h>
#include <LibJS/HeapBlock.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Object.h>

#define HEAP_DEBUG

namespace JS {

Heap::Heap(Interpreter& interpreter)
    : m_interpreter(interpreter)
{
}

Heap::~Heap()
{
}

Cell* Heap::allocate_cell(size_t size)
{
    for (auto& block : m_blocks) {
        if (size > block->cell_size())
            continue;
        if (auto* cell = block->allocate())
            return cell;
    }

    auto* block = (HeapBlock*)malloc(HeapBlock::block_size);
    new (block) HeapBlock(size);
    m_blocks.append(NonnullOwnPtr<HeapBlock>(NonnullOwnPtr<HeapBlock>::Adopt, *block));
    return block->allocate();
}

void Heap::collect_garbage()
{
    HashTable<Cell*> roots;
    collect_roots(roots);

    HashTable<Cell*> live_cells;
    visit_live_cells(roots, live_cells);

    mark_live_cells(live_cells);
    sweep_dead_cells();
}

void Heap::collect_roots(HashTable<Cell*>& roots)
{
    roots.set(&m_interpreter.global_object());

#ifdef HEAP_DEBUG
    dbg() << "collect_roots:";
    for (auto* root : roots) {
        dbg() << "  + " << root;
    }
#endif
}

class LivenessVisitor final : public Cell::Visitor {
public:
    LivenessVisitor(HashTable<Cell*>& live_cells)
        : m_live_cells(live_cells)
    {
    }

    virtual void did_visit(Cell* cell) override
    {
        m_live_cells.set(cell);
    }

private:
    HashTable<Cell*>& m_live_cells;
};

void Heap::visit_live_cells(const HashTable<Cell*>& roots, HashTable<Cell*>& live_cells)
{
    LivenessVisitor visitor(live_cells);
    for (auto* root : roots) {
        root->visit_graph(visitor);
    }

#ifdef HEAP_DEBUG
    dbg() << "visit_live_cells:";
    for (auto* cell : live_cells) {
        dbg() << "  @ " << cell;
    }
#endif
}

void Heap::mark_live_cells(const HashTable<Cell*>& live_cells)
{
#ifdef HEAP_DEBUG
    dbg() << "mark_live_cells:";
#endif
    for (auto& cell : live_cells) {
#ifdef HEAP_DEBUG
        dbg() << "  ! " << cell;
#endif
        cell->set_marked(true);
    }
}

void Heap::sweep_dead_cells()
{
#ifdef HEAP_DEBUG
    dbg() << "sweep_dead_cells:";
#endif
    for (auto& block : m_blocks) {
        block->for_each_cell([&](Cell* cell) {
            if (cell->is_live()) {
                if (!cell->is_marked()) {
#ifdef HEAP_DEBUG
                    dbg() << "  ~ " << cell;
#endif
                    block->deallocate(cell);
                } else {
                    cell->set_marked(false);
                }
            }
        });
    }
}

}
