package com.meetup.dynamicorm.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.Type;

import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * column types map to Hibernate field mapping types
 */
@Getter
public enum ColumnType {
	INTEGER(Constants._INTEGER, Types.INTEGER, 10),
	LONG(Constants._LONG, Types.BIGINT, 19),
	SHORT(Constants._SHORT, Types.SMALLINT, 5),
	FLOAT(Constants._FLOAT, Types.FLOAT, true, true, 53),
	DOUBLE(Constants._DOUBLE, Types.DOUBLE, true, true, 53),
	CHARACTER(Constants._CHARACTER, Types.CHAR, 1),
	BYTE(Constants._BYTE, Types.BIT, 1),
	BOOLEAN(Constants._BOOLEAN, Types.BOOLEAN, 1),
	YES_NO(Constants._YES_NO, Types.BOOLEAN, 1),
	TRUE_FALSE(Constants._TRUE_FALSE, Types.BOOLEAN, 1),
	STRING(Constants._STRING, Types.VARCHAR, true),
	// date time related mapping types
	DATE(Constants._DATE, Types.DATE),
	TIME(Constants._TIME, Types.TIME),
	TIMESTAMP(Constants._TIMESTAMP, Types.TIMESTAMP),
	// special mapping types
	BIG_DECIMAL(Constants._BIG_DECIMAL, Types.DECIMAL, true, true,38),
	BIG_INTEGER(Constants._BIG_INTEGER, Types.BIGINT, 19),
	TEXT(Constants._TEXT, Types.NVARCHAR, true),
	CLOB(Constants._CLOB, Types.CLOB, -1),
	BLOB(Constants._BLOB, Types.BLOB, -1);

	public static final Map<ColumnType, Function<String, ? extends Number>> columnTypeNumberTypeCastingMethodMap = new HashMap<>();

	static {
		columnTypeNumberTypeCastingMethodMap.put(INTEGER, Integer::parseInt);
		columnTypeNumberTypeCastingMethodMap.put(LONG, Long::parseLong);
		columnTypeNumberTypeCastingMethodMap.put(FLOAT, Float::parseFloat);
		columnTypeNumberTypeCastingMethodMap.put(DOUBLE, Double::parseDouble);
		columnTypeNumberTypeCastingMethodMap.put(SHORT, Short::parseShort);
		columnTypeNumberTypeCastingMethodMap.put(BYTE, Byte::parseByte);
	}

	public static final String DOUBLE_REGEX = "\\d*[.]\\d+";
	public static final String TIME_REGEX = "\\d{1,2}[:]\\d{1,2}[:]\\d{1,2}";
	public static final String TIMESTAMP_REGEX ="\\d{4}[/]\\d{1,2}[/]\\d{1,2}[ ]\\d{1,2}[:]\\d{1,2}[:]\\d{1,2}";
	public static final String DATE_REGEX = "\\d{1,2}[/]\\d{1,2}[/]\\d{4}";
	public static final String INTEGER_REGEX = "\\d+";
	public static final String BOOLEAN_REGEX = "true|false";
	public static final String YES_NO_REGEX = "YES|NO";

	private final String value;
	private final boolean isNumeric;
	private final boolean isText;
	private final boolean isDate;
	private final boolean isBoolean;
	private final boolean supportsLength;
	private final boolean supportsPrecision;
	private final String dataType;
	private final int defaultValue;
	private final int types;

	ColumnType(String value, int types) {
		this(value, types, false);
	}

	ColumnType(String value, int types, int defaultValue) {
		this(value, types, false, false, defaultValue);
	}

	ColumnType(String value, int types, Boolean supportsLength) {
		this(value, types, supportsLength, false, 0);
	}

	ColumnType(String value, int types, Boolean supportsLength, Boolean supportsPrecision, int defaultValue) {
		this.value = value;
		this.supportsLength = supportsLength;
		this.supportsPrecision = supportsPrecision;
		this.defaultValue = defaultValue;
		isNumeric = Constants.numericTypes.contains(value);
		isText = Constants.textTypes.contains(value);
		isDate = Constants.dateTypes.contains(value);
		isBoolean = Constants.booleanTypes.contains(value);
		this.types = types;
		if (isNumeric) {
			dataType = "numeric";
		} else if (isText) {
			dataType = "text";
		} else if (isDate) {
			dataType = "date";
		} else if (isBoolean) {
			dataType = "boolean";
		} else {
			dataType = "other";
		}
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	public Boolean isSupportsLength() {
		return supportsLength;
	}

	public Boolean isSupportsPrecision() {
		return supportsPrecision;
	}

	public String getDataType() {
		return dataType;
	}

	public Boolean getBoolean() {
		return isBoolean;
	}

	public Boolean getText() {
		return isText;
	}

	public Boolean getNumeric() {
		return isNumeric;
	}

	public Boolean getDate() {
		return isDate;
	}


	public Boolean isBoolean() {
		return isBoolean;
	}

	public Boolean isText() {
		return isText;
	}

	public Boolean isNumeric() {
		return isNumeric;
	}

	public Boolean isDate() {
		return isDate;
	}

	public Type getType() {
		return new BasicTypeRegistry().getRegisteredType(value);
	}

	public Class<?> toClass() {
		Class<?> result = Object.class;

		switch (this.types) {
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
				result = String.class;
				break;

			case Types.NUMERIC:
			case Types.DECIMAL:
				result = java.math.BigDecimal.class;
				break;

			case Types.BIT:
				result = Boolean.class;
				break;

			case Types.TINYINT:
				result = Byte.class;
				break;

			case Types.SMALLINT:
				result = Short.class;
				break;

			case Types.INTEGER:
				result = Integer.class;
				break;

			case Types.BIGINT:
				result = Long.class;
				break;

			case Types.REAL:
			case Types.FLOAT:
				result = Float.class;
				break;

			case Types.DOUBLE:
				result = Double.class;
				break;

			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
				result = Byte[].class;
				break;

			case Types.DATE:
				result = java.sql.Date.class;
				break;

			case Types.TIME:
				result = java.sql.Time.class;
				break;

			case Types.TIMESTAMP:
				result = java.sql.Timestamp.class;
				break;
		}

		return result;
	}

	@Override
	public String toString() {
		return value;
	}

	private static class Constants {
		public static final String _INTEGER = "int";
		public static final String _LONG = "bigint";
		public static final String _SHORT = "smallint";
		public static final String _FLOAT = "float";
		public static final String _DOUBLE = "double";
		public static final String _CHARACTER = "char";
		public static final String _BYTE = "byte";
		public static final String _BOOLEAN = "boolean";
		public static final String _YES_NO = "boolean";
		public static final String _TRUE_FALSE = "boolean";
		public static final String _STRING = "varchar";
		public static final String _DATE = "date";
		public static final String _TIME = "time";
		public static final String _TIMESTAMP = "datetime";
		public static final String _BIG_DECIMAL = "decimal";
		public static final String _BIG_INTEGER = "bigint";
		//		public static final String _LOCALE = "locale";
//		public static final String _TIMEZONE = "timezone";
//		public static final String _CURRENCY = "currency";
//		public static final String _CLASS = "class";
//		public static final String _BINARY = "binary";
		public static final String _TEXT = "nvarchar";
		public static final String _CLOB = "clob";
		public static final String _BLOB = "blob";
		public static final String _INTERVAL = "interval";

		public static final List<String> numericTypes = Arrays.asList(
				_BYTE,
				_SHORT,
				_INTEGER,
				_LONG,
				_FLOAT,
				_DOUBLE,
				_BIG_INTEGER,
				_BIG_DECIMAL
		);

		public static final List<String> textTypes = Arrays.asList(
				_CHARACTER,
				_STRING,
				_CLOB,
				_TEXT
		);

		public static final List<String> dateTypes = Arrays.asList(
				_DATE,
				_TIME,
				_TIMESTAMP,
				_INTERVAL
		);

		public static final List<String> booleanTypes = Arrays.asList(
				_BOOLEAN,
				_YES_NO,
				_TRUE_FALSE
		);

	}
}
