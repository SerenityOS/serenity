#pragma once

#include <Kernel/UnixTypes.h>
#include <Kernel/VM/VMObject.h>

class InodeVMObject final : public VMObject {
public:
    virtual ~InodeVMObject() override;

    static NonnullRefPtr<InodeVMObject> create_with_inode(Inode&);
    virtual NonnullRefPtr<VMObject> clone() override;

    Inode& inode() { return *m_inode; }
    const Inode& inode() const { return *m_inode; }

    void inode_contents_changed(Badge<Inode>, off_t, ssize_t, const u8*);
    void inode_size_changed(Badge<Inode>, size_t old_size, size_t new_size);

private:
    explicit InodeVMObject(Inode&);
    explicit InodeVMObject(const InodeVMObject&);

    InodeVMObject& operator=(const InodeVMObject&) = delete;
    InodeVMObject& operator=(InodeVMObject&&) = delete;
    InodeVMObject(InodeVMObject&&) = delete;

    virtual bool is_inode() const override { return true; }

    NonnullRefPtr<Inode> m_inode;
};
