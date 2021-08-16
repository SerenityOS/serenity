/*
 * Copyright (c) 2001, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4439631
 * @bug 4448721
 * @bug 4448603
 * @summary Test access to ranges within ArrayReferences
 * @author Robert Field
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g ArrayRangeTest.java
 * @run driver ArrayRangeTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class ArrayRangeTarg {
    static int[] emptyArray = {};
    static int[] fullArray = {0, 100, 200, 300, 400};

    public static void main(String[] args) {
        System.out.println("Goodbye from ArrayRangeTarg!");
    }
}

    /********** test program **********/

public class ArrayRangeTest extends TestScaffold {
    ReferenceType targetClass;

    class Sample {
        Sample(String name, ArrayReference arrRef, int[] expected) {
            this.name = name;
            this.arrRef = arrRef;
            this.expected = expected;
        }
        String name;
        ArrayReference arrRef;
        int[] expected;
    }

    ArrayRangeTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new ArrayRangeTest(args).startTests();
    }

    /********** test assist **********/

    String arr(int a[]) {
        StringBuffer buf = new StringBuffer();
        buf.append('[');
        if (a.length > 0) {
            buf.append(a[0]);
            for (int i = 1; i < a.length; ++i) {
                buf.append(',');
                buf.append(a[i]);
            }
        }
        buf.append(']');
        return buf.toString();
    }

    void getValueGood(Sample samp, int index) {
        try {
            Value val = samp.arrRef.getValue(index);
            int ival = ((IntegerValue)val).value();
            if (ival != samp.expected[index]) {
                failure("FAIL - " + samp.name +
                        ".getValue(" + index + ") - wrong value=" + ival);
            } else {
                println("pass - " + samp.name +
                        ".getValue(" + index + ") - value=" + ival);
            }
        } catch (Throwable exc) {
            failure("FAIL - " + samp.name +
                    ".getValue(" + index + ") - unexpected: " + exc);
        }
    }

    void getValueBad(Sample samp, int index) {
        try {
            Value val = samp.arrRef.getValue(index);
            failure("FAIL - " + samp.name +
                    ".getValue(" + index + ") - no expected exception");
        } catch (IndexOutOfBoundsException exc) {
            println("pass - " + samp.name +
                    ".getValue(" + index + ") - got expected: " + exc);
        } catch (Throwable exc) {
            failure("FAIL - " + samp.name +
                    ".getValue(" + index + ") - unexpected: " + exc);
        }
    }

    void getValuesGood(Sample samp) {
        String desc = samp.name + ".getValues()";
        try {
            List vals = samp.arrRef.getValues();
            if (vals.size() != samp.expected.length) {
                failure("FAIL - " + desc +
                        " - wrong size=" + vals.size() +
                        " , expected: " + samp.expected.length);
            }
            for (int index = 0; index < vals.size(); ++index) {
                int ival = ((IntegerValue)vals.get(index)).value();
                if (ival != samp.expected[index]) {
                    failure("FAIL - " + desc +
                            " - wrong value=" + ival);
                    return;
                }
            }
            println("pass - " + samp.name + ".getValues())");
        } catch (Throwable exc) {
            failure("FAIL - " + desc  +
                    " - unexpected: " + exc);
        }
    }

    void getValuesGood(Sample samp, int index, int length) {
        try {
            List vals = samp.arrRef.getValues(index, length);
            if (vals.size() !=
                 ((length==-1)? (samp.expected.length - index) : length)) {
                failure("FAIL - " + samp.name + ".getValues(" +
                        index + ", " + length + ") - wrong size=" +
                        vals.size());
            }
            for (int i = 0; i < vals.size(); ++i) {
                int ival = ((IntegerValue)vals.get(i)).value();
                if (ival != samp.expected[index + i]) {
                    failure("FAIL - " + samp.name + ".getValues(" +
                            index + ", " + length + ") - wrong value=" +
                            ival);
                    return;
                }
            }
            println("pass - " + samp.name + ".getValues(" +
                    index + ", " + length + "))");
        } catch (Throwable exc) {
            failure("FAIL - " + samp.name + ".getValues(" +
                    index + ", " + length + ") - unexpected: " + exc);
        }
    }

    void getValuesBad(Sample samp, int index, int length) {
        try {
            List vals = samp.arrRef.getValues(index, length);
            failure("FAIL - " + samp.name + ".getValues(" +
                        index + ", " + length + ") - no expected exception");
        } catch (IndexOutOfBoundsException exc) {
            println("pass - " + samp.name + ".getValue(" +
                    index + ", " + length + ") - got expected: " + exc);
        } catch (Throwable exc) {
            failure("FAIL - " + samp.name + ".getValues(" +
                    index + ", " + length + ") - unexpected: " + exc);
        }
    }

    void setValueGood(Sample samp, int index, int ival) {
        try {
            Value val = vm().mirrorOf(ival);
            samp.arrRef.setValue(index, val);
            println("pass - " + samp.name +
                    ".setValue(" + index + ", ..)");
        } catch (Throwable exc) {
            failure("FAIL - " + samp.name +
                    ".setValue(" + index + ",...) - unexpected: " + exc);
        }
    }

    void setValueBad(Sample samp, int index, int ival) {
        try {
            Value val = vm().mirrorOf(ival);
            samp.arrRef.setValue(index, val);
            failure("FAIL - " + samp.name +
                    ".setValue(" + index + ", ..) - no expected exception");
        } catch (IndexOutOfBoundsException exc) {
            println("pass - " + samp.name +
                    ".setValue(" + index + ",...) - got expected: " + exc);
        } catch (Throwable exc) {
            failure("FAIL - " + samp.name +
                    ".setValue(" + index + ",...) - unexpected: " + exc);
        }
    }

    void setValuesGood(Sample samp, int[] valArray) {
        String desc = samp.name + ".setValues(" + arr(valArray) + ")";
        try {
            List values = new ArrayList();
            for (int i = 0; i < valArray.length; ++i) {
                Value val = vm().mirrorOf(valArray[i]);
                values.add(val);
            }
            samp.arrRef.setValues(values);
            println("pass - " + desc);
        } catch (Throwable exc) {
            failure("FAIL - " + desc + " - unexpected: " + exc);
        }
    }

    void setValuesGood(Sample samp, int index, int[] valArray,
                       int srcInx, int length) {
        String desc = samp.name + ".setValues(" + index + ", " +
            arr(valArray) + ", " + srcInx + ", " + length + ")";
        try {
            List values = new ArrayList();
            for (int i = 0; i < valArray.length; ++i) {
                Value val = vm().mirrorOf(valArray[i]);
                values.add(val);
            }
            samp.arrRef.setValues(index, values, srcInx, length);
            println("pass - " + desc);
        } catch (Throwable exc) {
            failure("FAIL - " + desc + " - unexpected: " + exc);
        }
    }

    void setValuesBad(Sample samp, int index, int[] valArray,
                       int srcInx, int length) {
        String desc = samp.name + ".setValues(" + index + ", " +
            arr(valArray) + ", " + srcInx + ", " + length + ")";
        try {
            List values = new ArrayList();
            for (int i = 0; i < valArray.length; ++i) {
                Value val = vm().mirrorOf(valArray[i]);
                values.add(val);
            }
            samp.arrRef.setValues(index, values, srcInx, length);
            failure("FAIL - " + desc + " - no expected exception");
        } catch (IndexOutOfBoundsException exc) {
            println("pass - " + desc + " - got expected: " + exc);
        } catch (Throwable exc) {
            failure("FAIL - " + desc + " - unexpected: " + exc);
        }
    }

    void check(Sample samp, int[] expectArray) {
        String desc = samp.name + " - check - " + arr(expectArray);

        try {
            List vals = samp.arrRef.getValues();
            if (vals.size() != expectArray.length) {
                failure("FAIL - " + desc +
                        " - wrong size=" + vals.size() +
                        " , expected: " + expectArray.length);
            }
            for (int index = 0; index < vals.size(); ++index) {
                int ival = ((IntegerValue)vals.get(index)).value();
                if (ival != expectArray[index]) {
                    failure("FAIL - " + desc +
                            " - wrong value=" + ival);
                    return;
                }
            }
            println("pass - " + desc);
        } catch (Throwable exc) {
            failure("FAIL - " + desc  +
                    " - unexpected: " + exc);
        }
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main() to determine targetClass
         */
        BreakpointEvent bpe = startToMain("ArrayRangeTarg");
        targetClass = bpe.location().declaringType();
        Field fullField = targetClass.fieldByName("fullArray");
        Field emptyField = targetClass.fieldByName("emptyArray");
        ArrayReference emptyAR = (ArrayReference)targetClass.getValue(emptyField);
        ArrayReference fullAR = (ArrayReference)targetClass.getValue(fullField);
        Sample full = new Sample("full", fullAR, ArrayRangeTarg.fullArray);
        Sample empty = new Sample("empty", emptyAR, ArrayRangeTarg.emptyArray);

        getValueGood(full, 0);
        getValueGood(full, 4);

        // index < 0
        getValueBad(full, -1);
        getValueBad(full, -2);
        getValueBad(empty, -1);
        getValueBad(empty, -2);

        // index >= length
        getValueBad(full, 5);
        getValueBad(empty, 0);
        getValueBad(empty, 5);

        getValuesGood(full);
        getValuesGood(empty);

        getValuesGood(full, 0, 5);
        getValuesGood(full, 0, 4);
        getValuesGood(full, 1, 4);
        getValuesGood(full, 5, 0);
        getValuesGood(full, 0, 0);
        getValuesGood(full, 0, -1);
        getValuesGood(full, 1, -1);
        getValuesGood(full, 5, -1);

        getValuesGood(empty, 0, 0);
        getValuesGood(empty, 0, -1);

        // index < 0
        getValuesBad(full, -1, 0);
        getValuesBad(full, -1, 3);
        getValuesBad(full, -1, -1);
        getValuesBad(empty, -1, 0);
        getValuesBad(full, -2, 0);
        getValuesBad(full, -2, 3);
        getValuesBad(full, -2, -1);
        getValuesBad(empty, -2, 0);

        // index > length()
        getValuesBad(full, 6, 0);
        getValuesBad(full, 6, -1);
        getValuesBad(empty, 1, 0);
        getValuesBad(empty, 1, -1);

        // length < 0
        getValuesBad(full, 0, -2);
        getValuesBad(empty, 0, -2);

        // index + length > length()
        getValuesBad(full, 0, 6);
        getValuesBad(full, 1, 5);
        getValuesBad(full, 2, 4);
        getValuesBad(full, 5, 1);
        getValuesBad(empty, 0, 1);

        setValueGood(full, 0, 55);
        setValueGood(full, 4, 66);

        // index < 0
        setValueBad(full, -1, 77);
        setValueBad(full, -2, 77);

        // index > length()
        setValueBad(full, 5, 77);
        setValueBad(full, 6, 77);

        check(full, new int[] {55, 100, 200, 300, 66});

        // index < 0
        setValueBad(empty, -1, 77);
        setValueBad(empty, -2, 77);

        // index > length()
        setValueBad(empty, 0, 77);
        setValueBad(empty, 1, 77);

        setValuesGood(full, new int[] {40, 41, 42});
        setValuesGood(full, new int[] {});

        check(full, new int[] {40, 41, 42, 300, 66});

        setValuesGood(full, new int[] {99, 51, 52, 53, 54, 55});
        setValuesGood(full, new int[] {50});

        check(full, new int[] {50, 51, 52, 53, 54});

        setValuesGood(empty, new int[] {});
        setValuesGood(empty, new int[] {88});

        setValuesGood(full, 2, new int[] {30, 31, 32, 33, 34, 35}, 0, 3);
        setValuesGood(full, 0, new int[] {80}, 0, 1);

        check(full, new int[] {80, 51, 30, 31, 32});

        setValuesGood(full, 0, new int[] {90, 91, 92, 93, 94, 95}, 3, 3);
        setValuesGood(full, 4, new int[] {81}, 0, 1);

        check(full, new int[] {93, 94, 95, 31, 81});

        setValuesGood(full, 3, new int[] {60, 61, 62, 63}, 0, -1);
        setValuesGood(full, 0, new int[] {82}, 0, -1);

        check(full, new int[] {82, 94, 95, 60, 61});

        setValuesGood(full, 3, new int[] {20, 21, 22, 23}, 1, -1);
        setValuesGood(full, 1, new int[] {83, 84}, 1, -1);
        setValuesGood(full, 1, new int[] {}, 0, -1);
        setValuesGood(full, 2, new int[] {}, 0, 0);
        setValuesGood(full, 3, new int[] {99}, 0, 0);
        setValuesGood(full, 4, new int[] {99, 98}, 1, 0);

        check(full, new int[] {82, 84, 95, 21, 22});

        setValuesGood(empty, 0, new int[] {}, 0, -1);
        setValuesGood(empty, 0, new int[] {}, 0, 0);
        setValuesGood(empty, 0, new int[] {99}, 0, 0);
        setValuesGood(empty, 0, new int[] {99, 98}, 1, 0);

        // index < 0
        setValuesBad(full, -1, new int[] {30, 31, 32, 33, 34, 35}, 0, 0);
        setValuesBad(full, -1, new int[] {30, 31, 32, 33, 34, 35}, 0, -1);
        setValuesBad(full, -2, new int[] {30, 31, 32, 33, 34, 35}, 0, -1);
        setValuesBad(empty, -1, new int[] {}, 0, 0);
        setValuesBad(empty, -2, new int[] {}, 0, 0);

        // index > length()
        setValuesBad(full, 6, new int[] {30, 31, 32, 33, 34, 35}, 0, 1);
        setValuesBad(full, 6, new int[] {30, 31, 32, 33, 34, 35}, 0, -1);
        setValuesBad(empty, 1, new int[] {4}, 0, 0);
        setValuesBad(empty, 1, new int[] {}, 0, 0);
        setValuesBad(empty, 1, new int[] {}, 0, -1);

        // srcIndex < 0
        setValuesBad(full, 0, new int[] {90, 91, 92, 93, 94, 95}, -1, 3);
        setValuesBad(full, 0, new int[] {90, 91, 92, 93, 94, 95}, -1, 0);
        setValuesBad(full, 0, new int[] {90, 91, 92, 93, 94, 95}, -1, -1);
        setValuesBad(full, 0, new int[] {90, 91, 92, 93, 94, 95}, -2, -1);
        setValuesBad(full, 1, new int[] {}, -1, -1);
        setValuesBad(full, 2, new int[] {}, -1, 0);
        setValuesBad(empty, 0, new int[] {}, -1, 0);

        // srcIndex > values.size()
        setValuesBad(full, 0, new int[] {81}, 2, 0);
        setValuesBad(full, 0, new int[] {81}, 2, 1);
        setValuesBad(full, 0, new int[] {81}, 2, -1);
        setValuesBad(full, 4, new int[] {}, 1, 0);
        setValuesBad(full, 1, new int[] {}, 1, -1);
        setValuesBad(full, 2, new int[] {}, 1, 0);
        setValuesBad(empty, 0, new int[] {}, 1, 0);
        setValuesBad(empty, 0, new int[] {5}, 2, 0);

        // length < 0 (length != -1)
        setValuesBad(full, 3, new int[] {60, 61, 62, 63}, 0, -2);
        setValuesBad(full, 3, new int[] {}, 0, -2);

        // index + length > length()
        setValuesBad(full, 0, new int[] {20, 21, 22, 23, 24, 25, 26}, 0, 6);
        setValuesBad(full, 1, new int[] {20, 21, 22, 23, 24, 25, 26}, 0, 5);
        setValuesBad(full, 2, new int[] {20, 21, 22, 23, 24, 25, 26}, 0, 4);
        setValuesBad(full, 3, new int[] {20, 21, 22, 23, 24, 25, 26}, 0, 3);
        setValuesBad(full, 4, new int[] {20, 21, 22, 23, 24, 25, 26}, 0, 2);
        setValuesBad(full, 5, new int[] {20, 21, 22, 23, 24, 25, 26}, 0, 1);
        setValuesBad(full, 6, new int[] {20, 21, 22, 23, 24, 25, 26}, 0, 0);
        setValuesBad(full, 2, new int[] {20, 21, 22, 23, 24, 25, 26}, 1, 4);
        setValuesBad(full, 3, new int[] {20, 21, 22, 23, 24, 25, 26}, 1, 3);
        setValuesBad(full, 4, new int[] {20, 21, 22, 23, 24, 25, 26}, 2, 2);
        setValuesBad(full, 5, new int[] {20, 21, 22, 23, 24, 25, 26}, 3, 1);
        setValuesBad(full, 6, new int[] {20, 21, 22, 23, 24, 25, 26}, 4, 0);
        setValuesBad(empty, 0, new int[] {6}, 0, 1);

        // srcIndex + length > values.size()
        setValuesBad(full, 0, new int[] {82}, 0, 2);
        setValuesBad(full, 0, new int[] {82}, 1, 1);
        setValuesBad(full, 0, new int[] {82}, 2, 0);
        setValuesBad(full, 0, new int[] {20, 21, 22}, 0, 4);
        setValuesBad(full, 0, new int[] {20, 21, 22}, 1, 3);
        setValuesBad(full, 0, new int[] {20, 21, 22}, 2, 2);
        setValuesBad(full, 0, new int[] {20, 21, 22}, 3, 1);
        setValuesBad(full, 0, new int[] {20, 21, 22}, 4, 0);

        check(full, new int[] {82, 84, 95, 21, 22});

        /*
         * resume the target until end
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("ArrayRangeTest: passed");
        } else {
            throw new Exception("ArrayRangeTest: failed");
        }
    }
}
