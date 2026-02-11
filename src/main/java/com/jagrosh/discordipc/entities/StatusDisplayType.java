package com.jagrosh.discordipc.entities;

/**
 * Constants representing various Discord client status display types,
 * such as Name, State, or Details
 */
public enum StatusDisplayType {
    /**
     * Constant for the "Name" Discord RPC Status type.
     */
    Name,
    /**
     * Constant for the "State" Discord RPC Status type.
     */
    State,
    /**
     * Constant for the "Details" Discord RPC Status type.
     */
    Details;

    /**
     * Gets a {@link StatusDisplayType} matching the specified index.
     * <p>
     * This is only internally implemented.
     *
     * @param index The index to get from.
     * @return The {@link StatusDisplayType} corresponding to the parameters, or
     * {@link StatusDisplayType#Name} if none match.
     */
    public static StatusDisplayType from(int index) {
        for (StatusDisplayType value : values()) {
            if (value.ordinal() == index) {
                return value;
            }
        }
        return Name;
    }
}
