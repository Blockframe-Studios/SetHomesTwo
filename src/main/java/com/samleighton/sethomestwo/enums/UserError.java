package com.samleighton.sethomestwo.enums;

public enum UserError {
    /** Home Item restriction */
    INVALID_HOME_ITEM("This home item does not belong to you."),

    /** Max Home restriction */
    SET_MAX_HOMES_SINGULAR("Max Homes Type is singular. Usage: /%s [max number of homes]"),
    SET_MAX_HOMES_GROUPS("Max Homes Type is groups. Usage: /%s [group name] [max number of homes]"),
    MAX_HOMES("You have reached the maximum number of homes allowed."),

    /** Teleport restriction */
    TELEPORT_IS_BLACKLISTED("You cannot teleport to this home because the dimension it is in has been blacklisted."),
    DIMENSION_IS_BLACKLISTED("You cannot set a home in this dimension because it has been blacklisted."),
    MOVED_WHILE_TELEPORTING("Your teleport has been canceled because you have moved."),
    ALREADY_TELEPORTING("You cannot teleport while already teleporting."),
    UNSAFE_HOME("Teleport canceled: this home is not safe to stand in and no safe spot was found nearby."),

    /** Command Input Errors */
    DIMENSION_IS_NOT_BLACKLISTED("The %s dimension has not been blacklisted yet therefore you cannot remove it."),
    INVALID_WORLD("%s is not a valid world. This server's worlds are: %s"),
    DELETE_HOME_USAGE("Usage: /%s [name]"),
    INVALID_MATERIAL("The material you entered is not valid, please try a different one."),
    PLAYER_NOT_FOUND("No player by that name is online or has any saved homes."),
    PLAYERS_ONLY("Only players may execute this command."),
    DIMENSION_ALREADY_BLACKLISTED("The %s dimension has already been blacklisted. You cannot add it again."),
    GROUP_DOES_NOT_EXIST("Group does not exist. Use /get-max-homes-groups to see all groups."),
    HOME_DOES_NOT_EXIST("The home '%s' does not exist."),
    DUPLICATE_HOME_NAME("You already have a home called '%s'."),
    INVALID_HOME_NAME("That home name is not valid. Names must not be blank."),
    HOME_NAME_TOO_LONG("That home name is too long. The maximum is %d characters."),
    HOME_NO_LONGER_EXISTS("That home no longer exists."),
    EMPTY_HAND_FOR_ICON("Hold the item you want to use as the icon, then click again."),
    CANNOT_MOVE_TO_BLACKLISTED_DIMENSION("You cannot move a home into this dimension because it has been blacklisted.");


    private final String value;

    UserError(String message) {
        this.value = message;
    }

    public String getValue() {
        return value;
    }
}
