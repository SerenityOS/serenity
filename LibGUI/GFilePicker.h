#include <LibGUI/GWindow.h>

class GFilePicker final : public GWindow {
public:
    GFilePicker();
    virtual ~GFilePicker() override;

private:
    virtual const char* class_name() const override { return "GFilePicker"; }
};
