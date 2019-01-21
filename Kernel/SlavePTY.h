#pragma once

#include "TTY.h"

class MasterPTY;

class SlavePTY final : public TTY {
public:
    virtual ~SlavePTY() override;

    void on_master_write(const byte*, size_t);
    unsigned index() const { return m_index; }

private:
    // ^TTY
    virtual String tty_name() const override;
    virtual void on_tty_write(const byte*, size_t) override;

    // ^CharacterDevice
    virtual bool can_write(Process&) const override;
    virtual const char* class_name() const override { return "SlavePTY"; }

    friend class MasterPTY;
    SlavePTY(MasterPTY&, unsigned index);

    MasterPTY& m_master;
    unsigned m_index;
};

