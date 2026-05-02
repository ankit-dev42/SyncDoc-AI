package com.syncdoc.collaboration.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.UUID;

/**
 * JPA AttributeConverter that transparently maps a {@code String} field
 * to a PostgreSQL {@code uuid} column (and back).
 *
 * <p>Apply with {@code @Convert(converter = UUIDStringConverter.class)} on
 * any {@code String} field that maps to a {@code uuid} DB column.
 */
@Converter
public class UUIDStringConverter implements AttributeConverter<String, UUID> {

    @Override
    public UUID convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return null;
        }
        return UUID.fromString(attribute);
    }

    @Override
    public String convertToEntityAttribute(UUID dbData) {
        return dbData == null ? null : dbData.toString();
    }
}
