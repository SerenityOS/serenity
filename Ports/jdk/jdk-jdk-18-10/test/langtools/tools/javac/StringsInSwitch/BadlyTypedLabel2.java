/*
 * @test  /nodynamiccopyright/
 * @bug 6827009
 * @summary Check for case lables of different types.
 * @compile/fail/ref=BadlyTypedLabel2.out -XDrawDiagnostics BadlyTypedLabel2.java
 */
import static java.math.RoundingMode.*;

class BadlyTypedLabel2 {
    String m(String s) {
        switch(s) {
        case "Oh what a feeling...":
            return(s);
        case CEILING:
            return ("... switching on the ceiling!");
        }
    }
}
