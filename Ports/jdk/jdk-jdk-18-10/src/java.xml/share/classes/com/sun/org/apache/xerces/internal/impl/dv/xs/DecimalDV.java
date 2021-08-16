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

import java.math.BigDecimal;
import java.math.BigInteger;

import com.sun.org.apache.xerces.internal.impl.dv.InvalidDatatypeValueException;
import com.sun.org.apache.xerces.internal.impl.dv.ValidationContext;
import com.sun.org.apache.xerces.internal.xs.datatypes.XSDecimal;
import java.util.Objects;

/**
 * Represent the schema type "decimal"
 *
 * @xerces.internal
 *
 * @author Neeraj Bajaj, Sun Microsystems, inc.
 * @author Sandy Gao, IBM
 *
 */
public class DecimalDV extends TypeValidator {

    @Override
    public final short getAllowedFacets(){
        return ( XSSimpleTypeDecl.FACET_PATTERN | XSSimpleTypeDecl.FACET_WHITESPACE | XSSimpleTypeDecl.FACET_ENUMERATION |XSSimpleTypeDecl.FACET_MAXINCLUSIVE |XSSimpleTypeDecl.FACET_MININCLUSIVE | XSSimpleTypeDecl.FACET_MAXEXCLUSIVE  | XSSimpleTypeDecl.FACET_MINEXCLUSIVE | XSSimpleTypeDecl.FACET_TOTALDIGITS | XSSimpleTypeDecl.FACET_FRACTIONDIGITS);
    }

    @Override
    public Object getActualValue(String content, ValidationContext context) throws InvalidDatatypeValueException {
        try {
            return new XDecimal(content);
        } catch (NumberFormatException nfe) {
            throw new InvalidDatatypeValueException("cvc-datatype-valid.1.2.1", new Object[]{content, "decimal"});
        }
    }

    @Override
    public final int compare(Object value1, Object value2){
        return ((XDecimal)value1).compareTo((XDecimal)value2);
    }

    @Override
    public final int getTotalDigits(Object value){
        return ((XDecimal)value).totalDigits;
    }

    @Override
    public final int getFractionDigits(Object value){
        return ((XDecimal)value).fracDigits;
    }

    // Avoid using the heavy-weight java.math.BigDecimal
    static final class XDecimal implements XSDecimal {
        // sign: 0 for vlaue 0; 1 for positive values; -1 for negative values
        int sign = 1;
        // total digits. >= 1
        int totalDigits = 0;
        // integer digits when sign != 0
        int intDigits = 0;
        // fraction digits when sign != 0
        int fracDigits = 0;
        // the string representing the integer part
        String ivalue = "";
        // the string representing the fraction part
        String fvalue = "";
        // whether the canonical form contains decimal point
        boolean integer = false;

        XDecimal(String content) throws NumberFormatException {
            initD(content);
        }
        XDecimal(String content, boolean integer) throws NumberFormatException {
            if (integer)
                initI(content);
            else
                initD(content);
        }
        void initD(String content) throws NumberFormatException {
            int len = content.length();
            if (len == 0)
                throw new NumberFormatException();

            // these 4 variables are used to indicate where the integre/fraction
            // parts start/end.
            int intStart = 0, intEnd = 0, fracStart = 0, fracEnd = 0;

            // Deal with leading sign symbol if present
            if (content.charAt(0) == '+') {
                // skip '+', so intStart should be 1
                intStart = 1;
            }
            else if (content.charAt(0) == '-') {
                // keep '-', so intStart is stil 0
                intStart = 1;
                sign = -1;
            }

            // skip leading zeroes in integer part
            int actualIntStart = intStart;
            while (actualIntStart < len && content.charAt(actualIntStart) == '0') {
                actualIntStart++;
            }

            // Find the ending position of the integer part
            for (intEnd = actualIntStart;
                 intEnd < len && TypeValidator.isDigit(content.charAt(intEnd));
                 intEnd++);

            // Not reached the end yet
            if (intEnd < len) {
                // the remaining part is not ".DDD", error
                if (content.charAt(intEnd) != '.')
                    throw new NumberFormatException();

                // fraction part starts after '.', and ends at the end of the input
                fracStart = intEnd + 1;
                fracEnd = len;
            }

            // no integer part, no fraction part, error.
            if (intStart == intEnd && fracStart == fracEnd)
                throw new NumberFormatException();

            // ignore trailing zeroes in fraction part
            while (fracEnd > fracStart && content.charAt(fracEnd-1) == '0') {
                fracEnd--;
            }

            // check whether there is non-digit characters in the fraction part
            for (int fracPos = fracStart; fracPos < fracEnd; fracPos++) {
                if (!TypeValidator.isDigit(content.charAt(fracPos)))
                    throw new NumberFormatException();
            }

            intDigits = intEnd - actualIntStart;
            fracDigits = fracEnd - fracStart;
            totalDigits = intDigits + fracDigits;

            if (intDigits > 0) {
                ivalue = content.substring(actualIntStart, intEnd);
                if (fracDigits > 0)
                    fvalue = content.substring(fracStart, fracEnd);
            }
            else {
                if (fracDigits > 0) {
                    fvalue = content.substring(fracStart, fracEnd);
                }
                else {
                    // ".00", treat it as "0"
                    sign = 0;
                }
            }
        }
        void initI(String content) throws NumberFormatException {
            int len = content.length();
            if (len == 0)
                throw new NumberFormatException();

            // these 2 variables are used to indicate where the integre start/end.
            int intStart = 0, intEnd = 0;

            // Deal with leading sign symbol if present
            if (content.charAt(0) == '+') {
                // skip '+', so intStart should be 1
                intStart = 1;
            }
            else if (content.charAt(0) == '-') {
                // keep '-', so intStart is stil 0
                intStart = 1;
                sign = -1;
            }

            // skip leading zeroes in integer part
            int actualIntStart = intStart;
            while (actualIntStart < len && content.charAt(actualIntStart) == '0') {
                actualIntStart++;
            }

            // Find the ending position of the integer part
            for (intEnd = actualIntStart;
                 intEnd < len && TypeValidator.isDigit(content.charAt(intEnd));
                 intEnd++);

            // Not reached the end yet, error
            if (intEnd < len)
                throw new NumberFormatException();

            // no integer part, error.
            if (intStart == intEnd)
                throw new NumberFormatException();

            intDigits = intEnd - actualIntStart;
            fracDigits = 0;
            totalDigits = intDigits;

            if (intDigits > 0) {
                ivalue = content.substring(actualIntStart, intEnd);
            }
            else {
                // "00", treat it as "0"
                sign = 0;
            }

            integer = true;
        }

        @Override
        public boolean equals(Object val) {
            if (val == this)
                return true;

            if (!(val instanceof XDecimal))
                return false;
            XDecimal oval = (XDecimal)val;

            if (sign != oval.sign)
               return false;
            if (sign == 0)
                return true;

            return intDigits == oval.intDigits && fracDigits == oval.fracDigits &&
                   ivalue.equals(oval.ivalue) && fvalue.equals(oval.fvalue);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 17 * hash + this.sign;
            if (this.sign == 0) return hash;
            hash = 17 * hash + this.intDigits;
            hash = 17 * hash + this.fracDigits;
            hash = 17 * hash + Objects.hashCode(this.ivalue);
            hash = 17 * hash + Objects.hashCode(this.fvalue);
            return hash;
        }

        public int compareTo(XDecimal val) {
            if (sign != val.sign)
                return sign > val.sign ? 1 : -1;
            if (sign == 0)
                return 0;
            return sign * intComp(val);
        }
        private int intComp(XDecimal val) {
            if (intDigits != val.intDigits)
                return intDigits > val.intDigits ? 1 : -1;
            int ret = ivalue.compareTo(val.ivalue);
            if (ret != 0)
                return ret > 0 ? 1 : -1;;
            ret = fvalue.compareTo(val.fvalue);
            return ret == 0 ? 0 : (ret > 0 ? 1 : -1);
        }

        private String canonical;
        @Override
        public synchronized String toString() {
            if (canonical == null) {
                makeCanonical();
            }
            return canonical;
        }

        private void makeCanonical() {
            if (sign == 0) {
                if (integer)
                    canonical = "0";
                else
                    canonical = "0.0";
                return;
            }
            if (integer && sign > 0) {
                canonical = ivalue;
                return;
            }
            // for -0.1, total digits is 1, so we need 3 extra spots
            final StringBuilder buffer = new StringBuilder(totalDigits+3);
            if (sign == -1)
                buffer.append('-');
            if (intDigits != 0)
                buffer.append(ivalue);
            else
                buffer.append('0');
            if (!integer) {
                buffer.append('.');
                if (fracDigits != 0) {
                    buffer.append(fvalue);
                }
                else {
                    buffer.append('0');
                }
            }
            canonical = buffer.toString();
        }

        @Override
        public BigDecimal getBigDecimal() {
            if (sign == 0) {
                return new BigDecimal(BigInteger.ZERO);
            }
            return new BigDecimal(toString());
        }

        @Override
        public BigInteger getBigInteger() throws NumberFormatException {
            if (fracDigits != 0) {
                throw new NumberFormatException();
            }
            if (sign == 0) {
                return BigInteger.ZERO;
            }
            if (sign == 1) {
                return new BigInteger(ivalue);
            }
            return new BigInteger("-" + ivalue);
        }

        @Override
        public long getLong() throws NumberFormatException {
            if (fracDigits != 0) {
                throw new NumberFormatException();
            }
            if (sign == 0) {
                return 0L;
            }
            if (sign == 1) {
                return Long.parseLong(ivalue);
            }
            return Long.parseLong("-" + ivalue);
        }

        @Override
        public int getInt() throws NumberFormatException {
            if (fracDigits != 0) {
                throw new NumberFormatException();
            }
            if (sign == 0) {
                return 0;
            }
            if (sign == 1) {
                return Integer.parseInt(ivalue);
            }
            return Integer.parseInt("-" + ivalue);
        }

        @Override
        public short getShort() throws NumberFormatException {
            if (fracDigits != 0) {
                throw new NumberFormatException();
            }
            if (sign == 0) {
                return 0;
            }
            if (sign == 1) {
                return Short.parseShort(ivalue);
            }
            return Short.parseShort("-" + ivalue);
        }

        @Override
        public byte getByte() throws NumberFormatException {
            if (fracDigits != 0) {
                throw new NumberFormatException();
            }
            if (sign == 0) {
                return 0;
            }
            if (sign == 1) {
                return Byte.parseByte(ivalue);
            }
            return Byte.parseByte("-" + ivalue);
        }
    }
} // class DecimalDV
