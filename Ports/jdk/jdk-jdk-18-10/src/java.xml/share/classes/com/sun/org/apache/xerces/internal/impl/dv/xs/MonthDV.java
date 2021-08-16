/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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

package com.sun.org.apache.xerces.internal.impl.dv.xs;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeValueException;
import com.sun.org.apache.xerces.internal.impl.dv.ValidationContext;

/**
 * Validator for &lt;gMonth&gt; datatype (W3C Schema Datatypes)
 *
 * @xerces.internal
 *
 * @author Elena Litani
 * @author Gopal Sharma, SUN Microsystem Inc.
 *
 */

public class MonthDV extends AbstractDateTimeDV {

    /**
     * Convert a string to a compiled form
     *
     * @param  content The lexical representation of gMonth
     * @return a valid and normalized gMonth object
     */
    public Object getActualValue(String content, ValidationContext context) throws InvalidDatatypeValueException{
        try{
            return parse(content);
        } catch(Exception ex){
            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{content, "gMonth"});
        }
    }

    /**
     * Parses, validates and computes normalized version of gMonth object
     *
     * @param str    The lexical representation of gMonth object --MM
     *               with possible time zone Z or (-),(+)hh:mm
     * @return normalized date representation
     * @exception SchemaDateTimeException Invalid lexical representation
     */
    protected DateTimeData parse(String str) throws SchemaDateTimeException{
        DateTimeData date = new DateTimeData(str, this);
        int len = str.length();

        //set constants
        date.year=YEAR;
        date.day=DAY;
        if (str.charAt(0)!='-' || str.charAt(1)!='-') {
            throw new SchemaDateTimeException("Invalid format for gMonth: "+str);
        }
        int stop = 4;
        date.month=parseInt(str,2,stop);

        // REVISIT: allow both --MM and --MM-- now.
        // need to remove the following 4 lines to disallow --MM--
        // when the errata is offically in the rec.
        if (str.length() >= stop+2 &&
            str.charAt(stop) == '-' && str.charAt(stop+1) == '-') {
            stop += 2;
        }
        if (stop < len) {
            if (!isNextCharUTCSign(str, stop, len)) {
                throw new SchemaDateTimeException ("Error in month parsing: "+str);
            }
            else {
                getTimeZone(str, date, stop, len);
            }
        }
        //validate and normalize
        validateDateTime(date);

        //save unnormalized values
        saveUnnormalized(date);

        if ( date.utc!=0 && date.utc!='Z' ) {
            normalize(date);
        }
        date.position = 1;
        return date;
    }

    /**
     * Overwrite compare algorithm to optimize month comparison
     *
     * REVISIT: this one is lack of the third parameter: boolean strict, so it
     *          doesn't override the method in the base. But maybe this method
     *          is not correctly implemented, and I did encounter errors when
     *          trying to add the extra parameter. I'm leaving it as is. -SG
     *
     * @param date1
     * @param date2
     * @return less, greater, equal, indeterminate
     */
    /*protected  short compareDates(DateTimeData date1, DateTimeData date2) {

        if ( date1.utc==date2.utc ) {
            return (short)((date1.month>=date2.month)?(date1.month>date2.month)?1:0:-1);
        }

        if ( date1.utc=='Z' || date2.utc=='Z' ) {

            if ( date1.month==date2.month ) {
                //--05--Z and --05--
                return INDETERMINATE;
            }
            if ( (date1.month+1 == date2.month || date1.month-1 == date2.month) ) {
                //--05--Z and (--04-- or --05--)
                //REVISIT: should this case be less than or equal?
                //         maxExclusive should fail but what about maxInclusive
                //
                return INDETERMINATE;
            }
        }

        if ( date1.month<date2.month ) {
            return -1;
        }
        else {
            return 1;
        }

    }*/

    /**
     * Converts month object representation to String
     *
     * @param date   month object
     * @return lexical representation of month: --MM with an optional time zone sign
     */
    protected String dateToString(DateTimeData date) {
        StringBuffer message = new StringBuffer(5);
        message.append('-');
        message.append('-');
        append(message, date.month, 2);
        append(message, (char)date.utc, 0);
        return message.toString();
    }

    protected XMLGregorianCalendar getXMLGregorianCalendar(DateTimeData date) {
        return datatypeFactory.newXMLGregorianCalendar(DatatypeConstants.FIELD_UNDEFINED, date.unNormMonth,
                DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED,
                date.hasTimeZone() ? date.timezoneHr * 60 + date.timezoneMin : DatatypeConstants.FIELD_UNDEFINED);
    }
}
