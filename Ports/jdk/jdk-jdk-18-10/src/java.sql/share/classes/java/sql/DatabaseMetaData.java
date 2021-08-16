/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Comprehensive information about the database as a whole.
 * <P>
 * This interface is implemented by driver vendors to let users know the capabilities
 * of a Database Management System (DBMS) in combination with
 * the driver based on JDBC technology
 * ("JDBC driver") that is used with it.  Different relational DBMSs often support
 * different features, implement features in different ways, and use different
 * data types.  In addition, a driver may implement a feature on top of what the
 * DBMS offers.  Information returned by methods in this interface applies
 * to the capabilities of a particular driver and a particular DBMS working
 * together. Note that as used in this documentation, the term "database" is
 * used generically to refer to both the driver and DBMS.
 * <P>
 * A user for this interface is commonly a tool that needs to discover how to
 * deal with the underlying DBMS.  This is especially true for applications
 * that are intended to be used with more than one DBMS. For example, a tool might use the method
 * {@code getTypeInfo} to find out what data types can be used in a
 * {@code CREATE TABLE} statement.  Or a user might call the method
 * {@code supportsCorrelatedSubqueries} to see if it is possible to use
 * a correlated subquery or {@code supportsBatchUpdates} to see if it is
 * possible to use batch updates.
 * <P>
 * Some {@code DatabaseMetaData} methods return lists of information
 * in the form of {@code ResultSet} objects.
 * Regular {@code ResultSet} methods, such as
 * {@code getString} and {@code getInt}, can be used
 * to retrieve the data from these {@code ResultSet} objects.  If
 * a given form of metadata is not available, an empty {@code ResultSet}
 * will be returned. Additional columns beyond the columns defined to be
 * returned by the {@code ResultSet} object for a given method
 * can be defined by the JDBC driver vendor and must be accessed
 * by their <B>column label</B>.
 * <P>
 * Some {@code DatabaseMetaData} methods take arguments that are
 * String patterns.  These arguments all have names such as fooPattern.
 * Within a pattern String, "%" means match any substring of 0 or more
 * characters, and "_" means match any one character. Only metadata
 * entries matching the search pattern are returned. If a search pattern
 * argument is set to {@code null}, that argument's criterion will
 * be dropped from the search.
 *
 * @since 1.1
 */
public interface DatabaseMetaData extends Wrapper {

    //----------------------------------------------------------------------
    // First, a variety of minor information about the target database.

    /**
     * Retrieves whether the current user can call all the procedures
     * returned by the method {@code getProcedures}.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean allProceduresAreCallable() throws SQLException;

    /**
     * Retrieves whether the current user can use all the tables returned
     * by the method {@code getTables} in a {@code SELECT}
     * statement.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean allTablesAreSelectable() throws SQLException;

    /**
     * Retrieves the URL for this DBMS.
     *
     * @return the URL for this DBMS or {@code null} if it cannot be
     *          generated
     * @throws SQLException if a database access error occurs
     */
    String getURL() throws SQLException;

    /**
     * Retrieves the user name as known to this database.
     *
     * @return the database user name
     * @throws SQLException if a database access error occurs
     */
    String getUserName() throws SQLException;

    /**
     * Retrieves whether this database is in read-only mode.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean isReadOnly() throws SQLException;

    /**
     * Retrieves whether {@code NULL} values are sorted high.
     * Sorted high means that {@code NULL} values
     * sort higher than any other value in a domain.  In an ascending order,
     * if this method returns {@code true},  {@code NULL} values
     * will appear at the end. By contrast, the method
     * {@code nullsAreSortedAtEnd} indicates whether {@code NULL} values
     * are sorted at the end regardless of sort order.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean nullsAreSortedHigh() throws SQLException;

    /**
     * Retrieves whether {@code NULL} values are sorted low.
     * Sorted low means that {@code NULL} values
     * sort lower than any other value in a domain.  In an ascending order,
     * if this method returns {@code true},  {@code NULL} values
     * will appear at the beginning. By contrast, the method
     * {@code nullsAreSortedAtStart} indicates whether {@code NULL} values
     * are sorted at the beginning regardless of sort order.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean nullsAreSortedLow() throws SQLException;

    /**
     * Retrieves whether {@code NULL} values are sorted at the start regardless
     * of sort order.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean nullsAreSortedAtStart() throws SQLException;

    /**
     * Retrieves whether {@code NULL} values are sorted at the end regardless of
     * sort order.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean nullsAreSortedAtEnd() throws SQLException;

    /**
     * Retrieves the name of this database product.
     *
     * @return database product name
     * @throws SQLException if a database access error occurs
     */
    String getDatabaseProductName() throws SQLException;

    /**
     * Retrieves the version number of this database product.
     *
     * @return database version number
     * @throws SQLException if a database access error occurs
     */
    String getDatabaseProductVersion() throws SQLException;

    /**
     * Retrieves the name of this JDBC driver.
     *
     * @return JDBC driver name
     * @throws SQLException if a database access error occurs
     */
    String getDriverName() throws SQLException;

    /**
     * Retrieves the version number of this JDBC driver as a {@code String}.
     *
     * @return JDBC driver version
     * @throws SQLException if a database access error occurs
     */
    String getDriverVersion() throws SQLException;

    /**
     * Retrieves this JDBC driver's major version number.
     *
     * @return JDBC driver major version
     */
    int getDriverMajorVersion();

    /**
     * Retrieves this JDBC driver's minor version number.
     *
     * @return JDBC driver minor version number
     */
    int getDriverMinorVersion();

    /**
     * Retrieves whether this database stores tables in a local file.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean usesLocalFiles() throws SQLException;

    /**
     * Retrieves whether this database uses a file for each table.
     *
     * @return {@code true} if this database uses a local file for each table;
     *         {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean usesLocalFilePerTable() throws SQLException;

    /**
     * Retrieves whether this database treats mixed case unquoted SQL identifiers as
     * case sensitive and as a result stores them in mixed case.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsMixedCaseIdentifiers() throws SQLException;

    /**
     * Retrieves whether this database treats mixed case unquoted SQL identifiers as
     * case insensitive and stores them in upper case.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean storesUpperCaseIdentifiers() throws SQLException;

    /**
     * Retrieves whether this database treats mixed case unquoted SQL identifiers as
     * case insensitive and stores them in lower case.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean storesLowerCaseIdentifiers() throws SQLException;

    /**
     * Retrieves whether this database treats mixed case unquoted SQL identifiers as
     * case insensitive and stores them in mixed case.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean storesMixedCaseIdentifiers() throws SQLException;

    /**
     * Retrieves whether this database treats mixed case quoted SQL identifiers as
     * case sensitive and as a result stores them in mixed case.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsMixedCaseQuotedIdentifiers() throws SQLException;

    /**
     * Retrieves whether this database treats mixed case quoted SQL identifiers as
     * case insensitive and stores them in upper case.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean storesUpperCaseQuotedIdentifiers() throws SQLException;

    /**
     * Retrieves whether this database treats mixed case quoted SQL identifiers as
     * case insensitive and stores them in lower case.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean storesLowerCaseQuotedIdentifiers() throws SQLException;

    /**
     * Retrieves whether this database treats mixed case quoted SQL identifiers as
     * case insensitive and stores them in mixed case.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean storesMixedCaseQuotedIdentifiers() throws SQLException;

    /**
     * Retrieves the string used to quote SQL identifiers.
     * This method returns a space " " if identifier quoting is not supported.
     *
     * @return the quoting string or a space if quoting is not supported
     * @throws SQLException if a database access error occurs
     */
    String getIdentifierQuoteString() throws SQLException;

    /**
     * Retrieves a comma-separated list of all of this database's SQL keywords
     * that are NOT also SQL:2003 keywords.
     *
     * @return the list of this database's keywords that are not also
     *         SQL:2003 keywords
     * @throws SQLException if a database access error occurs
     */
    String getSQLKeywords() throws SQLException;

    /**
     * Retrieves a comma-separated list of math functions available with
     * this database.  These are the Open /Open CLI math function names used in
     * the JDBC function escape clause.
     *
     * @return the list of math functions supported by this database
     * @throws SQLException if a database access error occurs
     */
    String getNumericFunctions() throws SQLException;

    /**
     * Retrieves a comma-separated list of string functions available with
     * this database.  These are the  Open Group CLI string function names used
     * in the JDBC function escape clause.
     *
     * @return the list of string functions supported by this database
     * @throws SQLException if a database access error occurs
     */
    String getStringFunctions() throws SQLException;

    /**
     * Retrieves a comma-separated list of system functions available with
     * this database.  These are the  Open Group CLI system function names used
     * in the JDBC function escape clause.
     *
     * @return a list of system functions supported by this database
     * @throws SQLException if a database access error occurs
     */
    String getSystemFunctions() throws SQLException;

    /**
     * Retrieves a comma-separated list of the time and date functions available
     * with this database.
     *
     * @return the list of time and date functions supported by this database
     * @throws SQLException if a database access error occurs
     */
    String getTimeDateFunctions() throws SQLException;

    /**
     * Retrieves the string that can be used to escape wildcard characters.
     * This is the string that can be used to escape '_' or '%' in
     * the catalog search parameters that are a pattern (and therefore use one
     * of the wildcard characters).
     *
     * <P>The '_' character represents any single character;
     * the '%' character represents any sequence of zero or
     * more characters.
     *
     * @return the string used to escape wildcard characters
     * @throws SQLException if a database access error occurs
     */
    String getSearchStringEscape() throws SQLException;

    /**
     * Retrieves all the "extra" characters that can be used in unquoted
     * identifier names (those beyond a-z, A-Z, 0-9 and _).
     *
     * @return the string containing the extra characters
     * @throws SQLException if a database access error occurs
     */
    String getExtraNameCharacters() throws SQLException;

    //--------------------------------------------------------------------
    // Functions describing which features are supported.

    /**
     * Retrieves whether this database supports {@code ALTER TABLE}
     * with add column.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsAlterTableWithAddColumn() throws SQLException;

    /**
     * Retrieves whether this database supports {@code ALTER TABLE}
     * with drop column.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsAlterTableWithDropColumn() throws SQLException;

    /**
     * Retrieves whether this database supports column aliasing.
     *
     * <P>If so, the SQL AS clause can be used to provide names for
     * computed columns or to provide alias names for columns as
     * required.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsColumnAliasing() throws SQLException;

    /**
     * Retrieves whether this database supports concatenations between
     * {@code NULL} and non-{@code NULL} values being
     * {@code NULL}.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean nullPlusNonNullIsNull() throws SQLException;

    /**
     * Retrieves whether this database supports the JDBC scalar function
     * {@code CONVERT} for the conversion of one JDBC type to another.
     * The JDBC types are the generic SQL data types defined
     * in {@code java.sql.Types}.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsConvert() throws SQLException;

    /**
     * Retrieves whether this database supports the JDBC scalar function
     * {@code CONVERT} for conversions between the JDBC types <i>fromType</i>
     * and <i>toType</i>.  The JDBC types are the generic SQL data types defined
     * in {@code java.sql.Types}.
     *
     * @param fromType the type to convert from; one of the type codes from
     *        the class {@code java.sql.Types}
     * @param toType the type to convert to; one of the type codes from
     *        the class {@code java.sql.Types}
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @see Types
     */
    boolean supportsConvert(int fromType, int toType) throws SQLException;

    /**
     * Retrieves whether this database supports table correlation names.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsTableCorrelationNames() throws SQLException;

    /**
     * Retrieves whether, when table correlation names are supported, they
     * are restricted to being different from the names of the tables.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsDifferentTableCorrelationNames() throws SQLException;

    /**
     * Retrieves whether this database supports expressions in
     * {@code ORDER BY} lists.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsExpressionsInOrderBy() throws SQLException;

    /**
     * Retrieves whether this database supports using a column that is
     * not in the {@code SELECT} statement in an
     * {@code ORDER BY} clause.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsOrderByUnrelated() throws SQLException;

    /**
     * Retrieves whether this database supports some form of
     * {@code GROUP BY} clause.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsGroupBy() throws SQLException;

    /**
     * Retrieves whether this database supports using a column that is
     * not in the {@code SELECT} statement in a
     * {@code GROUP BY} clause.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsGroupByUnrelated() throws SQLException;

    /**
     * Retrieves whether this database supports using columns not included in
     * the {@code SELECT} statement in a {@code GROUP BY} clause
     * provided that all of the columns in the {@code SELECT} statement
     * are included in the {@code GROUP BY} clause.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsGroupByBeyondSelect() throws SQLException;

    /**
     * Retrieves whether this database supports specifying a
     * {@code LIKE} escape clause.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsLikeEscapeClause() throws SQLException;

    /**
     * Retrieves whether this database supports getting multiple
     * {@code ResultSet} objects from a single call to the
     * method {@code execute}.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsMultipleResultSets() throws SQLException;

    /**
     * Retrieves whether this database allows having multiple
     * transactions open at once (on different connections).
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsMultipleTransactions() throws SQLException;

    /**
     * Retrieves whether columns in this database may be defined as non-nullable.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsNonNullableColumns() throws SQLException;

    /**
     * Retrieves whether this database supports the ODBC Minimum SQL grammar.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsMinimumSQLGrammar() throws SQLException;

    /**
     * Retrieves whether this database supports the ODBC Core SQL grammar.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsCoreSQLGrammar() throws SQLException;

    /**
     * Retrieves whether this database supports the ODBC Extended SQL grammar.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsExtendedSQLGrammar() throws SQLException;

    /**
     * Retrieves whether this database supports the ANSI92 entry level SQL
     * grammar.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsANSI92EntryLevelSQL() throws SQLException;

    /**
     * Retrieves whether this database supports the ANSI92 intermediate SQL grammar supported.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsANSI92IntermediateSQL() throws SQLException;

    /**
     * Retrieves whether this database supports the ANSI92 full SQL grammar supported.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsANSI92FullSQL() throws SQLException;

    /**
     * Retrieves whether this database supports the SQL Integrity
     * Enhancement Facility.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsIntegrityEnhancementFacility() throws SQLException;

    /**
     * Retrieves whether this database supports some form of outer join.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsOuterJoins() throws SQLException;

    /**
     * Retrieves whether this database supports full nested outer joins.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsFullOuterJoins() throws SQLException;

    /**
     * Retrieves whether this database provides limited support for outer
     * joins.  (This will be {@code true} if the method
     * {@code supportsFullOuterJoins} returns {@code true}).
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsLimitedOuterJoins() throws SQLException;

    /**
     * Retrieves the database vendor's preferred term for "schema".
     *
     * @return the vendor term for "schema"
     * @throws SQLException if a database access error occurs
     */
    String getSchemaTerm() throws SQLException;

    /**
     * Retrieves the database vendor's preferred term for "procedure".
     *
     * @return the vendor term for "procedure"
     * @throws SQLException if a database access error occurs
     */
    String getProcedureTerm() throws SQLException;

    /**
     * Retrieves the database vendor's preferred term for "catalog".
     *
     * @return the vendor term for "catalog"
     * @throws SQLException if a database access error occurs
     */
    String getCatalogTerm() throws SQLException;

    /**
     * Retrieves whether a catalog appears at the start of a fully qualified
     * table name.  If not, the catalog appears at the end.
     *
     * @return {@code true} if the catalog name appears at the beginning
     *         of a fully qualified table name; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean isCatalogAtStart() throws SQLException;

    /**
     * Retrieves the {@code String} that this database uses as the
     * separator between a catalog and table name.
     *
     * @return the separator string
     * @throws SQLException if a database access error occurs
     */
    String getCatalogSeparator() throws SQLException;

    /**
     * Retrieves whether a schema name can be used in a data manipulation statement.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsSchemasInDataManipulation() throws SQLException;

    /**
     * Retrieves whether a schema name can be used in a procedure call statement.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsSchemasInProcedureCalls() throws SQLException;

    /**
     * Retrieves whether a schema name can be used in a table definition statement.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsSchemasInTableDefinitions() throws SQLException;

    /**
     * Retrieves whether a schema name can be used in an index definition statement.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsSchemasInIndexDefinitions() throws SQLException;

    /**
     * Retrieves whether a schema name can be used in a privilege definition statement.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsSchemasInPrivilegeDefinitions() throws SQLException;

    /**
     * Retrieves whether a catalog name can be used in a data manipulation statement.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsCatalogsInDataManipulation() throws SQLException;

    /**
     * Retrieves whether a catalog name can be used in a procedure call statement.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsCatalogsInProcedureCalls() throws SQLException;

    /**
     * Retrieves whether a catalog name can be used in a table definition statement.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsCatalogsInTableDefinitions() throws SQLException;

    /**
     * Retrieves whether a catalog name can be used in an index definition statement.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsCatalogsInIndexDefinitions() throws SQLException;

    /**
     * Retrieves whether a catalog name can be used in a privilege definition statement.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException;


    /**
     * Retrieves whether this database supports positioned {@code DELETE}
     * statements.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsPositionedDelete() throws SQLException;

    /**
     * Retrieves whether this database supports positioned {@code UPDATE}
     * statements.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsPositionedUpdate() throws SQLException;

    /**
     * Retrieves whether this database supports {@code SELECT FOR UPDATE}
     * statements.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsSelectForUpdate() throws SQLException;

    /**
     * Retrieves whether this database supports stored procedure calls
     * that use the stored procedure escape syntax.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsStoredProcedures() throws SQLException;

    /**
     * Retrieves whether this database supports subqueries in comparison
     * expressions.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsSubqueriesInComparisons() throws SQLException;

    /**
     * Retrieves whether this database supports subqueries in
     * {@code EXISTS} expressions.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsSubqueriesInExists() throws SQLException;

    /**
     * Retrieves whether this database supports subqueries in
     * {@code IN} expressions.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsSubqueriesInIns() throws SQLException;

    /**
     * Retrieves whether this database supports subqueries in quantified
     * expressions.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsSubqueriesInQuantifieds() throws SQLException;

    /**
     * Retrieves whether this database supports correlated subqueries.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsCorrelatedSubqueries() throws SQLException;

    /**
     * Retrieves whether this database supports SQL {@code UNION}.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsUnion() throws SQLException;

    /**
     * Retrieves whether this database supports SQL {@code UNION ALL}.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsUnionAll() throws SQLException;

    /**
     * Retrieves whether this database supports keeping cursors open
     * across commits.
     *
     * @return {@code true} if cursors always remain open;
     *       {@code false} if they might not remain open
     * @throws SQLException if a database access error occurs
     */
    boolean supportsOpenCursorsAcrossCommit() throws SQLException;

    /**
     * Retrieves whether this database supports keeping cursors open
     * across rollbacks.
     *
     * @return {@code true} if cursors always remain open;
     *       {@code false} if they might not remain open
     * @throws SQLException if a database access error occurs
     */
    boolean supportsOpenCursorsAcrossRollback() throws SQLException;

    /**
     * Retrieves whether this database supports keeping statements open
     * across commits.
     *
     * @return {@code true} if statements always remain open;
     *       {@code false} if they might not remain open
     * @throws SQLException if a database access error occurs
     */
    boolean supportsOpenStatementsAcrossCommit() throws SQLException;

    /**
     * Retrieves whether this database supports keeping statements open
     * across rollbacks.
     *
     * @return {@code true} if statements always remain open;
     *       {@code false} if they might not remain open
     * @throws SQLException if a database access error occurs
     */
    boolean supportsOpenStatementsAcrossRollback() throws SQLException;



    //----------------------------------------------------------------------
    // The following group of methods exposes various limitations
    // based on the target database with the current driver.
    // Unless otherwise specified, a result of zero means there is no
    // limit, or the limit is not known.

    /**
     * Retrieves the maximum number of hex characters this database allows in an
     * inline binary literal.
     *
     * @return max the maximum length (in hex characters) for a binary literal;
     *      a result of zero means that there is no limit or the limit
     *      is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxBinaryLiteralLength() throws SQLException;

    /**
     * Retrieves the maximum number of characters this database allows
     * for a character literal.
     *
     * @return the maximum number of characters allowed for a character literal;
     *      a result of zero means that there is no limit or the limit is
     *      not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxCharLiteralLength() throws SQLException;

    /**
     * Retrieves the maximum number of characters this database allows
     * for a column name.
     *
     * @return the maximum number of characters allowed for a column name;
     *      a result of zero means that there is no limit or the limit
     *      is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxColumnNameLength() throws SQLException;

    /**
     * Retrieves the maximum number of columns this database allows in a
     * {@code GROUP BY} clause.
     *
     * @return the maximum number of columns allowed;
     *      a result of zero means that there is no limit or the limit
     *      is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxColumnsInGroupBy() throws SQLException;

    /**
     * Retrieves the maximum number of columns this database allows in an index.
     *
     * @return the maximum number of columns allowed;
     *      a result of zero means that there is no limit or the limit
     *      is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxColumnsInIndex() throws SQLException;

    /**
     * Retrieves the maximum number of columns this database allows in an
     * {@code ORDER BY} clause.
     *
     * @return the maximum number of columns allowed;
     *      a result of zero means that there is no limit or the limit
     *      is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxColumnsInOrderBy() throws SQLException;

    /**
     * Retrieves the maximum number of columns this database allows in a
     * {@code SELECT} list.
     *
     * @return the maximum number of columns allowed;
     *      a result of zero means that there is no limit or the limit
     *      is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxColumnsInSelect() throws SQLException;

    /**
     * Retrieves the maximum number of columns this database allows in a table.
     *
     * @return the maximum number of columns allowed;
     *      a result of zero means that there is no limit or the limit
     *      is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxColumnsInTable() throws SQLException;

    /**
     * Retrieves the maximum number of concurrent connections to this
     * database that are possible.
     *
     * @return the maximum number of active connections possible at one time;
     *      a result of zero means that there is no limit or the limit
     *      is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxConnections() throws SQLException;

    /**
     * Retrieves the maximum number of characters that this database allows in a
     * cursor name.
     *
     * @return the maximum number of characters allowed in a cursor name;
     *      a result of zero means that there is no limit or the limit
     *      is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxCursorNameLength() throws SQLException;

    /**
     * Retrieves the maximum number of bytes this database allows for an
     * index, including all of the parts of the index.
     *
     * @return the maximum number of bytes allowed; this limit includes the
     *      composite of all the constituent parts of the index;
     *      a result of zero means that there is no limit or the limit
     *      is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxIndexLength() throws SQLException;

    /**
     * Retrieves the maximum number of characters that this database allows in a
     * schema name.
     *
     * @return the maximum number of characters allowed in a schema name;
     *      a result of zero means that there is no limit or the limit
     *      is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxSchemaNameLength() throws SQLException;

    /**
     * Retrieves the maximum number of characters that this database allows in a
     * procedure name.
     *
     * @return the maximum number of characters allowed in a procedure name;
     *      a result of zero means that there is no limit or the limit
     *      is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxProcedureNameLength() throws SQLException;

    /**
     * Retrieves the maximum number of characters that this database allows in a
     * catalog name.
     *
     * @return the maximum number of characters allowed in a catalog name;
     *      a result of zero means that there is no limit or the limit
     *      is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxCatalogNameLength() throws SQLException;

    /**
     * Retrieves the maximum number of bytes this database allows in
     * a single row.
     *
     * @return the maximum number of bytes allowed for a row; a result of
     *         zero means that there is no limit or the limit is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxRowSize() throws SQLException;

    /**
     * Retrieves whether the return value for the method
     * {@code getMaxRowSize} includes the SQL data types
     * {@code LONGVARCHAR} and {@code LONGVARBINARY}.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean doesMaxRowSizeIncludeBlobs() throws SQLException;

    /**
     * Retrieves the maximum number of characters this database allows in
     * an SQL statement.
     *
     * @return the maximum number of characters allowed for an SQL statement;
     *      a result of zero means that there is no limit or the limit
     *      is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxStatementLength() throws SQLException;

    /**
     * Retrieves the maximum number of active statements to this database
     * that can be open at the same time.
     *
     * @return the maximum number of statements that can be open at one time;
     *      a result of zero means that there is no limit or the limit
     *      is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxStatements() throws SQLException;

    /**
     * Retrieves the maximum number of characters this database allows in
     * a table name.
     *
     * @return the maximum number of characters allowed for a table name;
     *      a result of zero means that there is no limit or the limit
     *      is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxTableNameLength() throws SQLException;

    /**
     * Retrieves the maximum number of tables this database allows in a
     * {@code SELECT} statement.
     *
     * @return the maximum number of tables allowed in a {@code SELECT}
     *         statement; a result of zero means that there is no limit or
     *         the limit is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxTablesInSelect() throws SQLException;

    /**
     * Retrieves the maximum number of characters this database allows in
     * a user name.
     *
     * @return the maximum number of characters allowed for a user name;
     *      a result of zero means that there is no limit or the limit
     *      is not known
     * @throws SQLException if a database access error occurs
     */
    int getMaxUserNameLength() throws SQLException;

    //----------------------------------------------------------------------

    /**
     * Retrieves this database's default transaction isolation level.  The
     * possible values are defined in {@code java.sql.Connection}.
     *
     * @return the default isolation level
     * @throws SQLException if a database access error occurs
     * @see Connection
     */
    int getDefaultTransactionIsolation() throws SQLException;

    /**
     * Retrieves whether this database supports transactions. If not, invoking the
     * method {@code commit} is a noop, and the isolation level is
     * {@code TRANSACTION_NONE}.
     *
     * @return {@code true} if transactions are supported;
     *         {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsTransactions() throws SQLException;

    /**
     * Retrieves whether this database supports the given transaction isolation level.
     *
     * @param level one of the transaction isolation levels defined in
     *         {@code java.sql.Connection}
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @see Connection
     */
    boolean supportsTransactionIsolationLevel(int level)
        throws SQLException;

    /**
     * Retrieves whether this database supports both data definition and
     * data manipulation statements within a transaction.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsDataDefinitionAndDataManipulationTransactions()
        throws SQLException;
    /**
     * Retrieves whether this database supports only data manipulation
     * statements within a transaction.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean supportsDataManipulationTransactionsOnly()
        throws SQLException;

    /**
     * Retrieves whether a data definition statement within a transaction forces
     * the transaction to commit.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean dataDefinitionCausesTransactionCommit()
        throws SQLException;

    /**
     * Retrieves whether this database ignores a data definition statement
     * within a transaction.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean dataDefinitionIgnoredInTransactions()
        throws SQLException;

    /**
     * Retrieves a description of the stored procedures available in the given
     * catalog.
     * <P>
     * Only procedure descriptions matching the schema and
     * procedure name criteria are returned.  They are ordered by
     * {@code PROCEDURE_CAT}, {@code PROCEDURE_SCHEM},
     * {@code PROCEDURE_NAME} and {@code SPECIFIC_ NAME}.
     *
     * <P>Each procedure description has the following columns:
     *  <OL>
     *  <LI><B>PROCEDURE_CAT</B> String {@code =>} procedure catalog (may be {@code null})
     *  <LI><B>PROCEDURE_SCHEM</B> String {@code =>} procedure schema (may be {@code null})
     *  <LI><B>PROCEDURE_NAME</B> String {@code =>} procedure name
     *  <LI> reserved for future use
     *  <LI> reserved for future use
     *  <LI> reserved for future use
     *  <LI><B>REMARKS</B> String {@code =>} explanatory comment on the procedure
     *  <LI><B>PROCEDURE_TYPE</B> short {@code =>} kind of procedure:
     *      <UL>
     *      <LI> procedureResultUnknown - Cannot determine if  a return value
     *       will be returned
     *      <LI> procedureNoResult - Does not return a return value
     *      <LI> procedureReturnsResult - Returns a return value
     *      </UL>
     *  <LI><B>SPECIFIC_NAME</B> String  {@code =>} The name which uniquely identifies this
     * procedure within its schema.
     *  </OL>
     * <p>
     * A user may not have permissions to execute any of the procedures that are
     * returned by {@code getProcedures}
     *
     * @param catalog a catalog name; must match the catalog name as it
     *        is stored in the database; "" retrieves those without a catalog;
     *        {@code null} means that the catalog name should not be used to narrow
     *        the search
     * @param schemaPattern a schema name pattern; must match the schema name
     *        as it is stored in the database; "" retrieves those without a schema;
     *        {@code null} means that the schema name should not be used to narrow
     *        the search
     * @param procedureNamePattern a procedure name pattern; must match the
     *        procedure name as it is stored in the database
     * @return {@code ResultSet} - each row is a procedure description
     * @throws SQLException if a database access error occurs
     * @see #getSearchStringEscape
     */
    ResultSet getProcedures(String catalog, String schemaPattern,
                            String procedureNamePattern) throws SQLException;

    /**
     * Indicates that it is not known whether the procedure returns
     * a result.
     * <P>
     * A possible value for column {@code PROCEDURE_TYPE} in the
     * {@code ResultSet} object returned by the method
     * {@code getProcedures}.
     */
    int procedureResultUnknown  = 0;

    /**
     * Indicates that the procedure does not return a result.
     * <P>
     * A possible value for column {@code PROCEDURE_TYPE} in the
     * {@code ResultSet} object returned by the method
     * {@code getProcedures}.
     */
    int procedureNoResult               = 1;

    /**
     * Indicates that the procedure returns a result.
     * <P>
     * A possible value for column {@code PROCEDURE_TYPE} in the
     * {@code ResultSet} object returned by the method
     * {@code getProcedures}.
     */
    int procedureReturnsResult  = 2;

    /**
     * Retrieves a description of the given catalog's stored procedure parameter
     * and result columns.
     *
     * <P>Only descriptions matching the schema, procedure and
     * parameter name criteria are returned.  They are ordered by
     * PROCEDURE_CAT, PROCEDURE_SCHEM, PROCEDURE_NAME and SPECIFIC_NAME. Within this, the return value,
     * if any, is first. Next are the parameter descriptions in call
     * order. The column descriptions follow in column number order.
     *
     * <P>Each row in the {@code ResultSet} is a parameter description or
     * column description with the following fields:
     *  <OL>
     *  <LI><B>PROCEDURE_CAT</B> String {@code =>} procedure catalog (may be {@code null})
     *  <LI><B>PROCEDURE_SCHEM</B> String {@code =>} procedure schema (may be {@code null})
     *  <LI><B>PROCEDURE_NAME</B> String {@code =>} procedure name
     *  <LI><B>COLUMN_NAME</B> String {@code =>} column/parameter name
     *  <LI><B>COLUMN_TYPE</B> Short {@code =>} kind of column/parameter:
     *      <UL>
     *      <LI> procedureColumnUnknown - nobody knows
     *      <LI> procedureColumnIn - IN parameter
     *      <LI> procedureColumnInOut - INOUT parameter
     *      <LI> procedureColumnOut - OUT parameter
     *      <LI> procedureColumnReturn - procedure return value
     *      <LI> procedureColumnResult - result column in {@code ResultSet}
     *      </UL>
     *  <LI><B>DATA_TYPE</B> int {@code =>} SQL type from java.sql.Types
     *  <LI><B>TYPE_NAME</B> String {@code =>} SQL type name, for a UDT type the
     *  type name is fully qualified
     *  <LI><B>PRECISION</B> int {@code =>} precision
     *  <LI><B>LENGTH</B> int {@code =>} length in bytes of data
     *  <LI><B>SCALE</B> short {@code =>} scale -  null is returned for data types where
     * SCALE is not applicable.
     *  <LI><B>RADIX</B> short {@code =>} radix
     *  <LI><B>NULLABLE</B> short {@code =>} can it contain NULL.
     *      <UL>
     *      <LI> procedureNoNulls - does not allow NULL values
     *      <LI> procedureNullable - allows NULL values
     *      <LI> procedureNullableUnknown - nullability unknown
     *      </UL>
     *  <LI><B>REMARKS</B> String {@code =>} comment describing parameter/column
     *  <LI><B>COLUMN_DEF</B> String {@code =>} default value for the column, which should be interpreted as a string when the value is enclosed in single quotes (may be {@code null})
     *      <UL>
     *      <LI> The string NULL (not enclosed in quotes) - if NULL was specified as the default value
     *      <LI> TRUNCATE (not enclosed in quotes)        - if the specified default value cannot be represented without truncation
     *      <LI> NULL                                     - if a default value was not specified
     *      </UL>
     *  <LI><B>SQL_DATA_TYPE</B> int  {@code =>} reserved for future use
     *  <LI><B>SQL_DATETIME_SUB</B> int  {@code =>} reserved for future use
     *  <LI><B>CHAR_OCTET_LENGTH</B> int  {@code =>} the maximum length of binary and character based columns.  For any other datatype the returned value is a
     * NULL
     *  <LI><B>ORDINAL_POSITION</B> int  {@code =>} the ordinal position, starting from 1, for the input and output parameters for a procedure. A value of 0
     *is returned if this row describes the procedure's return value.  For result set columns, it is the
     *ordinal position of the column in the result set starting from 1.  If there are
     *multiple result sets, the column ordinal positions are implementation
     * defined.
     *  <LI><B>IS_NULLABLE</B> String  {@code =>} ISO rules are used to determine the nullability for a column.
     *       <UL>
     *       <LI> YES           --- if the column can include NULLs
     *       <LI> NO            --- if the column cannot include NULLs
     *       <LI> empty string  --- if the nullability for the
     * column is unknown
     *       </UL>
     *  <LI><B>SPECIFIC_NAME</B> String  {@code =>} the name which uniquely identifies this procedure within its schema.
     *  </OL>
     *
     * <P><B>Note:</B> Some databases may not return the column
     * descriptions for a procedure.
     *
     * <p>The PRECISION column represents the specified column size for the given column.
     * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
     * For datetime datatypes, this is the length in characters of the String representation (assuming the
     * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
     * this is the length in bytes. Null is returned for data types where the
     * column size is not applicable.
     * @param catalog a catalog name; must match the catalog name as it
     *        is stored in the database; "" retrieves those without a catalog;
     *        {@code null} means that the catalog name should not be used to narrow
     *        the search
     * @param schemaPattern a schema name pattern; must match the schema name
     *        as it is stored in the database; "" retrieves those without a schema;
     *        {@code null} means that the schema name should not be used to narrow
     *        the search
     * @param procedureNamePattern a procedure name pattern; must match the
     *        procedure name as it is stored in the database
     * @param columnNamePattern a column name pattern; must match the column name
     *        as it is stored in the database
     * @return {@code ResultSet} - each row describes a stored procedure parameter or
     *      column
     * @throws SQLException if a database access error occurs
     * @see #getSearchStringEscape
     */
    ResultSet getProcedureColumns(String catalog,
                                  String schemaPattern,
                                  String procedureNamePattern,
                                  String columnNamePattern) throws SQLException;

    /**
     * Indicates that type of the column is unknown.
     * <P>
     * A possible value for the column
     * {@code COLUMN_TYPE}
     * in the {@code ResultSet}
     * returned by the method {@code getProcedureColumns}.
     */
    int procedureColumnUnknown = 0;

    /**
     * Indicates that the column stores IN parameters.
     * <P>
     * A possible value for the column
     * {@code COLUMN_TYPE}
     * in the {@code ResultSet}
     * returned by the method {@code getProcedureColumns}.
     */
    int procedureColumnIn = 1;

    /**
     * Indicates that the column stores INOUT parameters.
     * <P>
     * A possible value for the column
     * {@code COLUMN_TYPE}
     * in the {@code ResultSet}
     * returned by the method {@code getProcedureColumns}.
     */
    int procedureColumnInOut = 2;

    /**
     * Indicates that the column stores OUT parameters.
     * <P>
     * A possible value for the column
     * {@code COLUMN_TYPE}
     * in the {@code ResultSet}
     * returned by the method {@code getProcedureColumns}.
     */
    int procedureColumnOut = 4;
    /**
     * Indicates that the column stores return values.
     * <P>
     * A possible value for the column
     * {@code COLUMN_TYPE}
     * in the {@code ResultSet}
     * returned by the method {@code getProcedureColumns}.
     */
    int procedureColumnReturn = 5;

    /**
     * Indicates that the column stores results.
     * <P>
     * A possible value for the column
     * {@code COLUMN_TYPE}
     * in the {@code ResultSet}
     * returned by the method {@code getProcedureColumns}.
     */
    int procedureColumnResult = 3;

    /**
     * Indicates that {@code NULL} values are not allowed.
     * <P>
     * A possible value for the column
     * {@code NULLABLE}
     * in the {@code ResultSet} object
     * returned by the method {@code getProcedureColumns}.
     */
    int procedureNoNulls = 0;

    /**
     * Indicates that {@code NULL} values are allowed.
     * <P>
     * A possible value for the column
     * {@code NULLABLE}
     * in the {@code ResultSet} object
     * returned by the method {@code getProcedureColumns}.
     */
    int procedureNullable = 1;

    /**
     * Indicates that whether {@code NULL} values are allowed
     * is unknown.
     * <P>
     * A possible value for the column
     * {@code NULLABLE}
     * in the {@code ResultSet} object
     * returned by the method {@code getProcedureColumns}.
     */
    int procedureNullableUnknown = 2;


    /**
     * Retrieves a description of the tables available in the given catalog.
     * Only table descriptions matching the catalog, schema, table
     * name and type criteria are returned.  They are ordered by
     * {@code TABLE_TYPE}, {@code TABLE_CAT},
     * {@code TABLE_SCHEM} and {@code TABLE_NAME}.
     * <P>
     * Each table description has the following columns:
     *  <OL>
     *  <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be {@code null})
     *  <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be {@code null})
     *  <LI><B>TABLE_NAME</B> String {@code =>} table name
     *  <LI><B>TABLE_TYPE</B> String {@code =>} table type.  Typical types are "TABLE",
     *                  "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
     *                  "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
     *  <LI><B>REMARKS</B> String {@code =>} explanatory comment on the table (may be {@code null})
     *  <LI><B>TYPE_CAT</B> String {@code =>} the types catalog (may be {@code null})
     *  <LI><B>TYPE_SCHEM</B> String {@code =>} the types schema (may be {@code null})
     *  <LI><B>TYPE_NAME</B> String {@code =>} type name (may be {@code null})
     *  <LI><B>SELF_REFERENCING_COL_NAME</B> String {@code =>} name of the designated
     *                  "identifier" column of a typed table (may be {@code null})
     *  <LI><B>REF_GENERATION</B> String {@code =>} specifies how values in
     *                  SELF_REFERENCING_COL_NAME are created. Values are
     *                  "SYSTEM", "USER", "DERIVED". (may be {@code null})
     *  </OL>
     *
     * <P><B>Note:</B> Some databases may not return information for
     * all tables.
     *
     * @param catalog a catalog name; must match the catalog name as it
     *        is stored in the database; "" retrieves those without a catalog;
     *        {@code null} means that the catalog name should not be used to narrow
     *        the search
     * @param schemaPattern a schema name pattern; must match the schema name
     *        as it is stored in the database; "" retrieves those without a schema;
     *        {@code null} means that the schema name should not be used to narrow
     *        the search
     * @param tableNamePattern a table name pattern; must match the
     *        table name as it is stored in the database
     * @param types a list of table types, which must be from the list of table types
     *         returned from {@link #getTableTypes},to include; {@code null} returns
     * all types
     * @return {@code ResultSet} - each row is a table description
     * @throws SQLException if a database access error occurs
     * @see #getSearchStringEscape
     */
    ResultSet getTables(String catalog, String schemaPattern,
                        String tableNamePattern, String types[]) throws SQLException;

    /**
     * Retrieves the schema names available in this database.  The results
     * are ordered by {@code TABLE_CATALOG} and
     * {@code TABLE_SCHEM}.
     *
     * <P>The schema columns are:
     *  <OL>
     *  <LI><B>TABLE_SCHEM</B> String {@code =>} schema name
     *  <LI><B>TABLE_CATALOG</B> String {@code =>} catalog name (may be {@code null})
     *  </OL>
     *
     * @return a {@code ResultSet} object in which each row is a
     *         schema description
     * @throws SQLException if a database access error occurs
     *
     */
    ResultSet getSchemas() throws SQLException;

    /**
     * Retrieves the catalog names available in this database.  The results
     * are ordered by catalog name.
     *
     * <P>The catalog column is:
     *  <OL>
     *  <LI><B>TABLE_CAT</B> String {@code =>} catalog name
     *  </OL>
     *
     * @return a {@code ResultSet} object in which each row has a
     *         single {@code String} column that is a catalog name
     * @throws SQLException if a database access error occurs
     */
    ResultSet getCatalogs() throws SQLException;

    /**
     * Retrieves the table types available in this database.  The results
     * are ordered by table type.
     *
     * <P>The table type is:
     *  <OL>
     *  <LI><B>TABLE_TYPE</B> String {@code =>} table type.  Typical types are "TABLE",
     *                  "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
     *                  "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
     *  </OL>
     *
     * @return a {@code ResultSet} object in which each row has a
     *         single {@code String} column that is a table type
     * @throws SQLException if a database access error occurs
     */
    ResultSet getTableTypes() throws SQLException;

    /**
     * Retrieves a description of table columns available in
     * the specified catalog.
     *
     * <P>Only column descriptions matching the catalog, schema, table
     * and column name criteria are returned.  They are ordered by
     * {@code TABLE_CAT},{@code TABLE_SCHEM},
     * {@code TABLE_NAME}, and {@code ORDINAL_POSITION}.
     *
     * <P>Each column description has the following columns:
     *  <OL>
     *  <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be {@code null})
     *  <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be {@code null})
     *  <LI><B>TABLE_NAME</B> String {@code =>} table name
     *  <LI><B>COLUMN_NAME</B> String {@code =>} column name
     *  <LI><B>DATA_TYPE</B> int {@code =>} SQL type from java.sql.Types
     *  <LI><B>TYPE_NAME</B> String {@code =>} Data source dependent type name,
     *  for a UDT the type name is fully qualified
     *  <LI><B>COLUMN_SIZE</B> int {@code =>} column size.
     *  <LI><B>BUFFER_LENGTH</B> is not used.
     *  <LI><B>DECIMAL_DIGITS</B> int {@code =>} the number of fractional digits. Null is returned for data types where
     * DECIMAL_DIGITS is not applicable.
     *  <LI><B>NUM_PREC_RADIX</B> int {@code =>} Radix (typically either 10 or 2)
     *  <LI><B>NULLABLE</B> int {@code =>} is NULL allowed.
     *      <UL>
     *      <LI> columnNoNulls - might not allow {@code NULL} values
     *      <LI> columnNullable - definitely allows {@code NULL} values
     *      <LI> columnNullableUnknown - nullability unknown
     *      </UL>
     *  <LI><B>REMARKS</B> String {@code =>} comment describing column (may be {@code null})
     *  <LI><B>COLUMN_DEF</B> String {@code =>} default value for the column, which should be interpreted as a string when the value is enclosed in single quotes (may be {@code null})
     *  <LI><B>SQL_DATA_TYPE</B> int {@code =>} unused
     *  <LI><B>SQL_DATETIME_SUB</B> int {@code =>} unused
     *  <LI><B>CHAR_OCTET_LENGTH</B> int {@code =>} for char types the
     *       maximum number of bytes in the column
     *  <LI><B>ORDINAL_POSITION</B> int {@code =>} index of column in table
     *      (starting at 1)
     *  <LI><B>IS_NULLABLE</B> String  {@code =>} ISO rules are used to determine the nullability for a column.
     *       <UL>
     *       <LI> YES           --- if the column can include NULLs
     *       <LI> NO            --- if the column cannot include NULLs
     *       <LI> empty string  --- if the nullability for the
     * column is unknown
     *       </UL>
     *  <LI><B>SCOPE_CATALOG</B> String {@code =>} catalog of table that is the scope
     *      of a reference attribute ({@code null} if DATA_TYPE isn't REF)
     *  <LI><B>SCOPE_SCHEMA</B> String {@code =>} schema of table that is the scope
     *      of a reference attribute ({@code null} if the DATA_TYPE isn't REF)
     *  <LI><B>SCOPE_TABLE</B> String {@code =>} table name that this the scope
     *      of a reference attribute ({@code null} if the DATA_TYPE isn't REF)
     *  <LI><B>SOURCE_DATA_TYPE</B> short {@code =>} source type of a distinct type or user-generated
     *      Ref type, SQL type from java.sql.Types ({@code null} if DATA_TYPE
     *      isn't DISTINCT or user-generated REF)
     *   <LI><B>IS_AUTOINCREMENT</B> String  {@code =>} Indicates whether this column is auto incremented
     *       <UL>
     *       <LI> YES           --- if the column is auto incremented
     *       <LI> NO            --- if the column is not auto incremented
     *       <LI> empty string  --- if it cannot be determined whether the column is auto incremented
     *       </UL>
     *   <LI><B>IS_GENERATEDCOLUMN</B> String  {@code =>} Indicates whether this is a generated column
     *       <UL>
     *       <LI> YES           --- if this a generated column
     *       <LI> NO            --- if this not a generated column
     *       <LI> empty string  --- if it cannot be determined whether this is a generated column
     *       </UL>
     *  </OL>
     *
     * <p>The COLUMN_SIZE column specifies the column size for the given column.
     * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
     * For datetime datatypes, this is the length in characters of the String representation (assuming the
     * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
     * this is the length in bytes. Null is returned for data types where the
     * column size is not applicable.
     *
     * @param catalog a catalog name; must match the catalog name as it
     *        is stored in the database; "" retrieves those without a catalog;
     *        {@code null} means that the catalog name should not be used to narrow
     *        the search
     * @param schemaPattern a schema name pattern; must match the schema name
     *        as it is stored in the database; "" retrieves those without a schema;
     *        {@code null} means that the schema name should not be used to narrow
     *        the search
     * @param tableNamePattern a table name pattern; must match the
     *        table name as it is stored in the database
     * @param columnNamePattern a column name pattern; must match the column
     *        name as it is stored in the database
     * @return {@code ResultSet} - each row is a column description
     * @throws SQLException if a database access error occurs
     * @see #getSearchStringEscape
     */
    ResultSet getColumns(String catalog, String schemaPattern,
                         String tableNamePattern, String columnNamePattern)
        throws SQLException;

    /**
     * Indicates that the column might not allow {@code NULL} values.
     * <P>
     * A possible value for the column
     * {@code NULLABLE}
     * in the {@code ResultSet} returned by the method
     * {@code getColumns}.
     */
    int columnNoNulls = 0;

    /**
     * Indicates that the column definitely allows {@code NULL} values.
     * <P>
     * A possible value for the column
     * {@code NULLABLE}
     * in the {@code ResultSet} returned by the method
     * {@code getColumns}.
     */
    int columnNullable = 1;

    /**
     * Indicates that the nullability of columns is unknown.
     * <P>
     * A possible value for the column
     * {@code NULLABLE}
     * in the {@code ResultSet} returned by the method
     * {@code getColumns}.
     */
    int columnNullableUnknown = 2;

    /**
     * Retrieves a description of the access rights for a table's columns.
     *
     * <P>Only privileges matching the column name criteria are
     * returned.  They are ordered by COLUMN_NAME and PRIVILEGE.
     *
     * <P>Each privilege description has the following columns:
     *  <OL>
     *  <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be {@code null})
     *  <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be {@code null})
     *  <LI><B>TABLE_NAME</B> String {@code =>} table name
     *  <LI><B>COLUMN_NAME</B> String {@code =>} column name
     *  <LI><B>GRANTOR</B> String {@code =>} grantor of access (may be {@code null})
     *  <LI><B>GRANTEE</B> String {@code =>} grantee of access
     *  <LI><B>PRIVILEGE</B> String {@code =>} name of access (SELECT,
     *      INSERT, UPDATE, REFERENCES, ...)
     *  <LI><B>IS_GRANTABLE</B> String {@code =>} "YES" if grantee is permitted
     *      to grant to others; "NO" if not; {@code null} if unknown
     *  </OL>
     *
     * @param catalog a catalog name; must match the catalog name as it
     *        is stored in the database; "" retrieves those without a catalog;
     *        {@code null} means that the catalog name should not be used to narrow
     *        the search
     * @param schema a schema name; must match the schema name as it is
     *        stored in the database; "" retrieves those without a schema;
     *        {@code null} means that the schema name should not be used to narrow
     *        the search
     * @param table a table name; must match the table name as it is
     *        stored in the database
     * @param columnNamePattern a column name pattern; must match the column
     *        name as it is stored in the database
     * @return {@code ResultSet} - each row is a column privilege description
     * @throws SQLException if a database access error occurs
     * @see #getSearchStringEscape
     */
    ResultSet getColumnPrivileges(String catalog, String schema,
                                  String table, String columnNamePattern) throws SQLException;

    /**
     * Retrieves a description of the access rights for each table available
     * in a catalog. Note that a table privilege applies to one or
     * more columns in the table. It would be wrong to assume that
     * this privilege applies to all columns (this may be true for
     * some systems but is not true for all.)
     *
     * <P>Only privileges matching the schema and table name
     * criteria are returned.  They are ordered by
     * {@code TABLE_CAT},
     * {@code TABLE_SCHEM}, {@code TABLE_NAME},
     * and {@code PRIVILEGE}.
     *
     * <P>Each privilege description has the following columns:
     *  <OL>
     *  <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be {@code null})
     *  <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be {@code null})
     *  <LI><B>TABLE_NAME</B> String {@code =>} table name
     *  <LI><B>GRANTOR</B> String {@code =>} grantor of access (may be {@code null})
     *  <LI><B>GRANTEE</B> String {@code =>} grantee of access
     *  <LI><B>PRIVILEGE</B> String {@code =>} name of access (SELECT,
     *      INSERT, UPDATE, REFERENCES, ...)
     *  <LI><B>IS_GRANTABLE</B> String {@code =>} "YES" if grantee is permitted
     *      to grant to others; "NO" if not; {@code null} if unknown
     *  </OL>
     *
     * @param catalog a catalog name; must match the catalog name as it
     *        is stored in the database; "" retrieves those without a catalog;
     *        {@code null} means that the catalog name should not be used to narrow
     *        the search
     * @param schemaPattern a schema name pattern; must match the schema name
     *        as it is stored in the database; "" retrieves those without a schema;
     *        {@code null} means that the schema name should not be used to narrow
     *        the search
     * @param tableNamePattern a table name pattern; must match the
     *        table name as it is stored in the database
     * @return {@code ResultSet} - each row is a table privilege description
     * @throws SQLException if a database access error occurs
     * @see #getSearchStringEscape
     */
    ResultSet getTablePrivileges(String catalog, String schemaPattern,
                                 String tableNamePattern) throws SQLException;

    /**
     * Retrieves a description of a table's optimal set of columns that
     * uniquely identifies a row. They are ordered by SCOPE.
     *
     * <P>Each column description has the following columns:
     *  <OL>
     *  <LI><B>SCOPE</B> short {@code =>} actual scope of result
     *      <UL>
     *      <LI> bestRowTemporary - very temporary, while using row
     *      <LI> bestRowTransaction - valid for remainder of current transaction
     *      <LI> bestRowSession - valid for remainder of current session
     *      </UL>
     *  <LI><B>COLUMN_NAME</B> String {@code =>} column name
     *  <LI><B>DATA_TYPE</B> int {@code =>} SQL data type from java.sql.Types
     *  <LI><B>TYPE_NAME</B> String {@code =>} Data source dependent type name,
     *  for a UDT the type name is fully qualified
     *  <LI><B>COLUMN_SIZE</B> int {@code =>} precision
     *  <LI><B>BUFFER_LENGTH</B> int {@code =>} not used
     *  <LI><B>DECIMAL_DIGITS</B> short  {@code =>} scale - Null is returned for data types where
     * DECIMAL_DIGITS is not applicable.
     *  <LI><B>PSEUDO_COLUMN</B> short {@code =>} is this a pseudo column
     *      like an Oracle ROWID
     *      <UL>
     *      <LI> bestRowUnknown - may or may not be pseudo column
     *      <LI> bestRowNotPseudo - is NOT a pseudo column
     *      <LI> bestRowPseudo - is a pseudo column
     *      </UL>
     *  </OL>
     *
     * <p>The COLUMN_SIZE column represents the specified column size for the given column.
     * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
     * For datetime datatypes, this is the length in characters of the String representation (assuming the
     * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
     * this is the length in bytes. Null is returned for data types where the
     * column size is not applicable.
     *
     * @param catalog a catalog name; must match the catalog name as it
     *        is stored in the database; "" retrieves those without a catalog;
     *        {@code null} means that the catalog name should not be used to narrow
     *        the search
     * @param schema a schema name; must match the schema name
     *        as it is stored in the database; "" retrieves those without a schema;
     *        {@code null} means that the schema name should not be used to narrow
     *        the search
     * @param table a table name; must match the table name as it is stored
     *        in the database
     * @param scope the scope of interest; use same values as SCOPE
     * @param nullable include columns that are nullable.
     * @return {@code ResultSet} - each row is a column description
     * @throws SQLException if a database access error occurs
     */
    ResultSet getBestRowIdentifier(String catalog, String schema,
                                   String table, int scope, boolean nullable) throws SQLException;

    /**
     * Indicates that the scope of the best row identifier is
     * very temporary, lasting only while the
     * row is being used.
     * <P>
     * A possible value for the column
     * {@code SCOPE}
     * in the {@code ResultSet} object
     * returned by the method {@code getBestRowIdentifier}.
     */
    int bestRowTemporary   = 0;

    /**
     * Indicates that the scope of the best row identifier is
     * the remainder of the current transaction.
     * <P>
     * A possible value for the column
     * {@code SCOPE}
     * in the {@code ResultSet} object
     * returned by the method {@code getBestRowIdentifier}.
     */
    int bestRowTransaction = 1;

    /**
     * Indicates that the scope of the best row identifier is
     * the remainder of the current session.
     * <P>
     * A possible value for the column
     * {@code SCOPE}
     * in the {@code ResultSet} object
     * returned by the method {@code getBestRowIdentifier}.
     */
    int bestRowSession     = 2;

    /**
     * Indicates that the best row identifier may or may not be a pseudo column.
     * <P>
     * A possible value for the column
     * {@code PSEUDO_COLUMN}
     * in the {@code ResultSet} object
     * returned by the method {@code getBestRowIdentifier}.
     */
    int bestRowUnknown  = 0;

    /**
     * Indicates that the best row identifier is NOT a pseudo column.
     * <P>
     * A possible value for the column
     * {@code PSEUDO_COLUMN}
     * in the {@code ResultSet} object
     * returned by the method {@code getBestRowIdentifier}.
     */
    int bestRowNotPseudo        = 1;

    /**
     * Indicates that the best row identifier is a pseudo column.
     * <P>
     * A possible value for the column
     * {@code PSEUDO_COLUMN}
     * in the {@code ResultSet} object
     * returned by the method {@code getBestRowIdentifier}.
     */
    int bestRowPseudo   = 2;

    /**
     * Retrieves a description of a table's columns that are automatically
     * updated when any value in a row is updated.  They are
     * unordered.
     *
     * <P>Each column description has the following columns:
     *  <OL>
     *  <LI><B>SCOPE</B> short {@code =>} is not used
     *  <LI><B>COLUMN_NAME</B> String {@code =>} column name
     *  <LI><B>DATA_TYPE</B> int {@code =>} SQL data type from {@code java.sql.Types}
     *  <LI><B>TYPE_NAME</B> String {@code =>} Data source-dependent type name
     *  <LI><B>COLUMN_SIZE</B> int {@code =>} precision
     *  <LI><B>BUFFER_LENGTH</B> int {@code =>} length of column value in bytes
     *  <LI><B>DECIMAL_DIGITS</B> short  {@code =>} scale - Null is returned for data types where
     * DECIMAL_DIGITS is not applicable.
     *  <LI><B>PSEUDO_COLUMN</B> short {@code =>} whether this is pseudo column
     *      like an Oracle ROWID
     *      <UL>
     *      <LI> versionColumnUnknown - may or may not be pseudo column
     *      <LI> versionColumnNotPseudo - is NOT a pseudo column
     *      <LI> versionColumnPseudo - is a pseudo column
     *      </UL>
     *  </OL>
     *
     * <p>The COLUMN_SIZE column represents the specified column size for the given column.
     * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
     * For datetime datatypes, this is the length in characters of the String representation (assuming the
     * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
     * this is the length in bytes. Null is returned for data types where the
     * column size is not applicable.
     * @param catalog a catalog name; must match the catalog name as it
     *        is stored in the database; "" retrieves those without a catalog;
     *        {@code null} means that the catalog name should not be used to narrow
     *        the search
     * @param schema a schema name; must match the schema name
     *        as it is stored in the database; "" retrieves those without a schema;
     *        {@code null} means that the schema name should not be used to narrow
     *        the search
     * @param table a table name; must match the table name as it is stored
     *        in the database
     * @return a {@code ResultSet} object in which each row is a
     *         column description
     * @throws SQLException if a database access error occurs
     */
    ResultSet getVersionColumns(String catalog, String schema,
                                String table) throws SQLException;

    /**
     * Indicates that this version column may or may not be a pseudo column.
     * <P>
     * A possible value for the column
     * {@code PSEUDO_COLUMN}
     * in the {@code ResultSet} object
     * returned by the method {@code getVersionColumns}.
     */
    int versionColumnUnknown    = 0;

    /**
     * Indicates that this version column is NOT a pseudo column.
     * <P>
     * A possible value for the column
     * {@code PSEUDO_COLUMN}
     * in the {@code ResultSet} object
     * returned by the method {@code getVersionColumns}.
     */
    int versionColumnNotPseudo  = 1;

    /**
     * Indicates that this version column is a pseudo column.
     * <P>
     * A possible value for the column
     * {@code PSEUDO_COLUMN}
     * in the {@code ResultSet} object
     * returned by the method {@code getVersionColumns}.
     */
    int versionColumnPseudo     = 2;

    /**
     * Retrieves a description of the given table's primary key columns.  They
     * are ordered by COLUMN_NAME.
     *
     * <P>Each primary key column description has the following columns:
     *  <OL>
     *  <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be {@code null})
     *  <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be {@code null})
     *  <LI><B>TABLE_NAME</B> String {@code =>} table name
     *  <LI><B>COLUMN_NAME</B> String {@code =>} column name
     *  <LI><B>KEY_SEQ</B> short {@code =>} sequence number within primary key( a value
     *  of 1 represents the first column of the primary key, a value of 2 would
     *  represent the second column within the primary key).
     *  <LI><B>PK_NAME</B> String {@code =>} primary key name (may be {@code null})
     *  </OL>
     *
     * @param catalog a catalog name; must match the catalog name as it
     *        is stored in the database; "" retrieves those without a catalog;
     *        {@code null} means that the catalog name should not be used to narrow
     *        the search
     * @param schema a schema name; must match the schema name
     *        as it is stored in the database; "" retrieves those without a schema;
     *        {@code null} means that the schema name should not be used to narrow
     *        the search
     * @param table a table name; must match the table name as it is stored
     *        in the database
     * @return {@code ResultSet} - each row is a primary key column description
     * @throws SQLException if a database access error occurs
     */
    ResultSet getPrimaryKeys(String catalog, String schema,
                             String table) throws SQLException;

    /**
     * Retrieves a description of the primary key columns that are
     * referenced by the given table's foreign key columns (the primary keys
     * imported by a table).  They are ordered by PKTABLE_CAT,
     * PKTABLE_SCHEM, PKTABLE_NAME, and KEY_SEQ.
     *
     * <P>Each primary key column description has the following columns:
     *  <OL>
     *  <LI><B>PKTABLE_CAT</B> String {@code =>} primary key table catalog
     *      being imported (may be {@code null})
     *  <LI><B>PKTABLE_SCHEM</B> String {@code =>} primary key table schema
     *      being imported (may be {@code null})
     *  <LI><B>PKTABLE_NAME</B> String {@code =>} primary key table name
     *      being imported
     *  <LI><B>PKCOLUMN_NAME</B> String {@code =>} primary key column name
     *      being imported
     *  <LI><B>FKTABLE_CAT</B> String {@code =>} foreign key table catalog (may be {@code null})
     *  <LI><B>FKTABLE_SCHEM</B> String {@code =>} foreign key table schema (may be {@code null})
     *  <LI><B>FKTABLE_NAME</B> String {@code =>} foreign key table name
     *  <LI><B>FKCOLUMN_NAME</B> String {@code =>} foreign key column name
     *  <LI><B>KEY_SEQ</B> short {@code =>} sequence number within a foreign key( a value
     *  of 1 represents the first column of the foreign key, a value of 2 would
     *  represent the second column within the foreign key).
     *  <LI><B>UPDATE_RULE</B> short {@code =>} What happens to a
     *       foreign key when the primary key is updated:
     *      <UL>
     *      <LI> importedNoAction - do not allow update of primary
     *               key if it has been imported
     *      <LI> importedKeyCascade - change imported key to agree
     *               with primary key update
     *      <LI> importedKeySetNull - change imported key to {@code NULL}
     *               if its primary key has been updated
     *      <LI> importedKeySetDefault - change imported key to default values
     *               if its primary key has been updated
     *      <LI> importedKeyRestrict - same as importedKeyNoAction
     *                                 (for ODBC 2.x compatibility)
     *      </UL>
     *  <LI><B>DELETE_RULE</B> short {@code =>} What happens to
     *      the foreign key when primary is deleted.
     *      <UL>
     *      <LI> importedKeyNoAction - do not allow delete of primary
     *               key if it has been imported
     *      <LI> importedKeyCascade - delete rows that import a deleted key
     *      <LI> importedKeySetNull - change imported key to NULL if
     *               its primary key has been deleted
     *      <LI> importedKeyRestrict - same as importedKeyNoAction
     *                                 (for ODBC 2.x compatibility)
     *      <LI> importedKeySetDefault - change imported key to default if
     *               its primary key has been deleted
     *      </UL>
     *  <LI><B>FK_NAME</B> String {@code =>} foreign key name (may be {@code null})
     *  <LI><B>PK_NAME</B> String {@code =>} primary key name (may be {@code null})
     *  <LI><B>DEFERRABILITY</B> short {@code =>} can the evaluation of foreign key
     *      constraints be deferred until commit
     *      <UL>
     *      <LI> importedKeyInitiallyDeferred - see SQL92 for definition
     *      <LI> importedKeyInitiallyImmediate - see SQL92 for definition
     *      <LI> importedKeyNotDeferrable - see SQL92 for definition
     *      </UL>
     *  </OL>
     *
     * @param catalog a catalog name; must match the catalog name as it
     *        is stored in the database; "" retrieves those without a catalog;
     *        {@code null} means that the catalog name should not be used to narrow
     *        the search
     * @param schema a schema name; must match the schema name
     *        as it is stored in the database; "" retrieves those without a schema;
     *        {@code null} means that the schema name should not be used to narrow
     *        the search
     * @param table a table name; must match the table name as it is stored
     *        in the database
     * @return {@code ResultSet} - each row is a primary key column description
     * @throws SQLException if a database access error occurs
     * @see #getExportedKeys
     */
    ResultSet getImportedKeys(String catalog, String schema,
                              String table) throws SQLException;

    /**
     * For the column {@code UPDATE_RULE},
     * indicates that
     * when the primary key is updated, the foreign key (imported key)
     * is changed to agree with it.
     * For the column {@code DELETE_RULE},
     * it indicates that
     * when the primary key is deleted, rows that imported that key
     * are deleted.
     * <P>
     * A possible value for the columns {@code UPDATE_RULE}
     * and {@code DELETE_RULE} in the
     * {@code ResultSet} objects returned by the methods
     * {@code getImportedKeys},  {@code getExportedKeys},
     * and {@code getCrossReference}.
     */
    int importedKeyCascade      = 0;

    /**
     * For the column {@code UPDATE_RULE}, indicates that
     * a primary key may not be updated if it has been imported by
     * another table as a foreign key.
     * For the column {@code DELETE_RULE}, indicates that
     * a primary key may not be deleted if it has been imported by
     * another table as a foreign key.
     * <P>
     * A possible value for the columns {@code UPDATE_RULE}
     * and {@code DELETE_RULE} in the
     * {@code ResultSet} objects returned by the methods
     * {@code getImportedKeys},  {@code getExportedKeys},
     * and {@code getCrossReference}.
     */
    int importedKeyRestrict = 1;

    /**
     * For the columns {@code UPDATE_RULE}
     * and {@code DELETE_RULE}, indicates that
     * when the primary key is updated or deleted, the foreign key (imported key)
     * is changed to {@code NULL}.
     * <P>
     * A possible value for the columns {@code UPDATE_RULE}
     * and {@code DELETE_RULE} in the
     * {@code ResultSet} objects returned by the methods
     * {@code getImportedKeys},  {@code getExportedKeys},
     * and {@code getCrossReference}.
     */
    int importedKeySetNull  = 2;

    /**
     * For the columns {@code UPDATE_RULE}
     * and {@code DELETE_RULE}, indicates that
     * if the primary key has been imported, it cannot be updated or deleted.
     * <P>
     * A possible value for the columns {@code UPDATE_RULE}
     * and {@code DELETE_RULE} in the
     * {@code ResultSet} objects returned by the methods
     * {@code getImportedKeys},  {@code getExportedKeys},
     * and {@code getCrossReference}.
     */
    int importedKeyNoAction = 3;

    /**
     * For the columns {@code UPDATE_RULE}
     * and {@code DELETE_RULE}, indicates that
     * if the primary key is updated or deleted, the foreign key (imported key)
     * is set to the default value.
     * <P>
     * A possible value for the columns {@code UPDATE_RULE}
     * and {@code DELETE_RULE} in the
     * {@code ResultSet} objects returned by the methods
     * {@code getImportedKeys},  {@code getExportedKeys},
     * and {@code getCrossReference}.
     */
    int importedKeySetDefault  = 4;

    /**
     * Indicates deferrability.  See SQL-92 for a definition.
     * <P>
     * A possible value for the column {@code DEFERRABILITY}
     * in the {@code ResultSet} objects returned by the methods
     * {@code getImportedKeys},  {@code getExportedKeys},
     * and {@code getCrossReference}.
     */
    int importedKeyInitiallyDeferred  = 5;

    /**
     * Indicates deferrability.  See SQL-92 for a definition.
     * <P>
     * A possible value for the column {@code DEFERRABILITY}
     * in the {@code ResultSet} objects returned by the methods
     * {@code getImportedKeys},  {@code getExportedKeys},
     * and {@code getCrossReference}.
     */
    int importedKeyInitiallyImmediate  = 6;

    /**
     * Indicates deferrability.  See SQL-92 for a definition.
     * <P>
     * A possible value for the column {@code DEFERRABILITY}
     * in the {@code ResultSet} objects returned by the methods
     * {@code getImportedKeys},  {@code getExportedKeys},
     * and {@code getCrossReference}.
     */
    int importedKeyNotDeferrable  = 7;

    /**
     * Retrieves a description of the foreign key columns that reference the
     * given table's primary key columns (the foreign keys exported by a
     * table).  They are ordered by FKTABLE_CAT, FKTABLE_SCHEM,
     * FKTABLE_NAME, and KEY_SEQ.
     *
     * <P>Each foreign key column description has the following columns:
     *  <OL>
     *  <LI><B>PKTABLE_CAT</B> String {@code =>} primary key table catalog (may be {@code null})
     *  <LI><B>PKTABLE_SCHEM</B> String {@code =>} primary key table schema (may be {@code null})
     *  <LI><B>PKTABLE_NAME</B> String {@code =>} primary key table name
     *  <LI><B>PKCOLUMN_NAME</B> String {@code =>} primary key column name
     *  <LI><B>FKTABLE_CAT</B> String {@code =>} foreign key table catalog (may be {@code null})
     *      being exported (may be {@code null})
     *  <LI><B>FKTABLE_SCHEM</B> String {@code =>} foreign key table schema (may be {@code null})
     *      being exported (may be {@code null})
     *  <LI><B>FKTABLE_NAME</B> String {@code =>} foreign key table name
     *      being exported
     *  <LI><B>FKCOLUMN_NAME</B> String {@code =>} foreign key column name
     *      being exported
     *  <LI><B>KEY_SEQ</B> short {@code =>} sequence number within foreign key( a value
     *  of 1 represents the first column of the foreign key, a value of 2 would
     *  represent the second column within the foreign key).
     *  <LI><B>UPDATE_RULE</B> short {@code =>} What happens to
     *       foreign key when primary is updated:
     *      <UL>
     *      <LI> importedNoAction - do not allow update of primary
     *               key if it has been imported
     *      <LI> importedKeyCascade - change imported key to agree
     *               with primary key update
     *      <LI> importedKeySetNull - change imported key to {@code NULL} if
     *               its primary key has been updated
     *      <LI> importedKeySetDefault - change imported key to default values
     *               if its primary key has been updated
     *      <LI> importedKeyRestrict - same as importedKeyNoAction
     *                                 (for ODBC 2.x compatibility)
     *      </UL>
     *  <LI><B>DELETE_RULE</B> short {@code =>} What happens to
     *      the foreign key when primary is deleted.
     *      <UL>
     *      <LI> importedKeyNoAction - do not allow delete of primary
     *               key if it has been imported
     *      <LI> importedKeyCascade - delete rows that import a deleted key
     *      <LI> importedKeySetNull - change imported key to {@code NULL} if
     *               its primary key has been deleted
     *      <LI> importedKeyRestrict - same as importedKeyNoAction
     *                                 (for ODBC 2.x compatibility)
     *      <LI> importedKeySetDefault - change imported key to default if
     *               its primary key has been deleted
     *      </UL>
     *  <LI><B>FK_NAME</B> String {@code =>} foreign key name (may be {@code null})
     *  <LI><B>PK_NAME</B> String {@code =>} primary key name (may be {@code null})
     *  <LI><B>DEFERRABILITY</B> short {@code =>} can the evaluation of foreign key
     *      constraints be deferred until commit
     *      <UL>
     *      <LI> importedKeyInitiallyDeferred - see SQL92 for definition
     *      <LI> importedKeyInitiallyImmediate - see SQL92 for definition
     *      <LI> importedKeyNotDeferrable - see SQL92 for definition
     *      </UL>
     *  </OL>
     *
     * @param catalog a catalog name; must match the catalog name as it
     *        is stored in this database; "" retrieves those without a catalog;
     *        {@code null} means that the catalog name should not be used to narrow
     *        the search
     * @param schema a schema name; must match the schema name
     *        as it is stored in the database; "" retrieves those without a schema;
     *        {@code null} means that the schema name should not be used to narrow
     *        the search
     * @param table a table name; must match the table name as it is stored
     *        in this database
     * @return a {@code ResultSet} object in which each row is a
     *         foreign key column description
     * @throws SQLException if a database access error occurs
     * @see #getImportedKeys
     */
    ResultSet getExportedKeys(String catalog, String schema,
                              String table) throws SQLException;

    /**
     * Retrieves a description of the foreign key columns in the given foreign key
     * table that reference the primary key or the columns representing a unique constraint of the  parent table (could be the same or a different table).
     * The number of columns returned from the parent table must match the number of
     * columns that make up the foreign key.  They
     * are ordered by FKTABLE_CAT, FKTABLE_SCHEM, FKTABLE_NAME, and
     * KEY_SEQ.
     *
     * <P>Each foreign key column description has the following columns:
     *  <OL>
     *  <LI><B>PKTABLE_CAT</B> String {@code =>} parent key table catalog (may be {@code null})
     *  <LI><B>PKTABLE_SCHEM</B> String {@code =>} parent key table schema (may be {@code null})
     *  <LI><B>PKTABLE_NAME</B> String {@code =>} parent key table name
     *  <LI><B>PKCOLUMN_NAME</B> String {@code =>} parent key column name
     *  <LI><B>FKTABLE_CAT</B> String {@code =>} foreign key table catalog (may be {@code null})
     *      being exported (may be {@code null})
     *  <LI><B>FKTABLE_SCHEM</B> String {@code =>} foreign key table schema (may be {@code null})
     *      being exported (may be {@code null})
     *  <LI><B>FKTABLE_NAME</B> String {@code =>} foreign key table name
     *      being exported
     *  <LI><B>FKCOLUMN_NAME</B> String {@code =>} foreign key column name
     *      being exported
     *  <LI><B>KEY_SEQ</B> short {@code =>} sequence number within foreign key( a value
     *  of 1 represents the first column of the foreign key, a value of 2 would
     *  represent the second column within the foreign key).
     *  <LI><B>UPDATE_RULE</B> short {@code =>} What happens to
     *       foreign key when parent key is updated:
     *      <UL>
     *      <LI> importedNoAction - do not allow update of parent
     *               key if it has been imported
     *      <LI> importedKeyCascade - change imported key to agree
     *               with parent key update
     *      <LI> importedKeySetNull - change imported key to {@code NULL} if
     *               its parent key has been updated
     *      <LI> importedKeySetDefault - change imported key to default values
     *               if its parent key has been updated
     *      <LI> importedKeyRestrict - same as importedKeyNoAction
     *                                 (for ODBC 2.x compatibility)
     *      </UL>
     *  <LI><B>DELETE_RULE</B> short {@code =>} What happens to
     *      the foreign key when parent key is deleted.
     *      <UL>
     *      <LI> importedKeyNoAction - do not allow delete of parent
     *               key if it has been imported
     *      <LI> importedKeyCascade - delete rows that import a deleted key
     *      <LI> importedKeySetNull - change imported key to {@code NULL} if
     *               its primary key has been deleted
     *      <LI> importedKeyRestrict - same as importedKeyNoAction
     *                                 (for ODBC 2.x compatibility)
     *      <LI> importedKeySetDefault - change imported key to default if
     *               its parent key has been deleted
     *      </UL>
     *  <LI><B>FK_NAME</B> String {@code =>} foreign key name (may be {@code null})
     *  <LI><B>PK_NAME</B> String {@code =>} parent key name (may be {@code null})
     *  <LI><B>DEFERRABILITY</B> short {@code =>} can the evaluation of foreign key
     *      constraints be deferred until commit
     *      <UL>
     *      <LI> importedKeyInitiallyDeferred - see SQL92 for definition
     *      <LI> importedKeyInitiallyImmediate - see SQL92 for definition
     *      <LI> importedKeyNotDeferrable - see SQL92 for definition
     *      </UL>
     *  </OL>
     *
     * @param parentCatalog a catalog name; must match the catalog name
     * as it is stored in the database; "" retrieves those without a
     * catalog; {@code null} means drop catalog name from the selection criteria
     * @param parentSchema a schema name; must match the schema name as
     * it is stored in the database; "" retrieves those without a schema;
     * {@code null} means drop schema name from the selection criteria
     * @param parentTable the name of the table that exports the key; must match
     * the table name as it is stored in the database
     * @param foreignCatalog a catalog name; must match the catalog name as
     * it is stored in the database; "" retrieves those without a
     * catalog; {@code null} means drop catalog name from the selection criteria
     * @param foreignSchema a schema name; must match the schema name as it
     * is stored in the database; "" retrieves those without a schema;
     * {@code null} means drop schema name from the selection criteria
     * @param foreignTable the name of the table that imports the key; must match
     * the table name as it is stored in the database
     * @return {@code ResultSet} - each row is a foreign key column description
     * @throws SQLException if a database access error occurs
     * @see #getImportedKeys
     */
    ResultSet getCrossReference(
                                String parentCatalog, String parentSchema, String parentTable,
                                String foreignCatalog, String foreignSchema, String foreignTable
                                ) throws SQLException;

    /**
     * Retrieves a description of all the data types supported by
     * this database. They are ordered by DATA_TYPE and then by how
     * closely the data type maps to the corresponding JDBC SQL type.
     *
     * <P>If the database supports SQL distinct types, then getTypeInfo() will return
     * a single row with a TYPE_NAME of DISTINCT and a DATA_TYPE of Types.DISTINCT.
     * If the database supports SQL structured types, then getTypeInfo() will return
     * a single row with a TYPE_NAME of STRUCT and a DATA_TYPE of Types.STRUCT.
     *
     * <P>If SQL distinct or structured types are supported, then information on the
     * individual types may be obtained from the getUDTs() method.
     *
     *
     * <P>Each type description has the following columns:
     *  <OL>
     *  <LI><B>TYPE_NAME</B> String {@code =>} Type name
     *  <LI><B>DATA_TYPE</B> int {@code =>} SQL data type from java.sql.Types
     *  <LI><B>PRECISION</B> int {@code =>} maximum precision
     *  <LI><B>LITERAL_PREFIX</B> String {@code =>} prefix used to quote a literal
     *      (may be {@code null})
     *  <LI><B>LITERAL_SUFFIX</B> String {@code =>} suffix used to quote a literal
     *  (may be {@code null})
     *  <LI><B>CREATE_PARAMS</B> String {@code =>} parameters used in creating
     *      the type (may be {@code null})
     *  <LI><B>NULLABLE</B> short {@code =>} can you use NULL for this type.
     *      <UL>
     *      <LI> typeNoNulls - does not allow NULL values
     *      <LI> typeNullable - allows NULL values
     *      <LI> typeNullableUnknown - nullability unknown
     *      </UL>
     *  <LI><B>CASE_SENSITIVE</B> boolean{@code =>} is it case sensitive.
     *  <LI><B>SEARCHABLE</B> short {@code =>} can you use "WHERE" based on this type:
     *      <UL>
     *      <LI> typePredNone - No support
     *      <LI> typePredChar - Only supported with WHERE .. LIKE
     *      <LI> typePredBasic - Supported except for WHERE .. LIKE
     *      <LI> typeSearchable - Supported for all WHERE ..
     *      </UL>
     *  <LI><B>UNSIGNED_ATTRIBUTE</B> boolean {@code =>} is it unsigned.
     *  <LI><B>FIXED_PREC_SCALE</B> boolean {@code =>} can it be a money value.
     *  <LI><B>AUTO_INCREMENT</B> boolean {@code =>} can it be used for an
     *      auto-increment value.
     *  <LI><B>LOCAL_TYPE_NAME</B> String {@code =>} localized version of type name
     *      (may be {@code null})
     *  <LI><B>MINIMUM_SCALE</B> short {@code =>} minimum scale supported
     *  <LI><B>MAXIMUM_SCALE</B> short {@code =>} maximum scale supported
     *  <LI><B>SQL_DATA_TYPE</B> int {@code =>} unused
     *  <LI><B>SQL_DATETIME_SUB</B> int {@code =>} unused
     *  <LI><B>NUM_PREC_RADIX</B> int {@code =>} usually 2 or 10
     *  </OL>
     *
     * <p>The PRECISION column represents the maximum column size that the server supports for the given datatype.
     * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
     * For datetime datatypes, this is the length in characters of the String representation (assuming the
     * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
     * this is the length in bytes. Null is returned for data types where the
     * column size is not applicable.
     *
     * @return a {@code ResultSet} object in which each row is an SQL
     *         type description
     * @throws SQLException if a database access error occurs
     */
    ResultSet getTypeInfo() throws SQLException;

    /**
     * Indicates that a {@code NULL} value is NOT allowed for this
     * data type.
     * <P>
     * A possible value for column {@code NULLABLE} in the
     * {@code ResultSet} object returned by the method
     * {@code getTypeInfo}.
     */
    int typeNoNulls = 0;

    /**
     * Indicates that a {@code NULL} value is allowed for this
     * data type.
     * <P>
     * A possible value for column {@code NULLABLE} in the
     * {@code ResultSet} object returned by the method
     * {@code getTypeInfo}.
     */
    int typeNullable = 1;

    /**
     * Indicates that it is not known whether a {@code NULL} value
     * is allowed for this data type.
     * <P>
     * A possible value for column {@code NULLABLE} in the
     * {@code ResultSet} object returned by the method
     * {@code getTypeInfo}.
     */
    int typeNullableUnknown = 2;

    /**
     * Indicates that {@code WHERE} search clauses are not supported
     * for this type.
     * <P>
     * A possible value for column {@code SEARCHABLE} in the
     * {@code ResultSet} object returned by the method
     * {@code getTypeInfo}.
     */
    int typePredNone = 0;

    /**
     * Indicates that the data type
     * can be only be used in {@code WHERE} search clauses
     * that  use {@code LIKE} predicates.
     * <P>
     * A possible value for column {@code SEARCHABLE} in the
     * {@code ResultSet} object returned by the method
     * {@code getTypeInfo}.
     */
    int typePredChar = 1;

    /**
     * Indicates that the data type can be only be used in {@code WHERE}
     * search clauses
     * that do not use {@code LIKE} predicates.
     * <P>
     * A possible value for column {@code SEARCHABLE} in the
     * {@code ResultSet} object returned by the method
     * {@code getTypeInfo}.
     */
    int typePredBasic = 2;

    /**
     * Indicates that all {@code WHERE} search clauses can be
     * based on this type.
     * <P>
     * A possible value for column {@code SEARCHABLE} in the
     * {@code ResultSet} object returned by the method
     * {@code getTypeInfo}.
     */
    int typeSearchable  = 3;

    /**
     * Retrieves a description of the given table's indices and statistics. They are
     * ordered by NON_UNIQUE, TYPE, INDEX_NAME, and ORDINAL_POSITION.
     *
     * <P>Each index column description has the following columns:
     *  <OL>
     *  <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be {@code null})
     *  <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be {@code null})
     *  <LI><B>TABLE_NAME</B> String {@code =>} table name
     *  <LI><B>NON_UNIQUE</B> boolean {@code =>} Can index values be non-unique.
     *      false when TYPE is tableIndexStatistic
     *  <LI><B>INDEX_QUALIFIER</B> String {@code =>} index catalog (may be {@code null});
     *      {@code null} when TYPE is tableIndexStatistic
     *  <LI><B>INDEX_NAME</B> String {@code =>} index name; {@code null} when TYPE is
     *      tableIndexStatistic
     *  <LI><B>TYPE</B> short {@code =>} index type:
     *      <UL>
     *      <LI> tableIndexStatistic - this identifies table statistics that are
     *           returned in conjunction with a table's index descriptions
     *      <LI> tableIndexClustered - this is a clustered index
     *      <LI> tableIndexHashed - this is a hashed index
     *      <LI> tableIndexOther - this is some other style of index
     *      </UL>
     *  <LI><B>ORDINAL_POSITION</B> short {@code =>} column sequence number
     *      within index; zero when TYPE is tableIndexStatistic
     *  <LI><B>COLUMN_NAME</B> String {@code =>} column name; {@code null} when TYPE is
     *      tableIndexStatistic
     *  <LI><B>ASC_OR_DESC</B> String {@code =>} column sort sequence, "A" {@code =>} ascending,
     *      "D" {@code =>} descending, may be {@code null} if sort sequence is not supported;
     *      {@code null} when TYPE is tableIndexStatistic
     *  <LI><B>CARDINALITY</B> long {@code =>} When TYPE is tableIndexStatistic, then
     *      this is the number of rows in the table; otherwise, it is the
     *      number of unique values in the index.
     *  <LI><B>PAGES</B> long {@code =>} When TYPE is  tableIndexStatistic then
     *      this is the number of pages used for the table, otherwise it
     *      is the number of pages used for the current index.
     *  <LI><B>FILTER_CONDITION</B> String {@code =>} Filter condition, if any.
     *      (may be {@code null})
     *  </OL>
     *
     * @param catalog a catalog name; must match the catalog name as it
     *        is stored in this database; "" retrieves those without a catalog;
     *        {@code null} means that the catalog name should not be used to narrow
     *        the search
     * @param schema a schema name; must match the schema name
     *        as it is stored in this database; "" retrieves those without a schema;
     *        {@code null} means that the schema name should not be used to narrow
     *        the search
     * @param table a table name; must match the table name as it is stored
     *        in this database
     * @param unique when true, return only indices for unique values;
     *     when false, return indices regardless of whether unique or not
     * @param approximate when true, result is allowed to reflect approximate
     *     or out of data values; when false, results are requested to be
     *     accurate
     * @return {@code ResultSet} - each row is an index column description
     * @throws SQLException if a database access error occurs
     */
    ResultSet getIndexInfo(String catalog, String schema, String table,
                           boolean unique, boolean approximate)
        throws SQLException;

    /**
     * Indicates that this column contains table statistics that
     * are returned in conjunction with a table's index descriptions.
     * <P>
     * A possible value for column {@code TYPE} in the
     * {@code ResultSet} object returned by the method
     * {@code getIndexInfo}.
     */
    short tableIndexStatistic = 0;

    /**
     * Indicates that this table index is a clustered index.
     * <P>
     * A possible value for column {@code TYPE} in the
     * {@code ResultSet} object returned by the method
     * {@code getIndexInfo}.
     */
    short tableIndexClustered = 1;

    /**
     * Indicates that this table index is a hashed index.
     * <P>
     * A possible value for column {@code TYPE} in the
     * {@code ResultSet} object returned by the method
     * {@code getIndexInfo}.
     */
    short tableIndexHashed    = 2;

    /**
     * Indicates that this table index is not a clustered
     * index, a hashed index, or table statistics;
     * it is something other than these.
     * <P>
     * A possible value for column {@code TYPE} in the
     * {@code ResultSet} object returned by the method
     * {@code getIndexInfo}.
     */
    short tableIndexOther     = 3;

    //--------------------------JDBC 2.0-----------------------------

    /**
     * Retrieves whether this database supports the given result set type.
     *
     * @param type defined in {@code java.sql.ResultSet}
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @see Connection
     * @since 1.2
     */
    boolean supportsResultSetType(int type) throws SQLException;

    /**
     * Retrieves whether this database supports the given concurrency type
     * in combination with the given result set type.
     *
     * @param type defined in {@code java.sql.ResultSet}
     * @param concurrency type defined in {@code java.sql.ResultSet}
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @see Connection
     * @since 1.2
     */
    boolean supportsResultSetConcurrency(int type, int concurrency)
        throws SQLException;

    /**
     *
     * Retrieves whether for the given type of {@code ResultSet} object,
     * the result set's own updates are visible.
     *
     * @param type the {@code ResultSet} type; one of
     *        {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     *        {@code ResultSet.TYPE_SCROLL_SENSITIVE}
     * @return {@code true} if updates are visible for the given result set type;
     *        {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.2
     */
    boolean ownUpdatesAreVisible(int type) throws SQLException;

    /**
     * Retrieves whether a result set's own deletes are visible.
     *
     * @param type the {@code ResultSet} type; one of
     *        {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     *        {@code ResultSet.TYPE_SCROLL_SENSITIVE}
     * @return {@code true} if deletes are visible for the given result set type;
     *        {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.2
     */
    boolean ownDeletesAreVisible(int type) throws SQLException;

    /**
     * Retrieves whether a result set's own inserts are visible.
     *
     * @param type the {@code ResultSet} type; one of
     *        {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     *        {@code ResultSet.TYPE_SCROLL_SENSITIVE}
     * @return {@code true} if inserts are visible for the given result set type;
     *        {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.2
     */
    boolean ownInsertsAreVisible(int type) throws SQLException;

    /**
     * Retrieves whether updates made by others are visible.
     *
     * @param type the {@code ResultSet} type; one of
     *        {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     *        {@code ResultSet.TYPE_SCROLL_SENSITIVE}
     * @return {@code true} if updates made by others
     *        are visible for the given result set type;
     *        {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.2
     */
    boolean othersUpdatesAreVisible(int type) throws SQLException;

    /**
     * Retrieves whether deletes made by others are visible.
     *
     * @param type the {@code ResultSet} type; one of
     *        {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     *        {@code ResultSet.TYPE_SCROLL_SENSITIVE}
     * @return {@code true} if deletes made by others
     *        are visible for the given result set type;
     *        {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.2
     */
    boolean othersDeletesAreVisible(int type) throws SQLException;

    /**
     * Retrieves whether inserts made by others are visible.
     *
     * @param type the {@code ResultSet} type; one of
     *        {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     *        {@code ResultSet.TYPE_SCROLL_SENSITIVE}
     * @return {@code true} if inserts made by others
     *         are visible for the given result set type;
     *         {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.2
     */
    boolean othersInsertsAreVisible(int type) throws SQLException;

    /**
     * Retrieves whether or not a visible row update can be detected by
     * calling the method {@code ResultSet.rowUpdated}.
     *
     * @param type the {@code ResultSet} type; one of
     *        {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     *        {@code ResultSet.TYPE_SCROLL_SENSITIVE}
     * @return {@code true} if changes are detected by the result set type;
     *         {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.2
     */
    boolean updatesAreDetected(int type) throws SQLException;

    /**
     * Retrieves whether or not a visible row delete can be detected by
     * calling the method {@code ResultSet.rowDeleted}.  If the method
     * {@code deletesAreDetected} returns {@code false}, it means that
     * deleted rows are removed from the result set.
     *
     * @param type the {@code ResultSet} type; one of
     *        {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     *        {@code ResultSet.TYPE_SCROLL_SENSITIVE}
     * @return {@code true} if deletes are detected by the given result set type;
     *         {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.2
     */
    boolean deletesAreDetected(int type) throws SQLException;

    /**
     * Retrieves whether or not a visible row insert can be detected
     * by calling the method {@code ResultSet.rowInserted}.
     *
     * @param type the {@code ResultSet} type; one of
     *        {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     *        {@code ResultSet.TYPE_SCROLL_SENSITIVE}
     * @return {@code true} if changes are detected by the specified result
     *         set type; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.2
     */
    boolean insertsAreDetected(int type) throws SQLException;

    /**
     * Retrieves whether this database supports batch updates.
     *
     * @return {@code true} if this database supports batch updates;
     *         {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.2
     */
    boolean supportsBatchUpdates() throws SQLException;

    /**
     * Retrieves a description of the user-defined types (UDTs) defined
     * in a particular schema.  Schema-specific UDTs may have type
     * {@code JAVA_OBJECT}, {@code STRUCT},
     * or {@code DISTINCT}.
     *
     * <P>Only types matching the catalog, schema, type name and type
     * criteria are returned.  They are ordered by {@code DATA_TYPE},
     * {@code TYPE_CAT}, {@code TYPE_SCHEM}  and
     * {@code TYPE_NAME}.  The type name parameter may be a fully-qualified
     * name.  In this case, the catalog and schemaPattern parameters are
     * ignored.
     *
     * <P>Each type description has the following columns:
     *  <OL>
     *  <LI><B>TYPE_CAT</B> String {@code =>} the type's catalog (may be {@code null})
     *  <LI><B>TYPE_SCHEM</B> String {@code =>} type's schema (may be {@code null})
     *  <LI><B>TYPE_NAME</B> String {@code =>} type name
     *  <LI><B>CLASS_NAME</B> String {@code =>} Java class name
     *  <LI><B>DATA_TYPE</B> int {@code =>} type value defined in java.sql.Types.
     *     One of JAVA_OBJECT, STRUCT, or DISTINCT
     *  <LI><B>REMARKS</B> String {@code =>} explanatory comment on the type
     *  <LI><B>BASE_TYPE</B> short {@code =>} type code of the source type of a
     *     DISTINCT type or the type that implements the user-generated
     *     reference type of the SELF_REFERENCING_COLUMN of a structured
     *     type as defined in java.sql.Types ({@code null} if DATA_TYPE is not
     *     DISTINCT or not STRUCT with REFERENCE_GENERATION = USER_DEFINED)
     *  </OL>
     *
     * <P><B>Note:</B> If the driver does not support UDTs, an empty
     * result set is returned.
     *
     * @param catalog a catalog name; must match the catalog name as it
     *        is stored in the database; "" retrieves those without a catalog;
     *        {@code null} means that the catalog name should not be used to narrow
     *        the search
     * @param schemaPattern a schema pattern name; must match the schema name
     *        as it is stored in the database; "" retrieves those without a schema;
     *        {@code null} means that the schema name should not be used to narrow
     *        the search
     * @param typeNamePattern a type name pattern; must match the type name
     *        as it is stored in the database; may be a fully qualified name
     * @param types a list of user-defined types (JAVA_OBJECT,
     *        STRUCT, or DISTINCT) to include; {@code null} returns all types
     * @return {@code ResultSet} object in which each row describes a UDT
     * @throws SQLException if a database access error occurs
     * @see #getSearchStringEscape
     * @since 1.2
     */
    ResultSet getUDTs(String catalog, String schemaPattern,
                      String typeNamePattern, int[] types)
        throws SQLException;

    /**
     * Retrieves the connection that produced this metadata object.
     *
     * @return the connection that produced this metadata object
     * @throws SQLException if a database access error occurs
     * @since 1.2
     */
    Connection getConnection() throws SQLException;

    // ------------------- JDBC 3.0 -------------------------

    /**
     * Retrieves whether this database supports savepoints.
     *
     * @return {@code true} if savepoints are supported;
     *         {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.4
     */
    boolean supportsSavepoints() throws SQLException;

    /**
     * Retrieves whether this database supports named parameters to callable
     * statements.
     *
     * @return {@code true} if named parameters are supported;
     *         {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.4
     */
    boolean supportsNamedParameters() throws SQLException;

    /**
     * Retrieves whether it is possible to have multiple {@code ResultSet} objects
     * returned from a {@code CallableStatement} object
     * simultaneously.
     *
     * @return {@code true} if a {@code CallableStatement} object
     *         can return multiple {@code ResultSet} objects
     *         simultaneously; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.4
     */
    boolean supportsMultipleOpenResults() throws SQLException;

    /**
     * Retrieves whether auto-generated keys can be retrieved after
     * a statement has been executed
     *
     * @return {@code true} if auto-generated keys can be retrieved
     *         after a statement has executed; {@code false} otherwise
     * <p>If {@code true} is returned, the JDBC driver must support the
     * returning of auto-generated keys for at least SQL INSERT statements
     *
     * @throws SQLException if a database access error occurs
     * @since 1.4
     */
    boolean supportsGetGeneratedKeys() throws SQLException;

    /**
     * Retrieves a description of the user-defined type (UDT) hierarchies defined in a
     * particular schema in this database. Only the immediate super type/
     * sub type relationship is modeled.
     * <P>
     * Only supertype information for UDTs matching the catalog,
     * schema, and type name is returned. The type name parameter
     * may be a fully-qualified name. When the UDT name supplied is a
     * fully-qualified name, the catalog and schemaPattern parameters are
     * ignored.
     * <P>
     * If a UDT does not have a direct super type, it is not listed here.
     * A row of the {@code ResultSet} object returned by this method
     * describes the designated UDT and a direct supertype. A row has the following
     * columns:
     *  <OL>
     *  <LI><B>TYPE_CAT</B> String {@code =>} the UDT's catalog (may be {@code null})
     *  <LI><B>TYPE_SCHEM</B> String {@code =>} UDT's schema (may be {@code null})
     *  <LI><B>TYPE_NAME</B> String {@code =>} type name of the UDT
     *  <LI><B>SUPERTYPE_CAT</B> String {@code =>} the direct super type's catalog
     *                           (may be {@code null})
     *  <LI><B>SUPERTYPE_SCHEM</B> String {@code =>} the direct super type's schema
     *                             (may be {@code null})
     *  <LI><B>SUPERTYPE_NAME</B> String {@code =>} the direct super type's name
     *  </OL>
     *
     * <P><B>Note:</B> If the driver does not support type hierarchies, an
     * empty result set is returned.
     *
     * @param catalog a catalog name; "" retrieves those without a catalog;
     *        {@code null} means drop catalog name from the selection criteria
     * @param schemaPattern a schema name pattern; "" retrieves those
     *        without a schema
     * @param typeNamePattern a UDT name pattern; may be a fully-qualified
     *        name
     * @return a {@code ResultSet} object in which a row gives information
     *         about the designated UDT
     * @throws SQLException if a database access error occurs
     * @see #getSearchStringEscape
     * @since 1.4
     */
    ResultSet getSuperTypes(String catalog, String schemaPattern,
                            String typeNamePattern) throws SQLException;

    /**
     * Retrieves a description of the table hierarchies defined in a particular
     * schema in this database.
     *
     * <P>Only supertable information for tables matching the catalog, schema
     * and table name are returned. The table name parameter may be a fully-
     * qualified name, in which case, the catalog and schemaPattern parameters
     * are ignored. If a table does not have a super table, it is not listed here.
     * Supertables have to be defined in the same catalog and schema as the
     * sub tables. Therefore, the type description does not need to include
     * this information for the supertable.
     *
     * <P>Each type description has the following columns:
     *  <OL>
     *  <LI><B>TABLE_CAT</B> String {@code =>} the type's catalog (may be {@code null})
     *  <LI><B>TABLE_SCHEM</B> String {@code =>} type's schema (may be {@code null})
     *  <LI><B>TABLE_NAME</B> String {@code =>} type name
     *  <LI><B>SUPERTABLE_NAME</B> String {@code =>} the direct super type's name
     *  </OL>
     *
     * <P><B>Note:</B> If the driver does not support type hierarchies, an
     * empty result set is returned.
     *
     * @param catalog a catalog name; "" retrieves those without a catalog;
     *        {@code null} means drop catalog name from the selection criteria
     * @param schemaPattern a schema name pattern; "" retrieves those
     *        without a schema
     * @param tableNamePattern a table name pattern; may be a fully-qualified
     *        name
     * @return a {@code ResultSet} object in which each row is a type description
     * @throws SQLException if a database access error occurs
     * @see #getSearchStringEscape
     * @since 1.4
     */
    ResultSet getSuperTables(String catalog, String schemaPattern,
                             String tableNamePattern) throws SQLException;

    /**
     * Indicates that {@code NULL} values might not be allowed.
     * <P>
     * A possible value for the column
     * {@code NULLABLE} in the {@code ResultSet} object
     * returned by the method {@code getAttributes}.
     */
    short attributeNoNulls = 0;

    /**
     * Indicates that {@code NULL} values are definitely allowed.
     * <P>
     * A possible value for the column {@code NULLABLE}
     * in the {@code ResultSet} object
     * returned by the method {@code getAttributes}.
     */
    short attributeNullable = 1;

    /**
     * Indicates that whether {@code NULL} values are allowed is not
     * known.
     * <P>
     * A possible value for the column {@code NULLABLE}
     * in the {@code ResultSet} object
     * returned by the method {@code getAttributes}.
     */
    short attributeNullableUnknown = 2;

    /**
     * Retrieves a description of the given attribute of the given type
     * for a user-defined type (UDT) that is available in the given schema
     * and catalog.
     * <P>
     * Descriptions are returned only for attributes of UDTs matching the
     * catalog, schema, type, and attribute name criteria. They are ordered by
     * {@code TYPE_CAT}, {@code TYPE_SCHEM},
     * {@code TYPE_NAME} and {@code ORDINAL_POSITION}. This description
     * does not contain inherited attributes.
     * <P>
     * The {@code ResultSet} object that is returned has the following
     * columns:
     * <OL>
     *  <LI><B>TYPE_CAT</B> String {@code =>} type catalog (may be {@code null})
     *  <LI><B>TYPE_SCHEM</B> String {@code =>} type schema (may be {@code null})
     *  <LI><B>TYPE_NAME</B> String {@code =>} type name
     *  <LI><B>ATTR_NAME</B> String {@code =>} attribute name
     *  <LI><B>DATA_TYPE</B> int {@code =>} attribute type SQL type from java.sql.Types
     *  <LI><B>ATTR_TYPE_NAME</B> String {@code =>} Data source dependent type name.
     *  For a UDT, the type name is fully qualified. For a REF, the type name is
     *  fully qualified and represents the target type of the reference type.
     *  <LI><B>ATTR_SIZE</B> int {@code =>} column size.  For char or date
     *      types this is the maximum number of characters; for numeric or
     *      decimal types this is precision.
     *  <LI><B>DECIMAL_DIGITS</B> int {@code =>} the number of fractional digits. Null is returned for data types where
     * DECIMAL_DIGITS is not applicable.
     *  <LI><B>NUM_PREC_RADIX</B> int {@code =>} Radix (typically either 10 or 2)
     *  <LI><B>NULLABLE</B> int {@code =>} whether NULL is allowed
     *      <UL>
     *      <LI> attributeNoNulls - might not allow NULL values
     *      <LI> attributeNullable - definitely allows NULL values
     *      <LI> attributeNullableUnknown - nullability unknown
     *      </UL>
     *  <LI><B>REMARKS</B> String {@code =>} comment describing column (may be {@code null})
     *  <LI><B>ATTR_DEF</B> String {@code =>} default value (may be {@code null})
     *  <LI><B>SQL_DATA_TYPE</B> int {@code =>} unused
     *  <LI><B>SQL_DATETIME_SUB</B> int {@code =>} unused
     *  <LI><B>CHAR_OCTET_LENGTH</B> int {@code =>} for char types the
     *       maximum number of bytes in the column
     *  <LI><B>ORDINAL_POSITION</B> int {@code =>} index of the attribute in the UDT
     *      (starting at 1)
     *  <LI><B>IS_NULLABLE</B> String  {@code =>} ISO rules are used to determine
     * the nullability for a attribute.
     *       <UL>
     *       <LI> YES           --- if the attribute can include NULLs
     *       <LI> NO            --- if the attribute cannot include NULLs
     *       <LI> empty string  --- if the nullability for the
     * attribute is unknown
     *       </UL>
     *  <LI><B>SCOPE_CATALOG</B> String {@code =>} catalog of table that is the
     *      scope of a reference attribute ({@code null} if DATA_TYPE isn't REF)
     *  <LI><B>SCOPE_SCHEMA</B> String {@code =>} schema of table that is the
     *      scope of a reference attribute ({@code null} if DATA_TYPE isn't REF)
     *  <LI><B>SCOPE_TABLE</B> String {@code =>} table name that is the scope of a
     *      reference attribute ({@code null} if the DATA_TYPE isn't REF)
     * <LI><B>SOURCE_DATA_TYPE</B> short {@code =>} source type of a distinct type or user-generated
     *      Ref type,SQL type from java.sql.Types ({@code null} if DATA_TYPE
     *      isn't DISTINCT or user-generated REF)
     *  </OL>
     * @param catalog a catalog name; must match the catalog name as it
     *        is stored in the database; "" retrieves those without a catalog;
     *        {@code null} means that the catalog name should not be used to narrow
     *        the search
     * @param schemaPattern a schema name pattern; must match the schema name
     *        as it is stored in the database; "" retrieves those without a schema;
     *        {@code null} means that the schema name should not be used to narrow
     *        the search
     * @param typeNamePattern a type name pattern; must match the
     *        type name as it is stored in the database
     * @param attributeNamePattern an attribute name pattern; must match the attribute
     *        name as it is declared in the database
     * @return a {@code ResultSet} object in which each row is an
     *         attribute description
     * @throws SQLException if a database access error occurs
     * @see #getSearchStringEscape
     * @since 1.4
     */
    ResultSet getAttributes(String catalog, String schemaPattern,
                            String typeNamePattern, String attributeNamePattern)
        throws SQLException;

    /**
     * Retrieves whether this database supports the given result set holdability.
     *
     * @param holdability one of the following constants:
     *          {@code ResultSet.HOLD_CURSORS_OVER_COMMIT} or
     *          {@code ResultSet.CLOSE_CURSORS_AT_COMMIT}
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @see Connection
     * @since 1.4
     */
    boolean supportsResultSetHoldability(int holdability) throws SQLException;

    /**
     * Retrieves this database's default holdability for {@code ResultSet}
     * objects.
     *
     * @return the default holdability; either
     *         {@code ResultSet.HOLD_CURSORS_OVER_COMMIT} or
     *         {@code ResultSet.CLOSE_CURSORS_AT_COMMIT}
     * @throws SQLException if a database access error occurs
     * @since 1.4
     */
    int getResultSetHoldability() throws SQLException;

    /**
     * Retrieves the major version number of the underlying database.
     *
     * @return the underlying database's major version
     * @throws SQLException if a database access error occurs
     * @since 1.4
     */
    int getDatabaseMajorVersion() throws SQLException;

    /**
     * Retrieves the minor version number of the underlying database.
     *
     * @return underlying database's minor version
     * @throws SQLException if a database access error occurs
     * @since 1.4
     */
    int getDatabaseMinorVersion() throws SQLException;

    /**
     * Retrieves the major JDBC version number for this
     * driver.
     *
     * @return JDBC version major number
     * @throws SQLException if a database access error occurs
     * @since 1.4
     */
    int getJDBCMajorVersion() throws SQLException;

    /**
     * Retrieves the minor JDBC version number for this
     * driver.
     *
     * @return JDBC version minor number
     * @throws SQLException if a database access error occurs
     * @since 1.4
     */
    int getJDBCMinorVersion() throws SQLException;

    /**
     *  A possible return value for the method
     * {@code DatabaseMetaData.getSQLStateType} which is used to indicate
     * whether the value returned by the method
     * {@code SQLException.getSQLState} is an
     * X/Open (now know as Open Group) SQL CLI SQLSTATE value.
     *
     * @since 1.4
     */
    int sqlStateXOpen = 1;

    /**
     *  A possible return value for the method
     * {@code DatabaseMetaData.getSQLStateType} which is used to indicate
     * whether the value returned by the method
     * {@code SQLException.getSQLState} is an SQLSTATE value.
     *
     * @since 1.6
     */
    int sqlStateSQL = 2;

     /**
     *  A possible return value for the method
     * {@code DatabaseMetaData.getSQLStateType} which is used to indicate
     * whether the value returned by the method
     * {@code SQLException.getSQLState} is an SQL99 SQLSTATE value.
     * <P>
     * <b>Note:</b>This constant remains only for compatibility reasons. Developers
     * should use the constant {@code sqlStateSQL} instead.
     *
     * @since 1.4
     */
    int sqlStateSQL99 = sqlStateSQL;

    /**
     * Indicates whether the SQLSTATE returned by {@code SQLException.getSQLState}
     * is X/Open (now known as Open Group) SQL CLI or SQL:2003.
     * @return the type of SQLSTATE; one of:
     *        sqlStateXOpen or
     *        sqlStateSQL
     * @throws SQLException if a database access error occurs
     * @since 1.4
     */
    int getSQLStateType() throws SQLException;

    /**
     * Indicates whether updates made to a LOB are made on a copy or directly
     * to the LOB.
     * @return {@code true} if updates are made to a copy of the LOB;
     *         {@code false} if updates are made directly to the LOB
     * @throws SQLException if a database access error occurs
     * @since 1.4
     */
    boolean locatorsUpdateCopy() throws SQLException;

    /**
     * Retrieves whether this database supports statement pooling.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.4
     */
    boolean supportsStatementPooling() throws SQLException;

    //------------------------- JDBC 4.0 -----------------------------------

    /**
     * Indicates whether this data source supports the SQL {@code  ROWID} type,
     * and the lifetime for which a {@link  RowId} object remains valid.
     *
     * @return the status indicating the lifetime of a {@code  RowId}
     * @throws SQLException if a database access error occurs
     * @since 1.6
     */
    RowIdLifetime getRowIdLifetime() throws SQLException;

    /**
     * Retrieves the schema names available in this database.  The results
     * are ordered by {@code TABLE_CATALOG} and
     * {@code TABLE_SCHEM}.
     *
     * <P>The schema columns are:
     *  <OL>
     *  <LI><B>TABLE_SCHEM</B> String {@code =>} schema name
     *  <LI><B>TABLE_CATALOG</B> String {@code =>} catalog name (may be {@code null})
     *  </OL>
     *
     *
     * @param catalog a catalog name; must match the catalog name as it is stored
     * in the database;"" retrieves those without a catalog; null means catalog
     * name should not be used to narrow down the search.
     * @param schemaPattern a schema name; must match the schema name as it is
     * stored in the database; null means
     * schema name should not be used to narrow down the search.
     * @return a {@code ResultSet} object in which each row is a
     *         schema description
     * @throws SQLException if a database access error occurs
     * @see #getSearchStringEscape
     * @since 1.6
     */
    ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException;

    /**
     * Retrieves whether this database supports invoking user-defined or vendor functions
     * using the stored procedure escape syntax.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.6
     */
    boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException;

    /**
     * Retrieves whether a {@code SQLException} while autoCommit is {@code true} indicates
     * that all open ResultSets are closed, even ones that are holdable.  When a {@code SQLException} occurs while
     * autocommit is {@code true}, it is vendor specific whether the JDBC driver responds with a commit operation, a
     * rollback operation, or by doing neither a commit nor a rollback.  A potential result of this difference
     * is in whether or not holdable ResultSets are closed.
     *
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.6
     */
    boolean autoCommitFailureClosesAllResultSets() throws SQLException;
        /**
         * Retrieves a list of the client info properties
         * that the driver supports.  The result set contains the following columns
         *
         * <ol>
         * <li><b>NAME</b> String{@code =>} The name of the client info property<br>
         * <li><b>MAX_LEN</b> int{@code =>} The maximum length of the value for the property<br>
         * <li><b>DEFAULT_VALUE</b> String{@code =>} The default value of the property<br>
         * <li><b>DESCRIPTION</b> String{@code =>} A description of the property.  This will typically
         *                                              contain information as to where this property is
         *                                              stored in the database.
         * </ol>
         * <p>
         * The {@code ResultSet} is sorted by the NAME column
         *
         * @return      A {@code ResultSet} object; each row is a supported client info
         * property
         *
         * @throws SQLException if a database access error occurs
         *
         * @since 1.6
         */
        ResultSet getClientInfoProperties()
                throws SQLException;

    /**
     * Retrieves a description of the  system and user functions available
     * in the given catalog.
     * <P>
     * Only system and user function descriptions matching the schema and
     * function name criteria are returned.  They are ordered by
     * {@code FUNCTION_CAT}, {@code FUNCTION_SCHEM},
     * {@code FUNCTION_NAME} and
     * {@code SPECIFIC_ NAME}.
     *
     * <P>Each function description has the following columns:
     *  <OL>
     *  <LI><B>FUNCTION_CAT</B> String {@code =>} function catalog (may be {@code null})
     *  <LI><B>FUNCTION_SCHEM</B> String {@code =>} function schema (may be {@code null})
     *  <LI><B>FUNCTION_NAME</B> String {@code =>} function name.  This is the name
     * used to invoke the function
     *  <LI><B>REMARKS</B> String {@code =>} explanatory comment on the function
     * <LI><B>FUNCTION_TYPE</B> short {@code =>} kind of function:
     *      <UL>
     *      <LI>functionResultUnknown - Cannot determine if a return value
     *       or table will be returned
     *      <LI> functionNoTable- Does not return a table
     *      <LI> functionReturnsTable - Returns a table
     *      </UL>
     *  <LI><B>SPECIFIC_NAME</B> String  {@code =>} the name which uniquely identifies
     *  this function within its schema.  This is a user specified, or DBMS
     * generated, name that may be different then the {@code FUNCTION_NAME}
     * for example with overload functions
     *  </OL>
     * <p>
     * A user may not have permission to execute any of the functions that are
     * returned by {@code getFunctions}
     *
     * @param catalog a catalog name; must match the catalog name as it
     *        is stored in the database; "" retrieves those without a catalog;
     *        {@code null} means that the catalog name should not be used to narrow
     *        the search
     * @param schemaPattern a schema name pattern; must match the schema name
     *        as it is stored in the database; "" retrieves those without a schema;
     *        {@code null} means that the schema name should not be used to narrow
     *        the search
     * @param functionNamePattern a function name pattern; must match the
     *        function name as it is stored in the database
     * @return {@code ResultSet} - each row is a function description
     * @throws SQLException if a database access error occurs
     * @see #getSearchStringEscape
     * @since 1.6
     */
    ResultSet getFunctions(String catalog, String schemaPattern,
                            String functionNamePattern) throws SQLException;
    /**
     * Retrieves a description of the given catalog's system or user
     * function parameters and return type.
     *
     * <P>Only descriptions matching the schema,  function and
     * parameter name criteria are returned. They are ordered by
     * {@code FUNCTION_CAT}, {@code FUNCTION_SCHEM},
     * {@code FUNCTION_NAME} and
     * {@code SPECIFIC_ NAME}. Within this, the return value,
     * if any, is first. Next are the parameter descriptions in call
     * order. The column descriptions follow in column number order.
     *
     * <P>Each row in the {@code ResultSet}
     * is a parameter description, column description or
     * return type description with the following fields:
     *  <OL>
     *  <LI><B>FUNCTION_CAT</B> String {@code =>} function catalog (may be {@code null})
     *  <LI><B>FUNCTION_SCHEM</B> String {@code =>} function schema (may be {@code null})
     *  <LI><B>FUNCTION_NAME</B> String {@code =>} function name.  This is the name
     * used to invoke the function
     *  <LI><B>COLUMN_NAME</B> String {@code =>} column/parameter name
     *  <LI><B>COLUMN_TYPE</B> Short {@code =>} kind of column/parameter:
     *      <UL>
     *      <LI> functionColumnUnknown - nobody knows
     *      <LI> functionColumnIn - IN parameter
     *      <LI> functionColumnInOut - INOUT parameter
     *      <LI> functionColumnOut - OUT parameter
     *      <LI> functionColumnReturn - function return value
     *      <LI> functionColumnResult - Indicates that the parameter or column
     *  is a column in the {@code ResultSet}
     *      </UL>
     *  <LI><B>DATA_TYPE</B> int {@code =>} SQL type from java.sql.Types
     *  <LI><B>TYPE_NAME</B> String {@code =>} SQL type name, for a UDT type the
     *  type name is fully qualified
     *  <LI><B>PRECISION</B> int {@code =>} precision
     *  <LI><B>LENGTH</B> int {@code =>} length in bytes of data
     *  <LI><B>SCALE</B> short {@code =>} scale -  null is returned for data types where
     * SCALE is not applicable.
     *  <LI><B>RADIX</B> short {@code =>} radix
     *  <LI><B>NULLABLE</B> short {@code =>} can it contain NULL.
     *      <UL>
     *      <LI> functionNoNulls - does not allow NULL values
     *      <LI> functionNullable - allows NULL values
     *      <LI> functionNullableUnknown - nullability unknown
     *      </UL>
     *  <LI><B>REMARKS</B> String {@code =>} comment describing column/parameter
     *  <LI><B>CHAR_OCTET_LENGTH</B> int  {@code =>} the maximum length of binary
     * and character based parameters or columns.  For any other datatype the returned value
     * is a NULL
     *  <LI><B>ORDINAL_POSITION</B> int  {@code =>} the ordinal position, starting
     * from 1, for the input and output parameters. A value of 0
     * is returned if this row describes the function's return value.
     * For result set columns, it is the
     * ordinal position of the column in the result set starting from 1.
     *  <LI><B>IS_NULLABLE</B> String  {@code =>} ISO rules are used to determine
     * the nullability for a parameter or column.
     *       <UL>
     *       <LI> YES           --- if the parameter or column can include NULLs
     *       <LI> NO            --- if the parameter or column  cannot include NULLs
     *       <LI> empty string  --- if the nullability for the
     * parameter  or column is unknown
     *       </UL>
     *  <LI><B>SPECIFIC_NAME</B> String  {@code =>} the name which uniquely identifies
     * this function within its schema.  This is a user specified, or DBMS
     * generated, name that may be different then the {@code FUNCTION_NAME}
     * for example with overload functions
     *  </OL>
     *
     * <p>The PRECISION column represents the specified column size for the given
     * parameter or column.
     * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
     * For datetime datatypes, this is the length in characters of the String representation (assuming the
     * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
     * this is the length in bytes. Null is returned for data types where the
     * column size is not applicable.
     * @param catalog a catalog name; must match the catalog name as it
     *        is stored in the database; "" retrieves those without a catalog;
     *        {@code null} means that the catalog name should not be used to narrow
     *        the search
     * @param schemaPattern a schema name pattern; must match the schema name
     *        as it is stored in the database; "" retrieves those without a schema;
     *        {@code null} means that the schema name should not be used to narrow
     *        the search
     * @param functionNamePattern a procedure name pattern; must match the
     *        function name as it is stored in the database
     * @param columnNamePattern a parameter name pattern; must match the
     * parameter or column name as it is stored in the database
     * @return {@code ResultSet} - each row describes a
     * user function parameter, column  or return type
     *
     * @throws SQLException if a database access error occurs
     * @see #getSearchStringEscape
     * @since 1.6
     */
    ResultSet getFunctionColumns(String catalog,
                                  String schemaPattern,
                                  String functionNamePattern,
                                  String columnNamePattern) throws SQLException;


    /**
     * Indicates that type of the parameter or column is unknown.
     * <P>
     * A possible value for the column
     * {@code COLUMN_TYPE}
     * in the {@code ResultSet}
     * returned by the method {@code getFunctionColumns}.
     */
    int functionColumnUnknown = 0;

    /**
     * Indicates that the parameter or column is an IN parameter.
     * <P>
     *  A possible value for the column
     * {@code COLUMN_TYPE}
     * in the {@code ResultSet}
     * returned by the method {@code getFunctionColumns}.
     * @since 1.6
     */
    int functionColumnIn = 1;

    /**
     * Indicates that the parameter or column is an INOUT parameter.
     * <P>
     * A possible value for the column
     * {@code COLUMN_TYPE}
     * in the {@code ResultSet}
     * returned by the method {@code getFunctionColumns}.
     * @since 1.6
     */
    int functionColumnInOut = 2;

    /**
     * Indicates that the parameter or column is an OUT parameter.
     * <P>
     * A possible value for the column
     * {@code COLUMN_TYPE}
     * in the {@code ResultSet}
     * returned by the method {@code getFunctionColumns}.
     * @since 1.6
     */
    int functionColumnOut = 3;
    /**
     * Indicates that the parameter or column is a return value.
     * <P>
     *  A possible value for the column
     * {@code COLUMN_TYPE}
     * in the {@code ResultSet}
     * returned by the method {@code getFunctionColumns}.
     * @since 1.6
     */
    int functionReturn = 4;

       /**
     * Indicates that the parameter or column is a column in a result set.
     * <P>
     *  A possible value for the column
     * {@code COLUMN_TYPE}
     * in the {@code ResultSet}
     * returned by the method {@code getFunctionColumns}.
     * @since 1.6
     */
    int functionColumnResult = 5;


    /**
     * Indicates that {@code NULL} values are not allowed.
     * <P>
     * A possible value for the column
     * {@code NULLABLE}
     * in the {@code ResultSet} object
     * returned by the method {@code getFunctionColumns}.
     * @since 1.6
     */
    int functionNoNulls = 0;

    /**
     * Indicates that {@code NULL} values are allowed.
     * <P>
     * A possible value for the column
     * {@code NULLABLE}
     * in the {@code ResultSet} object
     * returned by the method {@code getFunctionColumns}.
     * @since 1.6
     */
    int functionNullable = 1;

    /**
     * Indicates that whether {@code NULL} values are allowed
     * is unknown.
     * <P>
     * A possible value for the column
     * {@code NULLABLE}
     * in the {@code ResultSet} object
     * returned by the method {@code getFunctionColumns}.
     * @since 1.6
     */
    int functionNullableUnknown = 2;

    /**
     * Indicates that it is not known whether the function returns
     * a result or a table.
     * <P>
     * A possible value for column {@code FUNCTION_TYPE} in the
     * {@code ResultSet} object returned by the method
     * {@code getFunctions}.
     * @since 1.6
     */
    int functionResultUnknown   = 0;

    /**
     * Indicates that the function  does not return a table.
     * <P>
     * A possible value for column {@code FUNCTION_TYPE} in the
     * {@code ResultSet} object returned by the method
     * {@code getFunctions}.
     * @since 1.6
     */
    int functionNoTable         = 1;

    /**
     * Indicates that the function  returns a table.
     * <P>
     * A possible value for column {@code FUNCTION_TYPE} in the
     * {@code ResultSet} object returned by the method
     * {@code getFunctions}.
     * @since 1.6
     */
    int functionReturnsTable    = 2;

    //--------------------------JDBC 4.1 -----------------------------

    /**
     * Retrieves a description of the pseudo or hidden columns available
     * in a given table within the specified catalog and schema.
     * Pseudo or hidden columns may not always be stored within
     * a table and are not visible in a ResultSet unless they are
     * specified in the query's outermost SELECT list. Pseudo or hidden
     * columns may not necessarily be able to be modified. If there are
     * no pseudo or hidden columns, an empty ResultSet is returned.
     *
     * <P>Only column descriptions matching the catalog, schema, table
     * and column name criteria are returned.  They are ordered by
     * {@code TABLE_CAT},{@code TABLE_SCHEM}, {@code TABLE_NAME}
     * and {@code COLUMN_NAME}.
     *
     * <P>Each column description has the following columns:
     *  <OL>
     *  <LI><B>TABLE_CAT</B> String {@code =>} table catalog (may be {@code null})
     *  <LI><B>TABLE_SCHEM</B> String {@code =>} table schema (may be {@code null})
     *  <LI><B>TABLE_NAME</B> String {@code =>} table name
     *  <LI><B>COLUMN_NAME</B> String {@code =>} column name
     *  <LI><B>DATA_TYPE</B> int {@code =>} SQL type from java.sql.Types
     *  <LI><B>COLUMN_SIZE</B> int {@code =>} column size.
     *  <LI><B>DECIMAL_DIGITS</B> int {@code =>} the number of fractional digits. Null is returned for data types where
     * DECIMAL_DIGITS is not applicable.
     *  <LI><B>NUM_PREC_RADIX</B> int {@code =>} Radix (typically either 10 or 2)
     *  <LI><B>COLUMN_USAGE</B> String {@code =>} The allowed usage for the column.  The
     *  value returned will correspond to the enum name returned by {@link PseudoColumnUsage#name PseudoColumnUsage.name()}
     *  <LI><B>REMARKS</B> String {@code =>} comment describing column (may be {@code null})
     *  <LI><B>CHAR_OCTET_LENGTH</B> int {@code =>} for char types the
     *       maximum number of bytes in the column
     *  <LI><B>IS_NULLABLE</B> String  {@code =>} ISO rules are used to determine the nullability for a column.
     *       <UL>
     *       <LI> YES           --- if the column can include NULLs
     *       <LI> NO            --- if the column cannot include NULLs
     *       <LI> empty string  --- if the nullability for the column is unknown
     *       </UL>
     *  </OL>
     *
     * <p>The COLUMN_SIZE column specifies the column size for the given column.
     * For numeric data, this is the maximum precision.  For character data, this is the length in characters.
     * For datetime datatypes, this is the length in characters of the String representation (assuming the
     * maximum allowed precision of the fractional seconds component). For binary data, this is the length in bytes.  For the ROWID datatype,
     * this is the length in bytes. Null is returned for data types where the
     * column size is not applicable.
     *
     * @param catalog a catalog name; must match the catalog name as it
     *        is stored in the database; "" retrieves those without a catalog;
     *        {@code null} means that the catalog name should not be used to narrow
     *        the search
     * @param schemaPattern a schema name pattern; must match the schema name
     *        as it is stored in the database; "" retrieves those without a schema;
     *        {@code null} means that the schema name should not be used to narrow
     *        the search
     * @param tableNamePattern a table name pattern; must match the
     *        table name as it is stored in the database
     * @param columnNamePattern a column name pattern; must match the column
     *        name as it is stored in the database
     * @return {@code ResultSet} - each row is a column description
     * @throws SQLException if a database access error occurs
     * @see PseudoColumnUsage
     * @since 1.7
     */
    ResultSet getPseudoColumns(String catalog, String schemaPattern,
                         String tableNamePattern, String columnNamePattern)
        throws SQLException;

    /**
     * Retrieves whether a generated key will always be returned if the column
     * name(s) or index(es) specified for the auto generated key column(s)
     * are valid and the statement succeeds.  The key that is returned may or
     * may not be based on the column(s) for the auto generated key.
     * Consult your JDBC driver documentation for additional details.
     * @return {@code true} if so; {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.7
     */
    boolean  generatedKeyAlwaysReturned() throws SQLException;

    //--------------------------JDBC 4.2 -----------------------------

    /**
     *
     * Retrieves the maximum number of bytes this database allows for
     * the logical size for a {@code LOB}.
     *<p>
     * The default implementation will return {@code 0}
     *
     * @return the maximum number of bytes allowed; a result of zero
     * means that there is no limit or the limit is not known
     * @throws SQLException if a database access error occurs
     * @since 1.8
     */
    default long getMaxLogicalLobSize() throws SQLException {
        return 0;
    }

    /**
     * Retrieves whether this database supports REF CURSOR.
     *<p>
     * The default implementation will return {@code false}
     *
     * @return {@code true} if this database supports REF CURSOR;
     *         {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 1.8
     */
    default boolean supportsRefCursors() throws SQLException{
        return false;
    }

    // JDBC 4.3

    /**
     * Retrieves whether this database supports sharding.
     * @implSpec
     * The default implementation will return {@code false}
     *
     * @return {@code true} if this database supports sharding;
     *         {@code false} otherwise
     * @throws SQLException if a database access error occurs
     * @since 9
     */
    default boolean supportsSharding() throws SQLException {
        return false;
    }
}
