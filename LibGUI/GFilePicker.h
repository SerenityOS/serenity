#include <LibGUI/GDialog.h>

class GFilePicker final : public GDialog {
public:
    GFilePicker();
    virtual ~GFilePicker() override;

    virtual const char* class_name() const override { return "GFilePicker"; }

private:
};
