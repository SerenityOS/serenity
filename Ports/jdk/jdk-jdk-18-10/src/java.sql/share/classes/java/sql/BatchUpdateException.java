/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.sql;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

/**
 * The subclass of {@link SQLException} thrown when an error
 * occurs during a batch update operation.  In addition to the
 * information provided by {@link SQLException}, a
 * {@code BatchUpdateException} provides the update
 * counts for all commands that were executed successfully during the
 * batch update, that is, all commands that were executed before the error
 * occurred.  The order of elements in an array of update counts
 * corresponds to the order in which commands were added to the batch.
 * <P>
 * After a command in a batch update fails to execute properly
 * and a {@code BatchUpdateException} is thrown, the driver
 * may or may not continue to process the remaining commands in
 * the batch.  If the driver continues processing after a failure,
 * the array returned by the method
 * {@code BatchUpdateException.getUpdateCounts} will have
 * an element for every command in the batch rather than only
 * elements for the commands that executed successfully before
 * the error.  In the case where the driver continues processing
 * commands, the array element for any command
 * that failed is {@code Statement.EXECUTE_FAILED}.
 * <P>
 * A JDBC driver implementation should use
 * the constructor {@code BatchUpdateException(String reason, String SQLState,
 * int vendorCode, long []updateCounts,Throwable cause) } instead of
 * constructors that take {@code int[]} for the update counts to avoid the
 * possibility of overflow.
 * <p>
 * If {@code Statement.executeLargeBatch} method is invoked it is recommended that
 * {@code getLargeUpdateCounts} be called instead of {@code getUpdateCounts}
 * in order to avoid a possible overflow of the integer update count.
 * @since 1.2
 */

public class BatchUpdateException extends SQLException {

  /**
   * Constructs a {@code BatchUpdateException} object initialized with a given
   * {@code reason}, {@code SQLState}, {@code vendorCode} and
   * {@code updateCounts}.
   * The {@code cause} is not initialized, and may subsequently be
   * initialized by a call to the
   * {@link Throwable#initCause(java.lang.Throwable)} method.
   * <p>
   * <strong>Note:</strong> There is no validation of {@code updateCounts} for
   * overflow and because of this it is recommended that you use the constructor
   * {@code BatchUpdateException(String reason, String SQLState,
   * int vendorCode, long []updateCounts,Throwable cause) }.
   * </p>
   * @param reason a description of the error
   * @param SQLState an XOPEN or SQL:2003 code identifying the exception
   * @param vendorCode an exception code used by a particular
   * database vendor
   * @param updateCounts an array of {@code int}, with each element
   * indicating the update count, {@code Statement.SUCCESS_NO_INFO} or
   * {@code Statement.EXECUTE_FAILED} for each SQL command in
   * the batch for JDBC drivers that continue processing
   * after a command failure; an update count or
   * {@code Statement.SUCCESS_NO_INFO} for each SQL command in the batch
   * prior to the failure for JDBC drivers that stop processing after a command
   * failure
   * @since 1.2
   * @see #BatchUpdateException(java.lang.String, java.lang.String, int, long[],
   * java.lang.Throwable)
   */
  public BatchUpdateException( String reason, String SQLState, int vendorCode,
                               int[] updateCounts ) {
      super(reason, SQLState, vendorCode);
      this.updateCounts  = (updateCounts == null) ? null : Arrays.copyOf(updateCounts, updateCounts.length);
      this.longUpdateCounts = (updateCounts == null) ? null : copyUpdateCount(updateCounts);
  }

  /**
   * Constructs a {@code BatchUpdateException} object initialized with a given
   * {@code reason}, {@code SQLState} and
   * {@code updateCounts}.
   * The {@code cause} is not initialized, and may subsequently be
   * initialized by a call to the
   * {@link Throwable#initCause(java.lang.Throwable)} method. The vendor code
   * is initialized to 0.
   * <p>
   * <strong>Note:</strong> There is no validation of {@code updateCounts} for
   * overflow and because of this it is recommended that you use the constructor
   * {@code BatchUpdateException(String reason, String SQLState,
   * int vendorCode, long []updateCounts,Throwable cause) }.
   * </p>
   * @param reason a description of the exception
   * @param SQLState an XOPEN or SQL:2003 code identifying the exception
   * @param updateCounts an array of {@code int}, with each element
   * indicating the update count, {@code Statement.SUCCESS_NO_INFO} or
   * {@code Statement.EXECUTE_FAILED} for each SQL command in
   * the batch for JDBC drivers that continue processing
   * after a command failure; an update count or
   * {@code Statement.SUCCESS_NO_INFO} for each SQL command in the batch
   * prior to the failure for JDBC drivers that stop processing after a command
   * failure
   * @since 1.2
   * @see #BatchUpdateException(java.lang.String, java.lang.String, int, long[],
   * java.lang.Throwable)
   */
  public BatchUpdateException(String reason, String SQLState,
                              int[] updateCounts) {
      this(reason, SQLState, 0, updateCounts);
  }

  /**
   * Constructs a {@code BatchUpdateException} object initialized with a given
   * {@code reason} and {@code updateCounts}.
   * The {@code cause} is not initialized, and may subsequently be
   * initialized by a call to the
   * {@link Throwable#initCause(java.lang.Throwable)} method.  The
   * {@code SQLState} is initialized to {@code null}
   * and the vendor code is initialized to 0.
   * <p>
   * <strong>Note:</strong> There is no validation of {@code updateCounts} for
   * overflow and because of this it is recommended that you use the constructor
   * {@code BatchUpdateException(String reason, String SQLState,
   * int vendorCode, long []updateCounts,Throwable cause) }.
   * </p>
   * @param reason a description of the exception
   * @param updateCounts an array of {@code int}, with each element
   * indicating the update count, {@code Statement.SUCCESS_NO_INFO} or
   * {@code Statement.EXECUTE_FAILED} for each SQL command in
   * the batch for JDBC drivers that continue processing
   * after a command failure; an update count or
   * {@code Statement.SUCCESS_NO_INFO} for each SQL command in the batch
   * prior to the failure for JDBC drivers that stop processing after a command
   * failure
   * @since 1.2
   * @see #BatchUpdateException(java.lang.String, java.lang.String, int, long[],
   * java.lang.Throwable)
   */
  public  BatchUpdateException(String reason, int[] updateCounts) {
      this(reason, null, 0, updateCounts);
  }

  /**
   * Constructs a {@code BatchUpdateException} object initialized with a given
   * {@code updateCounts}.
   * initialized by a call to the
   * {@link Throwable#initCause(java.lang.Throwable)} method. The  {@code reason}
   * and {@code SQLState} are initialized to null and the vendor code
   * is initialized to 0.
   * <p>
   * <strong>Note:</strong> There is no validation of {@code updateCounts} for
   * overflow and because of this it is recommended that you use the constructor
   * {@code BatchUpdateException(String reason, String SQLState,
   * int vendorCode, long []updateCounts,Throwable cause) }.
   * </p>
   * @param updateCounts an array of {@code int}, with each element
   * indicating the update count, {@code Statement.SUCCESS_NO_INFO} or
   * {@code Statement.EXECUTE_FAILED} for each SQL command in
   * the batch for JDBC drivers that continue processing
   * after a command failure; an update count or
   * {@code Statement.SUCCESS_NO_INFO} for each SQL command in the batch
   * prior to the failure for JDBC drivers that stop processing after a command
   * failure
   * @since 1.2
   * @see #BatchUpdateException(java.lang.String, java.lang.String, int, long[],
   * java.lang.Throwable)
   */
  public BatchUpdateException(int[] updateCounts) {
      this(null, null, 0, updateCounts);
  }

  /**
   * Constructs a {@code BatchUpdateException} object.
   * The {@code reason}, {@code SQLState} and {@code updateCounts}
   *  are initialized to {@code null} and the vendor code is initialized to 0.
   * The {@code cause} is not initialized, and may subsequently be
   * initialized by a call to the
   * {@link Throwable#initCause(java.lang.Throwable)} method.
   *
   * @since 1.2
   * @see #BatchUpdateException(java.lang.String, java.lang.String, int, long[],
   * java.lang.Throwable)
   */
  public BatchUpdateException() {
        this(null, null, 0, null);
  }

  /**
   * Constructs a {@code BatchUpdateException} object initialized with
   *  a given {@code cause}.
   * The {@code SQLState} and {@code updateCounts}
   * are initialized
   * to {@code null} and the vendor code is initialized to 0.
   * The {@code reason}  is initialized to {@code null} if
   * {@code cause==null} or to {@code cause.toString()} if
   *  {@code cause!=null}.
   * @param cause the underlying reason for this {@code SQLException}
   * (which is saved for later retrieval by the {@code getCause()} method);
   * may be null indicating the cause is non-existent or unknown.
   * @since 1.6
   * @see #BatchUpdateException(java.lang.String, java.lang.String, int, long[],
   * java.lang.Throwable)
   */
  public BatchUpdateException(Throwable cause) {
      this((cause == null ? null : cause.toString()), null, 0, (int[])null, cause);
  }

  /**
   * Constructs a {@code BatchUpdateException} object initialized with a
   * given {@code cause} and {@code updateCounts}.
   * The {@code SQLState} is initialized
   * to {@code null} and the vendor code is initialized to 0.
   * The {@code reason}  is initialized to {@code null} if
   * {@code cause==null} or to {@code cause.toString()} if
   * {@code cause!=null}.
   * <p>
   * <strong>Note:</strong> There is no validation of {@code updateCounts} for
   * overflow and because of this it is recommended that you use the constructor
   * {@code BatchUpdateException(String reason, String SQLState,
   * int vendorCode, long []updateCounts,Throwable cause) }.
   * </p>
   * @param updateCounts an array of {@code int}, with each element
   * indicating the update count, {@code Statement.SUCCESS_NO_INFO} or
   * {@code Statement.EXECUTE_FAILED} for each SQL command in
   * the batch for JDBC drivers that continue processing
   * after a command failure; an update count or
   * {@code Statement.SUCCESS_NO_INFO} for each SQL command in the batch
   * prior to the failure for JDBC drivers that stop processing after a command
   * failure
   * @param cause the underlying reason for this {@code SQLException}
   * (which is saved for later retrieval by the {@code getCause()} method); may be null indicating
   * the cause is non-existent or unknown.
   * @since 1.6
   * @see #BatchUpdateException(java.lang.String, java.lang.String, int, long[],
   * java.lang.Throwable)
   */
  public BatchUpdateException(int []updateCounts , Throwable cause) {
      this((cause == null ? null : cause.toString()), null, 0, updateCounts, cause);
  }

  /**
   * Constructs a {@code BatchUpdateException} object initialized with
   * a given {@code reason}, {@code cause}
   * and {@code updateCounts}. The {@code SQLState} is initialized
   * to {@code null} and the vendor code is initialized to 0.
   * <p>
   * <strong>Note:</strong> There is no validation of {@code updateCounts} for
   * overflow and because of this it is recommended that you use the constructor
   * {@code BatchUpdateException(String reason, String SQLState,
   * int vendorCode, long []updateCounts,Throwable cause) }.
   * </p>
   * @param reason a description of the exception
   * @param updateCounts an array of {@code int}, with each element
   * indicating the update count, {@code Statement.SUCCESS_NO_INFO} or
   * {@code Statement.EXECUTE_FAILED} for each SQL command in
   * the batch for JDBC drivers that continue processing
   * after a command failure; an update count or
   * {@code Statement.SUCCESS_NO_INFO} for each SQL command in the batch
   * prior to the failure for JDBC drivers that stop processing after a command
   * failure
   * @param cause the underlying reason for this {@code SQLException} (which is saved for later retrieval by the {@code getCause()} method);
   * may be null indicating
   * the cause is non-existent or unknown.
   * @since 1.6
   * @see #BatchUpdateException(java.lang.String, java.lang.String, int, long[],
   * java.lang.Throwable)
   */
  public BatchUpdateException(String reason, int []updateCounts, Throwable cause) {
      this(reason, null, 0, updateCounts, cause);
  }

  /**
   * Constructs a {@code BatchUpdateException} object initialized with
   * a given {@code reason}, {@code SQLState},{@code cause}, and
   * {@code updateCounts}. The vendor code is initialized to 0.
   *
   * @param reason a description of the exception
   * @param SQLState an XOPEN or SQL:2003 code identifying the exception
   * @param updateCounts an array of {@code int}, with each element
   * indicating the update count, {@code Statement.SUCCESS_NO_INFO} or
   * {@code Statement.EXECUTE_FAILED} for each SQL command in
   * the batch for JDBC drivers that continue processing
   * after a command failure; an update count or
   * {@code Statement.SUCCESS_NO_INFO} for each SQL command in the batch
   * prior to the failure for JDBC drivers that stop processing after a command
   * failure
   * <p>
   * <strong>Note:</strong> There is no validation of {@code updateCounts} for
   * overflow and because of this it is recommended that you use the constructor
   * {@code BatchUpdateException(String reason, String SQLState,
   * int vendorCode, long []updateCounts,Throwable cause) }.
   * </p>
   * @param cause the underlying reason for this {@code SQLException}
   * (which is saved for later retrieval by the {@code getCause()} method);
   * may be null indicating
   * the cause is non-existent or unknown.
   * @since 1.6
   * @see #BatchUpdateException(java.lang.String, java.lang.String, int, long[],
   * java.lang.Throwable)
   */
  public BatchUpdateException(String reason, String SQLState,
          int []updateCounts, Throwable cause) {
      this(reason, SQLState, 0, updateCounts, cause);
  }

  /**
   * Constructs a {@code BatchUpdateException} object initialized with
   * a given {@code reason}, {@code SQLState}, {@code vendorCode}
   * {@code cause} and {@code updateCounts}.
   *
   * @param reason a description of the error
   * @param SQLState an XOPEN or SQL:2003 code identifying the exception
   * @param vendorCode an exception code used by a particular
   * database vendor
   * @param updateCounts an array of {@code int}, with each element
   *indicating the update count, {@code Statement.SUCCESS_NO_INFO} or
   * {@code Statement.EXECUTE_FAILED} for each SQL command in
   * the batch for JDBC drivers that continue processing
   * after a command failure; an update count or
   * {@code Statement.SUCCESS_NO_INFO} for each SQL command in the batch
   * prior to the failure for JDBC drivers that stop processing after a command
   * failure
   * <p>
   * <strong>Note:</strong> There is no validation of {@code updateCounts} for
   * overflow and because of this it is recommended that you use the constructor
   * {@code BatchUpdateException(String reason, String SQLState,
   * int vendorCode, long []updateCounts,Throwable cause) }.
   * </p>
   * @param cause the underlying reason for this {@code SQLException} (which is saved for later retrieval by the {@code getCause()} method);
   * may be null indicating
   * the cause is non-existent or unknown.
   * @since 1.6
   * @see #BatchUpdateException(java.lang.String, java.lang.String, int, long[],
   * java.lang.Throwable)
   */
  public BatchUpdateException(String reason, String SQLState, int vendorCode,
                                int []updateCounts,Throwable cause) {
        super(reason, SQLState, vendorCode, cause);
        this.updateCounts  = (updateCounts == null) ? null : Arrays.copyOf(updateCounts, updateCounts.length);
        this.longUpdateCounts = (updateCounts == null) ? null : copyUpdateCount(updateCounts);
  }

  /**
   * Retrieves the update count for each update statement in the batch
   * update that executed successfully before this exception occurred.
   * A driver that implements batch updates may or may not continue to
   * process the remaining commands in a batch when one of the commands
   * fails to execute properly. If the driver continues processing commands,
   * the array returned by this method will have as many elements as
   * there are commands in the batch; otherwise, it will contain an
   * update count for each command that executed successfully before
   * the {@code BatchUpdateException} was thrown.
   * <P>
   * The possible return values for this method were modified for
   * the Java 2 SDK, Standard Edition, version 1.3.  This was done to
   * accommodate the new option of continuing to process commands
   * in a batch update after a {@code BatchUpdateException} object
   * has been thrown.
   *
   * @return an array of {@code int} containing the update counts
   * for the updates that were executed successfully before this error
   * occurred.  Or, if the driver continues to process commands after an
   * error, one of the following for every command in the batch:
   * <OL>
   * <LI>an update count
   *  <LI>{@code Statement.SUCCESS_NO_INFO} to indicate that the command
   *     executed successfully but the number of rows affected is unknown
   *  <LI>{@code Statement.EXECUTE_FAILED} to indicate that the command
   *     failed to execute successfully
   * </OL>
   * @since 1.3
   * @see #getLargeUpdateCounts()
   */
  public int[] getUpdateCounts() {
      return (updateCounts == null) ? null : Arrays.copyOf(updateCounts, updateCounts.length);
  }

  /**
   * Constructs a {@code BatchUpdateException} object initialized with
   * a given {@code reason}, {@code SQLState}, {@code vendorCode}
   * {@code cause} and {@code updateCounts}.
   * <p>
   * This constructor should be used when the returned update count may exceed
   * {@link Integer#MAX_VALUE}.
   *
   * @param reason a description of the error
   * @param SQLState an XOPEN or SQL:2003 code identifying the exception
   * @param vendorCode an exception code used by a particular
   * database vendor
   * @param updateCounts an array of {@code long}, with each element
   *indicating the update count, {@code Statement.SUCCESS_NO_INFO} or
   * {@code Statement.EXECUTE_FAILED} for each SQL command in
   * the batch for JDBC drivers that continue processing
   * after a command failure; an update count or
   * {@code Statement.SUCCESS_NO_INFO} for each SQL command in the batch
   * prior to the failure for JDBC drivers that stop processing after a command
   * failure
   * @param cause the underlying reason for this {@code SQLException}
   * (which is saved for later retrieval by the {@code getCause()} method);
   * may be null indicating the cause is non-existent or unknown.
   * @since 1.8
   */
  public BatchUpdateException(String reason, String SQLState, int vendorCode,
          long []updateCounts,Throwable cause) {
      super(reason, SQLState, vendorCode, cause);
      this.longUpdateCounts  = (updateCounts == null) ? null : Arrays.copyOf(updateCounts, updateCounts.length);
      this.updateCounts = (longUpdateCounts == null) ? null : copyUpdateCount(longUpdateCounts);
  }

  /**
   * Retrieves the update count for each update statement in the batch
   * update that executed successfully before this exception occurred.
   * A driver that implements batch updates may or may not continue to
   * process the remaining commands in a batch when one of the commands
   * fails to execute properly. If the driver continues processing commands,
   * the array returned by this method will have as many elements as
   * there are commands in the batch; otherwise, it will contain an
   * update count for each command that executed successfully before
   * the {@code BatchUpdateException} was thrown.
   * <p>
   * This method should be used when {@code Statement.executeLargeBatch} is
   * invoked and the returned update count may exceed {@link Integer#MAX_VALUE}.
   *
   * @return an array of {@code long} containing the update counts
   * for the updates that were executed successfully before this error
   * occurred.  Or, if the driver continues to process commands after an
   * error, one of the following for every command in the batch:
   * <OL>
   * <LI>an update count
   *  <LI>{@code Statement.SUCCESS_NO_INFO} to indicate that the command
   *     executed successfully but the number of rows affected is unknown
   *  <LI>{@code Statement.EXECUTE_FAILED} to indicate that the command
   *     failed to execute successfully
   * </OL>
   * @since 1.8
   */
  public long[] getLargeUpdateCounts() {
      return (longUpdateCounts == null) ? null :
              Arrays.copyOf(longUpdateCounts, longUpdateCounts.length);
  }

  /**
   * The array that describes the outcome of a batch execution.
   * @serial
   * @since 1.2
   */
  private  int[] updateCounts;

  /*
   * Starting with Java SE 8, JDBC has added support for returning an update
   * count > Integer.MAX_VALUE.  Because of this the following changes were made
   * to BatchUpdateException:
   * <ul>
   * <li>Add field longUpdateCounts</li>
   * <li>Add Constructor which takes long[] for update counts</li>
   * <li>Add getLargeUpdateCounts method</li>
   * </ul>
   * When any of the constructors are called, the int[] and long[] updateCount
   * fields are populated by copying the one array to each other.
   *
   * As the JDBC driver passes in the updateCounts, there has always been the
   * possibility for overflow and BatchUpdateException does not need to account
   * for that, it simply copies the arrays.
   *
   * JDBC drivers should always use the constructor that specifies long[] and
   * JDBC application developers should call getLargeUpdateCounts.
   */

  /**
   * The array that describes the outcome of a batch execution.
   * @serial
   * @since 1.8
   */
  private  long[] longUpdateCounts;

  private static final long serialVersionUID = 5977529877145521757L;

  /*
   * Utility method to copy int[] updateCount to long[] updateCount
   */
  private static long[] copyUpdateCount(int[] uc) {
      long[] copy = new long[uc.length];
      for(int i= 0; i< uc.length; i++) {
          copy[i] = uc[i];
      }
      return copy;
  }

  /*
   * Utility method to copy long[] updateCount to int[] updateCount.
   * No checks for overflow will be done as it is expected a  user will call
   * getLargeUpdateCounts.
   */
  private static int[] copyUpdateCount(long[] uc) {
      int[] copy = new int[uc.length];
      for(int i= 0; i< uc.length; i++) {
          copy[i] = (int) uc[i];
      }
      return copy;
  }
    /**
     * readObject is called to restore the state of the
     * {@code BatchUpdateException} from a stream.
     * @param s the {@code ObjectInputStream} to read from.
     *
     * @throws  ClassNotFoundException if the class of a serialized object
     *          could not be found.
     * @throws  IOException if an I/O error occurs.
     */
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {

       ObjectInputStream.GetField fields = s.readFields();
       int[] tmp = (int[])fields.get("updateCounts", null);
       long[] tmp2 = (long[])fields.get("longUpdateCounts", null);
       if(tmp != null && tmp2 != null && tmp.length != tmp2.length)
           throw new InvalidObjectException("update counts are not the expected size");
       if (tmp != null)
           updateCounts = tmp.clone();
       if (tmp2 != null)
           longUpdateCounts = tmp2.clone();
       if(updateCounts == null && longUpdateCounts != null)
           updateCounts = copyUpdateCount(longUpdateCounts);
       if(longUpdateCounts == null && updateCounts != null)
           longUpdateCounts = copyUpdateCount(updateCounts);

    }

    /**
     * writeObject is called to save the state of the {@code BatchUpdateException}
     * to a stream.
     * @param s the {@code ObjectOutputStream} to write to.
     * @throws  IOException if an I/O error occurs.
     */
    private void writeObject(ObjectOutputStream s)
            throws IOException {

        ObjectOutputStream.PutField fields = s.putFields();
        fields.put("updateCounts", updateCounts);
        fields.put("longUpdateCounts", longUpdateCounts);
        s.writeFields();
    }
}
