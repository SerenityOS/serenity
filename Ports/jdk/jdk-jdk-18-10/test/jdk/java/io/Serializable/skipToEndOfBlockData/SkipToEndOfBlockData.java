/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4228592
 * @summary Ensure that ObjectInputStream properly skips over block data when a
 *          class that defines readObject() or readExternal() fails to read all
 *          of the data written by the corresponding writeObject() or
 *          writeExternal() method.
 */

import java.io.*;

class MismatchedRead implements Serializable {
    private static final long serialVersionUID = 1L;

    int i;
    float f;

    MismatchedRead(int i, float f) {
        this.i = i;
        this.f = f;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(i);
        out.writeFloat(f);
        out.writeUTF("skip me");
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        i = in.readInt();
        f = in.readFloat();
    }

    public boolean equals(Object obj) {
        if (! (obj instanceof MismatchedRead))
            return false;
        MismatchedRead other = (MismatchedRead) obj;
        return (i == other.i && f == other.f);
    }

    public int hashCode() {
        return i;
    }
}

class MismatchedReadExternal implements Externalizable {
    private static final long serialVersionUID = 1L;

    int i;
    float f;

    public MismatchedReadExternal() {
        this(0, (float) 0.0);
    }

    MismatchedReadExternal(int i, float f) {
        this.i = i;
        this.f = f;
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(i);
        out.writeFloat(f);
        out.writeUTF("skip another");
    }

    public void readExternal(ObjectInput in) throws IOException {
        i = in.readInt();
        f = in.readFloat();
    }

    public boolean equals(Object obj) {
        if (! (obj instanceof MismatchedReadExternal))
            return false;
        MismatchedReadExternal other = (MismatchedReadExternal) obj;
        return (i == other.i && f == other.f);
    }

    public int hashCode() {
        return i;
    }
}

class InnocentBystander implements Serializable {
    private static final long serialVersionUID = 1L;

    String s;

    InnocentBystander(String s) {
        this.s = s;
    }

    public boolean equals(Object obj) {
        if (! (obj instanceof InnocentBystander))
            return false;
        InnocentBystander other = (InnocentBystander) obj;
        if (s != null)
            return s.equals(other.s);
        return (s == other.s);
    }

    public int hashCode() {
        return s.hashCode();
    }
}

public class SkipToEndOfBlockData {
    public static void main(String[] args) throws Exception {
        ObjectOutputStream oout;
        ObjectInputStream oin;
        ByteArrayOutputStream bout;
        ByteArrayInputStream bin;
        MismatchedRead mr, mrcopy;
        MismatchedReadExternal mre, mrecopy;
        InnocentBystander ib, ibcopy;

        bout = new ByteArrayOutputStream();
        oout = new ObjectOutputStream(bout);
        mr = new MismatchedRead(1, (float) 2.34);
        mre = new MismatchedReadExternal(5, (float) 6.78);
        ib = new InnocentBystander("foo");

        oout.writeObject(mr);
        oout.writeObject(mre);
        oout.writeObject(ib);
        oout.flush();

        bin = new ByteArrayInputStream(bout.toByteArray());
        oin = new ObjectInputStream(bin);
        mrcopy = (MismatchedRead) oin.readObject();
        mrecopy = (MismatchedReadExternal) oin.readObject();
        ibcopy = (InnocentBystander) oin.readObject();

        if (! (mr.equals(mrcopy) && mre.equals(mrecopy) && ib.equals(ibcopy)))
            throw new Error("copy not equal to original");
    }
}
