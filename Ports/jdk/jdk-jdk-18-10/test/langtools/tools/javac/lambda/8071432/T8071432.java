/*
 * @test /nodynamiccopyright/
 * @bug 8071432 8069545
 * @summary structural most specific and stuckness
 * @compile/fail/ref=T8071432.out -XDrawDiagnostics T8071432.java
 */

import java.util.Arrays;
import java.util.Collection;

class T8071432 {

    static class Point {

        private double x, y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double distance(Point p) {
            return Math.sqrt((this.x - p.x) * (this.x - p.x) +
                             (this.y - p.y) * (this.y - p.y));
        }

        public double distance() {
            return Math.sqrt(this.x * this.x + this.y * this.y);
        }

        public String toString() {
            return "(" + x + ":" + y + ")";
        }
    }

    public static void main(String[] args) {
        Collection<Point> c = Arrays.asList(new Point(1.0, 0.1));
        System.out.println("------- 1 ---------------");
        System.out.println(c.stream().reduce(0.0,
                                            (s, p) -> s += p.distance(), (d1, d2) -> 0));
    }
}
