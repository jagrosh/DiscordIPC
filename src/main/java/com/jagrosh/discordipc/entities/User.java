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

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class User
{
    private final String name;
    private final String discriminator;
    private final long id;
    private final String avatar;
    
    public User(String name, String discriminator, long id, String avatar)
    {
        this.name = name;
        this.discriminator = discriminator;
        this.id = id;
        this.avatar = avatar;
    }
    
    public String getName()
    {
        return name;
    }
    
    public String getDiscriminator()
    {
        return discriminator;
    }
    
    public long getIdLong()
    {
        return id;
    }
    
    public String getId()
    {
        return Long.toString(id);
    }
    
    public String getAvatarId()
    {
        return avatar;
    }
    
    public String getAvatarUrl()
    {
        return getAvatarId() == null ? null : "https://cdn.discordapp.com/avatars/" + getId() + "/" + getAvatarId()
            + (getAvatarId().startsWith("a_") ? ".gif" : ".png");
    }
    
    public String getDefaultAvatarId()
    {
        return DefaultAvatar.values()[Integer.parseInt(getDiscriminator()) % DefaultAvatar.values().length].toString();
    }
    
    public String getDefaultAvatarUrl()
    {
        return "https://discordapp.com/assets/" + getDefaultAvatarId() + ".png";
    }
    
    public String getEffectiveAvatarUrl()
    {
        return getAvatarUrl() == null ? getDefaultAvatarUrl() : getAvatarUrl();
    }
    
    public boolean isBot()
    {
        return false; //bots cannot use RPC
    }
    
    public String getAsMention()
    {
        return "<@" + id + '>';
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof User))
            return false;
        User oUser = (User) o;
        return this == oUser || this.id == oUser.id;
    }
    
    @Override
    public int hashCode()
    {
        return Long.hashCode(id);
    }

    @Override
    public String toString()
    {
        return "U:" + getName() + '(' + id + ')';
    }
    
    public enum DefaultAvatar
    {
        BLURPLE("6debd47ed13483642cf09e832ed0bc1b"),
        GREY("322c936a8c8be1b803cd94861bdfa868"),
        GREEN("dd4dbc0016779df1378e7812eabaa04d"),
        ORANGE("0e291f67c9274a1abdddeb3fd919cbaa"),
        RED("1cbd08c76f8af6dddce02c5138971129");

        private final String text;

        DefaultAvatar(String text)
        {
            this.text = text;
        }

        @Override
        public String toString()
        {
            return text;
        }
    }
}
