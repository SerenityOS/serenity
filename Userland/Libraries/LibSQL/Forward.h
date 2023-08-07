/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace SQL {
class BTree;
class BTreeIterator;
class ColumnDef;
class Database;
class Heap;
class Index;
class IndexNode;
class IndexDef;
class Key;
class KeyPartDef;
class Relation;
class Result;
class ResultSet;
class Row;
class SchemaDef;
class Serializer;
class TableDef;
class TreeNode;
class Tuple;
class TupleDescriptor;
struct TupleElementDescriptor;
class Value;
}

namespace SQL::AST {
class AddColumn;
class AlterTable;
class ASTNode;
class BetweenExpression;
class BinaryOperatorExpression;
class BlobLiteral;
class CaseExpression;
class CastExpression;
class ChainedExpression;
class CollateExpression;
class ColumnDefinition;
class ColumnNameExpression;
class CommonTableExpression;
class CommonTableExpressionList;
class CreateTable;
class Delete;
class DropColumn;
class DropTable;
class ErrorExpression;
class ErrorStatement;
class ExistsExpression;
class Expression;
class GroupByClause;
class InChainedExpression;
class InSelectionExpression;
class Insert;
class InTableExpression;
class InvertibleNestedDoubleExpression;
class InvertibleNestedExpression;
class IsExpression;
class Lexer;
class LimitClause;
class MatchExpression;
class NestedDoubleExpression;
class NestedExpression;
class NullExpression;
class NullLiteral;
class NumericLiteral;
class OrderingTerm;
class Parser;
class QualifiedTableName;
class RenameColumn;
class RenameTable;
class ResultColumn;
class ReturningClause;
class Select;
class SignedNumber;
class Statement;
class StringLiteral;
class TableOrSubquery;
class Token;
class TypeName;
class UnaryOperatorExpression;
class Update;
}
