/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/* Copyright  (c) 2002 Graz University of Technology. All rights reserved.
 *
 * Redistribution and use in  source and binary forms, with or without
 * modification, are permitted  provided that the following conditions are met:
 *
 * 1. Redistributions of  source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in  binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment:
 *
 *    "This product includes software developed by IAIK of Graz University of
 *     Technology."
 *
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Graz University of Technology" and "IAIK of Graz University of
 *    Technology" must not be used to endorse or promote products derived from
 *    this software without prior written permission.
 *
 * 5. Products derived from this software may not be called
 *    "IAIK PKCS Wrapper", nor may "IAIK" appear in their name, without prior
 *    written permission of Graz University of Technology.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE LICENSOR BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 *  OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 *  OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY  OF SUCH DAMAGE.
 */

package sun.security.pkcs11.wrapper;



/**
 * class  CK_INFO provides general information about Cryptoki.<p>
 * <B>PKCS#11 structure:</B>
 * <PRE>
 *  typedef struct CK_INFO {&nbsp;&nbsp;
 *    CK_VERSION cryptokiVersion;&nbsp;&nbsp;
 *    CK_UTF8CHAR manufacturerID[32];&nbsp;&nbsp;
 *    CK_FLAGS flags;&nbsp;&nbsp;
 *    CK_UTF8CHAR libraryDescription[32];&nbsp;&nbsp;
 *    CK_VERSION libraryVersion;&nbsp;&nbsp;
 *  } CK_INFO;
 * </PRE>
 *
 * @author Karl Scheibelhofer <Karl.Scheibelhofer@iaik.at>
 * @author Martin Schlaeffer <schlaeff@sbox.tugraz.at>
 */
public class CK_INFO {

    /**
     * Cryptoki interface version number<p>
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_VERSION cryptokiVersion;
     * </PRE>
     */
    public CK_VERSION cryptokiVersion;

    /**
     * ID of the Cryptoki library manufacturer. must be blank
     * padded - only the first 32 chars will be used<p>
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_UTF8CHAR manufacturerID[32];
     * </PRE>
     */
    public char[] manufacturerID;

    /**
     * bit flags reserved for future versions. must be zero<p>
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_FLAGS flags;
     * </PRE>
     */
    public long flags;


/* libraryDescription and libraryVersion are new for v2.0 */

    /**
     * must be blank padded - only the first 32 chars will be used<p>
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_UTF8CHAR libraryDescription[32];
     * </PRE>
     */
    public char[] libraryDescription;

    /**
     * Cryptoki library version number<p>
     * <B>PKCS#11:</B>
     * <PRE>
     *   CK_VERSION libraryVersion;
     * </PRE>
     */
    public CK_VERSION libraryVersion;

    public CK_INFO(CK_VERSION cryptoVer, char[] vendor, long flags,
                   char[] libDesc, CK_VERSION libVer) {
        this.cryptokiVersion = cryptoVer;
        this.manufacturerID = vendor;
        this.flags = flags;
        this.libraryDescription = libDesc;
        this.libraryVersion = libVer;
    }

    /**
     * Returns the string representation of CK_INFO.
     *
     * @return the string representation of CK_INFO
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(Constants.INDENT);
        sb.append("cryptokiVersion: ");
        sb.append(cryptokiVersion.toString());
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("manufacturerID: ");
        sb.append(new String(manufacturerID));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("flags: ");
        sb.append(Functions.toBinaryString(flags));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("libraryDescription: ");
        sb.append(new String(libraryDescription));
        sb.append(Constants.NEWLINE);

        sb.append(Constants.INDENT);
        sb.append("libraryVersion: ");
        sb.append(libraryVersion.toString());
        //buffer.append(Constants.NEWLINE);

        return sb.toString() ;
    }

}
