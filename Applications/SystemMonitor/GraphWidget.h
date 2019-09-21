#include <AK/CircularQueue.h>
#include <LibGUI/GFrame.h>

class GraphWidget final : public GFrame {
    C_OBJECT(GraphWidget)
public:
    virtual ~GraphWidget() override;

    void set_max(int max) { m_max = max; }
    void add_value(int);

    void set_graph_color(Color color) { m_graph_color = color; }
    void set_text_color(Color color) { m_text_color = color; }

    Function<String(int value, int max)> text_formatter;

private:
    explicit GraphWidget(GWidget* parent);

    virtual void paint_event(GPaintEvent&) override;

    int m_max { 100 };
    CircularQueue<int, 4000> m_values;
    Color m_graph_color;
    Color m_text_color;
};
