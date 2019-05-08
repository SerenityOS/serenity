#include <LibGUI/GDialog.h>
#include <LibGUI/GTableView.h>

class GDirectoryModel;

class GFilePicker final : public GDialog {
public:
    GFilePicker(const String& path = "/", CObject* parent = nullptr);
    virtual ~GFilePicker() override;

    virtual const char* class_name() const override { return "GFilePicker"; }

private:
    GDirectoryModel& model() { return *m_model; }

    GTableView* m_view { nullptr };
    Retained<GDirectoryModel> m_model;
};
