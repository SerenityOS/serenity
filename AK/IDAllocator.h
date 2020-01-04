#pragma once

#include <stdlib.h>
#include <AK/HashTable.h>

namespace AK {

class IDAllocator {

public:
    IDAllocator() {}
    ~IDAllocator() {}

    int allocate()
    {
        int r = rand();
        for (int i = 0; i < 100000; ++i) {
            int allocated_id = r + i;
            if (!m_allocated_ids.contains(allocated_id)) {
                m_allocated_ids.set(allocated_id);
                return allocated_id;
            }
        }
        ASSERT_NOT_REACHED();
    }

    void deallocate(int id)
    {
        m_allocated_ids.remove(id);
    }

private:
    HashTable<int> m_allocated_ids;
};
}

using AK::IDAllocator;
