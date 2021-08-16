/* /nodynamiccopyright/ hard coded linenumbers - DO NOT CHANGE */
/*
 *
 * This test class has a long
 * header comment so that the
 * source line numbers will be
 * different when compared to:
 *
 *   RedefineSubTarg.java
 *
 *
 *
 *
 *
 *
 *
 *
 */
class RedefineSubTarg {
    String foo(String prev) {
        StringBuffer buf = new StringBuffer(prev);
        buf.append("Different ");
        return buf.toString();
    }
}
