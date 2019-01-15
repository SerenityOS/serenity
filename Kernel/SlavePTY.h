#pragma once

#include "TTY.h"

class MasterPTY;

class SlavePTY final : public TTY {
public:
    explicit SlavePTY(unsigned index);
    virtual ~SlavePTY() override;
    void set_master(MasterPTY& master) { m_master = &master; }

    virtual String tty_name() const override;

    void on_master_write(const byte*, size_t);

protected:
    virtual void on_tty_write(const byte*, size_t) override;
    virtual bool can_write(Process&) const override;

private:
    unsigned m_index;
    MasterPTY* m_master { nullptr };
};

