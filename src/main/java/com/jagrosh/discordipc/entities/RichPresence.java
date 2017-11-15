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

import java.time.OffsetDateTime;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RichPresence
{
    private final String state;
    private final String details;
    private final OffsetDateTime startTimestamp;
    private final OffsetDateTime endTimestamp;
    private final String largeImageKey;
    private final String largeImageText;
    private final String smallImageKey;
    private final String smallImageText;
    private final String partyId;
    private final int partySize;
    private final int partyMax;
    private final String matchSecret;
    private final String joinSecret;
    private final String spectateSecret;
    private final boolean instance;
    
    public RichPresence(String state, String details, OffsetDateTime startTimestamp, OffsetDateTime endTimestamp, 
            String largeImageKey, String largeImageText, String smallImageKey, String smallImageText, 
            String partyId, int partySize, int partyMax, String matchSecret, String joinSecret, 
            String spectateSecret, boolean instance)
    {
        this.state = state;
        this.details = details;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.largeImageKey = largeImageKey;
        this.largeImageText = largeImageText;
        this.smallImageKey = smallImageKey;
        this.smallImageText = smallImageText;
        this.partyId = partyId;
        this.partySize = partySize;
        this.partyMax = partyMax;
        this.matchSecret = matchSecret;
        this.joinSecret = joinSecret;
        this.spectateSecret = spectateSecret;
        this.instance = instance;
    }
            
    public JSONObject toJson()
    {
        return new JSONObject()
                .put("state", state)
                .put("details", details)
                .put("timestamps", new JSONObject()
                        .put("start", startTimestamp==null ? null : startTimestamp.toEpochSecond())
                        .put("end", endTimestamp==null ? null : endTimestamp.toEpochSecond()))
                .put("assets", new JSONObject()
                        .put("large_image", largeImageKey)
                        .put("large_text", largeImageText)
                        .put("small_image", smallImageKey)
                        .put("small_text", smallImageText))
                .put("party", new JSONObject()
                        .put("id", partyId)
                        .put("size", new JSONArray().put(partySize).put(partyMax)))
                .put("secrets", new JSONObject()
                        .put("join", joinSecret)
                        .put("spectate", spectateSecret)
                        .put("match", matchSecret))
                .put("instance", instance);
    }
    
    //const char* details; /* max 128 bytes */
    //int64_t startTimestamp;
    //int64_t endTimestamp;
    //const char* largeImageKey;  /* max 32 bytes */
    //const char* largeImageText; /* max 128 bytes */
    //const char* smallImageKey;  /* max 32 bytes */
    //const char* smallImageText; /* max 128 bytes */
    //const char* partyId;        /* max 128 bytes */
    //int partySize;
    //int partyMax;
    //const char* matchSecret;    /* max 128 bytes */
    //const char* joinSecret;     /* max 128 bytes */
    //const char* spectateSecret; /* max 128 bytes */
    //int8_t instance;
}
