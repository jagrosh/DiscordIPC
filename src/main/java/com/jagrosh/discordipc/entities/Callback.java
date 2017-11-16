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

import java.util.function.Consumer;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Callback
{
    private final Runnable success;
    private final Consumer<String> failure;
    
    public Callback(Runnable success)
    {
        this(success, null);
    }
    
    public Callback(Consumer<String> failure)
    {
        this(null, failure);
    }
    
    public Callback(Runnable success, Consumer<String> failure)
    {
        this.success = success;
        this.failure = failure;
    }
    
    public boolean isEmpty()
    {
        return success == null && failure == null;
    }
    
    public void succeed()
    {
        if(success != null)
            success.run();
    }
    
    public void fail(String message)
    {
        if(failure != null)
            failure.accept(message);
    }
}
