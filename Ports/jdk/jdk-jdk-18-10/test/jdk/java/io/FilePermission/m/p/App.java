package p;
import java.io.InputStream;
import java.io.FileInputStream;
public class App {
    public static void main(String[] args) throws Exception {
        boolean f = true;
        StringBuilder sb = new StringBuilder();
        String expected = null;
        for (String s: args) {
            if (expected == null) {
                expected = s;
            } else if (s.equals("-")) {
                f = false;
            } else if (f) {
                try (InputStream is = new FileInputStream(s)) {
                    is.readAllBytes();
                    sb.append('+');
                } catch (SecurityException se) {
                    System.out.println(se);
                    sb.append('S');
                } catch (Exception e) {
                    System.out.println(e);
                    sb.append('-');
                }
            } else {
                try (InputStream is = App.class.getResourceAsStream(s)) {
                    is.readAllBytes();
                    sb.append('+');
                } catch (NullPointerException npe) {
                    System.out.println(npe);
                    sb.append('0');
                } catch (Exception e) {
                    System.out.println(e);
                    sb.append('-');
                }
            }
        }
        if (!sb.toString().equals(expected)) {
            throw new Exception("Expected " + expected + ", actually " + sb);
        } else {
            System.out.println("OK");
        }
    }
}
