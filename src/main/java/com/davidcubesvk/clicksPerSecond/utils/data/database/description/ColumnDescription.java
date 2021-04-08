package com.davidcubesvk.clicksPerSecond.utils.data.database.description;

/**
 * Holds a description of a column.
 */
public class ColumnDescription {

    //Data
    private String field, type, nullable, key, defaultValue, extra;

    /**
     * Initializes the column description with the given information.
     *
     * @param field        name of the column
     * @param type         data type stored in the column
     * @param nullable     if the column allows <code>NULL</code> values (<code>YES</code> or <code>NO</code>)
     * @param key          key specification, if it is <code>UNIQUE</code> or <code>PRIMARY</code>
     * @param defaultValue default value for the column
     * @param extra        extra specifications, like <code>auto_increment</code> (stands for <code>AUTO_INCREMENT</code>)
     */
    public ColumnDescription(String field, String type, String nullable, String key, String defaultValue, String extra) {
        this.field = field;
        this.type = type;
        this.nullable = nullable;
        this.key = key;
        this.defaultValue = defaultValue;
        this.extra = extra;
    }

    /**
     * Returns the field (column) name.
     *
     * @return the field (column) name
     */
    public String getField() {
        return field;
    }

    /**
     * Returns the data type stored in the column.
     *
     * @return the data type stored
     */
    public String getType() {
        return type;
    }

    /**
     * Returns if <code>NULL</code> values (returns <code>"YES"</code> or <code>"NO"</code>) are allowed by this column.
     *
     * @return if <code>NULL</code> values are allowed (returns <code>"YES"</code> or <code>"NO"</code>)
     */
    public String getNullable() {
        return nullable;
    }

    /**
     * Returns the key specification, if it is <code>UNIQUE</code> or <code>PRIMARY</code>, or returns an empty string if there's no key specification.
     *
     * @return the key specification, or an empty string if there's no key specification
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the default value for this column, or returns <code>null</code> if there's no default value.
     *
     * @return the default value, or <code>null</code> if there's no default value
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Returns all extra specifications, like <code>auto_increment</code> (stands for <code>AUTO_INCREMENT</code>), or returns an empty string if there's no extra specification.
     *
     * @return all extra specifications, or an empty string if there's no extra specification
     */
    public String getExtra() {
        return extra;
    }

    /**
     * Compares current description with the given description.
     *
     * @param columnDescription the description to compare
     * @return if both descriptions equal
     */
    public boolean equalsDesc(ColumnDescription columnDescription) {
        return equalsWithNull(field, columnDescription.field) &&
                equalsWithNull(type, columnDescription.type) &&
                equalsWithNull(nullable, columnDescription.nullable) &&
                equalsWithNull(key, columnDescription.key) &&
                equalsWithNull(defaultValue, columnDescription.defaultValue) &&
                equalsWithNull(extra, columnDescription.extra);
    }

    /**
     * Provides the same functionality as {@link String#equals(Object)}, but allows <code>null</code> values.
     *
     * @param a a string
     * @param b a string
     * @return if the given strings equal
     */
    private boolean equalsWithNull(String a, String b) {
        //If both are null
        if (a == null && b == null)
            return true;
        //If one is null and the other not (both can't be null, covered in the first if statement)
        if (a == null || b == null)
            return false;

        //Return using equals
        return a.equals(b);
    }
}
