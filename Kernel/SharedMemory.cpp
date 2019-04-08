#include <Kernel/SharedMemory.h>
#include <Kernel/VM/VMObject.h>

SharedMemory::SharedMemory()
{
}

SharedMemory::~SharedMemory()
{
}

KResult SharedMemory::truncate(int length)
{
    if (!length) {
        m_vmo = nullptr;
        return KSuccess;
    }

    if (!m_vmo) {
        m_vmo = VMObject::create_anonymous(length);
        return KSuccess;
    }

    // FIXME: Support truncation.
    ASSERT_NOT_REACHED();
    return KResult(-ENOTIMPL);
}
