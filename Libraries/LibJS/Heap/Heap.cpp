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

#include <AK/Badge.h>
#include <AK/HashTable.h>
#include <LibJS/Heap/Handle.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Heap/HeapBlock.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/Object.h>
#include <serenity.h>
#include <setjmp.h>
#include <stdio.h>

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
    if (should_collect_on_every_allocation())
        collect_garbage();

    for (auto& block : m_blocks) {
        if (size > block->cell_size())
            continue;
        if (auto* cell = block->allocate())
            return cell;
    }

    auto block = HeapBlock::create_with_cell_size(*this, size);
    auto* cell = block->allocate();
    m_blocks.append(move(block));
    return cell;
}

void Heap::collect_garbage()
{
    HashTable<Cell*> roots;
    gather_roots(roots);
    mark_live_cells(roots);
    sweep_dead_cells();
}

void Heap::gather_roots(HashTable<Cell*>& roots)
{
    m_interpreter.gather_roots({}, roots);

    gather_conservative_roots(roots);

    for (auto* handle : m_handles)
        roots.set(handle->cell());

#ifdef HEAP_DEBUG
    dbg() << "gather_roots:";
    for (auto* root : roots) {
        dbg() << "  + " << root;
    }
#endif
}

void Heap::gather_conservative_roots(HashTable<Cell*>& roots)
{
    FlatPtr dummy;

#ifdef HEAP_DEBUG
    dbg() << "gather_conservative_roots:";
#endif

    jmp_buf buf;
    setjmp(buf);

    HashTable<FlatPtr> possible_pointers;

    for (size_t i = 0; i < (sizeof(buf->regs) / sizeof(FlatPtr)); ++i)
        possible_pointers.set(buf->regs[i]);

    FlatPtr stack_base;
    size_t stack_size;
    if (get_stack_bounds(&stack_base, &stack_size) < 0) {
        perror("get_stack_bounds");
        ASSERT_NOT_REACHED();
    }

    FlatPtr stack_reference = reinterpret_cast<FlatPtr>(&dummy);
    FlatPtr stack_top = stack_base + stack_size;

    for (FlatPtr stack_address = stack_reference; stack_address < stack_top; stack_address += sizeof(FlatPtr)) {
        auto data = *reinterpret_cast<FlatPtr*>(stack_address);
        possible_pointers.set(data);
    }

    for (auto possible_pointer : possible_pointers) {
        if (!possible_pointer)
            continue;
#ifdef HEAP_DEBUG
        dbg() << "  ? " << (const void*)possible_pointer;
#endif
        if (auto* cell = cell_from_possible_pointer(possible_pointer)) {
            if (cell->is_live()) {
#ifdef HEAP_DEBUG
                dbg() << "  ?-> " << (const void*)cell;
#endif
                roots.set(cell);
            } else {
#ifdef HEAP_DEBUG
                dbg() << "  #-> " << (const void*)cell;
#endif
            }
        }
    }
}

Cell* Heap::cell_from_possible_pointer(FlatPtr pointer)
{
    auto* possible_heap_block = HeapBlock::from_cell(reinterpret_cast<const Cell*>(pointer));
    if (m_blocks.find([possible_heap_block](auto& block) { return block.ptr() == possible_heap_block; }) == m_blocks.end())
        return nullptr;
    return possible_heap_block->cell_from_possible_pointer(pointer);
}

class MarkingVisitor final : public Cell::Visitor {
public:
    MarkingVisitor() {}

    virtual void visit(Cell* cell)
    {
        if (cell->is_marked())
            return;
#ifdef HEAP_DEBUG
        dbg() << "  ! " << cell;
#endif
        cell->set_marked(true);
        cell->visit_children(*this);
    }
};

void Heap::mark_live_cells(const HashTable<Cell*>& roots)
{
#ifdef HEAP_DEBUG
    dbg() << "mark_live_cells:";
#endif
    MarkingVisitor visitor;
    for (auto* root : roots) {
        if (!root)
            continue;
        visitor.visit(root);
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

void Heap::did_create_handle(Badge<HandleImpl>, HandleImpl& impl)
{
    ASSERT(!m_handles.contains(&impl));
    m_handles.set(&impl);
}

void Heap::did_destroy_handle(Badge<HandleImpl>, HandleImpl& impl)
{
    ASSERT(m_handles.contains(&impl));
    m_handles.remove(&impl);
}

}
