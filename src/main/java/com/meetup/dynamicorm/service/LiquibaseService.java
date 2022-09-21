package com.meetup.dynamicorm.service;

import com.meetup.dynamicorm.model.DbColumnDto;
import com.meetup.dynamicorm.model.DbTableDto;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.ObjectQuotingStrategy;
import liquibase.database.jvm.JdbcConnection;
import liquibase.datatype.DataTypeFactory;
import liquibase.datatype.LiquibaseDataType;
import liquibase.diff.DiffGeneratorFactory;
import liquibase.diff.DiffResult;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.StandardObjectChangeFilter;
import liquibase.diff.output.changelog.DiffToChangeLog;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import liquibase.serializer.core.xml.XMLChangeLogSerializer;
import liquibase.snapshot.DatabaseSnapshot;
import liquibase.snapshot.EmptyDatabaseSnapshot;
import liquibase.snapshot.InvalidExampleException;
import liquibase.snapshot.SnapshotControl;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.structure.DatabaseObject;
import liquibase.structure.DatabaseObjectCollection;
import liquibase.structure.core.Catalog;
import liquibase.structure.core.Column;
import liquibase.structure.core.DataType;
import liquibase.structure.core.ForeignKey;
import liquibase.structure.core.Index;
import liquibase.structure.core.PrimaryKey;
import liquibase.structure.core.Schema;
import liquibase.structure.core.Sequence;
import liquibase.structure.core.Table;
import liquibase.structure.core.UniqueConstraint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiquibaseService {
	private final DataSource dataSource;

	@Transactional
	public String updateRemoteDatabaseFromLocalChanges(List<DbTableDto> dbTableDtoList) throws LiquibaseException, IOException {
		Connection connection = DataSourceUtils.getConnection(dataSource);
		DatabaseSnapshot remoteDatabaseSnapshot = generateRemoteDatabaseSnapshot(connection);
		DatabaseSnapshot localDatabaseSnapshot = createLocalDatabaseSnapshot(connection, dbTableDtoList);
		File xmlFile = createDiff(localDatabaseSnapshot, remoteDatabaseSnapshot);

		updateRemoteDatabase(remoteDatabaseSnapshot.getDatabase(), xmlFile);
		DataSourceUtils.releaseConnection(connection, dataSource);
		return Files.readString(xmlFile.toPath(), StandardCharsets.UTF_8);
	}

	DatabaseObject[] generateDatabaseObjectArray(Database remoteDatabase, List<DbTableDto> dbTableDtoList) {
		List<DatabaseObject> databaseObjectList = new ArrayList<>();

		String catalogName = remoteDatabase.getDefaultCatalogName();
		String schemaName = remoteDatabase.getDefaultSchemaName();
		Catalog catalog = new Catalog(catalogName);
		Schema schema = new Schema(catalog, schemaName);

		for (DbTableDto dbTable : dbTableDtoList) {
			String tableName = dbTable.getName();

			// create table
			Table table = new Table();
			table.setSnapshotId("localSnapshot");
			table.setName(tableName);
			table.setSchema(schema);

			for (DbColumnDto dbColumn : dbTable.getColumnSet()) {
				String columnName = dbColumn.getName();
				// create column
				Column column = new Column(columnName);
				column.setRelation(table);

				DataType temporaryDataType = new DataType(dbColumn.getColumnType().getValue());
				LiquibaseDataType liquibaseDataType = DataTypeFactory.getInstance().from(temporaryDataType, remoteDatabase);
				String jdbcType = liquibaseDataType.toDatabaseDataType(remoteDatabase).getType().replaceAll("\\([^()]*\\)", "");

				DataType dataType = new DataType(jdbcType);

				if (dbColumn.getColumnType().isSupportsLength()) {
					if (dbColumn.getLength() != null) {
						dataType.setColumnSize(dbColumn.getLength());
						dataType.setColumnSizeUnit(DataType.ColumnSizeUnit.BYTE);
					} else if (dbColumn.getColumnType().getDefaultValue() != 0) {
						dataType.setColumnSize(dbColumn.getColumnType().getDefaultValue());
						dataType.setColumnSizeUnit(DataType.ColumnSizeUnit.BYTE);
					}
				}

				if (dbColumn.getColumnType().isSupportsPrecision()) {
					dataType.setDecimalDigits(dbColumn.getScale());
				}

				dataType.setDataTypeId(dbColumn.getColumnType().getTypes());

				column.setType(dataType);
				column.setSnapshotId("localSnapshot");
				column.setNullable(dbColumn.getIsNullable());

				if (Boolean.TRUE.equals(dbColumn.getIsPk())) {
					// create primary key
					String primaryKeyName = String.format("%s_%s_pk", tableName, columnName);
					PrimaryKey primaryKey = new PrimaryKey(primaryKeyName, catalogName, schemaName, tableName, column);
					table.setPrimaryKey(primaryKey);

					/* EG. delete this part after making sure for PK fields, index is automatically created by DB */
					Index index = new Index(primaryKeyName);
					index.setRelation(table);
					index.setUnique(true);
					index.setColumns(Collections.singletonList(column));
					databaseObjectList.add(index);
					primaryKey.setBackingIndex(index);
					databaseObjectList.add(primaryKey);
				}
				table.addColumn(column);
				databaseObjectList.add(column);
			}

			// create sequence
			Sequence sequence = new Sequence(catalogName, schemaName, String.format("%s_sequence", tableName));
			sequence.setMinValue(BigInteger.ONE);
			sequence.setMaxValue(new BigInteger("2147483647"));
			sequence.setIncrementBy(BigInteger.ONE);
			sequence.setStartValue(BigInteger.ONE);
			sequence.setWillCycle(false);

			databaseObjectList.add(sequence);

			schema.addDatabaseObject(table);
			databaseObjectList.add(table);

		}

		databaseObjectList.add(schema);
		databaseObjectList.add(catalog);
		return databaseObjectList.toArray(DatabaseObject[]::new);
	}

	private DatabaseSnapshot createLocalDatabaseSnapshot(Connection connection, List<DbTableDto> dbTableDtoList) {

		try {

			Database localDatabase = getRemoteDatabase(connection);
			DatabaseSnapshot localDatabaseSnapshot = new EmptyDatabaseSnapshot(localDatabase);
			DatabaseObjectCollection databaseObjectCollection = (DatabaseObjectCollection) localDatabaseSnapshot.getSerializableFieldValue("objects");
			DatabaseObject[] databaseObjectArray = generateDatabaseObjectArray(localDatabase, dbTableDtoList);
			for (DatabaseObject databaseObject : databaseObjectArray) {
				databaseObjectCollection.add(databaseObject);
			}
			return localDatabaseSnapshot;
		} catch (RuntimeException e) {
			log.error(e.getMessage());
			throw e;
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Database getRemoteDatabase(Connection connection) throws DatabaseException {
		return DatabaseFactory.getInstance()
				.findCorrectDatabaseImplementation(new JdbcConnection(connection));
	}

	private DatabaseSnapshot generateRemoteDatabaseSnapshot(Connection connection) {

		try {

			Database remoteDatabase = getRemoteDatabase(connection);
			return generateRemoteDatabaseSnapshot(remoteDatabase);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private DatabaseSnapshot generateRemoteDatabaseSnapshot(Database remoteDatabase)
			throws DatabaseException, InvalidExampleException {
		Set<Class<? extends DatabaseObject>> compareTypes = getCompareTypeSet();
		return SnapshotGeneratorFactory.getInstance()
				.createSnapshot(
						remoteDatabase.getDefaultSchema(), remoteDatabase,
						new SnapshotControl(remoteDatabase,
								compareTypes.toArray(new Class[compareTypes.size()])));
	}

	private Set<Class<? extends DatabaseObject>> getCompareTypeSet() {
		Set<Class<? extends DatabaseObject>> compareTypes = new HashSet<>();
		compareTypes.add(Table.class);
		compareTypes.add(PrimaryKey.class);
		compareTypes.add(ForeignKey.class);
		compareTypes.add(Column.class);
		//compareTypes.add(Trigger.class);
		compareTypes.add(Sequence.class);
		compareTypes.add(UniqueConstraint.class);
		compareTypes.add(Index.class);
		compareTypes.add(Schema.class);
		compareTypes.add(Catalog.class);
		return compareTypes;
	}

	private File createDiff(DatabaseSnapshot localDatabaseSnapshot, DatabaseSnapshot remoteDatabaseSnapshot)
			throws LiquibaseException, IOException {
		DiffResult diffResult = DiffGeneratorFactory.getInstance()
				.compare(localDatabaseSnapshot, remoteDatabaseSnapshot, createCompareControl());

		DiffToChangeLog diffToChangeLog = new DiffToChangeLog(diffResult, createDiffOutputControl());

		diffToChangeLog.setChangeSetAuthor("ams");
		diffToChangeLog.setChangeSetContext("production");
		diffToChangeLog.setIdRoot("text.xml");

		List<ChangeSet> changeSets = diffToChangeLog.generateChangeSets();
		return createXMLFile(changeSets);

	}

	private CompareControl createCompareControl() {
		CompareControl compareControl = new CompareControl(getCompareTypeSet());
		compareControl.addSuppressedField(Column.class,"type");
		compareControl.addSuppressedField(Column.class,"order");
		return compareControl;
	}

	private DiffOutputControl createDiffOutputControl() {
		String exclusionFilter = "";

		DiffOutputControl diffOutputControl = new DiffOutputControl(false, false, false, null);
		diffOutputControl.setObjectChangeFilter(new StandardObjectChangeFilter(StandardObjectChangeFilter.FilterType.EXCLUDE, exclusionFilter));
		diffOutputControl.setObjectQuotingStrategy(ObjectQuotingStrategy.QUOTE_ALL_OBJECTS);
		return diffOutputControl;
	}

	private void updateRemoteDatabase(Database remoteDatabase, File file) throws LiquibaseException {
		ResourceAccessor accessor = new FileSystemResourceAccessor(new File(file.getParentFile().getPath()));
		Liquibase liquibase = new Liquibase(file.getName(), accessor, remoteDatabase);
		liquibase.update(new Contexts(), new LabelExpression());
		liquibase.forceReleaseLocks();
	}

	private File createXMLFile(List<ChangeSet> changeSets) throws IOException {
		String fileName = "_" + System.nanoTime() + ".xml";
		File directory = new File("liquibase");
		if (!directory.exists()) {
			if (!directory.mkdir()) {
				return null;
			}
		}

		File file = new File(directory.getPath() + "/" + fileName);
		try (PrintStream out = new PrintStream(file, Charset.defaultCharset().name())) {
			XMLChangeLogSerializer xmlChangeLogSerializer = new XMLChangeLogSerializer();
			xmlChangeLogSerializer.write(changeSets, out);
		}
		return file;
	}

}

