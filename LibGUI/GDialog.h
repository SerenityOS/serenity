#pragma once

#include <LibGUI/GWindow.h>

class GDialog : public GWindow {
public:
    virtual ~GDialog() override;

    int exec();

    int result() const { return m_result; }
    void done(int result);

protected:
    explicit GDialog(GObject* parent);

private:
    int m_result { 0 };
};
