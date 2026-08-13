package com.samleighton.sethomestwo.dao;

import java.util.List;

public interface Dao<T> {

    /**
     * Retrieve all models
     * @param keys Keys used to retrieve the model
     */
    List<T> getAll(Object... keys);

    /**
     * Retrieve a single model
     * @param keys Keys used to retrieve the model
     */
    T get(Object... keys);

    /**
     * Save a single model
     * @param object The model to save
     */
    boolean save(Object object);

    /**
     * Delete a single model
     * @param object The model to delete
     */
    boolean delete(Object object);

    /**
     * Update a single existing model.
     *
     * @param object The model to update
     * @return true when a row was updated
     * @implNote Default returns false. Only implementations that own a mutable
     * table override this.
     */
    default boolean update(Object object) {
        return false;
    }
}
