/*
 * @test /nodynamiccopyright/
 * @bug 8010303 8062373
 * @summary Graph inference: missing incorporation step causes spurious inference error
 * @compile/fail/ref=TargetType68.out -XDrawDiagnostics TargetType68.java
 */
import java.util.*;

class TargetType68 {

    //derived from FX 2.2 API
    static class XYChart<X,Y> {
        static final class Series<X,Y> {
            Series(java.lang.String name, ObservableList<XYChart.Data<X,Y>> data) { }
        }

        static final class Data<X,Y> { }

        ObservableList<XYChart.Series<X,Y>> getData() { return null; }
    }

    //derived from FX 2.2 API
    interface ObservableList<X> extends List<X> {
        boolean setAll(Collection<? extends X> col);
    }

    //derived from FX 2.2 API
    static class FXCollections {
        static <E> ObservableList<E> observableList(List<E> l) { return null; }
    }

    private void testMethod() {
            XYChart<Number, Number> numberChart = null;
            List<XYChart.Data<Number, Number>> data_1 = new ArrayList<>();
            List<XYChart.Data<Number, Number>> data_2 = new ArrayList<>();
            numberChart.getData().setAll(
                    Arrays.asList(new XYChart.Series<>("Data", FXCollections.observableList(data_1)),
                    new XYChart.Series<>("Data", FXCollections.observableList(data_2)) {}));
    }
}
