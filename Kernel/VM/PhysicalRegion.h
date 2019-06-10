#pragma once

#include <AK/Retained.h>
#include <Kernel/Assertions.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/PhysicalPage.h>

class PhysicalRegion : public Retainable<PhysicalRegion> {
    AK_MAKE_ETERNAL

    friend class MemoryManager;

public:
    static Retained<PhysicalRegion> create(PhysicalAddress lower, PhysicalAddress upper);
    ~PhysicalRegion() {}

    bool is_empty() { return m_next == m_upper; }
    int size() { return (m_upper.get() - m_next.get()) / PAGE_SIZE; }

    PhysicalAddress next();

private:
    PhysicalRegion(PhysicalAddress lower, PhysicalAddress upper);

    PhysicalAddress m_lower;
    PhysicalAddress m_upper;
    PhysicalAddress m_next;
};
