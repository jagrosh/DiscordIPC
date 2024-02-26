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

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.impl.ExtendedLong;

/**
 * A encapsulation of a Discord User's data provided when a
 * {@link IPCListener IPCListener} fires
 * {@link IPCListener#onActivityJoinRequest(IPCClient, String, User)
 * onActivityJoinRequest}.
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class User {
    private final String username;
    private final String nickname;
    private final String discriminator;
    private final long id;
    private final String avatar;

    /**
     * Constructs a new {@link User}.<br>
     * Only implemented internally.
     *
     * @param username      user's name
     * @param nickname      user's nickname
     * @param discriminator user's discriminator
     * @param id            user's id
     * @param avatar        user's avatar hash, or {@code null} if they have no avatar
     */
    public User(String username, String nickname, String discriminator, long id, String avatar) {
        this.username = username;
        this.nickname = nickname;
        this.discriminator = discriminator;
        this.id = id;
        this.avatar = avatar;
    }

    /**
     * Gets the Users account name.
     *
     * @return The Users account name.
     */
    public String getName() {
        return username;
    }

    /**
     * Gets the Users nickname, if any.
     *
     * @return The Users nickname.
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Gets the Users nickname, or their account name if they
     * do not have a custom nickname set on their account.
     *
     * @return The Users effective name.
     */
    public String getEffectiveName() {
        return nickname == null ? username : nickname;
    }

    /**
     * Gets the Users discriminator.
     *
     * @return The Users discriminator.
     */
    public String getDiscriminator() {
        return discriminator;
    }

    /**
     * Gets the Users Snowflake ID as a {@code long}.
     *
     * @return The Users Snowflake ID as a {@code long}.
     */
    public long getIdLong() {
        return id;
    }

    /**
     * Gets the Users Snowflake ID as a {@code String}.
     *
     * @return The Users Snowflake ID as a {@code String}.
     */
    public String getId() {
        return Long.toString(id);
    }

    /**
     * Gets the Users avatar ID.
     *
     * @return The Users avatar ID.
     */
    public String getAvatarId() {
        return avatar;
    }

    /**
     * Gets the Users avatar URL.
     *
     * @return The Users avatar URL.
     */
    public String getAvatarUrl() {
        return getAvatarId() == null ? null : "https://cdn.discordapp.com/avatars/" + getId() + "/" + getAvatarId()
                + (getAvatarId().startsWith("a_") ? ".gif" : ".png");
    }

    /**
     * Gets the Users {@link DefaultAvatar} avatar ID.
     *
     * @return The Users {@link DefaultAvatar} avatar ID.
     */
    public String getDefaultAvatarId() {
        return DefaultAvatar.values()[(getDiscriminator().equals("0") ? (int) getIdLong() >> 22 : Integer.parseInt(getDiscriminator())) % DefaultAvatar.values().length].toString();
    }

    /**
     * Gets the Users {@link DefaultAvatar} avatar URL.
     *
     * @return The Users {@link DefaultAvatar} avatar URL.
     */
    public String getDefaultAvatarUrl() {
        return "https://discord.com/assets/" + getDefaultAvatarId() + ".png";
    }

    /**
     * Gets the Users avatar URL, or their {@link DefaultAvatar} avatar URL if they
     * do not have a custom avatar set on their account.
     *
     * @return The Users effective avatar URL.
     */
    public String getEffectiveAvatarUrl() {
        return getAvatarUrl() == null ? getDefaultAvatarUrl() : getAvatarUrl();
    }

    /**
     * Gets whether or not this User is a bot.
     * <p>
     * While, at the time of writing this documentation, bots cannot
     * use Rich Presence features, there may be a time in the future
     * where they have such an ability.
     *
     * @return False
     */
    public boolean isBot() {
        return false; //bots cannot use RPC
    }

    /**
     * Gets the User as a discord formatted mention.
     * <p>
     * {@code <@SNOWFLAKE_ID> }
     *
     * @return A discord formatted mention of this User.
     */
    public String getAsMention() {
        return "<@" + id + '>';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User))
            return false;
        User oUser = (User) o;
        return this == oUser || this.id == oUser.id;
    }

    @Override
    public int hashCode() {
        return ExtendedLong.hashCode(id);
    }

    @Override
    public String toString() {
        return "U:" + getName() + '(' + id + ')';
    }

    /**
     * Constants representing one of five different
     * default avatars a {@link User} can have.
     */
    public enum DefaultAvatar {
        BLURPLE("6debd47ed13483642cf09e832ed0bc1b"),
        GREY("322c936a8c8be1b803cd94861bdfa868"),
        GREEN("dd4dbc0016779df1378e7812eabaa04d"),
        ORANGE("0e291f67c9274a1abdddeb3fd919cbaa"),
        RED("1cbd08c76f8af6dddce02c5138971129");

        private final String text;

        DefaultAvatar(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
