# LibJS Garbage Collection

LibJS has a [non-generational](https://en.wikipedia.org/wiki/Tracing_garbage_collection#Generational_GC_(ephemeral_GC)), [non-compacting](https://en.wikipedia.org/wiki/Tracing_garbage_collection#Moving_vs._non-moving), [stop-the-world](https://en.wikipedia.org/wiki/Tracing_garbage_collection#Stop-the-world_vs._incremental_vs._concurrent) [mark-and-sweep](https://en.wikipedia.org/wiki/Tracing_garbage_collection#Na%C3%AFve_mark-and-sweep) garbage collector.

## Allocating Cells

Allocation and garbage collection are tightly integrated and therefore the story of a JS object and its relationship with the GC starts at its allocation. The `Interpreter` has a member of the `Heap` class which allows to allocate a new `Cell` which will be kept alive as long as the Interpreter needs it (has an active reference to it). While the GC is an integral part of the JS engine, it is not limited to just JS objects. Any class like the `Object` class (JS object) can inherit from `Cell` and can then be managed by the GC. The browser for example also leverages the GC to manage DOM elements.

The actual allocations of the objects aren't made with `malloc` and `free` but are made inside `HeapBlock`s of 16KB each. This is a performance improvement as the GC has to visit the entire heap during collection and this increases locality (without being a compacting GC).

The `Heap` class outsources the allocations to the `CellAllocator` which manages a list of heap blocks. Cells can have different sizes because different JS objects inherit from `Cell`. Therefore,  multiple cell allocators exist where each one manages cells of a particular maximum size and the smallest allocator that can hold the required cell is chosen.

In case all available heap blocks of a cell allocator are full, a new heap block is allocated by the `BlockAllocator`. The block allocator is a general class to allocate blocks of memory and is not bound to the `HeapBlock` class. The big advantage of this class is that it doesn't immediately free empty blocks but keeps them around for some time, in case it can reuse them ([relevant Andreas Kling video: Improving JS allocation performance by recycling GC heap blocks](https://www.youtube.com/watch?v=x9xLOuMK9wo)). If no available heap blocks are available, a new one will be allocated with `aligned_alloc`.

## Triggering the Garbage Collector

The GC is triggered every `100000` allocations, independent of the size of the allocations (in the smallest size that means every 1.6MB or on the other extreme every 307.2MB). 

There is also an option to stress-test the GC and run it on every allocation. (`js --gc-on-every-allocation` or `js -g` when running the standalone interpreter)

## Mark and Sweep

To mark all reachable cells, an initial set of cells is needed which is often called the GC-roots. LibJS gathers these roots from the `VM` and also with a conservative GC that scans all registers and the stack for pointers that _could_ reference a `Cell`. This was added so that the GC doesn't remove an object while it is being constructed which might take multiple allocations ([relevant Andreas Kling video: Conservative garbage collection](https://www.youtube.com/watch?v=c6IghR0W254)).

Cells can hold references to other Cells and during marking the GC needs to trace them all. This is solved with the `visit_edges` virtual method that classes inheriting from `Cell` must implement.

Once all reachable cells are marked, the GC iterates over all cells, and deallocates all cells that aren't marked. Cells that were marked get their mark bit reset, which allows the next GC cycle to start off with all cells being unmarked.