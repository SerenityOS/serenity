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

package com.sun.org.apache.xerces.internal.impl.dtd.models;


/**
 * This class is a very simple bitset class. The DFA content model code needs
 * to support a bit set, but the java BitSet class is way, way overkill. Our
 * bitset never needs to be expanded after creation, hash itself, etc...
 *
 * Since the vast majority of content models will never require more than 64
 * bits, and since allocation of anything in Java is expensive, this class
 * provides a hybrid implementation that uses two ints for instances that use
 * 64 bits or fewer. It has a byte array reference member which will only be
 * used if more than 64 bits are required.
 *
 * Note that the code that uses this class will never perform operations
 * on sets of different sizes, so that check does not have to be made here.
 *
 * @xerces.internal
 *
 */
// made this class public so it can be accessed by
// the XS content models from the schema package -neilg.
public class CMStateSet
{
    // -------------------------------------------------------------------
    //  Constructors
    // -------------------------------------------------------------------
    public CMStateSet(int bitCount)
    {
        // Store the required bit count and insure its legal
        fBitCount = bitCount;
        if (fBitCount < 0)
            throw new RuntimeException("ImplementationMessages.VAL_CMSI");

        //
        //  See if we need to allocate the byte array or whether we can live
        //  within the 64 bit high performance scheme.
        //
        if (fBitCount > 64)
        {
            fByteCount = fBitCount / 8;
            if (fBitCount % 8 != 0)
                fByteCount++;
            fByteArray = new byte[fByteCount];
        }

        // Init all the bits to zero
        zeroBits();
    }


    // -------------------------------------------------------------------
    //  Public inherited methods
    // -------------------------------------------------------------------
    public String toString()
    {
        StringBuffer strRet = new StringBuffer();
        try
        {
            strRet.append("{");
            for (int index = 0; index < fBitCount; index++)
            {
                if (getBit(index))
                    strRet.append(" " + index);
            }
            strRet.append(" }");
        }

        catch(RuntimeException exToCatch)
        {
            //
            //  We know this won't happen but we have to catch it to avoid it
            //  having to be in our 'throws' list.
            //
        }
        return strRet.toString();
    }


    // -------------------------------------------------------------------
    //  Package final methods
    // -------------------------------------------------------------------
// the XS content models from the schema package -neilg.
    public final void intersection(CMStateSet setToAnd)
    {
        if (fBitCount < 65)
        {
            fBits1 &= setToAnd.fBits1;
            fBits2 &= setToAnd.fBits2;
        }
         else
        {
            for (int index = fByteCount - 1; index >= 0; index--)
                fByteArray[index] &= setToAnd.fByteArray[index];
        }
    }

    public final boolean getBit(int bitToGet)
    {
        if (bitToGet >= fBitCount)
            throw new RuntimeException("ImplementationMessages.VAL_CMSI");

        if (fBitCount < 65)
        {
            final int mask = (0x1 << (bitToGet % 32));
            if (bitToGet < 32)
                return (fBits1 & mask) != 0;
            else
                return (fBits2 & mask) != 0;
        }
         else
        {
            // Create the mask and byte values
            final byte mask = (byte)(0x1 << (bitToGet % 8));
            final int ofs = bitToGet >> 3;

            // And access the right bit and byte
            return ((fByteArray[ofs] & mask) != 0);
        }
    }

    public final boolean isEmpty()
    {
        if (fBitCount < 65)
        {
            return ((fBits1 == 0) && (fBits2 == 0));
        }
         else
        {
            for (int index = fByteCount - 1; index >= 0; index--)
            {
                if (fByteArray[index] != 0)
                    return false;
            }
        }
        return true;
    }

    final boolean isSameSet(CMStateSet setToCompare)
    {
        if (fBitCount != setToCompare.fBitCount)
            return false;

        if (fBitCount < 65)
        {
            return ((fBits1 == setToCompare.fBits1)
            &&      (fBits2 == setToCompare.fBits2));
        }

        for (int index = fByteCount - 1; index >= 0; index--)
        {
            if (fByteArray[index] != setToCompare.fByteArray[index])
                return false;
        }
        return true;
    }

// the XS content models from the schema package -neilg.
    public final void union(CMStateSet setToOr)
    {
        if (fBitCount < 65)
        {
            fBits1 |= setToOr.fBits1;
            fBits2 |= setToOr.fBits2;
        }
         else
        {
            for (int index = fByteCount - 1; index >= 0; index--)
                fByteArray[index] |= setToOr.fByteArray[index];
        }
    }

    public final void setBit(int bitToSet)
    {
        if (bitToSet >= fBitCount)
            throw new RuntimeException("ImplementationMessages.VAL_CMSI");

        if (fBitCount < 65)
        {
            final int mask = (0x1 << (bitToSet % 32));
            if (bitToSet < 32)
            {
                fBits1 &= ~mask;
                fBits1 |= mask;
            }
             else
            {
                fBits2 &= ~mask;
                fBits2 |= mask;
            }
        }
         else
        {
            // Create the mask and byte values
            final byte mask = (byte)(0x1 << (bitToSet % 8));
            final int ofs = bitToSet >> 3;

            // And access the right bit and byte
            fByteArray[ofs] &= ~mask;
            fByteArray[ofs] |= mask;
        }
    }

// the XS content models from the schema package -neilg.
    public final void setTo(CMStateSet srcSet)
    {
        // They have to be the same size
        if (fBitCount != srcSet.fBitCount)
            throw new RuntimeException("ImplementationMessages.VAL_CMSI");

        if (fBitCount < 65)
        {
            fBits1 = srcSet.fBits1;
            fBits2 = srcSet.fBits2;
        }
         else
        {
            for (int index = fByteCount - 1; index >= 0; index--)
                fByteArray[index] = srcSet.fByteArray[index];
        }
    }

    // had to make this method public so it could be accessed from
    // schema package - neilg.
    public final void zeroBits()
    {
        if (fBitCount < 65)
        {
            fBits1 = 0;
            fBits2 = 0;
        }
         else
        {
            for (int index = fByteCount - 1; index >= 0; index--)
                fByteArray[index] = 0;
        }
    }


    // -------------------------------------------------------------------
    //  Private data members
    //
    //  fBitCount
    //      The count of bits that the outside world wants to support,
    //      so its the max bit index plus one.
    //
    //  fByteCount
    //      If the bit count is > 64, then we use the fByteArray member to
    //      store the bits, and this indicates its size in bytes. Otherwise
    //      its value is meaningless.
    //
    //  fBits1
    //  fBits2
    //      When the bit count is < 64 (very common), these hold the bits.
    //      Otherwise, the fByteArray member holds htem.
    // -------------------------------------------------------------------
    int         fBitCount;
    int         fByteCount;
    int         fBits1;
    int         fBits2;
    byte[]      fByteArray;
    /* Optimization(Jan, 2001) */
    public boolean equals(Object o) {
        if (!(o instanceof CMStateSet)) return false;
        return isSameSet((CMStateSet)o);
    }

    public int hashCode() {
        if (fBitCount < 65)
        {
            return fBits1+ fBits2 * 31;
        }
         else
        {
            int hash = 0;
            for (int index = fByteCount - 1; index >= 0; index--)
                hash = fByteArray[index] + hash * 31;
            return hash;
        }
    }
   /* Optimization(Jan, 2001) */
};
