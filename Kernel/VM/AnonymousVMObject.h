#pragma once

#include <Kernel/VM/PhysicalAddress.h>
#include <Kernel/VM/VMObject.h>

class AnonymousVMObject : public VMObject {
public:
    virtual ~AnonymousVMObject() override;

    static NonnullRefPtr<AnonymousVMObject> create_with_size(size_t);
    static NonnullRefPtr<AnonymousVMObject> create_for_physical_range(PhysicalAddress, size_t);
    virtual NonnullRefPtr<VMObject> clone() override;

protected:
    explicit AnonymousVMObject(size_t);
    explicit AnonymousVMObject(const AnonymousVMObject&);

private:
    AnonymousVMObject(PhysicalAddress, size_t);

    AnonymousVMObject& operator=(const AnonymousVMObject&) = delete;
    AnonymousVMObject& operator=(AnonymousVMObject&&) = delete;
    AnonymousVMObject(AnonymousVMObject&&) = delete;

    virtual bool is_anonymous() const override { return true; }
};
