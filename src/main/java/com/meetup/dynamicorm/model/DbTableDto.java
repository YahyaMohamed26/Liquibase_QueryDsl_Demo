package com.meetup.dynamicorm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DbTableDto {

	private Long id;

	private String name;

	private String description;

	private Boolean isClassificationEnabled = false;

	private Boolean isPublished = false;

	private Boolean isStateTransitionEnabled = false;

	private Boolean isAttachmentEnabled = false;

	private Boolean isAuditEnabled = false;

	private Set<DbColumnDto> columnSet = new LinkedHashSet<>();

}
