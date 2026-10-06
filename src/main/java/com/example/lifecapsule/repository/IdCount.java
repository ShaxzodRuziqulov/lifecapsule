package com.example.lifecapsule.repository;

/** Row of a grouped "count per id" query. */
public interface IdCount {
    Long getId();

    Long getTotal();
}
