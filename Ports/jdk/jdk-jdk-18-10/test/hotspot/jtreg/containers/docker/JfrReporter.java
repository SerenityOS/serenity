/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 */
import java.nio.file.Path;
import java.nio.file.Paths;
import jdk.jfr.Recording;
import jdk.jfr.EventSettings;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

// This class is intended to run inside a container
public class JfrReporter {
    public static void main(String[] args) throws Exception {
        String eventName = args[0];
        try(Recording r = new Recording()) {
            EventSettings es = r.enable(eventName);
            for (int i = 1; i < args.length; i++) {
                String[] kv = args[i].split("=");
                es = es.with(kv[0], kv[1]);
            }
            r.start();
            r.stop();
            Path p = Paths.get("/", "tmp", eventName + ".jfr");
            r.dump(p);
            for (RecordedEvent e : RecordingFile.readAllEvents(p)) {
                System.out.println("===== EventType: " + e.getEventType().getName());
                for (ValueDescriptor v : e.getEventType().getFields()) {
                    System.out.println(v.getName() + " = " + e.getValue(v.getName()));
                }
            }
        }
    }
}
  