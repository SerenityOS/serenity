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

package selectionresolution;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Representation of an expected result.
 */
public interface Result {
    public static final Result ICCE = new Exception(IncompatibleClassChangeError.class);
    public static final Result IAE  = new Exception(IllegalAccessError.class);
    public static final Result NSME = new Exception(NoSuchMethodError.class);
    public static final Result AME  = new Exception(AbstractMethodError.class);

    // Factories

    /**
     * Create a result that expects the given class.
     */
    public static Result is(int id) {
        return new Single(id);
    }

    /**
     * Create a result that expects the given classes.
     */
    public static Result is(int... multiple) {
        assert multiple.length > 0;

        if (multiple.length == 1) {
            return new Single(multiple[0]);
        } else {
            return new Any(multiple);
        }
    }

    /**
     * Create a result that expects the given exception to be thrown.
     */
    public static Result is(Class<? extends Throwable> exType) {
        return new Exception(exType);
    }

    /**
     * Create a result that expects the given exception to be thrown.
     */
    public static Result is(Throwable ex) {
        return Result.is(ex.getClass());
    }

    public static final Result EMPTY = new Empty();

    /**
     * Create an empty Result.
     */
    public static Result empty() {
        return EMPTY;
    }


    public boolean complyWith(int i);
    public boolean complyWith(Throwable e);
    public boolean complyWith(Result r);

    static class Empty implements Result {
        @Override
        public boolean complyWith(int i) {
            return false;
        }

        @Override
        public boolean complyWith(Throwable e) {
            return false;
        }

        @Override
        public boolean complyWith(Result r) {
            return false;
        }
    }

    static class Single implements Result {
        public int id;

        public Single(int id) {
            this.id = id;
        }

        @Override
        public boolean complyWith(int i) {
            return id == i;
        }

        @Override
        public boolean complyWith(Throwable e) {
            return false;
        }

        @Override
        public boolean complyWith(Result r) {
            if (r instanceof Single) {
                return complyWith(((Single)r).id);
            } else if (r instanceof Any) {
                return r.complyWith(this);
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Single)) return false;

            Single single = (Single) o;

            return (id == single.id);
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("Result=Single{");
            sb.append("id=").append(id);
            sb.append('}');
            return sb.toString();
        }
    }

    static class Any implements Result {
        public int[] ids;
        public Any(int[] ids) {
            this.ids = ids;
        }

        @Override
        public boolean complyWith(int i) {
            return Arrays.stream(ids)
                    .anyMatch(j -> j == i);
        }

        @Override
        public boolean complyWith(Throwable e) {
            return false;
        }

        @Override
        public boolean complyWith(Result r) {
            if (r instanceof Single) {
                return complyWith(((Single)r).id);
            }
            if (r instanceof Any) {
                return Arrays.equals(ids, ((Any) r).ids);
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Any any = (Any) o;

            return Arrays.equals(ids, any.ids);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(ids);
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("Result=Any{");
            sb.append("ids=");
            if (ids == null) sb.append("null");
            else {
                sb.append('[');
                for (int i = 0; i < ids.length; ++i)
                    sb.append(i == 0 ? "" : ", ").append(ids[i]);
                sb.append(']');
            }
            sb.append('}');
            return sb.toString();
        }
    }

    static class Exception implements Result {
        public Class<? extends Throwable> exc;
        public Exception(Class<? extends Throwable> e) {
            this.exc = e;
        }

        @Override
        public boolean complyWith(int i) {
            return false;
        }

        @Override
        public boolean complyWith(Throwable e) {
            return exc.isAssignableFrom(e.getClass());
        }

        @Override
        public boolean complyWith(Result r) {
            if (r instanceof Exception) {
                return exc.isAssignableFrom(((Exception) r).exc);
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Exception)) return false;

            Exception exception = (Exception) o;

            return exc.equals(exception.exc);
        }

        @Override
        public int hashCode() {
            return exc.hashCode();
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("Result=Exception{");
            sb.append("exc=").append(exc);
            sb.append('}');
            return sb.toString();
        }
    }
}
