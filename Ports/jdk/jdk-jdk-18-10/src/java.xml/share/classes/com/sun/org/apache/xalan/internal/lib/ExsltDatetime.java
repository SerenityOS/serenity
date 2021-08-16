/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xalan.internal.lib;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.sun.org.apache.xpath.internal.objects.XBoolean;
import com.sun.org.apache.xpath.internal.objects.XNumber;
import com.sun.org.apache.xpath.internal.objects.XObject;

/**
 * This class contains EXSLT dates and times extension functions.
 * It is accessed by specifying a namespace URI as follows:
 * <pre>
 *    xmlns:datetime="http://exslt.org/dates-and-times"
 * </pre>
 *
 * The documentation for each function has been copied from the relevant
 * EXSLT Implementer page.
 *
 * @see <a href="http://www.exslt.org/">EXSLT</a>
 * @xsl.usage general
 * @LastModified: Nov 2017
 */

public class ExsltDatetime
{
    // Datetime formats (era and zone handled separately).
    static final String dt = "yyyy-MM-dd'T'HH:mm:ss";
    static final String d = "yyyy-MM-dd";
    static final String gym = "yyyy-MM";
    static final String gy = "yyyy";
    static final String gmd = "--MM-dd";
    static final String gm = "--MM--";
    static final String gd = "---dd";
    static final String t = "HH:mm:ss";
    static final String EMPTY_STR = "";

    /**
     * The date:date-time function returns the current date and time as a date/time string.
     * The date/time string that's returned must be a string in the format defined as the
     * lexical representation of xs:dateTime in
     * <a href="http://www.w3.org/TR/xmlschema-2/#dateTime">[3.2.7 dateTime]</a> of
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>.
     * The date/time format is basically CCYY-MM-DDThh:mm:ss, although implementers should consult
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a> and
     * <a href="http://www.iso.ch/markete/8601.pdf">[ISO 8601]</a> for details.
     * The date/time string format must include a time zone, either a Z to indicate Coordinated
     * Universal Time or a + or - followed by the difference between the difference from UTC
     * represented as hh:mm.
     */
    public static String dateTime()
    {
      Calendar cal = Calendar.getInstance();
      Date datetime = cal.getTime();
      // Format for date and time.
      SimpleDateFormat dateFormat = new SimpleDateFormat(dt);

      StringBuffer buff = new StringBuffer(dateFormat.format(datetime));
      // Must also include offset from UTF.
      // Get the offset (in milliseconds).
      int offset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);
      // If there is no offset, we have "Coordinated
      // Universal Time."
      if (offset == 0)
        buff.append("Z");
      else
      {
        // Convert milliseconds to hours and minutes
        int hrs = offset/(60*60*1000);
        // In a few cases, the time zone may be +/-hh:30.
        int min = offset%(60*60*1000);
        char posneg = hrs < 0? '-': '+';
        buff.append(posneg).append(formatDigits(hrs)).append(':').append(formatDigits(min));
      }
      return buff.toString();
    }

    /**
     * Represent the hours and minutes with two-digit strings.
     * @param q hrs or minutes.
     * @return two-digit String representation of hrs or minutes.
     */
    private static String formatDigits(int q)
    {
      String dd = String.valueOf(Math.abs(q));
      return dd.length() == 1 ? '0' + dd : dd;
    }

    /**
     * The date:date function returns the date specified in the date/time string given
     * as the argument. If no argument is given, then the current local date/time, as
     * returned by date:date-time is used as a default argument.
     * The date/time string that's returned must be a string in the format defined as the
     * lexical representation of xs:dateTime in
     * <a href="http://www.w3.org/TR/xmlschema-2/#dateTime">[3.2.7 dateTime]</a> of
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>.
     * If the argument is not in either of these formats, date:date returns an empty string ('').
     * The date/time format is basically CCYY-MM-DDThh:mm:ss, although implementers should consult
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a> and
     * <a href="http://www.iso.ch/markete/8601.pdf">[ISO 8601]</a> for details.
     * The date is returned as a string with a lexical representation as defined for xs:date in
     * [3.2.9 date] of [XML Schema Part 2: Datatypes]. The date format is basically CCYY-MM-DD,
     * although implementers should consult [XML Schema Part 2: Datatypes] and [ISO 8601] for details.
     * If no argument is given or the argument date/time specifies a time zone, then the date string
     * format must include a time zone, either a Z to indicate Coordinated Universal Time or a + or -
     * followed by the difference between the difference from UTC represented as hh:mm. If an argument
     * is specified and it does not specify a time zone, then the date string format must not include
     * a time zone.
     */
    public static String date(String datetimeIn)
      throws ParseException
    {
      String[] edz = getEraDatetimeZone(datetimeIn);
      String leader = edz[0];
      String datetime = edz[1];
      String zone = edz[2];
      if (datetime == null || zone == null)
        return EMPTY_STR;

      String[] formatsIn = {dt, d};
      String formatOut = d;
      Date date = testFormats(datetime, formatsIn);
      if (date == null) return EMPTY_STR;

      SimpleDateFormat dateFormat = new SimpleDateFormat(formatOut);
      dateFormat.setLenient(false);
      String dateOut = dateFormat.format(date);
      if (dateOut.length() == 0)
          return EMPTY_STR;
      else
        return (leader + dateOut + zone);
    }


    /**
     * See above.
     */
    public static String date()
    {
      String datetime = dateTime().toString();
      String date = datetime.substring(0, datetime.indexOf("T"));
      String zone = datetime.substring(getZoneStart(datetime));
      return (date + zone);
    }

    /**
     * The date:time function returns the time specified in the date/time string given
     * as the argument. If no argument is given, then the current local date/time, as
     * returned by date:date-time is used as a default argument.
     * The date/time string that's returned must be a string in the format defined as the
     * lexical representation of xs:dateTime in
     * <a href="http://www.w3.org/TR/xmlschema-2/#dateTime">[3.2.7 dateTime]</a> of
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>.
     * If the argument string is not in this format, date:time returns an empty string ('').
     * The date/time format is basically CCYY-MM-DDThh:mm:ss, although implementers should consult
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a> and
     * <a href="http://www.iso.ch/markete/8601.pdf">[ISO 8601]</a> for details.
     * The date is returned as a string with a lexical representation as defined for xs:time in
     * <a href="http://www.w3.org/TR/xmlschema-2/#time">[3.2.8 time]</a> of [XML Schema Part 2: Datatypes].
     * The time format is basically hh:mm:ss, although implementers should consult [XML Schema Part 2:
     * Datatypes] and [ISO 8601] for details.
     * If no argument is given or the argument date/time specifies a time zone, then the time string
     * format must include a time zone, either a Z to indicate Coordinated Universal Time or a + or -
     * followed by the difference between the difference from UTC represented as hh:mm. If an argument
     * is specified and it does not specify a time zone, then the time string format must not include
     * a time zone.
     */
    public static String time(String timeIn)
      throws ParseException
    {
      String[] edz = getEraDatetimeZone(timeIn);
      String time = edz[1];
      String zone = edz[2];
      if (time == null || zone == null)
        return EMPTY_STR;

      String[] formatsIn = {dt, d, t};
      String formatOut =  t;
      Date date = testFormats(time, formatsIn);
      if (date == null) return EMPTY_STR;
      SimpleDateFormat dateFormat = new SimpleDateFormat(formatOut);
      String out = dateFormat.format(date);
      return (out + zone);
    }

    /**
     * See above.
     */
    public static String time()
    {
      String datetime = dateTime().toString();
      String time = datetime.substring(datetime.indexOf("T")+1);

          // The datetime() function returns the zone on the datetime string.  If we
          // append it, we get the zone substring duplicated.
          // Fix for JIRA 2013

      // String zone = datetime.substring(getZoneStart(datetime));
      // return (time + zone);
      return (time);
    }

    /**
     * The date:year function returns the year of a date as a number. If no
     * argument is given, then the current local date/time, as returned by
     * date:date-time is used as a default argument.
     * The date/time string specified as the first argument must be a right-truncated
     * string in the format defined as the lexical representation of xs:dateTime in one
     * of the formats defined in
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>.
     * The permitted formats are as follows:
     *   xs:dateTime (CCYY-MM-DDThh:mm:ss)
     *   xs:date (CCYY-MM-DD)
     *   xs:gYearMonth (CCYY-MM)
     *   xs:gYear (CCYY)
     * If the date/time string is not in one of these formats, then NaN is returned.
     */
    public static double year(String datetimeIn)
      throws ParseException
    {
      String[] edz = getEraDatetimeZone(datetimeIn);
      boolean ad = edz[0].length() == 0; // AD (Common Era -- empty leader)
      String datetime = edz[1];
      if (datetime == null)
        return Double.NaN;

      String[] formats = {dt, d, gym, gy};
      double yr = getNumber(datetime, formats, Calendar.YEAR);
      if (ad || yr == Double.NaN)
        return yr;
      else
        return -yr;
    }

    /**
     * See above.
     */
    public static double year()
    {
      Calendar cal = Calendar.getInstance();
      return cal.get(Calendar.YEAR);
    }

    /**
     * The date:month-in-year function returns the month of a date as a number. If no argument
     * is given, then the current local date/time, as returned by date:date-time is used
     * as a default argument.
     * The date/time string specified as the first argument is a left or right-truncated
     * string in the format defined as the lexical representation of xs:dateTime in one of
     * the formats defined in
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>.
     * The permitted formats are as follows:
     *    xs:dateTime (CCYY-MM-DDThh:mm:ss)
     *    xs:date (CCYY-MM-DD)
     *    xs:gYearMonth (CCYY-MM)
     *    xs:gMonth (--MM--)
     *    xs:gMonthDay (--MM-DD)
     * If the date/time string is not in one of these formats, then NaN is returned.
     */
    public static double monthInYear(String datetimeIn)
      throws ParseException
    {
      String[] edz = getEraDatetimeZone(datetimeIn);
      String datetime = edz[1];
      if (datetime == null)
        return Double.NaN;

      String[] formats = {dt, d, gym, gm, gmd};
      return getNumber(datetime, formats, Calendar.MONTH) + 1;
    }

    /**
     * See above.
     */
    public static double monthInYear()
    {
      Calendar cal = Calendar.getInstance();
      return cal.get(Calendar.MONTH) + 1;
   }

    /**
     * The date:week-in-year function returns the week of the year as a number. If no argument
     * is given, then the current local date/time, as returned by date:date-time is used as the
     * default argument. For the purposes of numbering, counting follows ISO 8601: week 1 in a year
     * is the week containing the first Thursday of the year, with new weeks beginning on a Monday.
     * The date/time string specified as the argument is a right-truncated string in the format
     * defined as the lexical representation of xs:dateTime in one of the formats defined in
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>. The
     * permitted formats are as follows:
     *    xs:dateTime (CCYY-MM-DDThh:mm:ss)
     *    xs:date (CCYY-MM-DD)
     * If the date/time string is not in one of these formats, then NaN is returned.
     */
    public static double weekInYear(String datetimeIn)
      throws ParseException
    {
      String[] edz = getEraDatetimeZone(datetimeIn);
      String datetime = edz[1];
      if (datetime == null)
        return Double.NaN;

      String[] formats = {dt, d};
      return getNumber(datetime, formats, Calendar.WEEK_OF_YEAR);
    }

    /**
     * See above.
     */
    public static double weekInYear()
    {
       Calendar cal = Calendar.getInstance();
      return cal.get(Calendar.WEEK_OF_YEAR);
   }

    /**
     * The date:day-in-year function returns the day of a date in a year
     * as a number. If no argument is given, then the current local
     * date/time, as returned by date:date-time is used the default argument.
     * The date/time string specified as the argument is a right-truncated
     * string in the format defined as the lexical representation of xs:dateTime
     * in one of the formats defined in
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>.
     * The permitted formats are as follows:
     *     xs:dateTime (CCYY-MM-DDThh:mm:ss)
     *     xs:date (CCYY-MM-DD)
     * If the date/time string is not in one of these formats, then NaN is returned.
     */
    public static double dayInYear(String datetimeIn)
      throws ParseException
    {
      String[] edz = getEraDatetimeZone(datetimeIn);
      String datetime = edz[1];
      if (datetime == null)
        return Double.NaN;

      String[] formats = {dt, d};
      return getNumber(datetime, formats, Calendar.DAY_OF_YEAR);
    }

    /**
     * See above.
     */
    public static double dayInYear()
    {
       Calendar cal = Calendar.getInstance();
      return cal.get(Calendar.DAY_OF_YEAR);
   }


    /**
     * The date:day-in-month function returns the day of a date as a number.
     * If no argument is given, then the current local date/time, as returned
     * by date:date-time is used the default argument.
     * The date/time string specified as the argument is a left or right-truncated
     * string in the format defined as the lexical representation of xs:dateTime
     * in one of the formats defined in
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>.
     * The permitted formats are as follows:
     *      xs:dateTime (CCYY-MM-DDThh:mm:ss)
     *      xs:date (CCYY-MM-DD)
     *      xs:gMonthDay (--MM-DD)
     *      xs:gDay (---DD)
     * If the date/time string is not in one of these formats, then NaN is returned.
     */
    public static double dayInMonth(String datetimeIn)
      throws ParseException
    {
      String[] edz = getEraDatetimeZone(datetimeIn);
      String datetime = edz[1];
      String[] formats = {dt, d, gmd, gd};
      double day = getNumber(datetime, formats, Calendar.DAY_OF_MONTH);
      return day;
    }

    /**
     * See above.
     */
    public static double dayInMonth()
    {
      Calendar cal = Calendar.getInstance();
      return cal.get(Calendar.DAY_OF_MONTH);
   }

    /**
     * The date:day-of-week-in-month function returns the day-of-the-week
     * in a month of a date as a number (e.g. 3 for the 3rd Tuesday in May).
     * If no argument is given, then the current local date/time, as returned
     * by date:date-time is used the default argument.
     * The date/time string specified as the argument is a right-truncated string
     * in the format defined as the lexical representation of xs:dateTime in one
     * of the formats defined in
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>.
     * The permitted formats are as follows:
     *      xs:dateTime (CCYY-MM-DDThh:mm:ss)
     *      xs:date (CCYY-MM-DD)
     * If the date/time string is not in one of these formats, then NaN is returned.
     */
    public static double dayOfWeekInMonth(String datetimeIn)
      throws ParseException
    {
      String[] edz = getEraDatetimeZone(datetimeIn);
      String datetime = edz[1];
      if (datetime == null)
        return Double.NaN;

      String[] formats =  {dt, d};
      return getNumber(datetime, formats, Calendar.DAY_OF_WEEK_IN_MONTH);
    }

    /**
     * See above.
     */
    public static double dayOfWeekInMonth()
    {
       Calendar cal = Calendar.getInstance();
      return cal.get(Calendar.DAY_OF_WEEK_IN_MONTH);
   }


    /**
     * The date:day-in-week function returns the day of the week given in a
     * date as a number. If no argument is given, then the current local date/time,
     * as returned by date:date-time is used the default argument.
     * The date/time string specified as the argument is a right-truncated string
     * in the format defined as the lexical representation of xs:dateTime in one
     * of the formats defined in
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>.
     * The permitted formats are as follows:
     *      xs:dateTime (CCYY-MM-DDThh:mm:ss)
     *      xs:date (CCYY-MM-DD)
     * If the date/time string is not in one of these formats, then NaN is returned.
                            The numbering of days of the week starts at 1 for Sunday, 2 for Monday and so on up to 7 for Saturday.
     */
    public static double dayInWeek(String datetimeIn)
      throws ParseException
    {
      String[] edz = getEraDatetimeZone(datetimeIn);
      String datetime = edz[1];
      if (datetime == null)
        return Double.NaN;

      String[] formats = {dt, d};
      return getNumber(datetime, formats, Calendar.DAY_OF_WEEK);
    }

    /**
     * See above.
     */
    public static double dayInWeek()
    {
       Calendar cal = Calendar.getInstance();
      return cal.get(Calendar.DAY_OF_WEEK);
   }

    /**
     * The date:hour-in-day function returns the hour of the day as a number.
     * If no argument is given, then the current local date/time, as returned
     * by date:date-time is used the default argument.
     * The date/time string specified as the argument is a right-truncated
     * string  in the format defined as the lexical representation of xs:dateTime
     * in one of the formats defined in
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>.
     * The permitted formats are as follows:
     *     xs:dateTime (CCYY-MM-DDThh:mm:ss)
     *     xs:time (hh:mm:ss)
     * If the date/time string is not in one of these formats, then NaN is returned.
     */
    public static double hourInDay(String datetimeIn)
      throws ParseException
    {
      String[] edz = getEraDatetimeZone(datetimeIn);
      String datetime = edz[1];
      if (datetime == null)
        return Double.NaN;

      String[] formats = {dt, t};
      return getNumber(datetime, formats, Calendar.HOUR_OF_DAY);
    }

    /**
     * See above.
     */
    public static double hourInDay()
    {
       Calendar cal = Calendar.getInstance();
      return cal.get(Calendar.HOUR_OF_DAY);
   }

    /**
     * The date:minute-in-hour function returns the minute of the hour
     * as a number. If no argument is given, then the current local
     * date/time, as returned by date:date-time is used the default argument.
     * The date/time string specified as the argument is a right-truncated
     * string in the format defined as the lexical representation of xs:dateTime
     * in one of the formats defined in
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>.
     * The permitted formats are as follows:
     *      xs:dateTime (CCYY-MM-DDThh:mm:ss)
     *      xs:time (hh:mm:ss)
     * If the date/time string is not in one of these formats, then NaN is returned.
     */
    public static double minuteInHour(String datetimeIn)
      throws ParseException
    {
      String[] edz = getEraDatetimeZone(datetimeIn);
      String datetime = edz[1];
      if (datetime == null)
        return Double.NaN;

      String[] formats = {dt,t};
      return getNumber(datetime, formats, Calendar.MINUTE);
    }

    /**
     * See above.
     */
   public static double minuteInHour()
    {
       Calendar cal = Calendar.getInstance();
      return cal.get(Calendar.MINUTE);
   }

    /**
     * The date:second-in-minute function returns the second of the minute
     * as a number. If no argument is given, then the current local
     * date/time, as returned by date:date-time is used the default argument.
     * The date/time string specified as the argument is a right-truncated
     * string in the format defined as the lexical representation of xs:dateTime
     * in one of the formats defined in
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>.
     * The permitted formats are as follows:
     *      xs:dateTime (CCYY-MM-DDThh:mm:ss)
     *      xs:time (hh:mm:ss)
     * If the date/time string is not in one of these formats, then NaN is returned.
     */
    public static double secondInMinute(String datetimeIn)
      throws ParseException
    {
      String[] edz = getEraDatetimeZone(datetimeIn);
      String datetime = edz[1];
      if (datetime == null)
        return Double.NaN;

      String[] formats = {dt, t};
      return getNumber(datetime, formats, Calendar.SECOND);
    }

    /**
     * See above.
     */
    public static double secondInMinute()
    {
       Calendar cal = Calendar.getInstance();
      return cal.get(Calendar.SECOND);
    }

    /**
     * The date:leap-year function returns true if the year given in a date
     * is a leap year. If no argument is given, then the current local
     * date/time, as returned by date:date-time is used as a default argument.
     * The date/time string specified as the first argument must be a
     * right-truncated string in the format defined as the lexical representation
     * of xs:dateTime in one of the formats defined in
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>.
     * The permitted formats are as follows:
     *    xs:dateTime (CCYY-MM-DDThh:mm:ss)
     *    xs:date (CCYY-MM-DD)
     *    xs:gYearMonth (CCYY-MM)
     *    xs:gYear (CCYY)
     * If the date/time string is not in one of these formats, then NaN is returned.
     */
    public static XObject leapYear(String datetimeIn)
      throws ParseException
    {
      String[] edz = getEraDatetimeZone(datetimeIn);
      String datetime = edz[1];
      if (datetime == null)
        return new XNumber(Double.NaN);

      String[] formats = {dt, d, gym, gy};
      double dbl = getNumber(datetime, formats, Calendar.YEAR);
      if (dbl == Double.NaN)
        return new XNumber(Double.NaN);
      int yr = (int)dbl;
      return new XBoolean(yr % 400 == 0 || (yr % 100 != 0 && yr % 4 == 0));
    }

    /**
     * See above.
     */
    public static boolean leapYear()
    {
      Calendar cal = Calendar.getInstance();
      int yr = cal.get(Calendar.YEAR);
      return (yr % 400 == 0 || (yr % 100 != 0 && yr % 4 == 0));
    }

    /**
     * The date:month-name function returns the full name of the month of a date.
     * If no argument is given, then the current local date/time, as returned by
     * date:date-time is used the default argument.
     * The date/time string specified as the argument is a left or right-truncated
     * string in the format defined as the lexical representation of xs:dateTime in
     *  one of the formats defined in
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>.
     * The permitted formats are as follows:
     *    xs:dateTime (CCYY-MM-DDThh:mm:ss)
     *    xs:date (CCYY-MM-DD)
     *    xs:gYearMonth (CCYY-MM)
     *    xs:gMonth (--MM--)
     * If the date/time string is not in one of these formats, then an empty string ('')
     * is returned.
     * The result is an English month name: one of 'January', 'February', 'March',
     * 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November'
     * or 'December'.
     */
    public static String monthName(String datetimeIn)
      throws ParseException
    {
      String[] edz = getEraDatetimeZone(datetimeIn);
      String datetime = edz[1];
      if (datetime == null)
        return EMPTY_STR;

      String[] formatsIn = {dt, d, gym, gm};
      String formatOut = "MMMM";
      return getNameOrAbbrev(datetimeIn, formatsIn, formatOut);
    }

    /**
     * See above.
     */
    public static String monthName()
    {
      Calendar cal = Calendar.getInstance();
      String format = "MMMM";
      return getNameOrAbbrev(format);
    }

    /**
     * The date:month-abbreviation function returns the abbreviation of the month of
     * a date. If no argument is given, then the current local date/time, as returned
     * by date:date-time is used the default argument.
     * The date/time string specified as the argument is a left or right-truncated
     * string in the format defined as the lexical representation of xs:dateTime in
     * one of the formats defined in
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>.
     * The permitted formats are as follows:
     *    xs:dateTime (CCYY-MM-DDThh:mm:ss)
     *    xs:date (CCYY-MM-DD)
     *    xs:gYearMonth (CCYY-MM)
     *    xs:gMonth (--MM--)
     * If the date/time string is not in one of these formats, then an empty string ('')
     * is returned.
     * The result is a three-letter English month abbreviation: one of 'Jan', 'Feb', 'Mar',
     * 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov' or 'Dec'.
     * An implementation of this extension function in the EXSLT date namespace must conform
     * to the behaviour described in this document.
     */
    public static String monthAbbreviation(String datetimeIn)
      throws ParseException
    {
      String[] edz = getEraDatetimeZone(datetimeIn);
      String datetime = edz[1];
      if (datetime == null)
        return EMPTY_STR;

      String[] formatsIn = {dt, d, gym, gm};
      String formatOut = "MMM";
      return getNameOrAbbrev(datetimeIn, formatsIn, formatOut);
    }

    /**
     * See above.
     */
    public static String monthAbbreviation()
    {
      String format = "MMM";
      return getNameOrAbbrev(format);
    }

    /**
     * The date:day-name function returns the full name of the day of the week
     * of a date.  If no argument is given, then the current local date/time,
     * as returned by date:date-time is used the default argument.
     * The date/time string specified as the argument is a left or right-truncated
     * string in the format defined as the lexical representation of xs:dateTime
     * in one of the formats defined in
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>.
     * The permitted formats are as follows:
     *     xs:dateTime (CCYY-MM-DDThh:mm:ss)
     *     xs:date (CCYY-MM-DD)
     * If the date/time string is not in one of these formats, then the empty string ('')
     * is returned.
     * The result is an English day name: one of 'Sunday', 'Monday', 'Tuesday', 'Wednesday',
     * 'Thursday' or 'Friday'.
     * An implementation of this extension function in the EXSLT date namespace must conform
     * to the behaviour described in this document.
     */
    public static String dayName(String datetimeIn)
      throws ParseException
    {
      String[] edz = getEraDatetimeZone(datetimeIn);
      String datetime = edz[1];
      if (datetime == null)
        return EMPTY_STR;

      String[] formatsIn = {dt, d};
      String formatOut = "EEEE";
      return getNameOrAbbrev(datetimeIn, formatsIn, formatOut);
    }

    /**
     * See above.
     */
    public static String dayName()
    {
      String format = "EEEE";
      return getNameOrAbbrev(format);
    }

    /**
     * The date:day-abbreviation function returns the abbreviation of the day
     * of the week of a date. If no argument is given, then the current local
     * date/time, as returned  by date:date-time is used the default argument.
     * The date/time string specified as the argument is a left or right-truncated
     * string in the format defined as the lexical representation of xs:dateTime
     * in one of the formats defined in
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>.
     * The permitted formats are as follows:
     *     xs:dateTime (CCYY-MM-DDThh:mm:ss)
     *     xs:date (CCYY-MM-DD)
     * If the date/time string is not in one of these formats, then the empty string
     * ('') is returned.
     * The result is a three-letter English day abbreviation: one of 'Sun', 'Mon', 'Tue',
     * 'Wed', 'Thu' or 'Fri'.
     * An implementation of this extension function in the EXSLT date namespace must conform
     * to the behaviour described in this document.
     */
    public static String dayAbbreviation(String datetimeIn)
      throws ParseException
    {
      String[] edz = getEraDatetimeZone(datetimeIn);
      String datetime = edz[1];
      if (datetime == null)
        return EMPTY_STR;

      String[] formatsIn = {dt, d};
      String formatOut = "EEE";
      return getNameOrAbbrev(datetimeIn, formatsIn, formatOut);
    }

    /**
     * See above.
     */
    public static String dayAbbreviation()
    {
      String format = "EEE";
      return getNameOrAbbrev(format);
    }

    /**
     * Returns an array with the 3 components that a datetime input string
     * may contain: - (for BC era), datetime, and zone. If the zone is not
     * valid, return null for that component.
     */
    private static String[] getEraDatetimeZone(String in)
    {
      String leader = "";
      String datetime = in;
      String zone = "";
      if (in.charAt(0)=='-' && !in.startsWith("--"))
      {
        leader = "-"; //  '+' is implicit , not allowed
        datetime = in.substring(1);
      }
      int z = getZoneStart(datetime);
      if (z > 0)
      {
        zone = datetime.substring(z);
        datetime = datetime.substring(0, z);
      }
      else if (z == -2)
        zone = null;
      //System.out.println("'" + leader + "' " + datetime + " " + zone);
      return new String[]{leader, datetime, zone};
    }

    /**
     * Get the start of zone information if the input ends
     * with 'Z' or +/-hh:mm. If a zone string is not
     * found, return -1; if the zone string is invalid,
     * return -2.
     */
    private static int getZoneStart (String datetime)
    {
      if (datetime.indexOf("Z") == datetime.length()-1)
        return datetime.length()-1;
      else if (datetime.length() >=6
                && datetime.charAt(datetime.length()-3) == ':'
                && (datetime.charAt(datetime.length()-6) == '+'
                    || datetime.charAt(datetime.length()-6) == '-'))
      {
        try
        {
          SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
          dateFormat.setLenient(false);
          Date d = dateFormat.parse(datetime.substring(datetime.length() -5));
          return datetime.length()-6;
        }
        catch (ParseException pe)
        {
          System.out.println("ParseException " + pe.getErrorOffset());
          return -2; // Invalid.
        }

      }
        return -1; // No zone information.
    }

    /**
     * Attempt to parse an input string with the allowed formats, returning
     * null if none of the formats work.
     */
    private static Date testFormats (String in, String[] formats)
      throws ParseException
    {
      for (int i = 0; i <formats.length; i++)
      {
        try
        {
          SimpleDateFormat dateFormat = new SimpleDateFormat(formats[i]);
          dateFormat.setLenient(false);
          return dateFormat.parse(in);
        }
        catch (ParseException pe)
        {
        }
      }
      return null;
    }


    /**
     * Parse the input string and return the corresponding calendar field
     * number.
     */
    private static double getNumber(String in, String[] formats, int calField)
      throws ParseException
    {
      Calendar cal = Calendar.getInstance();
      cal.setLenient(false);
      // Try the allowed formats, from longest to shortest.
      Date date = testFormats(in, formats);
      if (date == null) return Double.NaN;
      cal.setTime(date);
      return cal.get(calField);
    }

    /**
     *  Get the full name or abbreviation of the month or day.
     */
    private static String getNameOrAbbrev(String in,
                                         String[] formatsIn,
                                         String formatOut)
      throws ParseException
    {
      for (int i = 0; i <formatsIn.length; i++) // from longest to shortest.
      {
        try
        {
          SimpleDateFormat dateFormat = new SimpleDateFormat(formatsIn[i], Locale.ENGLISH);
          dateFormat.setLenient(false);
          Date dt = dateFormat.parse(in);
          dateFormat.applyPattern(formatOut);
          return dateFormat.format(dt);
        }
        catch (ParseException pe)
        {
        }
      }
      return "";
    }
    /**
     * Get the full name or abbreviation for the current month or day
     * (no input string).
     */
    private static String getNameOrAbbrev(String format)
    {
      Calendar cal = Calendar.getInstance();
      SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.ENGLISH);
      return dateFormat.format(cal.getTime());
    }

    /**
     * The date:format-date function formats a date/time according to a pattern.
     * <p>
     * The first argument to date:format-date specifies the date/time to be
     * formatted. It must be right or left-truncated date/time strings in one of
     * the formats defined in
     * <a href="http://www.w3.org/TR/xmlschema-2/">[XML Schema Part 2: Datatypes]</a>.
     * The permitted formats are as follows:
     * <ul>
     * <li>xs:dateTime (CCYY-MM-DDThh:mm:ss)
     * <li>xs:date (CCYY-MM-DD)
     * <li>xs:time (hh:mm:ss)
     * <li>xs:gYearMonth (CCYY-MM)
     * <li>xs:gYear (CCYY)
     * <li>xs:gMonthDay (--MM-DD)
     * <li>xs:gMonth (--MM--)
     * <li>xs:gDay (---DD)
     * </ul>
     * The second argument is a string that gives the format pattern used to
     * format the date. The format pattern must be in the syntax specified by
     * the JDK 1.1 SimpleDateFormat class. The format pattern string is
     * interpreted as described for the JDK 1.1 SimpleDateFormat class.
     * <p>
     * If the date/time format is right-truncated (i.e. in a format other than
     * xs:time, or xs:dateTime) then any missing components are assumed to be as
     * follows: if no month is specified, it is given a month of 01; if no day
     * is specified, it is given a day of 01; if no time is specified, it is
     * given a time of 00:00:00.
     * <p>
     * If the date/time format is left-truncated (i.e. xs:time, xs:gMonthDay,
     * xs:gMonth or xs:gDay) and the format pattern has a token that uses a
     * component that is missing from the date/time format used, then that token
     * is replaced with an empty string ('') within the result.
     *
     * The author is Helg Bredow (helg.bredow@kalido.com)
     */
    public static String formatDate(String dateTime, String pattern)
    {
        final String yearSymbols = "Gy";
        final String monthSymbols = "M";
        final String daySymbols = "dDEFwW";
        TimeZone timeZone;
        String zone;

        // Get the timezone information if it was supplied and modify the
        // dateTime so that SimpleDateFormat will understand it.
        if (dateTime.endsWith("Z") || dateTime.endsWith("z"))
        {
            timeZone = TimeZone.getTimeZone("GMT");
            dateTime = dateTime.substring(0, dateTime.length()-1) + "GMT";
            zone = "z";
        }
        else if ((dateTime.length() >= 6)
                 && (dateTime.charAt(dateTime.length()-3) == ':')
                 && ((dateTime.charAt(dateTime.length()-6) == '+')
                    || (dateTime.charAt(dateTime.length()-6) == '-')))
        {
            String offset = dateTime.substring(dateTime.length()-6);

            if ("+00:00".equals(offset) || "-00:00".equals(offset))
            {
                timeZone = TimeZone.getTimeZone("GMT");
            }
            else
            {
                timeZone = TimeZone.getTimeZone("GMT" + offset);
            }
            zone = "z";
            // Need to adjust it since SimpleDateFormat requires GMT+hh:mm but
            // we have +hh:mm.
            dateTime = dateTime.substring(0, dateTime.length()-6) + "GMT" + offset;
        }
        else
        {
            // Assume local time.
            timeZone = TimeZone.getDefault();
            zone = "";
            // Leave off the timezone since SimpleDateFormat will assume local
            // time if time zone is not included.
        }
        String[] formats = {dt + zone, d, gym, gy};

        // Try the time format first. We need to do this to prevent
        // SimpleDateFormat from interpreting a time as a year. i.e we just need
        // to check if it's a time before we check it's a year.
        try
        {
            SimpleDateFormat inFormat = new SimpleDateFormat(t + zone);
            inFormat.setLenient(false);
            Date d= inFormat.parse(dateTime);
            SimpleDateFormat outFormat = new SimpleDateFormat(strip
                (yearSymbols + monthSymbols + daySymbols, pattern));
            outFormat.setTimeZone(timeZone);
            return outFormat.format(d);
        }
        catch (ParseException pe)
        {
        }

        // Try the right truncated formats.
        for (int i = 0; i < formats.length; i++)
        {
            try
            {
                SimpleDateFormat inFormat = new SimpleDateFormat(formats[i]);
                inFormat.setLenient(false);
                Date d = inFormat.parse(dateTime);
                SimpleDateFormat outFormat = new SimpleDateFormat(pattern);
                outFormat.setTimeZone(timeZone);
                return outFormat.format(d);
            }
            catch (ParseException pe)
            {
            }
        }

        // Now try the left truncated ones. The Java format() function doesn't
        // return the correct strings in this case. We strip any pattern
        // symbols that shouldn't be output so that they are not defaulted to
        // inappropriate values in the output.
        try
        {
            SimpleDateFormat inFormat = new SimpleDateFormat(gmd);
            inFormat.setLenient(false);
            Date d = inFormat.parse(dateTime);
            SimpleDateFormat outFormat = new SimpleDateFormat(strip(yearSymbols, pattern));
            outFormat.setTimeZone(timeZone);
            return outFormat.format(d);
        }
        catch (ParseException pe)
        {
        }
        try
        {
            SimpleDateFormat inFormat = new SimpleDateFormat(gm);
            inFormat.setLenient(false);
            Date d = inFormat.parse(dateTime);
            SimpleDateFormat outFormat = new SimpleDateFormat(strip(yearSymbols, pattern));
            outFormat.setTimeZone(timeZone);
            return outFormat.format(d);
        }
        catch (ParseException pe)
        {
        }
        try
        {
            SimpleDateFormat inFormat = new SimpleDateFormat(gd);
            inFormat.setLenient(false);
            Date d = inFormat.parse(dateTime);
            SimpleDateFormat outFormat = new SimpleDateFormat(strip(yearSymbols + monthSymbols, pattern));
            outFormat.setTimeZone(timeZone);
            return outFormat.format(d);
        }
        catch (ParseException pe)
        {
        }
        return EMPTY_STR;
    }

    /**
     * Strips occurrences of the given character from a date format pattern.
     * @param symbols list of symbols to strip.
     * @param pattern
     * @return
     */
    private static String strip(String symbols, String pattern)
    {
        int quoteSemaphore = 0;
        int i = 0;
        StringBuffer result = new StringBuffer(pattern.length());

        while (i < pattern.length())
        {
            char ch = pattern.charAt(i);
            if (ch == '\'')
            {
                // Assume it's an openening quote so simply copy the quoted
                // text to the result. There is nothing to strip here.
                int endQuote = pattern.indexOf('\'', i + 1);
                if (endQuote == -1)
                {
                    endQuote = pattern.length();
                }
                result.append(pattern.substring(i, endQuote));
                i = endQuote++;
            }
            else if (symbols.indexOf(ch) > -1)
            {
                // The char needs to be stripped.
                i++;
            }
            else
            {
                result.append(ch);
                i++;
            }
        }
        return result.toString();
    }

}
