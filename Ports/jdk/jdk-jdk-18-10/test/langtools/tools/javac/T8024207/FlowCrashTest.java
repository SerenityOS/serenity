/*
 * @test  /nodynamiccopyright/
 * @bug 8024207
 * @summary javac crash in Flow$AssignAnalyzer.visitIdent
 * @compile/fail/ref=FlowCrashTest.out -XDrawDiagnostics FlowCrashTest.java
 */

import java.util.*;
import java.util.stream.*;

public class FlowCrashTest {
    static class ViewId { }

    public void crash() {

        Map<ViewId,String> viewToProfile = null;
        new TreeMap<>(viewToProfile.entrySet().stream()
                      .collect(Collectors.toMap((vid, prn) -> prn,
                                                (vid, prn) -> Arrays.asList(vid),
                                                (a, b) -> { a.addAll(b); return a; })));

    }
}
