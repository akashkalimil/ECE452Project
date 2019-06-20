package com.teamred.candid.data;

import io.reactivex.Maybe;

public interface Detector<In, Out> {

    /**
     * @param in The type which the detection is applied to
     * @return A Maybe containing the detected value.
     * Maybe.empty() if no target was detected.
     */
    Maybe<Out> detect(In in);
}
