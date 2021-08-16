package jdk.jfr.api.recorder;

import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

import jdk.jfr.FlightRecorder;
import jdk.jfr.FlightRecorderListener;
import jdk.jfr.Recording;
/**
 * @test TestRecorderListenerWithDump
 *
 * @key jfr
 * @requires vm.hasJFR
 * @run main/othervm jdk.jfr.api.recorder.TestRecorderListenerWithDump
 */
public class TestRecorderListenerWithDump {

    public static void main(String... args) throws Exception {
        AtomicBoolean nullRecording = new AtomicBoolean();
        FlightRecorder.addListener(new FlightRecorderListener() {
            public void recordingStateChanged(Recording r) {
                if (r == null) {
                    nullRecording.set(true);
                } else {
                    System.out.println("Recording " + r.getName() + " " + r.getState());
                }
            }
        });
        try (Recording r = new Recording()) {
            r.start();
            r.dump(Paths.get("dump.jfr"));
        }
        if (nullRecording.get()) {
            throw new Exception("FlightRecorderListener returned null recording");
        }
    }
}
