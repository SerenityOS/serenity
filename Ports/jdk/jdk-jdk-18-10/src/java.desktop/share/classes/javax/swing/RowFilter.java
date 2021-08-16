/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * <code>RowFilter</code> is used to filter out entries from the
 * model so that they are not shown in the view.  For example, a
 * <code>RowFilter</code> associated with a <code>JTable</code> might
 * only allow rows that contain a column with a specific string. The
 * meaning of <em>entry</em> depends on the component type.
 * For example, when a filter is
 * associated with a <code>JTable</code>, an entry corresponds to a
 * row; when associated with a <code>JTree</code>, an entry corresponds
 * to a node.
 * <p>
 * Subclasses must override the <code>include</code> method to
 * indicate whether the entry should be shown in the
 * view.  The <code>Entry</code> argument can be used to obtain the values in
 * each of the columns in that entry.  The following example shows an
 * <code>include</code> method that allows only entries containing one or
 * more values starting with the string "a":
 * <pre>
 * RowFilter&lt;Object,Object&gt; startsWithAFilter = new RowFilter&lt;Object,Object&gt;() {
 *   public boolean include(Entry&lt;? extends Object, ? extends Object&gt; entry) {
 *     for (int i = entry.getValueCount() - 1; i &gt;= 0; i--) {
 *       if (entry.getStringValue(i).startsWith("a")) {
 *         // The value starts with "a", include it
 *         return true;
 *       }
 *     }
 *     // None of the columns start with "a"; return false so that this
 *     // entry is not shown
 *     return false;
 *   }
 * };
 * </pre>
 * <code>RowFilter</code> has two formal type parameters that allow
 * you to create a <code>RowFilter</code> for a specific model. For
 * example, the following assumes a specific model that is wrapping
 * objects of type <code>Person</code>.  Only <code>Person</code>s
 * with an age over 20 will be shown:
 * <pre>
 * RowFilter&lt;PersonModel,Integer&gt; ageFilter = new RowFilter&lt;PersonModel,Integer&gt;() {
 *   public boolean include(Entry&lt;? extends PersonModel, ? extends Integer&gt; entry) {
 *     PersonModel personModel = entry.getModel();
 *     Person person = personModel.getPerson(entry.getIdentifier());
 *     if (person.getAge() &gt; 20) {
 *       // Returning true indicates this row should be shown.
 *       return true;
 *     }
 *     // Age is &lt;= 20, don't show it.
 *     return false;
 *   }
 * };
 * PersonModel model = createPersonModel();
 * TableRowSorter&lt;PersonModel&gt; sorter = new TableRowSorter&lt;PersonModel&gt;(model);
 * sorter.setRowFilter(ageFilter);
 * </pre>
 *
 * @param <M> the type of the model; for example <code>PersonModel</code>
 * @param <I> the type of the identifier; when using
 *            <code>TableRowSorter</code> this will be <code>Integer</code>
 * @see javax.swing.table.TableRowSorter
 * @since 1.6
 */
public abstract class RowFilter<M,I> {
    /**
     * Enumeration of the possible comparison values supported by
     * some of the default <code>RowFilter</code>s.
     *
     * @see RowFilter
     * @since 1.6
     */
    public enum ComparisonType {
        /**
         * Indicates that entries with a value before the supplied
         * value should be included.
         */
        BEFORE,

        /**
         * Indicates that entries with a value after the supplied
         * value should be included.
         */
        AFTER,

        /**
         * Indicates that entries with a value equal to the supplied
         * value should be included.
         */
        EQUAL,

        /**
         * Indicates that entries with a value not equal to the supplied
         * value should be included.
         */
        NOT_EQUAL
    }

    /**
     * Constructor for subclasses to call.
     */
    protected RowFilter() {}

    /**
     * Throws an IllegalArgumentException if any of the values in
     * columns are {@literal <} 0.
     */
    private static void checkIndices(int[] columns) {
        for (int i = columns.length - 1; i >= 0; i--) {
            if (columns[i] < 0) {
                throw new IllegalArgumentException("Index must be >= 0");
            }
        }
    }

    /**
     * Returns a <code>RowFilter</code> that uses a regular
     * expression to determine which entries to include.  Only entries
     * with at least one matching value are included.  For
     * example, the following creates a <code>RowFilter</code> that
     * includes entries with at least one value starting with
     * "a":
     * <pre>
     *   RowFilter.regexFilter("^a");
     * </pre>
     * <p>
     * The returned filter uses {@link java.util.regex.Matcher#find}
     * to test for inclusion.  To test for exact matches use the
     * characters '^' and '$' to match the beginning and end of the
     * string respectively.  For example, "^foo$" includes only rows whose
     * string is exactly "foo" and not, for example, "food".  See
     * {@link java.util.regex.Pattern} for a complete description of
     * the supported regular-expression constructs.
     *
     * @param <M> the type of the model to which the {@code RowFilter} applies
     * @param <I> the type of the identifier passed to the {@code RowFilter}
     * @param regex the regular expression to filter on
     * @param indices the indices of the values to check.  If not supplied all
     *               values are evaluated
     * @return a <code>RowFilter</code> implementing the specified criteria
     * @throws NullPointerException if <code>regex</code> is
     *         <code>null</code>
     * @throws IllegalArgumentException if any of the <code>indices</code>
     *         are &lt; 0
     * @throws PatternSyntaxException if <code>regex</code> is
     *         not a valid regular expression.
     * @see java.util.regex.Pattern
     */
    public static <M,I> RowFilter<M,I> regexFilter(String regex,
                                                       int... indices) {
        return new RegexFilter<M, I>(Pattern.compile(regex), indices);
    }

    /**
     * Returns a <code>RowFilter</code> that includes entries that
     * have at least one <code>Date</code> value meeting the specified
     * criteria.  For example, the following <code>RowFilter</code> includes
     * only entries with at least one date value after the current date:
     * <pre>
     *   RowFilter.dateFilter(ComparisonType.AFTER, new Date());
     * </pre>
     *
     * @param <M> the type of the model to which the {@code RowFilter} applies
     * @param <I> the type of the identifier passed to the {@code RowFilter}
     * @param type the type of comparison to perform
     * @param date the date to compare against
     * @param indices the indices of the values to check.  If not supplied all
     *               values are evaluated
     * @return a <code>RowFilter</code> implementing the specified criteria
     * @throws NullPointerException if <code>date</code> is
     *          <code>null</code>
     * @throws IllegalArgumentException if any of the <code>indices</code>
     *         are &lt; 0 or <code>type</code> is
     *         <code>null</code>
     * @see java.util.Calendar
     * @see java.util.Date
     */
    public static <M,I> RowFilter<M,I> dateFilter(ComparisonType type,
                                            Date date, int... indices) {
        return new DateFilter<M, I>(type, date.getTime(), indices);
    }

    /**
     * Returns a <code>RowFilter</code> that includes entries that
     * have at least one <code>Number</code> value meeting the
     * specified criteria.  For example, the following
     * filter will only include entries with at
     * least one number value equal to 10:
     * <pre>
     *   RowFilter.numberFilter(ComparisonType.EQUAL, 10);
     * </pre>
     *
     * @param <M> the type of the model to which the {@code RowFilter} applies
     * @param <I> the type of the identifier passed to the {@code RowFilter}
     * @param type the type of comparison to perform
     * @param number a {@code Number} value to compare against
     * @param indices the indices of the values to check.  If not supplied all
     *               values are evaluated
     * @return a <code>RowFilter</code> implementing the specified criteria
     * @throws IllegalArgumentException if any of the <code>indices</code>
     *         are &lt; 0, <code>type</code> is <code>null</code>
     *         or <code>number</code> is <code>null</code>
     */
    public static <M,I> RowFilter<M,I> numberFilter(ComparisonType type,
                                            Number number, int... indices) {
        return new NumberFilter<M, I>(type, number, indices);
    }

    /**
     * Returns a <code>RowFilter</code> that includes entries if any
     * of the supplied filters includes the entry.
     * <p>
     * The following example creates a <code>RowFilter</code> that will
     * include any entries containing the string "foo" or the string
     * "bar":
     * <pre>
     *   List&lt;RowFilter&lt;Object,Object&gt;&gt; filters = new ArrayList&lt;RowFilter&lt;Object,Object&gt;&gt;(2);
     *   filters.add(RowFilter.regexFilter("foo"));
     *   filters.add(RowFilter.regexFilter("bar"));
     *   RowFilter&lt;Object,Object&gt; fooBarFilter = RowFilter.orFilter(filters);
     * </pre>
     *
     * @param <M> the type of the model to which the {@code RowFilter} applies
     * @param <I> the type of the identifier passed to the {@code RowFilter}
     * @param filters the <code>RowFilter</code>s to test
     * @throws IllegalArgumentException if any of the filters
     *         are <code>null</code>
     * @throws NullPointerException if <code>filters</code> is null
     * @return a <code>RowFilter</code> implementing the specified criteria
     * @see java.util.Arrays#asList
     */
    public static <M,I> RowFilter<M,I> orFilter(
            Iterable<? extends RowFilter<? super M, ? super I>> filters) {
        return new OrFilter<M,I>(filters);
    }

    /**
     * Returns a <code>RowFilter</code> that includes entries if all
     * of the supplied filters include the entry.
     * <p>
     * The following example creates a <code>RowFilter</code> that will
     * include any entries containing the string "foo" and the string
     * "bar":
     * <pre>
     *   List&lt;RowFilter&lt;Object,Object&gt;&gt; filters = new ArrayList&lt;RowFilter&lt;Object,Object&gt;&gt;(2);
     *   filters.add(RowFilter.regexFilter("foo"));
     *   filters.add(RowFilter.regexFilter("bar"));
     *   RowFilter&lt;Object,Object&gt; fooBarFilter = RowFilter.andFilter(filters);
     * </pre>
     *
     * @param <M> the type of the model the {@code RowFilter} applies to
     * @param <I> the type of the identifier passed to the {@code RowFilter}
     * @param filters the <code>RowFilter</code>s to test
     * @return a <code>RowFilter</code> implementing the specified criteria
     * @throws IllegalArgumentException if any of the filters
     *         are <code>null</code>
     * @throws NullPointerException if <code>filters</code> is null
     * @see java.util.Arrays#asList
     */
    public static <M,I> RowFilter<M,I> andFilter(
            Iterable<? extends RowFilter<? super M, ? super I>> filters) {
        return new AndFilter<M,I>(filters);
    }

    /**
     * Returns a <code>RowFilter</code> that includes entries if the
     * supplied filter does not include the entry.
     *
     * @param <M> the type of the model to which the {@code RowFilter} applies
     * @param <I> the type of the identifier passed to the {@code RowFilter}
     * @param filter the <code>RowFilter</code> to negate
     * @return a <code>RowFilter</code> implementing the specified criteria
     * @throws IllegalArgumentException if <code>filter</code> is
     *         <code>null</code>
     */
    public static <M,I> RowFilter<M,I> notFilter(RowFilter<M,I> filter) {
        return new NotFilter<M,I>(filter);
    }

    /**
     * Returns true if the specified entry should be shown;
     * returns false if the entry should be hidden.
     * <p>
     * The <code>entry</code> argument is valid only for the duration of
     * the invocation.  Using <code>entry</code> after the call returns
     * results in undefined behavior.
     *
     * @param entry a non-<code>null</code> object that wraps the underlying
     *              object from the model
     * @return true if the entry should be shown
     */
    public abstract boolean include(Entry<? extends M, ? extends I> entry);

    //
    // WARNING:
    // Because of the method signature of dateFilter/numberFilter/regexFilter
    // we can NEVER add a method to RowFilter that returns M,I. If we were
    // to do so it would be possible to get a ClassCastException during normal
    // usage.
    //

    /**
     * An <code>Entry</code> object is passed to instances of
     * <code>RowFilter</code>, allowing the filter to get the value of the
     * entry's data, and thus to determine whether the entry should be shown.
     * An <code>Entry</code> object contains information about the model
     * as well as methods for getting the underlying values from the model.
     *
     * @param <M> the type of the model; for example <code>PersonModel</code>
     * @param <I> the type of the identifier; when using
     *            <code>TableRowSorter</code> this will be <code>Integer</code>
     * @see javax.swing.RowFilter
     * @see javax.swing.DefaultRowSorter#setRowFilter(javax.swing.RowFilter)
     * @since 1.6
     */
    public abstract static class Entry<M, I> {
        /**
         * Creates an <code>Entry</code>.
         */
        public Entry() {
        }

        /**
         * Returns the underlying model.
         *
         * @return the model containing the data that this entry represents
         */
        public abstract M getModel();

        /**
         * Returns the number of values in the entry.  For
         * example, when used with a table this corresponds to the
         * number of columns.
         *
         * @return number of values in the object being filtered
         */
        public abstract int getValueCount();

        /**
         * Returns the value at the specified index.  This may return
         * <code>null</code>.  When used with a table, index
         * corresponds to the column number in the model.
         *
         * @param index the index of the value to get
         * @return value at the specified index
         * @throws IndexOutOfBoundsException if index &lt; 0 or
         *         &gt;= getValueCount
         */
        public abstract Object getValue(int index);

        /**
         * Returns the string value at the specified index.  If
         * filtering is being done based on <code>String</code> values
         * this method is preferred to that of <code>getValue</code>
         * as <code>getValue(index).toString()</code> may return a
         * different result than <code>getStringValue(index)</code>.
         * <p>
         * This implementation calls <code>getValue(index).toString()</code>
         * after checking for <code>null</code>.  Subclasses that provide
         * different string conversion should override this method if
         * necessary.
         *
         * @param index the index of the value to get
         * @return {@code non-null} string at the specified index
         * @throws IndexOutOfBoundsException if index &lt; 0 ||
         *         &gt;= getValueCount
         */
        public String getStringValue(int index) {
            Object value = getValue(index);
            return (value == null) ? "" : value.toString();
        }

        /**
         * Returns the identifer (in the model) of the entry.
         * For a table this corresponds to the index of the row in the model,
         * expressed as an <code>Integer</code>.
         *
         * @return a model-based (not view-based) identifier for
         *         this entry
         */
        public abstract I getIdentifier();
    }


    private abstract static class GeneralFilter<M, I> extends RowFilter<M, I> {
        private int[] columns;

        GeneralFilter(int[] columns) {
            checkIndices(columns);
            this.columns = columns;
        }

        @Override
        public boolean include(Entry<? extends M, ? extends I> value){
            int count = value.getValueCount();
            if (columns.length > 0) {
                for (int i = columns.length - 1; i >= 0; i--) {
                    int index = columns[i];
                    if (index < count) {
                        if (include(value, index)) {
                            return true;
                        }
                    }
                }
            } else {
                while (--count >= 0) {
                    if (include(value, count)) {
                        return true;
                    }
                }
            }
            return false;
        }

        protected abstract boolean include(
              Entry<? extends M, ? extends I> value, int index);
    }


    private static class RegexFilter<M, I> extends GeneralFilter<M, I> {
        private Matcher matcher;

        RegexFilter(Pattern regex, int[] columns) {
            super(columns);
            if (regex == null) {
                throw new IllegalArgumentException("Pattern must be non-null");
            }
            matcher = regex.matcher("");
        }

        @Override
        protected boolean include(
                Entry<? extends M, ? extends I> value, int index) {
            matcher.reset(value.getStringValue(index));
            return matcher.find();
        }
    }


    private static class DateFilter<M, I> extends GeneralFilter<M, I> {
        private long date;
        private ComparisonType type;

        DateFilter(ComparisonType type, long date, int[] columns) {
            super(columns);
            if (type == null) {
                throw new IllegalArgumentException("type must be non-null");
            }
            this.type = type;
            this.date = date;
        }

        @Override
        protected boolean include(
                Entry<? extends M, ? extends I> value, int index) {
            Object v = value.getValue(index);

            if (v instanceof Date) {
                long vDate = ((Date)v).getTime();
                switch(type) {
                case BEFORE:
                    return (vDate < date);
                case AFTER:
                    return (vDate > date);
                case EQUAL:
                    return (vDate == date);
                case NOT_EQUAL:
                    return (vDate != date);
                default:
                    break;
                }
            }
            return false;
        }
    }

    private static class NumberFilter<M, I> extends GeneralFilter<M, I> {
        private boolean isComparable;
        private Number number;
        private ComparisonType type;

        NumberFilter(ComparisonType type, Number number, int[] columns) {
            super(columns);
            if (type == null || number == null) {
                throw new IllegalArgumentException(
                    "type and number must be non-null");
            }
            this.type = type;
            this.number = number;
            isComparable = (number instanceof Comparable);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected boolean include(
                Entry<? extends M, ? extends I> value, int index) {
            Object v = value.getValue(index);

            if (v instanceof Number) {
                boolean compared = true;
                int compareResult;
                Class<?> vClass = v.getClass();
                if (number.getClass() == vClass && isComparable) {
                    compareResult = ((Comparable)number).compareTo(v);
                }
                else {
                    compareResult = longCompare((Number)v);
                }
                switch(type) {
                case BEFORE:
                    return (compareResult > 0);
                case AFTER:
                    return (compareResult < 0);
                case EQUAL:
                    return (compareResult == 0);
                case NOT_EQUAL:
                    return (compareResult != 0);
                default:
                    break;
                }
            }
            return false;
        }

        private int longCompare(Number o) {
            long diff = number.longValue() - o.longValue();

            if (diff < 0) {
                return -1;
            }
            else if (diff > 0) {
                return 1;
            }
            return 0;
        }
    }


    private static class OrFilter<M,I> extends RowFilter<M,I> {
        List<RowFilter<? super M,? super I>> filters;

        OrFilter(Iterable<? extends RowFilter<? super M, ? super I>> filters) {
            this.filters = new ArrayList<RowFilter<? super M,? super I>>();
            for (RowFilter<? super M, ? super I> filter : filters) {
                if (filter == null) {
                    throw new IllegalArgumentException(
                        "Filter must be non-null");
                }
                this.filters.add(filter);
            }
        }

        public boolean include(Entry<? extends M, ? extends I> value) {
            for (RowFilter<? super M,? super I> filter : filters) {
                if (filter.include(value)) {
                    return true;
                }
            }
            return false;
        }
    }


    private static class AndFilter<M,I> extends OrFilter<M,I> {
        AndFilter(Iterable<? extends RowFilter<? super M,? super I>> filters) {
            super(filters);
        }

        public boolean include(Entry<? extends M, ? extends I> value) {
            for (RowFilter<? super M,? super I> filter : filters) {
                if (!filter.include(value)) {
                    return false;
                }
            }
            return true;
        }
    }


    private static class NotFilter<M,I> extends RowFilter<M,I> {
        private RowFilter<M,I> filter;

        NotFilter(RowFilter<M,I> filter) {
            if (filter == null) {
                throw new IllegalArgumentException(
                    "filter must be non-null");
            }
            this.filter = filter;
        }

        public boolean include(Entry<? extends M, ? extends I> value) {
            return !filter.include(value);
        }
    }
}
