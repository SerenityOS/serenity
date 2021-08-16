/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;

public class CalendarTestEngine {
    private static File file;
    private static BufferedReader in;
    private static int testCount;
    private static int lineno;
    private static Locale locale;
    private static TimeZone timezone;
    private static boolean leniency = true;

    public static void main(String[] args) throws Exception {
        Locale loc = Locale.getDefault();
        TimeZone tz = TimeZone.getDefault();
        locale = loc;
        timezone = tz;
        try {
            for (String arg : args) {
                file = new File(arg);
                FileInputStream fis = new FileInputStream(file);
                System.out.println("Starting " + file.getName() + "...");
                in = new BufferedReader(new InputStreamReader(fis));
                testCount = lineno = 0;
                run();
                System.out.println("Completed " + file.getName());
            }
        } finally {
            Locale.setDefault(loc);
            TimeZone.setDefault(tz);
        }
    }

    private static void run() throws Exception {
        String line;
        int section = 0;
        Map<String, CalendarAdapter> cals = new HashMap<String, CalendarAdapter>();
        CalendarAdapter calendar = null;
        Result result = new Result();

        while ((line = in.readLine()) != null) {
            lineno++;
            line = line.trim();
            // Skip blank and comment lines
            if (line.length() == 0 || line.charAt(0) == '#') {
                continue;
            }
            int comment = line.indexOf('#');
            if (comment != -1) {
                line = line.substring(0, comment).trim();
            }
            Scanner sc = new Scanner(line);
            String token = sc.next();
            Symbol operation = symbol(token);
            if (operation == null) {
                throw new RuntimeException(lineno() + "wrong op? " + token);
            }


            if (operation.type == Symbol.Type.EXCEPTION) {
                String className = sc.next();
                Class clazz = Exceptions.get(className);
                Exception e = result.getException();
                if (!clazz.isInstance(e)) {
                    throw new RuntimeException(lineno() + "unexpected exception: got: " + e
                                               + ", expected=" + clazz);
                }
            }

            Exception x = result.getException();
            if (x != null) {
                throw new RuntimeException(lineno(result.getLineno()) + "Unexpected exception", x);
            }

            try {
                switch (operation.type) {
                case LOCALE:
                    {
                        String lang = sc.next();
                        String country = "", var = "";
                        if (sc.hasNext()) {
                            country = sc.next();
                            if (sc.hasNext()) {
                                var = sc.next();
                            }
                        }
                        locale = new Locale(lang, country, var);
                    }
                    break;

                case TIMEZONE:
                    {
                        if (sc.hasNext()) {
                            String id = sc.next();
                            timezone = TimeZone.getTimeZone(id);
                            if (!timezone.getID().equals(id)) {
                                System.err.printf("Warning: line %d: may get wrong time zone? "
                                                  +"(specified: %s vs. actual: %s)%n",
                                                  lineno, id, timezone.getID());
                            }
                        }
                    }
                    break;

                case NEW:
                    {
                        Symbol op = symbol(sc.next());
                        Calendar cal = null;
                        switch (op.type) {
                        case INSTANCE:
                            cal = Calendar.getInstance(timezone, locale);
                            break;
                        case GREGORIAN:
                            cal = new GregorianAdapter(timezone, locale);
                            break;
                        default:
                            symbolError(op);
                            break;
                        }
                        cal.setLenient(leniency);
                        calendar = new CalendarAdapter(cal);
                        if (sc.hasNext()) {
                            String name = sc.next();
                            cals.put(name.toLowerCase(Locale.ROOT), calendar);
                            if (!leniency) {
                                System.out.printf("%s%s is non-lenient%n", lineno(), name);
                            }
                        } else {
                            throw new RuntimeException(lineno() + "Missing associated name");
                        }
                    }
                    break;

                case TEST:
                    testCount++;
                    if (sc.hasNext()) {
                        System.out.printf("Test#%d:%s%n", testCount, sc.findInLine(".+"));
                    } else {
                        System.out.printf("Test#%d:%n", testCount);
                    }
                    break;

                case USE:
                    {
                        String name = sc.next().toLowerCase(Locale.ROOT);
                        CalendarAdapter c = cals.get(name);
                        if (c == null) {
                            throw new CalendarTestException(lineno() + "calendar " + name
                                                            + " not found.");
                        }
                        calendar = c;
                    }
                    break;

                case ASSIGN:
                    {
                        long v = getLong(sc);
                        String to = sc.next().toLowerCase(Locale.ROOT);
                        boolean assign = true;
                        if (sc.hasNext()) {
                            Symbol condition = symbol(sc.next());
                            if (condition.type == Symbol.Type.IF) {
                                long v1 = getLong(sc);
                                Symbol op = symbol(sc.next());
                                long v2 = getLong(sc);
                                assign = relation(v1, op, v2);
                            } else {
                                symbolError(condition);
                            }
                        }
                        if (assign)
                            Variable.newVar(to, v);
                    }
                    break;

                case EVAL:
                    {
                        long v1 = getLong(sc);
                        String op = sc.next();
                        Symbol operator = symbol(op);
                        if (operator == null) {
                            throw new RuntimeException("op " + op + " invalid");
                        }
                        long v2 = getLong(sc);
                        if (operator.isArithmetic()) {
                            long value = 0;
                            switch (operator.type) {
                            case PLUS:
                                value = v1 + v2;
                                break;

                            case MINUS:
                                value = v1 - v2;
                                break;

                            case MULTIPLY:
                                value = v1 * v2;
                                break;

                            case DIVIDE:
                                value = v1 / v2;
                                break;

                            case MOD:
                                value = v1 % v2;
                                break;

                            default:
                                symbolError(operator);
                                break;
                            }
                            result.setValue(value);
                        } else {
                            if (!relation(v1, operator, v2)) {
                                throw new RuntimeException("not " + v1 + " " + op + " " + v2);
                            }
                        }
                    }
                    break;

                case CLEAR:
                    {
                        Symbol sym = symbol(sc.next());
                        if (sym.type == Symbol.Type.ALL) {
                            calendar.clearAll();
                        } else if (sym.type == Symbol.Type.FIELD) {
                            int f = sym.value();
                            calendar.clearField(f);
                        } else {
                            symbolError(sym);
                        }
                    }
                    break;

                case GET:
                    {
                        Symbol sym = symbol(sc.next());
                        switch (sym.type) {
                        case FIELD:
                            {
                                int f = sym.value();
                                int v = calendar.get(f);
                                result.setValue(v);
                            }
                            break;

                        case MILLIS:
                            {
                                long v = calendar.getTimeInMillis();
                                result.setValue(v);
                            }
                            break;

                        case MINIMUM:
                            {
                                int f = getInt(sc);
                                int v = calendar.getMinimum(f);
                                result.setValue(v);
                            }
                            break;

                        case GREATESTMINIMUM:
                            {
                                int f = getInt(sc);
                                int v = calendar.getGreatestMinimum(f);
                                result.setValue(v);
                            }
                            break;

                        case ACTUALMINIMUM:
                            {
                                int f = getInt(sc);
                                int v = calendar.getActualMinimum(f);
                                result.setValue(v);
                            }
                            break;

                        case MAXIMUM:
                            {
                                int f = getInt(sc);
                                int v = calendar.getMaximum(f);
                                result.setValue(v);
                            }
                            break;

                        case LEASTMAXIMUM:
                            {
                                int f = getInt(sc);
                                int v = calendar.getLeastMaximum(f);
                                result.setValue(v);
                            }
                            break;

                        case ACTUALMAXIMUM:
                            {
                                int f = getInt(sc);
                                int v = calendar.getActualMaximum(f);
                                result.setValue(v);
                            }
                            break;

                        case FIRSTDAYOFWEEK:
                            {
                                result.setValue(calendar.getFirstDayOfWeek());
                            }
                            break;

                        case MINIMALDAYSINFIRSTWEEK:
                            {
                                int v = calendar.getMinimalDaysInFirstWeek();
                                result.setValue(v);
                            }
                            break;

                        default:
                            symbolError(sym);
                            break;
                        }
                    }
                    break;

                case ADD:
                case ROLL:
                    {
                        Symbol sym = symbol(sc.next());
                        if (sym.type == Symbol.Type.FIELD) {
                            int f = sym.value();
                            int v = sc.nextInt();
                            switch (operation.type) {
                            case ADD:
                                calendar.add(f, v);
                                break;

                            case ROLL:
                                calendar.roll(f, v);
                                break;
                            }
                        } else {
                            symbolError(sym);
                        }
                    }
                    break;

                case SET:
                    {
                        Symbol sym = symbol(sc.next());
                        switch (sym.type) {
                        case FIELD:
                            {
                                int f = sym.value();
                                int v = getInt(sc);
                                calendar.set(f, v);
                            }
                            break;

                        case MILLIS:
                            {
                                long v = getLong(sc);
                                calendar.setTimeInMillis(v);
                            }
                            break;

                        case DATE:
                            {
                                int a = getInt(sc);
                                int b = getInt(sc);
                                int c = getInt(sc);
                                if (sc.hasNext()) {
                                    int d = getInt(sc);
                                    // era, year, month, dayOfMonth
                                    calendar.setDate(a, b, c, d);
                                } else {
                                    // year, month, dayOfMonth
                                    calendar.setDate(a, b, c);
                                }
                            }
                            break;

                        case DATETIME:
                            {
                                int y = getInt(sc);
                                int m = getInt(sc);
                                int d = getInt(sc);
                                int hh = getInt(sc);
                                int mm = getInt(sc);
                                int ss = getInt(sc);
                                calendar.setDateTime(y, m, d, hh, mm, ss);
                            }
                            break;

                        case TIMEOFDAY:
                            {
                                int hh = getInt(sc);
                                int mm = getInt(sc);
                                int ss = getInt(sc);
                                int ms = getInt(sc);
                                calendar.setTimeOfDay(hh, mm, ss, ms);
                            }
                            break;

                        case FIRSTDAYOFWEEK:
                            {
                                int v = getInt(sc);
                                calendar.setFirstDayOfWeek(v);
                            }
                            break;

                        case MINIMALDAYSINFIRSTWEEK:
                            {
                                int v = getInt(sc);
                                calendar.setMinimalDaysInFirstWeek(v);
                            }
                            break;

                        case LENIENT:
                            if (calendar != null) {
                                calendar.setLenient(true);
                            }
                            leniency = true;
                            break;

                        case NONLENIENT:
                            if (calendar != null) {
                                calendar.setLenient(false);
                            }
                            leniency = false;
                            break;

                        default:
                            symbolError(sym);
                        }
                        if (sc.hasNext()) {
                            throw new RuntimeException(lineno() + "extra param(s) "
                                                       + sc.findInLine(".+"));
                        }
                    }
                    break;

                case CHECK:
                    {
                        Symbol sym = symbol(sc.next());
                        boolean stat = false;
                        switch (sym.type) {
                        case MILLIS:
                            {
                                long millis = getLong(sc);
                                stat = calendar.checkMillis(millis);
                            }
                            break;

                        case FIELD:
                            {
                                int f = sym.value();
                                int v = getInt(sc);
                                stat = calendar.checkField(f, v);
                            }
                            break;

                        case DATE:
                            {
                                int a = getInt(sc);
                                int b = getInt(sc);
                                int c = getInt(sc);
                                if (sc.hasNext()) {
                                    int d = getInt(sc);
                                    // era year month dayOfMonth
                                    stat = calendar.checkDate(a, b, c, d);
                                } else {
                                    // year month dayOfMonth
                                    stat = calendar.checkDate(a, b, c);
                                }
                            }
                            break;

                        case DATETIME:
                            {
                                int y = getInt(sc);
                                int m = getInt(sc);
                                int d = getInt(sc);
                                int hh = getInt(sc);
                                int mm = getInt(sc);
                                int ss = getInt(sc);
                                if (sc.hasNext()) {
                                    int ms = getInt(sc);
                                    stat = calendar.checkDateTime(y, m, d, hh, mm, ss, ms);
                                } else {
                                    stat = calendar.checkDateTime(y, m, d, hh, mm, ss);
                                }
                            }
                            break;

                        case TIMEOFDAY:
                            {
                                int hh = sc.nextInt();
                                int mm = sc.nextInt();
                                int ss = sc.nextInt();
                                int millis = sc.nextInt();
                                stat = calendar.checkTimeOfDay(hh, mm, ss, millis);
                            }
                            break;

                        case MINIMUM:
                            {
                                int f = getInt(sc);
                                int v = getInt(sc);
                                stat = calendar.checkMinimum(f, v);
                            }
                            break;

                        case GREATESTMINIMUM:
                            {
                                int f = getInt(sc);
                                int v = getInt(sc);
                                stat = calendar.checkGreatestMinimum(f, v);
                            }
                            break;

                        case ACTUALMINIMUM:
                            {
                                int f = getInt(sc);
                                int v = getInt(sc);
                                stat = calendar.checkActualMinimum(f, v);
                            }
                            break;

                        case MAXIMUM:
                            {
                                int f = getInt(sc);
                                int v = getInt(sc);
                                stat = calendar.checkMaximum(f, v);
                            }
                            break;

                        case LEASTMAXIMUM:
                            {
                                int f = getInt(sc);
                                int v = getInt(sc);
                                stat = calendar.checkLeastMaximum(f, v);
                            }
                            break;

                        case ACTUALMAXIMUM:
                            {
                                int f = getInt(sc);
                                int v = getInt(sc);
                                stat = calendar.checkActualMaximum(f, v);
                            }
                            break;

                        default:
                            throw new RuntimeException(lineno() + "Unknown operand");
                        }
                        if (!stat) {
                            throw new RuntimeException(lineno() + calendar.getMessage());
                        }
                    }
                    break;

                case PRINT:
                    {
                        String s = sc.next();
                        if (s.charAt(0) == '$') {
                            Variable var = variable(s);
                            if (var == null)
                                throw new RuntimeException(lineno() + "Unknown token: " + s);
                            System.out.printf("%s%s=%d%n", lineno(), s, var.longValue());
                            break;
                        }

                        Symbol sym = symbol(s);
                        switch (sym.type) {
                        case INSTANCE:
                            {
                                Calendar cal = calendar;
                                String name = "current";
                                if (sc.hasNext()) {
                                    name = sc.next();
                                    cal = cals.get(name.toLowerCase(Locale.ROOT));
                                }
                                System.out.printf("%s%s=%s%n", lineno(), name, cal);
                            }
                            break;

                        case FIELD:
                            {
                                int f = sym.value();
                                String remark = "";
                                if (sc.hasNext()) {
                                    remark = sc.findInLine(".+");
                                }
                                System.out.printf("%s%s=%d %s%n", lineno(), calendar.fieldName(f),
                                                  calendar.get(f), remark);
                            }
                            break;

                        case MILLIS:
                            {
                                String remark = "";
                                if (sc.hasNext()) {
                                    remark = sc.findInLine(".+");
                                }
                                System.out.printf("%sMillis=%d %s%n", lineno(),
                                                  calendar.getTimeInMillis(), remark);
                            }
                            break;

                        case MINIMUM:
                            System.out.printf("%s%s=%d%n", lineno(),
                                              s, calendar.getMinimum(getInt(sc)));
                            break;

                        case GREATESTMINIMUM:
                            System.out.printf("%s%s=%d%n", lineno(),
                                              s, calendar.getGreatestMinimum(getInt(sc)));
                            break;

                        case ACTUALMINIMUM:
                            System.out.printf("%s%s=%d%n", lineno(),
                                              s, calendar.getActualMinimum(getInt(sc)));
                            break;

                        case MAXIMUM:
                            System.out.printf("%s%s=%d%n", lineno(),
                                              s, calendar.getMaximum(getInt(sc)));
                            break;

                        case LEASTMAXIMUM:
                            System.out.printf("%s%s=%d%n", lineno(),
                                              s, calendar.getLeastMaximum(getInt(sc)));
                            break;

                        case ACTUALMAXIMUM:
                            System.out.printf("%s%s=%d%n", lineno(),
                                              s, calendar.getActualMaximum(getInt(sc)));
                            break;

                        case DATE:
                            System.out.println(lineno() + calendar.toDateString());
                            break;

                        case DATETIME:
                            System.out.println(lineno() + calendar.toDateTimeString());
                            break;

                        case TIMEZONE:
                            System.out.println(lineno() + "timezone=" + timezone);
                            break;

                        case LOCALE:
                            System.out.println(lineno() + "locale=" + locale);
                            break;
                        }
                    }
                }
            } catch (CalendarTestException cte) {
                throw cte;
            } catch (NoSuchElementException nsee) {
                throw new NoSuchElementException(lineno() + "syntax error");
            } catch (Exception e) {
                result.setException(e);
                result.setLineno(lineno);
            }
        }
        Exception x = result.getException();
        if (x != null) {
            throw new RuntimeException(lineno(result.getLineno()) + "Unexpected exception", x);
        }
    }

    private static Symbol symbol(String s) {
        return Symbol.get(s.toLowerCase(Locale.ROOT));
    }

    private static Variable variable(String s) {
        return Variable.get(s.toLowerCase(Locale.ROOT));
    }

    private static int getInt(Scanner sc) {
        if (sc.hasNextInt()) {
            return sc.nextInt();
        }

        String s = sc.next();
        if (s.charAt(0) == '$') {
            Variable var = variable(s);
            if (var == null)
                throw new RuntimeException(lineno() + "Unknown token: " + s);
            return var.intValue();
        }
        Symbol sym = symbol(s);
        if (sym == null)
            throw new RuntimeException(lineno() + "Unknown token: " + s);
        return sym.value();
    }

    private static long getLong(Scanner sc) {
        if (sc.hasNextLong()) {
            return sc.nextLong();
        }

        String s = sc.next();
        if (s.charAt(0) == '$') {
            Variable var = variable(s);
            if (var == null)
                throw new RuntimeException(lineno() + "Unknown token: " + s);
            return var.longValue();
        }
        Symbol sym = symbol(s);
        if (sym == null)
            throw new RuntimeException(lineno() + "Unknown token: " + s);
        return sym.value();
    }

    private static boolean relation(long v1, Symbol relop, long v2) {
        boolean result = false;
        switch (relop.type) {
        case GT:
            result = v1 > v2;
            break;

        case GE:
            result = v1 >= v2;
            break;

        case EQ:
            result = v1 == v2;
            break;

        case NEQ:
            result = v1 != v2;
            break;

        case LE:
            result = v1 <= v2;
            break;

        case LT:
            result = v1 < v2;
            break;
        }
        return result;
    }

    private static String lineno() {
        return lineno(lineno);
    }

    private static String lineno(int ln) {
        return file.getName() + ":" + ln + ": ";
    }

    private static void symbolError(Symbol sym) {
        throw new RuntimeException(lineno + ": unexpected symbol: " + sym);
    }
}
