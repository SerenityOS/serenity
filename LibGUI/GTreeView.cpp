#include <LibGUI/GTreeView.h>
#include <LibGUI/GPainter.h>

class TestModel : public GModel {
public:
    static Retained<TestModel> create() { return adopt(*new TestModel); }

    virtual int row_count(const GModelIndex& = GModelIndex()) const;
    virtual int column_count(const GModelIndex& = GModelIndex()) const;
    virtual GVariant data(const GModelIndex&, Role = Role::Display) const;
    virtual void update();
    virtual ColumnMetadata column_metadata(int) const { return { 100 }; }
};

int TestModel::row_count(const GModelIndex& index) const
{
    return 0;
}

int TestModel::column_count(const GModelIndex&) const
{
    return 1;
}

void TestModel::update()
{
}

GVariant TestModel::data(const GModelIndex&, Role) const
{
    return { };
}

GTreeView::GTreeView(GWidget* parent)
    : GAbstractView(parent)
{
    set_frame_shape(GFrame::Shape::Container);
    set_frame_shadow(GFrame::Shadow::Sunken);
    set_frame_thickness(2);

    set_model(TestModel::create());
}

GTreeView::~GTreeView()
{
}

void GTreeView::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);
    GPainter painter(*this);
    painter.set_clip_rect(frame_inner_rect());
    painter.set_clip_rect(event.rect());

    painter.fill_rect(event.rect(), Color::White);
}
