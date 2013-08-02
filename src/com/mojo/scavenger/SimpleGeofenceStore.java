/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.mojo.scavenger;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

/**
 * Storage for geofence values, implemented in SharedPreferences.
 * For a production app, use a content provider that's synced to the
 * web or loads geofence data based on current location.
 */
public class SimpleGeofenceStore {

    // The SharedPreferences object in which geofences are stored
    private final SharedPreferences mPrefs;

    // The name of the resulting SharedPreferences
    private static final String SHARED_PREFERENCE_NAME =
                    MainActivity.class.getSimpleName();
    
    // ID for count of geofences in storage
    private static final String FENCELIST_ID_LIST_ID =
    				"FENCELIST_LIST";

    // Create the SharedPreferences storage with private access only
    public SimpleGeofenceStore(Context context) {
        mPrefs =
        		PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    /** Returns a set of IDs. One for each stored geofence
     * 
     */
    public Set<String> getStoredIDs() {
    	Set<String> ret;
    	
    	ret = mPrefs.getStringSet(FENCELIST_ID_LIST_ID, null);
    	return ret;
    }
    

    /**
     * Returns a stored geofence by its id, or returns {@code null}
     * if it's not found.
     *
     * @param id The ID of a stored geofence
     * @return A geofence defined by its center and radius. See
     * {@link SimpleGeofence}
     */
    public SimpleGeofence getGeofence(String id) {

        /*
         * Get the latitude for the geofence identified by id, or GeofenceUtils.INVALID_VALUE
         * if it doesn't exist
         */
        double lat = mPrefs.getFloat(
                getGeofenceFieldKey(id, GeofenceUtils.KEY_LATITUDE),
                GeofenceUtils.INVALID_FLOAT_VALUE);

        /*
         * Get the longitude for the geofence identified by id, or
         * -999 if it doesn't exist
         */
        double lng = mPrefs.getFloat(
                getGeofenceFieldKey(id, GeofenceUtils.KEY_LONGITUDE),
                GeofenceUtils.INVALID_FLOAT_VALUE);

        /*
         * Get the radius for the geofence identified by id, or GeofenceUtils.INVALID_VALUE
         * if it doesn't exist
         */
        float radius = mPrefs.getFloat(
                getGeofenceFieldKey(id, GeofenceUtils.KEY_RADIUS),
                GeofenceUtils.INVALID_FLOAT_VALUE);

        /*
         * Get the expiration duration for the geofence identified by
         * id, or GeofenceUtils.INVALID_VALUE if it doesn't exist
         */
        long expirationDuration = mPrefs.getLong(
                getGeofenceFieldKey(id, GeofenceUtils.KEY_EXPIRATION_DURATION),
                GeofenceUtils.INVALID_LONG_VALUE);

        /*
         * Get the transition type for the geofence identified by
         * id, or GeofenceUtils.INVALID_VALUE if it doesn't exist
         */
        int transitionType = mPrefs.getInt(
                getGeofenceFieldKey(id, GeofenceUtils.KEY_TRANSITION_TYPE),
                GeofenceUtils.INVALID_INT_VALUE);
        
        /**
         * Get the message ID associated with this geofence
         */
        String msgId = mPrefs.getString(
        		getGeofenceFieldKey(id, GeofenceUtils.KEY_MSGID_TYPE),
        		GeofenceUtils.INVALID_STR_VALUE);

        // If none of the values is incorrect, return the object
        if (
            lat != GeofenceUtils.INVALID_FLOAT_VALUE &&
            lng != GeofenceUtils.INVALID_FLOAT_VALUE &&
            radius != GeofenceUtils.INVALID_FLOAT_VALUE &&
            expirationDuration != GeofenceUtils.INVALID_LONG_VALUE &&
            transitionType != GeofenceUtils.INVALID_INT_VALUE &&
            msgId != GeofenceUtils.INVALID_STR_VALUE) {

            // Return a true Geofence object
            return new SimpleGeofence(id, lat, lng, radius, expirationDuration, transitionType, msgId);

        // Otherwise, return null.
        } else {
            return null;
        }
    }

    /**
     * Save a geofence.

     * @param geofence The {@link SimpleGeofence} containing the
     * values you want to save in SharedPreferences
     */
    public void putGeofence(String id, SimpleGeofence geofence) {

        /*
         * Get a SharedPreferences editor instance. Among other
         * things, SharedPreferences ensures that updates are atomic
         * and non-concurrent
         */
        Editor editor = mPrefs.edit();
        
        Set<String> _ids =  mPrefs.getStringSet(FENCELIST_ID_LIST_ID, new CopyOnWriteArraySet<String>());
        ArrayList<String> ids = new ArrayList<String>(_ids);
        
        // Write the Geofence values to SharedPreferences
        editor.putFloat(
                getGeofenceFieldKey(id, GeofenceUtils.KEY_LATITUDE),
                (float) geofence.getLatitude());

        editor.putFloat(
                getGeofenceFieldKey(id, GeofenceUtils.KEY_LONGITUDE),
                (float) geofence.getLongitude());

        editor.putFloat(
                getGeofenceFieldKey(id, GeofenceUtils.KEY_RADIUS),
                geofence.getRadius());

        editor.putLong(
                getGeofenceFieldKey(id, GeofenceUtils.KEY_EXPIRATION_DURATION),
                geofence.getExpirationDuration());

        editor.putInt(
                getGeofenceFieldKey(id, GeofenceUtils.KEY_TRANSITION_TYPE),
                geofence.getTransitionType());
        
        editor.putString(
        		getGeofenceFieldKey(id, GeofenceUtils.KEY_MSGID_TYPE),
        		geofence.getMsgId());
        
        // Update fencelist id list
        ids.add(id);        
        Set<String> new_ids = new CopyOnWriteArraySet<String>(ids);        
        editor.putStringSet(FENCELIST_ID_LIST_ID, new_ids);
        
        // Commit the changes
        editor.commit();
    }

    public void clearGeofence(String id) {

        // Remove a flattened geofence object from storage by removing all of its keys
        Editor editor = mPrefs.edit();
        
        // Remove from id list
        Set<String> _ids =  mPrefs.getStringSet(FENCELIST_ID_LIST_ID, new CopyOnWriteArraySet<String>());
        ArrayList<String> ids = new ArrayList<String>(_ids);
        ids.remove(id);
        Set<String> new_ids = new CopyOnWriteArraySet<String>(ids);
        editor.putStringSet(FENCELIST_ID_LIST_ID, new_ids);
        
        // Remove all references to this fence
        editor.remove(getGeofenceFieldKey(id, GeofenceUtils.KEY_LATITUDE));
        editor.remove(getGeofenceFieldKey(id, GeofenceUtils.KEY_LONGITUDE));
        editor.remove(getGeofenceFieldKey(id, GeofenceUtils.KEY_RADIUS));
        editor.remove(getGeofenceFieldKey(id, GeofenceUtils.KEY_EXPIRATION_DURATION));
        editor.remove(getGeofenceFieldKey(id, GeofenceUtils.KEY_TRANSITION_TYPE));
        editor.commit();
    }

    /**
     * Given a Geofence object's ID and the name of a field
     * (for example, GeofenceUtils.KEY_LATITUDE), return the key name of the
     * object's values in SharedPreferences.
     *
     * @param id The ID of a Geofence object
     * @param fieldName The field represented by the key
     * @return The full key name of a value in SharedPreferences
     */
    private String getGeofenceFieldKey(String id, String fieldName) {

        return
                GeofenceUtils.KEY_PREFIX +
                id +
                "_" +
                fieldName;
    }
}
