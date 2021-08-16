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
 * Validator for &lt;gDay&gt; datatype (W3C Schema datatypes)
 *
 * @xerces.internal
 *
 * @author Elena Litani
 * @author Gopal Sharma, SUN Microsystem Inc.
 */
public class DayDV extends AbstractDateTimeDV {

    //size without time zone: ---09
    private final static int DAY_SIZE=5;

    public Object getActualValue(String content, ValidationContext context) throws InvalidDatatypeValueException {
        try{
            return parse(content);
        } catch(Exception ex){
            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{content, "gDay"});
        }
    }

    /**
     * Parses, validates and computes normalized version of gDay object
     *
     * @param str    The lexical representation of gDay object ---DD
     *               with possible time zone Z or (-),(+)hh:mm
     *               Pattern: ---(\\d\\d)(Z|(([-+])(\\d\\d)(:(\\d\\d))?
     * @return normalized date representation
     * @exception SchemaDateTimeException Invalid lexical representation
     */
    protected DateTimeData parse(String str) throws SchemaDateTimeException {
        DateTimeData date = new DateTimeData(str, this);
        int len = str.length();

        if (str.charAt(0)!='-' || str.charAt(1)!='-' || str.charAt(2)!='-') {
            throw new SchemaDateTimeException ("Error in day parsing");
        }

        //initialize values
        date.year=YEAR;
        date.month=MONTH;

        date.day=parseInt(str, 3,5);

        if ( DAY_SIZE<len ) {
            if (!isNextCharUTCSign(str, DAY_SIZE, len)) {
                throw new SchemaDateTimeException ("Error in day parsing");
            }
            else {
                getTimeZone(str, date, DAY_SIZE, len);
            }
        }

       //validate and normalize
        validateDateTime(date);

        //save unnormalized values
        saveUnnormalized(date);

        if ( date.utc!=0 && date.utc!='Z' ) {
            normalize(date);
        }
        date.position = 2;
        return date;
    }

    /**
     * Converts gDay object representation to String
     *
     * @param date   gDay object
     * @return lexical representation of gDay: ---DD with an optional time zone sign
     */
    protected String dateToString(DateTimeData date) {
        StringBuffer message = new StringBuffer(6);
        message.append('-');
        message.append('-');
        message.append('-');
        append(message, date.day, 2);
        append(message, (char)date.utc, 0);
        return message.toString();
    }

    protected XMLGregorianCalendar getXMLGregorianCalendar(DateTimeData date) {
        return datatypeFactory.newXMLGregorianCalendar(DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED,
                date.unNormDay, DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED,
                date.hasTimeZone() ? date.timezoneHr * 60 + date.timezoneMin : DatatypeConstants.FIELD_UNDEFINED);
    }

}
