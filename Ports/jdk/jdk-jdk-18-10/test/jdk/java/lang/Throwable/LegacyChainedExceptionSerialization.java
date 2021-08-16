/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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
 */

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.PrivilegedActionException;
import java.util.Base64;
import java.util.Map;

/**
 * @test
 * @bug     4385429 8004928 8210721
 * @summary Certain legacy chained exceptions throw IllegalArgumentException
 *          upon deserialization if "causative exception" is null.
 * @author  Josh Bloch
 */
public class LegacyChainedExceptionSerialization {
    private static Throwable[] broken = {
        new ClassNotFoundException(),
        new ClassNotFoundException("bar", new IOException("reading class file")),
        new ExceptionInInitializerError(),
        new ExceptionInInitializerError(new NullPointerException("foo")),
        new java.lang.reflect.UndeclaredThrowableException(null),
        new java.lang.reflect.UndeclaredThrowableException(new IllegalArgumentException("foo")),
        new java.lang.reflect.InvocationTargetException(null),
        new java.lang.reflect.InvocationTargetException(new Error("goo")),
        new java.security.PrivilegedActionException(null),
        new java.security.PrivilegedActionException(new IOException("foo")),
    };


    public static void main(String[] args) throws Exception {
        for (int i=0; i<broken.length; i++)
            test(broken[i]);

        for (Map.Entry<String, Throwable> e : SERIALIZED_DATA.entrySet()) {
            Throwable t = deserialize(e.getKey());
            verify(t, e.getValue());
        }

        testOverriddenGetCause();
    }

    private static Throwable test(Throwable e) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(e);
        out.flush();

        ByteArrayInputStream bin =
            new ByteArrayInputStream(bout.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bin);
        Throwable clone = (Throwable) in.readObject();
        return clone;
    }

    private static void testOverriddenGetCause() throws Exception {
        SubClass sc = new SubClass(new NullPointerException());
        SubClass clone = (SubClass)test(sc);
        Throwable cause = clone.getException();
        if (!(cause instanceof NullPointerException) || cause.getMessage() != null) {
            throw new RuntimeException("unexpected cause: " + cause);
        }

    }

    private static Throwable deserialize(String ser) throws Exception {
        Base64.Decoder decoder = Base64.getDecoder();
        try (ByteArrayInputStream bin = new ByteArrayInputStream(decoder.decode(ser));
             ObjectInputStream ois = new ObjectInputStream(bin)) {
            return (Throwable)ois.readObject();
        }
    }

    /*
     * Verify the deserialization of the serialized data from an old version.
     * See SERIALIZED_DATA.
     */
    private static void verify(Throwable t, Throwable expected) {
        String msg = expected.getMessage();
        Throwable cause = expected.getCause();
        if (t.getMessage() != msg && msg != null && !msg.equals(t.getMessage())) {
            throw new RuntimeException("unexpected message: " + t.getMessage()
                    + " expected: " + msg);
        }
        Throwable e = t.getCause();
        if (e.getClass() != cause.getClass()) {
            throw new RuntimeException("unexpected cause: " + t.getCause());
        }
        String causedBy = cause.getMessage();
        if (e.getMessage() != causedBy) {
            if (e.getMessage() == null || causedBy == null || !causedBy.equals(e.getMessage())) {
                throw new RuntimeException("unexpected message: " + t.getMessage()
                    + " expected: " + causedBy);
            }
        }
        Throwable exception = null;
        if (t instanceof ExceptionInInitializerError) {
            exception = ((ExceptionInInitializerError)t).getException();
        } else if (t instanceof ClassNotFoundException) {
            exception = ((ClassNotFoundException)t).getException();
        } else if (t instanceof InvocationTargetException) {
            exception = ((InvocationTargetException) t).getTargetException();
        } else if (t instanceof UndeclaredThrowableException) {
            exception = ((UndeclaredThrowableException) t).getUndeclaredThrowable();
        } else if (t instanceof PrivilegedActionException) {
            exception = ((PrivilegedActionException) t).getException();
        } else {
            // skip the cause == exception check below
            e = null;
        }
        if (e != exception) {
            throw new RuntimeException("unexpected exception: " + exception);
        }
    }

    static class SubClass extends ExceptionInInitializerError {
        public SubClass(Throwable t) {
            super(t);
        }
        @Override
        public Throwable getCause() {
            return new Throwable("always new");
        }
    }

    /*
     * The following strings are base64-encoded serialized data generated
     * by running the jdk10SerializeThrowable method with JDK 10 runtime.
     *
     *   private static void jdk10SerializeThrowable(Throwable e) throws Exception {
     *       Base64.Encoder encoder = Base64.getEncoder();
     *       try (ByteArrayOutputStream os = new ByteArrayOutputStream();
     *            ObjectOutputStream out = new ObjectOutputStream(os)) {
     *           out.writeObject(e);
     *           out.flush();
     *           String s = encoder.encodeToString(os.toByteArray());
     *           for (int i=0; i < s.length();) {
     *               int end = Math.min(i+60, s.length());
     *               CharSequence seq = s.subSequence(i, end);
     *               System.out.format("\"%s\" +%n", seq);
     *               i = end;
     *           }
     *       }
     *   }
     */

    private static final String EIIE_OLD_VERSION =
        "rO0ABXNyACVqYXZhLmxhbmcuRXhjZXB0aW9uSW5Jbml0aWFsaXplckVycm9y" +
        "FR400Amhk4ACAAFMAAlleGNlcHRpb250ABVMamF2YS9sYW5nL1Rocm93YWJs" +
        "ZTt4cgAWamF2YS5sYW5nLkxpbmthZ2VFcnJvcjGtS1U0qEq6AgAAeHIAD2ph" +
        "dmEubGFuZy5FcnJvckUdNlaLgg5WAgAAeHIAE2phdmEubGFuZy5UaHJvd2Fi" +
        "bGXVxjUnOXe4ywMABEwABWNhdXNlcQB+AAFMAA1kZXRhaWxNZXNzYWdldAAS" +
        "TGphdmEvbGFuZy9TdHJpbmc7WwAKc3RhY2tUcmFjZXQAHltMamF2YS9sYW5n" +
        "L1N0YWNrVHJhY2VFbGVtZW50O0wAFHN1cHByZXNzZWRFeGNlcHRpb25zdAAQ" +
        "TGphdmEvdXRpbC9MaXN0O3hwcHB1cgAeW0xqYXZhLmxhbmcuU3RhY2tUcmFj" +
        "ZUVsZW1lbnQ7AkYqPDz9IjkCAAB4cAAAAAFzcgAbamF2YS5sYW5nLlN0YWNr" +
        "VHJhY2VFbGVtZW50YQnFmiY23YUCAAhCAAZmb3JtYXRJAApsaW5lTnVtYmVy" +
        "TAAPY2xhc3NMb2FkZXJOYW1lcQB+AAVMAA5kZWNsYXJpbmdDbGFzc3EAfgAF" +
        "TAAIZmlsZU5hbWVxAH4ABUwACm1ldGhvZE5hbWVxAH4ABUwACm1vZHVsZU5h" +
        "bWVxAH4ABUwADW1vZHVsZVZlcnNpb25xAH4ABXhwAQAAAAd0AANhcHB0AARU" +
        "ZXN0dAAJVGVzdC5qYXZhdAAEbWFpbnBwc3IAH2phdmEudXRpbC5Db2xsZWN0" +
        "aW9ucyRFbXB0eUxpc3R6uBe0PKee3gIAAHhweHNyAB5qYXZhLmxhbmcuTnVs" +
        "bFBvaW50ZXJFeGNlcHRpb25HpaGO/zHhuAIAAHhyABpqYXZhLmxhbmcuUnVu" +
        "dGltZUV4Y2VwdGlvbp5fBkcKNIPlAgAAeHIAE2phdmEubGFuZy5FeGNlcHRp" +
        "b27Q/R8+GjscxAIAAHhxAH4ABHEAfgAWdAADZm9vdXEAfgAJAAAAAXNxAH4A" +
        "CwEAAAAHcQB+AA1xAH4ADnEAfgAPcQB+ABBwcHEAfgASeA==";

    private static final String CNFE_OLD_VERSION =
        "rO0ABXNyACBqYXZhLmxhbmcuQ2xhc3NOb3RGb3VuZEV4Y2VwdGlvbn9azWY+" +
        "1CCOAgABTAACZXh0ABVMamF2YS9sYW5nL1Rocm93YWJsZTt4cgAmamF2YS5s" +
        "YW5nLlJlZmxlY3RpdmVPcGVyYXRpb25FeGNlcHRpb24AAAAAB1vNFQIAAHhy" +
        "ABNqYXZhLmxhbmcuRXhjZXB0aW9u0P0fPho7HMQCAAB4cgATamF2YS5sYW5n" +
        "LlRocm93YWJsZdXGNSc5d7jLAwAETAAFY2F1c2VxAH4AAUwADWRldGFpbE1l" +
        "c3NhZ2V0ABJMamF2YS9sYW5nL1N0cmluZztbAApzdGFja1RyYWNldAAeW0xq" +
        "YXZhL2xhbmcvU3RhY2tUcmFjZUVsZW1lbnQ7TAAUc3VwcHJlc3NlZEV4Y2Vw" +
        "dGlvbnN0ABBMamF2YS91dGlsL0xpc3Q7eHBwdAADYmFydXIAHltMamF2YS5s" +
        "YW5nLlN0YWNrVHJhY2VFbGVtZW50OwJGKjw8/SI5AgAAeHAAAAABc3IAG2ph" +
        "dmEubGFuZy5TdGFja1RyYWNlRWxlbWVudGEJxZomNt2FAgAIQgAGZm9ybWF0" +
        "SQAKbGluZU51bWJlckwAD2NsYXNzTG9hZGVyTmFtZXEAfgAFTAAOZGVjbGFy" +
        "aW5nQ2xhc3NxAH4ABUwACGZpbGVOYW1lcQB+AAVMAAptZXRob2ROYW1lcQB+" +
        "AAVMAAptb2R1bGVOYW1lcQB+AAVMAA1tb2R1bGVWZXJzaW9ucQB+AAV4cAEA" +
        "AAAMdAADYXBwdAAEVGVzdHQACVRlc3QuamF2YXQABG1haW5wcHNyAB9qYXZh" +
        "LnV0aWwuQ29sbGVjdGlvbnMkRW1wdHlMaXN0ergXtDynnt4CAAB4cHhzcgAT" +
        "amF2YS5pby5JT0V4Y2VwdGlvbmyAc2RlJfCrAgAAeHEAfgADcQB+ABV0ABJy" +
        "ZWFkaW5nIGNsYXNzIGZpbGV1cQB+AAoAAAABc3EAfgAMAQAAAAxxAH4ADnEA" +
        "fgAPcQB+ABBxAH4AEXBwcQB+ABN4";

    private static final String ITE1_OLD_VERSION =
        "rO0ABXNyACtqYXZhLmxhbmcucmVmbGVjdC5JbnZvY2F0aW9uVGFyZ2V0RXhj" +
        "ZXB0aW9uOLEmjtZxJG8CAAFMAAZ0YXJnZXR0ABVMamF2YS9sYW5nL1Rocm93" +
        "YWJsZTt4cgAmamF2YS5sYW5nLlJlZmxlY3RpdmVPcGVyYXRpb25FeGNlcHRp" +
        "b24AAAAAB1vNFQIAAHhyABNqYXZhLmxhbmcuRXhjZXB0aW9u0P0fPho7HMQC" +
        "AAB4cgATamF2YS5sYW5nLlRocm93YWJsZdXGNSc5d7jLAwAETAAFY2F1c2Vx" +
        "AH4AAUwADWRldGFpbE1lc3NhZ2V0ABJMamF2YS9sYW5nL1N0cmluZztbAApz" +
        "dGFja1RyYWNldAAeW0xqYXZhL2xhbmcvU3RhY2tUcmFjZUVsZW1lbnQ7TAAU" +
        "c3VwcHJlc3NlZEV4Y2VwdGlvbnN0ABBMamF2YS91dGlsL0xpc3Q7eHBwdAAD" +
        "YmFydXIAHltMamF2YS5sYW5nLlN0YWNrVHJhY2VFbGVtZW50OwJGKjw8/SI5" +
        "AgAAeHAAAAABc3IAG2phdmEubGFuZy5TdGFja1RyYWNlRWxlbWVudGEJxZom" +
        "Nt2FAgAIQgAGZm9ybWF0SQAKbGluZU51bWJlckwAD2NsYXNzTG9hZGVyTmFt" +
        "ZXEAfgAFTAAOZGVjbGFyaW5nQ2xhc3NxAH4ABUwACGZpbGVOYW1lcQB+AAVM" +
        "AAptZXRob2ROYW1lcQB+AAVMAAptb2R1bGVOYW1lcQB+AAVMAA1tb2R1bGVW" +
        "ZXJzaW9ucQB+AAV4cAEAAAARdAADYXBwdAAEVGVzdHQACVRlc3QuamF2YXQA" +
        "BG1haW5wcHNyAB9qYXZhLnV0aWwuQ29sbGVjdGlvbnMkRW1wdHlMaXN0ergX" +
        "tDynnt4CAAB4cHhzcgAPamF2YS5sYW5nLkVycm9yRR02VouCDlYCAAB4cQB+" +
        "AARxAH4AFXQAA2Zvb3VxAH4ACgAAAAFzcQB+AAwBAAAAEXEAfgAOcQB+AA9x" +
        "AH4AEHEAfgARcHBxAH4AE3g=";

    private static final String ITE2_OLD_VERSION =
        "rO0ABXNyACtqYXZhLmxhbmcucmVmbGVjdC5JbnZvY2F0aW9uVGFyZ2V0RXhj" +
        "ZXB0aW9uOLEmjtZxJG8CAAFMAAZ0YXJnZXR0ABVMamF2YS9sYW5nL1Rocm93" +
        "YWJsZTt4cgAmamF2YS5sYW5nLlJlZmxlY3RpdmVPcGVyYXRpb25FeGNlcHRp" +
        "b24AAAAAB1vNFQIAAHhyABNqYXZhLmxhbmcuRXhjZXB0aW9u0P0fPho7HMQC" +
        "AAB4cgATamF2YS5sYW5nLlRocm93YWJsZdXGNSc5d7jLAwAETAAFY2F1c2Vx" +
        "AH4AAUwADWRldGFpbE1lc3NhZ2V0ABJMamF2YS9sYW5nL1N0cmluZztbAApz" +
        "dGFja1RyYWNldAAeW0xqYXZhL2xhbmcvU3RhY2tUcmFjZUVsZW1lbnQ7TAAU" +
        "c3VwcHJlc3NlZEV4Y2VwdGlvbnN0ABBMamF2YS91dGlsL0xpc3Q7eHBwcHVy" +
        "AB5bTGphdmEubGFuZy5TdGFja1RyYWNlRWxlbWVudDsCRio8PP0iOQIAAHhw" +
        "AAAAAXNyABtqYXZhLmxhbmcuU3RhY2tUcmFjZUVsZW1lbnRhCcWaJjbdhQIA" +
        "CEIABmZvcm1hdEkACmxpbmVOdW1iZXJMAA9jbGFzc0xvYWRlck5hbWVxAH4A" +
        "BUwADmRlY2xhcmluZ0NsYXNzcQB+AAVMAAhmaWxlTmFtZXEAfgAFTAAKbWV0" +
        "aG9kTmFtZXEAfgAFTAAKbW9kdWxlTmFtZXEAfgAFTAANbW9kdWxlVmVyc2lv" +
        "bnEAfgAFeHABAAAAEnQAA2FwcHQABFRlc3R0AAlUZXN0LmphdmF0AARtYWlu" +
        "cHBzcgAfamF2YS51dGlsLkNvbGxlY3Rpb25zJEVtcHR5TGlzdHq4F7Q8p57e" +
        "AgAAeHB4c3IAD2phdmEubGFuZy5FcnJvckUdNlaLgg5WAgAAeHEAfgAEcQB+" +
        "ABR0AANnb291cQB+AAkAAAABc3EAfgALAQAAABJxAH4ADXEAfgAOcQB+AA9x" +
        "AH4AEHBwcQB+ABJ4";

    private static final String UTE1_OLD_VERSION =
        "rO0ABXNyAC5qYXZhLmxhbmcucmVmbGVjdC5VbmRlY2xhcmVkVGhyb3dhYmxl" +
        "RXhjZXB0aW9uBJTY3HP5/P8CAAFMABN1bmRlY2xhcmVkVGhyb3dhYmxldAAV" +
        "TGphdmEvbGFuZy9UaHJvd2FibGU7eHIAGmphdmEubGFuZy5SdW50aW1lRXhj" +
        "ZXB0aW9unl8GRwo0g+UCAAB4cgATamF2YS5sYW5nLkV4Y2VwdGlvbtD9Hz4a" +
        "OxzEAgAAeHIAE2phdmEubGFuZy5UaHJvd2FibGXVxjUnOXe4ywMABEwABWNh" +
        "dXNlcQB+AAFMAA1kZXRhaWxNZXNzYWdldAASTGphdmEvbGFuZy9TdHJpbmc7" +
        "WwAKc3RhY2tUcmFjZXQAHltMamF2YS9sYW5nL1N0YWNrVHJhY2VFbGVtZW50" +
        "O0wAFHN1cHByZXNzZWRFeGNlcHRpb25zdAAQTGphdmEvdXRpbC9MaXN0O3hw" +
        "cHQAA2JhcnVyAB5bTGphdmEubGFuZy5TdGFja1RyYWNlRWxlbWVudDsCRio8" +
        "PP0iOQIAAHhwAAAAAXNyABtqYXZhLmxhbmcuU3RhY2tUcmFjZUVsZW1lbnRh" +
        "CcWaJjbdhQIACEIABmZvcm1hdEkACmxpbmVOdW1iZXJMAA9jbGFzc0xvYWRl" +
        "ck5hbWVxAH4ABUwADmRlY2xhcmluZ0NsYXNzcQB+AAVMAAhmaWxlTmFtZXEA" +
        "fgAFTAAKbWV0aG9kTmFtZXEAfgAFTAAKbW9kdWxlTmFtZXEAfgAFTAANbW9k" +
        "dWxlVmVyc2lvbnEAfgAFeHABAAAAE3QAA2FwcHQABFRlc3R0AAlUZXN0Lmph" +
        "dmF0AARtYWlucHBzcgAfamF2YS51dGlsLkNvbGxlY3Rpb25zJEVtcHR5TGlz" +
        "dHq4F7Q8p57eAgAAeHB4c3IAImphdmEubGFuZy5JbGxlZ2FsQXJndW1lbnRF" +
        "eGNlcHRpb261iXPTfWaPvAIAAHhxAH4AAnEAfgAVdAADZm9vdXEAfgAKAAAA" +
        "AXNxAH4ADAEAAAATcQB+AA5xAH4AD3EAfgAQcQB+ABFwcHEAfgATeA==";

    private static final String UTE2_OLD_VERSION =
        "rO0ABXNyAC5qYXZhLmxhbmcucmVmbGVjdC5VbmRlY2xhcmVkVGhyb3dhYmxl" +
        "RXhjZXB0aW9uBJTY3HP5/P8CAAFMABN1bmRlY2xhcmVkVGhyb3dhYmxldAAV" +
        "TGphdmEvbGFuZy9UaHJvd2FibGU7eHIAGmphdmEubGFuZy5SdW50aW1lRXhj" +
        "ZXB0aW9unl8GRwo0g+UCAAB4cgATamF2YS5sYW5nLkV4Y2VwdGlvbtD9Hz4a" +
        "OxzEAgAAeHIAE2phdmEubGFuZy5UaHJvd2FibGXVxjUnOXe4ywMABEwABWNh" +
        "dXNlcQB+AAFMAA1kZXRhaWxNZXNzYWdldAASTGphdmEvbGFuZy9TdHJpbmc7" +
        "WwAKc3RhY2tUcmFjZXQAHltMamF2YS9sYW5nL1N0YWNrVHJhY2VFbGVtZW50" +
        "O0wAFHN1cHByZXNzZWRFeGNlcHRpb25zdAAQTGphdmEvdXRpbC9MaXN0O3hw" +
        "cHB1cgAeW0xqYXZhLmxhbmcuU3RhY2tUcmFjZUVsZW1lbnQ7AkYqPDz9IjkC" +
        "AAB4cAAAAAFzcgAbamF2YS5sYW5nLlN0YWNrVHJhY2VFbGVtZW50YQnFmiY2" +
        "3YUCAAhCAAZmb3JtYXRJAApsaW5lTnVtYmVyTAAPY2xhc3NMb2FkZXJOYW1l" +
        "cQB+AAVMAA5kZWNsYXJpbmdDbGFzc3EAfgAFTAAIZmlsZU5hbWVxAH4ABUwA" +
        "Cm1ldGhvZE5hbWVxAH4ABUwACm1vZHVsZU5hbWVxAH4ABUwADW1vZHVsZVZl" +
        "cnNpb25xAH4ABXhwAQAAABR0AANhcHB0AARUZXN0dAAJVGVzdC5qYXZhdAAE" +
        "bWFpbnBwc3IAH2phdmEudXRpbC5Db2xsZWN0aW9ucyRFbXB0eUxpc3R6uBe0" +
        "PKee3gIAAHhweHNyACJqYXZhLmxhbmcuSWxsZWdhbEFyZ3VtZW50RXhjZXB0" +
        "aW9utYlz031mj7wCAAB4cQB+AAJxAH4AFHQAA2dvb3VxAH4ACQAAAAFzcQB+" +
        "AAsBAAAAFHEAfgANcQB+AA5xAH4AD3EAfgAQcHBxAH4AEng=";

    private static final String PAE_OLD_VERSION =
        "rO0ABXNyACdqYXZhLnNlY3VyaXR5LlByaXZpbGVnZWRBY3Rpb25FeGNlcHRp" +
        "b25Bj1P2UhH1ugIAAUwACWV4Y2VwdGlvbnQAFUxqYXZhL2xhbmcvRXhjZXB0" +
        "aW9uO3hyABNqYXZhLmxhbmcuRXhjZXB0aW9u0P0fPho7HMQCAAB4cgATamF2" +
        "YS5sYW5nLlRocm93YWJsZdXGNSc5d7jLAwAETAAFY2F1c2V0ABVMamF2YS9s" +
        "YW5nL1Rocm93YWJsZTtMAA1kZXRhaWxNZXNzYWdldAASTGphdmEvbGFuZy9T" +
        "dHJpbmc7WwAKc3RhY2tUcmFjZXQAHltMamF2YS9sYW5nL1N0YWNrVHJhY2VF" +
        "bGVtZW50O0wAFHN1cHByZXNzZWRFeGNlcHRpb25zdAAQTGphdmEvdXRpbC9M" +
        "aXN0O3hwcHB1cgAeW0xqYXZhLmxhbmcuU3RhY2tUcmFjZUVsZW1lbnQ7AkYq" +
        "PDz9IjkCAAB4cAAAAAFzcgAbamF2YS5sYW5nLlN0YWNrVHJhY2VFbGVtZW50" +
        "YQnFmiY23YUCAAhCAAZmb3JtYXRJAApsaW5lTnVtYmVyTAAPY2xhc3NMb2Fk" +
        "ZXJOYW1lcQB+AAVMAA5kZWNsYXJpbmdDbGFzc3EAfgAFTAAIZmlsZU5hbWVx" +
        "AH4ABUwACm1ldGhvZE5hbWVxAH4ABUwACm1vZHVsZU5hbWVxAH4ABUwADW1v" +
        "ZHVsZVZlcnNpb25xAH4ABXhwAQAAABd0AANhcHB0AARUZXN0dAAJVGVzdC5q" +
        "YXZhdAAEbWFpbnBwc3IAH2phdmEudXRpbC5Db2xsZWN0aW9ucyRFbXB0eUxp" +
        "c3R6uBe0PKee3gIAAHhweHNyABNqYXZhLmlvLklPRXhjZXB0aW9ubIBzZGUl" +
        "8KsCAAB4cQB+AAJxAH4AFHQAA2Zvb3VxAH4ACQAAAAFzcQB+AAsBAAAAF3EA" +
        "fgANcQB+AA5xAH4AD3EAfgAQcHBxAH4AEng=";

    private static Map<String, Throwable> SERIALIZED_DATA = Map.of(
        EIIE_OLD_VERSION, new ExceptionInInitializerError(new NullPointerException("foo")),
        CNFE_OLD_VERSION, new ClassNotFoundException("bar", new IOException("reading class file")),
        ITE1_OLD_VERSION, new InvocationTargetException(new Error("foo"), "bar"),
        ITE2_OLD_VERSION, new InvocationTargetException(new Error("goo")),
        UTE1_OLD_VERSION, new UndeclaredThrowableException(new IllegalArgumentException("foo"), "bar"),
        UTE2_OLD_VERSION, new UndeclaredThrowableException(new IllegalArgumentException("goo")),
        PAE_OLD_VERSION,  new PrivilegedActionException(new IOException("foo"))
    );
}
