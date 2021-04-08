package com.davidcubesvk.clicksPerSecond.utils.replacer;

/**
 * Interface used to replace placeholders in command messages.
 */
public interface CommandMessageReplacer {

    /**
     * Replaces placeholders in the given message.
     *
     * @param message the message to replace placeholders in
     * @return the message with replaced placeholders
     */
    String replaceInMessage(String message);

}
