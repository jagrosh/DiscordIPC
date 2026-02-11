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
 * Constants representing various Discord client party privacy levels,
 * such as Public or Private
 */
public enum PartyPrivacy {
    /**
     * Constant for the "Private" Discord RPC Party privacy level.
     */
    Private,

    /**
     * Constant for the "Public" Discord RPC Party privacy level.
     */
    Public;

    /**
     * Gets a {@link PartyPrivacy} matching the specified index.
     * <p>
     * This is only internally implemented.
     *
     * @param index The index to get from.
     * @return The {@link PartyPrivacy} corresponding to the parameters, or
     * {@link PartyPrivacy#Public} if none match.
     */
    public static PartyPrivacy from(int index) {
        for (PartyPrivacy value : values()) {
            if (value.ordinal() == index) {
                return value;
            }
        }
        return Public;
    }
}
