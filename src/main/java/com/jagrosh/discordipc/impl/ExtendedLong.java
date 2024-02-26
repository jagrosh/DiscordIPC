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

package com.jagrosh.discordipc.impl;

public class ExtendedLong {
    /**
     * Returns a hash code for a {@code long} value; compatible with
     * {@code Long.hashCode()}.
     *
     * <p>Cloned from JDK8 for Lower Version Compatibility</p>
     *
     * @param value the value to hash
     * @return a hash code value for a {@code long} value.
     */
    public static int hashCode(long value) {
        return (int) (value ^ (value >>> 32));
    }
}
