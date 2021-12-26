#pragma once

#include <LibGUI/GEventLoop.h>
#include <LibGUI/GWindow.h>

class GDialog : public GWindow {
    C_OBJECT(GDialog)
public:
    enum ExecResult {
        ExecOK = 0,
        ExecCancel = 1,
        ExecAborted = 2
    };

    virtual ~GDialog() override;

    int exec();

    int result() const { return m_result; }
    void done(int result);

protected:
    explicit GDialog(CObject* parent);

private:
    OwnPtr<GEventLoop> m_event_loop;
    int m_result { ExecAborted };
};
