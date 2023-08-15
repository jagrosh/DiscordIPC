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

import net.lenni0451.reflect.Methods;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

public class WinRegistry {
    public static final int HKEY_CURRENT_USER = 0x80000001;
    public static final int HKEY_LOCAL_MACHINE = 0x80000002;
    public static final int REG_SUCCESS = 0;

    private static final int KEY_ALL_ACCESS = 0xf003f;
    private static final int KEY_READ = 0x20019;
    private static final Preferences userRoot = Preferences.userRoot();
    private static final Preferences systemRoot = Preferences.systemRoot();
    private static final Class<? extends Preferences> userClass = userRoot.getClass();
    private static final Method regOpenKey;
    private static final Method regCloseKey;
    private static final Method regQueryValueEx;
    private static final Method regEnumValue;
    private static final Method regQueryInfoKey;
    private static final Method regEnumKeyEx;
    private static final Method regCreateKeyEx;
    private static final Method regSetValueEx;
    private static final Method regDeleteKey;
    private static final Method regDeleteValue;

    private static final float javaSpec;

    static {
        try {
            javaSpec = Float.parseFloat(System.getProperty("java.specification.version"));

            regOpenKey = Methods.getDeclaredMethod(userClass, "WindowsRegOpenKey",
                    (javaSpec >= 11 ? long.class : int.class), byte[].class, int.class);
            regCloseKey = Methods.getDeclaredMethod(userClass, "WindowsRegCloseKey",
                    (javaSpec >= 11 ? long.class : int.class));
            regQueryValueEx = Methods.getDeclaredMethod(userClass, "WindowsRegQueryValueEx",
                    (javaSpec >= 11 ? long.class : int.class), byte[].class);
            regEnumValue = Methods.getDeclaredMethod(userClass, "WindowsRegEnumValue",
                    (javaSpec >= 11 ? long.class : int.class), int.class, int.class);
            regQueryInfoKey = Methods.getDeclaredMethod(userClass, "WindowsRegQueryInfoKey1",
                    (javaSpec >= 11 ? long.class : int.class));
            regEnumKeyEx = Methods.getDeclaredMethod(userClass,
                    "WindowsRegEnumKeyEx", (javaSpec >= 11 ? long.class : int.class), int.class,
                    int.class);
            regCreateKeyEx = Methods.getDeclaredMethod(userClass,
                    "WindowsRegCreateKeyEx", (javaSpec >= 11 ? long.class : int.class),
                    byte[].class);
            regSetValueEx = Methods.getDeclaredMethod(userClass,
                    "WindowsRegSetValueEx", (javaSpec >= 11 ? long.class : int.class),
                    byte[].class, byte[].class);
            regDeleteValue = Methods.getDeclaredMethod(userClass,
                    "WindowsRegDeleteValue", (javaSpec >= 11 ? long.class : int.class),
                    byte[].class);
            regDeleteKey = Methods.getDeclaredMethod(userClass,
                    "WindowsRegDeleteKey", (javaSpec >= 11 ? long.class : int.class),
                    byte[].class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private WinRegistry() {
    }

    /**
     * Read a value from key and value name
     *
     * @param hkey      HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key       The target key
     * @param valueName The target value name
     * @return the value
     * @throws IllegalArgumentException  if hkey is invalid
     * @throws IllegalAccessException    if permissions insufficient
     * @throws InvocationTargetException if underlying method(s) throw(s) an exception
     */
    public static String readString(int hkey, String key, String valueName)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        if (hkey == HKEY_LOCAL_MACHINE) {
            return readString(systemRoot, hkey, key, valueName);
        } else if (hkey == HKEY_CURRENT_USER) {
            return readString(userRoot, hkey, key, valueName);
        } else {
            throw new IllegalArgumentException("hkey=" + hkey);
        }
    }

    /**
     * Read a value from key and value name
     *
     * @param hkey      HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key       The target key
     * @param valueName The target value name
     * @return the value
     * @throws IllegalArgumentException  if hkey is invalid
     * @throws IllegalAccessException    if permissions insufficient
     * @throws InvocationTargetException if underlying method(s) throw(s) an exception
     */
    public static String readString(long hkey, String key, String valueName)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        if (hkey == HKEY_LOCAL_MACHINE) {
            return readString(systemRoot, hkey, key, valueName);
        } else if (hkey == HKEY_CURRENT_USER) {
            return readString(userRoot, hkey, key, valueName);
        } else {
            throw new IllegalArgumentException("hkey=" + hkey);
        }
    }

    /**
     * Read value(s) and value name(s) form given key
     *
     * @param hkey HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key  The target key
     * @return the value name(s) plus the value(s)
     * @throws IllegalArgumentException  if hkey is invalid
     * @throws IllegalAccessException    if permissions insufficient
     * @throws InvocationTargetException if underlying method(s) throw(s) an exception
     */
    public static Map<String, String> readStringValues(int hkey, String key)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        if (hkey == HKEY_LOCAL_MACHINE) {
            return readStringValues(systemRoot, hkey, key);
        } else if (hkey == HKEY_CURRENT_USER) {
            return readStringValues(userRoot, hkey, key);
        } else {
            throw new IllegalArgumentException("hkey=" + hkey);
        }
    }

    /**
     * Read the value name(s) from a given key
     *
     * @param hkey HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key  The target key
     * @return the value name(s)
     * @throws IllegalArgumentException  if hkey is invalid
     * @throws IllegalAccessException    if permissions insufficient
     * @throws InvocationTargetException if underlying method(s) throw(s) an exception
     */
    public static List<String> readStringSubKeys(int hkey, String key)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        if (hkey == HKEY_LOCAL_MACHINE) {
            return readStringSubKeys(systemRoot, hkey, key);
        } else if (hkey == HKEY_CURRENT_USER) {
            return readStringSubKeys(userRoot, hkey, key);
        } else {
            throw new IllegalArgumentException("hkey=" + hkey);
        }
    }

    /**
     * Create a key
     *
     * @param hkey HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key  The target key
     * @throws IllegalArgumentException  if hkey is invalid
     * @throws IllegalAccessException    if permissions insufficient
     * @throws InvocationTargetException if underlying method(s) throw an exception
     */
    public static void createKey(int hkey, String key)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        int[] ret;
        if (hkey == HKEY_LOCAL_MACHINE) {
            ret = createKey(systemRoot, hkey, key);
            Methods.invoke(systemRoot, regCloseKey, ret[0]);
        } else if (hkey == HKEY_CURRENT_USER) {
            ret = createKey(userRoot, hkey, key);
            Methods.invoke(userRoot, regCloseKey, ret[0]);
        } else {
            throw new IllegalArgumentException("hkey=" + hkey);
        }
        if (ret[1] != REG_SUCCESS) {
            throw new IllegalArgumentException("rc=" + ret[1] + "  key=" + key);
        }
    }

    /**
     * Create a key
     *
     * @param hkey HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key  The target key
     * @throws IllegalArgumentException  if hkey is invalid
     * @throws IllegalAccessException    if permissions insufficient
     * @throws InvocationTargetException if underlying method(s) throw an exception
     */
    public static void createKey(long hkey, String key)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        long[] ret;
        if (hkey == HKEY_LOCAL_MACHINE) {
            ret = createKey(systemRoot, hkey, key);
            Methods.invoke(systemRoot, regCloseKey, ret[0]);
        } else if (hkey == HKEY_CURRENT_USER) {
            ret = createKey(userRoot, hkey, key);
            Methods.invoke(userRoot, regCloseKey, ret[0]);
        } else {
            throw new IllegalArgumentException("hkey=" + hkey);
        }
        if (ret[1] != REG_SUCCESS) {
            throw new IllegalArgumentException("rc=" + ret[1] + "  key=" + key);
        }
    }

    /**
     * Write a value in a given key/value name
     *
     * @param hkey      HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key       The target key
     * @param valueName The target value name
     * @param value     The target value
     * @throws IllegalArgumentException  if hkey is invalid
     * @throws IllegalAccessException    if permissions insufficient
     * @throws InvocationTargetException if underlying method(s) throw an exception
     */
    public static void writeStringValue
    (int hkey, String key, String valueName, String value)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        if (hkey == HKEY_LOCAL_MACHINE) {
            writeStringValue(systemRoot, hkey, key, valueName, value);
        } else if (hkey == HKEY_CURRENT_USER) {
            writeStringValue(userRoot, hkey, key, valueName, value);
        } else {
            throw new IllegalArgumentException("hkey=" + hkey);
        }
    }

    /**
     * Write a value in a given key/value name
     *
     * @param hkey      HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key       The target key
     * @param valueName The target value name
     * @param value     The target value
     * @throws IllegalArgumentException  if hkey is invalid
     * @throws IllegalAccessException    if permissions insufficient
     * @throws InvocationTargetException if underlying method(s) throw an exception
     */
    public static void writeStringValue
    (long hkey, String key, String valueName, String value)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        if (hkey == HKEY_LOCAL_MACHINE) {
            writeStringValue(systemRoot, hkey, key, valueName, value);
        } else if (hkey == HKEY_CURRENT_USER) {
            writeStringValue(userRoot, hkey, key, valueName, value);
        } else {
            throw new IllegalArgumentException("hkey=" + hkey);
        }
    }

    /**
     * Delete a given key
     *
     * @param hkey HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key  The target key
     * @throws IllegalArgumentException  if hkey is invalid
     * @throws IllegalAccessException    if permissions insufficient
     * @throws InvocationTargetException if underlying method(s) throw an exception
     */
    public static void deleteKey(int hkey, String key)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        int rc = -1;
        if (hkey == HKEY_LOCAL_MACHINE) {
            rc = deleteKey(systemRoot, hkey, key);
        } else if (hkey == HKEY_CURRENT_USER) {
            rc = deleteKey(userRoot, hkey, key);
        }
        if (rc != REG_SUCCESS) {
            throw new IllegalArgumentException("rc=" + rc + "  key=" + key);
        }
    }

    /**
     * delete a value from a given key/value name
     *
     * @param hkey  HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key   The target key
     * @param value The target value
     * @throws IllegalArgumentException  if hkey is invalid
     * @throws IllegalAccessException    if permissions insufficient
     * @throws InvocationTargetException if underlying method(s) throw an exception
     */
    public static void deleteValue(int hkey, String key, String value)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        int rc = -1;
        if (hkey == HKEY_LOCAL_MACHINE) {
            rc = deleteValue(systemRoot, hkey, key, value);
        } else if (hkey == HKEY_CURRENT_USER) {
            rc = deleteValue(userRoot, hkey, key, value);
        }
        if (rc != REG_SUCCESS) {
            throw new IllegalArgumentException("rc=" + rc + "  key=" + key + "  value=" + value);
        }
    }

    // =====================

    private static int deleteValue
            (Preferences root, int hkey, String key, String value)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        int[] handles = Methods.invoke(root, regOpenKey, new Object[]{
                hkey, toCstr(key), KEY_ALL_ACCESS});
        if (handles[1] != REG_SUCCESS) {
            return handles[1];  // can be REG_NOTFOUND, REG_ACCESSDENIED
        }
        int rc = Methods.invoke(root, regDeleteValue,
                new Object[]{
                        handles[0], toCstr(value)
                });
        Methods.invoke(root, regCloseKey, handles[0]);
        return rc;
    }

    private static int deleteKey(Preferences root, int hkey, String key)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        return Methods.invoke(root, regDeleteKey,
                new Object[]{hkey, toCstr(key)});  // can REG_NOTFOUND, REG_ACCESSDENIED, REG_SUCCESS
    }

    private static String readString(Preferences root, int hkey, String key, String value)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        int[] handles = Methods.invoke(root, regOpenKey, new Object[]{
                hkey, toCstr(key), KEY_READ});
        if (handles[1] != REG_SUCCESS) {
            return null;
        }
        byte[] valb = Methods.invoke(root, regQueryValueEx, new Object[]{
                handles[0], toCstr(value)});
        Methods.invoke(root, regCloseKey, handles[0]);
        return (valb != null ? new String(valb).trim() : null);
    }

    private static String readString(Preferences root, long hkey, String key, String value)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        long[] handles = Methods.invoke(root, regOpenKey, new Object[]{
                hkey, toCstr(key), KEY_READ});
        if (handles[1] != REG_SUCCESS) {
            return null;
        }
        byte[] valb = Methods.invoke(root, regQueryValueEx, new Object[]{
                handles[0], toCstr(value)});
        Methods.invoke(root, regCloseKey, handles[0]);
        return (valb != null ? new String(valb).trim() : null);
    }

    @SuppressWarnings("DuplicatedCode")
    private static Map<String, String> readStringValues
            (Preferences root, int hkey, String key)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        HashMap<String, String> results = new HashMap<>();
        int[] handles = Methods.invoke(root, regOpenKey, new Object[]{
                hkey, toCstr(key), KEY_READ});
        if (handles[1] != REG_SUCCESS) {
            return null;
        }
        int[] info = Methods.invoke(root, regQueryInfoKey,
                new Object[]{handles[0]});

        int count = info[0]; // count
        int maxlen = info[3]; // value length max
        for (int index = 0; index < count; index++) {
            byte[] name = Methods.invoke(root, regEnumValue, new Object[]{
                    handles[0], index, maxlen + 1});
            String value = readString(hkey, key, new String(name));
            results.put(new String(name).trim(), value);
        }
        Methods.invoke(root, regCloseKey, handles[0]);
        return results;
    }

    @SuppressWarnings("DuplicatedCode")
    private static List<String> readStringSubKeys
            (Preferences root, int hkey, String key)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        List<String> results = new ArrayList<>();
        int[] handles = Methods.invoke(root, regOpenKey, new Object[]{
                hkey, toCstr(key), KEY_READ
        });
        if (handles[1] != REG_SUCCESS) {
            return null;
        }
        int[] info = Methods.invoke(root, regQueryInfoKey,
                new Object[]{handles[0]});

        int count = info[0]; // Fix: info[2] was being used here with wrong results. Suggested by davenpcj, confirmed by Petrucio
        int maxlen = info[3]; // value length max
        for (int index = 0; index < count; index++) {
            byte[] name = Methods.invoke(root, regEnumKeyEx, new Object[]{
                    handles[0], index, maxlen + 1
            });
            results.add(new String(name).trim());
        }
        Methods.invoke(root, regCloseKey, handles[0]);
        return results;
    }

    private static int[] createKey(Preferences root, int hkey, String key)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        return Methods.invoke(root, regCreateKeyEx,
                new Object[]{hkey, toCstr(key)});
    }

    private static long[] createKey(Preferences root, long hkey, String key)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        return Methods.invoke(root, regCreateKeyEx,
                new Object[]{hkey, toCstr(key)});
    }

    private static void writeStringValue
            (Preferences root, int hkey, String key, String valueName, String value)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        int[] handles = Methods.invoke(root, regOpenKey, new Object[]{
                hkey, toCstr(key), KEY_ALL_ACCESS});

        Methods.invoke(root, regSetValueEx,
                handles[0], toCstr(valueName), toCstr(value));
        Methods.invoke(root, regCloseKey, handles[0]);
    }

    private static void writeStringValue
            (Preferences root, long hkey, String key, String valueName, String value)
            throws IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        long[] handles = Methods.invoke(root, regOpenKey, new Object[]{
                hkey, toCstr(key), KEY_ALL_ACCESS});

        Methods.invoke(root, regSetValueEx,
                handles[0], toCstr(valueName), toCstr(value));
        Methods.invoke(root, regCloseKey, handles[0]);
    }

    // utility
    private static byte[] toCstr(String str) {
        byte[] result = new byte[str.length() + 1];

        for (int i = 0; i < str.length(); i++) {
            result[i] = (byte) str.charAt(i);
        }
        result[str.length()] = 0;
        return result;
    }
}
