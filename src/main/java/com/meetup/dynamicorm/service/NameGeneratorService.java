package com.meetup.dynamicorm.service;

import org.springframework.stereotype.Service;

@Service
public class NameGeneratorService {

	/**
	 * returns the {tableName}_sequence as the sequence name for the given table name
	 * e.g.
	 *  book --> book_sequence
	 *  author --> author_sequence
	 *
	 * @param tableName
	 * @return
	 */
	public String getSequenceName(String tableName) {
		return String.format("%s_sequence", tableName);
	}

	/**
	 * returns the _{tableName} as the alias name used in SQL queris
	 * e.g.
	 *  select _book.id, _book.title
	 *  from book _book
	 *
	 * @param tableName
	 * @return
	 */
	public String getTableAliasName(String tableName) {
		return String.format("_%s", tableName);
	}

}
