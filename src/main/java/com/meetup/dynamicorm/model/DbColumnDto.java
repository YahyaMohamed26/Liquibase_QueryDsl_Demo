package com.meetup.dynamicorm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DbColumnDto implements Serializable {

	private Long id;

	private String name;

	private String title;

	private String description;

	private ColumnType columnType;

	private Boolean isPk = false;

	private Integer length;

	private Integer scale;

	private Boolean isNullable = false;

	private Boolean isUnique = false;

	private String defaultValue;

}
