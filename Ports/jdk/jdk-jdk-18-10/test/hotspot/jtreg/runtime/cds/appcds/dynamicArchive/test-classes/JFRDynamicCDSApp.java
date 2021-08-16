/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */
import java.nio.file.Paths;
import jdk.jfr.Configuration;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordingStream;

public class JFRDynamicCDSApp {
    public static void main(String args[]) throws Exception {
        RecordingStream rs = new RecordingStream();
        rs.enable("JFRDynamicCDS.StressEvent");
        rs.startAsync();

        Recording recording = startRecording();
        loop();
        recording.stop();
        recording.close();

        rs.close();
    }

    static Recording startRecording() throws Exception {
        Configuration configuration = Configuration.getConfiguration("default");
        Recording recording = new Recording(configuration);

        recording.setName("internal");
        recording.enable(StressEvent.class);
        recording.setDestination(Paths.get("JFRDynamicCDS.jfr"));
        recording.start();
        return recording;
    }


    static void loop() {
        for (int i=0; i<100; i++) {
            StressEvent event = new StressEvent();
            event.iteration = i;
            event.description = "Stressful Event, take it easy!";
            event.customClazz = StressEvent.class;
            event.value = i;
            event.commit();
        }
    }


    /**
     * Internal StressEvent class.
     */
    @Label("Stress Event")
    @Description("A duration event with 4 entries")
    @Name("JFRDynamicCDS.StressEvent")
    public static class StressEvent extends jdk.jfr.Event {
        public Class<?> customClazz;
        public String description;
        public int iteration;
        public double value;
    }
}
