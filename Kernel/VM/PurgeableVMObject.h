#pragma once

#include <Kernel/VM/AnonymousVMObject.h>

class PurgeableVMObject final : public AnonymousVMObject {
public:
    virtual ~PurgeableVMObject() override;

    static NonnullRefPtr<PurgeableVMObject> create_with_size(size_t);
    virtual NonnullRefPtr<VMObject> clone() override;

    int purge();

    bool was_purged() const { return m_was_purged; }
    void set_was_purged(bool b) { m_was_purged = b; }

    bool is_volatile() const { return m_volatile; }
    void set_volatile(bool b) { m_volatile = b; }

private:
    explicit PurgeableVMObject(size_t);
    explicit PurgeableVMObject(const PurgeableVMObject&);

    PurgeableVMObject& operator=(const PurgeableVMObject&) = delete;
    PurgeableVMObject& operator=(PurgeableVMObject&&) = delete;
    PurgeableVMObject(PurgeableVMObject&&) = delete;

    virtual bool is_purgeable() const override { return true; }

    bool m_was_purged { false };
    bool m_volatile { false };
};
