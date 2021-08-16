/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8149028
 * @author  a.stepanov
 * @summary some simple checks for TIFFDirectory
 * @run     main TIFFDirectoryTest
 */

import java.util.List;
import java.util.ArrayList;
import javax.imageio.metadata.*;
import javax.imageio.plugins.tiff.*;


public class TIFFDirectoryTest {

    private static void check(boolean ok, String msg) {
        if (!ok) { throw new RuntimeException(msg); }
    }

    private void run() {

        int type = TIFFTag.TIFF_LONG, dt = 1 << type;
        int n0 = 1000, n1 = 1001, n2 = 1002, n3 = 1003;

        TIFFTag tag1 = new TIFFTag(Integer.toString(n1), n1, dt);
        TIFFTag tag2 = new TIFFTag(Integer.toString(n2), n2, dt);
        TIFFTag tag3 = new TIFFTag(Integer.toString(n3), n3, dt);
        TIFFTag parent = new TIFFTag(Integer.toString(n0), n0, dt);

        // tag sets array must not be null
        boolean ok = false;
        try { new TIFFDirectory(null, parent); }
        catch (NullPointerException e) { ok = true; }
        check(ok, "can construct TIFFDirectory with null tagsets array");

        // but can be empty
        TIFFTagSet emptySets[] = {};
        TIFFDirectory d = new TIFFDirectory(emptySets, parent);
        check(d.getTagSets().length == 0, "invalid number of tag sets");
        check(d.getParentTag().getName().equals(Integer.toString(n0)) &&
             (d.getParentTag().getNumber() == n0), "invalid parent tag");


        // add tags
        List<TIFFTag> tags = new ArrayList<>();
        tags.add(tag1);
        tags.add(tag2);
        TIFFTagSet ts1 = new TIFFTagSet(tags);

        tags.clear();
        tags.add(tag3);
        TIFFTagSet ts2 = new TIFFTagSet(tags);

        TIFFTagSet sets[] = {ts1, ts2};
        d = new TIFFDirectory(sets, parent);

        check(d.getTagSets().length == sets.length, "invalid number of tag sets");

        // check getTag()
        for (int i = n1; i <= n3; i++) {
            TIFFTag t = d.getTag(i);
            check(t.getNumber() == i, "invalid tag number");
            check(t.getName().equals(Integer.toString(i)), "invalid tag name");
            check(t.getDataTypes() == dt, "invalid tag data types");
        }

        TIFFDirectory d2;
        try { d2 = d.clone(); }
        catch (CloneNotSupportedException e) { throw new RuntimeException(e); }

        // check removeTagSet()
        d.removeTagSet(ts2);
        check(d.getTagSets().length == 1, "invalid number of tag sets");
        check(d.getTagSets()[0].getTag(n1).getName().equals(Integer.toString(n1)),
            "invalid tag name");
        check(d.getTagSets()[0].getTag(n2).getName().equals(Integer.toString(n2)),
            "invalid tag name");

        d.removeTagSet(ts1);
        check(d.getTagSets().length == 0, "invalid number of tag sets");

        // check cloned data
        check(d2.getTagSets().length == sets.length,
            "invalid number of tag sets");
        TIFFTagSet sets2[] = d2.getTagSets();
        check(sets2.length == sets.length, "invalid number of tag sets");
        check(
            (sets2[0].getTag(Integer.toString(n1)).getNumber() == n1) &&
            (sets2[0].getTag(Integer.toString(n2)).getNumber() == n2) &&
            (sets2[0].getTag(Integer.toString(n0)) == null) &&
            (sets2[1].getTag(Integer.toString(n3)).getNumber() == n3) &&
            (sets2[1].getTag(Integer.toString(n0)) == null), "invalid data");

        check(
            (sets2[0].getTag(Integer.toString(n1)).getDataTypes() == dt) &&
            (sets2[0].getTag(Integer.toString(n2)).getDataTypes() == dt) &&
            (sets2[1].getTag(Integer.toString(n3)).getDataTypes() == dt),
            "invalid data type");

        // must not be able to call removeTagSet with null argument
        ok = false;
        try { d.removeTagSet(null); }
        catch (NullPointerException e) { ok = true; }
        check(ok, "must not be able to use null as an argument for remove");

        // check parent tag
        check( d.getParentTag().getName().equals(Integer.toString(n0)) &&
              d2.getParentTag().getName().equals(Integer.toString(n0)),
            "invalid parent tag name");

        check(( d.getParentTag().getNumber() == n0) &&
              (d2.getParentTag().getNumber() == n0),
            "invalid parent tag number");

        check(( d.getParentTag().getDataTypes() == dt) &&
              (d2.getParentTag().getDataTypes() == dt),
            "invalid parent data type");

        d.addTagSet(ts1);
        d.addTagSet(ts2);

        // add the same tag set twice and check that nothing changed
        d.addTagSet(ts2);

        check(d.getTagSets().length == 2, "invalid number of tag sets");

        // check field operations
        check(d.getNumTIFFFields() == 0, "invalid TIFFFields number");
        check(d.getTIFFField(Integer.MAX_VALUE) == null,
            "must return null TIFFField");

        long offset = 4L;
        long a[] = {0, Integer.MAX_VALUE, (1 << 32) - 1};
        int v = 100500;
        TIFFField
                f1 = new TIFFField(tag1, type, offset, d),
                f2 = new TIFFField(tag2, v),
                f3 = new TIFFField(tag3, type, a.length, a);

        d.addTIFFField(f1);
        d.addTIFFField(f2);
        d.addTIFFField(f3);

        check(d.containsTIFFField(n1) &&
              d.containsTIFFField(n2) &&
              d.containsTIFFField(n3) &&
             !d.containsTIFFField(n0), "invalid containsTIFFField() results");

        check(d.getTIFFField(n0) == null, "can get unadded field");

        check(d.getNumTIFFFields() == 3, "invalid TIFFFields number");

        check(d.getTIFFField(n1).getCount() == 1, "invalid TIFFField count");
        check(d.getTIFFField(n1).getAsLong(0) == offset, "invalid offset");

        check(d.getTIFFField(n2).getCount() == 1, "invalid TIFFField count");
        check(d.getTIFFField(n2).getAsInt(0) == v, "invalid TIFFField value");

        check(d.getTIFFField(n3).getCount() == a.length,
            "invalid TIFFField count");
        for (int i = 0; i < a.length; ++i) {
            check(d.getTIFFField(n3).getAsLong(i) == a[i],
                "invalid TIFFField value");
        }

        TIFFField nested = d.getTIFFField(n1).getDirectory().getTIFFField(n1);
        check(nested.getTag().getNumber() == n1, "invalid tag number");
        check(nested.getCount() == 1, "invalid field count");
        check(nested.getAsLong(0) == offset, "invalid offset");

        // check that the field is overwritten correctly
        int v2 = 1 << 16;
        d.addTIFFField(new TIFFField(tag3, v2));
        check(d.getTIFFField(n3).getCount() == 1, "invalid TIFFField count");
        check(d.getTIFFField(n3).getAsInt(0)== v2, "invalid TIFFField value");
        check(d.getNumTIFFFields() == 3, "invalid TIFFFields number");

        // check removeTIFFField()
        d.removeTIFFField(n3);
        check(d.getNumTIFFFields() == 2, "invalid TIFFFields number");
        check(d.getTIFFField(n3) == null, "can get removed field");

        d.removeTIFFFields();
        check((d.getTIFFField(n1) == null) && (d.getTIFFField(n2) == null),
            "can get removed field");
        check((d.getNumTIFFFields() == 0) && (d.getTIFFFields().length == 0),
            "invalid TIFFFields number");

        // check that array returned by getTIFFFields() is sorted
        // by tag number (as it stated in the docs)
        d.addTIFFField(f3);
        d.addTIFFField(f1);
        d.addTIFFField(f2);

        TIFFField fa[] = d.getTIFFFields();
        check(fa.length == 3, "invalid number of fields");
        check((fa[0].getTagNumber() == n1) &&
              (fa[1].getTagNumber() == n2) &&
              (fa[2].getTagNumber() == n3),
            "array of the fields must be sorted by tag number");

        d.removeTIFFFields();
        d.addTIFFField(f2);

        // test getAsMetaData / createFromMetadata
        try {
            d2 = TIFFDirectory.createFromMetadata(d.getAsMetadata());
        } catch (IIOInvalidTreeException e) {
            throw new RuntimeException(e);
        }

        // check new data
        check(d2.getTagSets().length == sets.length,
            "invalid number of tag sets");
        sets2 = d2.getTagSets();
        check(sets2.length == sets.length, "invalid number of tag sets");
        check(
            (sets2[0].getTag(Integer.toString(n1)).getNumber() == n1) &&
            (sets2[0].getTag(Integer.toString(n2)).getNumber() == n2) &&
            (sets2[0].getTag(Integer.toString(n0)) == null) &&
            (sets2[1].getTag(Integer.toString(n3)).getNumber() == n3) &&
            (sets2[1].getTag(Integer.toString(n0)) == null), "invalid data");

        check(
            (sets2[0].getTag(Integer.toString(n1)).getDataTypes() == dt) &&
            (sets2[0].getTag(Integer.toString(n2)).getDataTypes() == dt) &&
            (sets2[1].getTag(Integer.toString(n3)).getDataTypes() == dt),
            "invalid data type");

        check(!d2.containsTIFFField(n1) &&
               d2.containsTIFFField(n2) &&
              !d2.containsTIFFField(n3), "invalid containsTIFFField() results");
        check(d2.getTIFFField(n2).getCount()  == 1, "invalid TIFFField count");
        check(d2.getTIFFField(n2).getAsInt(0) == v, "invalid TIFFField value");

        check((d2.getParentTag().getNumber() == n0) &&
               d2.getParentTag().getName().equals(Integer.toString(n0)),
               "invalid parent tag");
    }

    public static void main(String[] args) { (new TIFFDirectoryTest()).run(); }
}
