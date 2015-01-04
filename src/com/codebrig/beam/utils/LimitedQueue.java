/*
 * Copyright © 2014 CodeBrig, LLC. All rights reserved.
 * CONFIDENTIAL & PROPRIETARY, NOT FOR PUBLIC RELEASE.
 */
package com.codebrig.beam.utils;

import java.util.LinkedList;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class LimitedQueue<E> extends LinkedList<E>
{

    private final int limit;

    public LimitedQueue (int limit) {
        this.limit = limit;
    }

    @Override
    public boolean add (E o) {
        boolean added = super.add (o);
        while (added && size () > limit) {
            super.remove ();
        }

        return added;
    }

}
