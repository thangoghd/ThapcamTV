package com.thangoghd.thapcamtv.repositories;

public interface RepositoryCallback<T> {
    void onSuccess(T result);
    void onError(Exception e);
}
