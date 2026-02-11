/*
 * Copyright 2017 John Grosh (john.a.grosh@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jagrosh.discordipc.entities;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Objects;

/**
 * An encapsulation of all data needed to properly construct a JSON RichPresence payload.
 *
 * <p>These can be built using {@link RichPresence.Builder}.
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RichPresence {
    private static final int MIN_ALLOWED_BUTTONS = 1;
    private static final int MAX_ALLOWED_BUTTONS = 2;
    private final ActivityType activityType;
    private final StatusDisplayType statusDisplayType;
    private final String state;
    private final String stateUrl;
    private final String details;
    private final String detailsUrl;
    private final String name;
    private final long startTimestamp;
    private final long endTimestamp;
    private final String largeImageKey;
    private final String largeImageText;
    private final String largeImageUrl;
    private final String smallImageKey;
    private final String smallImageText;
    private final String smallImageUrl;
    private final String partyId;
    private final int partySize;
    private final int partyMax;
    private final PartyPrivacy partyPrivacy;
    private final String matchSecret;
    private final String joinSecret;
    private final String spectateSecret;
    private final JsonArray buttons;
    private final boolean instance;

    public RichPresence(ActivityType activityType, StatusDisplayType statusDisplayType,
                        String state, String stateUrl,
                        String details, String detailsUrl,
                        String name, long startTimestamp, long endTimestamp,
                        String largeImageKey, String largeImageText, String largeImageUrl,
                        String smallImageKey, String smallImageText, String smallImageUrl,
                        String partyId, int partySize, int partyMax, PartyPrivacy partyPrivacy,
                        String matchSecret, String joinSecret, String spectateSecret,
                        JsonArray buttons, boolean instance) {
        this.activityType = activityType;
        this.statusDisplayType = statusDisplayType;
        this.state = state;
        this.stateUrl = stateUrl;
        this.details = details;
        this.detailsUrl = detailsUrl;
        this.name = name;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.largeImageKey = largeImageKey;
        this.largeImageText = largeImageText;
        this.largeImageUrl = largeImageUrl;
        this.smallImageKey = smallImageKey;
        this.smallImageText = smallImageText;
        this.smallImageUrl = smallImageUrl;
        this.partyId = partyId;
        this.partySize = partySize;
        this.partyMax = partyMax;
        this.partyPrivacy = partyPrivacy;
        this.matchSecret = matchSecret;
        this.joinSecret = joinSecret;
        this.spectateSecret = spectateSecret;
        this.buttons = buttons;
        this.instance = instance;
    }

    /**
     * Constructs a {@link JsonObject} representing a payload to send to discord
     * to update a user's Rich Presence.
     *
     * <p>This is purely internal, and should not ever need to be called outside
     * the library.
     *
     * @return A JSONObject payload for updating a user's Rich Presence.
     */
    public JsonObject toJson() {
        JsonObject timestamps = new JsonObject(),
                assets = new JsonObject(),
                party = new JsonObject(),
                secrets = new JsonObject(),
                finalObject = new JsonObject();

        if (startTimestamp > 0) {
            timestamps.addProperty("start", startTimestamp);

            if (endTimestamp > startTimestamp) {
                timestamps.addProperty("end", endTimestamp);
            }
        }

        if (largeImageKey != null && !largeImageKey.isEmpty()) {
            assets.addProperty("large_image", largeImageKey);

            if (largeImageText != null && !largeImageText.isEmpty()) {
                assets.addProperty("large_text", largeImageText);
            }
            if (largeImageUrl != null && !largeImageUrl.isEmpty()) {
                assets.addProperty("large_url", largeImageUrl);
            }
        }

        if (smallImageKey != null && !smallImageKey.isEmpty()) {
            assets.addProperty("small_image", smallImageKey);

            if (smallImageText != null && !smallImageText.isEmpty()) {
                assets.addProperty("small_text", smallImageText);
            }
            if (smallImageUrl != null && !smallImageUrl.isEmpty()) {
                assets.addProperty("small_url", smallImageUrl);
            }
        }

        if ((partyId != null && !partyId.isEmpty()) ||
                (partySize > 0 && partyMax > 0)) {
            if (partyId != null && !partyId.isEmpty()) {
                party.addProperty("id", partyId);
            }

            JsonArray partyData = new JsonArray();

            if (partySize > 0) {
                partyData.add(new JsonPrimitive(partySize));

                if (partyMax >= partySize) {
                    partyData.add(new JsonPrimitive(partyMax));
                }
            }
            party.add("size", partyData);
            party.add("privacy", new JsonPrimitive(partyPrivacy.ordinal()));
        }

        if (joinSecret != null && !joinSecret.isEmpty()) {
            secrets.addProperty("join", joinSecret);
        }
        if (spectateSecret != null && !spectateSecret.isEmpty()) {
            secrets.addProperty("spectate", spectateSecret);
        }
        if (matchSecret != null && !matchSecret.isEmpty()) {
            secrets.addProperty("match", matchSecret);
        }

        finalObject.addProperty("type", activityType.ordinal());
        finalObject.addProperty("status_display_type", statusDisplayType.ordinal());

        if (state != null && !state.isEmpty()) {
            finalObject.addProperty("state", state);

            if (stateUrl != null && !stateUrl.isEmpty()) {
                finalObject.addProperty("state_url", stateUrl);
            }
        }
        if (details != null && !details.isEmpty()) {
            finalObject.addProperty("details", details);

            if (detailsUrl != null && !detailsUrl.isEmpty()) {
                finalObject.addProperty("details_url", detailsUrl);
            }
        }

        if (name != null && !name.isEmpty()) {
            finalObject.addProperty("name", name);
        }

        if (timestamps.has("start")) {
            finalObject.add("timestamps", timestamps);
        }
        if (assets.has("large_image")) {
            finalObject.add("assets", assets);
        }
        if (party.has("id")) {
            finalObject.add("party", party);
        }
        if (secrets.has("join") || secrets.has("spectate") || secrets.has("match")) {
            finalObject.add("secrets", secrets);
        }
        if (buttons != null && !buttons.isJsonNull() && buttons.size() >= MIN_ALLOWED_BUTTONS && buttons.size() <= MAX_ALLOWED_BUTTONS) {
            finalObject.add("buttons", buttons);
        }
        finalObject.addProperty("instance", instance);

        return finalObject;
    }

    public String toDecodedJson(String encoding) {
        try {
            return new String(toJson().toString().getBytes(encoding));
        } catch (Exception ex) {
            return toJson().toString();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RichPresence))
            return false;
        RichPresence oPresence = (RichPresence) o;
        return this == oPresence || (
                Objects.equals(activityType, oPresence.activityType) &&
                        Objects.equals(statusDisplayType, oPresence.statusDisplayType) &&
                        Objects.equals(state, oPresence.state) &&
                        Objects.equals(stateUrl, oPresence.stateUrl) &&
                        Objects.equals(details, oPresence.details) &&
                        Objects.equals(detailsUrl, oPresence.detailsUrl) &&
                        Objects.equals(name, oPresence.name) &&
                        Objects.equals(startTimestamp, oPresence.startTimestamp) &&
                        Objects.equals(endTimestamp, oPresence.endTimestamp) &&
                        Objects.equals(largeImageKey, oPresence.largeImageKey) &&
                        Objects.equals(largeImageText, oPresence.largeImageText) &&
                        Objects.equals(largeImageUrl, oPresence.largeImageUrl) &&
                        Objects.equals(smallImageKey, oPresence.smallImageKey) &&
                        Objects.equals(smallImageText, oPresence.smallImageText) &&
                        Objects.equals(smallImageUrl, oPresence.smallImageUrl) &&
                        Objects.equals(partyId, oPresence.partyId) &&
                        Objects.equals(partySize, oPresence.partySize) &&
                        Objects.equals(partyMax, oPresence.partyMax) &&
                        Objects.equals(partyPrivacy, oPresence.partyPrivacy) &&
                        Objects.equals(matchSecret, oPresence.matchSecret) &&
                        Objects.equals(joinSecret, oPresence.joinSecret) &&
                        Objects.equals(spectateSecret, oPresence.spectateSecret) &&
                        Objects.equals(buttons, oPresence.buttons) &&
                        Objects.equals(instance, oPresence.instance)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                activityType, statusDisplayType,
                state, stateUrl,
                details, detailsUrl,
                name, startTimestamp, endTimestamp,
                largeImageKey, largeImageText, largeImageUrl,
                smallImageKey, smallImageText, smallImageUrl,
                partyId, partySize, partyMax, partyPrivacy,
                matchSecret, joinSecret, spectateSecret,
                buttons, instance
        );
    }

    /**
     * A chain builder for a {@link RichPresence} object.
     *
     * <p>An accurate description of each field and it's functions can be found
     * <a href="https://discord.com/developers/docs/rich-presence/how-to#updating-presence-update-presence-payload-fields">here</a>
     */
    public static class Builder {
        private ActivityType activityType = ActivityType.Playing;
        private StatusDisplayType statusDisplayType = StatusDisplayType.Name;
        private String state;
        private String stateUrl;
        private String details;
        private String detailsUrl;
        private String name;
        private long startTimestamp;
        private long endTimestamp;
        private String largeImageKey;
        private String largeImageText;
        private String largeImageUrl;
        private String smallImageKey;
        private String smallImageText;
        private String smallImageUrl;
        private String partyId;
        private int partySize;
        private int partyMax;
        private PartyPrivacy partyPrivacy = PartyPrivacy.Private;
        private String matchSecret;
        private String joinSecret;
        private String spectateSecret;
        private JsonArray buttons;
        private boolean instance;

        /**
         * Builds the {@link RichPresence} from the current state of this builder.
         *
         * @return The RichPresence built.
         */
        public RichPresence build() {
            return new RichPresence(activityType, statusDisplayType,
                    state, stateUrl,
                    details, detailsUrl,
                    name, startTimestamp, endTimestamp,
                    largeImageKey, largeImageText, largeImageUrl,
                    smallImageKey, smallImageText, smallImageUrl,
                    partyId, partySize, partyMax, partyPrivacy,
                    matchSecret, joinSecret, spectateSecret,
                    buttons, instance);
        }

        /**
         * Sets the activity type for the player's current activity
         *
         * @param activityType The new activity type
         * @return This Builder.
         */
        public Builder setActivityType(ActivityType activityType) {
            this.activityType = activityType;
            return this;
        }

        /**
         * Sets the status display type for the player's current activity
         *
         * @param statusDisplayType The new status display type
         * @return This Builder.
         */
        public Builder setStatusDisplayType(StatusDisplayType statusDisplayType) {
            this.statusDisplayType = statusDisplayType;
            return this;
        }

        /**
         * Sets the state of the user's current party.
         *
         * @param state The state of the user's current party.
         * @return This Builder.
         */
        public Builder setState(String state) {
            this.state = state;
            return this;
        }

        /**
         * Sets the state url of the user's current party
         *
         * @param stateUrl The state url of the user's current party.
         * @return This Builder.
         */
        public Builder setStateUrl(String stateUrl) {
            this.stateUrl = stateUrl;
            return this;
        }

        /**
         * Sets details of what the player is currently doing.
         *
         * @param details The details of what the player is currently doing.
         * @return This Builder.
         */
        public Builder setDetails(String details) {
            this.details = details;
            return this;
        }

        /**
         * Sets the details url of what the player is currently doing.
         *
         * @param detailsUrl The details url of what the player is currently doing
         * @return This Builder.
         */
        public Builder setDetailsUrl(String detailsUrl) {
            this.detailsUrl = detailsUrl;
            return this;
        }

        /**
         * Sets the player activity name.
         *
         * @param name The player activity name.
         * @return This Builder.
         */
        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the time that the player started a match or activity.
         *
         * @param startTimestamp The time the player started a match or activity.
         * @return This Builder.
         */
        public Builder setStartTimestamp(long startTimestamp) {
            this.startTimestamp = startTimestamp;
            return this;
        }

        /**
         * Sets the time that the player's current activity will end.
         *
         * @param endTimestamp The time the player's activity will end.
         * @return This Builder.
         */
        public Builder setEndTimestamp(long endTimestamp) {
            this.endTimestamp = endTimestamp;
            return this;
        }

        /**
         * Sets the key of the uploaded image for the large profile artwork, as well as
         * the text tooltip shown when a cursor hovers over it, and the url when clicked.
         *
         * <p>These can be configured in the <a href="https://discord.com/developers/applications/me">applications</a>
         * page on the discord website.
         *
         * @param largeImageKey  A key to an image to display.
         * @param largeImageText Text displayed when a cursor hovers over the large image.
         * @param largeImageUrl The Url to navigate to when clicking the large image.
         * @return This Builder.
         */
        public Builder setLargeImage(String largeImageKey, String largeImageText, String largeImageUrl) {
            this.largeImageKey = largeImageKey;
            this.largeImageText = largeImageText;
            this.largeImageUrl = largeImageUrl;
            return this;
        }

        /**
         * Sets the key of the uploaded image for the large profile artwork, as well as
         * the text tooltip shown when a cursor hovers over it.
         *
         * <p>These can be configured in the <a href="https://discord.com/developers/applications/me">applications</a>
         * page on the discord website.
         *
         * @param largeImageKey  A key to an image to display.
         * @param largeImageText Text displayed when a cursor hovers over the large image.
         * @return This Builder.
         */
        public Builder setLargeImageWithTooltip(String largeImageKey, String largeImageText) {
            return setLargeImage(largeImageKey, largeImageText, null);
        }

        /**
         * Sets the key of the uploaded image for the large profile artwork, as well as
         * the url to navigate to when clicked.
         *
         * <p>These can be configured in the <a href="https://discord.com/developers/applications/me">applications</a>
         * page on the discord website.
         *
         * @param largeImageKey  A key to an image to display.
         * @param largeImageUrl The Url to navigate to when clicking the large image.
         * @return This Builder.
         */
        public Builder setLargeImageWithUrl(String largeImageKey, String largeImageUrl) {
            return setLargeImage(largeImageKey, null, largeImageUrl);
        }

        /**
         * Sets the key of the uploaded image for the large profile artwork.
         *
         * <p>These can be configured in the <a href="https://discord.com/developers/applications/me">applications</a>
         * page on the discord website.
         *
         * @param largeImageKey A key to an image to display.
         * @return This Builder.
         */
        public Builder setLargeImage(String largeImageKey) {
            return setLargeImage(largeImageKey, null, null);
        }

        /**
         * Sets the key of the uploaded image for the small profile artwork, as well as
         * the text tooltip shown when a cursor hovers over it, and the url when clicked.
         *
         * <p>These can be configured in the <a href="https://discord.com/developers/applications/me">applications</a>
         * page on the discord website.
         *
         * @param smallImageKey  A key to an image to display.
         * @param smallImageText Text displayed when a cursor hovers over the small image.
         * @param smallImageUrl The Url to navigate to when clicking the small image.
         * @return This Builder.
         */
        public Builder setSmallImage(String smallImageKey, String smallImageText, String smallImageUrl) {
            this.smallImageKey = smallImageKey;
            this.smallImageText = smallImageText;
            this.smallImageUrl = smallImageUrl;
            return this;
        }

        /**
         * Sets the key of the uploaded image for the small profile artwork, as well as
         * the text tooltip shown when a cursor hovers over it.
         *
         * <p>These can be configured in the <a href="https://discord.com/developers/applications/me">applications</a>
         * page on the discord website.
         *
         * @param smallImageKey  A key to an image to display.
         * @param smallImageText Text displayed when a cursor hovers over the small image.
         * @return This Builder.
         */
        public Builder setSmallImageWithTooltip(String smallImageKey, String smallImageText) {
            return setSmallImage(smallImageKey, smallImageText, null);
        }

        /**
         * Sets the key of the uploaded image for the small profile artwork, as well as
         * the url to navigate to when clicked.
         *
         * <p>These can be configured in the <a href="https://discord.com/developers/applications/me">applications</a>
         * page on the discord website.
         *
         * @param smallImageKey  A key to an image to display.
         * @param smallImageUrl The Url to navigate to when clicking the small image.
         * @return This Builder.
         */
        public Builder setSmallImageWithUrl(String smallImageKey, String smallImageUrl) {
            return setSmallImage(smallImageKey, null, smallImageUrl);
        }

        /**
         * Sets the key of the uploaded image for the small profile artwork.
         *
         * <p>These can be configured in the <a href="https://discord.com/developers/applications/me">applications</a>
         * page on the discord website.
         *
         * @param smallImageKey A key to an image to display.
         * @return This Builder.
         */
        public Builder setSmallImage(String smallImageKey) {
            return setSmallImage(smallImageKey, null, null);
        }

        /**
         * Sets party configurations for a team, lobby, or other form of group.
         *
         * <p>The {@code partyId} is ID of the player's party.
         * <br>The {@code partySize} is the current size of the player's party.
         * <br>The {@code partyMax} is the maximum number of player's allowed in the party.
         *
         * @param partyId      The ID of the player's party.
         * @param partySize    The current size of the player's party.
         * @param partyMax     The maximum number of player's allowed in the party.
         * @param partyPrivacy The privacy level for the player's party.
         * @return This Builder.
         */
        public Builder setParty(String partyId, int partySize, int partyMax, PartyPrivacy partyPrivacy) {
            this.partyId = partyId;
            this.partySize = partySize;
            this.partyMax = partyMax;
            this.partyPrivacy = partyPrivacy;
            return this;
        }

        /**
         * Sets the unique hashed string for Spectate and Join.
         *
         * @param matchSecret The unique hashed string for Spectate and Join.
         * @return This Builder.
         */
        public Builder setMatchSecret(String matchSecret) {
            this.matchSecret = matchSecret;
            return this;
        }

        /**
         * Sets the unique hashed string for chat invitations and Ask to Join.
         *
         * @param joinSecret The unique hashed string for chat invitations and Ask to Join.
         * @return This Builder.
         */
        public Builder setJoinSecret(String joinSecret) {
            this.joinSecret = joinSecret;
            return this;
        }

        /**
         * Sets the unique hashed string for Spectate button.
         *
         * @param spectateSecret The unique hashed string for Spectate button.
         * @return This Builder.
         */
        public Builder setSpectateSecret(String spectateSecret) {
            this.spectateSecret = spectateSecret;
            return this;
        }

        /**
         * Sets the button array to be used within the RichPresence
         * <p>Must be a format of {'label': "...", 'url': "..."} with a max length of 2</p>
         *
         * @param buttons The new array of button objects to use
         * @return This Builder.
         */
        public Builder setButtons(JsonArray buttons) {
            this.buttons = buttons;
            return this;
        }

        /**
         * Marks the {@link #setMatchSecret(String) matchSecret} as a game
         * session with a specific beginning and end.
         *
         * @param instance Whether the {@code matchSecret} is a game
         *                 with a specific beginning and end.
         * @return This Builder.
         */
        public Builder setInstance(boolean instance) {
            this.instance = instance;
            return this;
        }
    }
}
